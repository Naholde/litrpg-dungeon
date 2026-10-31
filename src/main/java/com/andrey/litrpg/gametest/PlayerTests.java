package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Персонаж на настоящем сервере: атрибуты Minecraft совпадают с расчётом, очки и роль проверяются сервером. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlayerTests {
    private PlayerTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h) {
        return h.makeMockServerPlayerInLevel();
    }

    /** Здоровье Minecraft = здоровью из расчёта, и выше ванильного предела 1024. */
    @GameTest(template = "empty")
    public static void maxHealthFollowsBreakdown(GameTestHelper h) {
        ServerPlayer p = player(h);
        PlayerService.setRole(p, Role.TANK);
        PlayerService.setLevel(p, 50);
        double expected = PlayerService.breakdown(p).get(Derived.MAX_HEALTH);
        h.assertTrue(expected > 1024, "ожидалось здоровье танка > 1024, а " + expected);
        h.assertTrue(Math.abs(p.getMaxHealth() - expected) < 0.01,
                "здоровье " + p.getMaxHealth() + " ≠ расчёту " + expected);
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "после повышения уровня здоровье полное");
        h.succeed();
    }

    /** Нельзя вложить больше очков, чем заработано; вложение меняет характеристику на 0,3 за очко. */
    @GameTest(template = "empty")
    public static void allocationIsValidated(GameTestHelper h) {
        ServerPlayer p = player(h);
        PlayerService.setRole(p, Role.DAMAGE);
        PlayerService.setLevel(p, 3);
        int free = PlayerService.data(p).freeStatPoints(Balance.get(Balance.PROGRESSION));
        h.assertTrue(free == 20, "на 3 уровне 20 очков, а " + free);
        h.assertFalse(PlayerService.allocate(p, new int[]{21, 0, 0, 0, 0}), "вложено больше, чем есть");
        h.assertFalse(PlayerService.allocate(p, new int[]{-1, 5, 0, 0, 0}), "отрицательное вложение");
        double before = PlayerService.breakdown(p).get(Stat.STRENGTH);
        h.assertTrue(PlayerService.allocate(p, new int[]{10, 0, 0, 0, 0}), "вложение 10 очков");
        double after = PlayerService.breakdown(p).get(Stat.STRENGTH);
        h.assertTrue(Math.abs(after - before - 3) < 1e-9, "10 очков = +3 Силы, а " + (after - before));
        PlayerService.resetStats(p);
        h.assertTrue(PlayerService.data(p).spentStatPoints() == 0, "сброс вернул очки");
        h.succeed();
    }

    /** Роль выбирается один раз; повторный выбор игнорируется. */
    @GameTest(template = "empty")
    public static void roleChosenOnce(GameTestHelper h) {
        ServerPlayer p = player(h);
        h.assertTrue(PlayerService.chooseRole(p, Role.SUPPORT), "первый выбор");
        h.assertFalse(PlayerService.chooseRole(p, Role.TANK), "второй выбор");
        h.assertTrue(PlayerService.data(p).role() == Role.SUPPORT, "роль осталась «Поддержка»");
        h.succeed();
    }

    /** Опыт переливается через уровни. */
    @GameTest(template = "empty")
    public static void xpLevelsUp(GameTestHelper h) {
        ServerPlayer p = player(h);
        var prog = Balance.get(Balance.PROGRESSION);
        PlayerService.addXp(p, prog.xpToNext(1) + prog.xpToNext(2) + 7);
        h.assertTrue(PlayerService.data(p).level() == 3, "уровень 3, а " + PlayerService.data(p).level());
        h.assertTrue(PlayerService.data(p).xp() == 7, "остаток опыта 7");
        h.succeed();
    }
}
