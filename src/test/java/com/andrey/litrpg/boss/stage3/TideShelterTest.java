package com.andrey.litrpg.boss.stage3;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Волна Стража Прилива: укрывает колонна внутри арены, а стена зала — нет (иначе в зале со стенами волна не бьёт никого). */
class TideShelterTest {
    private static final double R = 18;

    @Test
    void columnInsideArenaShelters() {
        assertTrue(TideWarden.shelters(8, 8, 0, 0, R), "колонна в 8,8 от центра");
        assertTrue(TideWarden.shelters(0, 13, 0, 0, R), "колонна у края, 13 от центра");
        assertTrue(TideWarden.shelters(0.6 * R, 0, 0, 0, R), "колонна «Колонн прилива» на 0,6 радиуса");
    }

    @Test
    void arenaWallDoesNotShelter() {
        assertFalse(TideWarden.shelters(19.5, 0, 0, 0, R), "стена квадратного зала (полуширина 19)");
        assertFalse(TideWarden.shelters(0, -18, 0, 0, R), "стена на самом радиусе");
        assertFalse(TideWarden.shelters(19, 19, 0, 0, R), "угол зала");
    }
}
