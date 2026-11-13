package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Паркур Башни ветров (обновление 15). Встал на стартовую площадку — пошёл отсчёт (строка действия); золотые площадки —
 * сохранения; сорвался в бассейн — назад на последнее сохранение (время идёт дальше); финиш — время, личный рекорд,
 * рекорд сервера, титулы «Акробат» (дошёл) и «Ветер в ногах» (быстрее {@link Titles#WINDRUNNER_TICKS}).
 * Ушёл из башни, взлетел или включил творческий режим — попытка снимается; финиш — только после всех сохранений.
 * Считается каждые 2 тика.
 */
public final class ParkourService {
    private ParkourService() {
    }

    /** Попытка: когда начата, какое сохранение последнее (−1 — старт). */
    private static final class Attempt {
        long start;
        int checkpoint = -1;
        long lastBar;
    }

    private static final Map<UUID, Attempt> RUNS = new HashMap<>();
    private static final double PAD = 1.6;

    public static void tick(MinecraftServer server) {
        ServerLevel level = RunService.dungeon(server);
        if (level == null) return;
        Layout.Parkour pk = Layout.get().lobby().parkour().orElse(null);
        if (pk == null) return;
        long now = server.overworld().getGameTime();
        RUNS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator()) {
                RUNS.remove(p.getUUID());
                continue;
            }
            boolean inside = pk.area().aabb().inflate(0.5).contains(p.position());
            Attempt a = RUNS.get(p.getUUID());
            // Опку получают все (решение владельца) — творческий режим и полёт снимают попытку: рекорд только честный.
            if (p.isCreative() || p.getAbilities().flying) {
                if (a != null) {
                    RUNS.remove(p.getUUID());
                    p.displayClientMessage(Component.literal("Паркур прерван: полёт не в счёт").withStyle(ChatFormatting.GRAY), true);
                }
                continue;
            }
            if (!inside) {
                if (a != null) {
                    RUNS.remove(p.getUUID());
                    p.displayClientMessage(Component.literal("Паркур прерван").withStyle(ChatFormatting.GRAY), true);
                }
                continue;
            }
            // старт: стоит на стартовой площадке — отсчёт с нуля (пока не ушёл с неё)
            if (on(p, pk.start())) {
                if (a == null) {
                    a = new Attempt();
                    RUNS.put(p.getUUID(), a);
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 0.6f, 1.6f);
                }
                a.start = now;
                a.checkpoint = -1;
                bar(p, a, now, pk);
                continue;
            }
            if (a == null) continue;
            // сорвался: в воде (или ниже бассейна) — на последнее сохранение
            if (p.isInWater() || p.getY() < pk.area().y1() + 1) {
                Vec3 to = a.checkpoint < 0 ? pk.start() : pk.checkpoints().get(a.checkpoint);
                p.teleportTo(level, to.x, to.y + 0.1, to.z, p.getYRot(), p.getXRot());
                p.fallDistance = 0;
                p.setDeltaMovement(Vec3.ZERO);
                p.hurtMarked = true;
                p.playNotifySound(SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.MASTER, 0.5f, 1.2f);
                continue;
            }
            List<Vec3> cps = pk.checkpoints();
            for (int i = a.checkpoint + 1; i < Math.min(cps.size(), a.checkpoint + 2); i++) {   // только следующее
                if (on(p, cps.get(i))) {
                    a.checkpoint = i;
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 0.7f, 1.2f + 0.1f * i);
                    level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 0.3, p.getZ(), 10, 0.5, 0.2, 0.5, 0);
                    p.displayClientMessage(Component.literal("Сохранение " + (i + 1) + "/" + cps.size() + " · "
                            + Chronicle.time(now - a.start)).withStyle(ChatFormatting.GOLD), true);
                    a.lastBar = now + 30;
                    break;
                }
            }
            if (on(p, pk.finish()) && a.checkpoint == cps.size() - 1) {    // все сохранения по порядку — иначе не в счёт
                finish(p, a, now - a.start);
                RUNS.remove(p.getUUID());
                continue;
            }
            bar(p, a, now, pk);
        }
    }

    /** Стоит на площадке с серединой {@code c} (по клетке, на её уровне). */
    private static boolean on(ServerPlayer p, Vec3 c) {
        double dx = p.getX() - c.x, dz = p.getZ() - c.z;
        return p.onGround() && Math.abs(dx) <= PAD && Math.abs(dz) <= PAD && Math.abs(p.getY() - c.y) < 0.6;
    }

    private static void bar(ServerPlayer p, Attempt a, long now, Layout.Parkour pk) {
        if (now < a.lastBar) return;
        p.displayClientMessage(Component.literal("Башня ветров: " + Chronicle.time(now - a.start) + " · сохранение "
                + (a.checkpoint + 1) + "/" + pk.checkpoints().size()).withStyle(ChatFormatting.AQUA), true);
    }

    private static void finish(ServerPlayer p, Attempt a, long ticks) {
        MinecraftServer server = p.server;
        Records r = Records.get(server);
        Records.Profile prof = r.profile(p.getUUID(), p.getGameProfile().getName());
        long serverBest = r.parkourTop(1).stream().map(Map.Entry::getValue).findFirst().orElse(Long.MAX_VALUE);
        boolean personal = prof.parkourBest == 0 || ticks < prof.parkourBest;
        if (personal) prof.parkourBest = ticks;
        r.changed();
        p.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.MASTER, 0.8f, 1.1f);
        ServerLevel level = p.serverLevel();
        level.sendParticles(ParticleTypes.FIREWORK, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.15);
        SystemChat.title(p, Component.literal("Вершина!").withStyle(ChatFormatting.GOLD),
                Component.literal(Chronicle.time(ticks) + (personal ? " · личный рекорд" : "")).withStyle(ChatFormatting.GRAY));
        if (ticks < serverBest) {
            for (ServerPlayer q : server.getPlayerList().getPlayers()) {
                SystemChat.send(q, Component.literal(p.getGameProfile().getName() + " — новый рекорд Башни ветров: "
                        + Chronicle.time(ticks)).withStyle(ChatFormatting.AQUA));
            }
        }
        Titles.grant(p, Titles.ACROBAT);
        if (ticks <= Titles.WINDRUNNER_TICKS) Titles.grant(p, Titles.WINDRUNNER);
    }

    /** Для тестов: время попытки (или −1). */
    public static long elapsedForTest(ServerPlayer p) {
        Attempt a = RUNS.get(p.getUUID());
        return a == null ? -1 : p.server.overworld().getGameTime() - a.start;
    }
}
