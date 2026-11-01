package com.andrey.litrpg.skill;

import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Role;

import java.util.List;
import java.util.Map;

/**
 * Класс: своя идея, свой набор из 5–6 навыков, открывающихся с 10 по 48 уровень, рост характеристик
 * и «стиль игры» — по нему Система предлагает класс.
 *
 * @param idea   идея в одну строку (её видно в окне выбора класса)
 * @param lore   пара предложений о классе
 * @param style  веса стилей игры: чем ближе стиль игрока — тем выше класс в предложении
 */
public record PlayerClass(String id, Role role, String name, int color, String icon, String idea, String lore,
                          Resource resource, StatSet growth, List<Unlock> skills, Map<Style, Double> style) {

    /** Навык и уровень, с которого он открывается. */
    public record Unlock(int level, Skill skill) {
    }

    /** Навыки, открытые на этом уровне, по порядку ячеек панели. */
    public List<Skill> unlocked(int level) {
        return skills.stream().filter(u -> u.level <= level).map(Unlock::skill).toList();
    }
}
