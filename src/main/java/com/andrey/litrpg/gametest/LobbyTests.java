package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.mob.ModMobs;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.run.Difficulty;
import com.andrey.litrpg.run.Records;
import com.andrey.litrpg.run.RunService;
import com.andrey.litrpg.run.RunState;
import com.andrey.litrpg.run.RunStats;
import com.andrey.litrpg.run.Titles;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Обновление 15 (лобби): новый забег с 1 уровня, роль — только в подземелье, сложность, статистика забега и вечные
 * рекорды с титулами. Состояние прохождения общее для сервера, а тесты одной партии идут одновременно — у каждого своя партия.
 */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LobbyTests {
    private LobbyTests() {
    }

    /**
     * Забег из лобби: персонаж с 1 уровня (уровень, роль, вещи сброшены), сложность «обычная» ослабляет монстров ×0,65.
     * Роль выбирается только на этапе 1; когда выбрана у всех — открыт путь с привала («roles»). Вне забега роль не выбрать.
     */
    @GameTest(template = "empty", batch = "lobby_fresh", timeoutTicks = 200)
    public static void freshRunAndRoleInDungeon(GameTestHelper h) {
        var server = h.getLevel().getServer();
        RunService.testing = true;
        ServerPlayer p = TestPlayers.create(h);
        PlayerService.setRole(p, Role.DAMAGE);
        PlayerService.setLevel(p, 20);
        p.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        DungeonMob before = h.spawn(ModMobs.TYPES.get(Bestiary.CRYPT_WARRIOR.id()).get(), new BlockPos(2, 1, 2));
        before.setup(10, MobRank.NORMAL);
        double heroicHp = before.getMaxHealth();

        RunService.start(server, List.of(p), Difficulty.NORMAL, true);
        RunState s = RunService.state(server);
        h.assertTrue(PlayerService.data(p).level() == 1, "новый забег — с 1 уровня: " + PlayerService.data(p).level());
        h.assertTrue(!PlayerService.data(p).role().chosen(), "роль сброшена — её выбирают в подземелье");
        h.assertTrue(p.getInventory().countItem(Items.DIAMOND) == 0, "вещи прошлого забега убраны");
        h.assertTrue(s.difficulty() == Difficulty.NORMAL && Math.abs(Difficulty.active(server).hp() - 0.65) < 1e-6
                && Math.abs(Difficulty.active(server).damage() - 0.7) < 1e-6, "обычная: здоровье ×0,65, урон ×0,7");
        DungeonMob normal = h.spawn(ModMobs.TYPES.get(Bestiary.CRYPT_WARRIOR.id()).get(), new BlockPos(4, 1, 4));
        normal.setup(10, MobRank.NORMAL);
        h.assertTrue(Math.abs(normal.getMaxHealth() / heroicHp - 0.65) < 0.01,
                "монстр на обычной — 65% здоровья: " + normal.getMaxHealth() + " / " + heroicHp);
        h.assertTrue(RunService.mayChooseRole(p), "на этапе 1 участник выбирает роль");

        h.runAfterDelay(70, () -> {
            RunService.tickRolesForTest(server);
            h.assertTrue(!s.done().contains("roles"), "пока роли нет — путь с привала закрыт");
            h.assertTrue(PlayerService.chooseRole(p, Role.TANK), "роль выбрана в подземелье");
            h.assertTrue(!p.getInventory().isEmpty(), "стартовый набор роли выдан");
            RunService.tickRolesForTest(server);
            h.assertTrue(s.done().contains("roles"), "роль у всех — путь открыт");
            RunService.reset(server);
            h.assertTrue(!RunService.mayChooseRole(p), "вне забега роль не выбрать");
            RunService.testing = false;
            h.succeed();
        });
    }

    /**
     * Статистика забега считается участнику и в конце уходит в вечные рекорды: суммы профиля, рекорды «за забег»,
     * история; за прохождение — титулы (первопроходец, герой, без потерь, бессмертный, рекорд урона).
     */
    @GameTest(template = "empty", batch = "lobby_stats", timeoutTicks = 200)
    public static void statsGoToRecordsAndTitles(GameTestHelper h) {
        var server = h.getLevel().getServer();
        RunService.testing = true;
        ServerPlayer p = TestPlayers.create(h);
        PlayerService.setRole(p, Role.DAMAGE);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(3, 1, 3));
        RunService.start(server, List.of(p), Difficulty.HEROIC, false);
        RunState s = RunService.state(server);
        h.runAfterDelay(70, () -> {
            double record = System.currentTimeMillis();   // растёт от запуска к запуску — заведомо новый рекорд
            RunStats.dealt(p, zombie, record);
            RunStats.dealt(p, p, 999);                 // по игроку — не урон по врагу
            RunStats.healed(p, 1200);
            RunStats.taken(p, 900);
            RunStats.revived(p);
            RunStats.killed(p);
            RunStats.Line l = s.stats().get(p.getUUID());
            h.assertTrue(l != null && l.dealt == record && l.healed == 1200 && l.taken == 900 && l.revives == 1 && l.kills == 1,
                    "строка забега посчитана");
            h.assertTrue(l.biggestHit == record, "самый сильный удар запомнен");
            RunService.finishForTest(server);
            Records r = Records.get(server);
            Records.Profile prof = r.profileOrNull(p.getUUID());
            h.assertTrue(prof != null && prof.runs == 1 && prof.completedHeroic == 1 && prof.dealt == record && prof.healed == 1200,
                    "суммы перенесены в профиль");
            for (Titles t : List.of(Titles.PATHFINDER, Titles.HERO, Titles.FLAWLESS, Titles.IMMORTAL, Titles.DESTROYER)) {
                h.assertTrue(prof.titles.contains(t.id), "титул «" + t.name + "» выдан");
            }
            h.assertTrue(!prof.title.isEmpty(), "первый титул стал активным");
            h.assertTrue(r.single().get("heroic:dealt").value() == record, "рекорд урона за забег (героическая)");
            h.assertTrue(!r.history().isEmpty() && r.history().get(0).completed() && r.history().get(0).difficulty().equals("heroic"),
                    "забег записан в историю");
            h.assertTrue(r.bestRun().containsKey("heroic"), "лучший забег на героической есть");
            RunService.reset(server);
            RunService.testing = false;
            h.succeed();
        });
    }

    /**
     * Сбор в лобби: двое встали в лобби; один готов — отсчёта нет; оба — отсчёт; один передумал — отсчёт отменён;
     * снова оба — через 5 с забег на выбранной сложности (обычная), персонажи с 1 уровня.
     */
    @GameTest(template = "empty", batch = "lobby_gather", timeoutTicks = 300)
    public static void gatherReadyCountdownStart(GameTestHelper h) {
        var server = h.getLevel().getServer();
        RunService.testing = true;
        com.andrey.litrpg.run.LobbyService.resetForTest();
        RunService.reset(server);
        RunService.setMinPlayers(server, 2);
        ServerPlayer a = TestPlayers.create(h), b = TestPlayers.create(h);
        PlayerService.setLevel(a, 15);
        h.runAfterDelay(70, () -> {
            // на сервере проверок в лобби падают посторонние тестовые игроки — считать только этих двоих
            com.andrey.litrpg.run.LobbyService.onlyForTest = java.util.Set.of(a.getUUID(), b.getUUID());
            for (ServerPlayer p : List.of(a, b)) com.andrey.litrpg.run.RunService.toLobbyForTest(p);
            var lobby = com.andrey.litrpg.run.LobbyService.inLobby(server);
            h.assertTrue(lobby.contains(a) && lobby.contains(b), "оба в лобби");
            var act = com.andrey.litrpg.net.LobbyActionPayload.Action.READY;
            com.andrey.litrpg.run.LobbyService.act(a, act, "");
            com.andrey.litrpg.run.LobbyService.tick(server);
            h.assertTrue(!com.andrey.litrpg.run.LobbyService.countingForTest(), "готов один из двух — ждём");
            com.andrey.litrpg.run.LobbyService.act(b, act, "");
            com.andrey.litrpg.run.LobbyService.tick(server);
            h.assertTrue(com.andrey.litrpg.run.LobbyService.countingForTest(), "готовы все — отсчёт");
            com.andrey.litrpg.run.LobbyService.act(b, act, "");
            com.andrey.litrpg.run.LobbyService.tick(server);
            h.assertTrue(!com.andrey.litrpg.run.LobbyService.countingForTest(), "передумал — отсчёт остановлен");
            com.andrey.litrpg.run.LobbyService.act(b, act, "");
            com.andrey.litrpg.run.LobbyService.tick(server);
            h.assertTrue(com.andrey.litrpg.run.LobbyService.countingForTest(), "снова все — снова отсчёт");
        });
        h.runAfterDelay(70 + 5 * 20 + 5, () -> {
            com.andrey.litrpg.run.LobbyService.tick(server);
            RunState s = RunService.state(server);
            h.assertTrue(s.stage() == 1 && s.member(a.getUUID()) && s.member(b.getUUID()), "забег начался с обоими: этап " + s.stage() + ", отряд " + s.party().size()
                    + ", отсчёт " + com.andrey.litrpg.run.LobbyService.countingForTest() + ", в лобби "
                    + com.andrey.litrpg.run.LobbyService.inLobby(server).size() + ", a в лобби " + com.andrey.litrpg.run.LobbyService.inLobby(server).contains(a));
            h.assertTrue(s.difficulty() == Difficulty.NORMAL, "сложность — выбранная (обычная)");
            h.assertTrue(PlayerService.data(a).level() == 1, "персонаж с 1 уровня");
            RunService.reset(server);
            RunService.setMinPlayers(server, 0);
            com.andrey.litrpg.run.LobbyService.resetForTest();
            RunService.testing = false;
            h.succeed();
        });
    }
}
