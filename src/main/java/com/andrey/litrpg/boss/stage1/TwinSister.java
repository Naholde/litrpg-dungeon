package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Сестра-некромантка. Общее у Морры и Вессы:
 * <ul>
 *   <li><b>Пала одна — вторая поднимет:</b> смертельный удар не убивает, сестра падает и через {@code revive_seconds}
 *   встаёт с половиной здоровья. Умирают обе, только если вторую добили, пока первая лежит.</li>
 *   <li><b>Связь сестёр:</b> луч между ними снижает урон по обеим; кто встал в луч — разрывает его (и получает урон).</li>
 * </ul>
 */
public abstract class TwinSister extends BossEntity {
    protected UUID sister;
    private boolean fallen;
    private long riseAt;
    private boolean finishing;

    protected TwinSister(EntityType<? extends TwinSister> type, Level level) {
        super(type, level);
    }

    public void setSister(TwinSister other) {
        sister = other.getUUID();
    }

    protected TwinSister sister() {
        if (sister == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getEntity(sister) instanceof TwinSister t && t.isAlive() ? t : null;
    }

    public boolean fallen() {
        return fallen;
    }

    @Override
    protected boolean dormant() {
        return fallen;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return false;
        if (fallen && !finishing) return false;
        TwinSister other = sister();
        if (!finishing && other != null && !other.fallen && amount >= getHealth() + getAbsorptionAmount()) {
            // Смертельный удар, а сестра жива — упасть, а не умереть.
            fall(other);
            return true;
        }
        if (!finishing && other != null && !other.fallen && linkIntact(other)) amount *= (float) (1 - linkCut());
        return super.hurt(source, amount);
    }

    private void fall(TwinSister other) {
        fallen = true;
        cancelCast();
        setHealth(1);
        riseAt = level().getGameTime() + Math.round(reviveSeconds() * 20);
        tell(Component.literal(type().name() + " падает — " + other.type().name() + " поднимет её через "
                + Math.round(reviveSeconds()) + " с! Добейте вторую сестру!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        Fx.pillar((ServerLevel) level(), position(), 1.2f, 4, (int) Math.round(reviveSeconds() * 20), Fx.SHADOW);
        level().playSound(null, blockPosition(), SoundEvents.WITCH_DEATH, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    @Override
    public void resetFight() {
        fallen = false;
        finishing = false;
        super.resetFight();
    }

    /** Вторая сестра погибла, пока эта лежала: умирают обе. */
    void finish() {
        finishing = true;
        fallen = false;
        hurt(damageSources().genericKill(), Float.MAX_VALUE);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level().isClientSide) return;
        TwinSister other = sister();
        if (other != null && other.fallen) other.finish();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) return;
        if (fallen) {
            getNavigation().stop();
            TwinSister other = sister();
            if (other == null) {
                finish();
            } else if (level().getGameTime() >= riseAt) {
                fallen = false;
                setHealth(getMaxHealth() * 0.5f);
                tell(Component.literal(other.type().name() + " поднимает сестру! " + type().name() + " снова в бою.")
                        .withStyle(ChatFormatting.DARK_RED));
                Fx.burst((ServerLevel) level(), position().add(0, 1, 0), 2.5f, 14, Fx.SHADOW);
            }
            return;
        }
        if (engaged() && tickCount % 20 == 0 && this instanceof Morra) tickLink();
    }

    /** Раз в секунду (ведёт Морра): луч между сёстрами; кто стоит в нём — получает урон, а связь рвётся. */
    private void tickLink() {
        TwinSister other = sister();
        if (other == null || other.fallen || !(level() instanceof ServerLevel sl)) return;
        boolean intact = linkIntact(other);
        Fx.beam(sl, this, other, 0.35f, 21, intact ? 0xB04AFF : 0xFF4A4A);
        for (ServerPlayer p : cutters(other)) BossDamage.hit(this, p, p("link_units"));
    }

    protected boolean linkIntact(TwinSister other) {
        return cutters(other).isEmpty();
    }

    private java.util.List<ServerPlayer> cutters(TwinSister other) {
        Vec3 a = position(), b = other.position();
        Vec3 ab = b.subtract(a).multiply(1, 0, 1);
        double len = ab.length();
        if (len < 1) return java.util.List.of();
        Vec3 n = ab.normalize();
        return playersInArena().stream().filter(p -> {
            Vec3 ap = p.position().subtract(a).multiply(1, 0, 1);
            double along = ap.dot(n);
            return along > 0.8 && along < len - 0.8 && ap.subtract(n.scale(along)).length() < 1.1;
        }).toList();
    }

    protected abstract double reviveSeconds();

    protected abstract double linkCut();

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (sister != null) tag.putUUID("sister", sister);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("sister")) sister = tag.getUUID("sister");
    }
}
