package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.boss.TrialWarden;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Каркас хранителя на настоящем сервере: вступление в бой, масштаб, разметка, щит, срыв подготовки, сброс. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BossTests {
    private static final int AFTER_SPAWN = 70;

    private BossTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, Role role, double x, double z) {
        ServerPlayer p = TestPlayers.create(h);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.setRole(p, role);
        PlayerService.setLevel(p, 10);
        return p;
    }

    private static TrialWarden boss(GameTestHelper h) {
        TrialWarden b = h.spawn(ModEntities.TRIAL_WARDEN.get(), new BlockPos(4, 1, 4));
        b.setArena(b.position(), 8);
        return b;
    }

    /** Отряд из трёх рядом — хранитель вступает в бой со здоровьем «на троих». */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void engagesAndScalesToParty(GameTestHelper h) {
        player(h, Role.TANK, 2.5, 2.5);
        player(h, Role.DAMAGE, 6.5, 2.5);
        player(h, Role.SUPPORT, 2.5, 6.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(5, () -> {
            h.assertTrue(b.engaged(), "хранитель вступил в бой");
            h.assertTrue(b.getTarget() != null, "у хранителя есть цель (по угрозе)");
            double expected = b.p("hp_units") * BossDamage.unit(b.bossLevel())
                    * Balance.get(Balance.BOSSES).common().hpScale(3);
            h.assertTrue(b.partySize() == 3, "отряд трое, а " + b.partySize());
            h.assertTrue(Math.abs(b.getMaxHealth() - expected) < 1, "здоровье " + b.getMaxHealth() + " ≠ " + expected);
            b.resetFight();
            h.succeed();
        });
    }

    /** Танк получает от удара хранителя меньше половины «единицы», боец — около единицы. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void bossHitsRoleScaled(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 2.5, 2.5), dps = player(h, Role.DAMAGE, 6.5, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            double unit = BossDamage.unit(10);
            float t0 = tank.getHealth(), d0 = dps.getHealth();
            BossDamage.hit(b, tank, 0.5);
            BossDamage.hit(b, dps, 0.5);
            double tankLost = t0 - tank.getHealth(), dpsLost = d0 - dps.getHealth();
            double tough = PlayerService.breakdown(tank).get(Derived.TOUGHNESS);
            h.assertTrue(Math.abs(dpsLost - 0.5 * unit) < 1, "боец потерял " + dpsLost + ", ожидалось " + 0.5 * unit);
            h.assertTrue(Math.abs(tankLost - 0.5 * unit * 0.5 * (1 - tough)) < 1, "танк потерял " + tankLost);
            b.resetFight();
            h.succeed();
        });
    }

    /** Разметка бьёт тех, кто внутри В МОМЕНТ удара: вышедший из круга цел. */
    @GameTest(template = "arena", timeoutTicks = 260)
    public static void telegraphHitsOnlyThoseInside(GameTestHelper h) {
        ServerPlayer stay = player(h, Role.DAMAGE, 2.5, 2.5), leave = player(h, Role.DAMAGE, 3.0, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            var cast = b.testCast(20);
            Telegraph.circle(cast, stay.position(), 2, 0.3);
            leave.teleportTo(leave.getX() + 4, leave.getY(), leave.getZ());
            float s0 = stay.getHealth(), l0 = leave.getHealth();
            h.runAfterDelay(25, () -> {
                h.assertTrue(stay.getHealth() < s0, "стоявший в круге получил удар");
                h.assertTrue(leave.getHealth() == l0, "ушедший из круга цел");
                b.resetFight();
                h.succeed();
            });
        });
    }

    /** С 66% — щит: урон не проходит, пока не разбиты узлы силы. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void shieldHoldsUntilNodesBreak(GameTestHelper h) {
        ServerPlayer dps = player(h, Role.DAMAGE, 2.5, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(5, () -> {
            b.setHealth(b.getMaxHealth() * 0.6f);
            h.runAfterDelay(2, () -> {
                h.assertTrue(b.shielded(), "щит поднят на 60%");
                float hp = b.getHealth();
                b.invulnerableTime = 0;
                b.hurt(dps.damageSources().playerAttack(dps), 50);
                h.assertTrue(b.getHealth() == hp, "под щитом урон не проходит");
                for (PowerNode n : h.getLevel().getEntitiesOfClass(PowerNode.class, b.getBoundingBox().inflate(30))) {
                    n.hurt(dps.damageSources().playerAttack(dps), 1_000_000);
                }
                h.runAfterDelay(3, () -> {
                    h.assertFalse(b.shielded(), "узлы разбиты — щит пал");
                    b.resetFight();
                    h.succeed();
                });
            });
        });
    }

    /** Подготовку «Восстановления» сбивает оглушение; самого хранителя оглушение не берёт. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void stunInterruptsMend(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 2.5, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(5, () -> {
            b.testStart("mend");
            h.assertTrue(b.isCasting(), "идёт подготовка");
            StatusService.apply(b, Status.of(StatusType.STUN, "test", tank.getUUID(), 1, h.getLevel().getGameTime(), 30));
            h.assertFalse(b.isCasting(), "подготовка сорвана");
            h.assertFalse(StatusService.has(b, StatusType.STUN), "хранитель не оглушён");
            b.resetFight();
            h.succeed();
        });
    }

    /** Все ушли с арены — через несколько секунд бой сбрасывается, здоровье полное. */
    @GameTest(template = "arena", timeoutTicks = 400)
    public static void resetsWhenEveryoneLeaves(GameTestHelper h) {
        ServerPlayer p = player(h, Role.DAMAGE, 2.5, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(5, () -> {
            h.assertTrue(b.engaged(), "в бою");
            b.setHealth(b.getMaxHealth() * 0.8f);
            p.teleportTo(p.getX() + 60, p.getY(), p.getZ());
            double wait = Balance.get(Balance.BOSSES).common().resetSeconds() * 20 + 10;
            h.runAfterDelay((int) wait, () -> {
                h.assertFalse(b.engaged(), "бой сброшен");
                h.assertTrue(b.getHealth() == b.getMaxHealth(), "здоровье восстановлено");
                h.succeed();
            });
        });
    }
}
