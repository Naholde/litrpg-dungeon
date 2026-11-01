package com.andrey.litrpg.world;

import com.andrey.litrpg.LitRPG;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Измерение подземелья: генератор по карте и биомы этапов. Само измерение — data/litrpg/dimension/dungeon.json. */
public final class ModWorld {
    public static final ResourceKey<Level> DUNGEON = ResourceKey.create(Registries.DIMENSION, LitRPG.id("dungeon"));

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, LitRPG.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, LitRPG.MOD_ID);

    static {
        GENERATORS.register("dungeon", () -> DungeonChunkGenerator.CODEC);
        BIOME_SOURCES.register("stages", () -> StageBiomeSource.CODEC);
    }

    private ModWorld() {
    }

    public static void register(IEventBus modBus) {
        GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
    }
}
