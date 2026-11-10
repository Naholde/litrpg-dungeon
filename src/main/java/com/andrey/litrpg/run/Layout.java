package com.andrey.litrpg.run;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Схема подземелья из {@code /litrpg_map/layout.json} (пишет {@code tools/gen_dungeon.py} вместе с картой):
 * преддверие, этапы, отряды, хранители, преграды, разломы, таблички. Координаты — абсолютные, мир litrpg:dungeon.
 */
public record Layout(int version, Lobby lobby, List<Stage> stages) {
    public static final String RESOURCE = "/litrpg_map/layout.json";

    /** Коробка блоков (включительно). */
    public record Box(int x1, int y1, int z1, int x2, int y2, int z2) {
        public static final Codec<Box> CODEC = Codec.INT.listOf(6, 6).xmap(
                l -> new Box(Math.min(l.get(0), l.get(3)), Math.min(l.get(1), l.get(4)), Math.min(l.get(2), l.get(5)),
                        Math.max(l.get(0), l.get(3)), Math.max(l.get(1), l.get(4)), Math.max(l.get(2), l.get(5))),
                b -> List.of(b.x1, b.y1, b.z1, b.x2, b.y2, b.z2));

        public boolean contains(double x, double y, double z) {
            return x >= x1 && x < x2 + 1 && y >= y1 && y < y2 + 1 && z >= z1 && z < z2 + 1;
        }

        public boolean contains(Vec3 p) {
            return contains(p.x, p.y, p.z);
        }

        public Vec3 center() {
            return new Vec3((x1 + x2 + 1) / 2.0, y1, (z1 + z2 + 1) / 2.0);
        }

        public AABB aabb() {
            return new AABB(x1, y1, z1, x2 + 1, y2 + 1, z2 + 1);
        }

        public Iterable<BlockPos> blocks() {
            return BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2);
        }
    }

    /** Точка появления: x, y, z, поворот. */
    public record Spot(double x, double y, double z, float yaw) {
        public static final Codec<Spot> CODEC = Codec.DOUBLE.listOf(4, 4).xmap(
                l -> new Spot(l.get(0), l.get(1), l.get(2), l.get(3).floatValue()),
                s -> List.of(s.x, s.y, s.z, (double) s.yaw));

        public Vec3 pos() {
            return new Vec3(x, y, z);
        }
    }

    public record Sign(BlockPos at, List<String> lines) {
        public static final Codec<Sign> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("at").forGetter(Sign::at),
                Codec.STRING.listOf().fieldOf("lines").forGetter(Sign::lines)
        ).apply(i, Sign::new));
    }

    /** Боевой манекен Двора испытаний: где стоит, куда смотрит, злой ли (бьёт того, кто ему опаснее). */
    public record Dummy(Vec3 at, float yaw, boolean aggressive) {
        public static final Codec<Dummy> CODEC = RecordCodecBuilder.create(i -> i.group(
                Vec3.CODEC.fieldOf("at").forGetter(Dummy::at),
                Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Dummy::yaw),
                Codec.BOOL.optionalFieldOf("aggressive", false).forGetter(Dummy::aggressive)
        ).apply(i, Dummy::new));
    }

    public record Lobby(Box box, Spot spawn, BlockPos seal, List<Sign> signs, List<Dummy> dummies) {
        public static final Codec<Lobby> CODEC = RecordCodecBuilder.create(i -> i.group(
                Box.CODEC.fieldOf("box").forGetter(Lobby::box),
                Spot.CODEC.fieldOf("spawn").forGetter(Lobby::spawn),
                BlockPos.CODEC.fieldOf("seal").forGetter(Lobby::seal),
                Sign.CODEC.listOf().optionalFieldOf("signs", List.of()).forGetter(Lobby::signs),
                Dummy.CODEC.listOf().optionalFieldOf("dummies", List.of()).forGetter(Lobby::dummies)
        ).apply(i, Lobby::new));
    }

    /** Монстр отряда. {@code minParty} — появляется, только если в отряде не меньше стольких игроков. */
    public record Mob(String species, BlockPos at, int level, String rank, int minParty) {
        public static final Codec<Mob> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("species").forGetter(Mob::species),
                BlockPos.CODEC.fieldOf("at").forGetter(Mob::at),
                Codec.INT.fieldOf("level").forGetter(Mob::level),
                Codec.STRING.optionalFieldOf("rank", "normal").forGetter(Mob::rank),
                Codec.INT.optionalFieldOf("min_party", 1).forGetter(Mob::minParty)
        ).apply(i, Mob::new));
    }

    /**
     * Отряд монстров. {@code weight} — доля опыта этапа «за отряды». {@code title} — имя мини-хранителя: первый монстр
     * отряда становится им (чемпион с именем, за победу каждому — ключ тайника).
     */
    public record Pack(String id, Vec3 at, double weight, List<Mob> mobs, Optional<String> title) {
        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Pack::id),
                Vec3.CODEC.fieldOf("at").forGetter(Pack::at),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(Pack::weight),
                Mob.CODEC.listOf().fieldOf("mobs").forGetter(Pack::mobs),
                Codec.STRING.optionalFieldOf("title").forGetter(Pack::title)
        ).apply(i, Pack::new));
    }

    /**
     * Хранитель: тип (= id сущности), место, радиус арены, последний ли на этапе. {@code arena2} — второй зал
     * (x, y, z, радиус): на половине здоровья хранитель переносит бой туда.
     */
    public record Boss(String id, String type, BlockPos at, double arena, boolean last, Optional<List<Double>> arena2) {
        public static final Codec<Boss> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Boss::id),
                Codec.STRING.fieldOf("type").forGetter(Boss::type),
                BlockPos.CODEC.fieldOf("at").forGetter(Boss::at),
                Codec.DOUBLE.fieldOf("arena").forGetter(Boss::arena),
                Codec.BOOL.optionalFieldOf("final", false).forGetter(Boss::last),
                Codec.DOUBLE.listOf(4, 4).optionalFieldOf("arena2").forGetter(Boss::arena2)
        ).apply(i, Boss::new));
    }

    /**
     * Механизм этапа: загадка или ловушка. {@code points} — блоки (плиты, рычаги, кнопки, столбы), {@code area} — область
     * (ловушка, место загадки), {@code answers} — ответы текстовой загадки, {@code text} — вопрос или подсказка,
     * {@code num} — цифры механизма (секунды, доля урона, период…).
     */
    public record Mech(String id, String type, List<BlockPos> points, Optional<Box> area, List<String> answers, String text,
                       Map<String, Double> num) {
        public static final Codec<Mech> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Mech::id),
                Codec.STRING.fieldOf("type").forGetter(Mech::type),
                BlockPos.CODEC.listOf().optionalFieldOf("points", List.of()).forGetter(Mech::points),
                Box.CODEC.optionalFieldOf("area").forGetter(Mech::area),
                Codec.STRING.listOf().optionalFieldOf("answers", List.of()).forGetter(Mech::answers),
                Codec.STRING.optionalFieldOf("text", "").forGetter(Mech::text),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("num", Map.of()).forGetter(Mech::num)
        ).apply(i, Mech::new));

        public double n(String key, double def) {
            return num.getOrDefault(key, def);
        }

        /** Загадка (даёт опыт «за загадки»), а не ловушка. */
        public boolean puzzle() {
            return !type.equals("hazard");
        }
    }

    /** Тайник: уровень 1 — обычный, 2 — запертый (нужен ключ), 3 — в секретке, 4 — божественный. */
    public record Cache(String id, BlockPos at, int tier) {
        public static final Codec<Cache> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Cache::id),
                BlockPos.CODEC.fieldOf("at").forGetter(Cache::at),
                Codec.INT.fieldOf("tier").forGetter(Cache::tier)
        ).apply(i, Cache::new));

        /** Тайник секретки — за находку отряд получает опыт «за загадки». */
        public boolean secret() {
            return tier >= 3;
        }
    }

    /** Голос Системы: отряд впервые вошёл в область — в чат приходит запись. */
    public record Lore(Box box, String text) {
        public static final Codec<Lore> CODEC = RecordCodecBuilder.create(i -> i.group(
                Box.CODEC.fieldOf("box").forGetter(Lore::box),
                Codec.STRING.fieldOf("text").forGetter(Lore::text)
        ).apply(i, Lore::new));
    }

    /**
     * Преграда-печать. {@code openWhen} — условия ("pack:id", "boss:id"): все выполнены → открывается.
     * {@code lock} — id хранителя: открытая преграда закрывается на время боя с ним.
     */
    public record Gate(String id, Box box, List<String> openWhen, Optional<String> lock) {
        public static final Codec<Gate> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Gate::id),
                Box.CODEC.fieldOf("box").forGetter(Gate::box),
                Codec.STRING.listOf().optionalFieldOf("open_when", List.of()).forGetter(Gate::openWhen),
                Codec.STRING.optionalFieldOf("lock").forGetter(Gate::lock)
        ).apply(i, Gate::new));
    }

    public record Stage(int n, String key, String name, List<Integer> levels, String biome, Box box, Spot camp, Spot trader,
                        Box rift, List<String> riftWhen, List<Pack> packs, List<Boss> bosses, List<Gate> gates,
                        List<Sign> signs, Extra extra) {
        public static final Codec<Stage> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("n").forGetter(Stage::n),
                Codec.STRING.fieldOf("key").forGetter(Stage::key),
                Codec.STRING.fieldOf("name").forGetter(Stage::name),
                Codec.INT.listOf(2, 2).fieldOf("levels").forGetter(Stage::levels),
                Codec.STRING.fieldOf("biome").forGetter(Stage::biome),
                Box.CODEC.fieldOf("box").forGetter(Stage::box),
                Spot.CODEC.fieldOf("camp").forGetter(Stage::camp),
                Spot.CODEC.fieldOf("trader").forGetter(Stage::trader),
                Box.CODEC.fieldOf("rift").forGetter(Stage::rift),
                Codec.STRING.listOf().fieldOf("rift_when").forGetter(Stage::riftWhen),
                Pack.CODEC.listOf().fieldOf("packs").forGetter(Stage::packs),
                Boss.CODEC.listOf().fieldOf("bosses").forGetter(Stage::bosses),
                Gate.CODEC.listOf().fieldOf("gates").forGetter(Stage::gates),
                Sign.CODEC.listOf().optionalFieldOf("signs", List.of()).forGetter(Stage::signs),
                Extra.CODEC.forGetter(Stage::extra)
        ).apply(i, Stage::new));

        public List<Mech> mechs() {
            return extra.mechs;
        }

        public List<Cache> caches() {
            return extra.caches;
        }

        public List<Lore> lore() {
            return extra.lore;
        }

        public Optional<Mech> mech(String id) {
            return extra.mechs.stream().filter(m -> m.id.equals(id)).findFirst();
        }

        public int fromLevel() {
            return levels.get(0);
        }

        public int toLevel() {
            return levels.get(1);
        }

        public Optional<Pack> pack(String id) {
            return packs.stream().filter(p -> p.id.equals(id)).findFirst();
        }

        public Optional<Boss> boss(String id) {
            return bosses.stream().filter(b -> b.id.equals(id)).findFirst();
        }
    }

    /** Механизмы, тайники и записки этапа (отдельно — у кодека этапа предел полей). */
    public record Extra(List<Mech> mechs, List<Cache> caches, List<Lore> lore) {
        public static final com.mojang.serialization.MapCodec<Extra> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Mech.CODEC.listOf().optionalFieldOf("mechs", List.of()).forGetter(Extra::mechs),
                Cache.CODEC.listOf().optionalFieldOf("caches", List.of()).forGetter(Extra::caches),
                Lore.CODEC.listOf().optionalFieldOf("lore", List.of()).forGetter(Extra::lore)
        ).apply(i, Extra::new));
    }

    public static final Codec<Layout> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("version").forGetter(Layout::version),
            Lobby.CODEC.fieldOf("lobby").forGetter(Layout::lobby),
            Stage.CODEC.listOf().fieldOf("stages").forGetter(Layout::stages)
    ).apply(i, Layout::new));

    private static volatile Layout instance;
    private static volatile Map<Long, List<String>> signs;

    public static Layout get() {
        Layout l = instance;
        if (l == null) {
            synchronized (Layout.class) {
                l = instance;
                if (l == null) instance = l = load();
            }
        }
        return l;
    }

    /** Подставить свою схему (автотесты); null — вернуть схему из файла. */
    public static synchronized void override(Layout layout) {
        instance = layout;
        signs = null;
    }

    private static Layout load() {
        try (InputStream in = Layout.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("нет " + RESOURCE);
            var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            Layout l = CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(e -> new IllegalStateException("схема подземелья: " + e));
            LitRPG.LOG.info("[LitRPG] Схема подземелья: {} этапов", l.stages.size());
            return l;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось прочитать " + RESOURCE, e);
        }
    }

    public Optional<Stage> stage(int n) {
        return stages.stream().filter(s -> s.n == n).findFirst();
    }

    public int lastStage() {
        return stages.stream().mapToInt(Stage::n).max().orElse(0);
    }

    /** Этап, в коробке которого точка (0 — нет такого). */
    public int stageAt(Vec3 p) {
        for (Stage s : stages) if (s.box.contains(p)) return s.n;
        return 0;
    }

    /** Строки таблички в точке или null. */
    public List<String> signAt(BlockPos pos) {
        Map<Long, List<String>> m = signs;
        if (m == null) {
            m = new HashMap<>();
            for (Sign s : lobby.signs) m.put(s.at.asLong(), s.lines);
            for (Stage st : stages) for (Sign s : st.signs) m.put(s.at.asLong(), s.lines);
            signs = m;
        }
        return m.get(pos.asLong());
    }
}
