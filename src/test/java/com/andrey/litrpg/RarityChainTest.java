package com.andrey.litrpg;

import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Добыча с монстров: каждая следующая редкость в несколько раз реже предыдущей (решение владельца — в 3 раза).
 * Находки сдвигают редкость, но цепочку не ломают: даже с находками 0,6 ступень остаётся реже минимум вдвое.
 */
class RarityChainTest {
    private static int[] roll(double find, Rarity min, int n) {
        RandomSource rnd = RandomSource.create(42);
        int[] c = new int[Rarity.values().length];
        for (int i = 0; i < n; i++) c[GearGen.rollRarity(rnd, find, min).ordinal()]++;
        return c;
    }

    @Test
    void eachRarityThreeTimesRarer() {
        int[] c = roll(0, Rarity.COMMON, 400_000);
        for (int i = 1; i <= Rarity.LEGENDARY.ordinal(); i++) {
            double ratio = (double) c[i - 1] / c[i];
            assertTrue(ratio > 2.8 && ratio < 3.2, Rarity.values()[i] + ": реже предыдущей в " + ratio + " раз, а не в 3");
        }
        assertTrue(c[Rarity.DIVINE.ordinal()] == 0, "божественные с монстров не выпадают");
    }

    @Test
    void findKeepsTheChain() {
        int[] c = roll(0.6, Rarity.COMMON, 400_000);
        for (int i = 1; i <= Rarity.LEGENDARY.ordinal(); i++) {
            double ratio = (double) c[i - 1] / c[i];
            assertTrue(ratio > 2.0, "с находками 0,6 " + Rarity.values()[i] + " реже предыдущей лишь в " + ratio + " раз");
        }
    }

    @Test
    void floorKeepsTheChainAbove() {
        // Мини-хранитель: не хуже редкого, дальше та же лестница ×3.
        int[] c = roll(0, Rarity.RARE, 200_000);
        assertTrue(c[0] == 0 && c[1] == 0, "ниже порога не выпадает");
        double ratio = (double) c[Rarity.RARE.ordinal()] / c[Rarity.EPIC.ordinal()];
        assertTrue(ratio > 2.8 && ratio < 3.2, "редкий/эпический у чемпиона: " + ratio);
    }
}
