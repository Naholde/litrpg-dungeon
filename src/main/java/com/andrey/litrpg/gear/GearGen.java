package com.andrey.litrpg.gear;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.GearBalance;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Unbreakable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Генерация вещей Системы: редкость по весам (удача сдвигает к редким), основа по уровню, характеристики
 * и свойства под роль игрока. Вид предмета и имя — от уровня (железо → закалённая сталь → звёздный металл).
 */
public final class GearGen {
    private GearGen() {
    }

    /** Предпочтения роли: какое оружие и какие характеристики чаще. */
    private static final Map<Role, List<GearKind>> WEAPONS = Map.of(
            Role.TANK, List.of(GearKind.SWORD, GearKind.SHIELD),
            Role.DAMAGE, List.of(GearKind.SWORD, GearKind.AXE, GearKind.BOW),
            Role.SUPPORT, List.of(GearKind.STAFF),
            Role.NONE, List.of(GearKind.SWORD, GearKind.BOW, GearKind.STAFF));
    private static final Map<Role, List<Stat>> STATS = Map.of(
            Role.TANK, List.of(Stat.VITALITY, Stat.VITALITY, Stat.STRENGTH, Stat.AGILITY),
            Role.DAMAGE, List.of(Stat.STRENGTH, Stat.AGILITY, Stat.AGILITY, Stat.LUCK, Stat.VITALITY),
            Role.SUPPORT, List.of(Stat.INTELLECT, Stat.INTELLECT, Stat.VITALITY, Stat.LUCK),
            Role.NONE, List.of(Stat.values()));
    private static final Map<Role, List<AffixType>> AFFIXES = Map.of(
            Role.TANK, List.of(AffixType.TOUGHNESS, AffixType.DODGE, AffixType.HASTE, AffixType.WEAPON_POWER, AffixType.MOVE_SPEED),
            Role.DAMAGE, List.of(AffixType.CRIT_CHANCE, AffixType.CRIT_DAMAGE, AffixType.WEAPON_POWER, AffixType.RANGED_POWER,
                    AffixType.SKILL_POWER, AffixType.HASTE, AffixType.MOVE_SPEED),
            Role.SUPPORT, List.of(AffixType.HEAL_POWER, AffixType.HASTE, AffixType.SKILL_POWER, AffixType.TOUGHNESS,
                    AffixType.CRIT_CHANCE, AffixType.MOVE_SPEED),
            Role.NONE, List.of(AffixType.values()));

    private static final GearKind[] ARMOR = {GearKind.HELMET, GearKind.CHEST, GearKind.LEGS, GearKind.BOOTS};
    private static final GearKind[] TRINKETS = {GearKind.RING, GearKind.RING, GearKind.AMULET, GearKind.CHARM};

    /** Редкость по весам; {@code find} (находки) поднимает вес редких: × (1 + find)^ступень. */
    public static Rarity rollRarity(RandomSource rnd, double find, Rarity min) {
        GearBalance g = Balance.get(Balance.GEAR);
        double total = 0;
        double[] w = new double[Rarity.values().length];
        for (Rarity r : Rarity.values()) {
            if (r.ordinal() < min.ordinal()) continue;
            w[r.ordinal()] = g.of(r).weight() * Math.pow(1 + Math.max(0, find), r.ordinal());
            total += w[r.ordinal()];
        }
        if (total <= 0) return min;
        double x = rnd.nextDouble() * total;
        for (Rarity r : Rarity.values()) {
            x -= w[r.ordinal()];
            if (x < 0) return r;
        }
        return min;
    }

    /** Случайный вид вещи под роль: оружие ~25%, броня ~50%, украшения ~25%. */
    public static GearKind rollKind(RandomSource rnd, Role role) {
        double x = rnd.nextDouble();
        if (x < 0.25) {
            List<GearKind> w = WEAPONS.getOrDefault(role, WEAPONS.get(Role.NONE));
            return w.get(rnd.nextInt(w.size()));
        }
        if (x < 0.75) return ARMOR[rnd.nextInt(ARMOR.length)];
        return TRINKETS[rnd.nextInt(TRINKETS.length)];
    }

    public static ItemStack roll(RandomSource rnd, Role role, int level, Rarity rarity) {
        return make(rollKind(rnd, role), level, rarity, role, rnd);
    }

    /**
     * Добыча для игрока: с долей {@code smart_slot} — в слот, где у него пусто или вещь самая старая
     * (по уровню), иначе — как обычно. Так вещи обновляются равномернее, а не пятый шлем подряд.
     */
    public static ItemStack rollFor(net.minecraft.server.level.ServerPlayer p, int level, Rarity rarity) {
        Role role = com.andrey.litrpg.player.PlayerService.data(p).role();
        RandomSource rnd = p.getRandom();
        GearKind kind = rollKind(rnd, role);
        if (rnd.nextDouble() < Balance.get(Balance.GEAR).drops().smartSlot()) {
            Map<GearKind, Integer> worn = new EnumMap<>(GearKind.class);
            int[] rings = {0};
            PlayerGear.forEachWorn(p, d -> {
                if (d.kind() == GearKind.RING) rings[0]++;
                worn.merge(d.kind(), d.level(), Math::min);
            });
            List<GearKind> wanted = new ArrayList<>(List.of(ARMOR));
            wanted.addAll(WEAPONS.getOrDefault(role, WEAPONS.get(Role.NONE)));
            wanted.addAll(List.of(GearKind.RING, GearKind.AMULET, GearKind.CHARM));
            GearKind best = null;
            int lowest = Integer.MAX_VALUE;
            java.util.Collections.shuffle(wanted, new java.util.Random(rnd.nextLong()));
            for (GearKind k : wanted) {
                int lv = k == GearKind.RING && rings[0] < 2 ? 0 : worn.getOrDefault(k, 0);
                if (lv < lowest) {
                    lowest = lv;
                    best = k;
                }
            }
            if (best != null) kind = best;
        }
        return make(kind, level, rarity, role, rnd);
    }

    public static ItemStack make(GearKind kind, int level, Rarity rarity, Role role, RandomSource rnd) {
        GearBalance g = Balance.get(Balance.GEAR);
        GearBalance.RarityInfo ri = g.of(rarity);
        int lv = Math.max(1, level);
        double base = base(kind, lv, ri.power(), g);
        // Характеристики: бюджет делится между выбранными (по предпочтениям роли), ±20%.
        Map<Stat, Double> stats = new EnumMap<>(Stat.class);
        List<Stat> pool = new ArrayList<>(STATS.getOrDefault(role, STATS.get(Role.NONE)));
        double budget = g.statPerLevel() * lv * ri.power();
        int n = Math.max(1, ri.stats());
        for (int i = 0; i < n && !pool.isEmpty(); i++) {
            Stat s = pool.remove(rnd.nextInt(pool.size()));
            pool.removeIf(x -> x == s);
            double v = budget / n * (0.8 + rnd.nextDouble() * 0.4);
            stats.merge(s, round1(v), Double::sum);
            if (pool.isEmpty()) pool.addAll(List.of(Stat.values()));
        }
        List<GearData.Affix> affixes = rollAffixes(rnd, role, lv, ri.affixes(), g);
        List<GearData.Effect> effects = rollEffects(rnd, role, rarity, g);
        GearData data = new GearData(kind, lv, rarity, round1(base), stats, affixes, "", 0, effects);
        return stack(data, name(kind, lv, stats, effects));
    }

    /** Сколько особых свойств: эпическая — с шансом одно, легендарная — одно, божественная — два. */
    public static int effectCount(RandomSource rnd, Rarity rarity, GearBalance g) {
        return switch (rarity) {
            case EPIC -> rnd.nextDouble() < g.drops().epicEffectChance() ? 1 : 0;
            case LEGENDARY -> 1;
            case DIVINE -> 2;
            default -> 0;
        };
    }

    /** Особые свойства под роль: сила — эпическая в нижней половине диапазона, легендарная в верхней, божественная — верх. */
    public static List<GearData.Effect> rollEffects(RandomSource rnd, Role role, Rarity rarity, GearBalance g) {
        int n = effectCount(rnd, rarity, g);
        List<GearData.Effect> out = new ArrayList<>();
        if (n == 0) return out;
        List<GearEffect> pool = new ArrayList<>(GearEffect.forRole(role));
        Collections.shuffle(pool, new java.util.Random(rnd.nextLong()));
        for (int i = 0; i < n && i < pool.size(); i++) out.add(effect(pool.get(i), rarity, rnd, g));
        return out;
    }

    public static GearData.Effect effect(GearEffect fx, Rarity rarity, RandomSource rnd, GearBalance g) {
        GearBalance.Range r = g.effects().getOrDefault(fx, new GearBalance.Range(0.1, 0.2));
        double lo = switch (rarity) {
            case LEGENDARY -> 0.5;
            case DIVINE -> 0.85;
            default -> 0;
        };
        double hi = rarity == Rarity.EPIC || rarity.ordinal() < Rarity.EPIC.ordinal() ? 0.5 : 1;
        double t = lo + rnd.nextDouble() * (hi - lo);
        double v = r.min() + (r.max() - r.min()) * t;
        return new GearData.Effect(fx, Math.round(v * 1000) / 1000.0);
    }

    public static List<GearData.Affix> rollAffixes(RandomSource rnd, Role role, int level, int count, GearBalance g) {
        List<AffixType> pool = new ArrayList<>(AFFIXES.getOrDefault(role, AFFIXES.get(Role.NONE)));
        Collections.shuffle(pool, new java.util.Random(rnd.nextLong()));
        List<GearData.Affix> out = new ArrayList<>();
        // Фаза 13: свойство заметно с первого уровня — половина силы на 1-м, полная на 50-м.
        double scale = affixScale(level);
        for (int i = 0; i < count && i < pool.size(); i++) {
            AffixType t = pool.get(i);
            GearBalance.Range r = g.affixes().get(t);
            if (r == null) continue;
            double v = (r.min() + rnd.nextDouble() * (r.max() - r.min())) * scale;
            out.add(new GearData.Affix(t, Math.round(v * 1000) / 1000.0));
        }
        return out;
    }

    /** Доля силы свойств на уровне: 0,5 на 1-м → 1 на 50-м. */
    public static double affixScale(int level) {
        return 0.5 + 0.5 * Math.max(0, Math.min(1, (level - 1) / 49.0));
    }

    public static double base(GearKind kind, int level, double power, GearBalance g) {
        GearBalance.Weapons w = g.weapons();
        GearBalance.Armor a = g.armor();
        return power * switch (kind) {
            case SWORD -> w.meleeBase() + w.meleePerLevel() * (level - 1);
            case AXE -> (w.meleeBase() + w.meleePerLevel() * (level - 1)) * w.axeMult();
            case BOW -> w.bowBase() + w.bowPerLevel() * (level - 1);
            case STAFF -> w.staffBase() + w.staffPerLevel() * (level - 1);
            case HELMET -> a.perLevel() * level * a.helmet();
            case CHEST -> a.perLevel() * level * a.chest();
            case LEGS -> a.perLevel() * level * a.legs();
            case BOOTS -> a.perLevel() * level * a.boots();
            case SHIELD -> a.perLevel() * level * a.shield();
            case RING, AMULET, CHARM -> 0;
        };
    }

    /** Предмет-основа: вид от уровня — до 16 железо, до 32 сталь (алмаз), дальше звёздный металл (незерит). */
    public static Item baseItem(GearKind kind, int level) {
        int tier = level < 16 ? 0 : level < 32 ? 1 : 2;
        return switch (kind) {
            case SWORD -> new Item[]{Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD}[tier];
            case AXE -> new Item[]{Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE}[tier];
            case BOW -> Items.BOW;
            case STAFF -> ModItems.STAFF.get();
            case SHIELD -> Items.SHIELD;
            case HELMET -> new Item[]{Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET}[tier];
            case CHEST -> new Item[]{Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE}[tier];
            case LEGS -> new Item[]{Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS}[tier];
            case BOOTS -> new Item[]{Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}[tier];
            case RING -> ModItems.RING.get();
            case AMULET -> ModItems.AMULET.get();
            case CHARM -> ModItems.CHARM.get();
        };
    }

    /** Собрать предмет: компонент, имя цветом редкости, неразрушимость, атрибуты оружия; ванильная броня — ноль. */
    public static ItemStack stack(GearData data, String name) {
        ItemStack s = new ItemStack(baseItem(data.kind(), data.level()));
        s.set(ModComponents.GEAR.get(), data);
        s.set(DataComponents.ITEM_NAME, Component.literal(name).withColor(data.rarity().color & 0xFFFFFF));
        s.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        s.set(DataComponents.ATTRIBUTE_MODIFIERS, attributes(data));
        // Эпические и выше светятся — их видно в сумке и на земле.
        if (data.rarity().ordinal() >= Rarity.EPIC.ordinal()) s.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return s;
    }

    /** Урон оружия ближнего боя — через атрибут (ванильный удар); броня — только своя (ванильная обнулена). */
    private static ItemAttributeModifiers attributes(GearData d) {
        var b = ItemAttributeModifiers.builder();
        if (d.kind() == GearKind.SWORD || d.kind() == GearKind.AXE) {
            b.add(Attributes.ATTACK_DAMAGE, new AttributeModifier(LitRPG.id("gear_damage"), d.base() - 1,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            b.add(Attributes.ATTACK_SPEED, new AttributeModifier(LitRPG.id("gear_speed"), d.kind() == GearKind.AXE ? -3.0 : -2.4,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        }
        return b.build().withTooltip(false);
    }

    private static final Map<Stat, String> SUFFIX = Map.of(Stat.STRENGTH, "силы", Stat.AGILITY, "ловкости",
            Stat.VITALITY, "здоровья", Stat.INTELLECT, "мудрости", Stat.LUCK, "удачи");

    /** Имя: материал по уровню + вид + «чего» по главной характеристике: «Закалённый меч силы». */
    /** Имя с особым свойством: «Закалённый меч силы «Кровопийца»». */
    public static String name(GearKind kind, int level, Map<Stat, Double> stats, List<GearData.Effect> effects) {
        String n = name(kind, level, stats);
        return effects.isEmpty() ? n : n + " «" + effects.get(0).type().title + "»";
    }

    public static String name(GearKind kind, int level, Map<Stat, Double> stats) {
        int tier = level < 16 ? 0 : level < 32 ? 1 : 2;
        String[] stem = {"Железн", "Закалённ", "Звёздн"};
        String end = switch (kind) {
            case LEGS, BOOTS -> "ые";
            case RING -> "ое";
            default -> "ый";
        };
        String adj = stem[tier] + end;
        if (kind.isAccessory()) adj = new String[]{"Медн", "Серебрян", "Золот"}[tier] + (kind == GearKind.RING ? "ое" : "ый");
        Stat top = stats.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        String noun = kind.title.toLowerCase(java.util.Locale.ROOT);
        return adj + " " + noun + (top != null ? " " + SUFFIX.get(top) : "");
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
