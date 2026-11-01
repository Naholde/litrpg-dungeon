package com.andrey.litrpg.skill;

import com.andrey.litrpg.skill.classes.BerserkerSkills;
import com.andrey.litrpg.skill.classes.ClericSkills;
import com.andrey.litrpg.skill.classes.GuardianSkills;
import com.andrey.litrpg.skill.role.DamageSkills;
import com.andrey.litrpg.skill.role.SupportSkills;
import com.andrey.litrpg.skill.role.TankSkills;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Все навыки игры. Один экземпляр на навык: роль и классы ссылаются на одни и те же объекты (ранги общие). */
public final class SkillRegistry {
    private static final Map<String, Skill> ALL = new LinkedHashMap<>();

    // Роль «Танк»
    public static final Skill TAUNT = reg(new TankSkills.Taunt());
    public static final Skill SHIELD_BASH = reg(new TankSkills.ShieldBash());
    public static final Skill BULWARK = reg(new TankSkills.Bulwark());
    // Роль «Боец»
    public static final Skill HEAVY_STRIKE = reg(new DamageSkills.HeavyStrike());
    public static final Skill WHIRLWIND = reg(new DamageSkills.Whirlwind());
    public static final Skill THROWING_KNIFE = reg(new DamageSkills.ThrowingKnife());
    // Роль «Поддержка»
    public static final Skill MEND = reg(new SupportSkills.Mend());
    public static final Skill RENEW = reg(new SupportSkills.Renew());
    public static final Skill SMITE = reg(new SupportSkills.Smite());
    // Стражник
    public static final Skill COVER = reg(new GuardianSkills.Cover());
    public static final Skill CHARGE = reg(new GuardianSkills.Charge());
    public static final Skill BRACE = reg(new GuardianSkills.Brace());
    public static final Skill DOME = reg(new GuardianSkills.Dome());
    public static final Skill UNBREAKABLE = reg(new GuardianSkills.Unbreakable());
    // Берсерк
    public static final Skill CLEAVE = reg(new BerserkerSkills.Cleave());
    public static final Skill REND = reg(new BerserkerSkills.Rend());
    public static final Skill LEAP = reg(new BerserkerSkills.Leap());
    public static final Skill FRENZY = reg(new BerserkerSkills.Frenzy());
    public static final Skill EXECUTE = reg(new BerserkerSkills.Execute());
    public static final Skill BLOODBATH = reg(new BerserkerSkills.Bloodbath());
    // Лекарь
    public static final Skill BANDAGE = reg(new ClericSkills.Bandage());
    public static final Skill PRAYER = reg(new ClericSkills.Prayer());
    public static final Skill SANCTUARY = reg(new ClericSkills.Sanctuary());
    public static final Skill MIRACLE = reg(new ClericSkills.Miracle());

    private SkillRegistry() {
    }

    private static Skill reg(Skill s) {
        if (ALL.putIfAbsent(s.id, s) != null) throw new IllegalStateException("навык уже есть: " + s.id);
        return s;
    }

    public static Skill get(String id) {
        return ALL.get(id);
    }

    public static Collection<Skill> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    /** Значения по умолчанию всех навыков — так выглядит раздел {@code skills} в skills.json. */
    public static Map<String, Map<String, Double>> defaultValues() {
        Map<String, Map<String, Double>> out = new TreeMap<>();
        for (Skill s : ALL.values()) {
            Map<String, Double> m = new LinkedHashMap<>();
            s.params().values().forEach(p -> m.put(p.key(), p.def()));
            out.put(s.id, m);
        }
        return out;
    }
}
