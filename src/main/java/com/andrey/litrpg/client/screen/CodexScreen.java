package com.andrey.litrpg.client.screen;

import com.andrey.litrpg.boss.BossGuide;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.client.ui.SysButton;
import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.gear.Trophies;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.run.Layout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Кодекс Системы (клавиша J): правила, этапы, хранители с тактикой, бестиарий, трофеи. Всё собирается из данных
 * мода (схема подземелья, реестры хранителей и видов, трофеи) — Кодекс не расходится с игрой.
 */
public final class CodexScreen extends Screen {
    private static final String[] TABS = {"Правила", "Этапы", "Хранители", "Бестиарий", "Трофеи"};
    private static final int W = 360, H = 240;
    private static int tab;
    private int left, top, scroll;
    private final List<SysButton> tabs = new ArrayList<>();
    private List<Line> lines = List.of();

    private record Line(FormattedCharSequence text, int color) {
    }

    public CodexScreen() {
        super(Component.literal("Кодекс Системы"));
    }

    /** Сразу на вкладке {@code t} (dev-проверка окна). */
    public CodexScreen(int t) {
        this();
        tab = Math.max(0, Math.min(TABS.length - 1, t));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabs.clear();
        int tw = (W - 12) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int k = i;
            tabs.add(addRenderableWidget(new SysButton(left + 6 + i * tw, top + 20, tw - 3, 15, TABS[i], () -> {
                tab = k;
                scroll = 0;
                rebuild();
            })));
        }
        rebuild();
    }

    private void rebuild() {
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).selected = i == tab;
        List<Line> out = new ArrayList<>();
        int wrap = W - 24;
        java.util.function.BiConsumer<String, Integer> add = (s, c) -> {
            if (s.isEmpty()) {
                out.add(new Line(FormattedCharSequence.EMPTY, c));
                return;
            }
            for (FormattedCharSequence l : font.split(Component.literal(s), wrap)) out.add(new Line(l, c));
        };
        switch (tab) {
            case 0 -> rules(add);
            case 1 -> stages(add);
            case 2 -> guardians(add);
            case 3 -> bestiary(add);
            default -> trophies(add);
        }
        lines = out;
    }

    private static void rules(java.util.function.BiConsumer<String, Integer> add) {
        add.accept("Подземелье Системы", UiKit.GOLD);
        add.accept("Отряд 3–6 игроков проходит пять этапов по 10 уровней — от 1 до 50. Размер отряда не меняется до конца забега, под него подстраиваются хранители.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Цитадель Искателей (лобби)", UiKit.GOLD);
        add.accept("Отряд собирается на Площади: ПКМ по Печати или клавиша P — окно сбора. Выберите сложность (Обычная — враги слабее; Героическая — полная сила, добыча лучше, свои рекорды) и нажмите «Готов». Когда готовы все в лобби — через 5 секунд забег. Каждый забег — с 1 уровня: уровень, класс и вещи начинаются заново, а статистика, рекорды, трофеи и титулы — вечны.", UiKit.TEXT);
        add.accept("Пока ждёте друзей: Зал славы (рекорды), Зал трофеев (чучела побеждённых хранителей), Полигон (Испытатель учит уворачиваться — 20 подряд дают титул), паркур Башни ветров (рекорд — титул). Свои титулы — /litrpg title, статистика — /litrpg stats.", UiKit.MUTED);
        add.accept("", 0);
        add.accept("Роли", UiKit.GOLD);
        add.accept("Роль выбирают уже в подземелье — в Зале ролей на привале этапа 1 (клавиша K); путь вниз откроется, когда выберут все. Танк держит врагов провокацией и стоит к ним лицом. Боец наносит урон и не стоит в разметке. Поддержка лечит: Ctrl+1…6 — выбрать союзника. Без всех трёх ролей пройти почти невозможно.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Давление хранителей", UiKit.GOLD);
        add.accept("Каждый хранитель, кроме своих приёмов, давит на весь отряд. Волна (расходится кольцом от хранителя) бьёт всех — уйти нельзя, лекарь держит отряд. Метка ложится на бойца: она жжёт его, а когда круг под ним заполнится — бьёт всех, кто стоит рядом. Меченый — отойди от своих. Чем дольше бой, тем сильнее неистовство: +15% урона хранителя за минуту.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Ритм боя", UiKit.GOLD);
        add.accept("Хранитель начинает приём раз в 3–5 секунд и никогда — пока горит замах прошлого: разметки не ложатся одна на другую. Страж Прилива устроен иначе: в прилив бьёт без остановки и почти неуязвим, а в отлив выдыхается — «Отлив — бейте!», урон по нему ×2,5. Копите сильные навыки к отливу.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Опыт и класс", UiKit.GOLD);
        add.accept("Опыт этапа точный: половина — за отряды, почти половина — за хранителей, немного — за загадки и секретки. Награда выдаётся один раз. На 10 уровне Система предлагает три класса по вашему стилю игры.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Гибель и вайп", UiKit.GOLD);
        add.accept("Павший становится духом у места гибели. Союзник воскрешает, зажав Shift рядом (в бою дольше). Хранитель повержен — павшие возвращаются сами. Пали все — этап заново: уровни и вещи остаются, повторные победы наград не дают.", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Мир", UiKit.GOLD);
        add.accept("Ломать можно только треснувшие стены, паутину и горшки — за ними бывают секретки. Упавшего в пропасть или лаву Система вытаскивает ценой части здоровья. Тайник каждый открывает сам и получает своё; запертые — ключом (торговец, мини-хранители).", UiKit.TEXT);
        add.accept("", 0);
        add.accept("Загадки", UiKit.GOLD);
        add.accept("Плиты — встать на все разом (придётся разделиться). Рычаги — найти все. Кнопки — по подсказке на табличке. Столбы памяти — повторить вспышки. Текстовая загадка — ответ написать в чат, стоя у алтаря.", UiKit.TEXT);
    }

    private static void stages(java.util.function.BiConsumer<String, Integer> add) {
        for (Layout.Stage st : Layout.get().stages()) {
            add.accept("Этап " + st.n() + " · " + st.name() + "  (уровни " + st.fromLevel() + "–" + st.toLevel() + ")", UiKit.GOLD);
            StringBuilder b = new StringBuilder("Хранители: ");
            for (int i = 0; i < st.bosses().size(); i++) {
                var t = BossRegistry.get(st.bosses().get(i).type());
                b.append(i > 0 ? ", " : "").append(t != null ? t.name() : st.bosses().get(i).type());
            }
            add.accept(b.toString(), UiKit.TEXT);
            long puzzles = st.mechs().stream().filter(Layout.Mech::puzzle).count();
            long traps = st.mechs().size() - puzzles;
            long minis = st.packs().stream().filter(p -> p.title().isPresent()).count();
            add.accept("Отрядов: " + st.packs().size() + " (мини-хранителей: " + minis + "), загадок: " + puzzles + ", ловушек: " + traps
                    + ", тайников: " + st.caches().size() + ".", UiKit.MUTED);
            add.accept("", 0);
        }
    }

    private static void guardians(java.util.function.BiConsumer<String, Integer> add) {
        for (Layout.Stage st : Layout.get().stages()) {
            add.accept("— " + st.name() + " —", UiKit.MUTED);
            for (Layout.Boss b : st.bosses()) {
                var t = BossRegistry.get(b.type());
                if (t == null) continue;
                add.accept(t.name() + "  · ур. " + Math.round(t.params().get("level")) + (b.last() ? "  · финал этапа" : ""), UiKit.GOLD);
                BossGuide.Entry e = BossGuide.of(t.id());
                if (!e.quote().isEmpty()) add.accept("«" + e.quote() + "»", 0xFFC8A0A0);
                add.accept(e.tactic(), UiKit.TEXT);
                BossGuide.Pressure pr = BossGuide.pressure(t.id());
                if (pr != null) add.accept("Давление: волна «" + pr.pulse() + "», метка «" + pr.mark() + "».", 0xFFB8A6FF);
                add.accept("", 0);
            }
        }
    }

    private static void bestiary(java.util.function.BiConsumer<String, Integer> add) {
        int stage = -1;
        for (Bestiary.Species s : Bestiary.all()) {
            if (s == Bestiary.SHADOW) continue;
            if (s.stage() != stage) {
                stage = s.stage();
                int n = stage;
                String name = Layout.get().stage(n).map(Layout.Stage::name).orElse("Этап " + n);
                add.accept("— " + name + " —", UiKit.MUTED);
            }
            add.accept(s.name() + "  · " + roleName(s), UiKit.GOLD);
            add.accept(s.trait(), UiKit.TEXT);
        }
    }

    private static String roleName(Bestiary.Species s) {
        return switch (s.role()) {
            case BRUTE -> "громила";
            case SKIRMISHER -> "быстрый";
            case ARCHER -> "стрелок";
            case CASTER -> "заклинатель";
            case HEALER -> "лекарь";
            case GUARD -> "страж";
            case SWARM -> "рой";
        };
    }

    private static void trophies(java.util.function.BiConsumer<String, Integer> add) {
        add.accept("С каждого хранителя — трофей под вашу роль или украшение. У трофея два фирменных свойства сильнее обычных.", UiKit.MUTED);
        add.accept("", 0);
        for (var e : Trophies.all().entrySet()) {
            var t = BossRegistry.get(e.getKey());
            add.accept(t != null ? t.name() : e.getKey(), UiKit.GOLD);
            for (var tr : e.getValue()) {
                String who = tr.role() == Role.NONE ? "любому" : tr.role() == Role.TANK ? "танку" : tr.role() == Role.DAMAGE ? "бойцу" : "поддержке";
                add.accept("  " + tr.name() + " (" + tr.kind().title.toLowerCase() + ", " + who + ")", UiKit.TEXT);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int visible = (H - 50) / 10;
        scroll = Math.max(0, Math.min(Math.max(0, lines.size() - visible), scroll - (int) Math.signum(dy) * 3));
        return true;
    }

    /** Фон — затемнение и панель (ванильное размытие рисуется поверх уже нарисованного — не нужно). */
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        UiKit.panel(g, left, top, W, H);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        UiKit.textCenter(g, font, "КОДЕКС СИСТЕМЫ", left + W / 2, top + 7, UiKit.GOLD);
        int y = top + 42, visible = (H - 50) / 10;
        for (int i = scroll; i < Math.min(lines.size(), scroll + visible); i++) {
            Line l = lines.get(i);
            g.drawString(font, l.text(), left + 12, y, l.color(), false);
            y += 10;
        }
        if (lines.size() > visible) {
            int barH = Math.max(10, (H - 50) * visible / lines.size());
            int barY = top + 42 + (H - 50 - barH) * scroll / Math.max(1, lines.size() - visible);
            g.fill(left + W - 6, barY, left + W - 4, barY + barH, UiKit.FRAME_DIM);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
