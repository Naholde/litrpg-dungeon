package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.StatSet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.Style;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сохраняемые данные персонажа (вложение на игроке, переживает перезаход и смерть).
 * Менять только через {@link PlayerService} — он пересчитывает характеристики и шлёт клиенту.
 */
public final class PlayerData implements INBTSerializable<CompoundTag> {
    Role role = Role.NONE;
    int level = 1;
    long xp;
    final int[] allocated = new int[Stat.ALL.length];
    /** Класс ("" — ещё нет). */
    String classId = "";
    /** Ранги навыков класса: id навыка → 1…5. */
    final Map<String, Integer> ranks = new HashMap<>();
    /** Стиль игры до получения класса: сколько раз игрок делал то или иное. */
    final Map<Style, Integer> style = new EnumMap<>(Style.class);
    /** Классы, предложенные Системой (до выбора). */
    final List<String> offers = new ArrayList<>();

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

    public String classId() {
        return classId;
    }

    public PlayerClass playerClass() {
        return ClassRegistry.get(classId);
    }

    public int rank(String skillId) {
        return ranks.getOrDefault(skillId, 0);
    }

    public Map<String, Integer> ranksView() {
        return Collections.unmodifiableMap(ranks);
    }

    public Map<Style, Integer> styleView() {
        return Collections.unmodifiableMap(style);
    }

    public List<String> offers() {
        return Collections.unmodifiableList(offers);
    }

    /** Потрачено очков класса: ранг n стоит n очков (1 + 2 + … + n). */
    public int spentClassPoints() {
        return ranks.values().stream().mapToInt(r -> r * (r + 1) / 2).sum();
    }

    public int freeClassPoints(Progression p) {
        return Leveling.classPointsEarned(level, p) - spentClassPoints();
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
        PlayerClass c = playerClass();
        return new StatBreakdown.Inputs(role, level, allocated.clone(), StatSet.ZERO,
                c != null ? c.growth() : null, c != null ? c.name() : null);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putString("role", role.id);
        tag.putInt("level", level);
        tag.putLong("xp", xp);
        tag.putIntArray("allocated", allocated);
        tag.putString("class", classId);
        CompoundTag r = new CompoundTag();
        ranks.forEach(r::putInt);
        tag.put("ranks", r);
        CompoundTag st = new CompoundTag();
        style.forEach((k, v) -> st.putInt(k.name(), v));
        tag.put("style", st);
        ListTag of = new ListTag();
        offers.forEach(o -> of.add(StringTag.valueOf(o)));
        tag.put("offers", of);
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
        classId = ClassRegistry.get(tag.getString("class")) != null ? tag.getString("class") : "";
        ranks.clear();
        CompoundTag r = tag.getCompound("ranks");
        for (String k : r.getAllKeys()) ranks.put(k, r.getInt(k));
        style.clear();
        CompoundTag st = tag.getCompound("style");
        for (Style s : Style.values()) if (st.contains(s.name())) style.put(s, st.getInt(s.name()));
        offers.clear();
        ListTag of = tag.getList("offers", Tag.TAG_STRING);
        for (int i = 0; i < of.size(); i++) offers.add(of.getString(i));
    }
}
