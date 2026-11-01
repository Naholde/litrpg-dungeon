package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.run.Layout;
import com.andrey.litrpg.run.RunService;
import com.andrey.litrpg.run.RunState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Загадки и ловушки на маленьком «этапе» прямо на арене теста: рычаги, плиты на разделение (двое стоят на двух
 * плитах), текстовая загадка (ответ в чат), ловушка бьёт стоящего в разметке. Схема подземелья подменяется на время
 * теста, поэтому тест идёт один в своей пачке.
 */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MechTests {
    private MechTests() {
    }

    @GameTest(template = "arena", batch = "mech", timeoutTicks = 400)
    public static void puzzlesAndTraps(GameTestHelper h) {
        var server = h.getLevel().getServer();
        BlockPos o = h.absolutePos(BlockPos.ZERO);
        BlockPos lever1 = h.absolutePos(new BlockPos(1, 2, 1)), lever2 = h.absolutePos(new BlockPos(7, 2, 1));
        BlockPos plate1 = h.absolutePos(new BlockPos(2, 1, 7)), plate2 = h.absolutePos(new BlockPos(6, 1, 7));
        for (BlockPos l : List.of(lever1, lever2)) {
            h.getLevel().setBlockAndUpdate(l.south(), Blocks.STONE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(l, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.WALL)
                    .setValue(LeverBlock.FACING, Direction.NORTH));
        }
        for (BlockPos pl : List.of(plate1, plate2)) h.getLevel().setBlockAndUpdate(pl, Blocks.STONE_PRESSURE_PLATE.defaultBlockState());
        Layout.Box all = new Layout.Box(o.getX() - 2, o.getY() - 4, o.getZ() - 2, o.getX() + 10, o.getY() + 8, o.getZ() + 10);
        Layout.Box riddleArea = new Layout.Box(o.getX() + 3, o.getY() + 1, o.getZ() + 3, o.getX() + 5, o.getY() + 2, o.getZ() + 5);
        Layout.Box trapArea = new Layout.Box(o.getX() + 7, o.getY() + 1, o.getZ() + 4, o.getX() + 8, o.getY() + 1, o.getZ() + 5);
        Layout.Spot camp = new Layout.Spot(o.getX() + 4.5, o.getY() + 1, o.getZ() + 4.5, 0);
        Layout.Box rift = new Layout.Box(o.getX(), o.getY() + 1, o.getZ(), o.getX(), o.getY() + 1, o.getZ());
        List<Layout.Mech> mechs = List.of(
                new Layout.Mech("t_levers", "levers", List.of(lever1, lever2), Optional.empty(), List.of(), "Рычаги", Map.of()),
                new Layout.Mech("t_plates", "plates", List.of(plate1, plate2), Optional.empty(), List.of(), "Плиты", Map.of("hold", 1.0)),
                new Layout.Mech("t_riddle", "riddle", List.of(), Optional.of(riddleArea), List.of("яма"), "Загадка", Map.of()),
                new Layout.Mech("t_trap", "hazard", List.of(), Optional.of(trapArea), List.of(), "",
                        Map.of("period", 1.0, "warn", 0.25, "share", 0.3)));
        Layout.Stage st = new Layout.Stage(1, "test", "Проверка", List.of(1, 10), "crypt", all, camp, camp, rift, List.of("mech:t_levers"),
                List.of(), List.of(), List.of(), List.of(), new Layout.Extra(mechs, List.of(), List.of()));
        Layout real = Layout.get();
        Layout.override(new Layout(1, real.lobby(), List.of(st)));
        RunService.testing = true;

        ServerPlayer a = TestPlayers.create(h), b = TestPlayers.create(h);
        PlayerService.setRole(a, Role.TANK);
        PlayerService.setRole(b, Role.SUPPORT);
        RunService.start(server, List.of(a, b));
        RunState s = RunService.state(server);
        h.runAfterDelay(70, () -> {
            // рычаги
            for (BlockPos l : List.of(lever1, lever2)) {
                h.getLevel().setBlockAndUpdate(l, h.getLevel().getBlockState(l).setValue(LeverBlock.POWERED, true));
            }
            // двое — на плиты; третий игрок в ловушку не нужен: ловушка бьёт того, кто стоит в разметке
            a.teleportTo(plate1.getX() + 0.5, plate1.getY(), plate1.getZ() + 0.5);
            b.teleportTo(plate2.getX() + 0.5, plate2.getY(), plate2.getZ() + 0.5);
        });
        h.runAfterDelay(110, () -> {
            h.assertTrue(s.done().contains("mech:t_levers"), "рычаги: загадка решена");
            h.assertTrue(s.done().contains("mech:t_plates"), "плиты: двое продержались секунду");
            // текстовая загадка: ответ у алтаря (неверный — нет, верный — да)
            b.teleportTo(riddleArea.x1() + 1.5, riddleArea.y1(), riddleArea.z1() + 1.5);
            RunService.chatForTest(b, "дыра");
            RunService.chatForTest(b, "  Яма! ");
            // ловушка: танк в разметке
            a.teleportTo(trapArea.x1() + 0.5, trapArea.y1(), trapArea.z1() + 0.5);
            a.setHealth(a.getMaxHealth());
        });
        h.runAfterDelay(160, () -> {
            h.assertTrue(s.done().contains("mech:t_riddle"), "загадка: ответ «Яма!» принят");
            h.assertTrue(a.getHealth() < a.getMaxHealth() * 0.95, "ловушка ударила стоящего в разметке");
            RunService.reset(server);
            RunService.testing = false;
            Layout.override(null);
            h.succeed();
        });
    }
}
