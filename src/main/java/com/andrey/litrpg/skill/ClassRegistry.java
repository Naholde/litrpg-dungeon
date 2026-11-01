package com.andrey.litrpg.skill;

import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Role;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.andrey.litrpg.skill.SkillRegistry.*;

/**
 * Все классы. Полный задуманный список (~10 на роль) — в {@code docs/CLASSES.md}; здесь — уже сделанные.
 * Навыки открываются: три — на уровне получения класса, остальные — по уровням до 48.
 */
public final class ClassRegistry {
    private static final Map<String, PlayerClass> ALL = new LinkedHashMap<>();

    public static final PlayerClass GUARDIAN = reg(new PlayerClass("guardian", Role.TANK, "Стражник", 0xFF7FB8FF, "⛨",
            "Держит место: забирает урон союзников на себя и гасит вражеские снаряды куполом.",
            "Стражники не гонятся за славой. Их дело — стоять там, где другим не выстоять, и чтобы за спиной было тихо.",
            Resource.NONE, new StatSet(1.2, 0.5, 2.2, 0.5, 0.5),
            List.of(u(10, TAUNT), u(10, COVER), u(10, CHARGE), u(20, BRACE), u(32, DOME), u(44, UNBREAKABLE)),
            Map.of(Style.TANKING, 1.0, Style.PROTECTION, 0.8, Style.MELEE, 0.6)));

    public static final PlayerClass BLOOD_KNIGHT = reg(new PlayerClass("blood_knight", Role.TANK, "Кровавый рыцарь",
            0xFF8B1A1A, "†",
            "Платит кровью и пьёт её обратно: удары стоят здоровья, но лечат, а клеймо поит рыцаря с каждого удара по цели.",
            "Кровавый рыцарь не держит удар — он его покупает. Своей кровью, за чужую, и всегда с выгодой для отряда.",
            Resource.BLOOD, new StatSet(1.6, 0.5, 2.0, 0.5, 0.4),
            List.of(u(10, TAUNT), u(10, BLOOD_STRIKE), u(10, BLOOD_BRAND), u(20, BLOOD_CHAINS), u(32, BLOOD_FEAST),
                    u(44, BLOOD_THREADS)),
            Map.of(Style.TANKING, 1.0, Style.MELEE, 0.9, Style.HEALING, 0.2)));

    public static final PlayerClass MIRROR = reg(new PlayerClass("mirror", Role.TANK, "Зерцало", 0xFFB0E8FF, "◫",
            "Живёт чужими ударами: копит полученный урон в стекло и возвращает его сполна, а снаряды разворачивает обратно.",
            "Зерцало не спорит с ударом. Оно его запоминает — и однажды возвращает тому, кто начал.",
            Resource.GLASS, new StatSet(1.0, 0.8, 2.2, 0.8, 0.2),
            List.of(u(10, TAUNT), u(10, MIRROR_GLASS), u(10, MIRROR_REPAY), u(20, MIRROR_EDGE), u(32, MIRROR_HALL),
                    u(44, MIRROR_DOUBLE)),
            Map.of(Style.TANKING, 1.0, Style.PROTECTION, 0.6, Style.SKILLS, 0.5)));

    public static final PlayerClass GRAVITON = reg(new PlayerClass("graviton", Role.TANK, "Гравитон", 0xFF7A6BD8, "◉",
            "Собирает толпу весом: воронка стягивает врагов, пресс придавливает, а чёрная дыра держит их всех разом.",
            "Гравитон не бегает за врагами. Он делает так, чтобы они оказались там, где нужно ему.",
            Resource.NONE, new StatSet(1.0, 0.6, 2.1, 1.1, 0.2),
            List.of(u(10, TAUNT), u(10, GRAV_FUNNEL), u(10, GRAV_PRESS), u(20, GRAV_TOSS), u(32, GRAV_INVERSION),
                    u(44, GRAV_BLACKHOLE)),
            Map.of(Style.TANKING, 1.0, Style.SKILLS, 0.8, Style.PROTECTION, 0.5)));

    public static final PlayerClass BASTION = reg(new PlayerClass("bastion", Role.TANK, "Ледяной бастион",
            0xFF8FE3FF, "❅",
            "Меняет площадку боя: обморожение сковывает врагов, стена из настоящего льда перекрывает проход, мерзлота отнимает силу удара.",
            "Там, где встал бастион, бой идёт по его правилам: медленно, холодно и ровно столько, сколько он позволит.",
            Resource.NONE, new StatSet(1.1, 0.4, 2.3, 0.9, 0.3),
            List.of(u(10, TAUNT), u(10, BASTION_ARMOR), u(10, BASTION_FROST), u(20, BASTION_WALL),
                    u(32, BASTION_PERMAFROST), u(44, BASTION_TOMB)),
            Map.of(Style.TANKING, 1.0, Style.PROTECTION, 0.7, Style.SKILLS, 0.4)));

    public static final PlayerClass PALADIN = reg(new PlayerClass("paladin", Role.TANK, "Паладин клятвы",
            0xFFFFE27A, "✥",
            "Святой свет: клятва-аура на отряд, три секунды полной неуязвимости и лечение союзника ценой своего здоровья.",
            "Паладин держит не место, а слово. Пока клятва звучит, за его спиной никто не падает.",
            Resource.NONE, new StatSet(1.2, 0.5, 2.0, 1.2, 0.3),
            List.of(u(10, TAUNT), u(10, PAL_OATH), u(10, PAL_STRIKE), u(20, PAL_DIVINE), u(32, PAL_HANDS),
                    u(44, PAL_JUDGMENT)),
            Map.of(Style.TANKING, 1.0, Style.PROTECTION, 0.9, Style.HEALING, 0.6)));

    public static final PlayerClass BERSERKER = reg(new PlayerClass("berserker", Role.DAMAGE, "Берсерк", 0xFFD8343A, "⚔",
            "Чем больнее, тем злее: копит ярость от ударов и сжигает её в сокрушительные приёмы.",
            "Берсерк не считает раны — он считает врагов. Каждый пропущенный удар только подливает масла в огонь.",
            Resource.RAGE, new StatSet(1.8, 1.0, 1.0, 0.5, 0.6),
            List.of(u(10, CLEAVE), u(10, REND), u(10, LEAP), u(20, FRENZY), u(32, EXECUTE), u(44, BLOODBATH)),
            Map.of(Style.MELEE, 1.0, Style.SKILLS, 0.5, Style.TANKING, 0.4)));

    public static final PlayerClass CLERIC = reg(new PlayerClass("cleric", Role.SUPPORT, "Лекарь", 0xFFFFE27A, "✚",
            "Эталон лекаря: быстрая повязка по раненым, долгая молитва, святилище и «Чудо» для всей группы.",
            "Лекарь знает цену каждой секунде: одна повязка вовремя стоит десятка молитв запоздавших.",
            Resource.NONE, new StatSet(0.4, 0.7, 1.1, 2.2, 0.7),
            List.of(u(10, BANDAGE), u(10, PRAYER), u(10, RENEW), u(20, SANCTUARY), u(32, SMITE), u(44, MIRACLE)),
            Map.of(Style.HEALING, 1.0, Style.MAGIC, 0.5, Style.SKILLS, 0.3)));

    public static final PlayerClass SWIFT = reg(new PlayerClass("swift", Role.DAMAGE, "Стриж", 0xFF7FF0D0, "➤",
            "Дуэлянт в движении: разгон копится от его же приёмов и быстро тает, если стоять — на разгоне он бьёт вдвое больнее.",
            "Стриж не стоит в строю. Он всё время в движении, и чем быстрее движется, тем меньше шансов, что по нему попадут.",
            Resource.MOMENTUM, new StatSet(1.0, 2.0, 0.8, 0.6, 0.8),
            List.of(u(10, SWIFT_RUSH), u(10, SWIFT_FAN), u(10, SWIFT_PIROUETTE), u(20, SWIFT_DANCE),
                    u(32, SWIFT_DRAFT), u(44, SWIFT_LAST_SWING)),
            Map.of(Style.MELEE, 1.0, Style.SKILLS, 0.7)));

    public static final PlayerClass MONK = reg(new PlayerClass("monk", Role.DAMAGE, "Монах", 0xFFFFD27A, "✶",
            "Ставит печати на землю: две стягивают врагов между собой, три складываются в «Круг сомкнулся». Ци копится от его ударов.",
            "Монах не спешит. Он расставляет печати, и бой идёт туда, куда он нарисовал.",
            Resource.CHI, new StatSet(1.3, 1.3, 1.0, 0.9, 0.5),
            List.of(u(10, MONK_SEAL_STRIKE), u(10, MONK_SEAL_BIND), u(10, MONK_CIRCLE), u(20, MONK_PALM),
                    u(32, MONK_HUNDRED), u(44, MONK_MOUNTAIN)),
            Map.of(Style.MELEE, 0.9, Style.SKILLS, 1.0)));

    public static final PlayerClass SHADOW = reg(new PlayerClass("shadow", Role.DAMAGE, "Тень", 0xFF9B5CFF, "◖",
            "Со спины и из темноты: шаг за спину, невидимость, отравленный клинок и добивание, которое обнуляет свою перезарядку.",
            "Тень не дерётся — она заканчивает. Её замечают, когда уже поздно.",
            Resource.SHADE, new StatSet(1.1, 1.9, 0.7, 0.6, 0.9),
            List.of(u(10, SHADOW_FROM_DARK), u(10, SHADOW_KNIVES), u(10, SHADOW_CLOAK), u(20, SHADOW_NIGHT),
                    u(32, SHADOW_POISON), u(44, SHADOW_CUT)),
            Map.of(Style.MELEE, 1.0, Style.SKILLS, 0.8)));

    public static final PlayerClass REAPER = reg(new PlayerClass("reaper", Role.DAMAGE, "Жнец", 0xFF6B7A8B, "⚒",
            "Считает чужие смерти: души за убитых и за отмеченных уходят в жатву, а приговор повторяет врагу всё, что тот получил.",
            "Жнец не торопит смерть. Он просто помечает тех, кто уже её не минует, и приходит за своей долей.",
            Resource.SOULS, new StatSet(1.6, 1.0, 0.9, 0.8, 0.6),
            List.of(u(10, REAPER_SCYTHE), u(10, REAPER_SICKLE), u(10, REAPER_MARK), u(20, REAPER_HARVEST),
                    u(32, REAPER_FERRYMAN), u(44, REAPER_SENTENCE)),
            Map.of(Style.MELEE, 1.0, Style.SKILLS, 0.75)));

    public static final PlayerClass HARPOONER = reg(new PlayerClass("harpooner", Role.DAMAGE, "Гарпунёр",
            0xFF8B8B8B, "⚓",
            "Решает, кто где стоит: гарпун тянет врага к себе, двойная цепь связывает двух врагов, кандалы не дают уйти.",
            "Гарпунёру всё равно, куда враг собирался. У него есть цепь, и он уже решил.",
            Resource.NONE, new StatSet(1.5, 1.1, 1.1, 0.6, 0.5),
            List.of(u(10, HARPOON_THROW), u(10, HARPOON_DOUBLE), u(10, HARPOON_WINDUP), u(20, HARPOON_SHACKLES),
                    u(32, HARPOON_PULL_SELF), u(44, HARPOON_ABYSS)),
            Map.of(Style.MELEE, 0.9, Style.SKILLS, 0.8, Style.TANKING, 0.3)));

    public static final PlayerClass HEADSMAN = reg(new PlayerClass("headsman", Role.DAMAGE, "Палач", 0xFF8B2A2A, "☠",
            "Медленно и окончательно: замах нельзя сбить ничем, плаха удваивает его, а удачная казнь сразу снимает перезарядку.",
            "Палач не отбивается и не уклоняется. Он замахивается — и всё, что может сделать враг, это успеть отойти.",
            Resource.NONE, new StatSet(2.0, 0.6, 1.1, 0.4, 0.6),
            List.of(u(10, HEADSMAN_SWING), u(10, HEADSMAN_HOOK), u(10, HEADSMAN_WHEEL), u(20, HEADSMAN_VERDICT),
                    u(32, HEADSMAN_BLOCK), u(44, HEADSMAN_SCAFFOLD)),
            Map.of(Style.MELEE, 1.0, Style.TANKING, 0.6, Style.SKILLS, 0.4)));

    public static final PlayerClass SNIPER = reg(new PlayerClass("sniper", Role.DAMAGE, "Снайпер", 0xFFC9D6E8, "◎",
            "Боец одного выстрела: дыхание копится, пока он стоит, и чем дольше стоял, тем больнее бьёт. Метка делает цель удобной всей группе.",
            "Снайпер не бежит в бой. Он находит место, замирает — и ждёт того единственного выстрела, который всё решит.",
            Resource.BREATH, new StatSet(0.8, 2.1, 0.7, 0.9, 0.9),
            List.of(u(10, SNIPER_AIM), u(10, SNIPER_BREATH), u(10, SNIPER_MARK), u(20, SNIPER_TRAP),
                    u(32, SNIPER_PIERCE), u(44, SNIPER_FINISHER)),
            Map.of(Style.RANGED, 1.0, Style.SKILLS, 0.5)));

    public static final PlayerClass MECHANIC = reg(new PlayerClass("mechanic", Role.DAMAGE, "Механик", 0xFFFF7A2E, "⌂",
            "Воюет станками: турель, мина и мортира стоят в мире, у них есть здоровье, и враги могут их сломать. Заряд копится от попаданий.",
            "Механик не считает себя бойцом. Он просто ставит станки там, где врагу будет неудобно, и смотрит, как они работают.",
            Resource.CHARGE, new StatSet(0.7, 1.6, 0.9, 1.4, 0.8),
            List.of(u(10, MECH_TURRET), u(10, MECH_SHOTGUN), u(10, MECH_MINE), u(20, MECH_MORTAR),
                    u(32, MECH_OVERDRIVE), u(44, MECH_ARSENAL)),
            Map.of(Style.RANGED, 0.9, Style.SKILLS, 1.0)));

    public static final PlayerClass PYROMANCER = reg(new PlayerClass("pyromancer", Role.DAMAGE, "Пиромант",
            0xFFFF4A2E, "♨",
            "Огонь слоями: каждый приём кладёт слой жара, «Взрыв» детонирует их все, а на полном жаре пиромант горит сам — зато бьёт сильнее.",
            "Пиромант не тушит за собой. Он кладёт слой, потом ещё слой — и в нужный миг отпускает всё сразу.",
            Resource.HEAT, new StatSet(0.4, 0.9, 0.8, 2.3, 0.6),
            List.of(u(10, PYRO_SPARK), u(10, PYRO_GROUND), u(10, PYRO_DETONATE), u(20, PYRO_BREATH),
                    u(32, PYRO_FIRESTORM), u(44, PYRO_INFERNO)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.8)));

    public static final PlayerClass CRYOMANCER = reg(new PlayerClass("cryomancer", Role.DAMAGE, "Криомант",
            0xFF8FE3FF, "❄",
            "Холод до хруста: обморожение слоями, на пятом враг скован льдом, а «Раскол» по скованному бьёт втрое и разлетается осколками.",
            "Криоманту не нужно спешить. Он остужает — и в какой-то момент враг просто перестаёт двигаться.",
            Resource.NONE, new StatSet(0.4, 0.8, 0.9, 2.3, 0.6),
            List.of(u(10, CRYO_NEEDLE), u(10, CRYO_BLIZZARD), u(10, CRYO_FREEZE), u(20, CRYO_SHATTER),
                    u(32, CRYO_SPIKES), u(44, CRYO_ZERO)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.7)));

    public static final PlayerClass STORMCALLER = reg(new PlayerClass("stormcaller", Role.DAMAGE, "Громовержец",
            0xFF8FA6FF, "↯",
            "Цепи, статика и туча над головой: статика копится слоями и на пятом разряжается по всем рядом, а буря превращает площадку в грозу.",
            "Громовержец бьёт не туда, куда смотрит, а туда, где стоят плотнее. Молния сама найдёт следующего.",
            Resource.NONE, new StatSet(0.4, 1.0, 0.8, 2.3, 0.5),
            List.of(u(10, STORM_CHAIN), u(10, STORM_JOLT), u(10, STORM_STATIC), u(20, STORM_CLOUD),
                    u(32, STORM_STEP), u(44, STORM_TEMPEST)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.9)));

    public static final PlayerClass PLAGUE_LORD = reg(new PlayerClass("plague_lord", Role.DAMAGE, "Чумной владыка",
            0xFF8FA83A, "☣",
            "Зараза работает за него: слои копятся, перескакивают на соседей, мешают лечить, а «Пандемия» взыскивает с каждого заражённого разом.",
            "Владыка никого не торопит. Он заражает одного — и ждёт, пока зараза сделает всё остальное.",
            Resource.NONE, new StatSet(0.4, 0.8, 1.0, 2.2, 0.6),
            List.of(u(10, PLAGUE_PLAGUE), u(10, PLAGUE_SPREAD), u(10, PLAGUE_WITHER), u(20, PLAGUE_SWARM),
                    u(32, PLAGUE_TURN), u(44, PLAGUE_PANDEMIC)),
            Map.of(Style.MAGIC, 0.9, Style.SKILLS, 1.0)));

    public static final PlayerClass ASTROMANCER = reg(new PlayerClass("astromancer", Role.DAMAGE, "Астромант",
            0xFF7FD8FF, "✧",
            "Воюет светилами: звёзды висят в мире, бьют сами, усиливают его луч, тянут врагов, а в конце гаснут сверхновыми.",
            "Астромант не торопится бить. Он расставляет светила — и когда рисунок сложился, небо падает всё сразу.",
            Resource.NONE, new StatSet(0.4, 0.9, 0.8, 2.3, 0.7),
            List.of(u(10, ASTRO_FALLING), u(10, ASTRO_LUMINARY), u(10, ASTRO_BEAM), u(20, ASTRO_CONSTELLATION),
                    u(32, ASTRO_PULL), u(44, ASTRO_SUPERNOVA)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.8)));

    public static final PlayerClass SUMMONER = reg(new PlayerClass("summoner", Role.DAMAGE, "Призыватель",
            0xFFBFE8FF, "☗",
            "Играет армией: скелеты, теневые волки и костяной голем — настоящие существа, ходят за хозяином и сами бросаются на врагов.",
            "Призыватель редко бьёт сам. Он поднимает тех, кто уже не против послужить, и смотрит, как они работают.",
            Resource.SOULS, new StatSet(0.5, 0.9, 1.0, 2.1, 0.6),
            List.of(u(10, SUMMON_SKELETON), u(10, SUMMON_WOLVES), u(10, SUMMON_ORDER), u(20, SUMMON_GOLEM),
                    u(32, SUMMON_BURST), u(44, SUMMON_LEGION)),
            Map.of(Style.MAGIC, 0.8, Style.SKILLS, 1.0)));

    public static final PlayerClass ILLUSIONIST = reg(new PlayerClass("illusionist", Role.DAMAGE, "Иллюзионист",
            0xFFD6A6FF, "⧈",
            "Бой двойниками: копии стоят в мире, бьют сами, забирают на себя врагов, с ними можно поменяться местами и лопнуть их все разом.",
            "Иллюзионисту всё равно, по кому бьют. Главное, чтобы бил не по нему.",
            Resource.MIRAGE, new StatSet(0.6, 1.4, 0.8, 1.9, 0.8),
            List.of(u(10, ILLUSION_DOUBLE), u(10, ILLUSION_SWAP), u(10, ILLUSION_CHARM), u(20, ILLUSION_KALEIDOSCOPE),
                    u(32, ILLUSION_STRIKE), u(44, ILLUSION_VANISH)),
            Map.of(Style.MAGIC, 0.9, Style.SKILLS, 1.0)));

    public static final PlayerClass WARLOCK = reg(new PlayerClass("warlock", Role.DAMAGE, "Чернокнижник",
            0xFFB0001A, "⚯",
            "Платит своим здоровьем за чужую боль: пакт превращает кровь в урон, круг тянет здоровье врагов, приговор стоит половины жизни.",
            "Чернокнижник знает цену и платит её сам. Поэтому с ним не спорят даже те, кто сильнее.",
            Resource.PACT, new StatSet(0.5, 0.8, 1.1, 2.2, 0.6),
            List.of(u(10, WARLOCK_CURSE), u(10, WARLOCK_CORRUPTION), u(10, WARLOCK_PACT), u(20, WARLOCK_CIRCLE),
                    u(32, WARLOCK_TENTACLES), u(44, WARLOCK_SACRIFICE)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.7)));

    public static final PlayerClass RIFTER = reg(new PlayerClass("rifter", Role.DAMAGE, "Разломщик", 0xFF9B5CFF, "◇",
            "Рвёт мир: разломы тянут врагов, шаг в пустоту переносит сквозь стены, зев гасит снаряды, а схлопывание закрывает всё разом.",
            "Разломщик не ищет дорогу. Он делает дыру там, где ему нужно пройти, и закрывает её за собой.",
            Resource.NONE, new StatSet(0.5, 1.2, 0.9, 2.2, 0.6),
            List.of(u(10, RIFT_RIFT), u(10, RIFT_STEP), u(10, RIFT_MARK), u(20, RIFT_COLLAPSE), u(32, RIFT_MAW),
                    u(44, RIFT_INSIDE)),
            Map.of(Style.MAGIC, 1.0, Style.SKILLS, 0.85)));

    public static final PlayerClass CHRONOLOG = reg(new PlayerClass("chronolog", Role.SUPPORT, "Хронолог",
            0xFFFFE27A, "⧗",
            "Лечит временем: возвращает союзнику здоровье, которое было пять секунд назад, останавливает врага и ускоряет своих.",
            "Хронолог не спорит с раной. Он просто находит минуту, когда её ещё не было, и приносит её сюда.",
            Resource.SAND, new StatSet(0.3, 0.8, 1.0, 2.3, 0.6),
            List.of(u(10, CHRONO_REWIND), u(10, CHRONO_STASIS), u(10, CHRONO_HASTE), u(20, CHRONO_LOOP),
                    u(32, CHRONO_HOURGLASS), u(44, CHRONO_REPLAY)),
            Map.of(Style.HEALING, 1.0, Style.SKILLS, 0.7, Style.MAGIC, 0.4)));

    public static final PlayerClass SHADOW_PRIEST = reg(new PlayerClass("shadow_priest", Role.SUPPORT, "Теневой жрец",
            0xFF6B4AA8, "☾",
            "Лечит тем, что отнял: его урон становится здоровьем отмеченного союзника, а зона сумерек поит группу чужой болью.",
            "Жрец знает: свет кончается, а тьма — нет. Он берёт у врага и отдаёт своим, и совесть его спокойна.",
            Resource.TWILIGHT, new StatSet(0.5, 0.6, 1.2, 2.2, 0.5),
            List.of(u(10, PRIEST_LASH), u(10, PRIEST_ATONEMENT), u(10, PRIEST_TWILIGHT), u(20, PRIEST_BARGAIN),
                    u(32, PRIEST_VEIL), u(44, PRIEST_DEVOUR)),
            Map.of(Style.HEALING, 1.0, Style.MAGIC, 0.8, Style.SKILLS, 0.5)));

    public static final PlayerClass DRUID = reg(new PlayerClass("druid", Role.SUPPORT, "Друид", 0xFF6BE38A, "✿",
            "Лечение, которое растёт само: цветы и лоза тлеют на раненых, живое древо лечит в своём радиусе, весна ускоряет всё сразу.",
            "Друид не стоит над раненым. Он сажает — и уходит сажать дальше, а посаженное делает дело за него.",
            Resource.NONE, new StatSet(0.3, 0.7, 1.2, 2.2, 0.6),
            List.of(u(10, DRUID_FLOWER), u(10, DRUID_VINE), u(10, DRUID_THORNS), u(20, DRUID_TREE),
                    u(32, DRUID_BLOOM), u(44, DRUID_SPRING)),
            Map.of(Style.HEALING, 1.0, Style.MAGIC, 0.5, Style.SKILLS, 0.5)));

    public static final PlayerClass SHAMAN = reg(new PlayerClass("shaman", Role.SUPPORT, "Шаман", 0xFF7BF5A0, "⌂",
            "Работает тотемами: они стоят в мире, рисуют свой радиус, лечат, бьют молнией и держат бафф — пока враги их не сломают.",
            "Шаман не носит силу с собой. Он ставит её на землю, и дальше земля держит отряд сама.",
            Resource.NONE, new StatSet(0.4, 0.6, 1.3, 2.1, 0.6),
            List.of(u(10, SHAMAN_TOTEM_HEAL), u(10, SHAMAN_LIGHTNING), u(10, SHAMAN_TOTEM_STORM),
                    u(20, SHAMAN_TOTEM_EARTH), u(32, SHAMAN_ANCESTORS), u(44, SHAMAN_CONSUME)),
            Map.of(Style.HEALING, 1.0, Style.SKILLS, 0.8, Style.MAGIC, 0.6)));

    public static final PlayerClass BANNERMAN = reg(new PlayerClass("bannerman", Role.SUPPORT, "Знаменосец",
            0xFFD8343A, "⚑",
            "Держит отряд знамёнами: пока знамя цело, у своих в его радиусе аура, а клич снимает с группы оглушения.",
            "Знаменосец не бьёт и почти не лечит. Он просто ставит знамя там, где отряд должен выстоять, — и отряд стоит.",
            Resource.NONE, new StatSet(0.8, 0.6, 1.5, 1.8, 0.3),
            List.of(u(10, BANNER_POWER), u(10, BANNER_GUARD), u(10, BANNER_SHOUT), u(20, BANNER_SPEED),
                    u(32, BANNER_RANKS), u(44, BANNER_LAST_STAND)),
            Map.of(Style.HEALING, 0.8, Style.PROTECTION, 1.0, Style.SKILLS, 0.5)));

    /** Навыки роли до получения класса. */
    public static final Map<Role, List<Skill>> ROLE_KITS = Map.of(
            Role.TANK, List.of(TAUNT, SHIELD_BASH, BULWARK),
            Role.DAMAGE, List.of(HEAVY_STRIKE, WHIRLWIND, THROWING_KNIFE),
            Role.SUPPORT, List.of(MEND, RENEW, SMITE),
            Role.NONE, List.of());

    private ClassRegistry() {
    }

    private static PlayerClass.Unlock u(int level, Skill s) {
        return new PlayerClass.Unlock(level, s);
    }

    private static PlayerClass reg(PlayerClass c) {
        if (ALL.putIfAbsent(c.id(), c) != null) throw new IllegalStateException("класс уже есть: " + c.id());
        return c;
    }

    public static PlayerClass get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static Collection<PlayerClass> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    public static List<PlayerClass> ofRole(Role r) {
        return ALL.values().stream().filter(c -> c.role() == r).toList();
    }

    /** Навыки на панели: навыки класса (открытые) или, пока класса нет, навыки роли. Не больше 6. */
    public static List<Skill> loadout(Role role, String classId, int level) {
        PlayerClass c = get(classId);
        List<Skill> list = c != null ? c.unlocked(level) : ROLE_KITS.getOrDefault(role, List.of());
        return list.size() > 6 ? list.subList(0, 6) : list;
    }
}
