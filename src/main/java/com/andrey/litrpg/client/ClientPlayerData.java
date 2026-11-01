package com.andrey.litrpg.client;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.net.PlayerSyncPayload;
import com.andrey.litrpg.player.Leveling;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.StatBreakdown;
import com.andrey.litrpg.player.Stat;

import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillText;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Копия данных своего персонажа на клиенте. Характеристики считаются тем же {@link StatBreakdown}, что и на сервере. */
public final class ClientPlayerData {
    private static Role role = Role.NONE;
    private static int level = 1;
    private static long xp;
    private static int[] allocated = new int[Stat.ALL.length];
    private static String classId = "";
    private static Map<String, Integer> ranks = Map.of();
    private static List<String> offers = List.of();
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
        classId = p.classId();
        ranks = Map.copyOf(p.ranks());
        offers = List.copyOf(p.offers());
        received = true;
        cached = null;
    }

    public static void reset() {
        role = Role.NONE;
        level = 1;
        xp = 0;
        allocated = new int[Stat.ALL.length];
        classId = "";
        ranks = Map.of();
        offers = List.of();
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

    public static String classId() {
        return classId;
    }

    public static PlayerClass playerClass() {
        return ClassRegistry.get(classId);
    }

    public static int rank(String skillId) {
        return ranks.getOrDefault(skillId, 0);
    }

    public static List<String> offers() {
        return offers;
    }

    public static List<Skill> loadout() {
        return ClassRegistry.loadout(role, classId, level);
    }

    public static int freeClassPoints() {
        int spent = ranks.values().stream().mapToInt(r -> r * (r + 1) / 2).sum();
        return Leveling.classPointsEarned(level, progression()) - spent;
    }

    public static SkillText.Viewer viewer() {
        return new SkillText.Viewer(level, breakdown());
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

    private static com.andrey.litrpg.gear.PlayerGear.Totals cachedGear;

    public static StatBreakdown breakdown() {
        var gear = gear();
        if (cached == null || cachedGeneration != Balance.generation() || !gear.equals(cachedGear)) {
            cachedGear = gear;
            cached = compute(allocated);
            cachedGeneration = Balance.generation();
        }
        return cached;
    }

    private static com.andrey.litrpg.gear.PlayerGear.Totals gear() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        return mc.player != null ? com.andrey.litrpg.gear.PlayerGear.collect(mc.player) : com.andrey.litrpg.gear.PlayerGear.Totals.NONE;
    }

    /** Расчёт «как будет», если вложить ещё {@code extra} очков — для предпросмотра в окне персонажа. */
    public static StatBreakdown preview(int[] extra) {
        int[] a = allocated.clone();
        for (int i = 0; i < a.length; i++) a[i] += extra[i];
        return compute(a);
    }

    private static StatBreakdown compute(int[] alloc) {
        PlayerClass c = playerClass();
        var gear = gear();
        return StatBreakdown.compute(new StatBreakdown.Inputs(role, level, alloc, gear.stats(),
                        c != null ? c.growth() : null, c != null ? c.name() : null, gear.derived(), gear.armor()),
                Balance.get(Balance.PROGRESSION), Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES));
    }
}
