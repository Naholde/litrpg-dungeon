package com.andrey.litrpg.fx;

import com.andrey.litrpg.net.Net;
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

    public static void send(ServerLevel level, Vec3 near, FxPayload p) {
        for (ServerPlayer pl : level.players()) {
            if (pl.position().distanceToSqr(near) < RANGE * RANGE) Net.send(pl, p);
        }
    }

    private static FxPayload of(FxPayload.Shape s, Vec3 a, Vec3 b, float r1, float r2, float width, float height,
                                int duration, int color, boolean fill, int ea, int eb, String text) {
        return new FxPayload(s, a, b, r1, r2, width, height, duration, color, fill, ea, eb, text);
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

    public static void burst(ServerLevel l, Vec3 center, float radius, int duration, int color) {
        send(l, center, of(FxPayload.Shape.BURST, center, center, radius, 0, 0, 0, duration, color, false, -1, -1, ""));
    }

    /** Взмах перед сущностью на {@code radius} блоков, дугой {@code arcDeg}. */
    public static void slash(ServerLevel l, Entity e, float radius, float arcDeg, int duration, int color) {
        Vec3 dir = e.getViewVector(1).multiply(1, 0, 1).normalize();
        send(l, e.position(), of(FxPayload.Shape.SLASH, e.position().add(0, e.getBbHeight() * 0.55, 0), dir, radius, arcDeg,
                0.5f, 0, duration, color, false, -1, -1, ""));
    }

    /** Знак над головой (символ, например «!», «✚», «⛨»). */
    public static void mark(ServerLevel l, Entity e, String symbol, int duration, int color) {
        send(l, e.position(), of(FxPayload.Shape.MARK, e.position(), e.position(), 0, 0, 0, 0, duration, color, false,
                e.getId(), -1, symbol));
    }
}
