package com.andrey.litrpg.client;

import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Свой HUD вместо сердец, голода и ванильного опыта: здоровье числом и полосой, блок уровня.
 * Панель навыков добавится в фазе навыков, рамки группы — с отрядом.
 */
public final class HudOverlay {
    /** «Призрак» полосы здоровья: после удара светлая часть тает — видно, сколько сняли. */
    private static float ghost = 1f;

    private HudOverlay() {
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.options.hideGui || p.isSpectator()) return;
        Font f = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        if (!p.isCreative()) health(g, f, p, w, h);
        levelBlock(g, f, mc);
    }

    private static void health(GuiGraphics g, Font f, Player p, int w, int h) {
        float max = p.getMaxHealth(), hp = Math.min(p.getHealth(), max), abs = p.getAbsorptionAmount();
        float ratio = max > 0 ? hp / max : 0;
        ghost = ratio > ghost ? ratio : Math.max(ratio, ghost - 0.01f);
        int bw = 182, bh = 10, x = (w - bw) / 2, y = h - 41;
        g.fill(x - 1, y - 1, x + bw + 1, y + bh + 1, 0xA0000000);
        g.fill(x, y, x + bw, y + bh, 0xFF1A0B0E);
        int inner = bw - 2;
        int gw = Math.round(ghost * inner), fw = Math.round(ratio * inner);
        if (gw > fw) g.fill(x + 1 + fw, y + 1, x + 1 + gw, y + bh - 1, 0xFFFFE0B0);
        boolean low = ratio < 0.3f;
        int top = low ? 0xFFFF8A80 : UiKit.HEALTH_TOP, bottom = low ? 0xFFB71C1C : UiKit.HEALTH;
        if (fw > 0) g.fillGradient(x + 1, y + 1, x + 1 + fw, y + bh - 1, top, bottom);
        if (abs > 0) {
            int aw = Math.min(inner, Math.round(abs / max * inner));
            g.fillGradient(x + 1, y + 1, x + 1 + aw, y + 3, 0xFFFFF176, 0xFFFBC02D);
        }
        UiKit.border(g, x, y, bw, bh, low && (p.tickCount / 8) % 2 == 0 ? 0xFFFF6B6B : 0xFF7A2A30);
        String text = Derived.MAX_HEALTH.format(Math.ceil(hp)) + " / " + Derived.MAX_HEALTH.format(max)
                + (abs > 0 ? "  +" + Math.round(abs) : "");
        UiKit.textCenter(g, f, text, w / 2, y + 1, 0xFFFFFFFF);
    }

    private static void levelBlock(GuiGraphics g, Font f, Minecraft mc) {
        if (!ClientPlayerData.received()) return;
        int x = 4, y = 4, bw = 124, bh = 31;
        UiKit.panel(g, x, y, bw, bh);
        Role role = ClientPlayerData.role();
        int lv = ClientPlayerData.level();
        UiKit.text(g, f, "Ур. " + lv, x + 5, y + 5, UiKit.GOLD);
        int lvw = f.width("Ур. " + lv);
        UiKit.text(g, f, role.icon + " " + role.title, x + 10 + lvw, y + 5, role.color);
        long need = ClientPlayerData.xpToNext();
        double ratio = need > 0 ? (double) ClientPlayerData.xp() / need : 1;
        UiKit.bar(g, x + 5, y + 16, bw - 10, 4, ratio, 0xFFC6B5FF, UiKit.XP);
        int free = ClientPlayerData.freeStatPoints();
        String info = need > 0 ? Math.round(ratio * 100) + "% опыта" : "Макс. уровень";
        UiKit.text(g, f, info, x + 5, y + 21, UiKit.MUTED);
        if (free > 0) {
            int c = (mc.player.tickCount / 10) % 2 == 0 ? UiKit.GOLD : 0xFFFFE9A8;
            UiKit.textRight(g, f, "+" + free + " [K]", x + bw - 5, y + 21, c);
        }
    }
}
