package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.core.ModBlocks;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.mob.ModMobs;
import com.andrey.litrpg.shop.TraderEntity;
import com.andrey.litrpg.world.DungeonMap;
import com.andrey.litrpg.world.block.RiftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Мир этапа: появление отрядов и хранителей, преграды и разлом (приводятся к нужному виду сами), восстановление
 * разбитого после вайпа. Сущности прохождения помечены {@link #TAG}: эпоха, вид, id — устаревшие убираются при загрузке.
 */
public final class StageWorld {
    public static final String TAG = "litrpg_run";
    /** Метка «без добычи»: отряд или хранитель, за которого награда уже выдана (повтор после вайпа). */
    public static final String NO_LOOT = com.andrey.litrpg.gear.LootService.NO_LOOT;

    private StageWorld() {
    }

    // ------------------------------------------------------------------ метки сущностей

    static void tag(Entity e, RunState s, String kind, String id) {
        CompoundTag t = new CompoundTag();
        t.putInt("epoch", s.epoch);
        t.putString("kind", kind);
        t.putString("id", id);
        t.putInt("stage", s.stage);
        e.getPersistentData().put(TAG, t);
    }

    public static CompoundTag tagOf(Entity e) {
        return e.getPersistentData().contains(TAG) ? e.getPersistentData().getCompound(TAG) : null;
    }

    /** Сущность прошлой попытки или прошлого этапа — не должна вернуться в мир. */
    static boolean stale(Entity e, RunState s) {
        CompoundTag t = tagOf(e);
        if (t == null) return false;
        if (t.getString("kind").equals("trader")) return t.getInt("stage") != s.stage;
        return t.getInt("epoch") != s.epoch;
    }

    /** Убрать всех монстров и хранителей прохождения (вайп, смена этапа). */
    static void clearEntities(ServerLevel level, boolean traders) {
        List<Entity> gone = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            CompoundTag t = tagOf(e);
            if (t != null && (traders || !t.getString("kind").equals("trader"))) gone.add(e);
        }
        gone.forEach(Entity::discard);
    }

    // ------------------------------------------------------------------ появление

    static void spawnPack(ServerLevel level, RunState s, Layout.Pack pack) {
        int n = 0;
        boolean claimed = s.claims.contains("pack:" + pack.id());
        boolean first = true;
        for (Layout.Mob m : pack.mobs()) {
            if (m.minParty() > s.partySize) continue;
            var type = ModMobs.TYPES.get(m.species());
            if (type == null) {
                LitRPG.LOG.error("[LitRPG] Отряд {}: нет вида {}", pack.id(), m.species());
                continue;
            }
            DungeonMob mob = type.get().create(level);
            if (mob == null) continue;
            float yaw = level.random.nextFloat() * 360;
            mob.moveTo(m.at().getX() + 0.5, m.at().getY(), m.at().getZ() + 0.5, yaw, 0);
            mob.setYHeadRot(yaw);
            mob.setYBodyRot(yaw);
            if (first && pack.title().isPresent()) {
                mob.setTitle(pack.title().get());
                mob.setup(m.level(), MobRank.CHAMPION);
            } else {
                mob.setup(m.level(), rank(m.rank()));
            }
            first = false;
            mob.setHome(m.at());
            mob.setPersistenceRequired();
            tag(mob, s, "pack", pack.id());
            if (claimed) mob.getPersistentData().putBoolean(NO_LOOT, true);
            level.addFreshEntity(mob);
            n++;
        }
        s.spawned.add("pack:" + pack.id());
        s.alive.put(pack.id(), n);
        s.setDirty();
    }

    private static MobRank rank(String r) {
        return switch (r) {
            case "elite" -> MobRank.ELITE;
            case "champion" -> MobRank.CHAMPION;
            default -> MobRank.NORMAL;
        };
    }

    static void spawnBoss(ServerLevel level, RunState s, Layout.Boss b) {
        var type = BuiltInRegistries.ENTITY_TYPE.get(LitRPG.id(b.type()));
        Entity e = type.create(level);
        if (!(e instanceof BossEntity boss)) {
            LitRPG.LOG.error("[LitRPG] Хранитель {}: {} — не хранитель", b.id(), b.type());
            return;
        }
        Vec3 c = Vec3.atBottomCenterOf(b.at());
        boss.moveTo(c.x, c.y, c.z, 0, 0);
        boss.setArena(c, b.arena());
        b.arena2().ifPresent(a -> boss.setSecondArena(new Vec3(a.get(0), a.get(1), a.get(2)), a.get(3)));
        boss.setRun(b.id(), s.partySize);
        tag(boss, s, "boss", b.id());
        if (s.claims.contains("boss:" + b.id())) boss.getPersistentData().putBoolean(NO_LOOT, true);
        level.addFreshEntity(boss);
        s.spawned.add("boss:" + b.id());
        s.setDirty();
    }

    /** Торговец привала: один на этап, переживает вайпы. */
    static void ensureTrader(ServerLevel level, RunState s, Layout.Stage st) {
        for (Entity e : level.getAllEntities()) {
            CompoundTag t = tagOf(e);
            if (t != null && t.getString("kind").equals("trader") && t.getInt("stage") == st.n()) return;
        }
        TraderEntity tr = ModEntities.TRADER.get().create(level);
        if (tr == null) return;
        Layout.Spot sp = st.trader();
        tr.moveTo(sp.x(), sp.y(), sp.z(), sp.yaw(), 0);
        tr.setYHeadRot(sp.yaw());
        tr.setYBodyRot(sp.yaw());
        tr.setStage(st.n(), (st.fromLevel() + st.toLevel() + 1) / 2);
        tag(tr, s, "trader", "trader");
        level.addFreshEntity(tr);
    }

    // ------------------------------------------------------------------ преграды и разлом

    /** Привести преграду к виду: закрыта — блоки печати, открыта — пусто. Только если её чанки загружены. */
    static void setGate(ServerLevel level, Layout.Gate g, boolean closed, Vec3 pushTo) {
        Layout.Box b = g.box();
        BlockPos first = new BlockPos(b.x1(), b.y1(), b.z1());
        if (!level.isLoaded(first) || !level.isLoaded(new BlockPos(b.x2(), b.y2(), b.z2()))) return;
        boolean now = level.getBlockState(first).is(ModBlocks.SEALED_GATE.get());
        if (now == closed) return;
        BlockState to = closed ? ModBlocks.SEALED_GATE.get().defaultBlockState() : Blocks.AIR.defaultBlockState();
        if (closed && pushTo != null) {
            // Кто стоит в проёме — внутрь арены, а не в стену.
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, b.aabb().inflate(0.3))) {
                Vec3 dir = pushTo.subtract(p.position()).multiply(1, 0, 1).normalize();
                p.teleportTo(p.getX() + dir.x * 2.5, p.getY(), p.getZ() + dir.z * 2.5);
            }
        }
        for (BlockPos p : b.blocks()) level.setBlock(p, to, Block.UPDATE_CLIENTS);
        Vec3 c = b.aabb().getCenter();
        level.sendParticles(closed ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD, c.x, c.y, c.z, 40,
                (b.x2() - b.x1()) / 2.0 + 0.3, (b.y2() - b.y1()) / 2.0 + 0.3, (b.z2() - b.z1()) / 2.0 + 0.3, 0.05);
        level.playSound(null, BlockPos.containing(c), closed ? SoundEvents.RESPAWN_ANCHOR_SET_SPAWN : SoundEvents.BEACON_DEACTIVATE,
                SoundSource.BLOCKS, 1.2f, closed ? 0.7f : 1.2f);
    }

    /** Разлом: зажечь или погасить. */
    static boolean setRift(ServerLevel level, Layout.Box rift, boolean active) {
        boolean changed = false;
        for (BlockPos p : rift.blocks()) {
            if (!level.isLoaded(p)) return false;
            BlockState st = level.getBlockState(p);
            if (st.is(ModBlocks.RIFT.get()) && st.getValue(RiftBlock.ACTIVE) != active) {
                level.setBlock(p, st.setValue(RiftBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
                changed = true;
            }
        }
        return changed;
    }

    /** Вернуть разбитое на этапе (вайп, новый этап). */
    static void restoreBroken(ServerLevel level, RunState s) {
        DungeonMap map = DungeonMap.get();
        for (long l : s.broken) {
            BlockPos p = BlockPos.of(l);
            level.setBlock(p, map.original(p), Block.UPDATE_ALL);
        }
        s.broken.clear();
        s.setDirty();
    }
}
