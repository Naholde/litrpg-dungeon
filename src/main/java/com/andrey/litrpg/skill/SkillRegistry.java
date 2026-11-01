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
    // Кровавый рыцарь
    public static final Skill BLOOD_STRIKE = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.BloodStrike());
    public static final Skill BLOOD_BRAND = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.Brand());
    public static final Skill BLOOD_CHAINS = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.Chains());
    public static final Skill BLOOD_FEAST = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.Feast());
    public static final Skill BLOOD_UNDYING = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.Undying());
    // Зерцало
    public static final Skill MIRROR_GLASS = reg(new com.andrey.litrpg.skill.classes.MirrorSkills.Glass());
    public static final Skill MIRROR_REPAY = reg(new com.andrey.litrpg.skill.classes.MirrorSkills.Repay());
    public static final Skill MIRROR_EDGE = reg(new com.andrey.litrpg.skill.classes.MirrorSkills.Edge());
    public static final Skill MIRROR_HALL = reg(new com.andrey.litrpg.skill.classes.MirrorSkills.Hall());
    public static final Skill MIRROR_DOUBLE = reg(new com.andrey.litrpg.skill.classes.MirrorSkills.Reflection());
    // Гравитон
    public static final Skill GRAV_FUNNEL = reg(new com.andrey.litrpg.skill.classes.GravitonSkills.Funnel());
    public static final Skill GRAV_PRESS = reg(new com.andrey.litrpg.skill.classes.GravitonSkills.Press());
    public static final Skill GRAV_TOSS = reg(new com.andrey.litrpg.skill.classes.GravitonSkills.Toss());
    public static final Skill GRAV_INVERSION = reg(new com.andrey.litrpg.skill.classes.GravitonSkills.Inversion());
    public static final Skill GRAV_BLACKHOLE = reg(new com.andrey.litrpg.skill.classes.GravitonSkills.BlackHole());
    // Ледяной бастион
    public static final Skill BASTION_ARMOR = reg(new com.andrey.litrpg.skill.classes.BastionSkills.IceArmor());
    public static final Skill BASTION_FROST = reg(new com.andrey.litrpg.skill.classes.BastionSkills.Frost());
    public static final Skill BASTION_WALL = reg(new com.andrey.litrpg.skill.classes.BastionSkills.IceWall());
    public static final Skill BASTION_PERMAFROST = reg(new com.andrey.litrpg.skill.classes.BastionSkills.Permafrost());
    public static final Skill BASTION_TOMB = reg(new com.andrey.litrpg.skill.classes.BastionSkills.Tomb());
    // Паладин клятвы
    public static final Skill PAL_OATH = reg(new com.andrey.litrpg.skill.classes.PaladinSkills.Oath());
    public static final Skill PAL_STRIKE = reg(new com.andrey.litrpg.skill.classes.PaladinSkills.HolyStrike());
    public static final Skill PAL_DIVINE = reg(new com.andrey.litrpg.skill.classes.PaladinSkills.DivineShield());
    public static final Skill PAL_HANDS = reg(new com.andrey.litrpg.skill.classes.PaladinSkills.LayOnHands());
    public static final Skill PAL_JUDGMENT = reg(new com.andrey.litrpg.skill.classes.PaladinSkills.Judgment());
    // Хронолог
    public static final Skill CHRONO_REWIND = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Rewind());
    public static final Skill CHRONO_STASIS = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Stasis());
    public static final Skill CHRONO_HASTE = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Haste());
    public static final Skill CHRONO_LOOP = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Loop());
    public static final Skill CHRONO_HOURGLASS = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Hourglass());
    public static final Skill CHRONO_REPLAY = reg(new com.andrey.litrpg.skill.classes.ChronologSkills.Replay());
    // Теневой жрец
    public static final Skill PRIEST_LASH = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Lash());
    public static final Skill PRIEST_ATONEMENT = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Atonement());
    public static final Skill PRIEST_TWILIGHT = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Twilight());
    public static final Skill PRIEST_BARGAIN = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Bargain());
    public static final Skill PRIEST_VEIL = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Veil());
    public static final Skill PRIEST_DEVOUR = reg(new com.andrey.litrpg.skill.classes.ShadowPriestSkills.Devour());
    // Друид
    public static final Skill DRUID_FLOWER = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Flower());
    public static final Skill DRUID_VINE = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Vine());
    public static final Skill DRUID_THORNS = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Thorns());
    public static final Skill DRUID_TREE = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Tree());
    public static final Skill DRUID_BLOOM = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Bloom());
    public static final Skill DRUID_SPRING = reg(new com.andrey.litrpg.skill.classes.DruidSkills.Spring());
    // Шаман
    public static final Skill SHAMAN_TOTEM_HEAL = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.TotemHeal());
    public static final Skill SHAMAN_LIGHTNING = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.Lightning());
    public static final Skill SHAMAN_TOTEM_STORM = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.TotemStorm());
    public static final Skill SHAMAN_TOTEM_EARTH = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.TotemEarth());
    public static final Skill SHAMAN_ANCESTORS = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.Ancestors());
    public static final Skill SHAMAN_CONSUME = reg(new com.andrey.litrpg.skill.classes.ShamanSkills.Consume());
    // Знаменосец
    public static final Skill BANNER_POWER = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.BannerPower());
    public static final Skill BANNER_GUARD = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.BannerGuard());
    public static final Skill BANNER_SHOUT = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.Shout());
    public static final Skill BANNER_SPEED = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.BannerSpeed());
    public static final Skill BANNER_RANKS = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.CloseRanks());
    public static final Skill BANNER_LAST_STAND = reg(new com.andrey.litrpg.skill.classes.BannermanSkills.LastStand());
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
