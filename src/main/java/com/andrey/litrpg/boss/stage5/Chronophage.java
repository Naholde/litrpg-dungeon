package com.andrey.litrpg.boss.stage5;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.core.ModEntities;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

/**
 * Хронофаг. «Песочные часы»: по арене встают часы — не разбили до срока, время останавливается (всех оглушает и бьёт).
 * «Откат»: каждому на полу метка — через несколько секунд всех вернёт на метки, и в них сразу ударит: после отката —
 * сразу сойти с метки. Временной разрез по танку. С половины здоровья время ускоряется.
 */
public class Chronophage extends BossEntity {
    public Chronophage(EntityType<? extends Chronophage> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.CHRONOPHAGE;
    }

    @Override
    public String modelId() {
        return "chronophage";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.5};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Часовая стрелка", "clockhand", 1, false, b -> true, this::clockHand),
            new Ability("Застывшие следы", "frozensteps", 0, false, b -> b.phase() >= 1, this::frozenSteps),
            new Ability("Время кончается", "closing", 0, false, b -> true, this::closing),
            new Ability("Застывшее время", "stasis", 0, false, b -> b.phase() >= 1, this::stasis),
            new Ability("Эхо удара", "echo", 1, false, b -> true, this::echo),
            new Ability("Стрелка часов", "hand", 0, false, b -> true, this::hand),
            new Ability("Волны времени", "waves", 0, false, b -> true, this::waves),
            new Ability("Часы идут за вами", "clock", 1, false, b -> true, this::clock),
            new Ability("Песочные часы", "glass", 0, false, b -> spawnedAlive().isEmpty(), this::hourglasses),
            new Ability("Откат", "rewind", 1, false, b -> true, this::rewind),
            new Ability("Временной разрез", "slash", 2, false, b -> b.getTarget() != null, this::slash));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void slash(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 8, 70, p("slash_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void hourglasses(Cast c) {
        triggerAnim("main", "cast");
        int n = 2 + partySize() / 2;
        c.at(c.ticks, () -> {
            List<PowerNode> glasses = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + random.nextDouble() * 0.5;
                PowerNode g = spawnNode(ModEntities.HOURGLASS.get(), home().add(Math.cos(a) * (arenaRadius() - 3), 1,
                        Math.sin(a) * (arenaRadius() - 3)), p("glass_hp_units"), "Песочные часы");
                if (g != null) glasses.add(g);
            }
            int t = (int) Math.round(p("glass_seconds") * 20);
            tell(Component.literal("Песочные часы! Разбейте их за " + t / 20 + " с, иначе время остановится.").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            schedule(t, () -> {
                long left = glasses.stream().filter(PowerNode::isAlive).count();
                glasses.forEach(PowerNode::discard);
                if (left == 0) return;
                for (ServerPlayer p : playersInArena()) {
                    BossDamage.hit(this, p, p("stop_units") * left);
                    StatusService.apply(p, Status.of(StatusType.STUN, "chrono_stop", getUUID(), 1, level().getGameTime(), 50));
                }
                tell(Component.literal("Время остановилось! (часов не разбито: " + left + ")").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level().playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 2f, 0.5f);
            });
        });
    }

    /** Откат: метки на местах; через {@code rewind_seconds} всех вернёт на метки, и метки взорвутся. */
    private void rewind(Cast c) {
        Map<UUID, Vec3> marks = new HashMap<>();
        int delay = (int) Math.round(p("rewind_seconds") * 20);
        for (ServerPlayer p : playersInArena()) {
            marks.put(p.getUUID(), p.position());
            Fx.disc((ServerLevel) level(), p.position(), 1.5f, delay + 25, Fx.HOLY, false);
        }
        tell(Component.literal("Хронофаг метит время: через " + delay / 20 + " с всех вернёт на метки — сразу сойдите с них!")
                .withStyle(ChatFormatting.YELLOW));
        c.at(c.ticks, () -> schedule(delay - c.ticks, () -> {
            Cast blast = testCast(25);
            for (var e : marks.entrySet()) {
                ServerPlayer p = getServer().getPlayerList().getPlayer(e.getKey());
                if (p == null || !p.isAlive() || p.isSpectator()) continue;
                Vec3 at = e.getValue();
                p.teleportTo(at.x, at.y, at.z);
                p.fallDistance = 0;
                Telegraph.circle(blast, at, 2.5f, p("rewind_units"));
            }
            level().playSound(null, blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 2f, 0.6f);
        }));
    }

    @Override
    protected void onPhase(int phase) {
        tell(Component.literal("Время ускоряется!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
    }

    @Override
    public double p(String key) {
        if (key.endsWith("_cd") && phase() >= 1) return super.p(key) * 0.75;
        return super.p(key);
    }

    /** Волны времени */
    private void waves(Cast c) {
        Mechanics.rings(c, (int) p("waves_waves"), p("waves_units"), p("waves_gap"), 0xFFE8A6);
    }

    /** Часы идут за вами */
    private void clock(Cast c) {
        Mechanics.hunt(c, (int) p("clock_seconds"), p("clock_radius"), p("clock_units"), 0xE8C06B);
    }


    /** Время кончается */
    private void closing(Cast c) {
        MapMechanics.shrink(c, (int) p("closing_seconds"), p("closing_units"), 0xFFE8A6);
    }

    /** Стрелка часов */
    private void hand(Cast c) {
        MapMechanics.sweep(c, (int) p("hand_seconds"), p("hand_units"), getRandom().nextBoolean(), 0xE8C06B);
    }


    /** Время: песок, золотые искры, стрелки. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, 0xE8C878);
        Fx.rune(sl, at, radius * 0.8f, 12, 12, 0xFFE9A8);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.WAX_ON, at.x, at.y + 0.5, at.z,
                Math.round(8 + radius * 3), radius * 0.4, 0.3, radius * 0.4, 0.5);
    }

    /**
     * Эхо удара: под каждым круг — а через {@code echo_delay} секунд время повторяет удар в том же месте.
     * Ушли — не возвращайтесь.
     */
    private void echo(Cast c) {
        triggerAnim("main", "cast");
        int delay = (int) Math.round(p("echo_delay") * 20);
        for (ServerPlayer p : playersInArena()) {
            Vec3 at = p.position();
            Telegraph.circle(c, at, 2.6f, p("echo_units"));
            Telegraph.circle(testCast(c.ticks + delay), at, 2.6f, p("echo_units"));
        }
        level().playSound(null, blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.6f, 1.4f);
    }

    /** Застывшее время (с половины): по арене ложатся пятна стоячего времени — в них медленно и больно. */
    private void stasis(Cast c) {
        triggerAnim("main", "cast");
        ServerLevel sl = (ServerLevel) level();
        tell(Component.literal("Время застывает пятнами — не стойте в них.").withStyle(ChatFormatting.GOLD));
        c.at(c.ticks, () -> {
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = arenaRadius() * (0.25 + random.nextDouble() * 0.45);
                Vec3 at = home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                addPool(at, 4.5, 0, 4.5, (int) Math.round(p("stasis_seconds")), p("stasis_units"), 0xE8C878,
                        p -> StatusService.apply(p, Status.of(StatusType.SLOW, "chrono_stasis", getUUID(), 0.55, level().getGameTime(), 25)));
                Fx.rune(sl, at, 4.5f, 12, (int) Math.round(p("stasis_seconds") * 20), 0xE8C878);
            }
        });
    }

    /** Часовая стрелка: сектор времени идёт по кругу шаг за шагом — за ним горит след. Идите перед стрелкой... или за следом. */
    private void clockHand(Cast c) {
        triggerAnim("main", "cast");
        double a0 = random.nextDouble() * 360;
        boolean cw = random.nextBoolean();
        for (int i = 0; i < 8; i++) {
            com.andrey.litrpg.boss.Ground.sector(testCast(c.ticks + i * 20), home(), dir(a0 + (cw ? 45 : -45) * i), (float) arenaRadius() + 3, 50, 2,
                    p("clockhand_units"), 0xE8C878, null);
        }
        tell(Component.literal("Часовая стрелка пошла по кругу!").withStyle(ChatFormatting.GOLD));
    }

    /** Застывшие следы (с половины): шесть секунд там, где боец стоял две секунды назад, застывает время. Не стойте на месте. */
    private void frozenSteps(Cast c) {
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Telegraph.note(c, "move", 0);
        tell(Component.literal("Ваши следы застывают во времени — не возвращайтесь туда, где были.").withStyle(ChatFormatting.GOLD));
        for (int t = 0; t < 120; t += 20) {
            c.at(c.ticks + t, () -> {
                for (ServerPlayer p : playersInArena()) {
                    Vec3 at = p.position();
                    Fx.disc((ServerLevel) level(), at, 1.8f, 40, 0xE8C878, true);
                    schedule(40, () -> addZone(80, p("frozensteps_units"), pt -> {
                        double dx = pt.x - at.x, dz = pt.z - at.z;
                        return dx * dx + dz * dz <= 1.8 * 1.8;
                    }, sl -> Fx.disc(sl, at, 1.8f, 12, 0xE8C878, false), null));
                }
            });
        }
    }
}
