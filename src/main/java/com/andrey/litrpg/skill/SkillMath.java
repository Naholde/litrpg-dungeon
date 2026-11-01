package com.andrey.litrpg.skill;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.SkillsBalance;

/** Формулы навыков без Minecraft. */
public final class SkillMath {
    private SkillMath() {
    }

    /** «Единица силы» навыка на уровне: при доле 1,0 навык наносит/лечит столько (до усилений). */
    public static double unit(int level) {
        SkillsBalance.Common c = Balance.get(Balance.SKILLS).common();
        return c.powerBase() + c.powerPerLevel() * Math.max(0, level - 1);
    }

    /** Перезарядка с ускорением (доля, срезанная Интеллектом). */
    public static int cooldownTicks(double seconds, double haste) {
        return (int) Math.round(seconds * 20 * (1 - Math.max(0, Math.min(0.9, haste))));
    }
}
