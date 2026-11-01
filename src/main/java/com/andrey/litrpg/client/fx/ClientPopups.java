package com.andrey.litrpg.client.fx;

import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.PopupPayload;
import com.andrey.litrpg.player.Derived;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Всплывающие цифры. Над врагами — в мире (урон, крит, лечение союзника);
 * то, что случилось с самим игроком (урон по нему, лечение, уклонение), — у полосы здоровья.
 */
public final class ClientPopups {
    private static final int LIFE = 24;
    private static final Random RNG = new Random();

    private record World(Vec3 pos, String text, int color, float scale, long born) {
    }

    private record Hud(String text, int color, float scale, long born, int lane) {
    }

    private static final List<World> WORLD = new ArrayList<>();
    private static final List<Hud> HUD = new ArrayList<>();
    private static int lane;

    private ClientPopups() {
    }

    public static void add(PopupPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = mc.level.getGameTime();
        String num = Derived.MAX_HEALTH.format(Math.max(1, Math.round(p.amount())));
        boolean self = p.entityId() == mc.player.getId();
        if (self) {
            lane = (lane + 1) % 3;
            switch (p.kind()) {
                case TAKEN -> HUD.add(new Hud("−" + num, 0xFFFF5A5A, 1f, now, lane));
                case HEAL -> HUD.add(new Hud("+" + num, 0xFF6BE38A, 1f, now, lane));
                case DODGE -> HUD.add(new Hud("Уклонение", 0xFFA6E1FF, 1f, now, lane));
                case ABSORB -> HUD.add(new Hud("Поглощено", 0xFFFFF176, 1f, now, lane));
                default -> {
                }
            }
            return;
        }
        Entity e = mc.level.getEntity(p.entityId());
        if (e == null) return;
        Vec3 pos = e.position().add((RNG.nextDouble() - 0.5) * 0.8, e.getBbHeight() * 0.75 + RNG.nextDouble() * 0.3,
                (RNG.nextDouble() - 0.5) * 0.8);
        switch (p.kind()) {
            case DEALT -> WORLD.add(new World(pos, num, 0xFFFFFFFF, 2.0f, now));
            case CRIT -> WORLD.add(new World(pos, num + "!", 0xFFFFC857, 3.0f, now));
            case HEAL -> WORLD.add(new World(pos, "+" + num, 0xFF6BE38A, 2.0f, now));
            case ABSORB -> WORLD.add(new World(pos, "поглощено", 0xFFFFF176, 1.4f, now));
            default -> {
            }
        }
    }

    public static void renderWorld(PoseStack ps, MultiBufferSource buf, Camera cam, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        for (Iterator<World> it = WORLD.iterator(); it.hasNext(); ) {
            World w = it.next();
            float age = now - w.born + partial;
            if (age > LIFE) {
                it.remove();
                continue;
            }
            float t = age / LIFE;
            // Крит «выпрыгивает»: сначала крупнее, потом оседает.
            float pop = t < 0.15f ? 1 + (0.15f - t) * 3 : 1;
            int alpha = (int) (255 * Mth.clamp(1.4f - t * 1.4f, 0.05f, 1));
            WorldText.draw(ps, buf, cam, w.pos.add(0, t * 0.9, 0), w.text,
                    (alpha << 24) | (w.color & 0xFFFFFF), w.scale * pop, true);
        }
    }

    public static void renderHud(GuiGraphics g, Font f, int healthBarX, int healthBarY, int barWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        for (Iterator<Hud> it = HUD.iterator(); it.hasNext(); ) {
            Hud h = it.next();
            float age = now - h.born;
            if (age > LIFE * 1.5f) {
                it.remove();
                continue;
            }
            float t = age / (LIFE * 1.5f);
            int alpha = (int) (255 * Mth.clamp(1.3f - t * 1.3f, 0.05f, 1));
            int x = healthBarX + barWidth + 6 + h.lane * 4;
            int y = (int) (healthBarY - t * 22) - h.lane * 2;
            UiKit.text(g, f, h.text, x, y, (alpha << 24) | (h.color & 0xFFFFFF));
        }
    }

    public static void clear() {
        WORLD.clear();
        HUD.clear();
    }
}
