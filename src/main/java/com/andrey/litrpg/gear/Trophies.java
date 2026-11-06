package com.andrey.litrpg.gear;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.GearBalance;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Трофеи хранителей: у каждого хранителя четыре именные вещи — для танка, бойца, поддержки и украшение для любого.
 * С победы каждый получает трофей под свою роль (или украшение): эпический, чаще у финалов — легендарный; с финалов
 * Кузен и Шпиля — малый шанс божественного. У трофея два «фирменных» свойства сильнее обычных.
 */
public final class Trophies {
    /** Трофей: имя, вид, для какой роли (NONE — для любой), фирменные свойства, строка-описание. */
    public record Trophy(String id, String boss, String name, GearKind kind, Role role, AffixType a, AffixType b, String lore) {
    }

    private static final Map<String, Trophy> ALL = new LinkedHashMap<>();
    private static final Map<String, List<Trophy>> BY_BOSS = new LinkedHashMap<>();

    static {
        // Этап 1 · Склеп Забытых
        set("gatekeeper",
                t(GearKind.CHEST, Role.TANK, "Рёберный доспех Привратника", AffixType.TOUGHNESS, AffixType.HASTE, "Рёбра держат удар лучше любой стали."),
                t(GearKind.AXE, Role.DAMAGE, "Цепной топор Привратника", AffixType.CRIT_CHANCE, AffixType.WEAPON_POWER, "Звенит, даже когда стоит у стены."),
                t(GearKind.STAFF, Role.SUPPORT, "Посох костяного вала", AffixType.HEAL_POWER, AffixType.HASTE, "Вал из костей — тоже стена."),
                t(GearKind.RING, Role.NONE, "Звено вечной цепи", AffixType.TOUGHNESS, AffixType.FIND, "Цепь порвалась. Звено осталось."));
        set("morra",
                t(GearKind.SHIELD, Role.TANK, "Щит сестринской связи", AffixType.TOUGHNESS, AffixType.DODGE, "Между двумя — всегда третий."),
                t(GearKind.SWORD, Role.DAMAGE, "Коса Морры", AffixType.CRIT_DAMAGE, AffixType.WEAPON_POWER, "Жнёт и тех, кто встал в луч."),
                t(GearKind.STAFF, Role.SUPPORT, "Посох Вессы", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Поднимал сестру. Поднимет и вас."),
                t(GearKind.AMULET, Role.NONE, "Медальон двух сестёр", AffixType.CRIT_CHANCE, AffixType.HEAL_POWER, "Внутри — два портрета. Оба смотрят на вас."));
        set("lich",
                t(GearKind.HELMET, Role.TANK, "Корона Карна", AffixType.TOUGHNESS, AffixType.HASTE, "Тяжела. Холодна. Ваша."),
                t(GearKind.BOW, Role.DAMAGE, "Лук ледяной гробницы", AffixType.RANGED_POWER, AffixType.CRIT_CHANCE, "Стрелы покрываются инеем на тетиве."),
                t(GearKind.STAFF, Role.SUPPORT, "Филактерия на древке", AffixType.HEAL_POWER, AffixType.HASTE, "Душа внутри теперь лечит, а не прячется."),
                t(GearKind.CHARM, Role.NONE, "Осколок филактерии", AffixType.SKILL_POWER, AffixType.FIND, "Тёплый — для вещи мертвеца."));
        // Этап 2 · Паучья чаща
        set("arachna",
                t(GearKind.CHEST, Role.TANK, "Хитиновый панцирь Аракны", AffixType.TOUGHNESS, AffixType.DODGE, "Восемь глаз видели, как он выдерживал удары."),
                t(GearKind.BOW, Role.DAMAGE, "Тетива из паутины королевы", AffixType.RANGED_POWER, AffixType.CRIT_DAMAGE, "Не рвётся. Никогда."),
                t(GearKind.STAFF, Role.SUPPORT, "Посох коконов", AffixType.HEAL_POWER, AffixType.HASTE, "Кокон бережёт то, что внутри."),
                t(GearKind.RING, Role.NONE, "Кольцо восьми глаз", AffixType.CRIT_CHANCE, AffixType.DODGE, "Оно смотрит во все стороны сразу."));
        set("rot_ent",
                t(GearKind.SHIELD, Role.TANK, "Кора Древня", AffixType.TOUGHNESS, AffixType.HASTE, "Гниль снаружи, сердцевина — крепкая."),
                t(GearKind.AXE, Role.DAMAGE, "Топор гнилой сердцевины", AffixType.WEAPON_POWER, AffixType.CRIT_DAMAGE, "Рубит то, что растёт вниз."),
                t(GearKind.STAFF, Role.SUPPORT, "Росток сердцевины", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Зелёный побег на мёртвом дереве."),
                t(GearKind.CHARM, Role.NONE, "Споровый оберег", AffixType.HASTE, AffixType.FIND, "Пахнет лесом, которого больше нет."));
        set("morena",
                t(GearKind.HELMET, Role.TANK, "Рогатый венец Изнанки", AffixType.TOUGHNESS, AffixType.DODGE, "С ним страх приходит к другим."),
                t(GearKind.SWORD, Role.DAMAGE, "Клинок страха", AffixType.CRIT_CHANCE, AffixType.WEAPON_POWER, "Режет тех, кто остался один."),
                t(GearKind.STAFF, Role.SUPPORT, "Глаз Морены", AffixType.HEAL_POWER, AffixType.HASTE, "Видит Изнанку. И путь обратно."),
                t(GearKind.AMULET, Role.NONE, "Зеркальце Изнанки", AffixType.SKILL_POWER, AffixType.DODGE, "В нём вы стоите наоборот."));
        // Этап 3 · Затопленный храм
        set("tide_warden",
                t(GearKind.SHIELD, Role.TANK, "Створка раковины Прилива", AffixType.TOUGHNESS, AffixType.HASTE, "Волна разбивается о неё, как о колонну."),
                t(GearKind.AXE, Role.DAMAGE, "Трезубец Стража", AffixType.WEAPON_POWER, AffixType.CRIT_CHANCE, "Три зуба — три раны."),
                t(GearKind.STAFF, Role.SUPPORT, "Жезл отлива", AffixType.HEAL_POWER, AffixType.HASTE, "Отлив уносит боль."),
                t(GearKind.RING, Role.NONE, "Кольцо волны", AffixType.MOVE_SPEED, AffixType.TOUGHNESS, "Холодное, как глубина."));
        set("leviathan",
                t(GearKind.CHEST, Role.TANK, "Чешуя Левиафана", AffixType.TOUGHNESS, AffixType.DODGE, "Каждая чешуйка — с ладонь."),
                t(GearKind.BOW, Role.DAMAGE, "Гарпун-лук Левиафана", AffixType.RANGED_POWER, AffixType.CRIT_DAMAGE, "Бьёт туда, где вынырнет добыча."),
                t(GearKind.STAFF, Role.SUPPORT, "Клык глубин", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Клык, который научился лечить."),
                t(GearKind.AMULET, Role.NONE, "Глаз Левиафана", AffixType.CRIT_CHANCE, AffixType.FIND, "Всё ещё моргает."));
        set("naiad",
                t(GearKind.LEGS, Role.TANK, "Поножи жемчужной стражи", AffixType.TOUGHNESS, AffixType.HASTE, "Стражи Наяры не отступали."),
                t(GearKind.SWORD, Role.DAMAGE, "Копьё Наяры", AffixType.WEAPON_POWER, AffixType.CRIT_DAMAGE, "Бьёт только из света."),
                t(GearKind.STAFF, Role.SUPPORT, "Жемчужный посох Наяры", AffixType.HEAL_POWER, AffixType.HASTE, "Песнь глубин — теперь колыбельная."),
                t(GearKind.CHARM, Role.NONE, "Капля святилища", AffixType.SKILL_POWER, AffixType.HEAL_POWER, "Не высыхает уже триста лет."));
        // Этап 4 · Кузни Пепла
        set("smelter",
                t(GearKind.CHEST, Role.TANK, "Тигельный нагрудник", AffixType.TOUGHNESS, AffixType.HASTE, "Внутри ещё теплится расплав."),
                t(GearKind.AXE, Role.DAMAGE, "Молот плавильни", AffixType.WEAPON_POWER, AffixType.CRIT_CHANCE, "Ковал броню. Теперь ломает её."),
                t(GearKind.STAFF, Role.SUPPORT, "Ковш живого огня", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Огонь, который греет, а не жжёт."),
                t(GearKind.RING, Role.NONE, "Заклёпка голема", AffixType.TOUGHNESS, AffixType.CRIT_DAMAGE, "Держала броню целого голема."));
        set("chariot",
                t(GearKind.BOOTS, Role.TANK, "Подковы Колесницы", AffixType.MOVE_SPEED, AffixType.TOUGHNESS, "След горит ещё секунду."),
                t(GearKind.BOW, Role.DAMAGE, "Лук гонщика", AffixType.RANGED_POWER, AffixType.HASTE, "Стреляет быстрее, чем вы целитесь."),
                t(GearKind.STAFF, Role.SUPPORT, "Вожжи пламени", AffixType.HEAL_POWER, AffixType.MOVE_SPEED, "Держите отряд — как коней."),
                t(GearKind.CHARM, Role.NONE, "Спица огненного колеса", AffixType.MOVE_SPEED, AffixType.CRIT_CHANCE, "Всё ещё крутится."));
        set("ash_lord",
                t(GearKind.SHIELD, Role.TANK, "Щит Владыки Пепла", AffixType.TOUGHNESS, AffixType.DODGE, "Пепел с него не стряхнуть."),
                t(GearKind.SWORD, Role.DAMAGE, "Меч Пепла", AffixType.CRIT_DAMAGE, AffixType.WEAPON_POWER, "Вызывает на дуэль сам."),
                t(GearKind.STAFF, Role.SUPPORT, "Скипетр Кальдеры", AffixType.HEAL_POWER, AffixType.HASTE, "Под ним — спящий вулкан."),
                t(GearKind.AMULET, Role.NONE, "Корона из пепла", AffixType.SKILL_POWER, AffixType.CRIT_CHANCE, "Рассыпается в руках — и собирается снова."));
        // Этап 5 · Шпиль Бездны
        set("doppel",
                t(GearKind.HELMET, Role.TANK, "Маска Двойника", AffixType.TOUGHNESS, AffixType.DODGE, "В ней вы — это вы. Наверное."),
                t(GearKind.SWORD, Role.DAMAGE, "Зеркальный клинок", AffixType.CRIT_CHANCE, AffixType.CRIT_DAMAGE, "Отражает того, кто держит."),
                t(GearKind.STAFF, Role.SUPPORT, "Посох отражений", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Лечит и того, кто в зеркале."),
                t(GearKind.RING, Role.NONE, "Кольцо второго я", AffixType.HASTE, AffixType.DODGE, "Носите на левой. Или на правой."));
        set("chronophage",
                t(GearKind.CHEST, Role.TANK, "Доспех остановленного часа", AffixType.TOUGHNESS, AffixType.HASTE, "Удар доходит до него на миг позже."),
                t(GearKind.BOW, Role.DAMAGE, "Лук ускоренного времени", AffixType.RANGED_POWER, AffixType.HASTE, "Стрела прилетает до выстрела."),
                t(GearKind.STAFF, Role.SUPPORT, "Песочные часы-посох", AffixType.HEAL_POWER, AffixType.HASTE, "Переворачивает раны назад."),
                t(GearKind.CHARM, Role.NONE, "Песчинка вечности", AffixType.HASTE, AffixType.SKILL_POWER, "Самая медленная песчинка на свете."));
        set("xalathar",
                t(GearKind.SHIELD, Role.TANK, "Пасть Бездны", AffixType.TOUGHNESS, AffixType.DODGE, "Голод теперь на вашей стороне."),
                t(GearKind.SWORD, Role.DAMAGE, "Голод — клинок Ксалатара", AffixType.CRIT_DAMAGE, AffixType.WEAPON_POWER, "Всегда хочет ещё."),
                t(GearKind.STAFF, Role.SUPPORT, "Сердце Бездны", AffixType.HEAL_POWER, AffixType.SKILL_POWER, "Бьётся. Медленно. Ровно."),
                t(GearKind.AMULET, Role.NONE, "Глаз Голода", AffixType.CRIT_CHANCE, AffixType.FIND, "Видит, кто ранен."));
    }

    private Trophies() {
    }

    private static Trophy t(GearKind kind, Role role, String name, AffixType a, AffixType b, String lore) {
        return new Trophy("", "", name, kind, role, a, b, lore);
    }

    private static void set(String boss, Trophy... list) {
        List<Trophy> out = new ArrayList<>();
        for (int i = 0; i < list.length; i++) {
            Trophy t = list[i];
            Trophy full = new Trophy(boss + "_" + i, boss, t.name, t.kind, t.role, t.a, t.b, t.lore);
            ALL.put(full.id, full);
            out.add(full);
        }
        BY_BOSS.put(boss, List.copyOf(out));
    }

    public static Trophy get(String id) {
        return ALL.get(id);
    }

    public static List<Trophy> of(String boss) {
        return BY_BOSS.getOrDefault(boss, List.of());
    }

    public static Map<String, List<Trophy>> all() {
        return BY_BOSS;
    }

    /**
     * Трофей с хранителя {@code boss} для игрока роли {@code role}: вещь его роли (60%) или украшение. Редкость:
     * эпический, с шансом легендарный ({@code legendaryChance}), с ещё меньшим — божественный ({@code divineChance}).
     */
    public static ItemStack roll(RandomSource rnd, String boss, Role role, int level, double legendaryChance, double divineChance) {
        List<Trophy> list = of(boss);
        if (list.isEmpty()) return GearGen.roll(rnd, role, level, Rarity.EPIC);
        Trophy pick = null;
        if (rnd.nextDouble() < 0.6) pick = list.stream().filter(t -> t.role == role).findFirst().orElse(null);
        if (pick == null) pick = list.stream().filter(t -> t.role == Role.NONE).findFirst().orElse(list.get(0));
        Rarity rarity = rnd.nextDouble() < divineChance ? Rarity.DIVINE : rnd.nextDouble() < legendaryChance ? Rarity.LEGENDARY : Rarity.EPIC;
        return make(pick, level, rarity, role, rnd);
    }

    /**
     * Фирменное особое свойство трофея (фаза 13): у каждого хранителя — своё на каждую из четырёх вещей,
     * по смыслу хранителя (цепь Привратника держит удар, коса Морры добивает, посох Вессы лечит в избытке…).
     */
    private static final Map<String, GearEffect[]> SIGNATURE = Map.ofEntries(
            Map.entry("gatekeeper", new GearEffect[]{GearEffect.BULWARK, GearEffect.FIRST_STRIKE, GearEffect.MERCY, GearEffect.TAILWIND}),
            Map.entry("morra", new GearEffect[]{GearEffect.WARDEN, GearEffect.EXECUTE, GearEffect.OVERFLOW, GearEffect.TAILWIND}),
            Map.entry("lich", new GearEffect[]{GearEffect.SECOND_WIND, GearEffect.SPARK, GearEffect.ECHO, GearEffect.TAILWIND}),
            Map.entry("arachna", new GearEffect[]{GearEffect.THORNS, GearEffect.VAMPIRIC, GearEffect.MERCY, GearEffect.TAILWIND}),
            Map.entry("rot_ent", new GearEffect[]{GearEffect.BULWARK, GearEffect.DASH_FURY, GearEffect.OVERFLOW, GearEffect.TAILWIND}),
            Map.entry("morena", new GearEffect[]{GearEffect.WARDEN, GearEffect.OPENING, GearEffect.ECHO, GearEffect.TAILWIND}),
            Map.entry("tide_warden", new GearEffect[]{GearEffect.SECOND_WIND, GearEffect.SPARK, GearEffect.MERCY, GearEffect.TAILWIND}),
            Map.entry("leviathan", new GearEffect[]{GearEffect.THORNS, GearEffect.EXECUTE, GearEffect.OVERFLOW, GearEffect.TAILWIND}),
            Map.entry("naiad", new GearEffect[]{GearEffect.WARDEN, GearEffect.FIRST_STRIKE, GearEffect.ECHO, GearEffect.TAILWIND}),
            Map.entry("smelter", new GearEffect[]{GearEffect.BULWARK, GearEffect.VAMPIRIC, GearEffect.MERCY, GearEffect.TAILWIND}),
            Map.entry("chariot", new GearEffect[]{GearEffect.SECOND_WIND, GearEffect.DASH_FURY, GearEffect.OVERFLOW, GearEffect.TAILWIND}),
            Map.entry("ash_lord", new GearEffect[]{GearEffect.THORNS, GearEffect.OPENING, GearEffect.ECHO, GearEffect.TAILWIND}),
            Map.entry("doppel", new GearEffect[]{GearEffect.WARDEN, GearEffect.SPARK, GearEffect.MERCY, GearEffect.TAILWIND}),
            Map.entry("chronophage", new GearEffect[]{GearEffect.SECOND_WIND, GearEffect.DASH_FURY, GearEffect.OVERFLOW, GearEffect.TAILWIND}),
            Map.entry("xalathar", new GearEffect[]{GearEffect.BULWARK, GearEffect.EXECUTE, GearEffect.ECHO, GearEffect.TAILWIND}));

    /** Фирменное свойство трофея по его номеру у хранителя (0 — танк, 1 — боец, 2 — поддержка, 3 — украшение). */
    public static GearEffect signature(Trophy t) {
        GearEffect[] set = SIGNATURE.get(t.boss());
        if (set == null) return null;
        int i = Integer.parseInt(t.id().substring(t.id().lastIndexOf('_') + 1));
        return i < set.length ? set[i] : null;
    }

    /** Собрать трофей: основа и характеристики — как у вещи этой редкости, плюс два фирменных свойства (×1,5 от лучшего). */
    public static ItemStack make(Trophy t, int level, Rarity rarity, Role role, RandomSource rnd) {
        ItemStack base = GearGen.make(t.kind, level, rarity, role == Role.NONE ? Role.DAMAGE : role, rnd);
        GearData d = base.get(com.andrey.litrpg.core.ModComponents.GEAR.get());
        GearBalance g = Balance.get(Balance.GEAR);
        double scale = GearGen.affixScale(level);
        List<GearData.Affix> affixes = new ArrayList<>();
        for (AffixType a : List.of(t.a, t.b)) {
            GearBalance.Range r = g.affixes().get(a);
            if (r != null) affixes.add(new GearData.Affix(a, Math.round(r.max() * 1.5 * scale * 1000) / 1000.0));
        }
        for (GearData.Affix a : d.affixes()) if (a.type() != t.a && a.type() != t.b && affixes.size() < 4) affixes.add(a);
        Map<Stat, Double> stats = new EnumMap<>(Stat.class);
        stats.putAll(d.stats());
        // Особое свойство: фирменное всегда, у божественного — ещё одно случайное под роль.
        List<GearData.Effect> effects = new ArrayList<>();
        GearEffect sig = signature(t);
        if (sig != null) effects.add(GearGen.effect(sig, rarity == Rarity.EPIC ? Rarity.LEGENDARY : rarity, rnd, g));
        if (rarity == Rarity.DIVINE) {
            List<GearEffect> pool = new ArrayList<>(GearEffect.forRole(role));
            pool.remove(sig);
            if (!pool.isEmpty()) effects.add(GearGen.effect(pool.get(rnd.nextInt(pool.size())), rarity, rnd, g));
        }
        GearData nd = new GearData(t.kind, d.level(), rarity, d.base(), stats, affixes, t.id, 0, effects);
        return GearGen.stack(nd, t.name);
    }
}
