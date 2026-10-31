package com.andrey.litrpg.player;

import java.util.Locale;

/** Производные величины персонажа. Все считаются в {@link StatBreakdown} — и для боя, и для окна. */
public enum Derived {
    MAX_HEALTH("Здоровье", Format.FLAT, "Сколько урона выдержишь"),
    WEAPON_POWER("Урон оружием", Format.BONUS, "Прибавка к урону мечом, луком, посохом"),
    SKILL_POWER("Сила навыков", Format.BONUS, "Прибавка к урону, лечению и щитам навыков"),
    DAMAGE_MULT("Множитель роли", Format.MULT, "Итоговый множитель своего урона от роли"),
    CRIT_CHANCE("Шанс крита", Format.PERCENT, "Шанс нанести критический удар"),
    CRIT_DAMAGE("Сила крита", Format.MULT, "Во сколько раз крит сильнее обычного удара"),
    DODGE("Уклонение", Format.PERCENT, "Шанс полностью избежать удара монстра (не разметки хранителя)"),
    TOUGHNESS("Стойкость", Format.PERCENT, "Доля, на которую снижается любой полученный урон"),
    HEAL_POWER("Сила лечения", Format.MULT, "Множитель исходящего лечения"),
    HASTE("Ускорение", Format.PERCENT, "Насколько быстрее восстанавливаются навыки"),
    MOVE_SPEED("Скорость", Format.BONUS, "Прибавка к скорости бега"),
    THREAT("Угроза", Format.MULT, "Множитель угрозы: насколько охотнее враги бьют тебя"),
    BOSS_TAKEN("Урон от хранителей", Format.MULT, "Множитель урона, который ты получаешь от хранителей"),
    FIND("Находки", Format.BONUS, "Прибавка к шансу и редкости добычи");

    public enum Format { FLAT, PERCENT, BONUS, MULT }

    public final String title;
    public final Format format;
    public final String hint;

    Derived(String title, Format format, String hint) {
        this.title = title;
        this.format = format;
        this.hint = hint;
    }

    /** Значение для показа: «1 240», «12,5%», «+18%», «×1,30». */
    public String format(double v) {
        return switch (format) {
            case FLAT -> String.format(Locale.ROOT, "%,d", Math.round(v)).replace(',', ' ');
            case PERCENT -> trim(v * 100) + "%";
            case BONUS -> (v >= 0 ? "+" : "−") + trim(Math.abs(v) * 100) + "%";
            case MULT -> "×" + String.format(Locale.ROOT, "%.2f", v).replace('.', ',');
        };
    }

    /** Строка-слагаемое в «Расчёте»: всегда со знаком. */
    public String formatDelta(double v) {
        String sign = v >= 0 ? "+" : "−";
        double a = Math.abs(v);
        return switch (format) {
            case FLAT -> sign + String.format(Locale.ROOT, "%,d", Math.round(a)).replace(',', ' ');
            case PERCENT, BONUS -> sign + trim(a * 100) + "%";
            case MULT -> sign + String.format(Locale.ROOT, "%.2f", a).replace('.', ',');
        };
    }

    /** До двух знаков после запятой без лишних нулей: 1,15 · 0,6 · 5. */
    public static String trim2(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) s = s.substring(0, s.length() - 1);
        return s.replace('.', ',');
    }

    public static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.1f", v);
        if (s.endsWith(".0")) s = s.substring(0, s.length() - 2);
        return s.replace('.', ',');
    }
}
