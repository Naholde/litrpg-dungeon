package com.andrey.litrpg.run;

import com.andrey.litrpg.skill.prop.ClassProp;
import com.andrey.litrpg.skill.prop.SummonEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Статистика забега (обновление 15): на каждого участника — урон, лечение, принятый урон, смерти, воскрешения,
 * убитые монстры, самый сильный удар; на отряд — время этапов и боёв с хранителями. Живёт в {@link RunState}
 * (переживает перезапуск сервера), в конце забега уходит в вечные {@link Records}.
 * Считается только участникам идущего забега — тренировки в лобби на рекорды не влияют.
 */
public final class RunStats {
    private RunStats() {
    }

    /** Строка участника. */
    public static final class Line {
        public double dealt, healed, taken, biggestHit;
        public int deaths, revives, kills, caches;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putDouble("dealt", dealt);
            t.putDouble("healed", healed);
            t.putDouble("taken", taken);
            t.putDouble("biggest", biggestHit);
            t.putInt("deaths", deaths);
            t.putInt("revives", revives);
            t.putInt("kills", kills);
            t.putInt("caches", caches);
            return t;
        }

        static Line load(CompoundTag t) {
            Line l = new Line();
            l.dealt = t.getDouble("dealt");
            l.healed = t.getDouble("healed");
            l.taken = t.getDouble("taken");
            l.biggestHit = t.getDouble("biggest");
            l.deaths = t.getInt("deaths");
            l.revives = t.getInt("revives");
            l.kills = t.getInt("kills");
            l.caches = t.getInt("caches");
            return l;
        }
    }

    static ListTag save(Map<UUID, Line> lines) {
        ListTag l = new ListTag();
        lines.forEach((id, line) -> {
            CompoundTag e = line.save();
            e.putUUID("id", id);
            l.add(e);
        });
        return l;
    }

    static void load(ListTag list, Map<UUID, Line> into) {
        for (Tag t : list) {
            CompoundTag e = (CompoundTag) t;
            into.put(e.getUUID("id"), Line.load(e));
        }
    }

    static CompoundTag saveTimes(Map<String, Long> times) {
        CompoundTag t = new CompoundTag();
        times.forEach(t::putLong);
        return t;
    }

    static void loadTimes(CompoundTag t, Map<String, Long> into) {
        for (String k : t.getAllKeys()) into.put(k, t.getLong(k));
    }

    // ------------------------------------------------------------------ кому засчитать

    /** Игрок, которому засчитывается действие сущности: сам игрок, хозяин призыва или тотема. */
    public static ServerPlayer creditOf(Entity e) {
        if (e instanceof ServerPlayer p) return p;
        if (e instanceof SummonEntity s) return s.ownerPlayer();
        if (e instanceof ClassProp c && c.ownerId() != null && c.getServer() != null) {
            return c.getServer().getPlayerList().getPlayer(c.ownerId());
        }
        return null;
    }

    /** Строка участника идущего забега (null — не участник или забега нет). */
    private static Line line(ServerPlayer p) {
        if (p == null || p.server == null) return null;
        MinecraftServer server = p.server;
        if (server instanceof net.minecraft.gametest.framework.GameTestServer && !RunService.testing) return null;
        RunState s = RunService.state(server);
        if (!s.active(Layout.get().lastStage()) || !s.member(p.getUUID())) return null;
        s.setDirty();
        return s.stats.computeIfAbsent(p.getUUID(), id -> new Line());
    }

    // ------------------------------------------------------------------ крючки

    /** Удар по врагу (после брони): {@code attacker} — игрок, его призыв или тотем. */
    public static void dealt(Entity attacker, LivingEntity victim, double amount) {
        if (amount <= 0 || victim instanceof ServerPlayer) return;
        Line l = line(creditOf(attacker));
        if (l == null) return;
        l.dealt += amount;
        if (amount > l.biggestHit) l.biggestHit = amount;
    }

    public static void taken(ServerPlayer p, double amount) {
        if (amount <= 0) return;
        Line l = line(p);
        if (l != null) l.taken += amount;
    }

    /** Реально восстановленное здоровье (без перелечения). */
    public static void healed(ServerPlayer healer, double real) {
        if (real <= 0) return;
        Line l = line(healer);
        if (l != null) l.healed += real;
    }

    public static void died(ServerPlayer p) {
        Line l = line(p);
        if (l != null) l.deaths++;
    }

    public static void revived(ServerPlayer reviver) {
        Line l = line(reviver);
        if (l != null) l.revives++;
    }

    public static void killed(Entity killer) {
        Line l = line(creditOf(killer));
        if (l != null) l.kills++;
    }

    public static void cache(ServerPlayer p) {
        Line l = line(p);
        if (l != null) l.caches++;
    }

    /** Хранитель повержен за столько тиков (от вступления в бой). */
    static void bossTime(RunState s, String bossId, long ticks) {
        if (ticks <= 0) return;
        s.bossTimes.merge(bossId, ticks, Math::min);
        s.setDirty();
    }
}
