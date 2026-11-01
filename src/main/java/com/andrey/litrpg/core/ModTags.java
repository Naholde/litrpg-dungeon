package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

/** Теги мода. Содержимое — в {@code data/litrpg/tags/}. */
public final class ModTags {
    /** Хранители: от их ударов не уклониться, их урон режется множителем роли. */
    public static final TagKey<EntityType<?>> BOSSES = TagKey.create(Registries.ENTITY_TYPE, LitRPG.id("bosses"));

    private ModTags() {
    }
}
