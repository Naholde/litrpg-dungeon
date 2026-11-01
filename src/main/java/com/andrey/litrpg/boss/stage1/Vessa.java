package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.List;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Весса, младшая сестра: лечит раненую сестру (сбить!), проклинает (урон со временем — работа лекаря),
 * «Тёмная волна» вокруг себя — ближникам отойти.
 */
public class Vessa extends TwinSister {
    public Vessa(EntityType<? extends Vessa> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.VESSA;
    }

    @Override
    public String modelId() {
        return "vessa";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[0];
    }

    @Override
    protected void onPhase(int phase) {
    }

    /** Общие цифры сестёр — у Морры (bosses.json → morra). */
    private static double morra(String key) {
        return com.andrey.litrpg.core.balance.Balance.get(com.andrey.litrpg.core.balance.Balance.BOSSES)
                .value("morra", key, BossRegistry.MORRA.params().get(key));
    }

    @Override
    protected double reviveSeconds() {
        return morra("revive_seconds");
    }

    @Override
    protected double linkCut() {
        return morra("link_cut");
    }

    private final List<Ability> abilities = List.of(
            new Ability("Ползучая порча", "creep", 0, false, b -> true, this::creep),
            new Ability("Взгляд Вессы", "stare", 1, false, b -> true, this::stare),
            new Ability("Тёмное лечение", "mend", 0, true, b -> woundedSister() != null, this::mend),
            new Ability("Тёмная волна", "nova", 1, false, b -> !playersNear(6).isEmpty(), this::nova),
            new Ability("Проклятие", "curse", 2, false, b -> true, this::curse));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private TwinSister woundedSister() {
        TwinSister s = sister();
        TwinSister pick = getHealth() < getMaxHealth() * 0.8 ? this : null;
        if (s != null && !s.fallen() && s.getHealth() < s.getMaxHealth() * 0.8
                && (pick == null || s.getHealth() / s.getMaxHealth() < getHealth() / getMaxHealth())) pick = s;
        return pick;
    }

    private List<ServerPlayer> playersNear(double r) {
        return playersInArena().stream().filter(p -> p.distanceTo(this) < r).toList();
    }

    private void mend(Cast c) {
        TwinSister target = woundedSister();
        if (target == null) return;
        triggerAnim("main", "cast");
        Fx.beam((ServerLevel) level(), this, target, 0.3f, c.ticks, Fx.HEAL);
        c.at(c.ticks, () -> {
            if (!target.isAlive() || target.fallen()) return;
            target.heal((float) (target.getMaxHealth() * p("mend_share")));
            Fx.pillar((ServerLevel) level(), target.position(), 1.2f, 4, 14, Fx.HEAL);
            tell(Component.literal("Весса залечила " + (target == this ? "себя" : "сестру") + "!").withStyle(ChatFormatting.RED));
        });
    }

    private void nova(Cast c) {
        Telegraph.circle(c, position(), 6, p("nova_units"));
        triggerAnim("main", "cast");
    }

    private void curse(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Fx.beam((ServerLevel) level(), this, p, 0.15f, c.ticks, Fx.SHADOW);
        c.at(c.ticks, () -> {
            if (!p.isAlive()) return;
            double perSec = p("curse_units") * unit();
            StatusService.apply(p, Status.periodic(StatusType.DOT, "vessa_curse", getUUID(), perSec, level().getGameTime(),
                    (int) Math.round(p("curse_seconds") * 20), 20));
            Fx.mark((ServerLevel) level(), p, "☠", (int) Math.round(p("curse_seconds") * 20), Fx.SHADOW);
            level().playSound(null, p.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.8f, 1.4f);
        });
    }

    /** Ползучая порча */
    private void creep(Cast c) {
        Mechanics.hunt(c, (int) p("creep_seconds"), p("creep_radius"), p("creep_units"), 0x9B5CFF);
    }

    /** Взгляд Вессы */
    private void stare(Cast c) {
        Mechanics.gaze(c, (int) p("stare_seconds"), p("stare_angle"), p("stare_units"), 0x7A4ACF);
    }

}
