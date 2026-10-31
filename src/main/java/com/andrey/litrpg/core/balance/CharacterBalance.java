package com.andrey.litrpg.core.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Как характеристики превращаются в производные величины (здоровье, крит, уклонение…).
 * Файл {@code balance/character.json}. Доли — от 0 до 1 (0,05 = 5%).
 */
public record CharacterBalance(double baseStat, Health health, Power power, Crit crit, Defense defense,
                               Utility utility, Regen regen) {

    public record Health(double base, double perLevel, double perVitality) {
        static final Codec<Health> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("base").forGetter(Health::base),
                Codec.DOUBLE.fieldOf("per_level").forGetter(Health::perLevel),
                Codec.DOUBLE.fieldOf("per_vitality").forGetter(Health::perVitality)
        ).apply(i, Health::new));
    }

    public record Power(double weaponPerStrength, double skillPerIntellect) {
        static final Codec<Power> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("weapon_per_strength").forGetter(Power::weaponPerStrength),
                Codec.DOUBLE.fieldOf("skill_per_intellect").forGetter(Power::skillPerIntellect)
        ).apply(i, Power::new));
    }

    public record Crit(double baseChance, double chancePerAgility, double chancePerLuck, double chanceCap,
                       double baseDamage, double damagePerAgility) {
        static final Codec<Crit> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("base_chance").forGetter(Crit::baseChance),
                Codec.DOUBLE.fieldOf("chance_per_agility").forGetter(Crit::chancePerAgility),
                Codec.DOUBLE.fieldOf("chance_per_luck").forGetter(Crit::chancePerLuck),
                Codec.DOUBLE.fieldOf("chance_cap").forGetter(Crit::chanceCap),
                Codec.DOUBLE.fieldOf("base_damage").forGetter(Crit::baseDamage),
                Codec.DOUBLE.fieldOf("damage_per_agility").forGetter(Crit::damagePerAgility)
        ).apply(i, Crit::new));
    }

    public record Defense(double dodgePerAgility, double dodgeCap, double toughnessPerVitality, double toughnessCap) {
        static final Codec<Defense> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("dodge_per_agility").forGetter(Defense::dodgePerAgility),
                Codec.DOUBLE.fieldOf("dodge_cap").forGetter(Defense::dodgeCap),
                Codec.DOUBLE.fieldOf("toughness_per_vitality").forGetter(Defense::toughnessPerVitality),
                Codec.DOUBLE.fieldOf("toughness_cap").forGetter(Defense::toughnessCap)
        ).apply(i, Defense::new));
    }

    public record Utility(double healPerIntellect, double hastePerIntellect, double hasteCap,
                          double speedPerAgility, double speedCap, double findPerLuck) {
        static final Codec<Utility> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("heal_per_intellect").forGetter(Utility::healPerIntellect),
                Codec.DOUBLE.fieldOf("haste_per_intellect").forGetter(Utility::hastePerIntellect),
                Codec.DOUBLE.fieldOf("haste_cap").forGetter(Utility::hasteCap),
                Codec.DOUBLE.fieldOf("speed_per_agility").forGetter(Utility::speedPerAgility),
                Codec.DOUBLE.fieldOf("speed_cap").forGetter(Utility::speedCap),
                Codec.DOUBLE.fieldOf("find_per_luck").forGetter(Utility::findPerLuck)
        ).apply(i, Utility::new));
    }

    public record Regen(double outOfCombatPerSecond, int combatTimeoutSeconds) {
        static final Codec<Regen> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("out_of_combat_per_second").forGetter(Regen::outOfCombatPerSecond),
                Codec.intRange(1, 120).fieldOf("combat_timeout_seconds").forGetter(Regen::combatTimeoutSeconds)
        ).apply(i, Regen::new));
    }

    public static final MapCodec<CharacterBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("base_stat").forGetter(CharacterBalance::baseStat),
            Health.CODEC.fieldOf("health").forGetter(CharacterBalance::health),
            Power.CODEC.fieldOf("power").forGetter(CharacterBalance::power),
            Crit.CODEC.fieldOf("crit").forGetter(CharacterBalance::crit),
            Defense.CODEC.fieldOf("defense").forGetter(CharacterBalance::defense),
            Utility.CODEC.fieldOf("utility").forGetter(CharacterBalance::utility),
            Regen.CODEC.fieldOf("regen").forGetter(CharacterBalance::regen)
    ).apply(i, CharacterBalance::new));

    public static final CharacterBalance DEFAULT = new CharacterBalance(5,
            new Health(100, 18, 4),
            new Power(0.01, 0.01),
            new Crit(0.05, 0.001, 0.0015, 0.6, 1.5, 0.002),
            new Defense(0.0008, 0.3, 0.0005, 0.6),
            new Utility(0.01, 0.001, 0.3, 0.0005, 0.25, 0.01),
            new Regen(0.03, 6));
}
