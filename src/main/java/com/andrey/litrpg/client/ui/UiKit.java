package com.andrey.litrpg.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Стиль интерфейса «Системы»: тёмное стекло, голубые рамки. Все окна и HUD рисуются через него. */
public final class UiKit {
    public static final int GLASS = 0xE60A111D;
    public static final int GLASS_LIGHT = 0xD0132238;
    public static final int GLASS_HOVER = 0xE01B3352;
    public static final int FRAME = 0xFF3FB6FF;
    public static final int FRAME_DIM = 0x553FB6FF;
    public static final int FRAME_BRIGHT = 0xFFA6E1FF;
    public static final int TEXT = 0xFFE8F4FF;
    public static final int MUTED = 0xFF8EA4BC;
    public static final int GOLD = 0xFFFFC857;
    public static final int GOOD = 0xFF6BE38A;
    public static final int BAD = 0xFFFF6B6B;
    public static final int HEALTH = 0xFFD8343A;
    public static final int HEALTH_TOP = 0xFFFF6B5E;
    public static final int XP = 0xFF9B7BFF;

    private UiKit() {
    }

    /** Окно-панель: стекло, двойная рамка, светлые уголки. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, FRAME);
    }

    public static void panel(GuiGraphics g, int x, int y, int w, int h, int frame) {
        g.fill(x, y, x + w, y + h, GLASS);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + Math.min(h - 1, 14), 0x30A6E1FF, 0x00A6E1FF);
        border(g, x, y, w, h, frame);
        border(g, x + 2, y + 2, w - 4, h - 4, (frame & 0x00FFFFFF) | 0x40000000);
        corners(g, x, y, w, h, FRAME_BRIGHT);
    }

    public static void border(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void corners(GuiGraphics g, int x, int y, int w, int h, int c) {
        int l = Math.min(5, Math.min(w, h) / 3);
        g.fill(x, y, x + l, y + 1, c);
        g.fill(x, y, x + 1, y + l, c);
        g.fill(x + w - l, y, x + w, y + 1, c);
        g.fill(x + w - 1, y, x + w, y + l, c);
        g.fill(x, y + h - 1, x + l, y + h, c);
        g.fill(x, y + h - l, x + 1, y + h, c);
        g.fill(x + w - l, y + h - 1, x + w, y + h, c);
        g.fill(x + w - 1, y + h - l, x + w, y + h, c);
    }

    /** Полоса с долей заполнения; верх светлее низа. */
    public static void bar(GuiGraphics g, int x, int y, int w, int h, double ratio, int top, int bottom) {
        g.fill(x, y, x + w, y + h, 0xC0000000);
        int fw = (int) Math.round(Math.max(0, Math.min(1, ratio)) * (w - 2));
        if (fw > 0) g.fillGradient(x + 1, y + 1, x + 1 + fw, y + h - 1, top, bottom);
        border(g, x, y, w, h, 0x803FB6FF);
    }

    public static void text(GuiGraphics g, Font f, String s, int x, int y, int color) {
        g.drawString(f, s, x, y, color, true);
    }

    public static void textRight(GuiGraphics g, Font f, String s, int right, int y, int color) {
        g.drawString(f, s, right - f.width(s), y, color, true);
    }

    public static void textCenter(GuiGraphics g, Font f, String s, int cx, int y, int color) {
        g.drawString(f, s, cx - f.width(s) / 2, y, color, true);
    }

    public static void textCenter(GuiGraphics g, Font f, Component s, int cx, int y, int color) {
        g.drawString(f, s, cx - f.width(s) / 2, y, color, true);
    }

    /** Заголовок с линией под ним. */
    public static void heading(GuiGraphics g, Font f, String s, int x, int y, int w) {
        text(g, f, s, x, y, FRAME_BRIGHT);
        g.fill(x, y + 10, x + w, y + 11, FRAME_DIM);
    }

    public static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
