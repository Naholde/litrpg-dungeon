package com.andrey.litrpg.boss;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Приёмы, которые меняют саму арену: поднимают колонны, рассекают зал стеной, заливают пол,
 * плавят его или сужают круг. Всё временно — блоки возвращаются на место, какими были,
 * поэтому карта подземелья не портится.
 */
public final class MapMechanics {
    private MapMechanics() {
    }

    private static ServerLevel level(BossEntity b) {
        return (ServerLevel) b.level();
    }

    /** Поставить блоки и вернуть их обратно через {@code seconds} секунд. */
    private static void place(BossEntity b, List<BlockPos> where, BlockState what, int seconds) {
        ServerLevel l = level(b);
        List<BlockPos> placed = new ArrayList<>();
        List<BlockState> before = new ArrayList<>();
        for (BlockPos pos : where) {
            BlockState was = l.getBlockState(pos);
            if (!was.isAir() && !was.canBeReplaced()) continue;
            l.setBlock(pos, what, Block.UPDATE_ALL);
            placed.add(pos);
            before.add(was);
        }
        b.schedule(seconds * 20, () -> {
            for (int i = 0; i < placed.size(); i++) {
                if (l.getBlockState(placed.get(i)).is(what.getBlock())) {
                    l.setBlock(placed.get(i), before.get(i), Block.UPDATE_ALL);
                }
            }
        });
    }

    /** Заменить пол и вернуть его обратно (расплав, вода). */
    private static void floor(BossEntity b, List<BlockPos> where, BlockState what, int seconds) {
        ServerLevel l = level(b);
        List<BlockPos> changed = new ArrayList<>();
        List<BlockState> before = new ArrayList<>();
        for (BlockPos pos : where) {
            BlockState was = l.getBlockState(pos);
            if (was.isAir() || was.is(what.getBlock())) continue;
            l.setBlock(pos, what, Block.UPDATE_ALL);
            changed.add(pos);
            before.add(was);
        }
        b.schedule(seconds * 20, () -> {
            for (int i = 0; i < changed.size(); i++) {
                if (l.getBlockState(changed.get(i)).is(what.getBlock())) {
                    l.setBlock(changed.get(i), before.get(i), Block.UPDATE_ALL);
                }
            }
        });
    }

    private static BlockPos ground(BossEntity b, Vec3 at) {
        return BlockPos.containing(at.x, b.home().y, at.z);
    }

    /**
     * Колонны: вокруг арены вырастают столбы. За ними можно спрятаться от залпа и взгляда,
     * но они же мешают бежать — место боя меняется.
     */
    public static void pillars(BossEntity.Cast c, int count, int height, int seconds, BlockState block) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": колонны поднимаются — прячьтесь за ними!")
                .withStyle(ChatFormatting.GOLD));
        c.at(c.ticks, () -> {
            List<BlockPos> where = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                double a = Math.PI * 2 * i / count + b.getRandom().nextDouble() * 0.4;
                double r = b.arenaRadius() * 0.6;
                Vec3 at = b.home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                for (int y = 0; y < height; y++) where.add(ground(b, at).above(y));
                Fx.pillar(level(b), at, 1.1f, height, 30, 0xC9D6E8);
            }
            place(b, where, block, seconds);
            level(b).playSound(null, b.blockPosition(), SoundEvents.DEEPSLATE_PLACE, SoundSource.HOSTILE, 1.6f, 0.6f);
        });
    }

    /**
     * Стена поперёк зала: отряд разрезан надвое и несколько секунд дерётся порознь.
     * Лекарь по ту сторону — это сразу чувствуется.
     */
    public static void wall(BossEntity.Cast c, int height, int seconds, BlockState block, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": зал рассечён! Соберитесь по свою сторону.")
                .withStyle(ChatFormatting.GOLD));
        double a = b.getRandom().nextDouble() * Math.PI;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        double len = b.arenaRadius();
        Fx.line(level(b), b.home().add(dir.scale(-len)), b.home().add(dir.scale(len)), 1.4f, c.ticks, color, true);
        c.at(c.ticks, () -> {
            List<BlockPos> where = new ArrayList<>();
            for (double d = -len; d <= len; d += 0.9) {
                Vec3 at = b.home().add(dir.scale(d));
                for (int y = 0; y < height; y++) where.add(ground(b, at).above(y));
            }
            place(b, where, block, seconds);
            level(b).playSound(null, b.blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.HOSTILE, 1.6f, 0.5f);
        });
    }

    /**
     * Залить пол: половина зала уходит под воду. В воде медленно и больно — драться приходится
     * на оставшейся суше, а она не там, где удобно.
     */
    public static void flood(BossEntity.Cast c, int seconds, double unitsPerSecond, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": прилив заливает зал — ищите сушу!")
                .withStyle(ChatFormatting.GOLD));
        double a = b.getRandom().nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        c.at(c.ticks, () -> {
            List<BlockPos> where = new ArrayList<>();
            double r = b.arenaRadius();
            for (double x = -r; x <= r; x += 1) {
                for (double z = -r; z <= r; z += 1) {
                    Vec3 at = b.home().add(x, 0, z);
                    if (at.distanceTo(b.home()) > r) continue;
                    if (new Vec3(x, 0, z).dot(dir) < 0) continue;
                    where.add(ground(b, at));
                }
            }
            floor(b, where, Blocks.WATER.defaultBlockState(), seconds);
            Fx.disc(level(b), b.home().add(dir.scale(b.arenaRadius() * 0.5)), (float) b.arenaRadius() * 0.7f,
                    seconds * 20, color, false);
            level(b).playSound(null, b.blockPosition(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.HOSTILE, 1.6f, 0.7f);
        });
        for (int i = 1; i <= seconds; i++) {
            c.at(c.ticks + i * 20, () -> {
                for (ServerPlayer p : b.playersInArena()) {
                    if (!p.isInWater()) continue;
                    BossDamage.hit(b, p, unitsPerSecond);
                    p.setDeltaMovement(p.getDeltaMovement().multiply(0.6, 1, 0.6));
                }
            });
        }
    }

    /**
     * Расплав: сектора пола становятся раскалёнными. Стоять нельзя, а безопасные сектора
     * каждый раз другие — приходится переезжать всем отрядом.
     */
    public static void molten(BossEntity.Cast c, int sectors, int seconds, double unitsPerSecond, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": пол плавится — уходите с раскалённых секторов!")
                .withStyle(ChatFormatting.GOLD));
        double start = b.getRandom().nextDouble() * 360;
        double span = 360.0 / Math.max(2, sectors);
        c.at(c.ticks, () -> {
            List<BlockPos> where = new ArrayList<>();
            double r = b.arenaRadius();
            for (double x = -r; x <= r; x += 1) {
                for (double z = -r; z <= r; z += 1) {
                    Vec3 off = new Vec3(x, 0, z);
                    if (off.length() > r || off.length() < 2) continue;
                    double ang = ((Math.toDegrees(Math.atan2(z, x)) - start) % 360 + 360) % 360;
                    if ((int) (ang / span) % 2 != 0) continue;
                    where.add(ground(b, b.home().add(off)));
                }
            }
            floor(b, where, Blocks.MAGMA_BLOCK.defaultBlockState(), seconds);
            for (int i = 0; i < sectors; i += 2) {
                double mid = Math.toRadians(start + span * i + span / 2);
                Fx.cone(level(b), b.home(), new Vec3(Math.cos(mid), 0, Math.sin(mid)), (float) b.arenaRadius(),
                        (float) span, seconds * 20, color, false);
            }
            level(b).playSound(null, b.blockPosition(), SoundEvents.LAVA_POP, SoundSource.HOSTILE, 1.8f, 0.6f);
        });
        for (int i = 1; i <= seconds; i++) {
            c.at(c.ticks + i * 20, () -> {
                for (ServerPlayer p : b.playersInArena()) {
                    BlockPos under = p.blockPosition().below();
                    if (level(b).getBlockState(under).is(Blocks.MAGMA_BLOCK)) BossDamage.hit(b, p, unitsPerSecond);
                }
            });
        }
    }

    /**
     * Круг сужается: край зала становится смертельным и подбирается к центру. Отряд сбивается
     * в кучу, а там уже ждут круги у ног — поэтому стоять нельзя всё равно.
     */
    public static void shrink(BossEntity.Cast c, int seconds, double unitsPerSecond, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": край зала рушится — к центру!").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i <= seconds; i++) {
            final float safe = (float) (b.arenaRadius() * (1 - 0.06 * i));
            c.at(c.ticks + i * 20, () -> {
                Fx.ring(level(b), b.home(), safe, 20, color);
                Fx.donut(level(b), b.home(), safe, (float) b.arenaRadius() + 2, 20, color, false);
                for (ServerPlayer p : b.playersInArena()) {
                    if (p.position().distanceTo(b.home()) > safe) BossDamage.hit(b, p, unitsPerSecond);
                }
            });
        }
    }

    /**
     * Луч: хранитель ведёт лучом по кругу. Луч не обогнать стоя — надо бежать вместе с ним
     * или успеть проскочить рывком.
     */
    public static void sweep(BossEntity.Cast c, int seconds, double unitsPerSecond, boolean clockwise, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": луч идёт по кругу — бегите от него!")
                .withStyle(ChatFormatting.GOLD));
        double start = b.getRandom().nextDouble() * Math.PI * 2;
        int steps = seconds * 4;
        for (int i = 0; i < steps; i++) {
            final double ang = start + (clockwise ? 1 : -1) * Math.PI * 2 * i / steps;
            c.at(c.ticks + i * 5, () -> {
                Vec3 dir = new Vec3(Math.cos(ang), 0, Math.sin(ang));
                Vec3 end = b.home().add(dir.scale(b.arenaRadius()));
                Fx.bolt(level(b), b.home().add(0, 1.2, 0), end.add(0, 1, 0), 0.35f, 6, color);
                Fx.line(level(b), b.home(), end, 2.2f, 6, color, false);
                for (ServerPlayer p : b.playersInArena()) {
                    Vec3 to = p.position().subtract(b.home()).multiply(1, 0, 1);
                    if (to.length() < 1.5) continue;
                    double across = to.subtract(dir.scale(to.dot(dir))).length();
                    if (across <= 1.4 && to.dot(dir) > 0) BossDamage.hit(b, p, unitsPerSecond);
                }
            });
        }
    }

    /** Подмена мест: двоих бойцов меняет местами — строй рассыпается, надо быстро собраться. */
    public static void swapPlaces(BossEntity.Cast c, int color) {
        BossEntity b = c.boss();
        List<ServerPlayer> all = new ArrayList<>(b.playersInArena());
        if (all.size() < 2) return;
        java.util.Collections.shuffle(all, new java.util.Random(b.level().getGameTime()));
        ServerPlayer a = all.get(0), d = all.get(1);
        c.at(c.ticks, () -> {
            Vec3 pa = a.position(), pd = d.position();
            Fx.bolt(level(b), a, d, 0.2f, 10, color);
            a.teleportTo(pd.x, pd.y, pd.z);
            d.teleportTo(pa.x, pa.y, pa.z);
            Fx.burst(level(b), pa.add(0, 1, 0), 1.6f, 8, color);
            Fx.burst(level(b), pd.add(0, 1, 0), 1.6f, 8, color);
            level(b).playSound(null, b.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.4f, 0.8f);
            b.tell(Component.literal(b.type().name() + ": " + a.getGameProfile().getName() + " и "
                    + d.getGameProfile().getName() + " поменялись местами!").withStyle(ChatFormatting.GOLD));
        });
    }
}
