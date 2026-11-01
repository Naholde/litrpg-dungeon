package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Снаряд посоха: светящийся вращающийся сгусток, всегда повёрнутый к камере, окрашенный цветом посоха. */
public class MagicBoltRenderer extends EntityRenderer<MagicBolt> {
    private static final ResourceLocation TEXTURE = LitRPG.id("textures/entity/magic_bolt.png");
    private static final RenderType TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    public MagicBoltRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    protected int getBlockLightLevel(MagicBolt entity, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(MagicBolt bolt, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        int c = 0xFF000000 | bolt.color();
        VertexConsumer vc = buf.getBuffer(TYPE);
        float spin = (bolt.tickCount + partial) * 18f;
        for (float size : new float[]{0.75f, 0.45f}) {
            ps.pushPose();
            ps.mulPose(entityRenderDispatcher.cameraOrientation());
            ps.mulPose(Axis.ZP.rotationDegrees(size > 0.5f ? spin : -spin * 1.5f));
            ps.scale(size, size, size);
            PoseStack.Pose pose = ps.last();
            int col = size > 0.5f ? c : 0xFFFFFFFF;
            vertex(vc, pose, col, -0.5f, -0.5f, 0, 1);
            vertex(vc, pose, col, 0.5f, -0.5f, 1, 1);
            vertex(vc, pose, col, 0.5f, 0.5f, 1, 0);
            vertex(vc, pose, col, -0.5f, 0.5f, 0, 0);
            ps.popPose();
        }
        super.render(bolt, yaw, partial, ps, buf, light);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, int color, float x, float y, float u, float v) {
        vc.addVertex(pose, x, y, 0).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(pose, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(MagicBolt entity) {
        return TEXTURE;
    }
}
