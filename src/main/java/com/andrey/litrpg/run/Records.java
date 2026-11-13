package com.andrey.litrpg.run;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Вечные рекорды сервера (обновление 15, файл {@code data/litrpg_records.dat}): личная статистика каждого за всё время,
 * лучшие забеги по сложностям, лучшее время боя с каждым хранителем, рекорды «за забег», паркур, полигон, трофеи
 * (кто и когда впервые победил хранителя), титулы. Переживает новые забеги — сбрасывается только вместе с миром.
 */
public final class Records extends SavedData {
    public static final String NAME = "litrpg_records";
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Личная статистика игрока за всё время. */
    public static final class Profile {
        public String name = "?";
        public int runs, completedNormal, completedHeroic, bosses, deaths, revives, kills, caches;
        public double dealt, healed, taken;
        /** Лучшее время паркура (тики, 0 — не проходил). */
        public long parkourBest;
        /** Лучшая серия уворотов на полигоне. */
        public int polygonBest;
        /** Каких хранителей побеждал (id видов). */
        public final Set<String> bossKinds = new LinkedHashSet<>();
        public final Set<String> titles = new LinkedHashSet<>();
        public String title = "";

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("name", name);
            t.putInt("runs", runs);
            t.putInt("completedNormal", completedNormal);
            t.putInt("completedHeroic", completedHeroic);
            t.putInt("bosses", bosses);
            t.putInt("deaths", deaths);
            t.putInt("revives", revives);
            t.putInt("kills", kills);
            t.putInt("caches", caches);
            t.putDouble("dealt", dealt);
            t.putDouble("healed", healed);
            t.putDouble("taken", taken);
            t.putLong("parkourBest", parkourBest);
            t.putInt("polygonBest", polygonBest);
            t.put("bossKinds", strings(bossKinds));
            t.put("titles", strings(titles));
            t.putString("title", title);
            return t;
        }

        static Profile load(CompoundTag t) {
            Profile p = new Profile();
            p.name = t.getString("name");
            p.runs = t.getInt("runs");
            p.completedNormal = t.getInt("completedNormal");
            p.completedHeroic = t.getInt("completedHeroic");
            p.bosses = t.getInt("bosses");
            p.deaths = t.getInt("deaths");
            p.revives = t.getInt("revives");
            p.kills = t.getInt("kills");
            p.caches = t.getInt("caches");
            p.dealt = t.getDouble("dealt");
            p.healed = t.getDouble("healed");
            p.taken = t.getDouble("taken");
            p.parkourBest = t.getLong("parkourBest");
            p.polygonBest = t.getInt("polygonBest");
            readStrings(t, "bossKinds", p.bossKinds);
            readStrings(t, "titles", p.titles);
            p.title = t.getString("title");
            return p;
        }
    }

    /** Забег (или бой) в таблице: время, отряд, вайпы, дата. */
    public record RunRecord(long ticks, List<String> names, int wipes, String date, String difficulty, boolean completed, int stage) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("ticks", ticks);
            ListTag n = new ListTag();
            names.forEach(s -> n.add(StringTag.valueOf(s)));
            t.put("names", n);
            t.putInt("wipes", wipes);
            t.putString("date", date);
            t.putString("difficulty", difficulty);
            t.putBoolean("completed", completed);
            t.putInt("stage", stage);
            return t;
        }

        static RunRecord load(CompoundTag t) {
            List<String> n = new ArrayList<>();
            for (Tag s : t.getList("names", Tag.TAG_STRING)) n.add(s.getAsString());
            return new RunRecord(t.getLong("ticks"), n, t.getInt("wipes"), t.getString("date"), t.getString("difficulty"),
                    t.getBoolean("completed"), t.getInt("stage"));
        }
    }

    /** Рекорд одного игрока: значение, кто, когда. */
    public record Entry(double value, String who, String date) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putDouble("value", value);
            t.putString("who", who);
            t.putString("date", date);
            return t;
        }

        static Entry load(CompoundTag t) {
            return new Entry(t.getDouble("value"), t.getString("who"), t.getString("date"));
        }
    }

    /** Трофей: хранитель впервые побеждён — когда, кем, на какой сложности. */
    public record Trophy(String date, List<String> names, String difficulty) {
        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putString("date", date);
            ListTag n = new ListTag();
            names.forEach(s -> n.add(StringTag.valueOf(s)));
            t.put("names", n);
            t.putString("difficulty", difficulty);
            return t;
        }

        static Trophy load(CompoundTag t) {
            List<String> n = new ArrayList<>();
            for (Tag s : t.getList("names", Tag.TAG_STRING)) n.add(s.getAsString());
            return new Trophy(t.getString("date"), n, t.getString("difficulty"));
        }
    }

    /** Рекорды «за забег» (больше всего за один забег). */
    public enum Single {
        DEALT("dealt", "Урон за забег"),
        HEALED("healed", "Лечение за забег"),
        TAKEN("taken", "Принятый урон за забег"),
        REVIVES("revives", "Поднято павших за забег"),
        BIGGEST("biggest", "Самый сильный удар"),
        KILLS("kills", "Монстров за забег");

        public final String id, title;

        Single(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    final Map<UUID, Profile> players = new LinkedHashMap<>();
    /** Лучший полный забег: сложность → запись. */
    final Map<String, RunRecord> bestRun = new LinkedHashMap<>();
    /** Лучший бой с хранителем: "сложность:хранитель" → запись. */
    final Map<String, RunRecord> bossBest = new LinkedHashMap<>();
    /** Рекорды за забег: "сложность:вид" → запись. */
    final Map<String, Entry> single = new LinkedHashMap<>();
    /** Трофеи: хранитель → первая победа. */
    final Map<String, Trophy> trophies = new LinkedHashMap<>();
    /** Последние забеги (новые — первыми), не больше 20. */
    final List<RunRecord> history = new ArrayList<>();
    /** Растёт при каждом изменении — доски в лобби перерисовываются только тогда. */
    int revision;

    public static Factory<Records> factory() {
        return new Factory<>(Records::new, Records::load, null);
    }

    public static Records get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(factory(), NAME);
    }

    static String today() {
        return LocalDate.now().format(DATE);
    }

    public Profile profile(UUID id, String name) {
        Profile p = players.computeIfAbsent(id, k -> new Profile());
        if (name != null && !name.isEmpty()) p.name = name;
        return p;
    }

    public Profile profileOrNull(UUID id) {
        return players.get(id);
    }

    public Map<UUID, Profile> players() {
        return players;
    }

    public Map<String, RunRecord> bestRun() {
        return bestRun;
    }

    public Map<String, RunRecord> bossBest() {
        return bossBest;
    }

    public Map<String, Entry> single() {
        return single;
    }

    public Map<String, Trophy> trophies() {
        return trophies;
    }

    public List<RunRecord> history() {
        return history;
    }

    public int revision() {
        return revision;
    }

    void changed() {
        revision++;
        setDirty();
    }

    /** Лучшие по паркуру: (имя, тики), по возрастанию времени. */
    public List<Map.Entry<String, Long>> parkourTop(int n) {
        return players.values().stream().filter(p -> p.parkourBest > 0)
                .map(p -> Map.entry(p.name, p.parkourBest))
                .sorted(Map.Entry.comparingByValue())
                .limit(n).toList();
    }

    /** Лучшие по полю профиля (по убыванию). */
    public List<Profile> top(Comparator<Profile> by, int n) {
        return players.values().stream().sorted(by.reversed()).limit(n).toList();
    }

    // ------------------------------------------------------------------ запись

    /** Поставить рекорд «за забег», если он лучше. Возвращает true, если рекорд новый. */
    boolean offer(Difficulty d, Single kind, double value, String who) {
        if (value <= 0) return false;
        String key = d.id + ":" + kind.id;
        Entry cur = single.get(key);
        if (cur != null && cur.value() >= value) return false;
        single.put(key, new Entry(value, who, today()));
        changed();
        return true;
    }

    /** Время боя с хранителем; true — новый рекорд. */
    boolean offerBoss(Difficulty d, String boss, long ticks, List<String> names, int wipes) {
        String key = d.id + ":" + boss;
        RunRecord cur = bossBest.get(key);
        if (cur != null && cur.ticks() <= ticks) return false;
        bossBest.put(key, new RunRecord(ticks, List.copyOf(names), wipes, today(), d.id, true, 0));
        changed();
        return true;
    }

    /** Время полного забега; true — новый рекорд. */
    boolean offerRun(Difficulty d, RunRecord r) {
        RunRecord cur = bestRun.get(d.id);
        if (cur != null && cur.ticks() <= r.ticks()) return false;
        bestRun.put(d.id, r);
        changed();
        return true;
    }

    /** Первая победа над хранителем; true — трофей новый. */
    boolean trophy(String boss, List<String> names, Difficulty d) {
        if (trophies.containsKey(boss)) return false;
        trophies.put(boss, new Trophy(today(), List.copyOf(names), d.id));
        changed();
        return true;
    }

    void addHistory(RunRecord r) {
        history.add(0, r);
        while (history.size() > 20) history.remove(history.size() - 1);
        changed();
    }

    // ------------------------------------------------------------------ файл

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag ps = new ListTag();
        players.forEach((id, p) -> {
            CompoundTag e = p.save();
            e.putUUID("id", id);
            ps.add(e);
        });
        tag.put("players", ps);
        tag.put("bestRun", runs(bestRun));
        tag.put("bossBest", runs(bossBest));
        CompoundTag s = new CompoundTag();
        single.forEach((k, v) -> s.put(k, v.save()));
        tag.put("single", s);
        CompoundTag tr = new CompoundTag();
        trophies.forEach((k, v) -> tr.put(k, v.save()));
        tag.put("trophies", tr);
        ListTag h = new ListTag();
        history.forEach(r -> h.add(r.save()));
        tag.put("history", h);
        tag.putInt("revision", revision);
        return tag;
    }

    public static Records load(CompoundTag tag, HolderLookup.Provider registries) {
        Records r = new Records();
        for (Tag t : tag.getList("players", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            r.players.put(e.getUUID("id"), Profile.load(e));
        }
        readRuns(tag.getCompound("bestRun"), r.bestRun);
        readRuns(tag.getCompound("bossBest"), r.bossBest);
        CompoundTag s = tag.getCompound("single");
        for (String k : s.getAllKeys()) r.single.put(k, Entry.load(s.getCompound(k)));
        CompoundTag tr = tag.getCompound("trophies");
        for (String k : tr.getAllKeys()) r.trophies.put(k, Trophy.load(tr.getCompound(k)));
        for (Tag t : tag.getList("history", Tag.TAG_COMPOUND)) r.history.add(RunRecord.load((CompoundTag) t));
        r.revision = tag.getInt("revision");
        return r;
    }

    private static CompoundTag runs(Map<String, RunRecord> m) {
        CompoundTag t = new CompoundTag();
        m.forEach((k, v) -> t.put(k, v.save()));
        return t;
    }

    private static void readRuns(CompoundTag t, Map<String, RunRecord> into) {
        for (String k : t.getAllKeys()) into.put(k, RunRecord.load(t.getCompound(k)));
    }

    static ListTag strings(Set<String> set) {
        ListTag l = new ListTag();
        for (String s : set) l.add(StringTag.valueOf(s));
        return l;
    }

    static void readStrings(CompoundTag tag, String key, Set<String> into) {
        for (Tag t : tag.getList(key, Tag.TAG_STRING)) into.add(t.getAsString());
    }
}
