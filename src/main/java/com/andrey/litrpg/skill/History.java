package com.andrey.litrpg.skill;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Короткая память о игроках: сколько у них было здоровья полсекунды назад, секунду, пять — и сколько времени
 * игрок не сходил с места. Нужна хронологу («вернуть здоровье на 5 с назад») и снайперу («чем дольше стоял,
 * тем больнее»). Пишется раз в полсекунды, хранит 12 секунд.
 */
public final class History {
    private static final int SLOTS = 24;
    private static final int STEP = 10; // тиков между записями

    private static final class Entry {
        final float[] health = new float[SLOTS];
        int next;
        boolean filled;
        Vec3 lastPos = Vec3.ZERO;
        long movedAt;
    }

    private static final Map<UUID, Entry> ALL = new HashMap<>();

    private History() {
    }

    /** Раз в полсекунды: запомнить здоровье и заметить движение. */
    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (now % STEP != 0) return;
        for (ServerPlayer p : level.players()) {
            Entry e = ALL.computeIfAbsent(p.getUUID(), id -> new Entry());
            e.health[e.next] = p.getHealth();
            e.next = (e.next + 1) % SLOTS;
            if (e.next == 0) e.filled = true;
            if (p.position().distanceToSqr(e.lastPos) > 0.01) {
                e.lastPos = p.position();
                e.movedAt = now;
            }
        }
    }

    /** Здоровье игрока {@code seconds} секунд назад (нет записи — сколько сейчас). */
    public static double healthAgo(ServerPlayer p, double seconds) {
        Entry e = ALL.get(p.getUUID());
        if (e == null) return p.getHealth();
        int back = (int) Math.round(seconds * 20 / STEP);
        int have = e.filled ? SLOTS : e.next;
        if (have == 0) return p.getHealth();
        back = Math.min(back, have);
        int idx = ((e.next - back) % SLOTS + SLOTS) % SLOTS;
        float v = e.health[idx];
        return v > 0 ? v : p.getHealth();
    }

    /** Сколько секунд игрок стоит на месте (0 — только что двигался). */
    public static double stillSeconds(ServerPlayer p) {
        Entry e = ALL.get(p.getUUID());
        if (e == null) return 0;
        return Math.max(0, (p.level().getGameTime() - e.movedAt) / 20.0);
    }

    public static void forget(ServerPlayer p) {
        ALL.remove(p.getUUID());
    }

    public static void clear() {
        ALL.clear();
    }
}
