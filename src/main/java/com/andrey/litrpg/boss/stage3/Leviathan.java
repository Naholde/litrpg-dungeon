package com.andrey.litrpg.boss.stage3;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Левиафан. Укус по танку, удар хвостом кольцом вокруг себя (ближе к голове — безопасно), нырок — уходит под воду
 * (неуязвим) и выныривает под одним из отряда. На 66% и 33% — прилив: половину арены заливает, стоять там нельзя.
 */
public class Leviathan extends BossEntity {
    private boolean submerged;

    public Leviathan(EntityType<? extends Leviathan> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.LEVIATHAN;
    }

    @Override
    public String modelId() {
        return "leviathan";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.66, 0.33};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Нырок", "dive", 1, false, b -> b.playersInArena().size() > 0, this::dive),
            new Ability("Удар хвостом", "tail", 2, false, b -> true, this::tail),
            new Ability("Укус", "bite", 3, false, b -> b.getTarget() != null, this::bite));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    @Override
    protected boolean dormant() {
        return submerged;
    }

    private void bite(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 60, p("bite_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void tail(Cast c) {
        Telegraph.donut(c, position(), 4, 10, p("tail_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
    }

    /** Нырок: скрылся (неуязвим), через пару секунд выныривает под игроком — круг видно заранее. */
    private void dive(Cast c) {
        c.at(c.ticks, () -> {
            submerged = true;
            setInvisible(true);
            ServerLevel sl = (ServerLevel) level();
            sl.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 1, getZ(), 60, 1.5, 1, 1.5, 0.3);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.HOSTILE, 2f, 0.5f);
            ServerPlayer victim = randomNonTarget();
            Vec3 at = victim != null ? victim.position() : home();
            Cast up = testCast(50);
            Telegraph.circle(up, at, 4, p("emerge_units"));
            tell(Component.literal("Левиафан ушёл под воду…").withStyle(ChatFormatting.DARK_AQUA));
            schedule(50, () -> {
                submerged = false;
                setInvisible(false);
                teleportTo(at.x, home().y, at.z);
                sl.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 1, at.z, 80, 2, 1.5, 2, 0.4);
                sl.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 3f, 0.4f);
                triggerAnim("main", "slam");
            });
        });
    }

    /** Прилив: половину арены (случайную) заливает на {@code tide_seconds} — там вода бьёт и замедляет. */
    private void tide() {
        double a = random.nextDouble() * Math.PI * 2;
        Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
        double r = arenaRadius();
        // половина арены — круг радиусом r со смещённым центром
        Vec3 at = home().add(dir.scale(r * 0.75));
        Fx.disc((ServerLevel) level(), at, (float) (r * 0.95), 60, Fx.ICE, true);
        tell(Component.literal("Прилив! Половину арены заливает — на сухую сторону!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        schedule(60, () -> addPool(at, r * 0.95, 0, r * 0.95, (int) Math.round(p("tide_seconds")), p("tide_units"), Fx.ICE,
                pl -> StatusService.apply(pl, Status.of(StatusType.SLOW, "leviathan_tide", getUUID(), 0.5, level().getGameTime(), 30))));
    }

    @Override
    protected void onPhase(int phase) {
        tide();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (submerged && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void resetFight() {
        submerged = false;
        setInvisible(false);
        super.resetFight();
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    protected void onRelocated() {
        Fx.ring((ServerLevel) level(), home(), (float) arenaRadius(), 20, Fx.ICE);
    }
}
