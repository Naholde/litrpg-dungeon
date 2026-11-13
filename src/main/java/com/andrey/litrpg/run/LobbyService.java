package com.andrey.litrpg.run;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.net.LobbyActionPayload;
import com.andrey.litrpg.net.LobbySyncPayload;
import com.andrey.litrpg.net.Net;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Сбор отряда в лобби (обновление 15). Кто стоит в лобби — в списке; каждый жмёт «Готов». Когда готовы все, кто в
 * лобби, и их не меньше {@code party.min_players} — отсчёт 5 с и забег (персонажи с 1 уровня, роль — на привале этапа 1).
 * Пришёл новый игрок, кто-то передумал или сменили сложность — отсчёт отменяется: ждём всех.
 * Пока забег идёт: остальные видят отряд, этап, время и хранителя в бою, могут наблюдать; участники могут единогласно
 * «покинуть забег» (он уходит в летопись как брошенный).
 */
public final class LobbyService {
    private LobbyService() {
    }

    static final int COUNTDOWN_TICKS = 5 * 20;

    private static Difficulty chosen = Difficulty.NORMAL;
    /** Готовые — в порядке нажатия (если готовых больше мест — идут первые). */
    private static final Set<UUID> READY = new LinkedHashSet<>();
    private static final Set<UUID> VOTES = new LinkedHashSet<>();
    /** Наблюдатели из лобби (вернуть в лобби по «Вернуться» или когда забег кончится). */
    private static final Set<UUID> WATCHERS = new LinkedHashSet<>();
    private static long countdownEnd = -1;
    private static int lastHash;

    public static Difficulty chosen() {
        return chosen;
    }

    /** Игроки в лобби (в рамке лобби, не наблюдатели). */
    public static List<ServerPlayer> inLobby(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        ServerLevel level = RunService.dungeon(server);
        if (level == null) return out;
        var box = Layout.get().lobby().box();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && box.contains(p.position()) && (onlyForTest == null || onlyForTest.contains(p.getUUID()))) out.add(p);
        }
        return out;
    }

    private static boolean runActive(MinecraftServer server) {
        return RunService.state(server).active(Layout.get().lastStage());
    }

    // ------------------------------------------------------------------ действия

    public static void act(ServerPlayer p, LobbyActionPayload.Action action, String arg) {
        MinecraftServer server = p.server;
        switch (action) {
            case OPEN -> send(p, true);
            case READY -> toggleReady(p);
            case DIFFICULTY -> setDifficulty(p, Difficulty.byId(arg));
            case ABANDON -> voteAbandon(p);
            case SPECTATE -> spectate(p);
            case RETURN -> returnToLobby(p);
        }
        broadcast(server, true);
    }

    /** ПКМ по Печати — окно сбора. */
    public static void openFor(ServerPlayer p) {
        send(p, true);
    }

    static void toggleReady(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (runActive(server)) {
            SystemChat.send(p, Component.literal("Забег уже идёт — дождитесь его конца или наблюдайте.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!inLobby(server).contains(p)) {
            SystemChat.send(p, Component.literal("Готовность — только в лобби.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!READY.remove(p.getUUID())) {
            READY.add(p.getUUID());
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 0.6f, 1.4f);
        } else {
            cancelCountdown(server, p.getGameProfile().getName() + " не готов");
        }
    }

    static void setDifficulty(ServerPlayer p, Difficulty d) {
        MinecraftServer server = p.server;
        if (runActive(server) || d == chosen) return;
        chosen = d;
        cancelCountdown(server, null);
        for (ServerPlayer q : inLobby(server)) {
            SystemChat.send(q, Component.literal(p.getGameProfile().getName() + " выбрал сложность: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(d.title).withColor(d.color & 0xFFFFFF)));
        }
    }

    static void voteAbandon(ServerPlayer p) {
        MinecraftServer server = p.server;
        RunState s = RunService.state(server);
        if (!runActive(server) || !s.member(p.getUUID())) return;
        if (!VOTES.add(p.getUUID())) {
            VOTES.remove(p.getUUID());
            return;
        }
        List<ServerPlayer> members = RunService.onlineMembers(server);
        for (ServerPlayer m : members) {
            SystemChat.send(m, Component.literal(p.getGameProfile().getName() + " предлагает покинуть забег ("
                    + votes(server) + "/" + members.size() + "). Окно сбора — клавиша P.").withStyle(ChatFormatting.GOLD));
        }
        if (members.stream().allMatch(m -> VOTES.contains(m.getUUID()))) {
            for (ServerPlayer q : server.getPlayerList().getPlayers()) {
                SystemChat.send(q, Component.literal("Отряд покинул подземелье. Забег записан в летопись.").withStyle(ChatFormatting.GRAY));
            }
            RunService.reset(server);
        }
    }

    private static int votes(MinecraftServer server) {
        return (int) RunService.onlineMembers(server).stream().filter(m -> VOTES.contains(m.getUUID())).count();
    }

    static void spectate(ServerPlayer p) {
        MinecraftServer server = p.server;
        RunState s = RunService.state(server);
        if (!runActive(server) || s.member(p.getUUID())) return;
        List<ServerPlayer> members = RunService.aliveMembers(server);
        if (members.isEmpty()) members = RunService.onlineMembers(server);
        if (members.isEmpty()) {
            SystemChat.send(p, Component.literal("Никого из отряда нет в сети — смотреть не на кого.").withStyle(ChatFormatting.GRAY));
            return;
        }
        READY.remove(p.getUUID());
        WATCHERS.add(p.getUUID());
        ServerPlayer target = members.get(p.getRandom().nextInt(members.size()));
        p.setGameMode(GameType.SPECTATOR);
        p.teleportTo(target.serverLevel(), target.getX(), target.getY() + 1, target.getZ(), target.getYRot(), target.getXRot());
        SystemChat.send(p, Component.literal("Вы наблюдаете за забегом. Вернуться — окно сбора (P) → «В лобби».")
                .withStyle(ChatFormatting.GRAY));
    }

    static void returnToLobby(ServerPlayer p) {
        if (!WATCHERS.remove(p.getUUID()) && !p.isSpectator()) return;
        if (RunService.state(p.server).member(p.getUUID())) return;   // участник — не через лобби
        p.setGameMode(GameType.SURVIVAL);
        p.setCamera(p);
        RunService.toLobby(p);
    }

    private static void cancelCountdown(MinecraftServer server, String why) {
        if (countdownEnd < 0) return;
        countdownEnd = -1;
        for (ServerPlayer q : inLobby(server)) {
            SystemChat.send(q, Component.literal("Отсчёт остановлен" + (why == null ? "" : ": " + why) + ".").withStyle(ChatFormatting.YELLOW));
        }
    }

    // ------------------------------------------------------------------ тик

    /** Раз в 10 тиков: готовность, отсчёт, конец наблюдения, рассылка. */
    public static void tick(MinecraftServer server) {
        if (server instanceof net.minecraft.gametest.framework.GameTestServer && !RunService.testing) return;
        if (RunService.dungeon(server) == null) return;
        long now = server.overworld().getGameTime();
        boolean active = runActive(server);
        List<ServerPlayer> lobby = inLobby(server);
        Set<UUID> here = new LinkedHashSet<>();
        lobby.forEach(p -> here.add(p.getUUID()));
        if (READY.retainAll(here) && countdownEnd >= 0) cancelCountdown(server, "кто-то ушёл из лобби");
        if (!active) {
            VOTES.clear();
            // забег кончился — наблюдатели возвращаются
            for (UUID id : List.copyOf(WATCHERS)) {
                ServerPlayer w = server.getPlayerList().getPlayer(id);
                if (w != null) returnToLobby(w);
                else WATCHERS.remove(id);
            }
            int min = minPlayers(server);
            boolean allReady = !lobby.isEmpty() && READY.containsAll(here) && lobby.size() >= min;
            if (allReady && countdownEnd < 0) {
                countdownEnd = now + COUNTDOWN_TICKS;
                for (ServerPlayer q : lobby) {
                    SystemChat.title(q, Component.literal("Все готовы").withStyle(ChatFormatting.GOLD),
                            Component.literal("Сложность: " + chosen.title + " · старт через 5 с").withStyle(ChatFormatting.GRAY));
                }
            } else if (!allReady && countdownEnd >= 0) {
                cancelCountdown(server, lobby.size() < min ? "мало игроков" : "не все готовы");
            }
            if (countdownEnd >= 0) {
                long left = countdownEnd - now;
                if (left <= 0) {
                    launch(server, lobby);
                } else if (left % 20 < 10) {
                    for (ServerPlayer q : lobby) {
                        q.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 0.6f, 1.2f);
                    }
                }
            }
        } else {
            countdownEnd = -1;
            READY.clear();
        }
        broadcast(server, false);
    }

    private static int minPlayers(MinecraftServer server) {
        RunState s = RunService.state(server);
        return s.minPlayers > 0 ? s.minPlayers : Balance.get(Balance.RUN).party().minPlayers();
    }

    private static void launch(MinecraftServer server, List<ServerPlayer> lobby) {
        countdownEnd = -1;
        int max = Math.min(Balance.get(Balance.RUN).party().maxPlayers(), com.andrey.litrpg.party.Party.MAX);
        List<ServerPlayer> party = new ArrayList<>();
        for (UUID id : READY) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && lobby.contains(p) && party.size() < max) party.add(p);
        }
        READY.clear();
        if (party.isEmpty()) return;
        RunService.start(server, party, chosen, true);
        for (ServerPlayer p : lobby) {
            if (!party.contains(p)) SystemChat.send(p, "Отряд полон (" + party.size() + "). Вы останетесь в лобби и можете наблюдать.");
        }
    }

    // ------------------------------------------------------------------ рассылка

    private static void broadcast(MinecraftServer server, boolean force) {
        LobbySyncPayload base = snapshot(server, false);
        int h = base.hashCode();
        if (!force && h == lastHash && server.overworld().getGameTime() % 40 != 0) return;   // время забега — раз в 2 с
        lastHash = h;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) Net.send(p, base);
    }

    private static void send(ServerPlayer p, boolean open) {
        Net.send(p, snapshot(p.server, open));
    }

    static LobbySyncPayload snapshot(MinecraftServer server, boolean open) {
        RunState s = RunService.state(server);
        Layout layout = Layout.get();
        boolean active = s.active(layout.lastStage());
        List<LobbySyncPayload.Member> members = new ArrayList<>();
        if (active) {
            for (UUID id : s.party) {
                members.add(new LobbySyncPayload.Member(id, s.names.getOrDefault(id, "?"), VOTES.contains(id),
                        server.getPlayerList().getPlayer(id) != null));
            }
        } else {
            for (ServerPlayer p : inLobby(server)) {
                members.add(new LobbySyncPayload.Member(p.getUUID(), p.getGameProfile().getName(), READY.contains(p.getUUID()), true));
            }
        }
        LobbySyncPayload.Run run = LobbySyncPayload.Run.NONE;
        if (active) {
            Layout.Stage st = RunService.currentStage(server);
            long secs = (server.overworld().getGameTime() - s.runStart) / 20;
            run = new LobbySyncPayload.Run(true, s.stage, st == null ? "" : st.name(), s.difficulty.id, secs, s.wipes,
                    fighting(server, st), List.copyOf(s.party), votes(server), RunService.onlineMembers(server).size());
        }
        int cd = countdownEnd < 0 ? -1 : (int) Math.max(0, countdownEnd - server.overworld().getGameTime());
        return new LobbySyncPayload(open, chosen.id, minPlayers(server),
                Math.min(Balance.get(Balance.RUN).party().maxPlayers(), com.andrey.litrpg.party.Party.MAX), cd, members, run,
                recordLines(server));
    }

    private static String fighting(MinecraftServer server, Layout.Stage st) {
        if (st == null) return "";
        ServerLevel level = RunService.dungeon(server);
        for (BossEntity b : level.getEntitiesOfClass(BossEntity.class, st.box().aabb().inflate(16))) {
            if (b.isAlive() && b.engaged() && b.runId() != null) {
                int pct = (int) Math.round(100.0 * b.getHealth() / Math.max(1, b.getMaxHealth()));
                return b.type().name() + " · " + pct + "%";
            }
        }
        return "";
    }

    /** Строки «лучший забег» для окна. */
    private static List<String> recordLines(MinecraftServer server) {
        Records r = Records.get(server);
        List<String> out = new ArrayList<>();
        for (Difficulty d : Difficulty.values()) {
            Records.RunRecord rec = r.bestRun().get(d.id);
            out.add(d.title + ": " + (rec == null ? "ещё никто не прошёл" : Chronicle.time(rec.ticks()) + " — "
                    + String.join(", ", rec.names())));
        }
        return out;
    }

    /** Для тестов: учитывать в лобби только этих игроков (на сервере проверок в лобби падают посторонние). */
    public static java.util.Set<UUID> onlyForTest;

    /** Для тестов: сбросить состояние сбора. */
    public static void resetForTest() {
        onlyForTest = null;
        READY.clear();
        VOTES.clear();
        WATCHERS.clear();
        countdownEnd = -1;
        chosen = Difficulty.NORMAL;
        lastHash = 0;
    }

    static boolean isReady(UUID id) {
        return READY.contains(id);
    }

    public static boolean countingForTest() {
        return countdownEnd >= 0;
    }

    static boolean counting() {
        return countdownEnd >= 0;
    }
}
