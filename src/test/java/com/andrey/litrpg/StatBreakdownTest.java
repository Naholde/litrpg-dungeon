package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.CharacterBalance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.RolesBalance;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import com.andrey.litrpg.player.StatBreakdown;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatBreakdownTest {
    private static StatBreakdown calc(Role role, int level, int... alloc) {
        int[] a = new int[Stat.ALL.length];
        System.arraycopy(alloc, 0, a, 0, alloc.length);
        return StatBreakdown.compute(new StatBreakdown.Inputs(role, level, a, StatSet.ZERO),
                Progression.DEFAULT, CharacterBalance.DEFAULT, RolesBalance.DEFAULT);
    }

    @Test
    void totalsAreSumsOfLines() {
        StatBreakdown b = calc(Role.TANK, 37, 10, 20, 30, 40, 50);
        for (Stat s : Stat.ALL) {
            assertEquals(b.lines(s).stream().mapToDouble(StatBreakdown.Line::value).sum(), b.get(s), 1e-9, s.name());
        }
        for (Derived d : Derived.values()) {
            assertEquals(b.lines(d).stream().mapToDouble(StatBreakdown.Line::value).sum(), b.get(d), 1e-9, d.name());
        }
    }

    @Test
    void levelOneNoviceHasBaseHealth() {
        // 100 основы + Выносливость 5 × 10 = 150
        assertEquals(150, calc(Role.NONE, 1).get(Derived.MAX_HEALTH), 1e-9);
    }

    @Test
    void allocatedPointIsWorthMoreThanHalfAStat() {
        // Решение владельца: очки должны решать, поэтому одно очко — 0,55 характеристики, а не 0,3.
        double before = calc(Role.DAMAGE, 10).get(Stat.STRENGTH);
        double after = calc(Role.DAMAGE, 10, 10).get(Stat.STRENGTH);
        assertEquals(10 * Progression.DEFAULT.statPointValue(), after - before, 1e-9);
        assertTrue(Progression.DEFAULT.statPointValue() >= 0.5);
    }

    @Test
    void tankIsMuchSturdierThanFighterAtFifty() {
        double tank = calc(Role.TANK, 50).get(Derived.MAX_HEALTH);
        double fighter = calc(Role.DAMAGE, 50).get(Derived.MAX_HEALTH);
        assertTrue(tank > fighter * 2, "танк " + tank + " vs боец " + fighter);
        assertTrue(tank > 1024, "здоровье танка должно перешагнуть ванильный предел 1024");
        assertEquals(0.5, calc(Role.TANK, 50).get(Derived.BOSS_TAKEN), 1e-9);
        assertEquals(5, calc(Role.TANK, 1).get(Derived.THREAT), 1e-9);
    }

    @Test
    void capsAreRespected() {
        // Даже все очки в Ловкость не доводят уклонение до предела — предел срабатывает с экипировкой.
        assertTrue(calc(Role.DAMAGE, 50, 0, 490).get(Derived.DODGE) <= CharacterBalance.DEFAULT.defense().dodgeCap());
        StatBreakdown b = StatBreakdown.compute(new StatBreakdown.Inputs(Role.DAMAGE, 50, new int[5],
                        new StatSet(0, 1000, 0, 0, 1000)),
                Progression.DEFAULT, CharacterBalance.DEFAULT, RolesBalance.DEFAULT);
        assertEquals(CharacterBalance.DEFAULT.defense().dodgeCap(), b.get(Derived.DODGE), 1e-9);
        assertTrue(b.get(Derived.CRIT_CHANCE) <= CharacterBalance.DEFAULT.crit().chanceCap() + 1e-9);
    }

    @Test
    void supportHealsFarBetter() {
        double support = calc(Role.SUPPORT, 20).get(Derived.HEAL_POWER);
        double fighter = calc(Role.DAMAGE, 20).get(Derived.HEAL_POWER);
        assertTrue(support > fighter * 3, support + " vs " + fighter);
    }
}
