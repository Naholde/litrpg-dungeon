package com.andrey.litrpg.player;

import java.util.Map;

/**
 * От какой характеристики растут навыки класса (решение владельца: мечнику странно качать интеллект).
 * <ul>
 *   <li>танки — сила и выносливость: держат удар и бьют весом;</li>
 *   <li>ближний бой — сила, а быстрые классы (Стриж, Тень, Монах) — ловкость;</li>
 *   <li>стрелки — ловкость;</li>
 *   <li>колдуны и поддержка — интеллект.</li>
 * </ul>
 * Очки у всех одни и те же, поэтому класс с двумя характеристиками не слабее: складываются обе.
 */
public final class SkillStat {
    private static final Stat[] STR = {Stat.STRENGTH};
    private static final Stat[] AGI = {Stat.AGILITY};
    private static final Stat[] INT = {Stat.INTELLECT};
    private static final Stat[] TANK = {Stat.STRENGTH, Stat.VITALITY};

    private static final Map<String, Stat[]> BY_CLASS = Map.ofEntries(
            // танки
            Map.entry("guardian", TANK), Map.entry("blood_knight", TANK), Map.entry("mirror", TANK),
            Map.entry("graviton", TANK), Map.entry("bastion", TANK), Map.entry("paladin", TANK),
            // ближний бой
            Map.entry("berserker", STR), Map.entry("reaper", STR), Map.entry("harpooner", STR),
            Map.entry("headsman", STR),
            // быстрые
            Map.entry("swift", AGI), Map.entry("shadow", AGI), Map.entry("monk", AGI),
            // стрелки и станки
            Map.entry("sniper", AGI), Map.entry("mechanic", AGI),
            // колдуны
            Map.entry("pyromancer", INT), Map.entry("cryomancer", INT), Map.entry("stormcaller", INT),
            Map.entry("plague_lord", INT), Map.entry("astromancer", INT), Map.entry("summoner", INT),
            Map.entry("illusionist", INT), Map.entry("warlock", INT), Map.entry("rifter", INT),
            // поддержка
            Map.entry("cleric", INT), Map.entry("chronolog", INT), Map.entry("shadow_priest", INT),
            Map.entry("druid", INT), Map.entry("shaman", INT), Map.entry("bannerman", INT));

    private SkillStat() {
    }

    /** Характеристики, от которых растут навыки. Класса ещё нет — считаем по роли. */
    public static Stat[] of(String classId, Role role) {
        Stat[] byClass = classId == null ? null : BY_CLASS.get(classId);
        if (byClass != null) return byClass;
        return switch (role) {
            case TANK -> TANK;
            case SUPPORT -> INT;
            default -> STR;
        };
    }
}
