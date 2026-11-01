package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.TrialWarden;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.combat.entity.TrainingDummy;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Сущности мода. */
public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> REG = DeferredRegister.create(Registries.ENTITY_TYPE, LitRPG.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MagicBolt>> MAGIC_BOLT = REG.register("magic_bolt",
            () -> EntityType.Builder.<MagicBolt>of(MagicBolt::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f).clientTrackingRange(6).updateInterval(2).build("magic_bolt"));

    public static final DeferredHolder<EntityType<?>, EntityType<TrainingDummy>> TRAINING_DUMMY = REG.register("training_dummy",
            () -> EntityType.Builder.<TrainingDummy>of(TrainingDummy::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(8).build("training_dummy"));

    public static final DeferredHolder<EntityType<?>, EntityType<TrialWarden>> TRIAL_WARDEN = REG.register("trial_warden",
            () -> EntityType.Builder.<TrialWarden>of(TrialWarden::new, MobCategory.MONSTER)
                    .sized(1.6f, 4.2f).clientTrackingRange(10).fireImmune().build("trial_warden"));

    public static final DeferredHolder<EntityType<?>, EntityType<PowerNode>> POWER_NODE = REG.register("power_node",
            () -> EntityType.Builder.<PowerNode>of(PowerNode::new, MobCategory.MISC)
                    .sized(1.0f, 1.8f).clientTrackingRange(10).fireImmune().build("power_node"));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(TRAINING_DUMMY.get(), TrainingDummy.attributes().build());
            e.put(TRIAL_WARDEN.get(), BossEntity.bossAttributes().build());
            e.put(POWER_NODE.get(), PowerNode.attributes().build());
        });
    }
}
