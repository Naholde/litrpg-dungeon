package com.andrey.litrpg.boss.stage5;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.MobRank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

/**
 * Ксалатар, Голод Бездны — последний хранитель. «Голод»: метит самого раненого — через пару секунд съедает тем сильнее,
 * чем меньше у него здоровья (лекарю — поднять его до полного). «Крах края» — край арены рушится, там остаётся пустота.
 * Взгляд бездны полосой. Осколки на 75%. На половине — Сердце Бездны; на четверти — «Конец всего»: волны по всем,
 * каждая сильнее прошлой.
 */
public class Xalathar extends BossEntity {
    private static final int VOID = 0x6A2AAA;
    private int endPulses;

    public Xalathar(EntityType<? extends Xalathar> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.XALATHAR;
    }

    @Override
    public String modelId() {
        return "xalathar";
    }

    @Override
    protected String secondArenaName() {
        return "Сердце Бездны";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.75, 0.25};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Разрывы пустоты", "rifts", 1, false, b -> true, this::rifts),
            new Ability("Тьма сгущается", "gloom", 0, false, b -> b.phase() >= 1, this::gloom),
            new Ability("Край рушится", "edgefall", 0, false, b -> true, this::edgefall),
            new Ability("Тысяча глаз", "eyes", 0, false, b -> b.phase() >= 1 && spawnedAlive().stream().noneMatch(e ->
                    e.getType() == com.andrey.litrpg.core.ModEntities.VOID_EYE.get()), this::eyes),
            new Ability("Пасть бездны", "maw", 1, false, b -> b.playersInArena().size() > 0, this::maw),
            new Ability("Луч Бездны", "ray", 0, false, b -> true, this::ray),
            new Ability("Столпы Бездны", "pillars", 0, false, b -> true, this::pillars),
            new Ability("Пустота съедает пол", "voidfall", 0, false, b -> true, this::voidfall),
            new Ability("Голод", "hunger", 0, false, b -> b.playersInArena().size() > 1, this::hunger),
            new Ability("Крах края", "edge", 1, false, b -> poolCount() < 8, this::edge),
            new Ability("Взгляд бездны", "gaze", 2, false, b -> true, this::gaze),
            new Ability("Удар пустоты", "smash", 3, false, b -> b.getTarget() != null, this::smash));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void smash(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 8, 90, p("smash_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
    }

    private void gaze(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        Vec3 dir = p.position().subtract(position()).multiply(1, 0, 1).normalize();
        Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 4)), 3f, p("gaze_units"));
        triggerAnim("main", "cast");
    }

    /** Голод: самый раненый (по доле здоровья) — чем меньше здоровья к удару, тем сильнее. */
    private void hunger(Cast c) {
        ServerPlayer v = playersInArena().stream().filter(p -> p != getTarget())
                .min(Comparator.comparingDouble(p -> p.getHealth() / p.getMaxHealth())).orElse(null);
        if (v == null) return;
        Fx.mark((ServerLevel) level(), v, "☠", c.ticks, VOID);
        Fx.beam((ServerLevel) level(), this, v, 0.3f, c.ticks, VOID);
        tell(Component.literal("Голод тянется к " + v.getGameProfile().getName() + " — вылечите его до полного!").withStyle(ChatFormatting.DARK_PURPLE));
        c.at(c.ticks, () -> {
            if (!v.isAlive() || v.isSpectator()) return;
            double missing = 1 - v.getHealth() / v.getMaxHealth();
            BossDamage.hit(this, v, p("hunger_units") * (0.4 + missing * 1.6));
            Fx.burst((ServerLevel) level(), v.position().add(0, 1, 0), 2, 12, VOID);
            level().playSound(null, v.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1f, 0.8f);
        });
    }

    /** Крах края: кольцо у края арены; после — там пустота до конца боя. */
    private void edge(Cast c) {
        float r = (float) arenaRadius();
        Telegraph.donut(c, home(), r - 4 - poolCount() * 0.0f, r + 2, p("edge_units"));
        tell(Component.literal("Край рушится! К центру!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        c.at(c.ticks, () -> {
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + random.nextDouble() * 0.3;
                addPool(home().add(Math.cos(a) * (r - 2), 0, Math.sin(a) * (r - 2)), 2.5, 0, 2.5, 600, p("void_units"), VOID, null);
            }
        });
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            for (int i = 0; i < 2 + partySize(); i++) {
                double a = Math.PI * 2 * i / (2 + partySize());
                summon(Bestiary.SHARD, home().add(Math.cos(a) * 8, 0.5, Math.sin(a) * 8), MobRank.NORMAL);
            }
            tell(Component.literal("Ксалатар рассыпается осколками!").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else if (phase == 2) {
            tell(Component.literal("«Конец всего» — бездна бьёт волнами, каждая сильнее!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            endPulse();
        }
    }

    private void endPulse() {
        if (!isAlive() || !engaged()) return;
        endPulses++;
        double units = p("end_units") * Math.pow(1.2, endPulses - 1);
        for (ServerPlayer p : playersInArena()) BossDamage.hit(this, p, units);
        Fx.ring((ServerLevel) level(), home(), (float) arenaRadius(), 20, VOID);
        level().playSound(null, blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2f, 0.5f);
        schedule((int) Math.round(p("end_period") * 20), this::endPulse);
    }

    @Override
    public void resetFight() {
        endPulses = 0;
        super.resetFight();
    }

    /** Пустота съедает пол */
    private void voidfall(Cast c) {
        Mechanics.crumble(c, (int) p("voidfall_waves"), (int) p("voidfall_safe"), p("voidfall_radius"), p("voidfall_units"), 0x3A2A9B);
    }



    /** Край рушится */
    private void edgefall(Cast c) {
        MapMechanics.shrink(c, (int) p("edgefall_seconds"), p("edgefall_units"), 0x3A2A9B);
    }

    /** Луч Бездны */
    private void ray(Cast c) {
        MapMechanics.sweep(c, (int) p("ray_seconds"), p("ray_units"), getRandom().nextBoolean(), 0x9B5CFF);
    }

    /** Столпы Бездны */
    private void pillars(Cast c) {
        MapMechanics.pillars(c, (int) p("pillars_count"), (int) p("pillars_height"), (int) p("pillars_seconds"), Blocks.OBSIDIAN.defaultBlockState());
    }


    /** Бездна: лиловые осколки, чёрный дым, тянущиеся вниз искры. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, VOID);
        Fx.vortex(sl, at, radius * 0.6f, 2f, 12, 0x2A0A44);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.6, at.z,
                Math.round(14 + radius * 6), radius * 0.5, 0.4, radius * 0.5, 0.05);
    }

    /**
     * Пасть бездны: под бойцом раскрывается пасть. Пока круг заполняется — уходите; в последний миг пасть
     * втягивает всех рядом и смыкается. Чем дальше отошли, тем вернее.
     */
    private void maw(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Vec3 at = p.position();
        tell(Component.literal("Пасть бездны под " + p.getGameProfile().getName() + " — бегите от неё!").withStyle(ChatFormatting.DARK_PURPLE));
        Telegraph.circle(c, at, 3.4f, p("maw_units"));
        Fx.vortex((ServerLevel) level(), at, 3f, 1.5f, c.ticks, VOID);
        c.at(c.ticks - 10, () -> {
            for (ServerPlayer o : playersInArena()) {
                Vec3 v = at.subtract(o.position()).multiply(1, 0, 1);
                double d = v.length();
                if (d > 8 || d < 0.3) continue;
                v = v.normalize().scale(0.55);
                o.setDeltaMovement(v.x, 0.1, v.z);
                o.hurtMarked = true;
            }
            level().playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1.6f, 0.6f);
        });
    }

    /**
     * Тысяча глаз (с 75%): по краю открываются три глаза. Раз в {@code eyes_every} секунд каждый смотрит лучом
     * на ближайшего — полоса видна заранее. Глаза нужно разбить; сами гаснут через {@code eyes_seconds}.
     */
    private void eyes(Cast c) {
        triggerAnim("main", "cast");
        ServerLevel sl = (ServerLevel) level();
        c.at(c.ticks, () -> {
            tell(Component.literal("Открылись глаза Бездны — разбейте их!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            int life = (int) Math.round(p("eyes_seconds") * 20);
            int every = (int) Math.round(p("eyes_every") * 20);
            double a0 = random.nextDouble() * Math.PI * 2;
            for (int i = 0; i < 3; i++) {
                double a = a0 + Math.PI * 2 * i / 3;
                Vec3 at = home().add(Math.cos(a) * (arenaRadius() - 2), 0, Math.sin(a) * (arenaRadius() - 2));
                var eye = spawnNode(com.andrey.litrpg.core.ModEntities.VOID_EYE.get(), at, p("eyes_hp_units"), "Глаз Бездны");
                if (eye == null) continue;
                for (int t = every; t < life; t += every) {
                    detached(t, () -> {
                        if (!eye.isAlive()) return;
                        ServerPlayer near = playersInArena().stream()
                                .min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(eye))).orElse(null);
                        if (near == null) return;
                        Vec3 from = eye.position();
                        Vec3 dir = near.position().subtract(from).multiply(1, 0, 1).normalize();
                        Telegraph.line(testCast(20), from, from.add(dir.scale(arenaRadius() * 1.6)), 1.6f, p("eyes_units"));
                    });
                }
                detached(life, () -> {
                    if (eye.isAlive()) eye.discard();
                });
                Fx.pillar(sl, at, 0.8f, 4, 20, VOID);
            }
        });
    }

    /** Разрывы пустоты: четыре разрыва хордами через арену на {rifts_seconds} — их не перешагнуть, только обойти. */
    private void rifts(Cast c) {
        triggerAnim("main", "cast");
        for (int i = 0; i < 4; i++) {
            double a = random.nextDouble() * 360, b = a + 100 + random.nextDouble() * 160;
            Vec3 p1 = home().add(dir(a).scale(arenaRadius())), p2 = home().add(dir(b).scale(arenaRadius()));
            com.andrey.litrpg.boss.Ground.line(c, p1, p2, 2f, (int) p("rifts_seconds"), p("rifts_units"), VOID, null);
        }
    }

    /** Тьма сгущается (с 75%): {gloom_seconds} секунд светло только рядом с Ксалатаром — все к нему, в его удары. */
    private void gloom(Cast c) {
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.outside(c, position(), 7, (int) p("gloom_seconds"), p("gloom_units"), 0x2A0A44, null);
        tell(Component.literal("Тьма сгущается — свет только у Ксалатара!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }
}
