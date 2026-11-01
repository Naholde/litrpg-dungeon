package com.andrey.litrpg.skill.prop;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/** Регистрация объектов классов ({@link ClassProp}) и призванных помощников ({@link SummonEntity}). */
public final class ModProps {
    private static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(Registries.ENTITY_TYPE, LitRPG.MOD_ID);

    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<ClassProp>>> PROPS = new LinkedHashMap<>();
    public static final Map<String, DeferredHolder<EntityType<?>, EntityType<SummonEntity>>> SUMMONS = new LinkedHashMap<>();

    static {
        for (PropKind k : PropKind.all()) {
            PROPS.put(k.id(), REG.register(k.id(), () -> EntityType.Builder.<ClassProp>of(ClassProp::new, MobCategory.MISC)
                    .sized(k.width(), k.height()).clientTrackingRange(10).fireImmune().build(k.id())));
        }
        for (SummonKind k : SummonKind.all()) {
            SUMMONS.put(k.id(), REG.register(k.id(), () -> EntityType.Builder.<SummonEntity>of(SummonEntity::new, MobCategory.MISC)
                    .sized(k.width(), k.height()).clientTrackingRange(10).build(k.id())));
        }
    }

    private ModProps() {
    }

    public static EntityType<ClassProp> type(PropKind k) {
        return PROPS.get(k.id()).get();
    }

    public static EntityType<SummonEntity> type(SummonKind k) {
        return SUMMONS.get(k.id()).get();
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            PROPS.values().forEach(h -> e.put(h.get(), ClassProp.attributes().build()));
            SUMMONS.values().forEach(h -> e.put(h.get(), SummonEntity.attributes().build()));
        });
    }
}
