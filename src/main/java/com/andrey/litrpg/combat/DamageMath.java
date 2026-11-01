package com.andrey.litrpg.combat;

import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.StatBreakdown;

/** Формулы урона без Minecraft. Используются конвейером урона и тестами. */
public final class DamageMath {
    private DamageMath() {
    }

    /** Прибавка к урону от характеристик для этого рода оружия. */
    public static double power(WeaponKind kind, StatBreakdown b) {
        return switch (kind) {
            case MELEE -> b.get(Derived.WEAPON_POWER);
            case RANGED -> b.get(Derived.RANGED_POWER);
            case MAGIC, SKILL -> b.get(Derived.SKILL_POWER);
            case PLAIN, FIXED -> 0;
        };
    }

    /** Исходящий урон игрока без крита: основа × (1 + прибавка) × множитель роли. */
    public static double outgoing(double base, WeaponKind kind, StatBreakdown b) {
        return base * (1 + power(kind, b)) * b.get(Derived.DAMAGE_MULT);
    }

    /** Входящий урон по игроку: стойкость режет любой урон, хранители — ещё и множителем роли. */
    public static double incoming(double amount, StatBreakdown b, boolean fromBoss) {
        double v = amount * (1 - b.get(Derived.TOUGHNESS));
        return fromBoss ? v * b.get(Derived.BOSS_TAKEN) : v;
    }

    /** Урон окружения (падение, лава, утопление) в масштабе здоровья: как в обычном Minecraft с 20 здоровья. */
    public static double environment(double amount, double maxHealth) {
        return amount * maxHealth / 20.0;
    }
}
