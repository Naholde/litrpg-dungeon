package com.andrey.litrpg.skill.prop;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.mob.DungeonMob;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * Призванный помощник: ходит за хозяином, сам бросается на врагов, живёт отведённое время и ломается.
 * Сила и здоровье считаются в момент призыва — от характеристик хозяина.
 */
public class SummonEntity extends net.minecraft.world.entity.PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private UUID owner;
    private long until;

    public SummonEntity(EntityType<? extends SummonEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.ATTACK_DAMAGE, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 24);
    }

    /** Призвать помощника: {@code damage} и {@code hp} уже посчитаны навыком. */
    public static SummonEntity spawn(ServerLevel level, EntityType<? extends SummonEntity> type, Vec3 at,
                                     ServerPlayer owner, double hp, double damage, int durationTicks) {
        SummonEntity s = type.create(level);
        if (s == null) throw new IllegalStateException("не создался помощник " + type);
        s.moveTo(at.x, at.y, at.z, owner.getYRot(), 0);
        s.owner = owner.getUUID();
        s.until = level.getGameTime() + durationTicks;
        var mh = s.getAttribute(Attributes.MAX_HEALTH);
        if (mh != null) mh.setBaseValue(Math.max(1, hp));
        s.setHealth((float) Math.max(1, hp));
        var ad = s.getAttribute(Attributes.ATTACK_DAMAGE);
        if (ad != null) ad.setBaseValue(Math.max(1, damage));
        level.addFreshEntity(s);
        Fx.pillar(level, at, 0.6f, 2.5f, 12, Fx.SHADOW);
        level.playSound(null, s.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 0.9f, 0.8f);
        return s;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(2, new FollowOwnerGoal());
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Mob.class, 10, true, false,
                e -> e instanceof DungeonMob || e instanceof BossEntity));
    }

    public ServerPlayer ownerPlayer() {
        if (owner == null || !(level() instanceof ServerLevel sl) || sl.getServer() == null) return null;
        return sl.getServer().getPlayerList().getPlayer(owner);
    }

    /** Приказ: бросить всё и бить эту цель. */
    public void order(LivingEntity target) {
        setTarget(target);
        setAggressive(true);
    }

    public String kindId() {
        return BuiltInRegistries.ENTITY_TYPE.getKey(getType()).getPath();
    }

    /**
     * Удар помощника — с движением замаха. Голем бьёт двумя кулаками в землю: цель получает полный удар,
     * а враги вокруг неё в 2,5 блока — половину, и по полу расходится волна.
     */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        triggerAnim("main", "attack");
        boolean hit = super.doHurtTarget(target);
        if (hit && kindId().equals(SummonKind.GOLEM.id()) && level() instanceof ServerLevel sl) {
            float splash = (float) (getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5);
            for (Mob m : sl.getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(2.5),
                    m -> m != target && m.isAlive() && (m instanceof DungeonMob || m instanceof BossEntity))) {
                m.hurt(damageSources().mobAttack(this), splash);
            }
            Fx.wave(sl, target.position(), 2.5f, 0.8f, 10, 0xE8E8D8);
            sl.playSound(null, target.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1f, 0.6f);
        }
        return hit;
    }

    /** Союзники помощника не бьют: ни игроки, ни другие помощники. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player || source.getEntity() instanceof SummonEntity) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel sl && sl.getGameTime() >= until) {
            Fx.burst(sl, position().add(0, 0.8, 0), 1.0f, 8, Fx.SHADOW);
            discard();
        }
    }

    /** Лопнуть помощника (жертва, разрыв душ): урон по площади задаёт сам навык. */
    public void burst() {
        if (level() instanceof ServerLevel sl) {
            Fx.burst(sl, position().add(0, 0.8, 0), 2.0f, 10, Fx.SHADOW);
            sl.playSound(null, blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.7f, 1.4f);
        }
        discard();
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        discard();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            String anim = state.isMoving() ? "walk" : "idle";
            return state.setAndContinue(RawAnimation.begin().thenLoop("animation." + kindId() + "." + anim));
        }).triggerableAnim("attack", RawAnimation.begin().thenPlay("animation." + kindId() + ".attack")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Идти за хозяином, когда рядом нет врагов. */
    private final class FollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        @Override
        public boolean canUse() {
            ServerPlayer p = ownerPlayer();
            return p != null && getTarget() == null && distanceTo(p) > 6;
        }

        @Override
        public boolean canContinueToUse() {
            ServerPlayer p = ownerPlayer();
            return p != null && getTarget() == null && distanceTo(p) > 3;
        }

        @Override
        public void tick() {
            ServerPlayer p = ownerPlayer();
            if (p != null) getNavigation().moveTo(p, 1.1);
        }
    }
}
