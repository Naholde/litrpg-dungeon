package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.RunBalance;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.status.StatusService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Духи. Павший в прохождении не умирает, а становится духом (наблюдатель у места гибели): его не бьют, он не бьёт.
 * Воскрешает союзник — стоит рядом с духом, зажав Shift (в бою дольше). Хранитель повержен — павшие возвращаются сами.
 */
public final class Spirits {
    /** Сколько тиков союзник уже воскрешает духа. */
    private static final Map<UUID, Integer> CHANNEL = new HashMap<>();
    private static final Map<UUID, Integer> NEED = new HashMap<>();

    private Spirits() {
    }

    public static boolean isSpirit(ServerPlayer p) {
        return RunService.state(p.server).isSpirit(p.getUUID());
    }

    static float progress(UUID id) {
        int need = NEED.getOrDefault(id, 0);
        return need <= 0 ? 0 : Math.min(1f, CHANNEL.getOrDefault(id, 0) / (float) need);
    }

    /** Гибель участника прохождения: стать духом на месте. */
    static void become(ServerPlayer p) {
        RunState s = RunService.state(p.server);
        BlockPos at = p.blockPosition();
        s.spirits.put(p.getUUID(), at);
        s.setDirty();
        p.setHealth(1);
        p.fallDistance = 0;
        StatusService.clear(p);
        SkillService.reset(p);
        p.setGameMode(GameType.SPECTATOR);
        ServerLevel level = p.serverLevel();
        Fx.pillar(level, Vec3.atBottomCenterOf(at), 0.8f, 3f, 30, 0xB0C8FF);
        level.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.5f, 0.8f);
        SystemChat.title(p, Component.literal("Вы пали").withStyle(ChatFormatting.DARK_RED),
                Component.literal("Союзник воскресит вас: Shift рядом с вашим духом").withStyle(ChatFormatting.GRAY));
        for (ServerPlayer m : RunService.onlineMembers(p.server)) {
            if (m != p) SystemChat.send(m, Component.literal(p.getGameProfile().getName() + " пал. Подойдите и зажмите Shift рядом с духом.")
                    .withStyle(ChatFormatting.RED));
        }
        RunService.syncAll(p.server);
    }

    /** Вернуть к жизни на месте гибели с долей здоровья. */
    static void revive(ServerPlayer p, double share, String why) {
        RunState s = RunService.state(p.server);
        BlockPos at = s.spirits.remove(p.getUUID());
        CHANNEL.remove(p.getUUID());
        s.setDirty();
        if (at == null) return;
        p.setGameMode(GameType.SURVIVAL);
        p.setCamera(p);
        p.teleportTo(p.serverLevel(), at.getX() + 0.5, at.getY(), at.getZ() + 0.5, p.getYRot(), 0);
        p.fallDistance = 0;
        p.setHealth((float) Math.max(1, p.getMaxHealth() * share));
        p.invulnerableTime = 60;
        Fx.pillar(p.serverLevel(), p.position(), 0.8f, 3f, 20, Fx.HOLY);
        p.serverLevel().playSound(null, at, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.3f);
        SystemChat.title(p, Component.literal("Вы снова в строю").withStyle(ChatFormatting.GREEN),
                Component.literal(why).withStyle(ChatFormatting.GRAY));
        RunService.syncAll(p.server);
    }

    static void reviveAll(MinecraftServer server, double share, String why) {
        for (ServerPlayer p : RunService.onlineMembers(server)) {
            if (isSpirit(p)) revive(p, share, why);
        }
    }

    /** Бой перенесён в другой зал: духи павших на старой арене — следом (у края новой). */
    static void follow(MinecraftServer server, Vec3 from, Vec3 to, double radius) {
        RunState s = RunService.state(server);
        for (var e : Map.copyOf(s.spirits).entrySet()) {
            if (Vec3.atBottomCenterOf(e.getValue()).distanceTo(from) > 40) continue;
            BlockPos np = BlockPos.containing(to.x + (Math.random() - 0.5) * radius, to.y, to.z + (Math.random() - 0.5) * radius);
            s.spirits.put(e.getKey(), np);
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) p.teleportTo(RunService.dungeon(server), np.getX() + 0.5, np.getY() + 1, np.getZ() + 0.5, p.getYRot(), 0);
        }
        s.setDirty();
    }

    /** Снять дух без воскрешения на месте (перенос на привал, сброс). */
    static void restore(ServerPlayer p) {
        RunState s = RunService.state(p.server);
        if (s.spirits.remove(p.getUUID()) != null) s.setDirty();
        CHANNEL.remove(p.getUUID());
        if (p.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) p.setGameMode(GameType.SURVIVAL);
        p.setCamera(p);
        if (p.getHealth() < 1) p.setHealth(1);
    }

    /** Раз в полсекунды: дух не уходит далеко, светится; союзники с Shift рядом — воскрешают. */
    static void tick(MinecraftServer server, RunState s, List<ServerPlayer> alive) {
        if (s.spirits.isEmpty()) return;
        RunBalance.Spirits cfg = Balance.get(Balance.RUN).spirits();
        ServerLevel level = RunService.dungeon(server);
        for (var e : Map.copyOf(s.spirits).entrySet()) {
            UUID id = e.getKey();
            Vec3 at = Vec3.atBottomCenterOf(e.getValue());
            ServerPlayer spirit = server.getPlayerList().getPlayer(id);
            level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 0.6, at.z, 2, 0.25, 0.4, 0.25, 0.01);
            level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 1.2, at.z, 1, 0.1, 0.2, 0.1, 0.02);
            if (spirit == null) continue;
            if (spirit.getCamera() == spirit && spirit.position().distanceTo(at) > cfg.leash()) {
                spirit.teleportTo(level, at.x, at.y + 1, at.z, spirit.getYRot(), spirit.getXRot());
            }
            ServerPlayer reviver = null;
            for (ServerPlayer a : alive) {
                if (a.isShiftKeyDown() && a.position().distanceTo(at) <= cfg.reviveRadius()) {
                    reviver = a;
                    break;
                }
            }
            if (reviver == null) {
                if (CHANNEL.remove(id) != null) NEED.remove(id);
                continue;
            }
            int need = (int) Math.round((PlayerService.inCombat(reviver) ? cfg.reviveCombatSeconds() : cfg.reviveSeconds()) * 20);
            NEED.put(id, need);
            int now = CHANNEL.merge(id, 10, Integer::sum);
            String name = s.names.getOrDefault(id, "?");
            int bars = Math.min(10, now * 10 / Math.max(1, need));
            reviver.displayClientMessage(Component.literal("Воскрешение: " + name + " ")
                    .append(Component.literal("▮".repeat(bars)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal("▮".repeat(10 - bars)).withStyle(ChatFormatting.DARK_GRAY)), true);
            level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z, 3, 0.4, 0.6, 0.4, 0.02);
            if (now >= need) {
                NEED.remove(id);
                revive(spirit, cfg.reviveHealth(), "Вас воскресил " + reviver.getGameProfile().getName());
            }
        }
    }
}
