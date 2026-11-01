package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.SkillActionPayload;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Resource;
import com.andrey.litrpg.skill.SkillText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Выбор класса на 10 уровне: Система предлагает до трёх классов по стилю игры (первый — лучше всего подходит).
 * Снизу — идея и навыки класса; наведите на навык — полное описание с числами для вашего персонажа.
 */
public final class ClassChoiceScreen extends Screen {
    private static final int W = 310, H = 226, CARD_W = 94, CARD_H = 62, GAP = 8;

    private int left, top;
    private final List<PlayerClass> offers = new ArrayList<>();
    private PlayerClass chosen, hovered;
    private SysButton confirm, back;
    private final List<SysButton> picks = new ArrayList<>();

    public ClassChoiceScreen() {
        super(Component.literal("Выбор класса"));
    }

    private int cardX(int i) {
        int n = Math.max(1, offers.size());
        return left + (W - n * CARD_W - (n - 1) * GAP) / 2 + i * (CARD_W + GAP);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        offers.clear();
        for (String id : ClientPlayerData.offers()) {
            PlayerClass c = ClassRegistry.get(id);
            if (c != null) offers.add(c);
        }
        picks.clear();
        for (int i = 0; i < offers.size(); i++) {
            PlayerClass c = offers.get(i);
            SysButton b = addRenderableWidget(new SysButton(cardX(i) + 8, top + 24 + CARD_H - 19, CARD_W - 16, 14, "Выбрать",
                    () -> {
                        chosen = c;
                        refresh();
                    }));
            b.accent = c.color();
            picks.add(b);
        }
        confirm = addRenderableWidget(new SysButton(left + W / 2 - 104, top + H - 21, 100, 16, "Подтвердить", this::confirm));
        confirm.accent = UiKit.GOLD;
        back = addRenderableWidget(new SysButton(left + W / 2 + 4, top + H - 21, 100, 16, "Назад", () -> {
            chosen = null;
            refresh();
        }));
        refresh();
    }

    private void refresh() {
        confirm.visible = back.visible = chosen != null;
        for (int i = 0; i < picks.size(); i++) picks.get(i).selected = offers.get(i) == chosen;
    }

    private void confirm() {
        if (chosen == null) return;
        PacketDistributor.sendToServer(SkillActionPayload.chooseClass(chosen.id()));
        onClose();
    }

    @Override
    public void tick() {
        if (!ClientPlayerData.classId().isEmpty()) onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        UiKit.panel(g, left, top, W, H);
        UiKit.textCenter(g, font, "Система: выберите класс", left + W / 2, top + 9, UiKit.FRAME_BRIGHT);
        if (offers.isEmpty()) {
            UiKit.textCenter(g, font, "Для вашей роли пока нет свободных классов.", left + W / 2, top + 60, UiKit.MUTED);
            super.render(g, mx, my, pt);
            return;
        }
        hovered = null;
        for (int i = 0; i < offers.size(); i++) {
            PlayerClass c = offers.get(i);
            int cx = cardX(i), cy = top + 24;
            boolean picked = c == chosen, hover = UiKit.inside(mx, my, cx, cy, CARD_W, CARD_H);
            if (hover) hovered = c;
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, picked ? 0xE0182B45 : hover ? 0xE0132238 : 0xC00D1828);
            UiKit.border(g, cx, cy, CARD_W, CARD_H, picked || hover ? c.color() : (c.color() & 0xFFFFFF) | 0x70000000);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(cx + 14, cy + 5, 0);
            pose.scale(1.8f, 1.8f, 1);
            g.drawString(font, c.icon(), -font.width(c.icon()) / 2, 0, c.color(), true);
            pose.popPose();
            UiKit.text(g, font, c.name(), cx + 26, cy + 6, c.color());
            String res = c.resource() == Resource.NONE ? "без ресурса" : c.resource().title.toLowerCase();
            UiKit.text(g, font, font.plainSubstrByWidth(res, CARD_W - 30), cx + 26, cy + 17, UiKit.MUTED);
            if (i == 0 && offers.size() > 1) UiKit.textCenter(g, font, "★ подходит лучше всех", cx + CARD_W / 2, cy + 30, UiKit.GOLD);
        }
        PlayerClass shown = hovered != null ? hovered : chosen != null ? chosen : offers.get(0);
        details(g, shown, mx, my);
        if (chosen != null) {
            UiKit.textCenter(g, font, "Сменить класс можно только у торговца за монеты.", left + W / 2, top + H - 32, UiKit.GOLD);
        }
        super.render(g, mx, my, pt);
    }

    private void details(GuiGraphics g, PlayerClass c, int mx, int my) {
        int x = left + 12, y = top + 24 + CARD_H + 7, w = W - 24, bottom = top + H - 36;
        g.fill(x - 4, y - 3, x + w + 4, bottom, 0x80050A12);
        UiKit.border(g, x - 4, y - 3, w + 8, bottom - (y - 3), UiKit.FRAME_DIM);
        for (FormattedCharSequence line : font.split(Component.literal(c.idea()), w)) {
            g.drawString(font, line, x, y, UiKit.TEXT, true);
            y += 10;
        }
        y += 3;
        int colW = w / 2;
        List<String> tooltip = null;
        for (int i = 0; i < c.skills().size(); i++) {
            var u = c.skills().get(i);
            int sx = x + (i % 2) * colW, sy = y + (i / 2) * 11;
            boolean hover = UiKit.inside(mx, my, sx - 1, sy - 1, colW - 2, 10);
            if (hover) g.fill(sx - 1, sy - 1, sx + colW - 3, sy + 9, 0x303FB6FF);
            UiKit.text(g, font, u.level() + "", sx, sy, 0xFF5F7790);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(sx + 13, sy - 1, 0);
            pose.scale(0.625f, 0.625f, 1);
            if (!com.andrey.litrpg.client.ui.SkillIcons.draw(g, u.skill().id, 0, 0, 16)) {
                pose.popPose();
                UiKit.text(g, font, u.skill().glyph, sx + 14, sy, 0xFF000000 | u.skill().color);
            } else {
                pose.popPose();
            }
            UiKit.text(g, font, font.plainSubstrByWidth(u.skill().name, colW - 30), sx + 24, sy, UiKit.TEXT);
            if (hover) tooltip = SkillText.tooltip(u.skill(), 0, c.resource(), ClientPlayerData.viewer());
        }
        UiKit.text(g, font, "Уровни открытия навыков слева; наведите — описание.", x, bottom - 11, 0xFF5F7790);
        if (tooltip != null) showTooltip(g, tooltip, mx, my);
    }

    private void showTooltip(GuiGraphics g, List<String> lines, int mx, int my) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? 0xFFE27A : i == 1 ? 0xE8F4FF : 0x8EA4BC;
            out.addAll(font.split(Component.literal(lines.get(i)).withColor(color), 220));
        }
        g.renderTooltip(font, out, mx, my);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
