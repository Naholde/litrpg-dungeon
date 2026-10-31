package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.player.Leveling;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionTest {
    private final Progression p = Progression.DEFAULT;

    @Test
    void xpFormula() {
        assertEquals(190, p.xpToNext(1)); // 100 + 90 × 1^1,85
        assertEquals(0, p.xpToNext(p.maxLevel()), "на максимальном уровне опыт не нужен");
        for (int l = 1; l < p.maxLevel() - 1; l++) {
            assertTrue(p.xpToNext(l + 1) > p.xpToNext(l), "опыт до уровня растёт: " + l);
        }
    }

    @Test
    void xpOverflowsThroughSeveralLevels() {
        long toThree = p.xpToNext(1) + p.xpToNext(2);
        var r = Leveling.addXp(1, 0, toThree + 5, p);
        assertEquals(3, r.level());
        assertEquals(5, r.xp());
        assertEquals(2, r.levelsGained());
    }

    @Test
    void xpStopsAtMaxLevel() {
        var r = Leveling.addXp(p.maxLevel() - 1, 0, Long.MAX_VALUE / 4, p);
        assertEquals(p.maxLevel(), r.level());
        assertEquals(0, r.xp());
        assertEquals(0, Leveling.addXp(p.maxLevel(), 0, 1000, p).levelsGained());
    }

    @Test
    void pointsByLevel() {
        assertEquals(0, Leveling.statPointsEarned(1, p));
        assertEquals(490, Leveling.statPointsEarned(50, p));
        assertEquals(0, Leveling.classPointsEarned(9, p));
        assertEquals(2, Leveling.classPointsEarned(10, p));
        assertEquals(82, Leveling.classPointsEarned(50, p));
    }
}
