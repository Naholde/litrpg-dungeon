package com.andrey.litrpg;

import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusList;
import com.andrey.litrpg.status.StatusType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusListTest {
    private final UUID a = UUID.randomUUID(), b = UUID.randomUUID();

    @Test
    void sameKeyAndSourceRefreshesNotStacks() {
        StatusList l = new StatusList();
        l.add(Status.of(StatusType.DAMAGE_TAKEN, "mark", a, 1.2, 0, 100));
        l.add(Status.of(StatusType.DAMAGE_TAKEN, "mark", a, 1.2, 50, 100));
        assertEquals(1.2, l.product(StatusType.DAMAGE_TAKEN, 60), 1e-9, "обновилось, а не стало 1,44");
        l.add(Status.of(StatusType.DAMAGE_TAKEN, "mark", b, 1.2, 50, 100));
        assertEquals(1.44, l.product(StatusType.DAMAGE_TAKEN, 60), 1e-9, "от другого источника — перемножается");
    }

    @Test
    void expiresOnTime() {
        StatusList l = new StatusList();
        l.add(Status.of(StatusType.STUN, "s", a, 1, 0, 20));
        assertTrue(l.has(StatusType.STUN, 19));
        assertFalse(l.has(StatusType.STUN, 20));
        assertTrue(l.expire(20));
        assertTrue(l.isEmpty());
    }

    @Test
    void shieldsAbsorbInOrderAndBreak() {
        StatusList l = new StatusList();
        l.add(Status.of(StatusType.SHIELD, "s1", a, 30, 0, 200));
        l.add(Status.of(StatusType.SHIELD, "s2", b, 50, 0, 200));
        assertEquals(0, l.absorb(20, 1), 1e-9);
        assertEquals(60, l.shieldTotal(1), 1e-9);
        assertEquals(15, l.absorb(75, 1), 1e-9, "щиты на 60 — прошло 15");
        assertEquals(0, l.shieldTotal(1), 1e-9);
        assertTrue(l.all().stream().noneMatch(s -> s.type() == StatusType.SHIELD), "разбитые щиты убраны");
    }
}
