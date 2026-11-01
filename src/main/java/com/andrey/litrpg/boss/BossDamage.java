package com.andrey.litrpg.boss;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import com.andrey.litrpg.player.StatBreakdown;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Урон хранителей «по-рейдовому»: сила удара — доля «единицы», здоровья бойца того же уровня.
 * 0,3 — ощутимо, 0,6 — тяжело, 1+ — смертельно для не-танка. Танк получает меньше половины (роль + стойкость).
 */
public final class BossDamage {
    private BossDamage() {
    }

    /**
     * Мерило силы ударов — здоровье бойца этого уровня, который уже вложил очки так, как их вкладывают:
     * четверть в выносливость, остальное в урон. Считать от голого персонажа нельзя: очки дают много,
     * и тогда к пятидесятому уровню хранитель щекотал бы отряд.
     */
    public static double unit(int level) {
        int[] alloc = new int[Stat.ALL.length];
        int points = (int) Math.round(Balance.get(Balance.PROGRESSION).statPointsPerLevel() * (level - 1));
        alloc[Stat.VITALITY.ordinal()] = points / 4;
        alloc[Stat.STRENGTH.ordinal()] = points - points / 4;
        StatBreakdown b = StatBreakdown.compute(new StatBreakdown.Inputs(Role.DAMAGE, level, alloc, StatSet.ZERO),
                Balance.get(Balance.PROGRESSION), Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES));
        return b.get(Derived.MAX_HEALTH);
    }

    /** Ударить игрока на {@code units} единиц (с неистовством). Конвейер урона добавит роль и стойкость. */
    /**
     * Ранние этапы мягче (отзыв группы 02.11.2026: первый хранитель ваншотил, монстров били по 15 минут).
     * 0 на 1-м уровне → 1 к 30-му: до него здоровье, урон и темп хранителей и монстров плавно растут до полных.
     */
    public static double early(int level) {
        return Math.max(0, Math.min(1, (level - 1) / 29.0));
    }

    /** Урон хранителя: ×0,45 в начале → ×1 к 30-му уровню. */
    public static double earlyDamage(int level) {
        return 0.45 + 0.55 * early(level);
    }

    /** Здоровье хранителя: ×0,2 в начале → ×1 к 30-му уровню. */
    public static double earlyHp(int level) {
        return 0.2 + 0.8 * early(level);
    }

    /** Откаты хранителя и «давки»: ×1,6 в начале → ×1. */
    public static double earlyCooldown(int level) {
        return 1.6 - 0.6 * early(level);
    }

    /** Замах (время на уход из разметки): ×1,4 в начале → ×1. */
    public static double earlyCast(int level) {
        return 1.4 - 0.4 * early(level);
    }

    public static boolean hit(BossEntity boss, LivingEntity target, double units) {
        if (target == null || !target.isAlive() || (target instanceof ServerPlayer p && p.isSpectator())) return false;
        double amount = units * unit(boss.bossLevel()) * boss.enrageMultiplier() * earlyDamage(boss.bossLevel());
        target.invulnerableTime = 0;
        return target.hurt(boss.damageSources().mobAttack(boss), (float) amount);
    }
}
