package com.andrey.litrpg.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Текст в мире, всегда повёрнутый к камере (как таблички имён). */
public final class WorldText {
    private WorldText() {
    }

    /**
     * @param seeThrough виден ли сквозь стены (цифры урона — да, подписи — нет)
     */
    public static void draw(PoseStack ps, MultiBufferSource buf, Camera cam, Vec3 pos, String text, int argb, float scale,
                            boolean seeThrough) {
        Font font = Minecraft.getInstance().font;
        Vec3 c = cam.getPosition();
        ps.pushPose();
        ps.translate(pos.x - c.x, pos.y - c.y, pos.z - c.z);
        ps.mulPose(cam.rotation());
        float s = 0.025f * scale;
        ps.scale(s, -s, s);
        float x = -font.width(text) / 2f;
        font.drawInBatch(text, x, 0, argb, true, ps.last().pose(), buf,
                seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        ps.popPose();
    }
}
