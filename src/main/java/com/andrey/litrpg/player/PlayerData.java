package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.StatSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.Arrays;

/**
 * Сохраняемые данные персонажа (вложение на игроке, переживает перезаход и смерть).
 * Менять только через {@link PlayerService} — он пересчитывает характеристики и шлёт клиенту.
 */
public final class PlayerData implements INBTSerializable<CompoundTag> {
    Role role = Role.NONE;
    int level = 1;
    long xp;
    final int[] allocated = new int[Stat.ALL.length];

    /** Тик сервера, когда игрок последний раз бил или получал урон (не сохраняется). */
    long lastCombatTick = Long.MIN_VALUE / 2;

    public Role role() {
        return role;
    }

    public int level() {
        return level;
    }

    public long xp() {
        return xp;
    }

    public int allocated(Stat s) {
        return allocated[s.ordinal()];
    }

    public int[] allocatedCopy() {
        return allocated.clone();
    }

    public int spentStatPoints() {
        return Arrays.stream(allocated).sum();
    }

    public int freeStatPoints(Progression p) {
        return Leveling.statPointsEarned(level, p) - spentStatPoints();
    }

    public StatBreakdown.Inputs inputs() {
        return new StatBreakdown.Inputs(role, level, allocated.clone(), StatSet.ZERO);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("role", role.id);
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putIntArray("allocated", allocated);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        role = Role.byId(tag.getString("role"));
        level = Math.max(1, tag.getInt("level"));
        xp = Math.max(0, tag.getLong("xp"));
        int[] a = tag.getIntArray("allocated");
        Arrays.fill(allocated, 0);
        System.arraycopy(a, 0, allocated, 0, Math.min(a.length, allocated.length));
    }
}
