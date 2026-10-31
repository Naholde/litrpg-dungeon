package com.andrey.litrpg.player;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** Роль выбирается при первом входе. {@link #NONE} — ещё не выбрана. */
public enum Role implements StringRepresentable {
    NONE("none", "Без роли", 0xFF9E9E9E, "?", "Роль ещё не выбрана"),
    TANK("tank", "Танк", 0xFF4FA3FF, "⛨", "Держит хранителя и врагов на себе, принимает главный удар"),
    DAMAGE("damage", "Боец", 0xFFFF5A4F, "⚔", "Наносит основной урон, пока танк держит, а лекарь лечит"),
    SUPPORT("support", "Поддержка", 0xFF5CE07A, "✚", "Лечит и поднимает павших; без неё группа не выстоит");

    public static final Codec<Role> CODEC = StringRepresentable.fromEnum(Role::values);

    public final String id;
    public final String title;
    public final int color;
    public final String icon;
    public final String tagline;

    Role(String id, String title, int color, String icon, String tagline) {
        this.id = id;
        this.title = title;
        this.color = color;
        this.icon = icon;
        this.tagline = tagline;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Role byId(String id) {
        for (Role r : values()) if (r.id.equals(id)) return r;
        return NONE;
    }

    public boolean chosen() {
        return this != NONE;
    }
}
