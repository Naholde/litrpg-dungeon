package com.andrey.litrpg.core.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Прогрессия персонажа: уровни, опыт, очки. Файл {@code balance/progression.json}. */
public record Progression(
        int maxLevel,
        int classLevel,
        double xpBase,
        double xpPerLevel,
        double xpExponent,
        int statPointsPerLevel,
        double statPointValue,
        int classPointsPerLevel
) {
    public static final MapCodec<Progression> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 200).fieldOf("max_level").forGetter(Progression::maxLevel),
            Codec.intRange(1, 200).fieldOf("class_level").forGetter(Progression::classLevel),
            Codec.DOUBLE.fieldOf("xp_base").forGetter(Progression::xpBase),
            Codec.DOUBLE.fieldOf("xp_per_level").forGetter(Progression::xpPerLevel),
            Codec.DOUBLE.fieldOf("xp_exponent").forGetter(Progression::xpExponent),
            Codec.intRange(0, 100).fieldOf("stat_points_per_level").forGetter(Progression::statPointsPerLevel),
            Codec.DOUBLE.fieldOf("stat_point_value").forGetter(Progression::statPointValue),
            Codec.intRange(0, 100).fieldOf("class_points_per_level").forGetter(Progression::classPointsPerLevel)
    ).apply(i, Progression::new));

    public static final Progression DEFAULT = new Progression(50, 10, 100, 90, 1.85, 10, 0.55, 2);

    /** Опыт, нужный, чтобы с уровня {@code level} перейти на следующий: base + perLevel × level^exponent. */
    public long xpToNext(int level) {
        if (level >= maxLevel) return 0;
        return Math.round(xpBase + xpPerLevel * Math.pow(level, xpExponent));
    }

    /** Весь опыт от уровня {@code from} до уровня {@code to} (from &lt; to). */
    public long xpBetween(int from, int to) {
        long sum = 0;
        for (int l = from; l < to; l++) sum += xpToNext(l);
        return sum;
    }
}
