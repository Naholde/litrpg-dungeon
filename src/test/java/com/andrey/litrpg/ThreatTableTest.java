package com.andrey.litrpg;

import com.andrey.litrpg.combat.threat.ThreatTable;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreatTableTest {
    private final UUID tank = UUID.randomUUID(), dps = UUID.randomUUID(), healer = UUID.randomUUID();

    private UUID choose(ThreatTable t, long now, boolean dpsInMelee) {
        return t.choose(now, u -> u.equals(tank) || (dpsInMelee && u.equals(dps)), 1.1, 1.3);
    }

    @Test
    void firstHitterBecomesTarget() {
        ThreatTable t = new ThreatTable();
        t.add(dps, 10);
        assertEquals(dps, choose(t, 0, true));
    }

    @Test
    void switchNeedsTenPercentInMeleeThirtyAtRange() {
        ThreatTable t = new ThreatTable();
        t.add(tank, 100);
        assertEquals(tank, choose(t, 0, true));
        t.add(dps, 109);
        assertEquals(tank, choose(t, 0, true), "109 < 110 — цель не меняется");
        t.add(dps, 2);
        assertEquals(dps, choose(t, 0, true), "111 > 110 вблизи — сорвал агро");

        ThreatTable r = new ThreatTable();
        r.add(tank, 100);
        choose(r, 0, false);
        r.add(dps, 125);
        assertEquals(tank, choose(r, 0, false), "издалека нужно больше 130");
        r.add(dps, 6);
        assertEquals(dps, choose(r, 0, false));
    }

    @Test
    void tauntSticksForItsDuration() {
        ThreatTable t = new ThreatTable();
        t.add(dps, 1000);
        choose(t, 0, true);
        t.taunt(tank, 0, 80);
        assertEquals(tank, choose(t, 10, true));
        assertTrue(t.get(tank) > t.get(dps), "после провокации угроза танка выше всех");
        t.add(dps, 5000);
        assertEquals(tank, choose(t, 79, true), "пока действует провокация — бьёт танка");
        assertEquals(dps, choose(t, 81, true), "провокация кончилась — боец перебил угрозу");
    }

    @Test
    void decayForgetsAndRemoveDropsTarget() {
        ThreatTable t = new ThreatTable();
        t.add(healer, 0.02);
        t.decay(0.6);
        assertTrue(t.isEmpty(), "малая угроза растаяла");
        t.add(tank, 50);
        choose(t, 0, true);
        t.remove(tank);
        assertNull(choose(t, 0, true));
    }
}
