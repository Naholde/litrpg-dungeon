package com.andrey.litrpg.client.look;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;

/**
 * Облик класса поверх игрока: капюшоны, шлемы, наплечники, плащи, заплечные вещи. Каждая часть облика идёт
 * за своей частью тела (а значит, и за движениями навыков из {@code ClientAnims}), плащ развевается на бегу.
 * Вид от первого лица не трогаем — там рисуется только рука, без слоёв.
 */
public final class LookLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public LookLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing,
                       float limbSwingAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch) {
        if (p.isInvisible() || p.isSpectator()) return;
        LookModels.Entry e = LookModels.get(ClientLooks.classOf(p));
        if (e == null) return;
        boolean slim = p.getSkin().model() == PlayerSkin.Model.SLIM;
        float cape = capeAngle(p, partial);
        int overlay = LivingEntityRenderer.getOverlayCoords(p, 0);
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        draw(ps, buffers.getBuffer(RenderType.entityCutoutNoCull(e.texture())), light, overlay, model, e.model(), slim, cape);
        if (e.glow() != null) {
            draw(ps, buffers.getBuffer(RenderType.eyes(e.glow())), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    model, e.model(), slim, cape);
        }
    }

    private static void draw(PoseStack ps, VertexConsumer vc, int light, int overlay, PlayerModel<?> model, LookModel m,
                             boolean slim, float cape) {
        for (LookModel.Bone b : m.tops) {
            ModelPart part = part(model, b.name);
            if (part == null || !part.visible) continue;
            float[] p0 = rest(b.name);
            ps.pushPose();
            part.translateAndRotate(ps);
            if (slim && b.name.endsWith("_arm")) {
                // У тонких рук внешний край на пиксель ближе: сжимаем облик руки к внутреннему краю.
                float inner = b.name.startsWith("right") ? 1 / 16f : -1 / 16f;
                ps.translate(inner, 0, 0);
                ps.scale(0.8f, 1, 1);
                ps.translate(-inner, 0, 0);
            }
            ps.translate(-p0[0] / 16f, -p0[1] / 16f, -p0[2] / 16f);
            // Из координат модели (пиксели, y вверх от ступней, x отражён) — в систему частей тела игрока.
            ps.scale(1 / 16f, 1 / 16f, 1 / 16f);
            ps.scale(-1, -1, 1);
            ps.translate(0, -24, 0);
            bone(ps, vc, light, overlay, b, cape);
            ps.popPose();
        }
    }

    private static void bone(PoseStack ps, VertexConsumer vc, int light, int overlay, LookModel.Bone b, float cape) {
        ps.pushPose();
        ps.mulPose(b.local);
        if (b.cape()) {
            // Плащ отлетает назад (+z); нижние полотнища — меньше, получается изгиб.
            float a = b.name.equals("cape") ? cape : cape * 0.4f;
            ps.translate(b.pivot.x, b.pivot.y, b.pivot.z);
            ps.mulPose(Axis.XP.rotationDegrees(-a));
            ps.translate(-b.pivot.x, -b.pivot.y, -b.pivot.z);
        }
        PoseStack.Pose pose = ps.last();
        for (LookModel.Quad q : b.quads) {
            float[] v = q.pos(), uv = q.uv();
            for (int i = 0; i < 4; i++) {
                vc.addVertex(pose, v[i * 3], v[i * 3 + 1], v[i * 3 + 2]).setColor(-1).setUv(uv[i * 2], uv[i * 2 + 1])
                        .setOverlay(overlay).setLight(light).setNormal(pose, q.normal().x, q.normal().y, q.normal().z);
            }
        }
        for (LookModel.Bone c : b.children) bone(ps, vc, light, overlay, c, cape);
        ps.popPose();
    }

    private static ModelPart part(PlayerModel<?> m, String name) {
        return switch (name) {
            case "head" -> m.head;
            case "body" -> m.body;
            case "right_arm" -> m.rightArm;
            case "left_arm" -> m.leftArm;
            case "right_leg" -> m.rightLeg;
            case "left_leg" -> m.leftLeg;
            default -> null;
        };
    }

    /** Опорная точка части тела в покое (как в ванильной модели игрока). */
    private static float[] rest(String name) {
        return switch (name) {
            case "right_arm" -> new float[]{-5, 2, 0};
            case "left_arm" -> new float[]{5, 2, 0};
            case "right_leg" -> new float[]{-1.9f, 12, 0};
            case "left_leg" -> new float[]{1.9f, 12, 0};
            default -> new float[]{0, 0, 0};
        };
    }

    /** Угол плаща, как у ванильного плаща: от скорости вперёд, подъёма/падения и шага. */
    static float capeAngle(AbstractClientPlayer p, float pt) {
        double dx = Mth.lerp(pt, p.xCloakO, p.xCloak) - Mth.lerp(pt, p.xo, p.getX());
        double dy = Mth.lerp(pt, p.yCloakO, p.yCloak) - Mth.lerp(pt, p.yo, p.getY());
        double dz = Mth.lerp(pt, p.zCloakO, p.zCloak) - Mth.lerp(pt, p.zo, p.getZ());
        float yaw = Mth.rotLerp(pt, p.yBodyRotO, p.yBodyRot) * Mth.DEG_TO_RAD;
        double s = Mth.sin(yaw), c = -Mth.cos(yaw);
        float lift = Mth.clamp((float) dy * 10, -6, 32);
        float fwd = Mth.clamp((float) (dx * s + dz * c) * 100, 0, 150);
        float bob = Mth.lerp(pt, p.oBob, p.bob);
        lift += Mth.sin(Mth.lerp(pt, p.walkDistO, p.walkDist) * 6) * 32 * bob;
        return Mth.clamp(4 + fwd / 2 + lift, 0, 75);
    }
}
