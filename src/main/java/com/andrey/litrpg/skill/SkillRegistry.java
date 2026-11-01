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
    public static final Skill BLOOD_THREADS = reg(new com.andrey.litrpg.skill.classes.BloodKnightSkills.Threads());
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
    // Стриж
    public static final Skill SWIFT_RUSH = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.Rush());
    public static final Skill SWIFT_FAN = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.Fan());
    public static final Skill SWIFT_PIROUETTE = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.Pirouette());
    public static final Skill SWIFT_DANCE = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.Dance());
    public static final Skill SWIFT_DRAFT = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.Draft());
    public static final Skill SWIFT_LAST_SWING = reg(new com.andrey.litrpg.skill.classes.SwiftSkills.LastSwing());
    // Монах
    public static final Skill MONK_SEAL_STRIKE = reg(new com.andrey.litrpg.skill.classes.MonkSkills.SealStrike());
    public static final Skill MONK_SEAL_BIND = reg(new com.andrey.litrpg.skill.classes.MonkSkills.SealBind());
    public static final Skill MONK_CIRCLE = reg(new com.andrey.litrpg.skill.classes.MonkSkills.CircleClosed());
    public static final Skill MONK_PALM = reg(new com.andrey.litrpg.skill.classes.MonkSkills.PalmOfChi());
    public static final Skill MONK_HUNDRED = reg(new com.andrey.litrpg.skill.classes.MonkSkills.HundredPalms());
    public static final Skill MONK_MOUNTAIN = reg(new com.andrey.litrpg.skill.classes.MonkSkills.MountainFalls());
    // Тень
    public static final Skill SHADOW_FROM_DARK = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.FromDark());
    public static final Skill SHADOW_KNIVES = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.KnifeFan());
    public static final Skill SHADOW_CLOAK = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.Cloak());
    public static final Skill SHADOW_NIGHT = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.Night());
    public static final Skill SHADOW_POISON = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.BladePoison());
    public static final Skill SHADOW_CUT = reg(new com.andrey.litrpg.skill.classes.ShadowSkills.Cut());
    // Жнец
    public static final Skill REAPER_SCYTHE = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.Scythe());
    public static final Skill REAPER_SICKLE = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.ChainSickle());
    public static final Skill REAPER_MARK = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.DeathMark());
    public static final Skill REAPER_HARVEST = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.Harvest());
    public static final Skill REAPER_FERRYMAN = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.Ferryman());
    public static final Skill REAPER_SENTENCE = reg(new com.andrey.litrpg.skill.classes.ReaperSkills.Sentence());
    // Гарпунёр
    public static final Skill HARPOON_THROW = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.Harpoon());
    public static final Skill HARPOON_DOUBLE = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.DoubleChain());
    public static final Skill HARPOON_WINDUP = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.Windup());
    public static final Skill HARPOON_SHACKLES = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.Shackles());
    public static final Skill HARPOON_PULL_SELF = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.ChainPull());
    public static final Skill HARPOON_ABYSS = reg(new com.andrey.litrpg.skill.classes.HarpoonerSkills.AbyssPull());
    // Палач
    public static final Skill HEADSMAN_SWING = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.HeavySwing());
    public static final Skill HEADSMAN_HOOK = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.Hook());
    public static final Skill HEADSMAN_WHEEL = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.Wheel());
    public static final Skill HEADSMAN_VERDICT = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.Verdict());
    public static final Skill HEADSMAN_BLOCK = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.Block());
    public static final Skill HEADSMAN_SCAFFOLD = reg(new com.andrey.litrpg.skill.classes.HeadsmanSkills.Scaffold());
    // Снайпер
    public static final Skill SNIPER_AIM = reg(new com.andrey.litrpg.skill.classes.SniperSkills.Aim());
    public static final Skill SNIPER_BREATH = reg(new com.andrey.litrpg.skill.classes.SniperSkills.HoldBreath());
    public static final Skill SNIPER_MARK = reg(new com.andrey.litrpg.skill.classes.SniperSkills.Mark());
    public static final Skill SNIPER_TRAP = reg(new com.andrey.litrpg.skill.classes.SniperSkills.Trap());
    public static final Skill SNIPER_PIERCE = reg(new com.andrey.litrpg.skill.classes.SniperSkills.Pierce());
    public static final Skill SNIPER_FINISHER = reg(new com.andrey.litrpg.skill.classes.SniperSkills.Finisher());
    // Механик
    public static final Skill MECH_TURRET = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.Turret());
    public static final Skill MECH_SHOTGUN = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.Shotgun());
    public static final Skill MECH_MINE = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.Mine());
    public static final Skill MECH_MORTAR = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.Mortar());
    public static final Skill MECH_OVERDRIVE = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.Overdrive());
    public static final Skill MECH_ARSENAL = reg(new com.andrey.litrpg.skill.classes.MechanicSkills.FullArsenal());
    // Пиромант
    public static final Skill PYRO_SPARK = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.Spark());
    public static final Skill PYRO_GROUND = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.BurningGround());
    public static final Skill PYRO_DETONATE = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.Detonate());
    public static final Skill PYRO_BREATH = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.FireBreath());
    public static final Skill PYRO_FIRESTORM = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.Firestorm());
    public static final Skill PYRO_INFERNO = reg(new com.andrey.litrpg.skill.classes.PyromancerSkills.Inferno());
    // Криомант
    public static final Skill CRYO_NEEDLE = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.IceNeedle());
    public static final Skill CRYO_BLIZZARD = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.Blizzard());
    public static final Skill CRYO_FREEZE = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.Freeze());
    public static final Skill CRYO_SHATTER = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.Shatter());
    public static final Skill CRYO_SPIKES = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.IceSpikes());
    public static final Skill CRYO_ZERO = reg(new com.andrey.litrpg.skill.classes.CryomancerSkills.AbsoluteZero());
    // Громовержец
    public static final Skill STORM_CHAIN = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.ChainLightning());
    public static final Skill STORM_JOLT = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.Jolt());
    public static final Skill STORM_STATIC = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.Static());
    public static final Skill STORM_CLOUD = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.StormCloud());
    public static final Skill STORM_STEP = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.ThunderStep());
    public static final Skill STORM_TEMPEST = reg(new com.andrey.litrpg.skill.classes.StormcallerSkills.Tempest());
    // Чумной владыка
    public static final Skill PLAGUE_PLAGUE = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.Plague());
    public static final Skill PLAGUE_SPREAD = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.Spread());
    public static final Skill PLAGUE_WITHER = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.Wither());
    public static final Skill PLAGUE_SWARM = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.FlySwarm());
    public static final Skill PLAGUE_TURN = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.Turn());
    public static final Skill PLAGUE_PANDEMIC = reg(new com.andrey.litrpg.skill.classes.PlagueSkills.Pandemic());
    // Астромант
    public static final Skill ASTRO_FALLING = reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.FallingStar());
    public static final Skill ASTRO_LUMINARY = reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.Luminary());
    public static final Skill ASTRO_BEAM = reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.StarBeam());
    public static final Skill ASTRO_CONSTELLATION =
            reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.Constellation());
    public static final Skill ASTRO_PULL = reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.StarPull());
    public static final Skill ASTRO_SUPERNOVA = reg(new com.andrey.litrpg.skill.classes.AstromancerSkills.Supernova());
    // Призыватель
    public static final Skill SUMMON_SKELETON = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.RaiseSkeleton());
    public static final Skill SUMMON_WOLVES = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.ShadowWolves());
    public static final Skill SUMMON_ORDER = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.Order());
    public static final Skill SUMMON_GOLEM = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.BoneGolem());
    public static final Skill SUMMON_BURST = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.SoulBurst());
    public static final Skill SUMMON_LEGION = reg(new com.andrey.litrpg.skill.classes.SummonerSkills.Legion());
    // Иллюзионист
    public static final Skill ILLUSION_DOUBLE = reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.Phantom());
    public static final Skill ILLUSION_SWAP = reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.Swap());
    public static final Skill ILLUSION_CHARM = reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.Charm());
    public static final Skill ILLUSION_KALEIDOSCOPE =
            reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.Kaleidoscope());
    public static final Skill ILLUSION_STRIKE =
            reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.MirroredStrike());
    public static final Skill ILLUSION_VANISH = reg(new com.andrey.litrpg.skill.classes.IllusionistSkills.Vanish());
    // Чернокнижник
    public static final Skill WARLOCK_CURSE = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.CurseOfWeakness());
    public static final Skill WARLOCK_CORRUPTION = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.Corruption());
    public static final Skill WARLOCK_PACT = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.Pact());
    public static final Skill WARLOCK_CIRCLE = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.BloodCircle());
    public static final Skill WARLOCK_TENTACLES = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.Tentacles());
    public static final Skill WARLOCK_SACRIFICE = reg(new com.andrey.litrpg.skill.classes.WarlockSkills.Sacrifice());
    // Разломщик
    public static final Skill RIFT_RIFT = reg(new com.andrey.litrpg.skill.classes.RiftSkills.Rift());
    public static final Skill RIFT_STEP = reg(new com.andrey.litrpg.skill.classes.RiftSkills.VoidStep());
    public static final Skill RIFT_MARK = reg(new com.andrey.litrpg.skill.classes.RiftSkills.VoidMark());
    public static final Skill RIFT_COLLAPSE = reg(new com.andrey.litrpg.skill.classes.RiftSkills.Collapse());
    public static final Skill RIFT_MAW = reg(new com.andrey.litrpg.skill.classes.RiftSkills.Maw());
    public static final Skill RIFT_INSIDE = reg(new com.andrey.litrpg.skill.classes.RiftSkills.Inside());
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
