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
                    * Balance.get(Balance.BOSSES).common().hpScale(3) * BossDamage.earlyHp(b.bossLevel());
            h.assertTrue(b.partySize() == 3, "отряд трое, а " + b.partySize());
            h.assertTrue(Math.abs(b.getMaxHealth() - expected) < 1, "здоровье " + b.getMaxHealth() + " ≠ " + expected);
            b.resetFight();
            h.succeed();
        });
    }

    /**
     * Ритм (фаза 13): за 50 с боя хранитель не начинает навык, пока прошлый не доиграл и не прошла пауза;
     * навыки идут по очереди (не один и тот же подряд), и после серии приходит передышка, когда по нему
     * проходит больше урона.
     */
    @GameTest(template = "arena", timeoutTicks = 1100)
    public static void bossKeepsRhythm(GameTestHelper h) {
        ServerPlayer[] ps = {player(h, Role.TANK, 3.5, 3.5), player(h, Role.DAMAGE, 6.5, 2.5), player(h, Role.SUPPORT, 2.5, 6.5)};
        for (ServerPlayer p : ps) {
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE,
                    1200, 4, false, false));
        }
        TrialWarden b = boss(h);
        long[] lastBusy = {0};
        java.util.List<String> casts = new java.util.ArrayList<>();
        boolean[] wasCasting = {false}, sawTired = {false};
        String[] bad = {null};
        for (int t = 0; t < 1000; t++) {
            h.runAfterDelay(t + 1, () -> {
                for (ServerPlayer p : ps) p.setHealth(p.getMaxHealth());
                long now = h.getLevel().getGameTime();
                boolean casting = b.isCasting();
                if (casting && !wasCasting[0]) {
                    if (now < lastBusy[0] && bad[0] == null) bad[0] = "навык начался на " + (lastBusy[0] - now) + " тиков раньше конца прошлого";
                    casts.add(b.castName().split("[|]")[0]);
                }
                wasCasting[0] = casting;
                if (b.tired()) sawTired[0] = true;
                lastBusy[0] = b.busyUntil();
            });
        }
        h.runAfterDelay(1050, () -> {
            h.assertTrue(bad[0] == null, String.valueOf(bad[0]));
            h.assertTrue(casts.size() >= 4, "мало навыков за 50 с: " + casts);
            for (int i = 1; i < casts.size(); i++) {
                h.assertFalse(casts.get(i).equals(casts.get(i - 1)), "один навык дважды подряд: " + casts);
            }
            h.assertTrue(sawTired[0], "передышки не было: " + casts);
            b.resetFight();
            h.succeed();
        });
    }

    /** Вшестером: хранитель считает отряд из шести и крепче, чем на пятерых; седьмой в отряд не входит. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void sixPlayerParty(GameTestHelper h) {
        java.util.List<ServerPlayer> all = new java.util.ArrayList<>();
        double[][] at = {{2.5, 2.5}, {6.5, 2.5}, {2.5, 6.5}, {6.5, 6.5}, {4.5, 2.5}, {2.5, 4.5}, {6.5, 4.5}};
        Role[] roles = {Role.TANK, Role.SUPPORT, Role.DAMAGE, Role.DAMAGE, Role.DAMAGE, Role.DAMAGE, Role.DAMAGE};
        for (int i = 0; i < 6; i++) all.add(player(h, roles[i], at[i][0], at[i][1]));
        TrialWarden b = boss(h);
        h.runAfterDelay(5, () -> {
            h.assertTrue(b.partySize() == 6, "отряд шесть, а " + b.partySize());
            var c = Balance.get(Balance.BOSSES).common();
            h.assertTrue(c.hpScale(6) > c.hpScale(5), "вшестером здоровья больше, чем впятером");
            double expected = b.p("hp_units") * BossDamage.unit(b.bossLevel()) * c.hpScale(6) * BossDamage.earlyHp(b.bossLevel());
            h.assertTrue(Math.abs(b.getMaxHealth() - expected) < 1, "здоровье " + b.getMaxHealth() + " ≠ " + expected);
            b.resetFight();
            all.add(player(h, roles[6], at[6][0], at[6][1]));
            var party = com.andrey.litrpg.party.PartyService.form(all);
            h.assertTrue(party.members().size() == 6, "в отряде шесть, седьмой лишний: " + party.members().size());
            all.forEach(com.andrey.litrpg.party.PartyService::leave);
            h.succeed();
        });
    }

    /** Танк получает от удара хранителя меньше половины «единицы», боец — около единицы. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void bossHitsRoleScaled(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 2.5, 2.5), dps = player(h, Role.DAMAGE, 6.5, 2.5);
        TrialWarden b = boss(h);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            // Удар хранителя уровня b — в «единицах» его уровня, смягчён на ранних этапах (earlyDamage).
            double unit = BossDamage.unit(b.bossLevel()) * BossDamage.earlyDamage(b.bossLevel());
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
