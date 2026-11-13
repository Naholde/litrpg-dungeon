package com.andrey.litrpg.run;

import com.andrey.litrpg.boss.BossRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Доски лобби (обновление 15): text_display по схеме {@code lobby.boards} — рекорды Зала славы, «Сейчас в подземелье»
 * на Площади, паркур, полигон, титулы. Раз в 2 секунды: нет доски — ставится, текст изменился — переписывается
 * (только изменившиеся). Метка {@code litrpg_board} — доски не путаются с чужими.
 */
public final class LobbyBoards {
    static final String TAG = "litrpg_board";
    private static final Map<String, String> SHOWN = new HashMap<>();

    private LobbyBoards() {
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = RunService.dungeon(server);
        if (level == null) return;
        sweep(level);
        for (Layout.Board b : Layout.get().lobby().boards()) {
            if (!level.isLoaded(net.minecraft.core.BlockPos.containing(b.at()))) continue;
            Component text = text(server, b.id());
            if (text == null) continue;
            place(level, TAG + ":" + b.id(), b.at(), b.yaw(), b.scale(), b.width(), b.id().equals("now"), text, 0xD0101826);
        }
    }

    /** Убрать доски и таблички лобби, которых больше нет в схеме или которые стоят не там (карта поменялась). */
    private static void sweep(ServerLevel level) {
        Map<String, Vec3> want = new HashMap<>();
        for (Layout.Board b : Layout.get().lobby().boards()) want.put(TAG + ":" + b.id(), b.at());
        for (Layout.TrophySpot t : Layout.get().lobby().trophies()) {
            want.put(LobbyTrophies.TAG + ":" + t.boss() + ":plaque", t.at().add(0, 3.4, 0));
        }
        for (Display.TextDisplay d : level.getEntitiesOfClass(Display.TextDisplay.class, Layout.get().lobby().box().aabb().inflate(4))) {
            for (String tag : d.getTags()) {
                if (!tag.startsWith(TAG + ":") && !tag.startsWith(LobbyTrophies.TAG + ":")) continue;
                Vec3 at = want.get(tag);
                if (at == null || d.position().distanceToSqr(at) > 2.25) {
                    d.discard();
                    SHOWN.remove(tag);
                }
            }
        }
    }

    /** Поставить или обновить text_display с меткой {@code tag}. */
    static void place(ServerLevel level, String tag, Vec3 at, float yaw, float scale, int width, boolean billboard,
                      Component text, int background) {
        String json = Component.Serializer.toJson(text, level.registryAccess());
        List<Display.TextDisplay> found = level.getEntitiesOfClass(Display.TextDisplay.class, new AABB(at, at).inflate(1.5),
                e -> e.getTags().contains(tag));
        for (int i = 1; i < found.size(); i++) found.get(i).discard();
        Display.TextDisplay d = found.isEmpty() ? null : found.get(0);
        if (d != null && json.equals(SHOWN.get(tag)) && d.position().distanceToSqr(at) < 0.01) return;
        if (d == null) {
            d = EntityType.TEXT_DISPLAY.create(level);
            if (d == null) return;
            d.addTag(tag);
            d.moveTo(at.x, at.y, at.z, yaw, 0);
            level.addFreshEntity(d);
        }
        CompoundTag nbt = d.saveWithoutId(new CompoundTag());
        nbt.putString("text", json);
        nbt.putInt("line_width", width);
        nbt.putInt("background", background);
        nbt.putBoolean("shadow", true);
        nbt.putString("billboard", billboard ? "vertical" : "fixed");
        nbt.putString("alignment", "center");
        CompoundTag tr = new CompoundTag();
        tr.put("scale", floats(scale, scale, scale));
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        nbt.put("transformation", tr);
        d.load(nbt);
        d.moveTo(at.x, at.y, at.z, yaw, 0);
        SHOWN.put(tag, json);
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) l.add(FloatTag.valueOf(f));
        return l;
    }

    // ------------------------------------------------------------------ тексты

    private static MutableComponent title(String s) {
        return Component.literal(s + "\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
    }

    private static MutableComponent line(String s, ChatFormatting c) {
        return Component.literal(s + "\n").withStyle(c);
    }

    private static String names(List<String> n) {
        String s = String.join(", ", n);
        return s.length() > 40 ? s.substring(0, 39) + "…" : s;
    }

    static String bossName(String id) {
        BossRegistry.Type t = BossRegistry.get(id);
        return t == null ? id : t.name();
    }

    static Component text(MinecraftServer server, String id) {
        Records r = Records.get(server);
        return switch (id) {
            case "now" -> now(server);
            case "best_normal" -> best(r, Difficulty.NORMAL);
            case "best_heroic" -> best(r, Difficulty.HEROIC);
            case "single_normal" -> single(r, Difficulty.NORMAL);
            case "single_heroic" -> single(r, Difficulty.HEROIC);
            case "bosses_normal" -> bosses(r, Difficulty.NORMAL);
            case "bosses_heroic" -> bosses(r, Difficulty.HEROIC);
            case "heroes" -> heroes(r);
            case "history" -> history(r);
            case "parkour" -> parkour(r);
            case "polygon" -> polygon(r);
            case "titles" -> titles(r);
            default -> null;
        };
    }

    private static Component now(MinecraftServer server) {
        RunState s = RunService.state(server);
        MutableComponent c = Component.empty();
        if (s.active(Layout.get().lastStage())) {
            Layout.Stage st = RunService.currentStage(server);
            long secs = (server.overworld().getGameTime() - s.runStart) / 20;
            c.append(title("Сейчас в подземелье"));
            c.append(line("Этап " + s.stage + (st == null ? "" : " · " + st.name()), ChatFormatting.YELLOW));
            c.append(Component.literal("Сложность: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(s.difficulty.title + "\n").withColor(s.difficulty.color & 0xFFFFFF));
            c.append(line("Отряд: " + names(new ArrayList<>(s.names.values())), ChatFormatting.WHITE));
            c.append(line("Время: " + Chronicle.time(secs * 20) + " · вайпов: " + s.wipes, ChatFormatting.GRAY));
            c.append(line("Наблюдать — окно сбора (P)", ChatFormatting.DARK_GRAY));
        } else {
            List<net.minecraft.server.level.ServerPlayer> lobby = LobbyService.inLobby(server);
            long ready = lobby.stream().filter(p -> LobbyService.isReady(p.getUUID())).count();
            c.append(title("Цитадель ждёт отряд"));
            c.append(line("В лобби: " + lobby.size() + " · готовы: " + ready, ChatFormatting.WHITE));
            c.append(Component.literal("Сложность: ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(LobbyService.chosen().title + "\n").withColor(LobbyService.chosen().color & 0xFFFFFF));
            c.append(line("ПКМ по Печати или P — сбор", ChatFormatting.GRAY));
        }
        return c;
    }

    private static Component best(Records r, Difficulty d) {
        MutableComponent c = Component.empty().append(title("Быстрейший забег"));
        c.append(Component.literal(d.title + "\n").withColor(d.color & 0xFFFFFF));
        Records.RunRecord b = r.bestRun().get(d.id);
        if (b == null) {
            c.append(line("\nЕщё никто не прошёл.", ChatFormatting.GRAY));
            c.append(line("Станьте первыми!", ChatFormatting.DARK_GRAY));
            return c;
        }
        c.append(line("\n" + Chronicle.time(b.ticks()), ChatFormatting.YELLOW));
        c.append(line(names(b.names()), ChatFormatting.WHITE));
        c.append(line(b.date() + " · вайпов: " + b.wipes(), ChatFormatting.GRAY));
        List<Records.RunRecord> done = r.history().stream().filter(h -> h.completed() && h.difficulty().equals(d.id))
                .sorted(Comparator.comparingLong(Records.RunRecord::ticks)).limit(4).toList();
        if (done.size() > 1) {
            c.append(line("\nДругие прохождения:", ChatFormatting.DARK_AQUA));
            for (Records.RunRecord h : done) {
                if (h == b || h.ticks() == b.ticks()) continue;
                c.append(line(Chronicle.time(h.ticks()) + " — " + names(h.names()), ChatFormatting.GRAY));
            }
        }
        return c;
    }

    private static Component single(Records r, Difficulty d) {
        MutableComponent c = Component.empty().append(title("Рекорды за забег"));
        c.append(Component.literal(d.title + "\n\n").withColor(d.color & 0xFFFFFF));
        for (Records.Single k : Records.Single.values()) {
            Records.Entry e = r.single().get(d.id + ":" + k.id);
            c.append(Component.literal(k.title + ": ").withStyle(ChatFormatting.GRAY));
            if (e == null) c.append(line("—", ChatFormatting.DARK_GRAY));
            else c.append(line(RecordsCommands.num(e.value()) + " · " + e.who(), ChatFormatting.WHITE));
        }
        return c;
    }

    private static Component bosses(Records r, Difficulty d) {
        MutableComponent c = Component.empty().append(title("Хранители · лучшее время"));
        c.append(Component.literal(d.title + "\n").withColor(d.color & 0xFFFFFF));
        for (Layout.TrophySpot t : Layout.get().lobby().trophies()) {
            Records.RunRecord b = r.bossBest().get(d.id + ":" + t.boss());
            c.append(Component.literal(bossName(t.boss()) + ": ").withStyle(ChatFormatting.GRAY));
            if (b == null) c.append(line("—", ChatFormatting.DARK_GRAY));
            else c.append(line(Chronicle.time(b.ticks()) + " · " + (b.names().isEmpty() ? "" : b.names().get(0)
                    + (b.names().size() > 1 ? " и др." : "")), ChatFormatting.WHITE));
        }
        return c;
    }

    private static Component heroes(Records r) {
        MutableComponent c = Component.empty().append(title("Герои Цитадели"));
        List<Records.Profile> top = r.top(Comparator.<Records.Profile>comparingInt(p -> p.completedNormal + p.completedHeroic * 2)
                .thenComparingInt(p -> p.bosses), 7);
        if (top.isEmpty()) return c.append(line("\nПока никого. Всё впереди.", ChatFormatting.GRAY));
        c.append(line("", ChatFormatting.WHITE));
        int i = 1;
        for (Records.Profile p : top) {
            MutableComponent row = Component.literal(i++ + ". " + p.name).withStyle(ChatFormatting.WHITE);
            Titles t = Titles.byId(p.title);
            if (t != null) row.append(Component.literal(" ")).append(t.badge());
            c.append(row).append(Component.literal("\n"));
            c.append(line("   пройдено " + (p.completedNormal + p.completedHeroic) + " (героич. " + p.completedHeroic
                    + ") · хранителей " + p.bosses, ChatFormatting.GRAY));
        }
        return c;
    }

    private static Component history(Records r) {
        MutableComponent c = Component.empty().append(title("Летопись забегов"));
        if (r.history().isEmpty()) return c.append(line("\nЗаписей ещё нет.", ChatFormatting.GRAY));
        c.append(line("", ChatFormatting.WHITE));
        for (Records.RunRecord h : r.history().stream().limit(6).toList()) {
            Difficulty d = Difficulty.byId(h.difficulty());
            c.append(Component.literal(h.date() + " · ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(d.title).withColor(d.color & 0xFFFFFF))
                    .append(Component.literal(" · " + (h.completed() ? "пройдено за " + Chronicle.time(h.ticks())
                            : "брошен на этапе " + h.stage()) + "\n").withStyle(h.completed() ? ChatFormatting.GREEN : ChatFormatting.RED));
            c.append(line("   " + names(h.names()), ChatFormatting.WHITE));
        }
        return c;
    }

    private static Component parkour(Records r) {
        MutableComponent c = Component.empty().append(title("Башня ветров"));
        c.append(line("лучшее время паркура", ChatFormatting.AQUA));
        var top = r.parkourTop(8);
        if (top.isEmpty()) return c.append(line("\nНикто ещё не дошёл до вершины.", ChatFormatting.GRAY));
        c.append(line("", ChatFormatting.WHITE));
        int i = 1;
        for (var e : top) c.append(line(i++ + ". " + e.getKey() + " — " + Chronicle.time(e.getValue()),
                i == 2 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
        return c;
    }

    private static Component polygon(Records r) {
        MutableComponent c = Component.empty().append(title("Полигон"));
        c.append(line("лучшие серии уворотов", ChatFormatting.AQUA));
        var top = r.top(Comparator.comparingInt(p -> p.polygonBest), 6).stream().filter(p -> p.polygonBest > 0).toList();
        if (top.isEmpty()) return c.append(line("\nВыйдите на арену —\nИспытатель ждёт.", ChatFormatting.GRAY));
        c.append(line("", ChatFormatting.WHITE));
        int i = 1;
        for (Records.Profile p : top) c.append(line(i++ + ". " + p.name + " — " + p.polygonBest, ChatFormatting.WHITE));
        return c;
    }

    private static Component titles(Records r) {
        MutableComponent c = Component.empty().append(title("Титулы Цитадели"));
        c.append(line("носить — /litrpg title", ChatFormatting.DARK_GRAY));
        for (Titles t : Titles.values()) {
            List<String> who = r.players().values().stream().filter(p -> p.titles.contains(t.id)).map(p -> p.name).toList();
            c.append(t.badge()).append(Component.literal(" " + t.how + "\n").withStyle(ChatFormatting.GRAY));
            c.append(line("   " + (who.isEmpty() ? "— пока никто" : names(who)), who.isEmpty() ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE));
        }
        return c;
    }
}
