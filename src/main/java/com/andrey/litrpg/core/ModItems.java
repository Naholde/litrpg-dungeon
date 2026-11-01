package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.item.StaffItem;
import com.andrey.litrpg.gear.item.CoinItem;
import com.andrey.litrpg.gear.item.ElixirItem;
import com.andrey.litrpg.gear.item.ScrollItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Предметы мода. */
public final class ModItems {
    private static final DeferredRegister.Items REG = DeferredRegister.createItems(LitRPG.MOD_ID);

    /** Посох ученика: стартовое оружие поддержки. */
    public static final DeferredItem<StaffItem> APPRENTICE_STAFF = REG.register("apprentice_staff",
            () -> new StaffItem(new Item.Properties(), 5, 16, 0x7FD8FF));

    /** Посох «Системы»: урон берёт из вещи (уровень, редкость). */
    public static final DeferredItem<StaffItem> STAFF = REG.register("staff",
            () -> new StaffItem(new Item.Properties(), 5, 16, 0xB46BFF));

    public static final DeferredItem<Item> RING = REG.register("ring", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> AMULET = REG.register("amulet", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<Item> CHARM = REG.register("charm", () -> new Item(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<CoinItem> COPPER_COIN = REG.register("copper_coin",
            () -> new CoinItem(new Item.Properties(), 1));
    public static final DeferredItem<CoinItem> SILVER_COIN = REG.register("silver_coin",
            () -> new CoinItem(new Item.Properties(), 9));
    public static final DeferredItem<CoinItem> GOLD_COIN = REG.register("gold_coin",
            () -> new CoinItem(new Item.Properties(), 81));

    /** Ключ тайника: открывает запертый тайник (продаёт торговец, дают мини-хранители). */
    public static final DeferredItem<Item> CACHE_KEY = REG.register("cache_key", () -> new Item(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ElixirItem> ELIXIR = REG.register("elixir_of_life", () -> new ElixirItem(new Item.Properties()));
    public static final DeferredItem<ScrollItem> SCROLL_OBLIVION = REG.register("scroll_of_oblivion",
            () -> new ScrollItem(new Item.Properties()));

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
