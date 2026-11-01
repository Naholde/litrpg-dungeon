package com.andrey.litrpg.mob;

/** Ранг монстра: обычный, элитный (★), чемпион (✦). Множители здоровья, урона и награды. */
public enum MobRank {
    NORMAL("", 1.0, 1.0, 1.0),
    ELITE("★", 2.2, 1.35, 3.0),
    CHAMPION("✦", 4.0, 1.7, 6.0);

    public final String mark;
    public final double health;
    public final double damage;
    public final double reward;

    MobRank(String mark, double health, double damage, double reward) {
        this.mark = mark;
        this.health = health;
        this.damage = damage;
        this.reward = reward;
    }
}
