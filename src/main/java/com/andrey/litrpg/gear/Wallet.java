package com.andrey.litrpg.gear;

import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.gear.item.CoinItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Кошелёк — монеты в инвентаре. Счёт в медных: серебряная = 9, золотая = 81. Сдача выдаётся сама. */
public final class Wallet {
    private Wallet() {
    }

    public static long total(Player p) {
        long sum = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof CoinItem c) sum += (long) c.value * s.getCount();
        }
        return sum;
    }

    /** Выдать монеты крупными: золото, серебро, медь. */
    public static void give(Player p, long copper) {
        if (copper <= 0) return;
        long gold = copper / 81, silver = copper % 81 / 9, cu = copper % 9;
        add(p, new ItemStack(ModItems.GOLD_COIN.get()), gold);
        add(p, new ItemStack(ModItems.SILVER_COIN.get()), silver);
        add(p, new ItemStack(ModItems.COPPER_COIN.get()), cu);
    }

    private static void add(Player p, ItemStack proto, long count) {
        while (count > 0) {
            int n = (int) Math.min(count, proto.getMaxStackSize());
            ItemStack s = proto.copyWithCount(n);
            if (!p.getInventory().add(s)) p.drop(s, false);
            count -= n;
        }
    }

    /** Списать {@code copper}: забираются все монеты, остаток выдаётся сдачей. Не хватает — ничего не трогаем. */
    public static boolean take(Player p, long copper) {
        long have = total(p);
        if (copper <= 0) return true;
        if (have < copper) return false;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof CoinItem) inv.setItem(i, ItemStack.EMPTY);
        }
        give(p, have - copper);
        return true;
    }

    /** «2 зол. 3 сер. 4 мед.» */
    public static String format(long copper) {
        long gold = copper / 81, silver = copper % 81 / 9, cu = copper % 9;
        StringBuilder sb = new StringBuilder();
        if (gold > 0) sb.append(gold).append(" зол. ");
        if (silver > 0) sb.append(silver).append(" сер. ");
        if (cu > 0 || sb.isEmpty()) sb.append(cu).append(" мед.");
        return sb.toString().trim();
    }
}
