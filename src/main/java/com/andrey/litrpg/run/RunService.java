package com.andrey.litrpg.run;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.RunBalance;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.RunSyncPayload;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.world.ModWorld;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Прохождение: Печать собирает отряд → этапы 1…5 → конец. Отряд и его размер не меняются до конца. На этапе:
 * отряды и хранители появляются, когда к ним подходят; преграды открываются по условиям; после хранителей вспыхивает
 * разлом — весь живой отряд у разлома переносит на следующий этап. Все пали — вайп: этап заново (уровни и вещи остаются,
 * награды повторно не выдаются).
 */
public final class RunService {
    private static int riftTimer = -1;
    private static int wipeTimer = -1;
    private static long sealConfirmUntil;
    private static int syncTimer;

    private RunService() {
    }

    // ------------------------------------------------------------------ доступ

    public static RunState state(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(RunState.factory(), RunState.NAME);
    }

    public static ServerLevel dungeon(MinecraftServer server) {
        ServerLevel l = server.getLevel(ModWorld.DUNGEON);
        // Сервер автотестов не грузит измерения из данных — там «подземелье» играется в обычном мире.
        return l != null ? l : server instanceof net.minecraft.gametest.framework.GameTestServer ? server.overworld() : null;
    }

    public static boolean inDungeon(Entity e) {
        return e.getServer() != null && e.level() == dungeon(e.getServer());
    }

    public static boolean active(MinecraftServer server) {
        return state(server).active(Layout.get().lastStage());
    }

    /** Игрок — участник идущего прохождения. */
    public static boolean member(ServerPlayer p) {
        RunState s = state(p.server);
        return s.active(Layout.get().lastStage()) && s.member(p.getUUID());
    }

    public static Layout.Stage currentStage(MinecraftServer server) {
        return Layout.get().stage(state(server).stage).orElse(null);
    }

    public static List<ServerPlayer> onlineMembers(MinecraftServer server) {
        List<ServerPlayer> out = new ArrayList<>();
        for (UUID id : state(server).party) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    /** Живые (не духи) участники в сети и в подземелье. */
    public static List<ServerPlayer> aliveMembers(MinecraftServer server) {
        RunState s = state(server);
        return onlineMembers(server).stream().filter(p -> inDungeon(p) && !s.isSpirit(p.getUUID()) && p.isAlive()).toList();
    }

    // ------------------------------------------------------------------ Печать

    /** ПКМ по Печати: собрать отряд из стоящих в преддверии и начать прохождение. */
    public static void onSeal(ServerPlayer clicker) {
        MinecraftServer server = clicker.server;
        RunState s = state(server);
        Layout layout = Layout.get();
        if (s.stage != RunState.LOBBY) {
            SystemChat.send(clicker, s.stage > layout.lastStage()
                    ? "Подземелье уже пройдено. Новый поход начнёт оператор: /litrpg run reset."
                    : "Прохождение уже идёт.");
            return;
        }
        RunBalance.Party cfg = Balance.get(Balance.RUN).party();
        int min = s.minPlayers > 0 ? s.minPlayers : cfg.minPlayers();
        Layout.Lobby lobby = layout.lobby();
        List<ServerPlayer> here = new ArrayList<>();
        for (ServerPlayer p : dungeon(server).players()) {
            if (!p.isSpectator() && lobby.box().contains(p.position())) here.add(p);
        }
        List<ServerPlayer> noRole = here.stream().filter(p -> PlayerService.data(p).role() == Role.NONE).toList();
        if (!noRole.isEmpty()) {
            SystemChat.send(clicker, Component.literal("Не выбрали роль: " + names(noRole) + ". Окно роли — клавиша K.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (here.size() < min) {
            SystemChat.send(clicker, Component.literal("Печать не отзывается: нужно не меньше " + min + " игроков в преддверии"
                    + " (сейчас " + here.size() + ").").withStyle(ChatFormatting.YELLOW));
            return;
        }
        Vec3 seal = Vec3.atCenterOf(lobby.seal());
        here.sort(java.util.Comparator.comparingDouble(p -> p.position().distanceToSqr(seal)));
        List<ServerPlayer> party = here.subList(0, Math.min(cfg.maxPlayers(), here.size()));
        EnumSet<Role> roles = EnumSet.noneOf(Role.class);
        party.forEach(p -> roles.add(PlayerService.data(p).role()));
        List<String> missing = new ArrayList<>();
        if (!roles.contains(Role.TANK)) missing.add("танка");
        if (!roles.contains(Role.SUPPORT)) missing.add("поддержки");
        if (!roles.contains(Role.DAMAGE)) missing.add("бойца");
        long now = server.overworld().getGameTime();
        if (!missing.isEmpty() && now > sealConfirmUntil) {
            sealConfirmUntil = now + Math.round(cfg.sealConfirmSeconds() * 20);
            for (ServerPlayer p : party) {
                SystemChat.send(p, Component.literal("В отряде нет " + String.join(" и ", missing)
                        + " — без всех трёх ролей пройти почти невозможно. Нажмите Печать ещё раз, чтобы начать всё равно.")
                        .withStyle(ChatFormatting.GOLD));
            }
            return;
        }
        sealConfirmUntil = 0;
        start(server, party);
        for (ServerPlayer p : here) {
            if (!party.contains(p)) SystemChat.send(p, "Отряд полон (" + party.size() + "). Вы останетесь наблюдателем.");
        }
    }

    private static String names(List<ServerPlayer> list) {
        return String.join(", ", list.stream().map(p -> p.getGameProfile().getName()).toList());
    }

    /** Начать прохождение с этим отрядом (Печать или команда). */
    public static void start(MinecraftServer server, List<ServerPlayer> party) {
        RunState s = state(server);
        s.resetAll();
        for (ServerPlayer p : party) {
            s.party.add(p.getUUID());
            s.names.put(p.getUUID(), p.getGameProfile().getName());
        }
        s.partySize = party.size();
        s.runStart = server.overworld().getGameTime();
        PartyService.form(party);
        for (ServerPlayer p : party) {
            SystemChat.title(p, Component.literal("Печать снята").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("Отряд: " + party.size() + " · подземелье подстроилось под вас").withStyle(ChatFormatting.GRAY));
        }
        enterStage(server, 1);
    }

    // ------------------------------------------------------------------ этапы

    /** Перенести отряд на этап {@code n} (новый этап — новая «эпоха»: всё прошлое убирается). */
    public static void enterStage(MinecraftServer server, int n) {
        RunState s = state(server);
        ServerLevel level = dungeon(server);
        Layout layout = Layout.get();
        Layout.Stage prev = layout.stage(s.stage).orElse(null);
        Layout.Stage st = layout.stage(n).orElse(null);
        if (st == null) {
            finish(server);
            return;
        }
        StageWorld.restoreBroken(level, s);
        StageWorld.clearEntities(level, false);
        s.stage = n;
        s.attempt = 1;
        s.epoch++;
        s.clearAttempt();
        s.stageStart = server.overworld().getGameTime();
        riftTimer = -1;
        wipeTimer = -1;
        StageWorld.ensureTrader(level, s, st);
        for (ServerPlayer p : onlineMembers(server)) {
            if (prev != null) Rewards.topUp(p, prev);
            bringToCamp(p, st, true);
            SystemChat.title(p, Component.literal("Этап " + n + " · " + st.name()).withStyle(ChatFormatting.GOLD),
                    Component.literal("Уровни " + st.fromLevel() + "–" + st.toLevel()).withStyle(ChatFormatting.GRAY));
        }
        s.setDirty();
        syncAll(server);
    }

    /** На привал этапа: ожить, полное здоровье, перезарядки и метки сброшены. */
    static void bringToCamp(ServerPlayer p, Layout.Stage st, boolean fullHeal) {
        Spirits.restore(p);
        Protect.forget(p.getUUID());
        Layout.Spot c = st.camp();
        p.teleportTo(dungeon(p.server), c.x(), c.y(), c.z(), c.yaw(), 0);
        p.fallDistance = 0;
        if (fullHeal) p.setHealth(p.getMaxHealth());
        StatusService.clear(p);
        SkillService.reset(p);
    }

    /** Подземелье пройдено. */
    static void finish(MinecraftServer server) {
        RunState s = state(server);
        Layout layout = Layout.get();
        Layout.Stage last = layout.stage(layout.lastStage()).orElse(null);
        StageWorld.clearEntities(dungeon(server), true);
        s.stage = layout.lastStage() + 1;
        s.epoch++;
        s.clearAttempt();
        s.setDirty();
        long secs = (server.overworld().getGameTime() - s.runStart) / 20;
        Component time = Component.literal("Время: " + secs / 3600 + " ч " + secs / 60 % 60 + " мин · вайпов: " + s.wipes)
                .withStyle(ChatFormatting.GRAY);
        for (ServerPlayer p : onlineMembers(server)) {
            if (last != null) Rewards.topUp(p, last);
            Spirits.restore(p);
            Layout.Spot sp = layout.lobby().spawn();
            p.teleportTo(dungeon(server), sp.x(), sp.y(), sp.z(), sp.yaw(), 0);
            SystemChat.title(p, Component.literal("Подземелье пройдено").withStyle(ChatFormatting.GOLD), time);
            SystemChat.send(p, Component.literal("Система: испытание окончено. Вы дошли до конца.").withStyle(ChatFormatting.GOLD));
        }
        syncAll(server);
    }

    /** Вайп: этап заново. Опыт, уровни и вещи остаются; повторные победы наград не дают. */
    static void wipe(MinecraftServer server) {
        RunState s = state(server);
        ServerLevel level = dungeon(server);
        Layout.Stage st = currentStage(server);
        if (st == null) return;
        StageWorld.restoreBroken(level, s);
        StageWorld.clearEntities(level, false);
        s.attempt++;
        s.wipes++;
        s.epoch++;
        s.clearAttempt();
        riftTimer = -1;
        for (ServerPlayer p : onlineMembers(server)) {
            bringToCamp(p, st, true);
            SystemChat.title(p, Component.literal("Группа пала").withStyle(ChatFormatting.DARK_RED),
                    Component.literal("Этап " + st.n() + " заново · попытка " + s.attempt).withStyle(ChatFormatting.GRAY));
            p.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.MASTER, 0.5f, 0.6f);
        }
        s.setDirty();
        syncAll(server);
    }

    // ------------------------------------------------------------------ события

    /** Монстр отряда пал. Последний — отряд побеждён. */
    static void onMobDeath(DungeonMob mob) {
        CompoundTag t = StageWorld.tagOf(mob);
        if (t == null || !t.getString("kind").equals("pack")) return;
        MinecraftServer server = mob.getServer();
        RunState s = state(server);
        if (t.getInt("epoch") != s.epoch) return;
        String id = t.getString("id");
        int left = s.alive.getOrDefault(id, 1) - 1;
        s.alive.put(id, Math.max(0, left));
        s.setDirty();
        if (left > 0) return;
        s.done.add("pack:" + id);
        Layout.Stage st = currentStage(server);
        if (st != null) st.pack(id).ifPresent(p -> Rewards.claim(server, s, "pack:" + id, Rewards.packXp(st, p), "отряд разбит"));
        syncAll(server);
    }

    /** Хранитель повержен: награда, павшие возвращаются. */
    static void onBossDeath(BossEntity boss) {
        CompoundTag t = StageWorld.tagOf(boss);
        if (t == null || !t.getString("kind").equals("boss")) return;
        MinecraftServer server = boss.getServer();
        RunState s = state(server);
        if (t.getInt("epoch") != s.epoch) return;
        String id = t.getString("id");
        s.done.add("boss:" + id);
        s.setDirty();
        Layout.Stage st = currentStage(server);
        if (st != null) Rewards.claim(server, s, "boss:" + id, Rewards.bossXp(st), boss.type().name() + " повержен");
        Spirits.reviveAll(server, Balance.get(Balance.RUN).spirits().bossReviveHealth(), "Хранитель повержен — павшие возвращаются");
        syncAll(server);
    }

    // ------------------------------------------------------------------ вход и перерождение

    static void onLogin(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (server instanceof net.minecraft.gametest.framework.GameTestServer) return;
        RunState s = state(server);
        Layout layout = Layout.get();
        if (s.active(layout.lastStage())) {
            if (s.member(p.getUUID())) {
                s.names.put(p.getUUID(), p.getGameProfile().getName());
                PartyService.restore(s.party);
                Layout.Stage st = currentStage(server);
                if (s.isSpirit(p.getUUID())) {
                    p.setGameMode(GameType.SPECTATOR);
                } else {
                    if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) p.setGameMode(GameType.SURVIVAL);
                    if (!inDungeon(p) || !st.box().contains(p.position())) bringToCamp(p, st, false);
                }
                Rewards.catchUp(p, s);
            } else {
                toLobby(p);
                if (!p.hasPermissions(2)) p.setGameMode(GameType.SPECTATOR);
                SystemChat.send(p, Component.literal("Идёт прохождение (отряд: " + String.join(", ", s.names.values())
                        + "). Вы — наблюдатель.").withStyle(ChatFormatting.GRAY));
            }
        } else {
            if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) p.setGameMode(GameType.SURVIVAL);
            toLobby(p);
        }
        sync(p);
    }

    static void toLobby(ServerPlayer p) {
        Layout.Spot sp = Layout.get().lobby().spawn();
        Protect.forget(p.getUUID());
        p.teleportTo(dungeon(p.server), sp.x(), sp.y(), sp.z(), sp.yaw(), 0);
        p.fallDistance = 0;
    }

    /** После настоящей смерти (вне прохождения): в преддверие или на привал. */
    static void onRespawn(ServerPlayer p) {
        MinecraftServer server = p.server;
        if (server instanceof net.minecraft.gametest.framework.GameTestServer) return;
        if (member(p)) bringToCamp(p, currentStage(server), true);
        else toLobby(p);
    }

    // ------------------------------------------------------------------ тик

    public static void tick(MinecraftServer server) {
        if (server instanceof net.minecraft.gametest.framework.GameTestServer && !testing) return;
        ServerLevel level = dungeon(server);
        if (level == null) return;
        RunState s = state(server);
        Protect.tickRescue(server, s);
        Layout layout = Layout.get();
        if (!s.active(layout.lastStage())) {
            if (++syncTimer >= 20) {
                syncTimer = 0;
                syncAll(server);
            }
            return;
        }
        Layout.Stage st = currentStage(server);
        List<ServerPlayer> alive = aliveMembers(server);
        RunBalance cfg = Balance.get(Balance.RUN);

        // появление: кто-то из отряда подошёл
        double r2 = cfg.world().spawnRadius() * cfg.world().spawnRadius();
        for (Layout.Pack pack : st.packs()) {
            if (s.spawned.contains("pack:" + pack.id()) || !near(alive, pack.at(), r2) || !level.isLoaded(BlockPos.containing(pack.at())))
                continue;
            StageWorld.spawnPack(level, s, pack);
        }
        for (Layout.Boss b : st.bosses()) {
            if (s.spawned.contains("boss:" + b.id()) || s.done.contains("boss:" + b.id())) continue;
            if (!near(alive, Vec3.atCenterOf(b.at()), r2) || !level.isLoaded(b.at())) continue;
            StageWorld.spawnBoss(level, s, b);
        }

        // преграды: условия и «замки» боя
        Set<String> engaged = engagedBosses(level, st);
        for (Layout.Gate g : st.gates()) {
            boolean closed;
            Vec3 push = null;
            if (g.lock().isPresent()) {
                closed = engaged.contains(g.lock().get());
                push = st.boss(g.lock().get()).map(b -> Vec3.atCenterOf(b.at())).orElse(null);
            } else {
                boolean open = s.done.containsAll(g.openWhen());
                if (open && s.done.add("gate:" + g.id())) {
                    s.setDirty();
                    for (ServerPlayer p : onlineMembers(server)) {
                        SystemChat.send(p, Component.literal("Печать на пути снята.").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
                closed = !open;
            }
            StageWorld.setGate(level, g, closed, push);
        }

        // разлом
        boolean riftOpen = s.done.containsAll(st.riftWhen());
        if (StageWorld.setRift(level, st.rift(), riftOpen) && riftOpen) {
            for (ServerPlayer p : onlineMembers(server)) {
                SystemChat.title(p, Component.literal("Разлом открыт").withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal("Соберитесь у разлома всем отрядом").withStyle(ChatFormatting.GRAY));
            }
        }
        if (riftOpen) tickRift(server, s, st, alive, cfg);

        Spirits.tick(server, s, alive);
        // вайп: в сети никого живого
        List<ServerPlayer> online = onlineMembers(server);
        if (!online.isEmpty() && alive.isEmpty()) {
            if (wipeTimer < 0) wipeTimer = (int) Math.round(cfg.spirits().wipeSeconds() * 20);
            wipeTimer -= 10;
            if (wipeTimer <= 0) {
                wipeTimer = -1;
                wipe(server);
            }
        } else {
            wipeTimer = -1;
        }
        if (++syncTimer >= 2) {
            syncTimer = 0;
            syncAll(server);
        }
    }

    private static boolean near(List<ServerPlayer> players, Vec3 at, double r2) {
        for (ServerPlayer p : players) if (p.position().distanceToSqr(at) <= r2) return true;
        return false;
    }

    private static Set<String> engagedBosses(ServerLevel level, Layout.Stage st) {
        Set<String> out = new java.util.HashSet<>();
        for (BossEntity b : level.getEntitiesOfClass(BossEntity.class, st.box().aabb().inflate(16))) {
            if (b.isAlive() && b.engaged() && b.runId() != null) out.add(b.runId());
        }
        return out;
    }

    private static void tickRift(MinecraftServer server, RunState s, Layout.Stage st, List<ServerPlayer> alive, RunBalance cfg) {
        Vec3 c = st.rift().aabb().getCenter();
        double rr = cfg.world().riftRadius() * cfg.world().riftRadius();
        long atRift = alive.stream().filter(p -> p.position().distanceToSqr(c) <= rr).count();
        if (alive.isEmpty() || atRift < alive.size()) {
            if (riftTimer >= 0) {
                for (ServerPlayer p : alive) SystemChat.send(p, "Разлом затих: ждём весь отряд.");
            }
            riftTimer = -1;
            return;
        }
        if (riftTimer < 0) riftTimer = (int) Math.round(cfg.world().riftSeconds() * 20);
        if (riftTimer % 20 == 0) {
            int sec = riftTimer / 20;
            for (ServerPlayer p : onlineMembers(server)) {
                SystemChat.title(p, Component.literal(sec > 0 ? String.valueOf(sec) : "").withStyle(ChatFormatting.LIGHT_PURPLE),
                        Component.literal(st.n() < Layout.get().lastStage() ? "Разлом уносит отряд дальше" : "Последний разлом")
                                .withStyle(ChatFormatting.GRAY));
                p.playNotifySound(SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.3f, 1.4f + 0.1f * sec);
            }
        }
        riftTimer -= 10;
        if (riftTimer < 0) enterStage(server, st.n() + 1);
    }

    // ------------------------------------------------------------------ рассылка

    public static void syncAll(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sync(p);
    }

    static void sync(ServerPlayer p) {
        RunState s = state(p.server);
        Layout layout = Layout.get();
        if (!s.active(layout.lastStage()) || !inDungeon(p)) {
            Net.send(p, RunSyncPayload.EMPTY);
            return;
        }
        Layout.Stage st = currentStage(p.server);
        long packs = st.packs().stream().filter(k -> s.done.contains("pack:" + k.id())).count();
        long bosses = st.bosses().stream().filter(b -> s.done.contains("boss:" + b.id())).count();
        String goal = "Отряды " + packs + "/" + st.packs().size() + " · Хранители " + bosses + "/" + st.bosses().size();
        if (s.done.containsAll(st.riftWhen())) goal = "Разлом открыт — соберитесь у него";
        List<RunSyncPayload.Spirit> spirits = new ArrayList<>();
        s.spirits.forEach((id, pos) -> spirits.add(new RunSyncPayload.Spirit(id, s.names.getOrDefault(id, "?"),
                pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, Spirits.progress(id))));
        Net.send(p, new RunSyncPayload(st.n(), st.name(), s.attempt, goal, s.isSpirit(p.getUUID()), spirits));
    }

    // ------------------------------------------------------------------ команды и тесты

    static boolean testing;

    /** Сбросить прохождение: все — в преддверие, мир этапов — как был. */
    public static void reset(MinecraftServer server) {
        RunState s = state(server);
        ServerLevel level = dungeon(server);
        StageWorld.restoreBroken(level, s);
        StageWorld.clearEntities(level, true);
        for (ServerPlayer p : onlineMembers(server)) {
            Spirits.restore(p);
            toLobby(p);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR && !p.hasPermissions(2)) p.setGameMode(GameType.SURVIVAL);
        }
        s.resetAll();
        riftTimer = -1;
        wipeTimer = -1;
        syncAll(server);
    }

    public static void reviveForTest(ServerPlayer p, double share) {
        Spirits.revive(p, share, "проверка");
    }

    public static void wipeForTest(MinecraftServer server) {
        wipe(server);
    }

    public static void setMinPlayers(MinecraftServer server, int n) {
        RunState s = state(server);
        s.minPlayers = n;
        s.setDirty();
    }
}
