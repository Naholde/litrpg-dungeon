package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.AllocateStatsPayload;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import com.andrey.litrpg.player.StatBreakdown;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Окно персонажа (K). «Статус» — характеристики и вложение очков с предпросмотром, «Навыки» — панель,
 * «Расчёт» — откуда берётся каждая цифра. Все числа — из того же {@link StatBreakdown}, что и на сервере.
 */
public final class CharacterScreen extends Screen {
    private static final int W = 310, H = 226;
    private enum Tab { STATUS, SKILLS, BREAKDOWN }

    private static Tab lastTab = Tab.STATUS;
    private Tab tab = lastTab;
    private int left, top;
    private final int[] pending = new int[Stat.ALL.length];
    /** Выбранная строка «Расчёта»: 0..4 — характеристики, дальше — производные. */
    private int selected = Stat.ALL.length;
    private final List<SysButton> tabButtons = new ArrayList<>();
    private SysButton apply, clear;
    private final SysButton[] plus = new SysButton[Stat.ALL.length];
    private final SysButton[] minus = new SysButton[Stat.ALL.length];

    public CharacterScreen() {
        super(Component.literal("Персонаж"));
    }

    /** Открыть сразу на вкладке: 0 — «Статус», 1 — «Навыки», 2 — «Расчёт» (для dev-проверки). */
    public CharacterScreen(int tabIndex) {
        this();
        tab = Tab.values()[Math.max(0, Math.min(Tab.values().length - 1, tabIndex))];
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabButtons.clear();
        String[] names = {"Статус", "Навыки", "Расчёт"};
        for (int i = 0; i < names.length; i++) {
            Tab t = Tab.values()[i];
            SysButton b = addRenderableWidget(new SysButton(left + 8 + i * 66, top + 6, 62, 16, names[i], () -> switchTab(t)));
            tabButtons.add(b);
        }
        for (Stat s : Stat.ALL) {
            int y = rowY(s);
            int i = s.ordinal();
            minus[i] = addRenderableWidget(new SysButton(left + 128, y - 3, 13, 13, "−", () -> change(s, -1)));
            plus[i] = addRenderableWidget(new SysButton(left + 143, y - 3, 13, 13, "+", () -> change(s, +1)));
        }
        apply = addRenderableWidget(new SysButton(left + 10, top + H - 24, 72, 16, "Применить", this::applyPending));
        apply.accent = UiKit.GOLD;
        clear = addRenderableWidget(new SysButton(left + 86, top + H - 24, 70, 16, "Отменить", () -> Arrays.fill(pending, 0)));
        updateWidgets();
    }

    private int rowY(Stat s) {
        return top + 74 + s.ordinal() * 17;
    }

    private void switchTab(Tab t) {
        tab = t;
        lastTab = t;
        updateWidgets();
    }

    /** Шаг вложения: Shift — по 5, Ctrl — все свободные. */
    private void change(Stat s, int dir) {
        int step = hasControlDown() ? 1000 : hasShiftDown() ? 5 : 1;
        int i = s.ordinal();
        if (dir > 0) pending[i] += Math.min(step, freeLeft());
        else pending[i] = Math.max(0, pending[i] - step);
        updateWidgets();
    }

    private int freeLeft() {
        return ClientPlayerData.freeStatPoints() - Arrays.stream(pending).sum();
    }

    private void applyPending() {
        if (Arrays.stream(pending).sum() == 0) return;
        PacketDistributor.sendToServer(new AllocateStatsPayload(pending.clone()));
        Arrays.fill(pending, 0);
        updateWidgets();
    }

    private void updateWidgets() {
        for (int i = 0; i < tabButtons.size(); i++) tabButtons.get(i).selected = tab.ordinal() == i;
        boolean status = tab == Tab.STATUS;
        boolean any = Arrays.stream(pending).sum() > 0;
        for (Stat s : Stat.ALL) {
            plus[s.ordinal()].visible = status;
            minus[s.ordinal()].visible = status;
            plus[s.ordinal()].active = freeLeft() > 0;
            minus[s.ordinal()].active = pending[s.ordinal()] > 0;
        }
        apply.visible = clear.visible = status;
        apply.active = clear.active = any;
    }

    @Override
    public void tick() {
        // Сервер мог изменить уровень или очки — держим кнопки в актуальном состоянии.
        if (freeLeft() < 0) Arrays.fill(pending, 0);
        updateWidgets();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        UiKit.panel(g, left, top, W, H);
        Role role = ClientPlayerData.role();
        String head = "Ур. " + ClientPlayerData.level() + "  " + role.icon + " " + role.title;
        UiKit.textRight(g, font, head, left + W - 10, top + 10, role.color);
        super.render(g, mx, my, pt);
        switch (tab) {
            case STATUS -> renderStatus(g, mx, my);
            case SKILLS -> renderSkills(g);
            case BREAKDOWN -> renderBreakdown(g, mx, my);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        // Фон рисуем сами в render (до панели), чтобы не затемнять дважды.
    }

    // ------------------------------------------------------------------ Статус

    private void renderStatus(GuiGraphics g, int mx, int my) {
        StatBreakdown now = ClientPlayerData.breakdown();
        boolean any = Arrays.stream(pending).sum() > 0;
        StatBreakdown next = any ? ClientPlayerData.preview(pending) : now;
        int x = left + 10;
        String name = minecraft.player != null ? minecraft.player.getGameProfile().getName() : "";
        UiKit.heading(g, font, name, x, top + 30, 146);
        long need = ClientPlayerData.xpToNext();
        double ratio = need > 0 ? (double) ClientPlayerData.xp() / need : 1;
        UiKit.bar(g, x, top + 45, 146, 6, ratio, 0xFFC6B5FF, UiKit.XP);
        String xpText = need > 0 ? Derived.MAX_HEALTH.format(ClientPlayerData.xp()) + " / "
                + Derived.MAX_HEALTH.format(need) + " опыта" : "Максимальный уровень";
        UiKit.text(g, font, xpText, x, top + 54, UiKit.MUTED);

        for (Stat s : Stat.ALL) {
            int y = rowY(s);
            boolean hover = UiKit.inside(mx, my, x - 2, y - 3, 118, 13);
            if (hover) g.fill(x - 2, y - 3, x + 116, y + 10, 0x203FB6FF);
            UiKit.text(g, font, s.title, x, y, UiKit.TEXT);
            double v = now.get(s), nv = next.get(s);
            String val = Derived.trim(v);
            if (pending[s.ordinal()] > 0) {
                UiKit.textRight(g, font, Derived.trim(nv), x + 114, y, UiKit.GOOD);
                UiKit.textRight(g, font, "+" + pending[s.ordinal()], x + 84, y, UiKit.GOLD);
            } else {
                UiKit.textRight(g, font, val, x + 114, y, UiKit.TEXT);
            }
            if (hover) {
                g.renderTooltip(font, List.of(Component.literal(s.title).withColor(UiKit.FRAME_BRIGHT & 0xFFFFFF),
                        Component.literal("Влияет на: " + s.hint).withColor(UiKit.MUTED & 0xFFFFFF),
                        Component.literal("Вложено очков: " + ClientPlayerData.allocated(s)).withColor(UiKit.MUTED & 0xFFFFFF),
                        Component.literal("Shift — по 5, Ctrl — все").withColor(UiKit.MUTED & 0xFFFFFF)),
                        java.util.Optional.empty(), mx, my);
            }
        }
        int free = freeLeft();
        UiKit.text(g, font, "Свободно очков: ", x, top + 162, UiKit.MUTED);
        UiKit.text(g, font, String.valueOf(free), x + font.width("Свободно очков: "), top + 162,
                free > 0 ? UiKit.GOLD : UiKit.MUTED);
        UiKit.text(g, font, "Каждое очко = " + Derived.trim(ClientPlayerData.progression().statPointValue()),
                x, top + 174, 0xFF5F7790);

        // Производные величины — правая колонка.
        int rx = left + 166, rw = W - 176;
        UiKit.heading(g, font, "Показатели", rx, top + 30, rw);
        int y = top + 44;
        Derived hovered = null;
        for (Derived d : Derived.values()) {
            boolean hover = UiKit.inside(mx, my, rx - 2, y - 1, rw + 4, 11);
            if (hover) {
                g.fill(rx - 2, y - 1, rx + rw + 2, y + 10, 0x203FB6FF);
                hovered = d;
            }
            UiKit.text(g, font, d.title, rx, y, UiKit.MUTED);
            double v = now.get(d), nv = next.get(d);
            boolean changed = Math.abs(nv - v) > 1e-9;
            UiKit.textRight(g, font, d.format(changed ? nv : v), rx + rw, y, changed ? UiKit.GOOD : UiKit.TEXT);
            y += 12;
        }
        if (hovered != null) {
            g.renderTooltip(font, List.of(Component.literal(hovered.title).withColor(UiKit.FRAME_BRIGHT & 0xFFFFFF),
                    Component.literal(hovered.hint).withColor(UiKit.MUTED & 0xFFFFFF),
                    Component.literal("Подробно — вкладка «Расчёт»").withColor(0x5F7790)),
                    java.util.Optional.empty(), mx, my);
        }
    }

    // ------------------------------------------------------------------ Навыки

    private void renderSkills(GuiGraphics g) {
        int x = left + 10;
        UiKit.heading(g, font, "Панель навыков", x, top + 30, W - 20);
        String[] keys = {"Z", "X", "C", "V", "B", "G"};
        int slot = 30, gap = 8, total = 7 * slot + 6 * gap, sx = left + (W - total) / 2, sy = top + 60;
        for (int i = 0; i < 7; i++) {
            int bx = sx + i * (slot + gap);
            g.fill(bx, sy, bx + slot, sy + slot, UiKit.GLASS_LIGHT);
            UiKit.border(g, bx, sy, slot, slot, i == 6 ? UiKit.GOLD : UiKit.FRAME_DIM);
            UiKit.textCenter(g, font, i == 6 ? "Alt" : keys[i], bx + slot / 2, sy + slot + 4, UiKit.MUTED);
        }
        UiKit.textCenter(g, font, "Рывок есть у всех — клавиша Alt.", left + W / 2, sy + slot + 22, UiKit.TEXT);
        UiKit.textCenter(g, font, "Навыки роли и класса Система выдаст позже.", left + W / 2, sy + slot + 36, UiKit.MUTED);
        UiKit.textCenter(g, font, "Класс — на " + ClientPlayerData.progression().classLevel() + " уровне, по стилю игры.",
                left + W / 2, sy + slot + 48, UiKit.MUTED);
    }

    // ------------------------------------------------------------------ Расчёт

    private void renderBreakdown(GuiGraphics g, int mx, int my) {
        StatBreakdown b = ClientPlayerData.breakdown();
        int lx = left + 8, lw = 118, y = top + 30;
        int rows = Stat.ALL.length + Derived.values().length;
        for (int i = 0; i < rows; i++) {
            int ry = y + i * 10;
            boolean sel = i == selected;
            boolean hover = UiKit.inside(mx, my, lx, ry - 1, lw, 10);
            if (sel) g.fill(lx, ry - 1, lx + lw, ry + 9, 0x503FB6FF);
            else if (hover) g.fill(lx, ry - 1, lx + lw, ry + 9, 0x203FB6FF);
            String t = i < Stat.ALL.length ? Stat.ALL[i].title : Derived.values()[i - Stat.ALL.length].title;
            UiKit.text(g, font, t, lx + 3, ry, sel ? 0xFFFFFFFF : i < Stat.ALL.length ? UiKit.GOLD : UiKit.TEXT);
        }
        g.fill(lx + lw + 4, top + 28, lx + lw + 5, top + H - 8, UiKit.FRAME_DIM);

        int rx = lx + lw + 12, rw = left + W - 10 - rx;
        List<StatBreakdown.Line> lines;
        String title, total, hint;
        java.util.function.DoubleFunction<String> fmt;
        if (selected < Stat.ALL.length) {
            Stat s = Stat.ALL[selected];
            lines = b.lines(s);
            title = s.title;
            total = Derived.trim(b.get(s));
            hint = "Влияет на: " + s.hint;
            fmt = v -> (v >= 0 ? "+" : "−") + Derived.trim(Math.abs(v));
        } else {
            Derived d = Derived.values()[selected - Stat.ALL.length];
            lines = b.lines(d);
            title = d.title;
            total = d.format(b.get(d));
            hint = d.hint;
            fmt = d::formatDelta;
        }
        UiKit.heading(g, font, title, rx, top + 30, rw);
        int ly = top + 46;
        for (var line : font.split(Component.literal(hint), rw)) {
            g.drawString(font, line, rx, ly, UiKit.MUTED, true);
            ly += 10;
        }
        ly += 4;
        for (StatBreakdown.Line l : lines) {
            // Длинную подпись переносим, а не обрезаем: обрезка прятала множители и сумма казалась неверной.
            String value = fmt.apply(l.value());
            UiKit.textRight(g, font, value, rx + rw, ly, l.value() < 0 ? UiKit.BAD : UiKit.GOOD);
            for (var part : font.split(Component.literal(l.source()), rw - font.width(value) - 6)) {
                g.drawString(font, part, rx, ly, UiKit.TEXT, true);
                ly += 10;
            }
            ly += 1;
        }
        g.fill(rx, ly + 1, rx + rw, ly + 2, UiKit.FRAME_DIM);
        UiKit.text(g, font, "Итого", rx, ly + 5, UiKit.FRAME_BRIGHT);
        UiKit.textRight(g, font, total, rx + rw, ly + 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (tab == Tab.BREAKDOWN) {
            int rows = Stat.ALL.length + Derived.values().length;
            for (int i = 0; i < rows; i++) {
                if (UiKit.inside(mx, my, left + 8, top + 30 + i * 10 - 1, 118, 10)) {
                    selected = i;
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (com.andrey.litrpg.client.KeyBinds.CHARACTER.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
