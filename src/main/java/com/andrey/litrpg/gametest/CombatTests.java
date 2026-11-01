package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.combat.entity.TrainingDummy;
import com.andrey.litrpg.combat.item.StaffItem;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/** Бой на настоящем сервере: конвейер урона, угроза, лечение, стартовый набор, посох. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CombatTests {
    private CombatTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, Role role, int level) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL); // тестовый сервер ставит творческий режим — в нём урон не проходит
        Vec3 at = h.absoluteVec(new Vec3(0.5, 1, 0.5));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.setRole(p, role);
        PlayerService.setLevel(p, level);
        return p;
    }

    private static TrainingDummy dummy(GameTestHelper h) {
        TrainingDummy d = h.spawn(ModEntities.TRAINING_DUMMY.get(), new BlockPos(0, 1, 0));
        d.setAggressive(true);
        return d;
    }

    /** Свежевошедший игрок 3 с неуязвим (ванильная защита) — урон проверяем после неё. */
    private static final int AFTER_SPAWN = 70;

    /** Урон по своим режется: 100 урона от бойца по союзнику — не больше четверти (с учётом крита). */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void friendlyFireIsReduced(GameTestHelper h) {
        h.getLevel().getServer().setPvpAllowed(true); // на тестовом сервере PvP выключен, на нашем — включён
        ServerPlayer a = player(h, Role.DAMAGE, 20), b = player(h, Role.SUPPORT, 20);
        h.runAfterDelay(AFTER_SPAWN, () -> friendlyFire(h, a, b));
    }

    private static void friendlyFire(GameTestHelper h, ServerPlayer a, ServerPlayer b) {
        var ba = PlayerService.breakdown(a);
        double out = 100 * (1 + ba.get(Derived.WEAPON_POWER)) * ba.get(Derived.DAMAGE_MULT);
        double ff = Balance.get(Balance.COMBAT).friendlyFireMult();
        double tough = PlayerService.breakdown(b).get(Derived.TOUGHNESS);
        double min = out * ff * (1 - tough), max = min * ba.get(Derived.CRIT_DAMAGE);
        float before = b.getHealth();
        b.invulnerableTime = 0;
        b.hurt(a.damageSources().playerAttack(a), 100);
        double lost = before - b.getHealth();
        h.assertTrue(lost >= min - 0.5 && lost <= max + 0.5, "урон по своему " + lost + " вне [" + min + "; " + max + "]");
        h.succeed();
    }

    /** Падение бьёт так же больно, как в обычном Minecraft: доля здоровья, а не жалкие 2 единицы из тысячи. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void environmentScalesWithHealth(GameTestHelper h) {
        ServerPlayer p = player(h, Role.TANK, 40);
        h.runAfterDelay(AFTER_SPAWN, () -> environment(h, p));
    }

    private static void environment(GameTestHelper h, ServerPlayer p) {
        float max = p.getMaxHealth();
        double tough = PlayerService.breakdown(p).get(Derived.TOUGHNESS);
        p.invulnerableTime = 0;
        p.hurt(p.damageSources().fall(), 4);
        double lost = max - p.getHealth();
        double expected = 4 * max / 20 * (1 - tough);
        h.assertTrue(Math.abs(lost - expected) < 1, "падение сняло " + lost + ", ожидалось " + expected);
        h.succeed();
    }

    /** Танк с меньшим уроном держит агро благодаря угрозе ×5; провокация отдаёт врага провокатору. */
    @GameTest(template = "empty")
    public static void tankHoldsAggroAndTauntWorks(GameTestHelper h) {
        ServerPlayer tank = player(h, Role.TANK, 10), dps = player(h, Role.DAMAGE, 10);
        TrainingDummy d = dummy(h);
        DamageService.deal(dps, null, d, 100, WeaponKind.PLAIN);
        ThreatService.tick(d);
        h.assertTrue(d.getTarget() == dps, "первым ударил боец — бьют его");
        // У танка свой урон ×0,6: 50 → 30 урона, × 5 угрозы = 150 > 100 × 1,1.
        DamageService.deal(tank, null, d, 50, WeaponKind.PLAIN);
        ThreatService.tick(d);
        h.assertTrue(d.getTarget() == tank, "танк перебил угрозу бойца — агро у танка");
        ThreatService.taunt(d, dps);
        ThreatService.tick(d);
        h.assertTrue(d.getTarget() == dps, "провокация отдала врага провокатору");
        h.succeed();
    }

    /** Лечение — сила лечения поддержки; лекарь получает угрозу у врагов, которые бьют вылеченного. */
    @GameTest(template = "empty")
    public static void healUsesPowerAndMakesThreat(GameTestHelper h) {
        ServerPlayer healer = player(h, Role.SUPPORT, 20), dps = player(h, Role.DAMAGE, 20);
        PartyService.form(List.of(healer, dps));
        TrainingDummy d = dummy(h);
        DamageService.deal(dps, null, d, 50, WeaponKind.PLAIN);
        dps.setHealth(dps.getMaxHealth() / 4);
        double power = PlayerService.breakdown(healer).get(Derived.HEAL_POWER);
        float real = HealService.heal(healer, dps, 100);
        h.assertTrue(Math.abs(real - 100 * power) < 0.5, "вылечено " + real + ", ожидалось " + 100 * power);
        h.assertTrue(ThreatService.table(d).get(healer.getUUID()) > 0, "лекарь получил угрозу");
        HealService.select(healer, dps.getUUID());
        h.assertTrue(HealService.resolveTarget(healer, 30) == dps, "выбранная цель лечения — боец");
        PartyService.leave(healer);
        PartyService.leave(dps);
        h.succeed();
    }

    /** Выбор роли «Поддержка» выдаёт посох; посох стреляет снарядом и уходит на перезарядку. */
    @GameTest(template = "empty")
    public static void supportKitAndStaff(GameTestHelper h) {
        @SuppressWarnings("removal")
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        Vec3 at = h.absoluteVec(new Vec3(0.5, 1, 0.5));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.chooseRole(p, Role.SUPPORT);
        int slot = p.getInventory().findSlotMatchingItem(ModItems.APPRENTICE_STAFF.toStack());
        h.assertTrue(slot >= 0, "в наборе поддержки есть посох");
        StaffItem staff = ModItems.APPRENTICE_STAFF.get();
        h.assertTrue(staff.cast(p, p.getInventory().getItem(slot)), "первый выстрел");
        h.assertFalse(staff.cast(p, p.getInventory().getItem(slot)), "второй — на перезарядке");
        int bolts = h.getLevel().getEntitiesOfClass(MagicBolt.class, p.getBoundingBox().inflate(4)).size();
        h.assertTrue(bolts == 1, "выпущен один снаряд, а " + bolts);
        h.succeed();
    }
}
