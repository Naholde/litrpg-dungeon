package com.andrey.litrpg.gear;

import com.andrey.litrpg.player.Derived;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/**
 * Свойства вещей — прибавки к производным величинам персонажа ({@link Derived}).
 * Значение в данных — доля (0,03 = +3%); диапазоны по уровню — в gear.json.
 */
public enum AffixType implements StringRepresentable {
    CRIT_CHANCE("crit_chance", Derived.CRIT_CHANCE, "к шансу крита"),
    CRIT_DAMAGE("crit_damage", Derived.CRIT_DAMAGE, "к силе крита"),
    DODGE("dodge", Derived.DODGE, "к уклонению"),
    TOUGHNESS("toughness", Derived.TOUGHNESS, "к стойкости"),
    HASTE("haste", Derived.HASTE, "к ускорению навыков"),
    HEAL_POWER("heal_power", Derived.HEAL_POWER, "к силе лечения"),
    SKILL_POWER("skill_power", Derived.SKILL_POWER, "к силе навыков"),
    WEAPON_POWER("weapon_power", Derived.WEAPON_POWER, "к урону в ближнем бою"),
    RANGED_POWER("ranged_power", Derived.RANGED_POWER, "к урону луком"),
    MOVE_SPEED("move_speed", Derived.MOVE_SPEED, "к скорости"),
    FIND("find", Derived.FIND, "к находкам");

    public static final Codec<AffixType> CODEC = StringRepresentable.fromEnum(AffixType::values);

    public final String id;
    public final Derived derived;
    public final String text;

    AffixType(String id, Derived derived, String text) {
        this.id = id;
        this.derived = derived;
        this.text = text;
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
