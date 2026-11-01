package com.andrey.litrpg.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Торговец привала: не двигается, неуязвим, смотрит на ближайшего игрока. ПКМ — лавка,
 * Shift+ПКМ с вещью в руке — продать её. {@code stage} — этап (цены и уровень товаров).
 */
public class TraderEntity extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stage = 1;
    private int goodsLevel = 5;

    public TraderEntity(EntityType<? extends TraderEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.MOVEMENT_SPEED, 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1);
    }

    public int stage() {
        return stage;
    }

    /** Уровень товаров (обычно начало этапа + несколько уровней). */
    public int goodsLevel() {
        return goodsLevel;
    }

    public void setStage(int stage, int level) {
        this.stage = stage;
        this.goodsLevel = level;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) ShopService.sell(sp, this);
            else ShopService.open(sp, this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount % 5 == 0) {
            var p = level().getNearestPlayer(this, 8);
            if (p != null) getLookControl().setLookAt(p, 20, 20);
        }
        // Неподвижный торговец поворачивается к собеседнику целиком, а не только головой.
        setYBodyRot(getYHeadRot());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("stage", stage);
        tag.putInt("level", goodsLevel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        stage = Math.max(1, tag.getInt("stage"));
        goodsLevel = Math.max(1, tag.getInt("level"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.trader.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
