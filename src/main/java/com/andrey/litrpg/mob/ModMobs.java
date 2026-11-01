package com.andrey.litrpg.mob;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Регистрация видов бестиария: по типу сущности на вид, у всех — общий класс {@link DungeonMob}. */
public final class ModMobs {
    private static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(Registries.ENTITY_TYPE, LitRPG.MOD_ID);
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<DungeonMob>>> TYPES = new LinkedHashMap<>();
    private static final Map<EntityType<?>, Bestiary.Species> BY_TYPE = new HashMap<>();

    static {
        for (Bestiary.Species s : Bestiary.all()) {
            TYPES.put(s.id(), REG.register(s.id(), () -> EntityType.Builder.<DungeonMob>of(DungeonMob::new, MobCategory.MONSTER)
                    .sized(s.width(), s.height()).clientTrackingRange(10).build(s.id())));
        }
    }

    private ModMobs() {
    }

    public static Bestiary.Species speciesOf(EntityType<?> type) {
        Bestiary.Species s = BY_TYPE.get(type);
        if (s != null) return s;
        var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type);
        s = Bestiary.get(key.getPath());
        BY_TYPE.put(type, s);
        return s;
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> TYPES.values().forEach(h -> e.put(h.get(), DungeonMob.attributes().build())));
    }
}
