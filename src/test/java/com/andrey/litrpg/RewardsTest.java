package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.run.Layout;
import com.andrey.litrpg.run.Rewards;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Опыт этапа: отряды + хранители дают ровно столько, сколько нужно на уровни этапа (с точностью округления). */
class RewardsTest {
    @Test
    void stageXpAddsUpToItsLevels() {
        Layout layout = Layout.get();
        assertFalse(layout.stages().isEmpty());
        int expectFrom = 1;
        for (Layout.Stage st : layout.stages()) {
            assertEquals(expectFrom, st.fromLevel(), "этап " + st.n() + " начинается там, где кончился прошлый");
            assertEquals(st.fromLevel() == 1 ? 10 : st.fromLevel() + 10, st.toLevel(), "этап " + st.n() + " — 10 уровней");
            expectFrom = st.toLevel();
            long sum = st.packs().stream().mapToLong(p -> Rewards.packXp(st, p)).sum() + Rewards.bossXp(st) * st.bosses().size();
            long budget = Rewards.budget(st);
            assertEquals(Balance.get(Balance.PROGRESSION).xpBetween(st.fromLevel(), st.toLevel()), budget);
            assertTrue(Math.abs(sum - budget) <= st.packs().size() + st.bosses().size(),
                    "этап " + st.n() + ": награды " + sum + " ≠ бюджет " + budget);
        }
    }
}
