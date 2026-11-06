package com.andrey.litrpg.gear;

import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Stat;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.EnumMap;
import java.util.Map;

/** Сумма надетого: оружие в руке, щит, броня, украшения (Curios). Одинаково на сервере и клиенте. */
public final class PlayerGear {
    /** Итог снаряжения для расчёта характеристик. */
    public record Totals(StatSet stats, Map<Derived, Double> derived, double armor) {
        public static final Totals NONE = new Totals(StatSet.ZERO, Map.of(), 0);
    }

    private PlayerGear() {
    }

    /** Каждая надетая вещь Системы: оружие в руке, щит, броня, украшения (Curios). */
    public static void forEachWorn(Player p, java.util.function.Consumer<GearData> f) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            GearData d = p.getItemBySlot(slot).get(ModComponents.GEAR.get());
            if (d != null && d.kind().slot == slot) f.accept(d);
        }
        try {
            var inv = CuriosApi.getCuriosInventory(p);
            if (inv.isPresent()) {
                var h = inv.get().getEquippedCurios();
                for (int i = 0; i < h.getSlots(); i++) {
                    GearData d = h.getStackInSlot(i).get(ModComponents.GEAR.get());
                    if (d != null && d.kind().isAccessory()) f.accept(d);
                }
            }
        } catch (RuntimeException ignored) {
            // Curios ещё не готов (самый первый тик входа) — без украшений.
        }
    }

    public static Totals collect(Player p) {
        double[] st = new double[Stat.ALL.length];
        Map<Derived, Double> der = new EnumMap<>(Derived.class);
        double armor = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack s = p.getItemBySlot(slot);
            GearData d = s.get(ModComponents.GEAR.get());
            if (d == null || d.kind().slot != slot) continue; // меч в шлеме не считается
            armor += add(d, st, der);
        }
        try {
            var inv = CuriosApi.getCuriosInventory(p);
            if (inv.isPresent()) {
                var h = inv.get().getEquippedCurios();
                for (int i = 0; i < h.getSlots(); i++) {
                    GearData d = h.getStackInSlot(i).get(ModComponents.GEAR.get());
                    if (d != null && d.kind().isAccessory()) armor += add(d, st, der);
                }
            }
        } catch (RuntimeException ignored) {
            // Curios ещё не готов (самый первый тик входа) — без украшений.
        }
        return new Totals(new StatSet(st[0], st[1], st[2], st[3], st[4]), der, armor);
    }

    private static double add(GearData d, double[] st, Map<Derived, Double> der) {
        for (Stat s : Stat.ALL) st[s.ordinal()] += d.stat(s);
        for (GearData.Affix a : d.affixes()) der.merge(a.type().derived, a.value(), Double::sum);
        return d.kind().isArmor() ? d.base() : 0;
    }
}
