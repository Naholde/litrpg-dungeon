package com.andrey.litrpg.client;

import com.andrey.litrpg.client.fx.WorldText;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.RunSyncPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/** Прохождение на клиенте: этап и цель в углу экрана, «вы пали», духи отряда в мире с полосой воскрешения. */
public final class ClientRun {
    private static RunSyncPayload data = RunSyncPayload.EMPTY;

    private ClientRun() {
    }

    public static void set(RunSyncPayload p) {
        data = p;
    }

    public static void reset() {
        data = RunSyncPayload.EMPTY;
    }

    public static int stage() {
        return data.stage();
    }

    public static boolean spirit() {
        return data.spirit();
    }

    /** Слой HUD: виден и духу (основной HUD у наблюдателя скрыт). */
    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || data.stage() <= 0) return;
        Font f = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        String title = "Этап " + data.stage() + " · " + data.stageName() + (data.attempt() > 1 ? " · попытка " + data.attempt() : "");
        int tw = Math.max(f.width(title), f.width(data.goal())) + 12;
        // Во время боя с хранителем верх экрана занят рамками и угрозой — панель этапа ниже.
        int x = w - tw - 4, y = ClientBoss.active() ? ClientBoss.frames() * 38 + 60 : 4;
        UiKit.panel(g, x, y, tw, 26);
        UiKit.text(g, f, title, x + 6, y + 4, 0xFFE8C66A);
        UiKit.text(g, f, data.goal(), x + 6, y + 15, 0xFFB8C4D6);
        if (data.spirit()) {
            // Крупное «Вы пали» показывает заголовок Системы; здесь — тихая подсказка внизу и полоса воскрешения.
            g.fill(0, 0, w, h, 0x30102030);
            UiKit.textCenter(g, f, "Вы — дух. Союзник воскресит вас: Shift рядом с местом гибели", w / 2, h - 58, 0xFFC8D0E0);
            data.spirits().stream().filter(s -> s.id().equals(mc.player.getUUID())).findFirst().ifPresent(s -> {
                if (s.progress() > 0) {
                    int bw = 140;
                    UiKit.bar(g, w / 2 - bw / 2, h - 46, bw, 5, s.progress(), 0xFF9CF09C, 0xFF3C9C3C);
                }
            });
        }
    }

    /** Духи в мире: имя над местом гибели и полоса воскрешения. */
    public static void renderWorld(PoseStack ps, MultiBufferSource buf, Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        for (RunSyncPayload.Spirit s : data.spirits()) {
            if (s.id().equals(mc.player.getUUID())) continue;
            Vec3 at = new Vec3(s.x(), s.y() + 2.2, s.z());
            if (at.distanceTo(cam.getPosition()) > 48) continue;
            WorldText.draw(ps, buf, cam, at, "✝ " + s.name(), 0xFFB8D0FF, 1.2f, true);
            String hint = s.progress() > 0 ? bar(s.progress()) : "Shift рядом — воскресить";
            WorldText.draw(ps, buf, cam, at.add(0, -0.3, 0), hint, s.progress() > 0 ? 0xFF9CF09C : 0xFF8090A0, 0.8f, true);
        }
    }

    private static String bar(float p) {
        int n = Math.round(p * 10);
        return "▮".repeat(n) + "▯".repeat(10 - n);
    }
}
