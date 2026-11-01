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
}
