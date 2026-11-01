package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.gear.GearData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Компоненты предметов мода. */
public final class ModComponents {
    private static final DeferredRegister<DataComponentType<?>> REG =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LitRPG.MOD_ID);

    /** «Вещь Системы»: уровень, редкость, характеристики, свойства. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GearData>> GEAR = REG.register("gear",
            () -> DataComponentType.<GearData>builder().persistent(GearData.CODEC).networkSynchronized(GearData.STREAM_CODEC).build());

    private ModComponents() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
    }
}
