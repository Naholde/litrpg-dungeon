package com.andrey.litrpg.combat.entity;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.CombatBalance;
import com.andrey.litrpg.player.Derived;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Боевой манекен для проверки: держит любой урон, показывает урон в секунду над головой и восстанавливается,
 * если его не бить. «Агрессивный» манекен бьёт свою цель по таблице угрозы — на нём проверяют агро, провокацию
 * и лечение без настоящих монстров.
 */
public class TrainingDummy extends Mob {
    private static final double HEALTH = 1_000_000;
    private static final int DPS_WINDOW = 100;

    private final Deque<long[]> hits = new ArrayDeque<>();
    private long lastHit = -1000;
    private boolean aggressive;

    public TrainingDummy(EntityType<? extends TrainingDummy> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void setAggressive(boolean v) {
        aggressive = v;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide) lastHit = level().getGameTime();
        return r;
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount);
        if (!level().isClientSide) hits.addLast(new long[]{level().getGameTime(), Float.floatToIntBits(amount)});
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        long now = level().getGameTime();
        CombatBalance.Dummy cfg = Balance.get(Balance.COMBAT).dummy();
        while (!hits.isEmpty() && now - hits.peekFirst()[0] > DPS_WINDOW) hits.removeFirst();
        if (now - lastHit > cfg.resetSeconds() * 20 && getHealth() < getMaxHealth()) {
            setHealth(getMaxHealth());
            hits.clear();
            if (hasData(ModAttachments.THREAT)) ThreatService.table(this).clear();
        }
        if (aggressive && getTarget() instanceof ServerPlayer p && now % Math.max(1, Math.round(cfg.hitIntervalSeconds() * 20)) == 0
                && p.distanceTo(this) <= cfg.hitRange()) {
            getLookControl().setLookAt(p);
            swing(InteractionHand.MAIN_HAND);
            p.hurt(damageSources().mobAttack(this), (float) (p.getMaxHealth() * cfg.hitShareOfMaxHealth()));
            level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 0.7f, 0.8f);
        }
        if (tickCount % 10 == 0) updateName(now);
        if (!(getTarget() instanceof ServerPlayer)) {
            var near = level().getNearestPlayer(this, 10);
            if (near != null) getLookControl().setLookAt(near, 30, 30);
        }
    }

    private void updateName(long now) {
        double sum = 0;
        long first = now;
        for (long[] h : hits) {
            sum += Float.intBitsToFloat((int) h[1]);
            first = Math.min(first, h[0]);
        }
        double seconds = Math.max(1, (now - first) / 20.0);
        String dps = hits.isEmpty() ? "—" : Derived.MAX_HEALTH.format(sum / seconds);
        var name = Component.literal(aggressive ? "Злой манекен" : "Манекен").withStyle(aggressive ? ChatFormatting.RED : ChatFormatting.GOLD)
                .append(Component.literal(" · урон/с: " + dps).withStyle(ChatFormatting.WHITE));
        setCustomName(name);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("aggressive", aggressive);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        aggressive = tag.getBoolean("aggressive");
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
