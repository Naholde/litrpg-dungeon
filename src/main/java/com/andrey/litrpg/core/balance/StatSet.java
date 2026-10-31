package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.player.Stat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** По числу на каждую характеристику (рост за уровень, бонусы и т.п.). */
public record StatSet(double strength, double agility, double vitality, double intellect, double luck) {
    public static final MapCodec<StatSet> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("strength").forGetter(StatSet::strength),
            Codec.DOUBLE.fieldOf("agility").forGetter(StatSet::agility),
            Codec.DOUBLE.fieldOf("vitality").forGetter(StatSet::vitality),
            Codec.DOUBLE.fieldOf("intellect").forGetter(StatSet::intellect),
            Codec.DOUBLE.fieldOf("luck").forGetter(StatSet::luck)
    ).apply(i, StatSet::new));
    public static final Codec<StatSet> CODEC = MAP_CODEC.codec();

    public static final StatSet ZERO = new StatSet(0, 0, 0, 0, 0);

    public double get(Stat s) {
        return switch (s) {
            case STRENGTH -> strength;
            case AGILITY -> agility;
            case VITALITY -> vitality;
            case INTELLECT -> intellect;
            case LUCK -> luck;
        };
    }
}
