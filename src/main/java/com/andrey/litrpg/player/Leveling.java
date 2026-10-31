package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.Progression;

/** Чистая логика опыта и очков — без Minecraft, проверяется обычными тестами. */
public final class Leveling {
    private Leveling() {
    }

    /** Результат начисления опыта. */
    public record Result(int level, long xp, int levelsGained) {
    }

    /** Добавить опыт: переливается через уровни; на максимуме опыт обнуляется. */
    public static Result addXp(int level, long xp, long amount, Progression p) {
        int start = level;
        if (level >= p.maxLevel()) return new Result(p.maxLevel(), 0, 0);
        xp = Math.max(0, xp + amount);
        while (level < p.maxLevel() && xp >= p.xpToNext(level)) {
            xp -= p.xpToNext(level);
            level++;
        }
        if (level >= p.maxLevel()) xp = 0;
        return new Result(level, xp, level - start);
    }

    /** Очки характеристик, заработанные к уровню. */
    public static int statPointsEarned(int level, Progression p) {
        return Math.max(0, level - 1) * p.statPointsPerLevel();
    }

    /** Очки класса, заработанные к уровню: начиная с уровня получения класса. */
    public static int classPointsEarned(int level, Progression p) {
        return Math.max(0, level - p.classLevel() + 1) * p.classPointsPerLevel();
    }
}
