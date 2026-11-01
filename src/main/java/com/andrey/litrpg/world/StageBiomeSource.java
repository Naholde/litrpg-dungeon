package com.andrey.litrpg.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

/**
 * Биом по этапу: у каждого этапа свой туман, вода, частицы и звуки. Преддверие — около x = 0, этап n — около x = 1024·n
 * (полоса x ∈ [1024·n, 1024·n + 1024)).
 */
public class StageBiomeSource extends BiomeSource {
    public static final MapCodec<StageBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Biome.CODEC.listOf().fieldOf("stages").forGetter(s -> s.stages)
    ).apply(i, i.stable(StageBiomeSource::new)));

    /** [0] — преддверие, [n] — этап n. */
    private final List<Holder<Biome>> stages;

    public StageBiomeSource(List<Holder<Biome>> stages) {
        this.stages = stages;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return stages.stream();
    }

    /** Номер этапа по координате x (блоки). */
    public static int stageAt(int blockX) {
        return Math.floorDiv(blockX, 1024);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler sampler) {
        int n = stageAt(qx << 2);
        return stages.get(Math.max(0, Math.min(stages.size() - 1, n)));
    }
}
