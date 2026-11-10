package com.andrey.litrpg.run;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Ловушки: какие части области бьют в цикл. Ярусы (pattern 3) — для подъёмов: шахта 2×2 высотой 12 с ярусом 2
 * бьёт тремя ярусами через один, в следующий цикл — тремя другими; вместе — вся шахта, без нахлёста.
 */
class HazardPartsTest {
    private static int cells(List<Layout.Box> parts) {
        int n = 0;
        for (Layout.Box b : parts) n += (b.x2() - b.x1() + 1) * (b.y2() - b.y1() + 1) * (b.z2() - b.z1() + 1);
        return n;
    }

    @Test
    void layersAlternateAndCoverShaft() {
        Layout.Box shaft = new Layout.Box(0, 70, 0, 1, 81, 1);
        List<Layout.Box> even = Mechanisms.hazardParts(shaft, 3, 2, 0), odd = Mechanisms.hazardParts(shaft, 3, 2, 1);
        assertEquals(3, even.size());
        assertEquals(3, odd.size());
        assertEquals(70, even.get(0).y1());
        assertEquals(71, even.get(0).y2());
        assertEquals(72, odd.get(0).y1());
        for (Layout.Box b : even) {
            assertEquals(0, b.x1());
            assertEquals(1, b.z2());
        }
        assertEquals(2 * 2 * 12, cells(even) + cells(odd));
        // через цикл — снова те же ярусы
        assertEquals(even, Mechanisms.hazardParts(shaft, 3, 2, 2));
    }

    @Test
    void stripesAndWholeUnchanged() {
        Layout.Box hall = new Layout.Box(0, 64, 0, 9, 64, 5);
        assertEquals(List.of(hall), Mechanisms.hazardParts(hall, 0, 1, 7));
        List<Layout.Box> alongX = Mechanisms.hazardParts(hall, 1, 2, 0);
        assertEquals(List.of(new Layout.Box(0, 64, 0, 9, 64, 1), new Layout.Box(0, 64, 4, 9, 64, 5)), alongX);
        List<Layout.Box> alongZ = Mechanisms.hazardParts(hall, 2, 3, 1);
        assertEquals(List.of(new Layout.Box(3, 64, 0, 5, 64, 5), new Layout.Box(9, 64, 0, 9, 64, 5)), alongZ);
    }
}
