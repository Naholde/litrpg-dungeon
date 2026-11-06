package com.andrey.litrpg.boss;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Зоны на полу (фаза 13, отзыв владельца: «навыки должны зонировать, а не просто закидывать и убивать»).
 * Зона сначала видна разметкой (замах навыка), потом стоит {@code seconds} секунд и раз в секунду жжёт стоящих
 * внутри на {@code ups} «единиц». Ваншотов нет — зоны отнимают место на арене, заставляют двигаться и
 * держать хранителя там, где можно стоять. Формы: круг, полоса, сектор, кольцо, «всё, кроме круга»,
 * ползущая зона, след за существом, клетки.
 */
public final class Ground {
    private Ground() {
    }

    private static ServerLevel level(BossEntity.Cast c) {
        return (ServerLevel) c.boss().level();
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Отметить для проверки «уйти можно»: зона — не удар, из неё просто выходят. */
    private static void note(BossEntity.Cast c) {
        Telegraph.note(c, "zone", 0);
    }

    /** Круглая зона. */
    public static void circle(BossEntity.Cast c, Vec3 at, float r, int seconds, double ups, int color, Consumer<ServerPlayer> extra) {
        note(c);
        Fx.disc(level(c), at, r, c.ticks, color, true);
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> flat(p, at) <= r,
                sl -> Fx.disc(sl, at, r, 12, color, false), extra));
    }

    /** Полоса шириной {@code width} от {@code from} до {@code to}. */
    public static void line(BossEntity.Cast c, Vec3 from, Vec3 to, float width, int seconds, double ups, int color,
                            Consumer<ServerPlayer> extra) {
        note(c);
        Fx.line(level(c), from, to, width, c.ticks, color, true);
        Vec3 ab = to.subtract(from).multiply(1, 0, 1);
        double len = ab.length();
        Vec3 n = ab.normalize();
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> {
            Vec3 ap = p.subtract(from).multiply(1, 0, 1);
            double along = ap.dot(n);
            return along >= -0.3 && along <= len + 0.3 && ap.subtract(n.scale(along)).length() <= width / 2;
        }, sl -> Fx.line(sl, from, to, width, 12, color, false), extra));
    }

    /** Сектор от {@code center} в сторону {@code dir}: угол {@code angle}, до {@code radius}. */
    public static void sector(BossEntity.Cast c, Vec3 center, Vec3 dir, float radius, float angle, int seconds, double ups,
                              int color, Consumer<ServerPlayer> extra) {
        note(c);
        Vec3 d = dir.multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(angle / 2));
        Fx.cone(level(c), center, d, radius, angle, c.ticks, color, true);
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> {
            Vec3 to = p.subtract(center).multiply(1, 0, 1);
            return to.length() <= radius && (to.lengthSqr() < 0.3 || to.normalize().dot(d) >= cos);
        }, sl -> Fx.cone(sl, center, d, radius, angle, 12, color, false), extra));
    }

    /** Кольцо между {@code in} и {@code out} вокруг {@code center}. */
    public static void ring(BossEntity.Cast c, Vec3 center, float in, float out, int seconds, double ups, int color,
                            Consumer<ServerPlayer> extra) {
        note(c);
        Fx.donut(level(c), center, in, out, c.ticks, color, true);
        Fx.ring(level(c), center, in, c.ticks, 0x7BF5A0);
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> {
            double d = flat(p, center);
            return d >= in && d <= out;
        }, sl -> Fx.donut(sl, center, in, out, 12, color, false), extra));
    }

    /** Безопасен только круг {@code safe} вокруг {@code center} — всё остальное на арене жжёт. */
    public static void outside(BossEntity.Cast c, Vec3 center, float safe, int seconds, double ups, int color,
                               Consumer<ServerPlayer> extra) {
        ring(c, center, safe, (float) c.boss().arenaRadius() + 3, seconds, ups, color, extra);
    }

    /** Половина арены по ту сторону от центра в направлении {@code dir}. */
    public static void half(BossEntity.Cast c, Vec3 center, Vec3 dir, int seconds, double ups, int color, Consumer<ServerPlayer> extra) {
        sector(c, center, dir, (float) c.boss().arenaRadius() + 3, 180, seconds, ups, color, extra);
    }

    /**
     * Ползущая зона: от {@code start} к цели {@code goal} (спрашивается каждые полсекунды) со скоростью {@code speed}
     * блоков в секунду. Обходить, уводить от своих.
     */
    public static void moving(BossEntity.Cast c, Vec3 start, Supplier<Vec3> goal, float r, double speed, int seconds, double ups,
                              int color, Consumer<ServerPlayer> extra) {
        note(c);
        Vec3[] at = {start};
        Fx.disc(level(c), start, r, c.ticks, color, true);
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> flat(p, at[0]) <= r, sl -> {
            Vec3 g = goal.get();
            if (g != null) {
                Vec3 to = g.subtract(at[0]).multiply(1, 0, 1);
                double len = to.length();
                if (len > 0.2) at[0] = at[0].add(to.scale(Math.min(1, speed / 2 / len)));
            }
            Fx.disc(sl, at[0], r, 12, color, false);
            Fx.orb(sl, at[0].add(0, 0.7, 0), r * 0.35f, 10, color);
        }, extra));
    }

    /** След: {@code who} оставляет за собой круги радиуса {@code r} каждые {@code every} тиков, каждый живёт {@code life} с. */
    public static void trail(BossEntity.Cast c, Entity who, int seconds, int every, float r, int life, double ups, int color,
                             Consumer<ServerPlayer> extra) {
        note(c);
        for (int t = 0; t < seconds * 20; t += every) {
            c.at(c.ticks + t, () -> {
                if (!who.isAlive()) return;
                Vec3 at = who.position();
                c.boss().addZone(life * 20, ups, p -> flat(p, at) <= r, sl -> Fx.disc(sl, at, r, 12, color, false), extra);
            });
        }
    }

    /**
     * Клетки: квадрат {@code n}×{@code n} клеток по {@code tile} блоков вокруг {@code center}; горят клетки одного
     * цвета шахматки ({@code odd}). Вызывайте дважды со сменой цвета — получится «перескакивай».
     */
    public static void tiles(BossEntity.Cast c, Vec3 center, int n, float tile, boolean odd, int seconds, double ups, int color,
                             Consumer<ServerPlayer> extra) {
        note(c);
        double half = n * tile / 2.0;
        Vec3 corner = center.add(-half, 0, -half);
        Consumer<ServerLevel> draw = sl -> {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if (((i + j) % 2 == 1) != odd) continue;
                    Vec3 a = corner.add((i + 0.5) * tile, 0, j * tile + 0.2);
                    Vec3 b = corner.add((i + 0.5) * tile, 0, (j + 1) * tile - 0.2);
                    Fx.line(sl, a, b, tile - 0.4f, 12, color, false);
                }
            }
        };
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (((i + j) % 2 == 1) != odd) continue;
                Vec3 a = corner.add((i + 0.5) * tile, 0, j * tile + 0.2);
                Vec3 b = corner.add((i + 0.5) * tile, 0, (j + 1) * tile - 0.2);
                Fx.line(level(c), a, b, tile - 0.4f, c.ticks, color, true);
            }
        }
        c.at(c.ticks, () -> c.boss().addZone(seconds * 20, ups, p -> {
            int i = (int) Math.floor((p.x - corner.x) / tile), j = (int) Math.floor((p.z - corner.z) / tile);
            return i >= 0 && j >= 0 && i < n && j < n && (((i + j) % 2 == 1) == odd);
        }, draw, extra));
    }
}
