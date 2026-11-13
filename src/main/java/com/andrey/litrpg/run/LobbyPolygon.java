package com.andrey.litrpg.run;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.TrialWarden;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.core.SystemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Полигон лобби (обновление 15): Испытатель Системы в учебном бою. Вышел на арену — он вступает в бой и по очереди
 * показывает разметку приёмов (круг, кольцо, конус, полоса). Урона нет: попадание лишь сбрасывает серию. Каждый его
 * приём, от которого игрок ушёл, — +1 к серии; лучшая серия — в рекорды, 20 подряд — титул «Неуловимый».
 * Испытатель не умирает: опустился ниже трети — восстанавливается.
 */
public final class LobbyPolygon {
    public static final String TAG = "litrpg_polygon";

    private LobbyPolygon() {
    }

    /** Игрок на полигоне: текущая серия, был ли задет с начала прошлого приёма, ждём ли итога приёма. */
    private static final class Score {
        int streak;
        boolean hit;
        boolean pending;
    }

    private static final Map<UUID, Score> SCORES = new HashMap<>();

    public static boolean is(Entity e) {
        return e != null && e.getTags().contains(TAG);
    }

    public static void tick(MinecraftServer server) {
        ServerLevel level = RunService.dungeon(server);
        if (level == null) return;
        Layout.PolygonSpot spot = Layout.get().lobby().polygon().orElse(null);
        if (spot == null || !level.isLoaded(BlockPos.containing(spot.center()))) return;
        AABB around = new AABB(spot.center(), spot.center()).inflate(spot.radius() + 4, 8, spot.radius() + 4);
        List<TrialWarden> found = level.getEntitiesOfClass(TrialWarden.class, around, LobbyPolygon::is);
        for (int i = 1; i < found.size(); i++) found.get(i).discard();
        TrialWarden w = found.isEmpty() ? null : found.get(0);
        if (w == null) {
            w = ModEntities.TRIAL_WARDEN.get().create(level);
            if (w == null) return;
            w.moveTo(spot.center().x, spot.center().y, spot.center().z, 90, 0);
            w.setArena(spot.center(), spot.radius());
            w.addTag(TAG);
            w.setPersistenceRequired();
            level.addFreshEntity(w);
        }
        if (w.isAlive() && w.getHealth() < w.getMaxHealth() / 3) {
            w.setHealth(w.getMaxHealth());
            for (ServerPlayer p : players(level, spot)) {
                p.displayClientMessage(Component.literal("Испытатель восстановился — бой продолжается").withStyle(ChatFormatting.GRAY), true);
            }
        }
        // кто ушёл с арены — серия сохраняется до возвращения, но «ожидание» сбрасывается
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(spot.center()) > spot.radius() + 2) {
                Score s = SCORES.get(p.getUUID());
                if (s != null) s.pending = false;
            }
        }
    }

    private static List<ServerPlayer> players(ServerLevel level, Layout.PolygonSpot spot) {
        return level.players().stream().filter(p -> !p.isSpectator()
                && p.position().distanceTo(spot.center()) <= spot.radius() + 1).toList();
    }

    /** Испытатель начал приём: итог прошлого — увернулись ли. */
    public static void onCast(BossEntity boss) {
        if (!is(boss) || !(boss.level() instanceof ServerLevel level)) return;
        Layout.PolygonSpot spot = Layout.get().lobby().polygon().orElse(null);
        if (spot == null) return;
        for (ServerPlayer p : players(level, spot)) {
            Score s = SCORES.computeIfAbsent(p.getUUID(), k -> new Score());
            if (s.pending && !s.hit) {
                s.streak++;
                p.displayClientMessage(Component.literal("Увернулся! Серия: " + s.streak).withStyle(ChatFormatting.GREEN), true);
                if (s.streak % 5 == 0) p.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.MASTER, 0.7f, 1.4f);
                record(p, s.streak);
            }
            s.pending = true;
            s.hit = false;
        }
    }

    /** Удар Испытателя по игроку: урона нет, серия сброшена. Возвращает true — удар учебный (отменить урон). */
    public static boolean onHit(ServerPlayer p, Entity attacker) {
        if (!is(attacker)) return false;
        Score s = SCORES.computeIfAbsent(p.getUUID(), k -> new Score());
        if (!s.hit) {
            if (s.streak >= 3) {
                p.displayClientMessage(Component.literal("Попадание! Серия " + s.streak + " прервана").withStyle(ChatFormatting.RED), true);
            } else {
                p.displayClientMessage(Component.literal("Попадание!").withStyle(ChatFormatting.RED), true);
            }
            p.playNotifySound(SoundEvents.SHIELD_BLOCK, SoundSource.MASTER, 0.6f, 0.8f);
        }
        s.hit = true;
        s.streak = 0;
        return true;
    }

    private static void record(ServerPlayer p, int streak) {
        Records r = Records.get(p.server);
        Records.Profile prof = r.profile(p.getUUID(), p.getGameProfile().getName());
        if (streak > prof.polygonBest) {
            prof.polygonBest = streak;
            r.changed();
        }
        if (streak >= Titles.ELUSIVE_STREAK) Titles.grant(p, Titles.ELUSIVE);
        if (streak == Titles.ELUSIVE_STREAK) {
            SystemChat.send(p, Component.literal("Двадцать приёмов подряд мимо. Испытатель впечатлён.").withStyle(ChatFormatting.GOLD));
        }
    }

    /** Для тестов. */
    public static int streakForTest(ServerPlayer p) {
        Score s = SCORES.get(p.getUUID());
        return s == null ? 0 : s.streak;
    }
}
