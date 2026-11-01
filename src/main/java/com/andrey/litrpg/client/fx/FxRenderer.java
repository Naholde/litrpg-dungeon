package com.andrey.litrpg.client.fx;

import com.andrey.litrpg.fx.FxPayload;
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
                case PILLAR -> pillar(m, vc, at, p.r1(), p.height(), p.color(), t);
                case BURST -> burst(m, vc, cam, at, p.r1(), p.color(), t);
                case SLASH -> slash(m, vc, at, p.b(), p.r1(), p.r2(), p.width(), p.color(), t);
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
    }

    private static void disc(Matrix4f m, VertexConsumer vc, Vec3 c, float r, int color, float t, boolean fill) {
        float f = fadeOut(t);
        annulus(m, vc, c, r - 0.12f, r, color, 0.95f * f, 0, 360, 2);
        if (fill) {
            annulus(m, vc, c, 0, r - 0.12f, color, 0.10f, 0, 360, 0);
            annulus(m, vc, c, 0, (r - 0.12f) * t, color, 0.30f, 0, 360, 1);
        } else {
            annulus(m, vc, c, 0, r - 0.12f, color, 0.22f * f, 0, 360);
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

    private static void pillar(Matrix4f m, VertexConsumer vc, Vec3 c, float r, float h, int color, float t) {
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
    }

    /** Вспышка: диск, повёрнутый к камере, растёт и гаснет. */
    private static void burst(Matrix4f m, VertexConsumer vc, Camera cam, Vec3 c, float r, int color, float t) {
        Vec3 look = cam.getPosition().subtract(c).normalize();
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = look.cross(up).normalize(), v = look.cross(u).normalize();
        float rr = r * (0.4f + 0.6f * t), alpha = (1 - t) * 0.8f;
        int seg = 24;
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            Vec3 p0 = c.add(u.scale(Math.cos(a0) * rr)).add(v.scale(Math.sin(a0) * rr));
            Vec3 p1 = c.add(u.scale(Math.cos(a1) * rr)).add(v.scale(Math.sin(a1) * rr));
            quad(m, vc, new float[]{(float) c.x, (float) c.y, (float) c.z, (float) c.x, (float) c.y, (float) c.z,
                    (float) p1.x, (float) p1.y, (float) p1.z, (float) p0.x, (float) p0.y, (float) p0.z}, color, alpha);
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
