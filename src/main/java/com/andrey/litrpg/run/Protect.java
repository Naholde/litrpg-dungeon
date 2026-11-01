package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Защита мира подземелья и спасение. Ломать можно только блоки с тегом {@code litrpg:breakable} (тайные стены, паутина,
 * горшки) — вайп их вернёт. Ставить блоки нельзя, взрывы мир не рушат. Упавшего в пропасть или застрявшего в лаве
 * Система возвращает на последнюю твёрдую опору — ценой части здоровья.
 */
public final class Protect {
    public static final TagKey<Block> BREAKABLE = TagKey.create(Registries.BLOCK, LitRPG.id("breakable"));
    private static final Map<UUID, Vec3> SAFE = new HashMap<>();
    private static final Map<UUID, Integer> LAVA = new HashMap<>();

    private Protect() {
    }

    private static boolean guarded(net.minecraft.world.level.LevelAccessor level, net.minecraft.world.entity.player.Player p) {
        return level instanceof ServerLevel sl && sl.dimension() == com.andrey.litrpg.world.ModWorld.DUNGEON
                && (p == null || !p.isCreative());
    }

    static void onBreak(BlockEvent.BreakEvent e) {
        if (!guarded(e.getLevel(), e.getPlayer())) return;
        e.setCanceled(true);
        BlockState st = e.getState();
        if (!st.is(BREAKABLE) || !(e.getLevel() instanceof ServerLevel level)) return;
        RunState s = RunService.state(level.getServer());
        if (!s.active(Layout.get().lastStage())) return;
        BlockPos pos = e.getPos();
        s.broken.add(pos.asLong());
        s.setDirty();
        level.destroyBlock(pos, false, e.getPlayer());
    }

    static void onPlace(BlockEvent.EntityPlaceEvent e) {
        var who = e.getEntity() instanceof net.minecraft.world.entity.player.Player p ? p : null;
        if (guarded(e.getLevel(), who)) e.setCanceled(true);
    }

    static void onExplosion(ExplosionEvent.Detonate e) {
        if (e.getLevel().dimension() == com.andrey.litrpg.world.ModWorld.DUNGEON) e.getAffectedBlocks().clear();
    }

    /** Раз в полсекунды: запомнить твёрдую опору; вытащить из пропасти и лавы. */
    static void tickRescue(MinecraftServer server, RunState s) {
        ServerLevel level = RunService.dungeon(server);
        Layout layout = Layout.get();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || p.isCreative() || !p.isAlive()) continue;
            UUID id = p.getUUID();
            int stage = layout.stageAt(p.position());
            Layout.Box box = stage > 0 ? layout.stage(stage).get().box() : layout.lobby().box();
            boolean lava = p.isInLava();
            if (p.onGround() && !p.isInLiquid() && !lava) {
                BlockState below = level.getBlockState(p.getOnPos());
                if (!below.is(BlockTags.FIRE) && !below.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK) && below.isSolid()) {
                    SAFE.put(id, p.position());
                }
            }
            int inLava = lava ? LAVA.merge(id, 10, Integer::sum) : 0;
            if (!lava) LAVA.remove(id);
            if (p.getY() < box.y1() - 2 || inLava >= 30) {
                rescue(p, s, stage, lava);
                LAVA.remove(id);
            }
        }
    }

    private static void rescue(ServerPlayer p, RunState s, int stage, boolean lava) {
        Vec3 to = SAFE.get(p.getUUID());
        Layout layout = Layout.get();
        if (to == null) {
            Layout.Spot sp = stage > 0 ? layout.stage(stage).get().camp() : layout.lobby().spawn();
            to = sp.pos();
        }
        p.teleportTo(p.serverLevel(), to.x, to.y, to.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        p.clearFire();
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        float loss = (float) (p.getMaxHealth() * Balance.get(Balance.RUN).world().rescueDamage());
        boolean inRun = s.active(layout.lastStage()) && s.member(p.getUUID()) && stage > 0;
        if (inRun && p.getHealth() <= loss) {
            Spirits.become(p);
            return;
        }
        if (inRun) p.setHealth(p.getHealth() - loss);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1f, 0.8f);
        SystemChat.send(p, Component.literal(lava ? "Система вытащила вас из лавы" : "Система вытащила вас из пропасти")
                .append(Component.literal(inRun ? " (−" + Math.round(Balance.get(Balance.RUN).world().rescueDamage() * 100)
                        + "% здоровья)" : "")).withStyle(ChatFormatting.GRAY));
    }

    static void forget(UUID id) {
        SAFE.remove(id);
        LAVA.remove(id);
    }
}
