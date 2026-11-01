package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.gear.AffixType;
import com.andrey.litrpg.gear.Rarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;

/** Снаряжение: редкости, оружие, броня, характеристики и свойства вещей, добыча. Файл {@code balance/gear.json}. */
public record GearBalance(Map<Rarity, RarityInfo> rarities, Weapons weapons, Armor armor, double statPerLevel,
                         Map<AffixType, Range> affixes, Drops drops) {

    /**
     * @param weight  вес при выпадении (чем меньше, тем реже); 0 — не выпадает случайно
     * @param power   множитель силы вещи (урон, броня, характеристики)
     * @param stats   сколько характеристик
     * @param affixes сколько свойств
     */
    public record RarityInfo(double weight, double power, int stats, int affixes) {
        static final Codec<RarityInfo> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("weight").forGetter(RarityInfo::weight),
                Codec.DOUBLE.fieldOf("power").forGetter(RarityInfo::power),
                Codec.intRange(0, 5).fieldOf("stats").forGetter(RarityInfo::stats),
                Codec.intRange(0, 8).fieldOf("affixes").forGetter(RarityInfo::affixes)
        ).apply(i, RarityInfo::new));
    }

    /** Урон оружия: основа + за уровень (до множителя редкости). */
    public record Weapons(double meleeBase, double meleePerLevel, double axeMult, double bowBase, double bowPerLevel,
                          double staffBase, double staffPerLevel) {
        static final Codec<Weapons> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("melee_base").forGetter(Weapons::meleeBase),
                Codec.DOUBLE.fieldOf("melee_per_level").forGetter(Weapons::meleePerLevel),
                Codec.DOUBLE.fieldOf("axe_mult").forGetter(Weapons::axeMult),
                Codec.DOUBLE.fieldOf("bow_base").forGetter(Weapons::bowBase),
                Codec.DOUBLE.fieldOf("bow_per_level").forGetter(Weapons::bowPerLevel),
                Codec.DOUBLE.fieldOf("staff_base").forGetter(Weapons::staffBase),
                Codec.DOUBLE.fieldOf("staff_per_level").forGetter(Weapons::staffPerLevel)
        ).apply(i, Weapons::new));
    }

    /**
     * Броня: {@code perLevel} — броня полного комплекта за уровень, доли частей; снижение урона =
     * броня / (броня + k × уровень + c), не больше {@code cap}.
     */
    public record Armor(double perLevel, double helmet, double chest, double legs, double boots, double shield,
                        double k, double c, double cap) {
        static final Codec<Armor> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("per_level").forGetter(Armor::perLevel),
                Codec.DOUBLE.fieldOf("helmet").forGetter(Armor::helmet),
                Codec.DOUBLE.fieldOf("chest").forGetter(Armor::chest),
                Codec.DOUBLE.fieldOf("legs").forGetter(Armor::legs),
                Codec.DOUBLE.fieldOf("boots").forGetter(Armor::boots),
                Codec.DOUBLE.fieldOf("shield").forGetter(Armor::shield),
                Codec.DOUBLE.fieldOf("k").forGetter(Armor::k),
                Codec.DOUBLE.fieldOf("c").forGetter(Armor::c),
                Codec.DOUBLE.fieldOf("cap").forGetter(Armor::cap)
        ).apply(i, Armor::new));

        public double reduction(double armor, int level) {
            if (armor <= 0) return 0;
            return Math.min(cap, armor / (armor + k * Math.max(1, level) + c));
        }
    }

    /** Диапазон свойства на 50 уровне (ниже — пропорционально, не меньше четверти). */
    public record Range(double min, double max) {
        static final Codec<Range> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("min").forGetter(Range::min),
                Codec.DOUBLE.fieldOf("max").forGetter(Range::max)
        ).apply(i, Range::new));
    }

    /**
     * @param coinsPerLevel медных монет с обычного монстра за уровень
     * @param gearChance    шанс вещи с обычного монстра
     */
    public record Drops(double coinsPerLevel, double gearChance) {
        static final Codec<Drops> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("coins_per_level").forGetter(Drops::coinsPerLevel),
                Codec.DOUBLE.fieldOf("gear_chance").forGetter(Drops::gearChance)
        ).apply(i, Drops::new));
    }

    public static final MapCodec<GearBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.unboundedMap(Rarity.CODEC, RarityInfo.CODEC).fieldOf("rarities").forGetter(GearBalance::rarities),
            Weapons.CODEC.fieldOf("weapons").forGetter(GearBalance::weapons),
            Armor.CODEC.fieldOf("armor").forGetter(GearBalance::armor),
            Codec.DOUBLE.fieldOf("stat_per_level").forGetter(GearBalance::statPerLevel),
            Codec.unboundedMap(AffixType.CODEC, Range.CODEC).fieldOf("affixes").forGetter(GearBalance::affixes),
            Drops.CODEC.fieldOf("drops").forGetter(GearBalance::drops)
    ).apply(i, GearBalance::new));

    public RarityInfo of(Rarity r) {
        return rarities.getOrDefault(r, DEFAULT.rarities.get(r));
    }

    public static final GearBalance DEFAULT;

    static {
        Map<Rarity, RarityInfo> r = new EnumMap<>(Rarity.class);
        r.put(Rarity.COMMON, new RarityInfo(1000, 1.0, 1, 0));
        r.put(Rarity.UNCOMMON, new RarityInfo(350, 1.08, 1, 1));
        r.put(Rarity.RARE, new RarityInfo(90, 1.16, 2, 2));
        r.put(Rarity.EPIC, new RarityInfo(18, 1.26, 2, 3));
        r.put(Rarity.LEGENDARY, new RarityInfo(3, 1.38, 3, 4));
        r.put(Rarity.DIVINE, new RarityInfo(0, 1.55, 3, 5));
        Map<AffixType, Range> a = new EnumMap<>(AffixType.class);
        a.put(AffixType.CRIT_CHANCE, new Range(0.01, 0.04));
        a.put(AffixType.CRIT_DAMAGE, new Range(0.05, 0.15));
        a.put(AffixType.DODGE, new Range(0.01, 0.03));
        a.put(AffixType.TOUGHNESS, new Range(0.01, 0.03));
        a.put(AffixType.HASTE, new Range(0.01, 0.04));
        a.put(AffixType.HEAL_POWER, new Range(0.03, 0.08));
        a.put(AffixType.SKILL_POWER, new Range(0.03, 0.08));
        a.put(AffixType.WEAPON_POWER, new Range(0.03, 0.08));
        a.put(AffixType.RANGED_POWER, new Range(0.03, 0.08));
        a.put(AffixType.MOVE_SPEED, new Range(0.01, 0.03));
        a.put(AffixType.FIND, new Range(0.03, 0.1));
        DEFAULT = new GearBalance(Map.copyOf(r),
                new Weapons(6, 1.0, 1.15, 6, 1.0, 5, 0.9),
                new Armor(9, 0.15, 0.35, 0.25, 0.15, 0.1, 15, 75, 0.75),
                0.25, Map.copyOf(a), new Drops(3, 0.04));
    }
}
