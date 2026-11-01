package com.andrey.litrpg.client.fx;

import com.andrey.litrpg.fx.FxPayload;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.joml.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Светящаяся геометрия эффектов (аддитивное смешение — ярко в тёмном подземелье).
 * Разметка удара: яркая граница + заполнение, которое растёт к моменту удара.
 */
public final class FxRenderer {
    private record Active(FxPayload p, long born) {
    }

    private static final List<Active> ACTIVE = new ArrayList<>();
    private static final int SEG = 48;
    private static final float LIFT = 0.04f;

    private FxRenderer() {
    }

    public static void add(FxPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        ACTIVE.add(new Active(p, mc.level.getGameTime()));
        if (ACTIVE.size() > 400) ACTIVE.remove(0);
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void render(PoseStack ps, MultiBufferSource buf, Camera cam, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return;
        long now = mc.level.getGameTime();
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Vec3 c = cam.getPosition();
        ps.pushPose();
        ps.translate(-c.x, -c.y, -c.z);
        Matrix4f m = ps.last().pose();
        for (Iterator<Active> it = ACTIVE.iterator(); it.hasNext(); ) {
            Active a = it.next();
            FxPayload p = a.p;
            float age = now - a.born + partial;
            if (age > p.duration()) {
                it.remove();
                continue;
            }
            float t = Mth.clamp(age / Math.max(1, p.duration()), 0, 1);
            Vec3 at = follow(p.entityA(), p.a(), partial);
            switch (p.shape()) {
                case RING -> ring(m, vc, at, Mth.lerp(t, p.r1(), p.r2()), p.width(), p.color(), 1 - t);
                case DISC -> disc(m, vc, at, p.r1(), p.color(), t, p.fill());
                case DONUT -> donut(m, vc, at, p.r1(), p.r2(), p.color(), t, p.fill());
                case CONE -> cone(m, vc, at, p.b(), p.r1(), p.r2(), p.color(), t, p.fill());
                case LINE -> line(m, vc, at, p.b(), p.width(), p.color(), t, p.fill());
                case BEAM -> beam(m, vc, cam, at.add(0, height(p.entityA()) * 0.6, 0),
                        follow(p.entityB(), p.b(), partial).add(0, height(p.entityB()) * 0.6, 0), p.width(), p.color(), t, age);
                case PILLAR -> pillar(m, vc, cam, at, p.r1(), p.height(), p.color(), t);
                case BURST -> burst(m, vc, cam, at, p.r1(), p.color(), t);
                case SLASH -> {
                    slash(m, vc, at, p.b(), p.r1(), p.r2(), p.width(), p.color(), t);
                    arc(m, vc, at, p.b(), p.r1() * 0.9f, Math.min(150, p.r2()), p.width() * 1.6f, p.color(), t);
                }
                case ORB -> orb(m, vc, cam, at.add(0, height(p.entityA()) * 0.6, 0), p.r1(), p.color(), t, age);
                case VORTEX -> vortex(m, vc, cam, at, p.r1(), p.height(), p.color(), t, age);
                case SHARDS -> shards(m, vc, cam, at, p.r1(), (int) p.r2(), p.color(), t);
                case BOLT -> bolt(m, vc, cam, at.add(0, height(p.entityA()) * 0.6, 0),
                        follow(p.entityB(), p.b(), partial).add(0, height(p.entityB()) * 0.6, 0), p.width(), p.color(), t, now);
                case ARC -> arc(m, vc, at, p.b(), p.r1(), p.r2(), p.width(), p.color(), t);
                case DOME -> dome(m, vc, at, p.r1(), p.color(), t, age);
                case SPIRAL -> spiral(m, vc, cam, at, p.r1(), p.height(), p.color(), t, age);
                case WAVE -> wave(m, vc, at, p.r2(), p.height(), p.color(), t);
                case RUNE -> rune(m, vc, at.add(0, p.entityA() >= 0 ? height(p.entityA()) + 0.7 : 0, 0),
                        p.r1(), (int) p.r2(), p.color(), t, age);
                case MARK -> {
                }
            }
        }
        ps.popPose();
        // Знаки — текстом поверх (отдельный проход: другой тип отрисовки).
        for (Active a : ACTIVE) {
            if (a.p.shape() != FxPayload.Shape.MARK) continue;
            Entity e = a.p.entityA() >= 0 ? mc.level.getEntity(a.p.entityA()) : null;
            if (e == null) continue;
            float age = now - a.born + partial;
            float bob = Mth.sin(age * 0.25f) * 0.08f;
            Vec3 pos = e.getPosition(partial).add(0, e.getBbHeight() + 0.9 + bob, 0);
            int alpha = (int) (255 * Mth.clamp((a.p.duration() - age) / 10f, 0.1f, 1));
            WorldText.draw(ps, buf, cam, pos, a.p.text(), (alpha << 24) | (a.p.color() & 0xFFFFFF), 2.6f, false);
        }
    }


    // ------------------------------------------------------------------ живые частицы

    /**
     * Раз в тик досыпать частиц по форме каждого живого эффекта. Частицы клиентские: их не шлёт сервер,
     * поэтому можно не жалеть — но шаг по времени всё равно держим редким.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || ACTIVE.isEmpty()) return;
        long now = mc.level.getGameTime();
        budget = BUDGET;
        for (Active a : ACTIVE) {
            float age = now - a.born;
            if (age > a.p.duration() || budget <= 0) continue;
            emit(mc, a.p, age, now, follow(a.p.entityA(), a.p.a(), 1));
        }
    }

    /** Чтобы экран не превращался в кашу, когда эффектов много: предел частиц на тик. */
    private static final int BUDGET = 70;
    private static int budget;


    /**
     * Частица стихии по цвету эффекта: оранжевый — пламя, голубой — снежинки, фиолетовый — пустота.
     * Так огонь выглядит огнём без единой настройки в каждом навыке: цвет у навыка уже свой.
     */
    private static ParticleOptions themed(int color) {
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        int best = 0;
        long bestD = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            int c = PALETTE[i];
            long d = sq(r - ((c >> 16) & 255)) + sq(g - ((c >> 8) & 255)) + sq(b - (c & 255));
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return THEMED[best];
    }

    private static long sq(int v) {
        return (long) v * v;
    }

    /** Цвета стихий и их частицы — порядок одинаковый. */
    private static final int[] PALETTE = {
            0xFF7A2E, 0xFF4A2E, 0x8FE3FF, 0xFFE27A, 0x9B5CFF, 0x6BE38A, 0xD8343A, 0x7FD8FF, 0xC9A055,
            0xB8C6FF, 0x7BF5A0, 0x7FB8FF, 0x2B2B3A};
    private static final ParticleOptions[] THEMED = {
            ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME, ParticleTypes.SNOWFLAKE, ParticleTypes.END_ROD,
            ParticleTypes.PORTAL, ParticleTypes.SPORE_BLOSSOM_AIR, ParticleTypes.CRIMSON_SPORE, ParticleTypes.ENCHANT,
            ParticleTypes.ASH, ParticleTypes.ELECTRIC_SPARK, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.GLOW,
            ParticleTypes.REVERSE_PORTAL};

    private static void themedAt(Minecraft mc, Vec3 at, int color, double vy) {
        if (budget-- <= 0) return;
        mc.level.addParticle(themed(color), at.x, at.y, at.z, 0, vy, 0);
    }

    private static void dustAt(Minecraft mc, Vec3 at, int color, float size, double vx, double vy, double vz) {
        if (budget-- <= 0) return;
        mc.level.addParticle(new DustParticleOptions(
                new Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f), size),
                at.x, at.y, at.z, vx, vy, vz);
    }

    private static double rnd(Minecraft mc) {
        return mc.level.random.nextDouble();
    }

    private static void emit(Minecraft mc, FxPayload p, float age, long now, Vec3 at) {
        int color = p.color();
        boolean born = age < 1;
        float t = Mth.clamp(age / Math.max(1, p.duration()), 0, 1);
        switch (p.shape()) {
            case RING -> {
                if (now % 2 != 0) return;
                float r = Mth.lerp(t, p.r1(), p.r2());
                int n = (int) Mth.clamp(r * 2.5f, 6, 16);
                for (int i = 0; i < n; i++) {
                    double ang = Math.PI * 2 * i / n + age * 0.05;
                    Vec3 q = at.add(Math.cos(ang) * r, 0.1 + rnd(mc) * 0.3, Math.sin(ang) * r);
                    dustAt(mc, q, color, 1.3f, Math.cos(ang) * 0.05, 0.05 + rnd(mc) * 0.06, Math.sin(ang) * 0.05);
                    if (i % 3 == 0) themedAt(mc, q, color, 0.04);
                }
            }
            case DISC, DONUT -> {
                if (p.fill() || now % 4 != 0) return;
                float r = p.shape() == FxPayload.Shape.DONUT ? p.r2() : p.r1();
                int n = (int) Mth.clamp(r * 1.6f, 3, 10);
                for (int i = 0; i < n; i++) {
                    double ang = Math.PI * 2 * (i + rnd(mc)) / n;
                    double rr = r * (0.2 + 0.8 * Math.sqrt(rnd(mc)));
                    Vec3 q = at.add(Math.cos(ang) * rr, 0.05, Math.sin(ang) * rr);
                    themedAt(mc, q, color, 0.05 + rnd(mc) * 0.05);
                }
                // По границе — редкая нитка искр, чтобы край зоны читался издалека.
                int edge = (int) Mth.clamp(r * 1.5f, 4, 12);
                for (int i = 0; i < edge; i++) {
                    double ang = Math.PI * 2 * i / edge + age * 0.03;
                    dustAt(mc, at.add(Math.cos(ang) * r, 0.08, Math.sin(ang) * r), color, 1.4f, 0, 0.03, 0);
                }
            }
            case CONE -> {
                if (p.fill() || now % 3 != 0) return;
                double yaw = Math.atan2(p.b().z, p.b().x);
                for (int i = 0; i < 6; i++) {
                    double ang = yaw + Math.toRadians((rnd(mc) - 0.5) * p.r2());
                    double rr = p.r1() * rnd(mc);
                    dustAt(mc, at.add(Math.cos(ang) * rr, 0.1, Math.sin(ang) * rr), color, 1.0f, 0, 0.05, 0);
                }
            }
            case LINE -> {
                if (now % 3 != 0) return;
                for (int i = 0; i < 8; i++) {
                    double k = rnd(mc);
                    Vec3 q = at.add(p.b().subtract(at).scale(k));
                    dustAt(mc, q.add((rnd(mc) - 0.5) * p.width(), 0.1, (rnd(mc) - 0.5) * p.width()), color, 1.0f,
                            0, 0.05, 0);
                }
            }
            case BEAM, BOLT -> {
                if (now % 2 != 0) return;
                Vec3 b = follow(p.entityB(), p.b(), 1).add(0, height(p.entityB()) * 0.6, 0);
                Vec3 a0 = at.add(0, height(p.entityA()) * 0.6, 0);
                for (int i = 0; i < 8; i++) {
                    Vec3 q = a0.add(b.subtract(a0).scale(rnd(mc)));
                    dustAt(mc, q, color, 1.1f, 0, 0.01, 0);
                    if (i % 3 == 0) themedAt(mc, q, color, 0.02);
                }
                if (p.shape() == FxPayload.Shape.BOLT) {
                    mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 0, 0.02, 0);
                }
            }
            case PILLAR -> {
                if (now % 2 != 0) return;
                for (int i = 0; i < 3; i++) {
                    double ang = Math.PI * 2 * rnd(mc), rr = p.r1() * Math.sqrt(rnd(mc));
                    themedAt(mc, at.add(Math.cos(ang) * rr, rnd(mc) * p.height() * 0.5, Math.sin(ang) * rr), color,
                            0.25 + rnd(mc) * 0.2);
                }
                for (int i = 0; i < 4; i++) {
                    double ang = Math.PI * 2 * rnd(mc), rr = p.r1() * Math.sqrt(rnd(mc));
                    dustAt(mc, at.add(Math.cos(ang) * rr, rnd(mc) * p.height() * 0.4, Math.sin(ang) * rr), color, 1.2f,
                            0, 0.2 + rnd(mc) * 0.2, 0);
                }
            }
            case BURST, SHARDS -> {
                if (!born) return;
                int n = p.shape() == FxPayload.Shape.BURST ? 26 : 18;
                for (int i = 0; i < n; i++) {
                    double ang = Math.PI * 2 * rnd(mc), up = (rnd(mc) - 0.35) * 1.4;
                    double sp = 0.12 + rnd(mc) * 0.2;
                    dustAt(mc, at, color, 1.3f, Math.cos(ang) * sp, up * sp, Math.sin(ang) * sp);
                }
                for (int i = 0; i < 6; i++) {
                    mc.level.addParticle(ParticleTypes.ENCHANTED_HIT, at.x, at.y, at.z,
                            (rnd(mc) - 0.5) * 0.3, rnd(mc) * 0.25, (rnd(mc) - 0.5) * 0.3);
                    themedAt(mc, at.add((rnd(mc) - 0.5) * 0.8, rnd(mc) * 0.6, (rnd(mc) - 0.5) * 0.8), color, 0.1);
                }
            }
            case SLASH, ARC -> {
                if (!born) return;
                double yaw = Math.atan2(p.b().z, p.b().x);
                for (int i = 0; i <= 12; i++) {
                    double k = i / 12.0;
                    double ang = yaw + Math.toRadians(p.r2() * (k - 0.5));
                    double lift = p.shape() == FxPayload.Shape.ARC ? Math.sin(Math.PI * k) * p.r1() * 0.5 : 0;
                    dustAt(mc, at.add(Math.cos(ang) * p.r1() * 0.85, lift, Math.sin(ang) * p.r1() * 0.85), color, 1.1f,
                            0, 0.02, 0);
                }
            }
            case ORB -> {
                if (now % 2 != 0) return;
                Vec3 c = at.add(0, height(p.entityA()) * 0.6, 0);
                for (int i = 0; i < 3; i++) {
                    double ang = Math.PI * 2 * rnd(mc), tilt = (rnd(mc) - 0.5) * Math.PI;
                    double rr = p.r1() * (0.8 + rnd(mc) * 0.5);
                    dustAt(mc, c.add(Math.cos(ang) * Math.cos(tilt) * rr, Math.sin(tilt) * rr,
                            Math.sin(ang) * Math.cos(tilt) * rr), color, 0.9f, 0, 0.01, 0);
                }
            }
            case VORTEX -> {
                if (now % 2 != 0) return;
                for (int i = 0; i < 4; i++) {
                    double ang = Math.PI * 2 * rnd(mc);
                    double rr = p.r1() * (0.4 + 0.6 * rnd(mc));
                    Vec3 q = at.add(Math.cos(ang) * rr, 0.2 + rnd(mc) * p.height(), Math.sin(ang) * rr);
                    Vec3 pull = at.add(0, 0.4, 0).subtract(q).normalize().scale(0.18);
                    dustAt(mc, q, color, 1.2f, pull.x, pull.y, pull.z);
                }
            }
            case DOME -> {
                if (now % 3 != 0) return;
                for (int i = 0; i < 4; i++) {
                    double ang = Math.PI * 2 * rnd(mc), phi = Math.PI / 2 * rnd(mc);
                    dustAt(mc, at.add(Math.cos(ang) * p.r1() * Math.cos(phi), p.r1() * Math.sin(phi),
                            Math.sin(ang) * p.r1() * Math.cos(phi)), color, 1.0f, 0, 0.01, 0);
                }
            }
            case SPIRAL -> {
                if (now % 2 != 0) return;
                Vec3 c = at;
                for (int i = 0; i < 3; i++) {
                    double k = rnd(mc);
                    double ang = k * 4.2 + age * 0.2 + i * 2.1;
                    double rr = p.r1() * (0.35 + 0.65 * Math.sin(k * Math.PI));
                    dustAt(mc, c.add(Math.cos(ang) * rr, p.height() * k, Math.sin(ang) * rr), color, 1.0f,
                            0, 0.06, 0);
                }
            }
            case WAVE -> {
                if (now % 2 != 0) return;
                float rr = p.r2() * (1 - (1 - t) * (1 - t));
                int n = (int) Math.max(6, rr * 3);
                for (int i = 0; i < n; i++) {
                    double ang = Math.PI * 2 * i / n;
                    dustAt(mc, at.add(Math.cos(ang) * rr, 0.1, Math.sin(ang) * rr), color, 1.3f,
                            Math.cos(ang) * 0.08, 0.12, Math.sin(ang) * 0.08);
                }
            }
            case RUNE -> {
                if (now % 3 != 0) return;
                Vec3 c = at.add(0, p.entityA() >= 0 ? height(p.entityA()) + 0.7 : 0, 0);
                for (int i = 0; i < 3; i++) {
                    double ang = Math.PI * 2 * rnd(mc);
                    dustAt(mc, c.add(Math.cos(ang) * p.r1(), 0.02, Math.sin(ang) * p.r1()), color, 1.0f, 0, 0.02, 0);
                }
            }
            case MARK -> {
                if (now % 5 != 0) return;
                dustAt(mc, at.add((rnd(mc) - 0.5) * 0.6, height(p.entityA()) + 0.8, (rnd(mc) - 0.5) * 0.6), color,
                        0.9f, 0, 0.02, 0);
            }
        }
    }

    // ------------------------------------------------------------------ помощники

    private static Vec3 follow(int entityId, Vec3 fallback, float partial) {
        if (entityId < 0) return fallback;
        Entity e = Minecraft.getInstance().level.getEntity(entityId);
        return e != null ? e.getPosition(partial) : fallback;
    }

    private static float height(int entityId) {
        if (entityId < 0) return 0;
        Entity e = Minecraft.getInstance().level.getEntity(entityId);
        return e != null ? e.getBbHeight() : 0;
    }

    private static void quad(Matrix4f m, VertexConsumer vc, float[] p, int color, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0, 1) * 255);
        if (a <= 2) return;
        int r = (color >> 16) & 255, g = (color >> 8) & 255, b = color & 255;
        for (int i = 0; i < 4; i++) vc.addVertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(r, g, b, a);
        // обратная сторона — чтобы было видно снизу и сзади
        for (int i = 3; i >= 0; i--) vc.addVertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(r, g, b, a);
    }

    /** Кольцо на земле между радиусами. */
    private static void annulus(Matrix4f m, VertexConsumer vc, Vec3 c, float rIn, float rOut, int color, float alpha,
                                float fromDeg, float toDeg) {
        annulus(m, vc, c, rIn, rOut, color, alpha, fromDeg, toDeg, 0);
    }

    /** {@code layer}: 0 — основа, 1 — заливка, 2 — граница. Слои чуть разнесены по высоте, чтобы не мерцали. */
    private static void annulus(Matrix4f m, VertexConsumer vc, Vec3 c, float rIn, float rOut, int color, float alpha,
                                float fromDeg, float toDeg, int layer) {
        if (rOut <= 0 || rOut <= rIn) return;
        float y = (float) c.y + LIFT + layer * 0.006f;
        int seg = Math.max(6, (int) (SEG * (toDeg - fromDeg) / 360f));
        for (int i = 0; i < seg; i++) {
            float a0 = (float) Math.toRadians(fromDeg + (toDeg - fromDeg) * i / seg);
            float a1 = (float) Math.toRadians(fromDeg + (toDeg - fromDeg) * (i + 1) / seg);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            quad(m, vc, new float[]{
                    (float) c.x + c0 * rIn, y, (float) c.z + s0 * rIn,
                    (float) c.x + c0 * rOut, y, (float) c.z + s0 * rOut,
                    (float) c.x + c1 * rOut, y, (float) c.z + s1 * rOut,
                    (float) c.x + c1 * rIn, y, (float) c.z + s1 * rIn}, color, alpha);
        }
    }

    private static float fadeOut(float t) {
        return t > 0.85f ? (1 - t) / 0.15f : 1;
    }

    private static void ring(Matrix4f m, VertexConsumer vc, Vec3 c, float r, float w, int color, float alpha) {
        annulus(m, vc, c, Math.max(0, r - w), r, color, alpha * 0.9f, 0, 360);
        annulus(m, vc, c, Math.max(0, r - w * 2.5f), Math.max(0, r - w), color, alpha * 0.25f, 0, 360);
        // Гребень волны: кольцо не только лежит на полу, но и поднимается стенкой.
        int seg = 32;
        float lip = 0.35f * alpha + 0.1f;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float x0 = (float) (c.x + Math.cos(a0) * r), z0 = (float) (c.z + Math.sin(a0) * r);
            float x1 = (float) (c.x + Math.cos(a1) * r), z1 = (float) (c.z + Math.sin(a1) * r);
            float y = (float) c.y + LIFT;
            quad(m, vc, new float[]{x0, y, z0, x1, y, z1, x1, y + lip, z1, x0, y + lip, z0}, color, alpha * 0.5f);
        }
    }

    private static void disc(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int color, float t, boolean fill) {
        float f = fadeOut(t);
        annulus(m, vc, c, r - 0.12f, r, color, 0.95f * f, 0, 360, 2);
        if (fill) {
            annulus(m, vc, c, 0, r - 0.12f, color, 0.10f, 0, 360, 0);
            annulus(m, vc, c, 0, (r - 0.12f) * t, color, 0.30f, 0, 360, 1);
        } else {
            annulus(m, vc, c, 0, r - 0.12f, color, 0.16f * f, 0, 360);
            // Вращающийся внутренний круг и невысокая стенка — чтобы зона читалась объёмом.
            annulus(m, vc, c, r * 0.55f, r * 0.62f, color, 0.5f * f, 0, 360, 1);
            int seg = 30;
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                float x0 = (float) (c.x + Math.cos(a0) * r), z0 = (float) (c.z + Math.sin(a0) * r);
                float x1 = (float) (c.x + Math.cos(a1) * r), z1 = (float) (c.z + Math.sin(a1) * r);
                float y = (float) c.y + LIFT;
                quad(m, vc, new float[]{x0, y, z0, x1, y, z1, x1, y + 0.45f, z1, x0, y + 0.45f, z0}, color, 0.18f * f);
            }
        }
    }

    private static void donut(Matrix4f m, VertexConsumer vc, Vec3 c, float in, float out, int color, float t, boolean fill) {
        float f = fadeOut(t);
        annulus(m, vc, c, out - 0.12f, out, color, 0.95f * f, 0, 360, 2);
        annulus(m, vc, c, in, in + 0.12f, color, 0.95f * f, 0, 360, 2);
        annulus(m, vc, c, in + 0.12f, out - 0.12f, color, fill ? 0.08f + 0.30f * t : 0.22f * f, 0, 360);
    }

    private static void cone(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 dir, float r, float angle, int color, float t,
                             boolean fill) {
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x));
        float from = yaw - angle / 2, to = yaw + angle / 2;
        float f = fadeOut(t);
        annulus(m, vc, c, r - 0.12f, r, color, 0.95f * f, from, to, 2);
        edge(m, vc, c, from, r, color, 0.95f * f);
        edge(m, vc, c, to, r, color, 0.95f * f);
        if (fill) {
            annulus(m, vc, c, 0, r, color, 0.10f, from, to, 0);
            annulus(m, vc, c, 0, r * t, color, 0.30f, from, to, 1);
        } else {
            annulus(m, vc, c, 0, r, color, 0.22f * f, from, to);
        }
    }

    private static void edge(Matrix4f m, VertexConsumer vc, Vec3 c, float deg, float r, int color, float alpha) {
        float a = (float) Math.toRadians(deg), ca = Mth.cos(a), sa = Mth.sin(a);
        float nx = -sa * 0.06f, nz = ca * 0.06f, y = (float) c.y + LIFT + 0.012f;
        quad(m, vc, new float[]{
                (float) c.x - nx, y, (float) c.z - nz,
                (float) c.x + ca * r - nx, y, (float) c.z + sa * r - nz,
                (float) c.x + ca * r + nx, y, (float) c.z + sa * r + nz,
                (float) c.x + nx, y, (float) c.z + nz}, color, alpha);
    }

    private static void line(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, float w, int color, float t, boolean fill) {
        Vec3 d = new Vec3(b.x - a.x, 0, b.z - a.z);
        double len = d.length();
        if (len < 1e-3) return;
        Vec3 n = new Vec3(-d.z / len, 0, d.x / len).scale(w / 2);
        float y = (float) a.y + LIFT, f = fadeOut(t);
        rect(m, vc, a, b, n, y, color, fill ? 0.10f : 0.22f * f);
        if (fill) rect(m, vc, a, a.add(d.scale(t)), n, y + 0.006f, color, 0.30f);
        Vec3 n2 = n.normalize().scale(0.07);
        rect(m, vc, a.add(n).subtract(n2), b.add(n).subtract(n2), n2, y + 0.012f, color, 0.95f * f);
        rect(m, vc, a.subtract(n).add(n2), b.subtract(n).add(n2), n2, y + 0.012f, color, 0.95f * f);
    }

    private static void rect(Matrix4f m, VertexConsumer vc, Vec3 a, Vec3 b, Vec3 n, float y, int color, float alpha) {
        quad(m, vc, new float[]{
                (float) (a.x - n.x), y, (float) (a.z - n.z),
                (float) (b.x - n.x), y, (float) (b.z - n.z),
                (float) (b.x + n.x), y, (float) (b.z + n.z),
                (float) (a.x + n.x), y, (float) (a.z + n.z)}, color, alpha);
    }

    /** Лента, повёрнутая к камере. */
    private static void beam(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 a, Vec3 b, float w, int color, float t, float age) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1e-4) return;
        Vec3 toCam = cam.getPosition().subtract(a.add(b).scale(0.5));
        Vec3 side = dir.cross(toCam).normalize();
        float pulse = 0.8f + 0.2f * Mth.sin(age * 0.6f);
        for (float k : new float[]{1f, 0.35f}) {
            Vec3 s = side.scale(w * k * pulse / 2);
            quad(m, vc, new float[]{
                    (float) (a.x - s.x), (float) (a.y - s.y), (float) (a.z - s.z),
                    (float) (b.x - s.x), (float) (b.y - s.y), (float) (b.z - s.z),
                    (float) (b.x + s.x), (float) (b.y + s.y), (float) (b.z + s.z),
                    (float) (a.x + s.x), (float) (a.y + s.y), (float) (a.z + s.z)},
                    k < 1 ? 0xFFFFFF : color, (k < 1 ? 0.7f : 0.45f) * fadeOut(t));
        }
    }

    private static void pillar(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, float h, int color, float t) {
        float alpha = (t < 0.15f ? t / 0.15f : 1) * fadeOut(t);
        int seg = 24;
        for (int layer = 0; layer < 4; layer++) {
            float y0 = (float) c.y + h * layer / 4f, y1 = (float) c.y + h * (layer + 1) / 4f;
            float al = alpha * (0.55f - layer * 0.12f);
            for (int i = 0; i < seg; i++) {
                float a0 = (float) (Math.PI * 2 * i / seg), a1 = (float) (Math.PI * 2 * (i + 1) / seg);
                quad(m, vc, new float[]{
                        (float) c.x + Mth.cos(a0) * r, y0, (float) c.z + Mth.sin(a0) * r,
                        (float) c.x + Mth.cos(a1) * r, y0, (float) c.z + Mth.sin(a1) * r,
                        (float) c.x + Mth.cos(a1) * r, y1, (float) c.z + Mth.sin(a1) * r,
                        (float) c.x + Mth.cos(a0) * r, y1, (float) c.z + Mth.sin(a0) * r}, color, al);
            }
        }
        annulus(m, vc, c, 0, r * 1.4f, color, 0.35f * alpha, 0, 360);
        spiral(m, vc, cam, c, r * 1.1f, h, color, t, t * 40);
    }

    /** Вспышка: белое ядро, цветной ореол и разлетающиеся куски — не плоский блин. */
    private static void burst(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, int color, float t) {
        float rr = r * (0.35f + 0.75f * t), alpha = (1 - t) * 0.8f;
        billboard(m, vc, cam, c, rr, color, alpha * 0.55f, 24);
        billboard(m, vc, cam, c, rr * 0.5f * (1 - t * 0.5f), 0xFFFFFF, alpha, 18);
        shards(m, vc, cam, c, r * 1.1f, 10, color, t);
        ring3(m, vc, c, new Vec3(1, 0, 0), new Vec3(0, 0, 1), rr * 1.15f, 0.1f, color, alpha * 0.7f);
    }


    // ------------------------------------------------------------------ объёмные фигуры

    /** Диск, повёрнутый к камере (искра, ядро, осколок). */
    private static void billboard(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, int color, float alpha, int seg) {
        Vec3 look = cam.getPosition().subtract(c);
        if (look.lengthSqr() < 1e-6) return;
        look = look.normalize();
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = look.cross(up).normalize(), v = look.cross(u).normalize();
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 p0 = c.add(u.scale(Math.cos(a0) * r)).add(v.scale(Math.sin(a0) * r));
            Vec3 p1 = c.add(u.scale(Math.cos(a1) * r)).add(v.scale(Math.sin(a1) * r));
            quad(m, vc, new float[]{(float) c.x, (float) c.y, (float) c.z, (float) c.x, (float) c.y, (float) c.z,
                    (float) p1.x, (float) p1.y, (float) p1.z, (float) p0.x, (float) p0.y, (float) p0.z}, color, alpha);
        }
    }

    /** Лента между двумя точками, повёрнутая к камере. */
    private static void ribbon(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 a, Vec3 b, float w, int color, float alpha) {
        Vec3 dir = b.subtract(a);
        if (dir.lengthSqr() < 1e-6) return;
        Vec3 toCam = cam.getPosition().subtract(a.add(b).scale(0.5));
        Vec3 side = dir.cross(toCam);
        if (side.lengthSqr() < 1e-6) return;
        side = side.normalize().scale(w / 2);
        quad(m, vc, new float[]{
                (float) (a.x - side.x), (float) (a.y - side.y), (float) (a.z - side.z),
                (float) (b.x - side.x), (float) (b.y - side.y), (float) (b.z - side.z),
                (float) (b.x + side.x), (float) (b.y + side.y), (float) (b.z + side.z),
                (float) (a.x + side.x), (float) (a.y + side.y), (float) (a.z + side.z)}, color, alpha);
    }

    /** Кольцо в плоскости (u, v) — из таких колец собирается объём. */
    private static void ring3(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 u, Vec3 v, float r, float w, int color, float alpha) {
        int seg = 28;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 o0 = u.scale(Math.cos(a0)).add(v.scale(Math.sin(a0)));
            Vec3 o1 = u.scale(Math.cos(a1)).add(v.scale(Math.sin(a1)));
            quad(m, vc, new float[]{
                    (float) (c.x + o0.x * (r - w)), (float) (c.y + o0.y * (r - w)), (float) (c.z + o0.z * (r - w)),
                    (float) (c.x + o0.x * r), (float) (c.y + o0.y * r), (float) (c.z + o0.z * r),
                    (float) (c.x + o1.x * r), (float) (c.y + o1.y * r), (float) (c.z + o1.z * r),
                    (float) (c.x + o1.x * (r - w)), (float) (c.y + o1.y * (r - w)), (float) (c.z + o1.z * (r - w))},
                    color, alpha);
        }
    }

    private static Vec3 spun(double angle, double tilt) {
        return new Vec3(Math.cos(angle) * Math.cos(tilt), Math.sin(tilt), Math.sin(angle) * Math.cos(tilt));
    }

    /** Шар: ядро, свечение и три обруча, которые медленно вращаются. */
    private static void orb(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, int color, float t, float age) {
        float f = (t < 0.12f ? t / 0.12f : 1) * fadeOut(t);
        float pulse = 0.92f + 0.08f * Mth.sin(age * 0.35f);
        billboard(m, vc, cam, c, r * 1.5f * pulse, color, 0.14f * f, 20);
        billboard(m, vc, cam, c, r * pulse, color, 0.34f * f, 20);
        billboard(m, vc, cam, c, r * 0.45f * pulse, 0xFFFFFF, 0.75f * f, 16);
        double sp = age * 0.08;
        for (int i = 0; i < 3; i++) {
            double a = sp + i * Math.PI / 3, tilt = Math.sin(age * 0.05 + i) * 0.9;
            Vec3 u = spun(a, tilt);
            Vec3 v = u.cross(new Vec3(0, 1, 0));
            if (v.lengthSqr() < 1e-4) v = u.cross(new Vec3(1, 0, 0));
            ring3(m, vc, c, u, v.normalize(), r * 1.35f, 0.07f, color, 0.55f * f);
        }
    }

    /** Воронка: нити закручиваются сверху вниз в центр. */
    private static void vortex(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, float h, int color, float t, float age) {
        float f = (t < 0.1f ? t / 0.1f : 1) * fadeOut(t);
        int strands = 10, steps = 7;
        for (int s = 0; s < strands; s++) {
            double base = Math.PI * 2 * s / strands + age * 0.22;
            Vec3 prev = null;
            for (int i = 0; i <= steps; i++) {
                float k = i / (float) steps;
                double a = base + k * 2.6;
                float rr = r * (1 - k) * (1 - k);
                Vec3 p = c.add(Math.cos(a) * rr, h * (1 - k) * 0.9 + 0.1, Math.sin(a) * rr);
                if (prev != null) ribbon(m, vc, cam, prev, p, 0.12f + 0.1f * (1 - k), color, (0.25f + 0.5f * k) * f);
                prev = p;
            }
        }
        annulus(m, vc, c, r * 0.9f, r, color, 0.7f * f, 0, 360, 2);
        billboard(m, vc, cam, c.add(0, 0.15, 0), 0.35f + 0.1f * Mth.sin(age * 0.5f), 0xFFFFFF, 0.5f * f, 14);
    }

    /** Осколки: куски разлетаются из центра и тают. */
    private static void shards(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, int count, int color, float t) {
        count = Math.max(4, Math.min(48, count));
        float grow = 1 - (1 - t) * (1 - t);
        for (int i = 0; i < count; i++) {
            float h1 = frac(i * 0.7548777f), h2 = frac(i * 0.5698403f), h3 = frac(i * 0.3f + 0.17f);
            double a = h1 * Math.PI * 2, up = (h2 - 0.3f) * 1.6;
            Vec3 dir = new Vec3(Math.cos(a), up, Math.sin(a)).normalize();
            Vec3 p = c.add(dir.scale(r * grow * (0.5 + h3))).add(0, 0.6 - 1.4 * t * t, 0);
            billboard(m, vc, cam, p, (0.10f + 0.12f * h3) * (1 - t * 0.7f), color, 0.85f * (1 - t), 4);
        }
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** Молния: ломаная с ветками, дрожит каждые два тика. */
    private static void bolt(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 a, Vec3 b, float w, int color, float t, long now) {
        Vec3 dir = b.subtract(a);
        double len = dir.length();
        if (len < 1e-3) return;
        Vec3 n1 = dir.cross(new Vec3(0, 1, 0));
        if (n1.lengthSqr() < 1e-4) n1 = dir.cross(new Vec3(1, 0, 0));
        n1 = n1.normalize();
        Vec3 n2 = dir.cross(n1).normalize();
        int seg = 10;
        long seed = now / 2;
        Vec3 prev = a;
        float fade = fadeOut(t);
        for (int i = 1; i <= seg; i++) {
            float k = i / (float) seg;
            double jitter = i == seg ? 0 : len * 0.12;
            double o1 = (hash(seed, i) - 0.5f) * 2 * jitter, o2 = (hash(seed, i + 77) - 0.5f) * 2 * jitter;
            Vec3 p = a.add(dir.scale(k)).add(n1.scale(o1)).add(n2.scale(o2));
            ribbon(m, vc, cam, prev, p, w * 1.8f, color, 0.5f * fade);
            ribbon(m, vc, cam, prev, p, w * 0.7f, 0xFFFFFF, 0.9f * fade);
            if (i % 4 == 0 && i < seg) {
                Vec3 br = p.add(n1.scale((hash(seed, i + 11) - 0.5f) * len * 0.35))
                        .add(n2.scale((hash(seed, i + 23) - 0.5f) * len * 0.35)).add(0, -0.2, 0);
                ribbon(m, vc, cam, p, br, w, color, 0.4f * fade);
            }
            prev = p;
        }
    }

    private static float hash(long seed, int i) {
        long h = seed * 6364136223846793005L + i * 1442695040888963407L;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1 << 24);
    }

    /** Полумесяц взмаха в воздухе: дуга идёт сверху вниз вдоль взгляда. */
    private static void arc(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 dir, float r, float arcDeg, float w, int color, float t) {
        Vec3 f = dir.lengthSqr() < 1e-6 ? new Vec3(0, 0, 1) : dir.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        float sweep = Math.min(1, t * 2.2f);
        int seg = 14;
        float half = (float) Math.toRadians(arcDeg) / 2;
        for (int i = 0; i < seg; i++) {
            float a0 = half - 2 * half * sweep * i / seg, a1 = half - 2 * half * sweep * (i + 1) / seg;
            float k = i / (float) seg;
            Vec3 d0 = f.scale(Math.cos(a0)).add(up.scale(Math.sin(a0)));
            Vec3 d1 = f.scale(Math.cos(a1)).add(up.scale(Math.sin(a1)));
            float rin0 = r - w * (0.4f + k), rin1 = r - w * (0.47f + k);
            quad(m, vc, new float[]{
                    (float) (c.x + d0.x * rin0), (float) (c.y + d0.y * rin0), (float) (c.z + d0.z * rin0),
                    (float) (c.x + d0.x * r), (float) (c.y + d0.y * r), (float) (c.z + d0.z * r),
                    (float) (c.x + d1.x * r), (float) (c.y + d1.y * r), (float) (c.z + d1.z * r),
                    (float) (c.x + d1.x * rin1), (float) (c.y + d1.y * rin1), (float) (c.z + d1.z * rin1)},
                    k > 0.7f ? 0xFFFFFF : color, 0.9f * (1 - t) * (0.25f + 0.75f * k));
        }
    }

    /** Купол: широты и меридианы. */
    private static void dome(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int color, float t, float age) {
        float f = (t < 0.1f ? t / 0.1f : 1) * fadeOut(t);
        float breathe = 1 + 0.02f * Mth.sin(age * 0.2f);
        int lat = 5, seg = 30;
        for (int j = 1; j <= lat; j++) {
            double phi = Math.PI / 2 * j / (lat + 1.0);
            float rr = (float) (r * Math.cos(phi)) * breathe, y = (float) (r * Math.sin(phi)) * breathe;
            for (int i = 0; i < seg; i++) {
                double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
                quad(m, vc, new float[]{
                        (float) (c.x + Math.cos(a0) * rr), (float) c.y + y, (float) (c.z + Math.sin(a0) * rr),
                        (float) (c.x + Math.cos(a1) * rr), (float) c.y + y, (float) (c.z + Math.sin(a1) * rr),
                        (float) (c.x + Math.cos(a1) * rr), (float) c.y + y + 0.06f, (float) (c.z + Math.sin(a1) * rr),
                        (float) (c.x + Math.cos(a0) * rr), (float) c.y + y + 0.06f, (float) (c.z + Math.sin(a0) * rr)},
                        color, 0.30f * f);
            }
        }
        for (int k = 0; k < 10; k++) {
            double a = Math.PI * 2 * k / 10 + age * 0.01;
            Vec3 prev = null;
            for (int j = 0; j <= 8; j++) {
                double phi = Math.PI / 2 * j / 8.0;
                Vec3 p = c.add(Math.cos(a) * r * Math.cos(phi) * breathe, r * Math.sin(phi) * breathe,
                        Math.sin(a) * r * Math.cos(phi) * breathe);
                if (prev != null) {
                    quad(m, vc, new float[]{(float) prev.x, (float) prev.y, (float) prev.z,
                            (float) p.x, (float) p.y, (float) p.z, (float) p.x, (float) p.y + 0.05f, (float) p.z,
                            (float) prev.x, (float) prev.y + 0.05f, (float) prev.z}, color, 0.45f * f);
                }
                prev = p;
            }
        }
        annulus(m, vc, c, r - 0.14f, r, color, 0.8f * f, 0, 360, 2);
    }

    /** Восходящая спираль: три нити поднимаются вокруг точки. */
    private static void spiral(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, float h, int color, float t, float age) {
        float f = (t < 0.12f ? t / 0.12f : 1) * fadeOut(t);
        int strands = 3, steps = 12;
        for (int s = 0; s < strands; s++) {
            Vec3 prev = null;
            for (int i = 0; i <= steps; i++) {
                float k = i / (float) steps;
                double a = Math.PI * 2 * s / strands + k * 4.2 + age * 0.18;
                float rr = r * (0.35f + 0.65f * (float) Math.sin(k * Math.PI));
                Vec3 p = c.add(Math.cos(a) * rr, h * k, Math.sin(a) * rr);
                if (prev != null) ribbon(m, vc, cam, prev, p, 0.13f, color, 0.75f * f * (1 - k * 0.5f));
                prev = p;
            }
        }
    }

    /** Ударная волна: кольцо растёт, поднимается, за ним тянется стенка. */
    private static void wave(Matrix4f m, VertexConsumer vc, Vec3 c, float r, float h, int color, float t) {
        float rr = r * (1 - (1 - t) * (1 - t));
        float y = h * t * 0.7f;
        float alpha = (1 - t) * 0.9f;
        int seg = 36;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            float x0 = (float) (c.x + Math.cos(a0) * rr), z0 = (float) (c.z + Math.sin(a0) * rr);
            float x1 = (float) (c.x + Math.cos(a1) * rr), z1 = (float) (c.z + Math.sin(a1) * rr);
            quad(m, vc, new float[]{x0, (float) c.y + LIFT, z0, x1, (float) c.y + LIFT, z1,
                    x1, (float) c.y + y, z1, x0, (float) c.y + y, z0}, color, alpha * 0.45f);
        }
        annulus(m, vc, c, Math.max(0, rr - 0.25f), rr, color, alpha, 0, 360, 2);
    }

    /** Руна: кольцо со спицами, висит в воздухе и вращается. */
    private static void rune(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int spokes, int color, float t, float age) {
        float f = (t < 0.1f ? t / 0.1f : 1) * fadeOut(t);
        int n = Math.max(3, Math.min(12, spokes));
        for (float[] layer : new float[][]{{r, 1.6f, 0.9f}, {r * 0.62f, -2.2f, 0.6f}}) {
            float rr = layer[0], al = layer[2] * f;
            double spin = Math.toRadians(age * layer[1]);
            annulus(m, vc, c, rr - 0.06f, rr, color, 0.85f * al, 0, 360, 2);
            for (int i = 0; i < n; i++) {
                double a = spin + Math.PI * 2 * i / n;
                double ca = Math.cos(a), sa = Math.sin(a);
                float nx = (float) (-sa * 0.05), nz = (float) (ca * 0.05);
                float y = (float) c.y + 0.015f;
                quad(m, vc, new float[]{
                        (float) (c.x + ca * rr * 0.35 - nx), y, (float) (c.z + sa * rr * 0.35 - nz),
                        (float) (c.x + ca * rr - nx), y, (float) (c.z + sa * rr - nz),
                        (float) (c.x + ca * rr + nx), y, (float) (c.z + sa * rr + nz),
                        (float) (c.x + ca * rr * 0.35 + nx), y, (float) (c.z + sa * rr * 0.35 + nz)},
                        color, 0.8f * al);
            }
        }
    }

    /** Полумесяц взмаха: дуга перед сущностью, проходит слева направо. */
    private static void slash(Matrix4f m, VertexConsumer vc, Vec3 c, Vec3 dir, float r, float arc, float w, int color, float t) {
        float yaw = (float) Math.toDegrees(Math.atan2(dir.z, dir.x));
        float sweep = arc * Math.min(1, t * 2.5f);
        float from = yaw - arc / 2, to = from + sweep;
        float y = (float) c.y;
        int seg = 16;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) Math.toRadians(from + (to - from) * i / seg);
            float a1 = (float) Math.toRadians(from + (to - from) * (i + 1) / seg);
            float k0 = (float) i / seg, k1 = (float) (i + 1) / seg;
            float rin0 = r - w * k0, rin1 = r - w * k1;
            quad(m, vc, new float[]{
                    (float) c.x + Mth.cos(a0) * rin0, y, (float) c.z + Mth.sin(a0) * rin0,
                    (float) c.x + Mth.cos(a0) * r, y, (float) c.z + Mth.sin(a0) * r,
                    (float) c.x + Mth.cos(a1) * r, y, (float) c.z + Mth.sin(a1) * r,
                    (float) c.x + Mth.cos(a1) * rin1, y, (float) c.z + Mth.sin(a1) * rin1},
                    color, 0.85f * (1 - t) * (0.3f + 0.7f * k1));
        }
    }
}
