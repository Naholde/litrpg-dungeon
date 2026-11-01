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

    public record Lobby(Box box, Spot spawn, BlockPos seal, List<Sign> signs) {
        public static final Codec<Lobby> CODEC = RecordCodecBuilder.create(i -> i.group(
                Box.CODEC.fieldOf("box").forGetter(Lobby::box),
                Spot.CODEC.fieldOf("spawn").forGetter(Lobby::spawn),
                BlockPos.CODEC.fieldOf("seal").forGetter(Lobby::seal),
                Sign.CODEC.listOf().optionalFieldOf("signs", List.of()).forGetter(Lobby::signs)
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

    /** Отряд монстров. {@code weight} — доля опыта этапа «за отряды». */
    public record Pack(String id, Vec3 at, double weight, List<Mob> mobs) {
        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Pack::id),
                Vec3.CODEC.fieldOf("at").forGetter(Pack::at),
                Codec.DOUBLE.optionalFieldOf("weight", 1.0).forGetter(Pack::weight),
                Mob.CODEC.listOf().fieldOf("mobs").forGetter(Pack::mobs)
        ).apply(i, Pack::new));
    }

    /** Хранитель: тип (= id сущности), место, радиус арены, последний ли на этапе. */
    public record Boss(String id, String type, BlockPos at, double arena, boolean last) {
        public static final Codec<Boss> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("id").forGetter(Boss::id),
                Codec.STRING.fieldOf("type").forGetter(Boss::type),
                BlockPos.CODEC.fieldOf("at").forGetter(Boss::at),
                Codec.DOUBLE.fieldOf("arena").forGetter(Boss::arena),
                Codec.BOOL.optionalFieldOf("final", false).forGetter(Boss::last)
        ).apply(i, Boss::new));
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
                        List<Sign> signs) {
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
                Sign.CODEC.listOf().optionalFieldOf("signs", List.of()).forGetter(Stage::signs)
        ).apply(i, Stage::new));

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

    /** Подставить свою схему (автотесты). */
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
