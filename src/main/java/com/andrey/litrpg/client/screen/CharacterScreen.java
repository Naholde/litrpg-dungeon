package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.AllocateStatsPayload;
import com.andrey.litrpg.net.SkillActionPayload;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillText;
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
    /** Высота строки списка «Расчёта»: 20 строк должны влезть в окно. */
    private static final int ROW = 9;
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
    private final SysButton[] rankUp = new SysButton[6];

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
            SysButton b = addRenderableWidget(new SysButton(left + 8 + i * 60, top + 6, 56, 16, names[i], () -> switchTab(t)));
            tabButtons.add(b);
        }
        for (Stat s : Stat.ALL) {
            int y = rowY(s);
            int i = s.ordinal();
            minus[i] = addRenderableWidget(new SysButton(left + 128, y - 3, 13, 13, "−", () -> change(s, -1)));
            plus[i] = addRenderableWidget(new SysButton(left + 143, y - 3, 13, 13, "+", () -> change(s, +1)));
        }
        for (int i = 0; i < rankUp.length; i++) {
            final int row = i;
            rankUp[i] = addRenderableWidget(new SysButton(left + W - 24, skillRowY(i) + 1, 13, 13, "+", () -> {
                var rows = skillRows();
                if (row < rows.size()) PacketDistributor.sendToServer(SkillActionPayload.rankUp(rows.get(row).skill().id));
            }));
            rankUp[i].accent = UiKit.GOLD;
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
        PlayerClass c = ClientPlayerData.playerClass();
        var rows = skillRows();
        var loadout = ClientPlayerData.loadout();
        int max = com.andrey.litrpg.core.balance.Balance.get(com.andrey.litrpg.core.balance.Balance.SKILLS).common().maxRank();
        for (int i = 0; i < rankUp.length; i++) {
            boolean show = tab == Tab.SKILLS && c != null && i < rows.size();
            rankUp[i].visible = show;
            if (show) {
                Skill s = rows.get(i).skill();
                int r = ClientPlayerData.rank(s.id);
                rankUp[i].active = loadout.contains(s) && r < max && ClientPlayerData.freeClassPoints() >= r + 1;
            }
        }
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
        PlayerClass pc = ClientPlayerData.playerClass();
        String head = "Ур. " + ClientPlayerData.level() + "  " + (pc != null ? pc.icon() + " " + pc.name() : role.icon + " " + role.title);
        UiKit.textRight(g, font, head, left + W - 10, top + 10, role.color);
        super.render(g, mx, my, pt);
        switch (tab) {
            case STATUS -> renderStatus(g, mx, my);
            case SKILLS -> renderSkills(g, mx, my);
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
            boolean hover = UiKit.inside(mx, my, rx - 2, y - 1, rw + 4, 10);
            if (hover) {
                g.fill(rx - 2, y - 1, rx + rw + 2, y + 9, 0x203FB6FF);
                hovered = d;
            }
            UiKit.text(g, font, d.title, rx, y, UiKit.MUTED);
            double v = now.get(d), nv = next.get(d);
            boolean changed = Math.abs(nv - v) > 1e-9;
            UiKit.textRight(g, font, d.format(changed ? nv : v), rx + rw, y, changed ? UiKit.GOOD : UiKit.TEXT);
            y += 11;
        }
        if (hovered != null) {
            g.renderTooltip(font, List.of(Component.literal(hovered.title).withColor(UiKit.FRAME_BRIGHT & 0xFFFFFF),
                    Component.literal(hovered.hint).withColor(UiKit.MUTED & 0xFFFFFF),
                    Component.literal("Подробно — вкладка «Расчёт»").withColor(0x5F7790)),
                    java.util.Optional.empty(), mx, my);
        }
    }

    // ------------------------------------------------------------------ Навыки

    private static final int SKILL_ROW = 22;

    private int skillRowY(int i) {
        return top + 44 + i * SKILL_ROW;
    }

    /** Навыки класса (все, с уровнями открытия) или, пока класса нет, навыки роли. */
    private List<PlayerClass.Unlock> skillRows() {
        PlayerClass c = ClientPlayerData.playerClass();
        if (c != null) return c.skills();
        List<PlayerClass.Unlock> out = new ArrayList<>();
        for (Skill s : ClassRegistry.ROLE_KITS.getOrDefault(ClientPlayerData.role(), List.of())) {
            out.add(new PlayerClass.Unlock(1, s));
        }
        return out;
    }

    private void renderSkills(GuiGraphics g, int mx, int my) {
        int x = left + 10, w = W - 20;
        PlayerClass c = ClientPlayerData.playerClass();
        String head = c != null ? c.icon() + " " + c.name() : "Навыки роли (класс — на "
                + ClientPlayerData.progression().classLevel() + " уровне)";
        UiKit.heading(g, font, head, x, top + 28, w);
        if (c != null) {
            int free = ClientPlayerData.freeClassPoints();
            UiKit.textRight(g, font, "Очки класса: " + free, x + w, top + 28, free > 0 ? UiKit.GOLD : UiKit.MUTED);
        }
        String[] keys = com.andrey.litrpg.client.KeyBinds.SKILL_LABELS;
        List<PlayerClass.Unlock> rows = skillRows();
        List<Skill> loadout = ClientPlayerData.loadout();
        List<String> tip = null;
        int max = com.andrey.litrpg.core.balance.Balance.get(com.andrey.litrpg.core.balance.Balance.SKILLS).common().maxRank();
        for (int i = 0; i < rows.size(); i++) {
            var u = rows.get(i);
            Skill s = u.skill();
            int y = skillRowY(i);
            boolean open = loadout.contains(s);
            boolean hover = UiKit.inside(mx, my, x, y - 2, w - 24, SKILL_ROW - 2);
            g.fill(x, y - 2, x + w, y + SKILL_ROW - 4, hover ? 0x30A6E1FF : 0x40000000);
            int slot = loadout.indexOf(s);
            UiKit.text(g, font, slot >= 0 ? keys[slot] : "", x + 3, y + 4, 0xFF5F7790);
            if (!com.andrey.litrpg.client.ui.SkillIcons.draw(g, s.id, x + 12, y - 1, 16)) {
                var pose = g.pose();
                pose.pushPose();
                pose.translate(x + 20, y + 1, 0);
                pose.scale(1.5f, 1.5f, 1);
                g.drawString(font, s.glyph, -font.width(s.glyph) / 2, 0, open ? 0xFF000000 | s.color : 0xFF4A5A6E, true);
                pose.popPose();
            } else if (!open) {
                g.fill(x + 12, y - 1, x + 28, y + 15, 0xA0000000); // ещё не открыт — значок притушен
            }
            UiKit.text(g, font, s.name, x + 32, y, open ? UiKit.TEXT : UiKit.MUTED);
            String sub = open ? font.plainSubstrByWidth(SkillText.body(s, ClientPlayerData.rank(s.id), ClientPlayerData.viewer()), 170)
                    : "откроется на " + u.level() + " уровне";
            var p2 = g.pose();
            p2.pushPose();
            p2.translate(x + 32, y + 10, 0);
            p2.scale(0.75f, 0.75f, 1);
            g.drawString(font, open ? sub + "…" : sub, 0, 0, 0xFF7A8DA3, true);
            p2.popPose();
            if (c != null) {
                int rank = ClientPlayerData.rank(s.id);
                for (int r = 0; r < max; r++) {
                    int px = x + w - 62 + r * 7;
                    g.fill(px, y + 4, px + 5, y + 9, r < rank ? UiKit.GOLD : 0xFF2A3A4E);
                }
            }
            if (hover) {
                tip = new ArrayList<>(SkillText.tooltip(s, ClientPlayerData.rank(s.id),
                        c != null ? c.resource() : com.andrey.litrpg.skill.Resource.NONE, ClientPlayerData.viewer()));
                if (c != null && ClientPlayerData.rank(s.id) < max) {
                    int next = ClientPlayerData.rank(s.id) + 1;
                    tip.add("Ранг " + next + " (" + next + " очк.): сила +8%, перезарядка −4%, длительность +6%");
                }
            }
        }
        if (c == null) {
            UiKit.textCenter(g, font, "Рывок — Alt. Цель лечения — Ctrl+1…6.", left + W / 2, top + H - 20, UiKit.MUTED);
        }
        if (tip != null) {
            List<net.minecraft.util.FormattedCharSequence> out = new ArrayList<>();
            for (int i = 0; i < tip.size(); i++) {
                int color = i == 0 ? 0xFFE27A : i == 1 ? 0xE8F4FF : 0x8EA4BC;
                out.addAll(font.split(Component.literal(tip.get(i)).withColor(color), 230));
            }
            g.renderTooltip(font, out, mx, my);
        }
    }

    // ------------------------------------------------------------------ Расчёт

    private void renderBreakdown(GuiGraphics g, int mx, int my) {
        StatBreakdown b = ClientPlayerData.breakdown();
        int lx = left + 8, lw = 118, y = top + 30;
        int rows = Stat.ALL.length + Derived.values().length;
        for (int i = 0; i < rows; i++) {
            int ry = y + i * ROW;
            boolean sel = i == selected;
            boolean hover = UiKit.inside(mx, my, lx, ry - 1, lw, ROW);
            if (sel) g.fill(lx, ry - 1, lx + lw, ry + ROW - 1, 0x503FB6FF);
            else if (hover) g.fill(lx, ry - 1, lx + lw, ry + ROW - 1, 0x203FB6FF);
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
                if (UiKit.inside(mx, my, left + 8, top + 30 + i * ROW - 1, 118, ROW)) {
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
