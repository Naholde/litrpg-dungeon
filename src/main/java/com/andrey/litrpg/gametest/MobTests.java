package com.andrey.litrpg.gametest;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.gear.Wallet;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.DungeonMob;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.mob.ModMobs;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Бестиарий на настоящем сервере. */
@GameTestHolder(LitRPG.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MobTests {
    private MobTests() {
    }

    private static ServerPlayer player(GameTestHelper h, int level, double x, double z) {
        ServerPlayer p = TestPlayers.create(h);
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        p.teleportTo(h.getLevel(), at.x, at.y, at.z, 0, 0);
        PlayerService.setRole(p, Role.DAMAGE);
        PlayerService.setLevel(p, level);
        return p;
    }

    private static DungeonMob mob(GameTestHelper h, Bestiary.Species s, int level, MobRank rank, int x, int z) {
        DungeonMob m = h.spawn(ModMobs.TYPES.get(s.id()).get(), new BlockPos(x, 1, z));
        m.setup(level, rank);
        return m;
    }

    /** Здоровье — доли «единицы» уровня × ранг; элитный крепче обычного в 2,2 раза. */
    @GameTest(template = "arena")
    public static void healthByLevelAndRank(GameTestHelper h) {
        DungeonMob n = mob(h, Bestiary.CRYPT_WARRIOR, 10, MobRank.NORMAL, 2, 2);
        DungeonMob e = mob(h, Bestiary.CRYPT_WARRIOR, 10, MobRank.ELITE, 6, 6);
        double expected = Bestiary.CRYPT_WARRIOR.param("hp_units") * BossDamage.unit(10);
        h.assertTrue(Math.abs(n.getMaxHealth() - expected) < 1, "здоровье " + n.getMaxHealth() + " ≠ " + expected);
        h.assertTrue(Math.abs(e.getMaxHealth() / n.getMaxHealth() - 2.2) < 0.01, "элитный ×2,2");
        h.assertTrue(n.getCustomName().getString().contains("Ур. 10"), "уровень в имени");
        h.succeed();
    }

    /** Страж: удар в лицо — втрое меньше, со спины — полностью. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void guardShieldFront(GameTestHelper h) {
        ServerPlayer front = player(h, 10, 4.5, 1.5), back = player(h, 10, 4.5, 7.5);
        DungeonMob g = mob(h, Bestiary.BONE_GUARD, 10, MobRank.NORMAL, 4, 4);
        h.runAfterDelay(2, () -> {
            g.setNoAi(true);
            g.setYRot(180);
            g.setYBodyRot(180); // смотрит на север, к «переднему» игроку
            float hp = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(front.damageSources().playerAttack(front), 30);
            float frontLost = hp - g.getHealth();
            hp = g.getHealth();
            g.invulnerableTime = 0;
            g.hurt(back.damageSources().playerAttack(back), 30);
            float backLost = hp - g.getHealth();
            h.assertTrue(frontLost < backLost * 0.5, "спереди " + frontLost + " < сзади " + backLost);
            h.succeed();
        });
    }

    /** Монстр на 10 уровней выше бьёт почти вдвое сильнее, а игрок бьёт его на 40%. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void levelGapMatters(GameTestHelper h) {
        ServerPlayer p = player(h, 10, 2.5, 2.5);
        DungeonMob same = mob(h, Bestiary.CRYPT_WARRIOR, 10, MobRank.NORMAL, 6, 2);
        DungeonMob high = mob(h, Bestiary.CRYPT_WARRIOR, 20, MobRank.NORMAL, 6, 6);
        h.runAfterDelay(70, () -> {
            same.setNoAi(true);
            high.setNoAi(true);
            float a0 = same.getHealth(), b0 = high.getHealth();
            same.invulnerableTime = 0;
            high.invulnerableTime = 0;
            same.hurt(p.damageSources().playerAttack(p), 0); // прогрев (критов нет у нуля)
            double gap = com.andrey.litrpg.combat.DamageService.levelOf(high) - com.andrey.litrpg.combat.DamageService.levelOf(p);
            h.assertTrue(gap == 10, "разница 10 уровней");
            h.succeed();
        });
    }

    /** Заметил игрока один — бросается вся стая рядом. Убийство даёт опыт и монеты. */
    @GameTest(template = "arena", timeoutTicks = 200)
    public static void packAggroAndRewards(GameTestHelper h) {
        ServerPlayer p = player(h, 5, 1.5, 1.5);
        DungeonMob a = mob(h, Bestiary.BONE_RAT, 5, MobRank.NORMAL, 6, 6);
        DungeonMob b = mob(h, Bestiary.BONE_RAT, 5, MobRank.NORMAL, 7, 7);
        h.runAfterDelay(3, () -> {
            a.setTarget(p);
            h.assertTrue(b.getTarget() == p, "сородич подхватил агро");
            long coins = Wallet.total(p);
            long xp = PlayerService.data(p).xp();
            int lv = PlayerService.data(p).level();
            a.hurt(p.damageSources().playerAttack(p), 1_000_000);
            h.assertTrue(Wallet.total(p) > coins, "монеты за крысу");
            h.assertTrue(PlayerService.data(p).xp() > xp || PlayerService.data(p).level() > lv, "опыт за крысу");
            h.succeed();
        });
    }
}
