package com.andrey.litrpg.combat.entity;

import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.core.ModEntities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

/** Магический снаряд посоха: летит прямо, без гравитации, светится и оставляет след. */
public class MagicBolt extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(MagicBolt.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 30;

    private double damage;
    /** Особенность стрелка: что ещё делает попадание (паутина, кислота). Не сохраняется. */
    public java.util.function.Consumer<LivingEntity> onHit;

    public MagicBolt(EntityType<? extends MagicBolt> type, Level level) {
        super(type, level);
    }

    public MagicBolt(Level level, LivingEntity owner, double damage, int color) {
        super(ModEntities.MAGIC_BOLT.get(), owner, level);
        this.damage = damage;
        this.entityData.set(COLOR, color);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(COLOR, 0x7FD8FF);
    }

    public int color() {
        return entityData.get(COLOR);
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            int c = color();
            var dust = new DustParticleOptions(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f,
                    (c & 255) / 255f), 0.8f);
            for (int i = 0; i < 2; i++) {
                level().addParticle(dust, getX() + random.nextGaussian() * 0.06, getY() + random.nextGaussian() * 0.06,
                        getZ() + random.nextGaussian() * 0.06, 0, 0, 0);
            }
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > LIFETIME) {
            burst();
            discard();
        }
    }

    /** Снаряд монстра не задевает других монстров, снаряд игрока — других игроков (урон по своим — только ближним боем). */
    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity e) {
        if (!super.canHitEntity(e)) return false;
        boolean ownerPlayer = getOwner() instanceof net.minecraft.world.entity.player.Player;
        boolean targetPlayer = e instanceof net.minecraft.world.entity.player.Player;
        return ownerPlayer != targetPlayer;
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (level().isClientSide) return;
        if (hit.getEntity() instanceof LivingEntity target && getOwner() instanceof LivingEntity owner && target != owner) {
            DamageService.deal(owner, this, target, damage, WeaponKind.MAGIC);
            if (onHit != null && target.isAlive()) onHit.accept(target);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            burst();
            discard();
        }
    }

    private void burst() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0.06);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putDouble("damage", damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        damage = tag.getDouble("damage");
    }
}
