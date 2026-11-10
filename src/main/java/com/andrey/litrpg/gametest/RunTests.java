package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.ModBlocks;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.run.Layout;
import com.andrey.litrpg.run.RunService;
import com.andrey.litrpg.run.RunState;
import com.andrey.litrpg.world.DungeonChunkGenerator;
import com.andrey.litrpg.world.DungeonMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Прохождение на настоящем сервере: мир подземелья генерируется по карте, павший становится духом, вайп возвращает
 * отряд на привал. Состояние прохождения общее для сервера — тесты прохождения идут по одному ({@code batch}).
 */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RunTests {
    private RunTests() {
    }

    private static ServerPlayer member(GameTestHelper h) {
        ServerPlayer p = TestPlayers.create(h);
        PlayerService.setRole(p, Role.DAMAGE);
        return p;
    }

    /**
     * Генератор ставит блоки карты: Печать и пол привала каждого этапа — как в map.bin. (Сервер автотестов не грузит
     * измерения из данных, поэтому чанки генерируются напрямую.)
     */
    @GameTest(template = "empty", batch = "run")
    public static void generatorPlacesMapBlocks(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var gen = new DungeonChunkGenerator(new FixedBiomeSource(biomes.getHolderOrThrow(Biomes.PLAINS)));
        Layout layout = Layout.get();
        h.assertTrue(DungeonMap.get().unknownBlocks().isEmpty(),
                "в карте блоки, которых нет в игре (стали воздухом): " + DungeonMap.get().unknownBlocks());
        h.assertTrue(chunkAt(gen, level, biomes, layout.lobby().seal()).getBlockState(layout.lobby().seal()).is(ModBlocks.SEAL.get()),
                "Печать на месте");
        for (Layout.Stage st : layout.stages()) {
            BlockPos under = BlockPos.containing(st.camp().pos()).below();
            ProtoChunk c = chunkAt(gen, level, biomes, under);
            h.assertTrue(c.getBlockState(under).equals(DungeonMap.get().original(under)), "этап " + st.n() + ": пол привала как в карте");
            h.assertTrue(c.getBlockState(under).isSolid(), "этап " + st.n() + ": под привалом твёрдо");
            h.assertTrue(c.getBlockState(under.above()).isAir(), "этап " + st.n() + ": на привале свободно");
        }
        h.succeed();
    }

    private static ProtoChunk chunkAt(DungeonChunkGenerator gen, ServerLevel level, net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> biomes,
                                      BlockPos pos) {
        ProtoChunk chunk = new ProtoChunk(new ChunkPos(pos), UpgradeData.EMPTY, level, biomes, null);
        gen.fillFromNoise(Blender.empty(), null, null, chunk).join();
        return chunk;
    }

    /** Пал в прохождении — дух (наблюдатель, жив); воскрешение — снова в строю с долей здоровья. Вайп — привал, попытка 2. */
    @GameTest(template = "empty", batch = "run", timeoutTicks = 200)
    public static void spiritReviveAndWipe(GameTestHelper h) {
        var server = h.getLevel().getServer();
        ServerPlayer p = member(h);
        RunService.start(server, List.of(p));
        RunState s = RunService.state(server);
        h.assertTrue(s.stage() == 1 && s.member(p.getUUID()) && s.partySize() == 1, "прохождение началось с отрядом 1");
        h.assertTrue(RunService.inDungeon(p), "отряд перенесён в подземелье");
        Layout.Stage st = RunService.currentStage(server);
        h.assertTrue(p.position().distanceTo(st.camp().pos()) < 1.5, "на привале этапа 1");
        h.runAfterDelay(70, () -> {
            p.kill();
            h.assertTrue(p.isAlive() && p.isSpectator() && s.isSpirit(p.getUUID()), "павший — дух, а не мертвец");
            RunService.reviveForTest(p, 0.3);
            h.assertTrue(!p.isSpectator() && p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "воскрешённый снова в игре");
            h.assertTrue(Math.abs(p.getHealth() - p.getMaxHealth() * 0.3) < 1, "здоровье после воскрешения — 30%");
            p.kill();
            RunService.wipeForTest(server);
            h.assertTrue(s.attempt() == 2 && !s.isSpirit(p.getUUID()) && p.getHealth() == p.getMaxHealth(), "вайп: попытка 2, все живы");
            h.assertTrue(p.position().distanceTo(st.camp().pos()) < 1.5, "вайп вернул на привал");
            RunService.reset(server);
            h.assertTrue(s.stage() == RunState.LOBBY && s.party().isEmpty(), "сброс — снова преддверие");
            h.succeed();
        });
    }
}
