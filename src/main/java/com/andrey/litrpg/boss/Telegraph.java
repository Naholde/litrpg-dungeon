package com.andrey.litrpg.boss;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Разметка ударов хранителя: сначала фигура на полу заполняется, в момент заполнения — удар по тем,
 * кто В ЭТОТ МОМЕНТ стоит внутри. Поэтому от любого удара можно уйти.
 */
public final class Telegraph {
    /** Цвет зоны «встаньте вместе» — не красный, чтобы не путать с «уйдите». */
    public static final int SOAK = 0xFFD54A;

    private Telegraph() {
    }

    /** Проверка «от всего можно уйти» (стенд фазы 12): сюда пишется каждая разметка — фигура, размер, время. */
    public static java.util.function.BiConsumer<BossEntity.Cast, String> audit;

    private static void note(BossEntity.Cast c, String shape, double size) {
        if (audit != null) audit.accept(c, shape + ";" + String.format(java.util.Locale.ROOT, "%.1f", size) + ";" + c.ticks);
    }

    private static ServerLevel level(BossEntity.Cast c) {
        return (ServerLevel) c.boss().level();
    }

    /** Круг: всем внутри — {@code units}. */
    public static void circle(BossEntity.Cast c, Vec3 center, float radius, double units) {
        circle(c, center, radius, units, null);
    }

    /** Круг с добавкой: {@code extra} — что ещё случается с попавшим (оковы, яд). */
    public static void circle(BossEntity.Cast c, Vec3 center, float radius, double units, java.util.function.Consumer<ServerPlayer> extra) {
        note(c, "circle", radius);
        Fx.disc(level(c), center, radius, c.ticks, Fx.DANGER, true);
        c.at(c.ticks, () -> {
            for (ServerPlayer p : c.boss().playersInArena()) {
                if (flat(p.position(), center) <= radius) {
                    BossDamage.hit(c.boss(), p, units);
                    if (extra != null && p.isAlive()) extra.accept(p);
                }
            }
            Fx.burst(level(c), center.add(0, 0.5, 0), radius * 0.8f, 8, Fx.DANGER);
        });
    }

    /** Кольцо: безопасно ближе {@code inner} (у хранителя) — остальным до {@code outer} достаётся. */
    public static void donut(BossEntity.Cast c, Vec3 center, float inner, float outer, double units) {
        note(c, "donut", (outer - inner) / 2.0);
        Fx.donut(level(c), center, inner, outer, c.ticks, Fx.DANGER, true);
        c.at(c.ticks, () -> {
            for (ServerPlayer p : c.boss().playersInArena()) {
                double d = flat(p.position(), center);
                if (d >= inner && d <= outer) BossDamage.hit(c.boss(), p, units);
            }
            Fx.ring(level(c), center, outer, 12, Fx.DANGER);
        });
    }

    /** Конус от хранителя в сторону {@code dir}. */
    public static void cone(BossEntity.Cast c, Vec3 origin, Vec3 dir, float radius, float angleDeg, double units) {
        Vec3 d = dir.multiply(1, 0, 1).normalize();
        // Уйти из конуса: стоящему вплотную (2 блока) — вбок, а если конус шире полукруга — за его край.
        note(c, "cone", angleDeg >= 180 ? radius - 1 : Math.min(radius - 2, 2 * Math.sin(Math.toRadians(angleDeg / 2)) + 0.5));
        Fx.cone(level(c), origin, d, radius, angleDeg, c.ticks, Fx.DANGER, true);
        double cos = Math.cos(Math.toRadians(angleDeg / 2));
        c.at(c.ticks, () -> {
            for (ServerPlayer p : c.boss().playersInArena()) {
                Vec3 to = p.position().subtract(origin).multiply(1, 0, 1);
                if (to.length() <= radius && (to.lengthSqr() < 0.5 || to.normalize().dot(d) >= cos)) {
                    BossDamage.hit(c.boss(), p, units);
                }
            }
        });
    }

    /** Полоса шириной {@code width} от {@code from} до {@code to}. */
    public static void line(BossEntity.Cast c, Vec3 from, Vec3 to, float width, double units) {
        line(c, from, to, width, units, null);
    }

    public static void line(BossEntity.Cast c, Vec3 from, Vec3 to, float width, double units, java.util.function.Consumer<ServerPlayer> extra) {
        note(c, "line", width / 2.0 + 0.3);
        Fx.line(level(c), from, to, width, c.ticks, Fx.DANGER, true);
        c.at(c.ticks, () -> {
            Vec3 ab = to.subtract(from).multiply(1, 0, 1);
            double len = ab.length();
            Vec3 n = ab.normalize();
            for (ServerPlayer p : c.boss().playersInArena()) {
                Vec3 ap = p.position().subtract(from).multiply(1, 0, 1);
                double along = ap.dot(n);
                double across = ap.subtract(n.scale(along)).length();
                if (along >= -0.5 && along <= len + 0.5 && across <= width / 2) {
                    BossDamage.hit(c.boss(), p, units);
                    if (extra != null && p.isAlive()) extra.accept(p);
                }
            }
        });
    }

    /**
     * «На разделение»: {@code totalUnits} делятся между стоящими в круге. Если внутри меньше {@code need},
     * удар полной силой бьёт ВСЕХ на арене — разметку нужно закрыть телами.
     */
    public static void soak(BossEntity.Cast c, Vec3 center, float radius, double totalUnits, int need) {
        note(c, "soak", 0);
        Fx.disc(level(c), center, radius, c.ticks, SOAK, true);
        Fx.pillar(level(c), center, radius * 0.3f, 3, c.ticks, SOAK);
        c.at(c.ticks, () -> {
            List<ServerPlayer> all = c.boss().playersInArena();
            List<ServerPlayer> inside = all.stream().filter(p -> flat(p.position(), center) <= radius).toList();
            if (inside.size() >= need) {
                for (ServerPlayer p : inside) BossDamage.hit(c.boss(), p, totalUnits / inside.size());
            } else {
                c.boss().tell(Component.literal("Ношу не разделили (" + inside.size() + " из " + need + ") — удар по всем!")
                        .withStyle(ChatFormatting.RED));
                for (ServerPlayer p : all) BossDamage.hit(c.boss(), p, totalUnits);
            }
            Fx.burst(level(c), center.add(0, 0.5, 0), radius, 10, SOAK);
        });
    }

    private static double flat(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
