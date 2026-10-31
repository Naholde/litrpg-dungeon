package com.andrey.litrpg.client;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.net.PlayerSyncPayload;
import com.andrey.litrpg.player.Leveling;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.StatBreakdown;
import com.andrey.litrpg.player.Stat;

import java.util.Arrays;

/** Копия данных своего персонажа на клиенте. Характеристики считаются тем же {@link StatBreakdown}, что и на сервере. */
public final class ClientPlayerData {
    private static Role role = Role.NONE;
    private static int level = 1;
    private static long xp;
    private static int[] allocated = new int[Stat.ALL.length];
    private static boolean received;

    private static StatBreakdown cached;
    private static int cachedGeneration = -1;

    private ClientPlayerData() {
    }

    public static void update(PlayerSyncPayload p) {
        role = p.role();
        level = p.level();
        xp = p.xp();
        allocated = Arrays.copyOf(p.allocated(), Stat.ALL.length);
        received = true;
        cached = null;
    }

    public static void reset() {
        role = Role.NONE;
        level = 1;
        xp = 0;
        allocated = new int[Stat.ALL.length];
        received = false;
        cached = null;
    }

    public static boolean received() {
        return received;
    }

    public static Role role() {
        return role;
    }

    public static int level() {
        return level;
    }

    public static long xp() {
        return xp;
    }

    public static int allocated(Stat s) {
        return allocated[s.ordinal()];
    }

    public static Progression progression() {
        return Balance.get(Balance.PROGRESSION);
    }

    public static long xpToNext() {
        return progression().xpToNext(level);
    }

    public static int freeStatPoints() {
        return Leveling.statPointsEarned(level, progression()) - Arrays.stream(allocated).sum();
    }

    public static StatBreakdown breakdown() {
        if (cached == null || cachedGeneration != Balance.generation()) {
            cached = compute(allocated);
            cachedGeneration = Balance.generation();
        }
        return cached;
    }

    /** Расчёт «как будет», если вложить ещё {@code extra} очков — для предпросмотра в окне персонажа. */
    public static StatBreakdown preview(int[] extra) {
        int[] a = allocated.clone();
        for (int i = 0; i < a.length; i++) a[i] += extra[i];
        return compute(a);
    }

    private static StatBreakdown compute(int[] alloc) {
        return StatBreakdown.compute(new StatBreakdown.Inputs(role, level, alloc, StatSet.ZERO),
                Balance.get(Balance.PROGRESSION), Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES));
    }
}
