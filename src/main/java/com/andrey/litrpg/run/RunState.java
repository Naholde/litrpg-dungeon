package com.andrey.litrpg.run;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Состояние прохождения (файл {@code data/litrpg_run.dat} мира): этап, отряд, попытка, что сделано на этапе,
 * какие награды уже выданы, духи. Меняет его только {@link RunService}.
 */
public final class RunState extends SavedData {
    public static final String NAME = "litrpg_run";
    public static final int LOBBY = 0;

    /** 0 — преддверие (прохождение не начато), 1…5 — этап, больше последнего — пройдено. */
    int stage = LOBBY;
    /** Отряд, собранный Печатью. Не меняется до конца прохождения. */
    final List<UUID> party = new ArrayList<>();
    final Map<UUID, String> names = new LinkedHashMap<>();
    /** Размер отряда на старте — по нему считаются хранители и состав отрядов монстров. */
    int partySize;
    int attempt = 1;
    int wipes;
    /** Растёт при вайпе и смене этапа: сущности прошлых «эпох» убираются при загрузке. */
    int epoch;
    long runStart;
    long stageStart;

    /** Выполнено на этой попытке этапа: "pack:id", "boss:id", "gate:id". Вайп очищает. */
    final Set<String> done = new HashSet<>();
    /** Появились на этой попытке (отряды и хранители). */
    final Set<String> spawned = new HashSet<>();
    /** Сколько монстров отряда ещё живо. */
    final Map<String, Integer> alive = new HashMap<>();
    /** Награды отряда, выданные за всё прохождение (повторно после вайпа — не выдаются). */
    final Set<String> claims = new HashSet<>();
    /** Кто какие награды уже получил (догоняющая выдача тем, кто был не в сети). */
    final Map<UUID, Set<String>> granted = new HashMap<>();
    /** Разбитые блоки этапа (восстанавливаются вайпом). */
    final Set<Long> broken = new HashSet<>();
    /** Духи: где пал. */
    final Map<UUID, BlockPos> spirits = new HashMap<>();
    /** Наименьший отряд у Печати (для проверки в одиночку можно опустить до 1). 0 — по балансу. */
    int minPlayers;

    public static Factory<RunState> factory() {
        return new Factory<>(RunState::new, RunState::load, null);
    }

    public int stage() {
        return stage;
    }

    public boolean active(int lastStage) {
        return stage >= 1 && stage <= lastStage;
    }

    public List<UUID> party() {
        return party;
    }

    public boolean member(UUID id) {
        return party.contains(id);
    }

    public int partySize() {
        return partySize;
    }

    public int attempt() {
        return attempt;
    }

    public boolean isSpirit(UUID id) {
        return spirits.containsKey(id);
    }

    public Set<String> done() {
        return done;
    }

    public Set<String> claims() {
        return claims;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("stage", stage);
        ListTag p = new ListTag();
        for (UUID id : party) {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", id);
            e.putString("name", names.getOrDefault(id, "?"));
            ListTag g = new ListTag();
            for (String s : granted.getOrDefault(id, Set.of())) g.add(StringTag.valueOf(s));
            e.put("granted", g);
            p.add(e);
        }
        tag.put("party", p);
        tag.putInt("partySize", partySize);
        tag.putInt("attempt", attempt);
        tag.putInt("wipes", wipes);
        tag.putInt("epoch", epoch);
        tag.putLong("runStart", runStart);
        tag.putLong("stageStart", stageStart);
        tag.put("done", strings(done));
        tag.put("spawned", strings(spawned));
        tag.put("claims", strings(claims));
        CompoundTag a = new CompoundTag();
        alive.forEach(a::putInt);
        tag.put("alive", a);
        ListTag b = new ListTag();
        for (long l : broken) b.add(LongTag.valueOf(l));
        tag.put("broken", b);
        ListTag s = new ListTag();
        spirits.forEach((id, pos) -> {
            CompoundTag e = new CompoundTag();
            e.putUUID("id", id);
            e.putLong("pos", pos.asLong());
            s.add(e);
        });
        tag.put("spirits", s);
        tag.putInt("minPlayers", minPlayers);
        return tag;
    }

    private static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();
        for (String s : set) l.add(StringTag.valueOf(s));
        return l;
    }

    private static void readStrings(CompoundTag tag, String key, Set<String> into) {
        for (Tag t : tag.getList(key, Tag.TAG_STRING)) into.add(t.getAsString());
    }

    public static RunState load(CompoundTag tag, HolderLookup.Provider registries) {
        RunState s = new RunState();
        s.stage = tag.getInt("stage");
        for (Tag t : tag.getList("party", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            UUID id = e.getUUID("id");
            s.party.add(id);
            s.names.put(id, e.getString("name"));
            Set<String> g = new HashSet<>();
            readStrings(e, "granted", g);
            s.granted.put(id, g);
        }
        s.partySize = tag.getInt("partySize");
        s.attempt = Math.max(1, tag.getInt("attempt"));
        s.wipes = tag.getInt("wipes");
        s.epoch = tag.getInt("epoch");
        s.runStart = tag.getLong("runStart");
        s.stageStart = tag.getLong("stageStart");
        readStrings(tag, "done", s.done);
        readStrings(tag, "spawned", s.spawned);
        readStrings(tag, "claims", s.claims);
        CompoundTag a = tag.getCompound("alive");
        for (String k : a.getAllKeys()) s.alive.put(k, a.getInt(k));
        for (Tag t : tag.getList("broken", Tag.TAG_LONG)) s.broken.add(((LongTag) t).getAsLong());
        for (Tag t : tag.getList("spirits", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            s.spirits.put(e.getUUID("id"), BlockPos.of(e.getLong("pos")));
        }
        s.minPlayers = tag.getInt("minPlayers");
        return s;
    }

    /** Сбросить всё к преддверию (новое прохождение). Награды и отряд забываются. */
    void resetAll() {
        stage = LOBBY;
        party.clear();
        names.clear();
        partySize = 0;
        attempt = 1;
        wipes = 0;
        epoch++;
        clearAttempt();
        claims.clear();
        granted.clear();
        setDirty();
    }

    /** Новая попытка этапа: всё «сделанное» и появившееся забывается, награды остаются. */
    void clearAttempt() {
        done.clear();
        spawned.clear();
        alive.clear();
        broken.clear();
        spirits.clear();
        setDirty();
    }
}
