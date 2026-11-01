package com.andrey.litrpg.gear;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Редкость вещи. Каждая следующая заметно реже (веса — в gear.json); божественные — только из особых мест. */
public enum Rarity implements StringRepresentable {
    COMMON("common", "Обычный", 0xFFC8C8C8),
    UNCOMMON("uncommon", "Необычный", 0xFF5CE07A),
    RARE("rare", "Редкий", 0xFF4FA3FF),
    EPIC("epic", "Эпический", 0xFFB46BFF),
    LEGENDARY("legendary", "Легендарный", 0xFFFFA733),
    DIVINE("divine", "Божественный", 0xFFFF5AA0);

    public static final Codec<Rarity> CODEC = StringRepresentable.fromEnum(Rarity::values);

    public final String id;
    public final String title;
    public final int color;

    Rarity(String id, String title, int color) {
        this.id = id;
        this.title = title;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public Rarity next() {
        return this == DIVINE ? DIVINE : values()[ordinal() + 1];
    }
}
