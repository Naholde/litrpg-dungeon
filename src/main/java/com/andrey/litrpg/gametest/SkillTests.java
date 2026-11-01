package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.combat.entity.TrainingDummy;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.SkillRegistry;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Навыки и классы на настоящем сервере. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SkillTests {
    private static final int AFTER_SPAWN = 70;

    private SkillTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, Role role, int level, double x) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(x, 1, 4.5));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.setRole(p, role);
        PlayerService.setLevel(p, level);
        return p;
    }

    private static TrainingDummy dummy(GameTestHelper h) {
        TrainingDummy d = h.spawn(ModEntities.TRAINING_DUMMY.get(), new BlockPos(6, 1, 4));
        d.setAggressive(true);
        return d;
    }

    private static void aim(ServerPlayer p, TrainingDummy d) {
        p.lookAt(EntityAnchorArgument.Anchor.EYES, d.position().add(0, 1.2, 0));
    }

    /** Провокация: танк забирает врага у бойца, который нанёс намного больше урона. */
    @GameTest(template = "arena")
    public static void tauntSkillTakesTheTarget(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 5, 2.5), dps = player(h, Role.DAMAGE, 5, 3.0);
        TrainingDummy d = dummy(h);
        DamageService.deal(dps, null, d, 500, WeaponKind.PLAIN);
        ThreatService.tick(d);
        h.assertTrue(d.getTarget() == dps, "сначала бьют бойца");
        aim(tank, d);
        var seen = SkillService.enemyTarget(tank, 14);
        h.assertTrue(seen == d, "манекен под прицелом");
        h.assertTrue(SkillService.use(tank, 0), "провокация сработала");
        DamageService.deal(dps, null, d, 2000, WeaponKind.PLAIN);
        ThreatService.tick(d);
        h.assertTrue(d.getTarget() == tank, "после провокации враг бьёт танка, даже когда боец бьёт сильнее");
        h.assertFalse(SkillService.use(tank, 0), "провокация на перезарядке");
        h.succeed();
    }

    /** Читаемое лечение: шаг — и чтение прервано; стоя на месте — лечит через 1,5 с. */
    @GameTest(template = "arena", timeoutTicks = 300)
    public static void castIsInterruptedByMoving(GameTestHelper h) {
        ServerPlayer sup = player(h, Role.SUPPORT, 5, 2.5);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            sup.setHealth(sup.getMaxHealth() / 2);
            h.assertTrue(SkillService.use(sup, 0), "начато чтение «Исцеления»");
            h.assertTrue(SkillService.state(sup).casting() != null, "идёт чтение");
            sup.teleportTo(sup.getX() + 1, sup.getY(), sup.getZ());
            SkillService.tick(sup);
            h.assertTrue(SkillService.state(sup).casting() == null, "шаг прервал чтение");
            float before = sup.getHealth();
            h.runAfterDelay(25, () -> {
                h.assertTrue(SkillService.use(sup, 0), "второе чтение (общая перезарядка прошла)");
                var startPos = sup.position();
                h.runAfterDelay(35, () -> {
                    SkillService.tick(sup); // у тестового игрока нет игрового тика — двигаем таймер вручную
                    h.assertTrue(sup.getHealth() > before + 1, "стоя на месте — вылечился: " + before + " → " + sup.getHealth()
                            + "; позиция " + startPos + " → " + sup.position() + ", на земле " + sup.onGround()
                            + ", чтение " + SkillService.state(sup).casting());
                    h.succeed();
                });
            });
        });
    }

    /** Ярость: копится от ударов, «Исступление» без 40 ярости не применить, с ней — даёт вампиризм. */
    @GameTest(template = "arena")
    public static void berserkerRageGatesFrenzy(GameTestHelper h) {
        ServerPlayer b = player(h, Role.DAMAGE, 20, 2.5);
        PlayerService.setClass(b, ClassRegistry.BERSERKER.id());
        int frenzy = ClassRegistry.loadout(Role.DAMAGE, "berserker", 20).indexOf(SkillRegistry.FRENZY);
        h.assertTrue(frenzy >= 0, "на 20 уровне Исступление открыто");
        h.assertFalse(SkillService.use(b, frenzy), "без ярости нельзя");
        for (int i = 0; i < 7; i++) SkillService.onDealt(b);
        h.assertTrue(SkillService.state(b).resource() >= 40, "ярость накопилась: " + SkillService.state(b).resource());
        h.assertTrue(SkillService.use(b, frenzy), "с яростью — можно");
        h.assertTrue(StatusService.has(b, StatusType.LEECH), "вампиризм включён");
        h.assertTrue(SkillService.state(b).resource() < 10, "ярость потрачена");
        h.succeed();
    }

    /** «Под прикрытие»: доля урона по союзнику уходит стражнику. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void guardianCoverRedirectsDamage(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 10, 2.5), dps = player(h, Role.DAMAGE, 10, 3.0);
        PlayerService.setClass(tank, ClassRegistry.GUARDIAN.id());
        PartyService.form(List.of(tank, dps));
        TrainingDummy d = dummy(h);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            HealService.select(tank, dps.getUUID());
            int cover = ClassRegistry.loadout(Role.TANK, "guardian", 10).indexOf(SkillRegistry.COVER);
            h.assertTrue(SkillService.use(tank, cover), "прикрытие наложено");
            h.assertTrue(StatusService.has(dps, StatusType.GUARD), "боец под защитой");
            float tankBefore = tank.getHealth(), dpsBefore = dps.getHealth();
            dps.invulnerableTime = 0;
            dps.hurt(d.damageSources().mobAttack(d), 100);
            h.assertTrue(tank.getHealth() < tankBefore, "часть урона досталась стражнику");
            h.assertTrue(dpsBefore - dps.getHealth() < 100, "боец получил меньше 100: " + (dpsBefore - dps.getHealth()));
            PartyService.leave(tank);
            PartyService.leave(dps);
            h.succeed();
        });
    }

    /** «Несокрушимый»: смертельный удар оставляет 1 здоровья. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void unbreakableKeepsOneHealth(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 44, 2.5);
        PlayerService.setClass(tank, ClassRegistry.GUARDIAN.id());
        TrainingDummy d = dummy(h);
        h.runAfterDelay(AFTER_SPAWN, () -> {
            int slot = ClassRegistry.loadout(Role.TANK, "guardian", 44).indexOf(SkillRegistry.UNBREAKABLE);
            h.assertTrue(SkillService.use(tank, slot), "Несокрушимый применён");
            tank.invulnerableTime = 0;
            tank.hurt(d.damageSources().mobAttack(d), 1_000_000);
            h.assertTrue(tank.isAlive() && tank.getHealth() >= 0.99f && tank.getHealth() <= 1.01f,
                    "жив с 1 здоровья: " + tank.getHealth());
            h.succeed();
        });
    }

    /** На 10 уровне Система предлагает классы роли; выбор даёт навыки класса; ранги стоят очки класса. */
    @GameTest(template = "arena")
    public static void classOfferChooseAndRanks(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        PlayerService.chooseRole(p, Role.SUPPORT);
        PlayerService.setLevel(p, 12);
        var d = PlayerService.data(p);
        h.assertTrue(d.offers().contains("cleric"), "Лекарь предложен: " + d.offers());
        h.assertFalse(PlayerService.chooseClass(p, "berserker"), "чужой класс не выбрать");
        h.assertTrue(PlayerService.chooseClass(p, "cleric"), "Лекарь выбран");
        h.assertTrue(SkillService.loadout(d).contains(SkillRegistry.BANDAGE), "на панели навыки Лекаря");
        h.assertTrue(d.freeClassPoints(com.andrey.litrpg.core.balance.Balance.get(com.andrey.litrpg.core.balance.Balance.PROGRESSION)) == 6,
                "на 12 уровне 6 очков класса");
        h.assertTrue(PlayerService.rankUp(p, SkillRegistry.BANDAGE.id), "ранг 1");
        h.assertTrue(PlayerService.rankUp(p, SkillRegistry.BANDAGE.id), "ранг 2");
        h.assertTrue(PlayerService.rankUp(p, SkillRegistry.BANDAGE.id), "ранг 3 (1+2+3 = 6 очков)");
        h.assertFalse(PlayerService.rankUp(p, SkillRegistry.BANDAGE.id), "на ранг 4 очков нет");
        h.assertFalse(PlayerService.rankUp(p, SkillRegistry.MIRACLE.id), "закрытый навык не качается");
        h.succeed();
    }
}
