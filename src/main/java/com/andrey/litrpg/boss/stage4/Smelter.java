package com.andrey.litrpg.boss.stage4;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.fx.Fx;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Плавильный голем. Жар: кто стоит рядом с ним, копит жар; полная шкала — «Расплав» по себе и соседям. Сбросить жар —
 * встать в чан с водой. Молот по танку, «Литьё» — полосы расплава оставляют огненные лужи. На 66% и 33% закрывается
 * бронёй: разбить заклёпки по краям арены.
 */
public class Smelter extends BossEntity {
    private final Map<UUID, Integer> heat = new HashMap<>();

    public Smelter(EntityType<? extends Smelter> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.SMELTER;
    }

    @Override
    public String modelId() {
        return "smelter";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.66, 0.33};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Сноп искр", "sparks", 0, false, b -> true, this::sparks),
            new Ability("Реки металла", "rivers", 1, false, b -> true, this::rivers),
            new Ability("Литьё", "pour", 1, false, b -> true, this::pour),
            new Ability("Молот", "hammer", 2, false, b -> b.getTarget() != null, this::hammer));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void hammer(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 70, p("hammer_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
        level().playSound(null, blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /** Литьё: полосы расплава к двум игрокам; там, где прошёл металл, ещё горит. */
    private void pour(Cast c) {
        triggerAnim("main", "cast");
        List<ServerPlayer> ps = new ArrayList<>(playersInArena());
        for (int i = 0; i < Math.min(2, ps.size()); i++) {
            Vec3 dir = ps.get(random.nextInt(ps.size())).position().subtract(position()).multiply(1, 0, 1).normalize();
            Vec3 from = position(), to = position().add(dir.scale(arenaRadius()));
            Telegraph.line(c, from, to, 2.4f, p("pour_units"));
            c.at(c.ticks, () -> {
                for (double d = 3; d <= arenaRadius(); d += 3) {
                    addPool(from.add(dir.scale(d)), 1.6, 0, 1.6, (int) Math.round(p("pour_seconds")), p("pour_burn_units"), Fx.FIRE, null);
                }
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !engaged() || tickCount % 40 != 0) return;
        ServerLevel sl = (ServerLevel) level();
        int max = (int) Math.round(p("heat_max"));
        for (ServerPlayer p : playersInArena()) {
            int h = heat.getOrDefault(p.getUUID(), 0);
            if (p.isInWater()) {
                if (h > 0) {
                    sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1, p.getZ(), 12, 0.4, 0.5, 0.4, 0.02);
                    sl.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1f);
                }
                heat.remove(p.getUUID());
                continue;
            }
            if (p.distanceTo(this) > p("heat_radius")) continue;
            h++;
            if (h >= max) {
                heat.remove(p.getUUID());
                meltdown(p);
            } else {
                heat.put(p.getUUID(), h);
                p.displayClientMessage(Component.literal("Жар: ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal("▮".repeat(h)).withStyle(ChatFormatting.RED))
                        .append(Component.literal("▮".repeat(max - h)).withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(h >= max - 2 ? "  — в чан с водой!" : "").withStyle(ChatFormatting.AQUA)), true);
            }
        }
    }

    /** Расплав: игрок перегрелся — урон ему и всем рядом. */
    private void meltdown(ServerPlayer p) {
        BossDamage.hit(this, p, p("melt_units"));
        for (ServerPlayer o : playersInArena()) {
            if (o != p && o.distanceTo(p) <= 3) BossDamage.hit(this, o, p("melt_units") * 0.6);
        }
        Fx.burst((ServerLevel) level(), p.position().add(0, 1, 0), 3, 12, Fx.FIRE);
        level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.2f);
        tell(Component.literal(p.getGameProfile().getName() + " перегрелся — расплав!").withStyle(ChatFormatting.RED));
    }

    @Override
    protected void onPhase(int phase) {
        List<PowerNode> rivets = new ArrayList<>();
        int n = 3;
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n + phase;
            PowerNode node = spawnNode(ModEntities.POWER_NODE.get(), home().add(Math.cos(a) * (arenaRadius() - 3), 1,
                    Math.sin(a) * (arenaRadius() - 3)), p("rivet_hp_units"), "Заклёпка брони");
            if (node != null) rivets.add(node);
        }
        raiseShield(rivets);
        tell(Component.literal("Голем закрылся бронёй — разбейте заклёпки!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
    }

    @Override
    public void resetFight() {
        heat.clear();
        super.resetFight();
    }

    /** Сноп искр */
    private void sparks(Cast c) {
        Mechanics.volley(c, (int) p("sparks_shots"), 8, p("sparks_units"), 0xFF7A2E);
    }

    /** Реки металла */
    private void rivers(Cast c) {
        Mechanics.crumble(c, (int) p("rivers_waves"), (int) p("rivers_safe"), p("rivers_radius"), p("rivers_units"), 0xE05A2A);
    }

}
