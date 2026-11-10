package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.world.DungeonMap;
import com.andrey.litrpg.world.ModWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** Подключение прохождения к событиям игры. */
public final class RunEvents {
    private RunEvents() {
    }

    public static void register(IEventBus game) {
        game.addListener(RunEvents::onAboutToStart);
        game.addListener((ServerStartedEvent e) -> onStarted(e.getServer()));
        game.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 10 == 0) RunService.tick(e.getServer());
            RunService.tickFast(e.getServer());
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.getEntity() instanceof ServerPlayer p && e.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    && RunService.inDungeon(p)) Mechanisms.onUse(p, e.getPos());
        });
        game.addListener((net.neoforged.neoforge.event.ServerChatEvent e) -> {
            if (RunService.inDungeon(e.getPlayer())) Mechanisms.onChat(e.getPlayer(), e.getRawText());
        });
        game.addListener(EventPriority.LOW, (PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) RunService.onLogin(p);
        });
        game.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !e.isEndConquered()) RunService.onRespawn(p);
        });
        game.addListener(EventPriority.HIGH, RunEvents::onDeathFirst);
        game.addListener(EventPriority.LOW, RunEvents::onDeathLast);
        game.addListener(RunEvents::onJoin);
        game.addListener(Protect::onBreak);
        game.addListener(Protect::onPlace);
        game.addListener(Protect::onExplosion);
    }

    /** Участник прохождения не умирает — становится духом. */
    private static void onDeathFirst(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.isCreative() || !RunService.inDungeon(p)) return;
        if (!RunService.member(p) || Spirits.isSpirit(p)) return;
        e.setCanceled(true);
        Spirits.become(p);
    }

    private static void onDeathLast(LivingDeathEvent e) {
        if (e.isCanceled() || e.getEntity().level().isClientSide) return;
        if (e.getEntity() instanceof DungeonMob m) RunService.onMobDeath(m);
        else if (e.getEntity() instanceof BossEntity b) RunService.onBossDeath(b);
    }

    /** Монстры и хранители прошлой попытки не возвращаются при загрузке чанка. */
    private static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || e.getLevel().dimension() != ModWorld.DUNGEON || StageWorld.tagOf(e.getEntity()) == null) return;
        if (e.loadedFromDisk() && StageWorld.stale(e.getEntity(), RunService.state(e.getLevel().getServer()))) e.setCanceled(true);
    }

    static {
        BossEntity.onRelocate = (boss, from) -> Spirits.follow(boss.getServer(), from, boss.home(), boss.arenaRadius());
    }

    private static void onStarted(MinecraftServer server) {
        GameRules r = server.getGameRules();
        r.getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        // Карта не меняется сама: лозы-подъёмы не расползаются, медь не зеленеет, лёд не тает (обновление 14).
        r.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        r.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        r.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        r.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        r.getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);
        r.getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(true, server);
        r.getRule(GameRules.RULE_ANNOUNCE_ADVANCEMENTS).set(false, server);
        r.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        r.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS).set(false, server);
        r.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        RunState s = RunService.state(server);
        if (!s.party.isEmpty()) PartyService.restore(s.party);
        LitRPG.LOG.info("[LitRPG] Прохождение: этап {}, отряд {}, попытка {}", s.stage, s.party.size(), s.attempt);
    }

    /**
     * Карта изменилась (новая версия мода) — мир подземелья пересоздаётся: старые чанки удаляются до загрузки мира,
     * прохождение сбрасывается к преддверию. Персонажи (уровни, вещи) хранятся у игроков и не теряются.
     */
    private static void onAboutToStart(ServerAboutToStartEvent e) {
        MinecraftServer server = e.getServer();
        Path root = server.getWorldPath(LevelResource.ROOT);
        Path marker = root.resolve("litrpg_map_version.txt");
        String now = DungeonMap.fileHash();
        try {
            String was = Files.exists(marker) ? Files.readString(marker, StandardCharsets.UTF_8).strip() : null;
            if (was != null && !was.equals(now)) {
                Path dim = DimensionType.getStorageFolder(ModWorld.DUNGEON, root);
                deleteTree(dim);
                Files.deleteIfExists(root.resolve("data").resolve(RunState.NAME + ".dat"));
                LitRPG.LOG.warn("[LitRPG] Карта подземелья обновилась ({} → {}): мир подземелья пересоздан, прохождение сброшено", was, now);
            }
            Files.createDirectories(root);
            Files.writeString(marker, now, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LitRPG.LOG.error("[LitRPG] Не удалось обновить мир подземелья", ex);
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }
}
