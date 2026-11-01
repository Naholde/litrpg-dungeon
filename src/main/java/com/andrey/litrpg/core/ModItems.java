package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.item.StaffItem;
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

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
