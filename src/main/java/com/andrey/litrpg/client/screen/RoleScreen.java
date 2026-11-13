package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.net.ChooseRolePayload;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.RoleInfo;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Выбор роли в Зале ролей (привал этапа 1). Открывается само один раз; закрыли — снова клавишей K. Сверху — три карточки, снизу — подробности
 * роли под курсором (или выбранной). Подробности строятся из roles.json через {@link RoleInfo}.
 */
public final class RoleScreen extends Screen {
    private static final Role[] ROLES = {Role.TANK, Role.DAMAGE, Role.SUPPORT};
    private static final int W = 310, H = 226, CARD_W = 94, CARD_H = 74, GAP = 8;

    private int left, top;
    private Role chosen;
    private Role hovered;
    private SysButton confirm, back;
    private final SysButton[] pick = new SysButton[ROLES.length];

    public RoleScreen() {
        super(Component.literal("Выбор роли"));
    }

    /** С уже отмеченной ролью (для dev-проверки окна). */
    public RoleScreen(Role preselected) {
        this();
        chosen = preselected;
    }

    private int cardX(int i) {
        return left + (W - 3 * CARD_W - 2 * GAP) / 2 + i * (CARD_W + GAP);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        for (int i = 0; i < ROLES.length; i++) {
            Role r = ROLES[i];
            pick[i] = addRenderableWidget(new SysButton(cardX(i) + 8, top + 24 + CARD_H - 20, CARD_W - 16, 15, "Выбрать",
                    () -> {
                        chosen = r;
                        refresh();
                    }));
            pick[i].accent = r.color;
        }
        confirm = addRenderableWidget(new SysButton(left + W / 2 - 104, top + H - 22, 100, 16, "Подтвердить", this::confirm));
        confirm.accent = UiKit.GOLD;
        back = addRenderableWidget(new SysButton(left + W / 2 + 4, top + H - 22, 100, 16, "Назад", () -> {
            chosen = null;
            refresh();
        }));
        refresh();
    }

    private void refresh() {
        confirm.visible = back.visible = chosen != null;
        for (int i = 0; i < ROLES.length; i++) pick[i].selected = ROLES[i] == chosen;
    }

    private void confirm() {
        if (chosen == null) return;
        PacketDistributor.sendToServer(new ChooseRolePayload(chosen));
        onClose();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        UiKit.panel(g, left, top, W, H);
        UiKit.textCenter(g, font, "Система: выберите роль в группе", left + W / 2, top + 9, UiKit.FRAME_BRIGHT);
        hovered = null;
        for (int i = 0; i < ROLES.length; i++) {
            Role r = ROLES[i];
            int cx = cardX(i), cy = top + 24;
            boolean picked = r == chosen;
            boolean hover = UiKit.inside(mx, my, cx, cy, CARD_W, CARD_H);
            if (hover) hovered = r;
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, picked ? 0xE0182B45 : hover ? 0xE0132238 : 0xC00D1828);
            UiKit.border(g, cx, cy, CARD_W, CARD_H, picked || hover ? r.color : (r.color & 0x00FFFFFF) | 0x70000000);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(cx + CARD_W / 2f, cy + 7, 0);
            pose.scale(2.4f, 2.4f, 1);
            g.drawString(font, r.icon, -font.width(r.icon) / 2, 0, r.color, true);
            pose.popPose();
            UiKit.textCenter(g, font, r.title, cx + CARD_W / 2, cy + 34, r.color);
        }
        details(g, hovered != null ? hovered : chosen);
        if (chosen != null) {
            UiKit.textCenter(g, font, "Сменить роль можно только у торговца за монеты.", left + W / 2, top + H - 34, UiKit.GOLD);
        } else {
            UiKit.textCenter(g, font, "Для прохождения группе нужны все три роли.", left + W / 2, top + H - 17, UiKit.MUTED);
        }
        super.render(g, mx, my, pt);
    }

    /** Подробности роли на всю ширину окна: девиз и особенности в две колонки. */
    private void details(GuiGraphics g, Role r) {
        int x = left + 12, y = top + 24 + CARD_H + 8, w = W - 24;
        g.fill(x - 4, y - 3, x + w + 4, top + H - 38, 0x80050A12);
        UiKit.border(g, x - 4, y - 3, w + 8, top + H - 38 - (y - 3), UiKit.FRAME_DIM);
        if (r == null) {
            UiKit.textCenter(g, font, "Наведите на роль, чтобы увидеть подробности.", left + W / 2, y + 30, UiKit.MUTED);
            return;
        }
        UiKit.text(g, font, r.icon + " " + r.title, x, y, r.color);
        y += 11;
        for (FormattedCharSequence line : font.split(Component.literal(r.tagline), w)) {
            g.drawString(font, line, x, y, UiKit.TEXT, true);
            y += 10;
        }
        y += 3;
        List<RoleInfo.Line> lines = RoleInfo.describe(r, Balance.get(Balance.ROLES));
        int half = (lines.size() + 1) / 2, colW = w / 2;
        for (int i = 0; i < lines.size(); i++) {
            RoleInfo.Line l = lines.get(i);
            int lx = x + (i < half ? 0 : colW), ly = y + (i % half) * 10;
            String text = font.plainSubstrByWidth("• " + l.text(), colW - 4);
            UiKit.text(g, font, text, lx, ly, l.good() ? UiKit.GOOD : 0xFFFFA07A);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;   // обновление 15: окно в Зале ролей — закрыть, осмотреться, открыть снова клавишей K
    }

    @Override
    public void tick() {
        // роль уже выбрана (другим путём или подтверждение дошло) — окно больше не нужно
        if (com.andrey.litrpg.client.ClientPlayerData.role().chosen()) onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
