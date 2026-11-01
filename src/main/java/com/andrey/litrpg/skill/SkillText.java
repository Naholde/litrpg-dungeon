package com.andrey.litrpg.skill;

import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.StatBreakdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Описание навыка из его шаблона и параметров: {@code {damage}} → «87» (готовое число для этого игрока).
 * Используется подсказками, окнами классов и Кодексом — везде одно и то же описание.
 */
public final class SkillText {
    private static final Pattern KEY = Pattern.compile("\\{([a-z_]+)}");

    private SkillText() {
    }

    /** Чьими глазами смотрим: уровень и расчёт характеристик (null — показать доли без чисел). */
    public record Viewer(int level, StatBreakdown stats) {
    }

    /** Текст шаблона с подставленными числами. Неизвестный ключ — исключение (ловит тест). */
    public static String body(Skill s, int rank, Viewer v) {
        Matcher m = KEY.matcher(s.template());
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String key = m.group(1);
            SkillParam p = s.params().get(key);
            if (p == null) throw new IllegalStateException(s.id + ": в описании {" + key + "}, а параметра нет");
            m.appendReplacement(sb, Matcher.quoteReplacement(format(p, s.value(key, rank), v)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Полная подсказка: название, тело, служебные строки. */
    public static List<String> tooltip(Skill s, int rank, Resource res, Viewer v) {
        List<String> out = new ArrayList<>();
        out.add(s.name + (rank > 0 ? "  · ранг " + rank : ""));
        out.add(body(s, rank, v));
        double cd = s.value("cooldown", rank), cast = s.value("cast", rank);
        StringBuilder meta = new StringBuilder();
        meta.append(s.passive() ? "Пассивно" : cast > 0 ? "Чтение " + sec(cast) : "Мгновенно");
        if (cd > 0) meta.append(" · перезарядка ").append(sec(cd));
        if (s.params().containsKey("range") && s.targeting() != Targeting.SELF) {
            meta.append(" · до ").append(Derived.trim(s.value("range", rank))).append(" бл.");
        }
        out.add(meta.toString());
        double cost = s.cost(rank);
        if (cost > 0 && res != Resource.NONE) out.add("Цена: " + Math.round(cost) + " — " + res.title.toLowerCase(Locale.ROOT));
        return out;
    }

    public static String format(SkillParam p, double value, Viewer v) {
        return switch (p.unit()) {
            case DAMAGE -> amount(value, v, true);
            case HEAL, SHIELD -> amount(value, v, false);
            case SECONDS -> sec(value);
            case PERCENT -> Derived.trim(value * 100) + "%";
            case BLOCKS -> Derived.trim(value) + " бл.";
            case COUNT -> String.valueOf(Math.round(value));
            case MULT -> "×" + Derived.trim2(value);
            case FLAT -> Derived.trim(value);
        };
    }

    /** Доля «единицы силы» → число для этого игрока (без крита); без игрока — проценты единицы. */
    private static String amount(double share, Viewer v, boolean damage) {
        if (v == null || v.stats() == null) return Math.round(share * 100) + "% силы";
        double unit = SkillMath.unit(v.level());
        double n = damage
                ? share * unit * (1 + v.stats().get(Derived.SKILL_POWER)) * v.stats().get(Derived.DAMAGE_MULT)
                : share * unit * v.stats().get(Derived.HEAL_POWER);
        return String.valueOf(Math.round(n));
    }

    private static String sec(double s) {
        return Derived.trim(s) + " с";
    }
}
