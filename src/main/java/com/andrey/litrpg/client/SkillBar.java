package com.andrey.litrpg.client;

import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.skill.Resource;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillRegistry;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/** Панель навыков над здоровьем: 6 ячеек (Z X C V B G) + рывок (Alt), чтение, ресурс, метки. */
public final class SkillBar {
    public static final int SLOT = 22, GAP = 3, COUNT = 7;
    public static final int WIDTH = COUNT * SLOT + (COUNT - 1) * GAP;

    private SkillBar() {
    }

    /** Верх панели (по нему выравниваются надписи над ней). */
    public static int top(int h) {
        return h - 41 - SLOT - 5;
    }

    public static void render(GuiGraphics g, Font f, int w, int h, float partial) {
        int x0 = (w - WIDTH) / 2, y = top(h);
        List<Skill> loadout = ClientPlayerData.loadout();
        double gcd = ClientSkills.gcdFraction(partial);
        for (int i = 0; i < 6; i++) {
            int x = x0 + i * (SLOT + GAP);
            slot(g, f, x, y, i < loadout.size() ? loadout.get(i) : null, KeyBinds.SKILL_LABELS[i], gcd, partial);
        }
        dashSlot(g, f, x0 + 6 * (SLOT + GAP), y, partial);
        castBar(g, f, w, y - 14, partial);
        resourceBar(g, f, w, h);
        statuses(g, f, x0, y - 30);
    }

    private static void slot(GuiGraphics g, Font f, int x, int y, Skill s, String key, double gcd, float partial) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xC0080E18);
        if (s == null) {
            UiKit.border(g, x, y, SLOT, SLOT, UiKit.FRAME_DIM);
            keyLabel(g, f, x, y, key, 0xFF4A5A6E);
            return;
        }
        int c = 0xFF000000 | s.color;
        g.fillGradient(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, (c & 0x00FFFFFF) | 0x50000000, (c & 0x00FFFFFF) | 0x18000000);
        glyph(g, f, s.glyph, x + SLOT / 2, y + 5, c);
        double cd = ClientSkills.cooldownFraction(s.id, partial);
        double shade = Math.max(cd, gcd);
        if (shade > 0) {
            int hh = (int) Math.round((SLOT - 2) * shade);
            g.fill(x + 1, y + 1 + (SLOT - 2 - hh), x + SLOT - 1, y + SLOT - 1, cd >= gcd ? 0xB0000000 : 0x70000000);
        }
        double secs = ClientSkills.cooldownSeconds(s.id, partial);
        if (secs > 0.05 && cd > 0) {
            String t = secs >= 10 ? String.valueOf((int) Math.ceil(secs)) : Derived.trim(Math.ceil(secs * 10) / 10);
            UiKit.textCenter(g, f, t, x + SLOT / 2, y + 7, 0xFFFFFFFF);
        }
        boolean casting = ClientSkills.casting() == s;
        UiKit.border(g, x, y, SLOT, SLOT, casting ? UiKit.GOLD : shade > 0 ? 0xFF3A4A5E : (c & 0x00FFFFFF) | 0xC0000000);
        keyLabel(g, f, x, y, key, 0xFFA6B8CC);
        int rank = ClientPlayerData.rank(s.id);
        if (rank > 0) {
            for (int r = 0; r < rank; r++) g.fill(x + SLOT - 3, y + SLOT - 3 - r * 3, x + SLOT - 1, y + SLOT - 2 - r * 3, UiKit.GOLD);
        }
    }

    private static void dashSlot(GuiGraphics g, Font f, int x, int y, float partial) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xC0080E18);
        glyph(g, f, "»", x + SLOT / 2, y + 5, 0xFFA6E1FF);
        double cd = ClientSkills.dashFraction(partial);
        if (cd > 0) {
            int hh = (int) Math.round((SLOT - 2) * cd);
            g.fill(x + 1, y + 1 + (SLOT - 2 - hh), x + SLOT - 1, y + SLOT - 1, 0xB0000000);
            UiKit.textCenter(g, f, String.valueOf((int) Math.ceil(ClientSkills.dashSeconds(partial))), x + SLOT / 2, y + 7, 0xFFFFFFFF);
        }
        UiKit.border(g, x, y, SLOT, SLOT, cd > 0 ? 0xFF5A4A2A : UiKit.GOLD);
        keyLabel(g, f, x, y, "Alt", 0xFFA6B8CC);
    }

    private static void glyph(GuiGraphics g, Font f, String s, int cx, int y, int color) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, y, 0);
        pose.scale(1.5f, 1.5f, 1);
        g.drawString(f, s, -f.width(s) / 2, 0, color, true);
        pose.popPose();
    }

    private static void keyLabel(GuiGraphics g, Font f, int x, int y, String key, int color) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 2, y + SLOT - 6, 0);
        pose.scale(0.55f, 0.55f, 1);
        g.drawString(f, key, 0, 0, color, true);
        pose.popPose();
    }

    private static void castBar(GuiGraphics g, Font f, int w, int y, float partial) {
        Skill s = ClientSkills.casting();
        if (s == null) return;
        int bw = 140, x = (w - bw) / 2;
        double prog = ClientSkills.castProgress(partial);
        g.fill(x - 1, y - 1, x + bw + 1, y + 10, 0xC0000000);
        int c = 0xFF000000 | s.color;
        g.fillGradient(x, y, x + (int) Math.round(bw * prog), y + 9, c, 0xFF000000 | ((c >> 1) & 0x7F7F7F));
        UiKit.border(g, x - 1, y - 1, bw + 2, 11, UiKit.GOLD);
        String text = s.name + "  " + Derived.trim(Math.ceil(ClientSkills.castSecondsLeft(partial) * 10) / 10) + " с";
        UiKit.textCenter(g, f, text, w / 2, y + 1, 0xFFFFFFFF);
    }

    private static void resourceBar(GuiGraphics g, Font f, int w, int h) {
        var cls = ClientPlayerData.playerClass();
        if (cls == null || cls.resource() == Resource.NONE) return;
        Resource r = cls.resource();
        int bw = 182, x = (w - bw) / 2, y = h - 30;
        double ratio = ClientSkills.resource() / r.max;
        g.fill(x, y, x + bw, y + 5, 0xC0000000);
        int fw = (int) Math.round((bw - 2) * Math.max(0, Math.min(1, ratio)));
        if (fw > 0) g.fillGradient(x + 1, y + 1, x + 1 + fw, y + 4, r.color | 0xFF000000, (r.color & 0x00FFFFFF) | 0xFF000000);
        UiKit.border(g, x, y, bw, 5, 0x80000000 | (r.color & 0xFFFFFF));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + bw + 3, y - 1, 0);
        pose.scale(0.6f, 0.6f, 1);
        g.drawString(f, Math.round(ClientSkills.resource()) + " " + r.title.toLowerCase(), 0, 0, r.color | 0xFF000000, true);
        pose.popPose();
    }

    /** Метки игрока: значок навыка/вида, цвет — полезная или вредная, секунды. */
    private static void statuses(GuiGraphics g, Font f, int x, int y) {
        var list = ClientStatuses.shown();
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (list.isEmpty() || mc.level == null) return;
        long now = mc.level.getGameTime();
        int i = 0;
        java.util.Set<String> shownIcons = new java.util.HashSet<>();
        for (var s : list) {
            long left = s.until() - now;
            // Один навык может наложить несколько меток (защита + отражение) — значок один.
            if (left <= 0 || !shownIcons.add(s.entry().icon())) continue;
            int bx = x + i * 18;
            StatusType t = s.entry().type();
            Skill src = SkillRegistry.get(s.entry().icon());
            int c = src != null ? 0xFF000000 | src.color : t.good ? UiKit.GOOD : UiKit.BAD;
            g.fill(bx, y, bx + 16, y + 16, 0xC0080E18);
            String gl = src != null ? src.glyph : t.title.substring(0, 1);
            UiKit.textCenter(g, f, gl, bx + 8, y + 4, c);
            UiKit.border(g, bx, y, 16, 16, t.good ? 0xFF3FA05A : 0xFFB03A3A);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(bx + 8, y + 17, 0);
            pose.scale(0.55f, 0.55f, 1);
            String sec = left >= 200 ? (left / 20) + "" : Derived.trim(Math.ceil(left / 2.0) / 10);
            g.drawString(f, sec, -f.width(sec) / 2, 0, 0xFFE0E0E0, true);
            pose.popPose();
            if (++i >= 10) break;
        }
    }
}
