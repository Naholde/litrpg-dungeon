package com.andrey.litrpg.boss.stage4;

import com.andrey.litrpg.boss.BossDamage;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

/**
 * Колесница Пламени. «Разгон»: полоса через игрока до края арены — колесница проносится по ней, сбивает и оставляет
 * огненный след. Каждый круг быстрее (подготовка короче). После четырёх кругов — «Пит-стоп»: колесница встаёт
 * и получает больше урона — время бить всем. «Огненный круг» — кольцо вокруг неё.
 */
public class Chariot extends BossEntity {
    private int laps;
    private boolean stopped;

    public Chariot(EntityType<? extends Chariot> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.CHARIOT;
    }

    @Override
    public String modelId() {
        return "chariot";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[0];
    }

    @Override
    protected void onPhase(int phase) {
    }

    private final List<Ability> abilities = List.of(
            new Ability("Огненный след", "firesweep", 0, false, b -> true, this::firesweep),
            new Ability("Круги колёс", "wheels", 0, false, b -> true, this::wheels),
            new Ability("Затоптать", "trample", 1, false, b -> true, this::trample),
            new Ability("Разгон", "charge", 1, false, b -> !stopped, this::charge),
            new Ability("Огненный круг", "ring", 2, false, b -> !stopped, this::ring));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    @Override
    protected boolean dormant() {
        return stopped;
    }

    @Override
    public double p(String key) {
        // Каждый круг — короче подготовка разгона (но не меньше секунды).
        if (key.equals("charge_cast")) return Math.max(1.0, super.p(key) - 0.25 * (laps % 4));
        return super.p(key);
    }

    private void ring(Cast c) {
        Telegraph.donut(c, position(), 3.5f, 9, p("ring_units"));
        triggerAnim("main", "slam");
    }

    /** Разгон через игрока до края арены: проносится, сбивает, оставляет огонь. */
    private void charge(Cast c) {
        ServerPlayer p = randomNonTarget();
        Vec3 aim = p != null ? p.position() : c.target.position();
        Vec3 dir = aim.subtract(position()).multiply(1, 0, 1).normalize();
        Vec3 from = position();
        // до края арены по направлению
        Vec3 rel = from.subtract(home()).multiply(1, 0, 1);
        double b = rel.dot(dir), cc = rel.lengthSqr() - arenaRadius() * arenaRadius() * 0.85;
        double len = -b + Math.sqrt(Math.max(0, b * b - cc));
        Vec3 to = from.add(dir.scale(Math.max(6, len)));
        Fx.line((ServerLevel) level(), from, to, 3f, c.ticks, Fx.DANGER, true);
        triggerAnim("main", "cast");
        tell(Component.literal("Колесница разгоняется! Круг " + (laps % 4 + 1) + " из 4.").withStyle(ChatFormatting.GOLD));
        c.at(c.ticks, () -> dash(from, to));
    }

    private void dash(Vec3 from, Vec3 to) {
        Set<UUID> hit = new HashSet<>();
        int steps = 10;
        ServerLevel sl = (ServerLevel) level();
        sl.playSound(null, blockPosition(), SoundEvents.HORSE_GALLOP, SoundSource.HOSTILE, 2f, 0.6f);
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Vec3 at = from.add(to.subtract(from).scale(t));
            schedule(i, () -> {
                teleportTo(at.x, at.y, at.z);
                sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 10, 0.8, 0.3, 0.8, 0.02);
                for (ServerPlayer pl : playersInArena()) {
                    if (pl.position().distanceTo(at) < 2.2 && hit.add(pl.getUUID())) {
                        BossDamage.hit(this, pl, p("charge_units"));
                        Vec3 side = new Vec3(-(to.z - from.z), 0, to.x - from.x).normalize().scale(1.4);
                        pl.push(side.x, 0.5, side.z);
                        pl.hurtMarked = true;
                    }
                }
            });
        }
        schedule(steps + 1, () -> {
            Vec3 d = to.subtract(from);
            for (double s = 2; s < d.length(); s += 3) addPool(from.add(d.normalize().scale(s)), 1.5, 0, 1.5,
                    (int) Math.round(p("trail_seconds")), p("trail_units"), Fx.FIRE, null);
            laps++;
            if (laps % 4 == 0) pitStop();
        });
    }

    /** Пит-стоп: стоит, уязвима — все бьют. */
    private void pitStop() {
        stopped = true;
        int dur = (int) Math.round(p("pit_seconds") * 20);
        StatusService.apply(this, Status.of(StatusType.DAMAGE_TAKEN, "chariot_pit", getUUID(), p("pit_damage"), level().getGameTime(), dur));
        Fx.pillar((ServerLevel) level(), position(), 1.6f, 4, dur, Fx.HOLY);
        tell(Component.literal("Пит-стоп! Колесница встала и уязвима — бейте!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        schedule(dur, () -> {
            stopped = false;
            tell(Component.literal("Колесница снова в пути!").withStyle(ChatFormatting.GOLD));
        });
    }

    @Override
    public void resetFight() {
        laps = 0;
        stopped = false;
        super.resetFight();
    }

    /** Круги колёс */
    private void wheels(Cast c) {
        Mechanics.rings(c, (int) p("wheels_waves"), p("wheels_units"), p("wheels_gap"), 0xFFB04A);
    }

    /** Затоптать */
    private void trample(Cast c) {
        Mechanics.charge(c, randomNonTarget() != null ? randomNonTarget()
                : (getTarget() instanceof ServerPlayer t ? t : null), p("trample_units"), p("trample_push"), 0xFF7A2E);
    }


    /** Огненный след */
    private void firesweep(Cast c) {
        MapMechanics.sweep(c, (int) p("firesweep_seconds"), p("firesweep_units"), getRandom().nextBoolean(), 0xFF9A4A);
    }

}
