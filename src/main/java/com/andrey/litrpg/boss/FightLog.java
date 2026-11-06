package com.andrey.litrpg.boss;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.SystemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Журнал боя с хранителем (просьба владельца 06.11.2026): кто сколько нанёс, получил и вылечил, от чего получал и
 * от чего пал, какие приёмы хранитель применял, когда менялись фазы. В конце боя — итог в чат всем на арене,
 * полный журнал — файлом {@code logs/litrpg/fights/<дата>_<хранитель>.txt} на сервере, последние бои —
 * {@code /litrpg fights [номер]}. По этим цифрам и правится баланс после проходов группы.
 */
public final class FightLog {
    private FightLog() {
    }

    /** Строка игрока в бою. */
    static final class Row {
        final String name;
        double dealt, taken, healed;
        int deaths;
        final Map<String, Double> takenBy = new HashMap<>();
        final List<String> diedTo = new ArrayList<>();
        String lastHit = "";

        Row(String name) {
            this.name = name;
        }
    }

    static final class Fight {
        final String boss, bossId;
        final long start;
        final int party;
        final LocalDateTime when = LocalDateTime.now();
        final Map<UUID, Row> rows = new LinkedHashMap<>();
        final Map<String, Integer> casts = new LinkedHashMap<>();
        final List<String> events = new ArrayList<>();

        Fight(String boss, String bossId, long start, int party) {
            this.boss = boss;
            this.bossId = bossId;
            this.start = start;
            this.party = party;
        }

        Row row(ServerPlayer p) {
            return rows.computeIfAbsent(p.getUUID(), id -> new Row(p.getGameProfile().getName()));
        }
    }

    private static final Map<UUID, Fight> ACTIVE = new HashMap<>();
    /** Итоги последних боёв (новые — первыми) для /litrpg fights. */
    private static final Deque<List<String>> LAST = new ArrayDeque<>();

    private static Fight of(BossEntity b) {
        return ACTIVE.get(b.logOwner().getUUID());
    }

    private static Fight withPlayer(UUID player) {
        for (Fight f : ACTIVE.values()) if (f.rows.containsKey(player)) return f;
        return null;
    }

    private static String time(long ticks) {
        long s = Math.max(0, ticks) / 20;
        return s / 60 + ":" + String.format(Locale.ROOT, "%02d", s % 60);
    }

    private static long now(BossEntity b) {
        return b.level().getGameTime();
    }

    public static void start(BossEntity b, List<ServerPlayer> players) {
        BossEntity owner = b.logOwner();
        if (ACTIVE.containsKey(owner.getUUID())) {
            Fight f = ACTIVE.get(owner.getUUID());
            for (ServerPlayer p : players) f.row(p);
            return;
        }
        Fight f = new Fight(owner.type().name(), owner.type().id(), now(b), owner.partySize());
        for (ServerPlayer p : players) f.row(p);
        ACTIVE.put(owner.getUUID(), f);
    }

    public static void cast(BossEntity b, String ability) {
        Fight f = of(b);
        if (f == null) return;
        String key = b == b.logOwner() ? ability : b.type().name() + ": " + ability;
        f.casts.merge(key, 1, Integer::sum);
    }

    public static void event(BossEntity b, String text) {
        Fight f = of(b);
        if (f != null) f.events.add(time(now(b) - f.start) + "  " + text);
    }

    /** Хранитель ударил бойца: сколько реально снято и каким приёмом. */
    public static void hit(BossEntity b, ServerPlayer p, String label, double amount) {
        Fight f = of(b);
        if (f == null || amount <= 0) return;
        Row r = f.row(p);
        r.taken += amount;
        r.takenBy.merge(label, amount, Double::sum);
        r.lastHit = label;
    }

    /** Боец нанёс урон хранителю. */
    public static void dealt(BossEntity b, ServerPlayer p, double amount) {
        Fight f = of(b);
        if (f != null && amount > 0) f.row(p).dealt += amount;
    }

    /** Лечение во время боя: засчитывается лекарю, если он в бою. */
    public static void healed(ServerPlayer healer, double amount) {
        if (healer == null || amount <= 0) return;
        Fight f = withPlayer(healer.getUUID());
        if (f != null) f.row(healer).healed += amount;
    }

    /** Боец пал (стал духом). */
    public static void died(ServerPlayer p) {
        Fight f = withPlayer(p.getUUID());
        if (f == null) return;
        Row r = f.row(p);
        r.deaths++;
        String by = r.lastHit.isEmpty() ? "?" : r.lastHit;
        r.diedTo.add(by);
        f.events.add(time(p.level().getGameTime() - f.start) + "  пал " + r.name + " — " + by);
    }

    /** Бой кончился: итог в чат, журнал — в файл. */
    public static void end(BossEntity b, String outcome, List<ServerPlayer> watchers) {
        BossEntity owner = b.logOwner();
        Fight f = ACTIVE.remove(owner.getUUID());
        if (f == null) return;
        long dur = now(b) - f.start;
        double total = f.rows.values().stream().mapToDouble(r -> r.dealt).sum();
        List<String> summary = new ArrayList<>();
        summary.add(f.boss + " — " + outcome + " за " + time(dur) + " (отряд " + f.party + ")");
        List<Row> rows = new ArrayList<>(f.rows.values());
        rows.sort(Comparator.comparingDouble((Row r) -> -r.dealt));
        for (Row r : rows) {
            summary.add(String.format(Locale.ROOT, "%s: урон %,.0f (%.0f%%), %,.0f/с; получил %,.0f; лечил %,.0f%s", r.name, r.dealt,
                    total > 0 ? r.dealt * 100 / total : 0, r.dealt / Math.max(1, dur / 20.0), r.taken, r.healed,
                    r.deaths > 0 ? "; пал ×" + r.deaths + " (" + String.join(", ", r.diedTo) + ")" : ""));
        }
        Map<String, Double> byAbility = new HashMap<>();
        for (Row r : rows) r.takenBy.forEach((k, v) -> byAbility.merge(k, v, Double::sum));
        List<Map.Entry<String, Double>> top = new ArrayList<>(byAbility.entrySet());
        top.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        if (!top.isEmpty()) {
            StringBuilder sb = new StringBuilder("Больнее всего: ");
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(top.get(i).getKey()).append(String.format(Locale.ROOT, " %,.0f", top.get(i).getValue()));
            }
            summary.add(sb.toString());
        }
        synchronized (LAST) {
            LAST.addFirst(summary);
            while (LAST.size() > 10) LAST.removeLast();
        }
        for (ServerPlayer p : watchers) {
            SystemChat.send(p, Component.literal("Итог боя: " + summary.get(0)).withStyle(ChatFormatting.GOLD));
            for (int i = 1; i < summary.size(); i++) SystemChat.send(p, Component.literal("  " + summary.get(i)).withStyle(ChatFormatting.GRAY));
        }
        write(b.getServer(), f, outcome, dur, summary, top);
    }

    private static void write(MinecraftServer server, Fight f, String outcome, long dur, List<String> summary,
                              List<Map.Entry<String, Double>> top) {
        if (server == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Бой: ").append(f.boss).append(" (").append(f.bossId).append(")\n");
        sb.append("Начало: ").append(f.when.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))).append('\n');
        sb.append("Итог: ").append(outcome).append(", длительность ").append(time(dur)).append(", отряд ").append(f.party).append("\n\n");
        sb.append("== Итог\n");
        for (String s : summary) sb.append(s).append('\n');
        sb.append("\n== Получено по приёмам\n");
        for (Row r : f.rows.values()) {
            sb.append(r.name).append(":\n");
            r.takenBy.entrySet().stream().sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .forEach(e -> sb.append(String.format(Locale.ROOT, "  %-28s %,10.0f%n", e.getKey(), e.getValue())));
        }
        sb.append("\n== Приёмы хранителя (сколько раз)\n");
        f.casts.forEach((k, v) -> sb.append(String.format(Locale.ROOT, "  %-28s %d%n", k, v)));
        sb.append("\n== События\n");
        for (String e : f.events) sb.append("  ").append(e).append('\n');
        try {
            Path dir = server.getServerDirectory().resolve("logs/litrpg/fights");
            Files.createDirectories(dir);
            String name = f.when.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "_" + f.bossId + ".txt";
            Files.writeString(dir.resolve(name), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LitRPG.LOG.warn("[LitRPG] журнал боя не записан: {}", e.toString());
        }
    }

    /** Итоги последних боёв для команды (0 — последний). */
    public static List<List<String>> last() {
        synchronized (LAST) {
            return new ArrayList<>(LAST);
        }
    }
}
