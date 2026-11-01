package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.GearKind;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.gear.Wallet;
import com.andrey.litrpg.net.ShopPayload;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import com.andrey.litrpg.shop.ShopService;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Вещи, кошелёк, лавка, добыча — на настоящем сервере. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GearTests {
    private GearTests() {
    }

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper h, Role role, int level) {
        ServerPlayer p = TestPlayers.create(h);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 1, 2.5));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.setRole(p, role);
        PlayerService.setLevel(p, level);
        return p;
    }

    /** Надетый доспех прибавляет броню и характеристики; удар монстра режется бронёй. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void armorCountsAndReducesDamage(GameTestHelper h) {
        ServerPlayer p = player(h, Role.DAMAGE, 20);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(6, 1, 6));
        h.runAfterDelay(70, () -> {
            float before = p.getHealth();
            p.invulnerableTime = 0;
            p.hurt(zombie.damageSources().mobAttack(zombie), 100);
            double bare = before - p.getHealth();
            p.setHealth(p.getMaxHealth());
            var chest = GearGen.make(GearKind.CHEST, 20, Rarity.EPIC, Role.DAMAGE, p.getRandom());
            p.setItemSlot(EquipmentSlot.CHEST, chest);
            PlayerService.refresh(p);
            var b = PlayerService.breakdown(p);
            h.assertTrue(b.get(Derived.ARMOR) > 0, "броня доспеха учтена");
            var gd = chest.get(ModComponents.GEAR.get());
            for (Stat s : Stat.ALL) {
                if (gd.stat(s) > 0) h.assertTrue(b.lines(s).stream().anyMatch(l -> l.source().equals("Экипировка")),
                        "строка «Экипировка» у " + s.title);
            }
            before = p.getHealth();
            p.invulnerableTime = 0;
            p.hurt(zombie.damageSources().mobAttack(zombie), 100);
            double armored = before - p.getHealth();
            h.assertTrue(armored < bare, "с бронёй урон меньше: " + armored + " < " + bare);
            zombie.discard();
            h.succeed();
        });
    }

    /** Кошелёк: списание со сдачей, нехватка ничего не трогает. */
    @GameTest(template = "empty")
    public static void walletTakesWithChange(GameTestHelper h) {
        ServerPlayer p = player(h, Role.DAMAGE, 5);
        Wallet.give(p, 200);
        h.assertTrue(Wallet.total(p) == 200, "выдано 200");
        h.assertTrue(Wallet.take(p, 37), "списано 37");
        h.assertTrue(Wallet.total(p) == 163, "осталось 163, а " + Wallet.total(p));
        h.assertFalse(Wallet.take(p, 1000), "не хватает — отказ");
        h.assertTrue(Wallet.total(p) == 163, "при отказе ничего не пропало");
        h.succeed();
    }

    /** Вещь из лавки покупается один раз; перековка меняет свойства и считается. */
    @GameTest(template = "arena")
    public static void shopBuysOnceAndRerolls(GameTestHelper h) {
        ServerPlayer p = player(h, Role.SUPPORT, 12);
        var t = h.spawn(ModEntities.TRADER.get(), new BlockPos(4, 1, 2));
        t.setStage(2, 15);
        Wallet.give(p, 100_000);
        int before = p.getInventory().items.stream().mapToInt(s -> s.has(ModComponents.GEAR.get()) ? 1 : 0).sum();
        ShopService.act(p, t.getId(), ShopPayload.Action.BUY, 0);
        ShopService.act(p, t.getId(), ShopPayload.Action.BUY, 0);
        int after = p.getInventory().items.stream().mapToInt(s -> s.has(ModComponents.GEAR.get()) ? 1 : 0).sum();
        h.assertTrue(after - before == 1, "куплена ровно одна вещь, а " + (after - before));
        var staff = GearGen.make(GearKind.STAFF, 12, Rarity.RARE, Role.SUPPORT, p.getRandom());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, staff);
        ShopService.act(p, t.getId(), ShopPayload.Action.REROLL, 0);
        h.assertTrue(p.getMainHandItem().get(ModComponents.GEAR.get()).rerolls() == 1, "перековка засчитана");
        h.succeed();
    }

    /** Убитый в бою монстр приносит участнику монеты. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void killGivesPersonalCoins(GameTestHelper h) {
        ServerPlayer p = player(h, Role.DAMAGE, 10);
        var zombie = h.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        h.runAfterDelay(5, () -> {
            long before = Wallet.total(p);
            zombie.hurt(p.damageSources().playerAttack(p), 100_000);
            h.assertTrue(Wallet.total(p) > before, "монеты пришли в кошелёк");
            h.succeed();
        });
    }
}
