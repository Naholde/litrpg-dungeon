package com.andrey.litrpg.boss;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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

/** Узел силы: пока стоит, хранитель под щитом. Не двигается, не бьёт; ломается только руками игроков. */
public class PowerNode extends Mob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int beamTimer;

    public PowerNode(EntityType<? extends PowerNode> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 100).add(Attributes.KNOCKBACK_RESISTANCE, 1)
                .add(Attributes.MOVEMENT_SPEED, 0);
    }

    public void setHp(double hp) {
        var a = getAttribute(Attributes.MAX_HEALTH);
        if (a != null) a.setBaseValue(hp);
        setHealth((float) hp);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(source.getEntity() instanceof Player)) return false;
        boolean r = super.hurt(source, amount);
        if (r && level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1f, 0.7f + random.nextFloat() * 0.3f);
        }
        return r;
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(0, 0, 0);
        if (level() instanceof ServerLevel sl && ++beamTimer % 20 == 0) {
            // Луч от узла к ближайшему хранителю — видно, что именно держит щит.
            var boss = sl.getEntitiesOfClass(BossEntity.class, getBoundingBox().inflate(40)).stream().findFirst().orElse(null);
            if (boss != null && boss.shielded()) Fx.beam(sl, this, boss, 0.2f, 21, Fx.ARCANE);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel sl) {
            Fx.burst(sl, position().add(0, 1, 0), 2.5f, 12, Fx.ARCANE);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2f, 0.8f);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 0,
                state -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.power_node.idle"))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
