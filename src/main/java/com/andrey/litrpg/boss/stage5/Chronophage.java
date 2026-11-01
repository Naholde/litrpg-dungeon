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

}
