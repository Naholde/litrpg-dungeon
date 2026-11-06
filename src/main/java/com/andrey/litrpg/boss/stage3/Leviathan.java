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
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

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
            new Ability("Чернильное пятно", "ink", 1, false, b -> b.playersInArena().size() > 0, this::ink),
            new Ability("Отбойное течение", "undertow", 1, false, b -> b.phase() >= 1, this::undertow),
            new Ability("Стена воды", "wall", 0, false, b -> true, this::wall),
            new Ability("Рёв глубин", "roar", 0, false, b -> b.phase() >= 1, this::roar),
            new Ability("Водяной смерч", "spout", 1, false, b -> true, this::spout),
            new Ability("Затопление зала", "deluge", 0, false, b -> true, this::deluge),
            new Ability("Таран", "ram", 0, false, b -> true, this::ram),
            new Ability("Затопление", "flood", 1, false, b -> true, this::flood),
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

    /** Таран */
    private void ram(Cast c) {
        Mechanics.charge(c, randomNonTarget() != null ? randomNonTarget()
                : (getTarget() instanceof ServerPlayer t ? t : null), p("ram_units"), p("ram_push"), 0x4A8ABF);
    }

    /** Затопление */
    private void flood(Cast c) {
        Mechanics.crumble(c, (int) p("flood_waves"), (int) p("flood_safe"), p("flood_radius"), p("flood_units"), 0x2A5A8A);
    }


    /** Стена воды */
    private void wall(Cast c) {
        MapMechanics.wall(c, (int) p("wall_height"), (int) p("wall_seconds"), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 0x4A8ABF);
    }

    /** Затопление зала */
    private void deluge(Cast c) {
        MapMechanics.flood(c, (int) p("deluge_seconds"), p("deluge_units"), 0x2A5A8A);
    }


    /** Глубина: тёмная вода, пена, пузыри. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, 0x2FA3A0);
        Fx.shards(sl, at.add(0, 0.2, 0), radius, Math.max(4, Math.round(radius * 3)), 12, 0x1B5E66);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, at.x, at.y + 0.4, at.z,
                Math.round(14 + radius * 5), radius * 0.5, 0.3, radius * 0.5, 0.05);
    }

    /**
     * Водяной смерч: смерч бродит по арене {@code spout_seconds} секунд и затягивает тех, кто рядом;
     * внутри — урон каждую секунду. Обходить, не давать затянуть.
     */
    private void spout(Cast c) {
        triggerAnim("main", "cast");
        ServerLevel sl = (ServerLevel) level();
        Telegraph.note(c, "orbit", 2.2);
        Vec3[] at = {home().add((random.nextDouble() - 0.5) * arenaRadius(), 0, (random.nextDouble() - 0.5) * arenaRadius())};
        Vec3[] dir = {new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5).normalize()};
        Fx.vortex(sl, at[0], 2.2f, 5, c.ticks, 0x2FA3A0);
        int steps = (int) Math.round(p("spout_seconds") * 10);
        for (int t = 0; t < steps; t++) {
            final int tick = t;
            c.at(c.ticks + t * 2, () -> {
                if (tick % 15 == 0) dir[0] = dir[0].yRot((float) ((random.nextDouble() - 0.5) * 1.6));
                Vec3 next = at[0].add(dir[0].scale(0.32));
                if (next.distanceTo(home()) > arenaRadius() - 2) {
                    dir[0] = home().subtract(at[0]).multiply(1, 0, 1).normalize();
                    next = at[0].add(dir[0].scale(0.32));
                }
                at[0] = next;
                if (tick % 2 == 0) Fx.vortex(sl, at[0], 2.2f, 5, 5, 0x2FA3A0);
                for (ServerPlayer p : playersInArena()) {
                    Vec3 v = at[0].subtract(p.position()).multiply(1, 0, 1);
                    double d = v.length();
                    if (d < 4.5 && d > 0.3) {
                        v = v.normalize().scale(0.05);
                        p.push(v.x, 0, v.z);
                        p.hurtMarked = true;
                    }
                    if (d <= 2.2 && tick % 10 == 0) com.andrey.litrpg.boss.BossDamage.hit(this, p, p("spout_units"));
                }
            });
        }
    }

    /**
     * Рёв глубин (с 66%): круг 6 блоков вокруг Левиафана; кого рёв застал внутри — немота (нельзя применять
     * навыки), урон и отброс. Отойдите заранее.
     */
    private void roar(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Левиафан набирает воздух — отойдите, рёв лишает навыков!").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        int silence = (int) Math.round(p("roar_silence") * 20);
        Vec3 at = position();
        Telegraph.circle(c, at, 6, p("roar_units"), p -> {
            StatusService.apply(p, Status.of(StatusType.SILENCE, "leviathan_roar", getUUID(), 1, level().getGameTime(), silence));
            Vec3 v = p.position().subtract(at).multiply(1, 0, 1).normalize().scale(1.1);
            p.setDeltaMovement(v.x, 0.4, v.z);
            p.hurtMarked = true;
        });
        c.at(c.ticks, () -> {
            Fx.wave((ServerLevel) level(), at, 10, 2f, 16, 0x2FA3A0);
            level().playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2f, 0.5f);
        });
    }

    /** Чернильное пятно: облако чернил ползёт за дальним бойцом {ink_seconds} — в нём темно и больно. */
    private void ink(Cast c) {
        List<ServerPlayer> by = playersByDistance();
        if (by.isEmpty()) return;
        ServerPlayer far = by.get(by.size() - 1);
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.moving(c, randomSpot(0.6, 0.9), () -> far.isAlive() ? far.position() : null, 3f, 1.8, (int) p("ink_seconds"),
                p("ink_units"), 0x1B1B2A, pl -> pl.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.BLINDNESS, 40, 0, false, false)));
        tell(Component.literal("Чернила ползут за " + far.getGameProfile().getName() + " — не давай им догнать.").withStyle(ChatFormatting.DARK_AQUA));
    }

    /** Отбойное течение (с 66%): половину зала {undertow_seconds} несёт к краю — выходите на спокойную сторону. */
    private void undertow(Cast c) {
        triggerAnim("main", "cast");
        Vec3 d = dir(random.nextDouble() * 360);
        com.andrey.litrpg.boss.Ground.half(c, home(), d, (int) p("undertow_seconds"), p("undertow_units"), 0x2FA3A0, pl -> push(pl, d, 0.5));
    }
}
