package com.andrey.litrpg.fx;

import com.andrey.litrpg.net.Net;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Серверная отправка эффектов. Цвета — по стихиям ({@link #FIRE}, {@link #ICE}…), чтобы по цвету было понятно,
 * что это. Разметка опасности всегда красно-оранжевая ({@link #DANGER}).
 */
public final class Fx {
    public static final int FIRE = 0xFF7A2E, ICE = 0x8FE3FF, HOLY = 0xFFE27A, SHADOW = 0x9B5CFF, NATURE = 0x6BE38A,
            BLOOD = 0xD8343A, ARCANE = 0x7FD8FF, EARTH = 0xC9A055, STORM = 0xB8C6FF, HEAL = 0x7BF5A0,
            SHIELD = 0x7FB8FF, DANGER = 0xFF4A2E;

    private static final double RANGE = 72;

    private Fx() {
    }

    // ------------------------------------------------------------------ навык игрока: удар или фон

    /** Чей сейчас эффект: 0 — мира/хранителя, {@link #SKILL} — действие навыка, {@link #PERSIST} — постоянный объект навыка. */
    private static final ThreadLocal<int[]> SCOPE = ThreadLocal.withInitial(() -> new int[1]);
    private static final int SKILL = 1, PERSIST = 2;

    /** Всё, что нарисует {@code r}, — эффекты действия навыка (удар, вспышка, луч): короткие станут сочнее. */
    public static void asSkill(Runnable r) {
        scoped(SKILL, r);
    }

    /** Всё, что нарисует {@code r}, — фон от постоянного объекта навыка (тотем, аура, зона): станет тише. */
    public static void asPersistent(Runnable r) {
        scoped(PERSIST, r);
    }

    private static void scoped(int kind, Runnable r) {
        int[] s = SCOPE.get();
        int was = s[0];
        // Постоянный объект важнее: удар внутри пульса тотема всё равно фон.
        s[0] = Math.max(was, kind);
        try {
            r.run();
        } finally {
            s[0] = was;
        }
    }

    /** Короткий удар: до этой длительности (тиков) эффект навыка рисуется ярче и живёт дольше. */
    static final int IMPACT_MAX = 14;
    /** Долгий эффект навыка (щит на союзнике, привязка, аура) — фон. */
    static final int AMBIENT_MIN = 60;

    static byte styleOf(FxPayload.Shape s, int duration, boolean fill) {
        int kind = SCOPE.get()[0];
        if (kind == PERSIST) return FxPayload.AMBIENT;
        if (kind != SKILL || s == FxPayload.Shape.MARK) return FxPayload.NORMAL;
        if (fill) return FxPayload.NORMAL;                       // своя разметка навыка («сюда упадёт») — как есть
        if (duration <= IMPACT_MAX) return FxPayload.IMPACT;
        if (duration >= AMBIENT_MIN) return FxPayload.AMBIENT;
        return FxPayload.NORMAL;
    }

    public static void send(ServerLevel level, Vec3 near, FxPayload p) {
        for (ServerPlayer pl : level.players()) {
            if (pl.position().distanceToSqr(near) < RANGE * RANGE) Net.send(pl, p);
        }
    }

    private static FxPayload of(FxPayload.Shape s, Vec3 a, Vec3 b, float r1, float r2, float width, float height,
                                int duration, int color, boolean fill, int ea, int eb, String text) {
        return new FxPayload(s, a, b, r1, r2, width, height, duration, color, fill, ea, eb, text, styleOf(s, duration, fill));
    }

    /** Волна от центра: кольцо растёт от 0 до {@code radius}. */
    public static void ring(ServerLevel l, Vec3 center, float radius, int duration, int color) {
        send(l, center, of(FxPayload.Shape.RING, center, center, 0, radius, 0.35f, 0, duration, color, false, -1, -1, ""));
    }

    /** Круг на земле. {@code fill} — заполняется к удару (разметка). */
    public static void disc(ServerLevel l, Vec3 center, float radius, int duration, int color, boolean fill) {
        send(l, center, of(FxPayload.Shape.DISC, center, center, radius, 0, 0, 0, duration, color, fill, -1, -1, ""));
    }

    /** Круг, следующий за сущностью (разметка под игроком, аура). */
    public static void discOn(ServerLevel l, Entity e, float radius, int duration, int color, boolean fill) {
        send(l, e.position(), of(FxPayload.Shape.DISC, e.position(), e.position(), radius, 0, 0, 0, duration, color, fill,
                e.getId(), -1, ""));
    }

    /** Кольцо с безопасным центром: опасно от {@code inner} до {@code outer}. */
    public static void donut(ServerLevel l, Vec3 center, float inner, float outer, int duration, int color, boolean fill) {
        send(l, center, of(FxPayload.Shape.DONUT, center, center, inner, outer, 0, 0, duration, color, fill, -1, -1, ""));
    }

    /** Сектор: из {@code origin} в сторону {@code dir}, угол в градусах. */
    public static void cone(ServerLevel l, Vec3 origin, Vec3 dir, float radius, float angleDeg, int duration, int color,
                            boolean fill) {
        send(l, origin, of(FxPayload.Shape.CONE, origin, dir.normalize(), radius, angleDeg, 0, 0, duration, color, fill,
                -1, -1, ""));
    }

    /** Полоса на земле. */
    public static void line(ServerLevel l, Vec3 from, Vec3 to, float width, int duration, int color, boolean fill) {
        send(l, from, of(FxPayload.Shape.LINE, from, to, 0, 0, width, 0, duration, color, fill, -1, -1, ""));
    }

    /** Луч между сущностями (держится за ними, пока жив эффект). */
    public static void beam(ServerLevel l, Entity from, Entity to, float width, int duration, int color) {
        send(l, from.position(), of(FxPayload.Shape.BEAM, from.position(), to.position(), 0, 0, width, 0, duration, color,
                false, from.getId(), to.getId(), ""));
    }

    /** Луч между точками. */
    public static void beam(ServerLevel l, Vec3 from, Vec3 to, float width, int duration, int color) {
        send(l, from, of(FxPayload.Shape.BEAM, from, to, 0, 0, width, 0, duration, color, false, -1, -1, ""));
    }

    public static void pillar(ServerLevel l, Vec3 base, float radius, float height, int duration, int color) {
        send(l, base, of(FxPayload.Shape.PILLAR, base, base, radius, 0, 0, height, duration, color, false, -1, -1, ""));
    }

    /** Столб, который держится на сущности (самоусиление идёт вместе с игроком, знамя — со знаменем). */
    public static void pillarOn(ServerLevel l, Entity e, float radius, float height, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.PILLAR, e.position(), e.position(), radius, 0, 0, height, duration, color,
                false, e.getId(), -1, ""));
    }

    public static void burst(ServerLevel l, Vec3 center, float radius, int duration, int color) {
        send(l, center, of(FxPayload.Shape.BURST, center, center, radius, 0, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Взмах перед сущностью на {@code radius} блоков, дугой {@code arcDeg}. */
    public static void slash(ServerLevel l, Entity e, float radius, float arcDeg, int duration, int color) {
        Vec3 dir = e.getViewVector(1).multiply(1, 0, 1).normalize();
        send(l, e.position(), of(FxPayload.Shape.SLASH, e.position().add(0, e.getBbHeight() * 0.55, 0), dir, radius, arcDeg,
                0.5f, 0, duration, color, false, -1, -1, ""));
    }


    // ------------------------------------------------------------------ объёмные фигуры

    /** Светящийся шар в точке. */
    public static void orb(ServerLevel l, Vec3 at, float radius, int duration, int color) {
        send(l, at, of(FxPayload.Shape.ORB, at, at, radius, 0, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Шар, который держится на сущности. */
    public static void orbOn(ServerLevel l, Entity e, float radius, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.ORB, e.position(), e.position(), radius, 0, 0, 0, duration, color,
                false, e.getId(), -1, ""));
    }

    /** Воронка: всё закручивается в центр ({@code height} — высота столба). */
    public static void vortex(ServerLevel l, Vec3 center, float radius, float height, int duration, int color) {
        send(l, center, of(FxPayload.Shape.VORTEX, center, center, radius, 0, 0, height, duration, color, false, -1, -1, ""));
    }

    /** Осколки: {@code count} кусков разлетаются на {@code radius}. */
    public static void shards(ServerLevel l, Vec3 center, float radius, int count, int duration, int color) {
        send(l, center, of(FxPayload.Shape.SHARDS, center, center, radius, count, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Молния между точками. */
    public static void bolt(ServerLevel l, Vec3 from, Vec3 to, float width, int duration, int color) {
        send(l, from, of(FxPayload.Shape.BOLT, from, to, 0, 0, width, 0, duration, color, false, -1, -1, ""));
    }

    /** Молния между сущностями (держится за ними). */
    public static void bolt(ServerLevel l, Entity from, Entity to, float width, int duration, int color) {
        send(l, from.position(), of(FxPayload.Shape.BOLT, from.position(), to.position(), 0, 0, width, 0, duration,
                color, false, from.getId(), to.getId(), ""));
    }

    /** Полумесяц взмаха в воздухе перед сущностью (не по полу). */
    public static void arc(ServerLevel l, Entity e, float radius, float arcDeg, int duration, int color) {
        Vec3 dir = e.getViewVector(1).multiply(1, 0, 1).normalize();
        send(l, e.position(), of(FxPayload.Shape.ARC, e.position().add(0, e.getBbHeight() * 0.6, 0), dir, radius, arcDeg,
                0.55f, 0, duration, color, false, -1, -1, ""));
    }

    /** Купол над точкой. */
    public static void dome(ServerLevel l, Vec3 center, float radius, int duration, int color) {
        send(l, center, of(FxPayload.Shape.DOME, center, center, radius, 0, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Купол, который держится на сущности. */
    public static void domeOn(ServerLevel l, Entity e, float radius, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.DOME, e.position(), e.position(), radius, 0, 0, 0, duration, color,
                false, e.getId(), -1, ""));
    }

    /** Восходящая спираль вокруг точки. */
    public static void spiral(ServerLevel l, Vec3 base, float radius, float height, int duration, int color) {
        send(l, base, of(FxPayload.Shape.SPIRAL, base, base, radius, 0, 0, height, duration, color, false, -1, -1, ""));
    }

    /** Спираль, которая держится на сущности (лечение, усиление). */
    public static void spiralOn(ServerLevel l, Entity e, float radius, float height, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.SPIRAL, e.position(), e.position(), radius, 0, 0, height, duration,
                color, false, e.getId(), -1, ""));
    }

    /** Ударная волна: кольцо растёт от 0 до {@code radius} и поднимается. */
    public static void wave(ServerLevel l, Vec3 center, float radius, float height, int duration, int color) {
        send(l, center, of(FxPayload.Shape.WAVE, center, center, 0, radius, 0, height, duration, color, false, -1, -1, ""));
    }

    /** Руна в воздухе: {@code spokes} спиц. */
    public static void rune(ServerLevel l, Vec3 at, float radius, int spokes, int duration, int color) {
        send(l, at, of(FxPayload.Shape.RUNE, at, at, radius, spokes, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Руна над сущностью. */
    public static void runeOn(ServerLevel l, Entity e, float radius, int spokes, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.RUNE, e.position(), e.position(), radius, spokes, 0, 0, duration,
                color, false, e.getId(), -1, ""));
    }

    // ------------------------------------------------------------------ частицы

    /** Цветная пыль под цвет навыка: горсть искр в точке. */
    public static void dust(ServerLevel l, Vec3 at, int color, int count, double spread, double speed, float size) {
        l.sendParticles(new DustParticleOptions(colorVec(color), size), at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** Пыль, которая меняет цвет (например, от огня к пеплу). */
    public static void dust(ServerLevel l, Vec3 at, int from, int to, int count, double spread, float size) {
        l.sendParticles(new DustColorTransitionOptions(colorVec(from), colorVec(to), size), at.x, at.y, at.z, count,
                spread, spread, spread, 0.02);
    }

    /** Любые частицы игры. */
    public static void puff(ServerLevel l, ParticleOptions type, Vec3 at, int count, double spread, double speed) {
        l.sendParticles(type, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    /** Дорожка частиц от точки к точке. */
    public static void trail(ServerLevel l, Vec3 from, Vec3 to, ParticleOptions type, int count) {
        Vec3 step = to.subtract(from).scale(1.0 / Math.max(1, count));
        for (int i = 0; i <= count; i++) {
            Vec3 p = from.add(step.scale(i));
            l.sendParticles(type, p.x, p.y, p.z, 1, 0.04, 0.04, 0.04, 0.005);
        }
    }

    /** Кольцо частиц по земле. */
    public static void ringOf(ServerLevel l, ParticleOptions type, Vec3 center, double radius, int count, double up) {
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            l.sendParticles(type, center.x + Math.cos(a) * radius, center.y + 0.1, center.z + Math.sin(a) * radius,
                    1, 0.02, 0.02, 0.02, up);
        }
    }

    private static org.joml.Vector3f colorVec(int color) {
        return new org.joml.Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f);
    }

    /** Знак над головой (символ, например «!», «✚», «⛨»). */
    public static void mark(ServerLevel l, Entity e, String symbol, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.MARK, e.position(), e.position(), 0, 0, 0, 0, duration, color, false,
                e.getId(), -1, symbol));
    }
}
