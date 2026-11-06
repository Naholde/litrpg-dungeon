package com.andrey.litrpg.gear;

import com.andrey.litrpg.player.Stat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * «Вещь Системы»: компонент на любом предмете. Уровень, редкость, основа (урон оружия или броня),
 * прибавки к характеристикам и свойства. {@code unique} — id уникальной вещи (трофей хранителя), иначе пусто.
 */
public record GearData(GearKind kind, int level, Rarity rarity, double base, Map<Stat, Double> stats, List<Affix> affixes,
                       String unique, int rerolls, List<Effect> effects) {

    public record Affix(AffixType type, double value) {
        public static final Codec<Affix> CODEC = RecordCodecBuilder.create(i -> i.group(
                AffixType.CODEC.fieldOf("type").forGetter(Affix::type),
                Codec.DOUBLE.fieldOf("value").forGetter(Affix::value)
        ).apply(i, Affix::new));
    }

    /** Особое свойство вещи ({@link GearEffect}) и его сила. */
    public record Effect(GearEffect type, double value) {
        public static final Codec<Effect> CODEC = RecordCodecBuilder.create(i -> i.group(
                GearEffect.CODEC.fieldOf("type").forGetter(Effect::type),
                Codec.DOUBLE.fieldOf("value").forGetter(Effect::value)
        ).apply(i, Effect::new));
    }

    private static final Codec<Stat> STAT = Codec.STRING.xmap(s -> {
        for (Stat st : Stat.ALL) if (st.id.equals(s)) return st;
        return Stat.STRENGTH;
    }, st -> st.id);

    public static final Codec<GearData> CODEC = RecordCodecBuilder.create(i -> i.group(
            GearKind.CODEC.fieldOf("kind").forGetter(GearData::kind),
            Codec.INT.fieldOf("level").forGetter(GearData::level),
            Rarity.CODEC.fieldOf("rarity").forGetter(GearData::rarity),
            Codec.DOUBLE.fieldOf("base").forGetter(GearData::base),
            Codec.unboundedMap(STAT, Codec.DOUBLE).fieldOf("stats").forGetter(GearData::stats),
            Affix.CODEC.listOf().fieldOf("affixes").forGetter(GearData::affixes),
            Codec.STRING.optionalFieldOf("unique", "").forGetter(GearData::unique),
            Codec.INT.optionalFieldOf("rerolls", 0).forGetter(GearData::rerolls),
            Effect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(GearData::effects)
    ).apply(i, GearData::new));

    /** Для отправки клиенту (подсказка) — тот же кодек через NBT. */
    public static final StreamCodec<ByteBuf, GearData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public GearData {
        stats = Map.copyOf(stats);
        affixes = List.copyOf(affixes);
        effects = List.copyOf(effects);
    }

    /** Вещь без особых свойств. */
    public GearData(GearKind kind, int level, Rarity rarity, double base, Map<Stat, Double> stats, List<Affix> affixes,
                    String unique, int rerolls) {
        this(kind, level, rarity, base, stats, affixes, unique, rerolls, List.of());
    }

    public GearData withEffects(List<Effect> list) {
        return new GearData(kind, level, rarity, base, stats, affixes, unique, rerolls, list);
    }

    public double stat(Stat s) {
        return stats.getOrDefault(s, 0.0);
    }

    public GearData withRerolls(int n) {
        return new GearData(kind, level, rarity, base, stats, affixes, unique, n, effects);
    }

    public static Map<Stat, Double> emptyStats() {
        return new EnumMap<>(Stat.class);
    }
}
