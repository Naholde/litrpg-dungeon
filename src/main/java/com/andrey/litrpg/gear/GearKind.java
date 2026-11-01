package com.andrey.litrpg.gear;

import com.andrey.litrpg.combat.WeaponKind;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;

/** Что это за вещь и куда надевается. Украшения — в слоты Curios (2 кольца, амулет, оберег). */
public enum GearKind implements StringRepresentable {
    SWORD("sword", "Меч", EquipmentSlot.MAINHAND, WeaponKind.MELEE),
    AXE("axe", "Топор", EquipmentSlot.MAINHAND, WeaponKind.MELEE),
    BOW("bow", "Лук", EquipmentSlot.MAINHAND, WeaponKind.RANGED),
    STAFF("staff", "Посох", EquipmentSlot.MAINHAND, WeaponKind.MAGIC),
    SHIELD("shield", "Щит", EquipmentSlot.OFFHAND, null),
    HELMET("helmet", "Шлем", EquipmentSlot.HEAD, null),
    CHEST("chest", "Доспех", EquipmentSlot.CHEST, null),
    LEGS("legs", "Поножи", EquipmentSlot.LEGS, null),
    BOOTS("boots", "Сапоги", EquipmentSlot.FEET, null),
    RING("ring", "Кольцо", null, null),
    AMULET("amulet", "Амулет", null, null),
    CHARM("charm", "Оберег", null, null);

    public static final Codec<GearKind> CODEC = StringRepresentable.fromEnum(GearKind::values);

    public final String id;
    public final String title;
    /** Слот брони/руки; null — украшение (Curios). */
    public final EquipmentSlot slot;
    /** Род урона для оружия; null — не оружие. */
    public final WeaponKind weapon;

    GearKind(String id, String title, EquipmentSlot slot, WeaponKind weapon) {
        this.id = id;
        this.title = title;
        this.slot = slot;
        this.weapon = weapon;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public boolean isWeapon() {
        return weapon != null;
    }

    public boolean isArmor() {
        return this == HELMET || this == CHEST || this == LEGS || this == BOOTS || this == SHIELD;
    }

    public boolean isAccessory() {
        return slot == null;
    }
}
