package com.andrey.litrpg.world;

import com.andrey.litrpg.run.Layout;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Генератор мира подземелья: пустота, в которую из {@link DungeonMap} ставятся блоки карты. Свет мир считает сам,
 * текст табличек берётся из схемы ({@link Layout}). Ни рельефа, ни пещер, ни построек.
 */
public class DungeonChunkGenerator extends ChunkGenerator {
    public static final MapCodec<DungeonChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
    ).apply(i, i.stable(DungeonChunkGenerator::new)));

    public DungeonChunkGenerator(BiomeSource biomes) {
        super(biomes);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk) {
        DungeonMap map = DungeonMap.get();
        int cx = chunk.getPos().x, cz = chunk.getPos().z;
        Heightmap ocean = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int sy = chunk.getMinSection(); sy < chunk.getMaxSection(); sy++) {
            short[] data = map.section(cx, sy, cz);
            if (data == null) continue;
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
            for (int i = 0; i < 4096; i++) {
                int idx = data[i];
                if (idx == 0) continue;
                BlockState st = map.state(idx);
                if (st.isAir()) continue;
                int x = i & 15, z = (i >> 4) & 15, y = i >> 8;
                section.setBlockState(x, y, z, st, false);
                int wy = (sy << 4) + y;
                ocean.update(x, wy, z, st);
                surface.update(x, wy, z, st);
                if (st.hasBlockEntity()) {
                    pos.set((cx << 4) + x, wy, (cz << 4) + z);
                    CompoundTag tag = blockEntity(pos, st);
                    if (tag != null) chunk.setBlockEntityNbt(tag);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Данные блока-сущности: текст табличек из схемы. */
    private static CompoundTag blockEntity(BlockPos pos, BlockState st) {
        if (!st.is(net.minecraft.tags.BlockTags.ALL_SIGNS)) return null;
        List<String> lines = Layout.get().signAt(pos);
        if (lines == null) return null;
        CompoundTag tag = new CompoundTag();
        tag.putString("id", st.is(net.minecraft.tags.BlockTags.ALL_HANGING_SIGNS) ? "minecraft:hanging_sign" : "minecraft:sign");
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.put("front_text", signSide(lines, true));
        tag.put("back_text", signSide(List.of(), false));
        tag.putBoolean("is_waxed", true);
        return tag;
    }

    private static CompoundTag signSide(List<String> lines, boolean glow) {
        CompoundTag side = new CompoundTag();
        ListTag msgs = new ListTag();
        for (int i = 0; i < 4; i++) {
            String s = i < lines.size() ? lines.get(i) : "";
            msgs.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(s),
                    net.minecraft.core.RegistryAccess.EMPTY)));
        }
        side.put("messages", msgs);
        side.putString("color", heading(lines) ? "yellow" : "white");
        side.putBoolean("has_glowing_text", glow);
        return side;
    }

    /** Первая строка заглавными — заголовок: табличка жёлтая. */
    private static boolean heading(List<String> lines) {
        return !lines.isEmpty() && lines.get(0).equals(lines.get(0).toUpperCase()) && lines.get(0).length() > 2;
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structures, RandomState random, ChunkAccess chunk) {
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomes, StructureManager structures,
                             ChunkAccess chunk, GenerationStep.Carving step) {
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
    }

    @Override
    public int getGenDepth() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 0;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        BlockState[] col = new BlockState[height.getHeight()];
        java.util.Arrays.fill(col, Blocks.AIR.defaultBlockState());
        return new NoiseColumn(height.getMinBuildHeight(), col);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        info.add("LitRPG: подземелье, карта " + DungeonMap.get().hash());
    }
}
