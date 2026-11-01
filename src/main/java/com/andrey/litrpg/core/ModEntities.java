package com.andrey.litrpg.core;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.TrialWarden;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.shop.TraderEntity;
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
    /** Хранители и объекты боя — им атрибуты и отрисовка выдаются списком. */
    public static final java.util.List<DeferredHolder<EntityType<?>, ? extends EntityType<? extends BossEntity>>> BOSSES = new java.util.ArrayList<>();
    public static final java.util.List<DeferredHolder<EntityType<?>, EntityType<PowerNode>>> NODES = new java.util.ArrayList<>();

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

    public static final DeferredHolder<EntityType<?>, EntityType<TraderEntity>> TRADER = REG.register("trader",
            () -> EntityType.Builder.<TraderEntity>of(TraderEntity::new, MobCategory.MISC)
                    .sized(0.7f, 2.0f).clientTrackingRange(10).build("trader"));

    // ---------------------------------------------------------------- хранители этапа 1
    public static final DeferredHolder<EntityType<?>, EntityType<com.andrey.litrpg.boss.stage1.Gatekeeper>> GATEKEEPER =
            boss("gatekeeper", com.andrey.litrpg.boss.stage1.Gatekeeper::new, 1.4f, 4.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<com.andrey.litrpg.boss.stage1.Morra>> MORRA =
            boss("morra", com.andrey.litrpg.boss.stage1.Morra::new, 0.9f, 2.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<com.andrey.litrpg.boss.stage1.Vessa>> VESSA =
            boss("vessa", com.andrey.litrpg.boss.stage1.Vessa::new, 0.9f, 2.7f);
    public static final DeferredHolder<EntityType<?>, EntityType<com.andrey.litrpg.boss.stage1.LichKing>> LICH =
            boss("lich", com.andrey.litrpg.boss.stage1.LichKing::new, 1.2f, 3.9f);

    // ---------------------------------------------------------------- объекты боя (ломаются руками)
    public static final DeferredHolder<EntityType<?>, EntityType<PowerNode>> ICE_TOMB = node("ice_tomb", 1.2f, 2.4f);
    public static final DeferredHolder<EntityType<?>, EntityType<PowerNode>> PHYLACTERY = node("phylactery", 0.8f, 1.6f);

    private static <T extends BossEntity> DeferredHolder<EntityType<?>, EntityType<T>> boss(String id, EntityType.EntityFactory<T> f,
                                                                                         float w, float h) {
        DeferredHolder<EntityType<?>, EntityType<T>> holder = REG.register(id, () -> EntityType.Builder.of(f, MobCategory.MONSTER)
                .sized(w, h).clientTrackingRange(10).fireImmune().build(id));
        BOSSES.add(holder);
        return holder;
    }

    private static DeferredHolder<EntityType<?>, EntityType<PowerNode>> node(String id, float w, float h) {
        DeferredHolder<EntityType<?>, EntityType<PowerNode>> holder = REG.register(id, () -> EntityType.Builder.<PowerNode>of(PowerNode::new,
                MobCategory.MISC).sized(w, h).clientTrackingRange(10).fireImmune().build(id));
        NODES.add(holder);
        return holder;
    }

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        REG.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> {
            e.put(TRAINING_DUMMY.get(), TrainingDummy.attributes().build());
            e.put(TRIAL_WARDEN.get(), BossEntity.bossAttributes().build());
            e.put(POWER_NODE.get(), PowerNode.attributes().build());
            for (var b : BOSSES) e.put(b.get(), BossEntity.bossAttributes().build());
            for (var n : NODES) e.put(n.get(), PowerNode.attributes().build());
            e.put(TRADER.get(), TraderEntity.attributes().build());
        });
    }
}
