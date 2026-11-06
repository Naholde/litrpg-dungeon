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
import net.minecraft.world.phys.Vec3;

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
            new Ability("Порченая земля", "blight", 1, false, b -> true, this::blight),
            new Ability("Тёмный круг", "darkring", 1, false, b -> true, this::darkRing),
            new Ability("Ползучая тень", "shade", 1, false, b -> b.playersInArena().size() > 0, this::shade),
            new Ability("Ползучая порча", "creep", 0, false, b -> true, this::creep),
            new Ability("Взгляд Вессы", "stare", 1, false, b -> true, this::stare),
            new Ability("Тёмное лечение", "mend", 0, true, b -> woundedSister() != null, this::mend),
            new Ability("Тёмная волна", "nova", 1, false, b -> !playersNear(6).isEmpty(), this::nova),
            new Ability("Проклятие", "curse", 2, false, b -> true, this::curse),
            new Ability("Тёмные свечи", "candles", 1, false, b -> spawnedAlive().stream().noneMatch(e ->
                    e.getType() == com.andrey.litrpg.core.ModEntities.DARK_CANDLE.get()), this::candles));

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
        Telegraph.circle(c, position(), 4.5f, p("nova_units"));
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


    /**
     * Тёмные свечи: по арене встают три свечи и, пока стоят, раз в {@code candles_every} секунд бьют
     * ближайшего бойца тенью (круг под ним — уйти или разбить свечу). Свечи гаснут сами через
     * {@code candles_seconds}. Весса тем временем ведёт бой дальше.
     */
    private void candles(Cast c) {
        triggerAnim("main", "cast");
        ServerLevel sl = (ServerLevel) level();
        c.at(c.ticks, () -> {
            tell(Component.literal("Весса зажгла тёмные свечи — разбейте их, пока они бьют тенью!").withStyle(ChatFormatting.DARK_PURPLE));
            int life = (int) Math.round(p("candles_seconds") * 20);
            int every = (int) Math.round(p("candles_every") * 20);
            for (int i = 0; i < 3; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = arenaRadius() * (0.4 + random.nextDouble() * 0.4);
                Vec3 at = home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                var node = spawnNode(com.andrey.litrpg.core.ModEntities.DARK_CANDLE.get(), at, p("candles_hp_units"), "Тёмная свеча");
                if (node == null) continue;
                Fx.pillar(sl, at, 0.6f, 3, 20, 0x9B5CFF);
                for (int t = every; t < life; t += every) {
                    detached(t, () -> {
                        if (!node.isAlive()) return;
                        ServerPlayer near = playersInArena().stream()
                                .min(java.util.Comparator.comparingDouble(p -> p.distanceToSqr(node))).orElse(null);
                        if (near == null) return;
                        Fx.beam(sl, node.position().add(0, 1.2, 0), near.position().add(0, 1, 0), 0.12f, 16, 0x9B5CFF);
                        Telegraph.shot(testCast(20), near.position(), 1.4f, p("candles_units"), 0x9B5CFF);
                    });
                }
                detached(life, () -> {
                    if (node.isAlive()) node.discard();
                });
            }
        });
    }


    /** Порченая земля: под каждым загорается круг и тлеет {blight_seconds} — сойдите и не возвращайтесь. */
    private void blight(Cast c) {
        triggerAnim("main", "cast");
        for (ServerPlayer p : playersInArena()) {
            com.andrey.litrpg.boss.Ground.circle(c, p.position(), 2.2f, (int) p("blight_seconds"), p("blight_units"), 0x7A4ACF, null);
        }
    }

    /** Тёмный круг: середина зала темнеет на {darkring_seconds} — бой уходит к стенам. */
    private void darkRing(Cast c) {
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.circle(c, home(), 5.5f, (int) p("darkring_seconds"), p("darkring_units"), 0x2A0F3A, null);
        tell(Component.literal("Середина зала темнеет — уходите к стенам.").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Ползучая тень: тень ползёт за бойцом {shade_seconds} — уводите её от своих, не стойте на месте. */
    private void shade(Cast c) {
        ServerPlayer prey = randomNonTarget();
        if (prey == null) return;
        triggerAnim("main", "cast");
        tell(Component.literal("Тень Вессы ползёт за " + prey.getGameProfile().getName() + " — уводи её от своих!")
                .withStyle(ChatFormatting.DARK_PURPLE));
        com.andrey.litrpg.boss.Ground.moving(c, position(), () -> prey.isAlive() ? prey.position() : null, 2.4f, 2.2, (int) p("shade_seconds"),
                p("shade_units"), 0x7A4ACF, null);
    }

    /** Бой сестёр — один: журнал Вессы пишется в бой Морры. */
    @Override
    public com.andrey.litrpg.boss.BossEntity logOwner() {
        return sister() != null ? sister() : this;
    }
}
