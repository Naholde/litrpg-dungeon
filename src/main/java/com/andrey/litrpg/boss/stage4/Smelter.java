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
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

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
            new Ability("Разлив металла", "spill", 1, false, b -> true, this::spill),
            new Ability("Горячий пол", "hotfloor", 0, false, b -> b.phase() >= 1, this::hotFloor),
            new Ability("Расплав", "molten", 0, false, b -> true, this::molten),
            new Ability("Раскалённый слиток", "ingot", 0, false, b -> b.phase() >= 1 && b.playersInArena().size() > 1 && !hasIngot(), this::ingot),
            new Ability("Наковальни", "anvils", 1, false, b -> true, this::anvils),
            new Ability("Литейная стена", "castwall", 0, false, b -> true, this::castwall),
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
        if (!level().isClientSide) tickIngot();
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

    /** Раскалённый слиток: у кого он сейчас, до какого тика, сколько раз уже передан. */
    private java.util.UUID ingot;

    private boolean hasIngot() {
        return ingot != null;
    }

    private long ingotUntil, ingotPassAt;
    private int ingotPasses;

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


    /** Расплав */
    private void molten(Cast c) {
        MapMechanics.molten(c, (int) p("molten_sectors"), (int) p("molten_seconds"), p("molten_units"), 0xFF7A2E);
    }

    /** Литейная стена */
    private void castwall(Cast c) {
        MapMechanics.wall(c, (int) p("castwall_height"), (int) p("castwall_seconds"), Blocks.BLACKSTONE.defaultBlockState(), 0xE05A2A);
    }


    /** Металл: искры, окалина, раскалённые брызги. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, 0xFF8A3D);
        Fx.shards(sl, at.add(0, 0.2, 0), radius, Math.max(5, Math.round(radius * 3)), 14, 0x6E6A66);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, at.x, at.y + 0.4, at.z,
                Math.round(4 + radius * 2), radius * 0.4, 0.2, radius * 0.4, 0.1);
    }

    /** Наковальни: три наковальни падают одна за другой, шагая к дальнему бойцу, — уходите вбок, не вдоль. */
    private void anvils(Cast c) {
        ServerPlayer far = playersByDistance().isEmpty() ? null : playersByDistance().get(playersByDistance().size() - 1);
        if (far == null) return;
        triggerAnim("main", "cast");
        Vec3 dir = far.position().subtract(position()).multiply(1, 0, 1).normalize();
        ServerLevel sl = (ServerLevel) level();
        for (int i = 0; i < 3; i++) {
            Vec3 at = position().add(dir.scale(5 + i * 4.5));
            Cast fall = testCast(c.ticks + i * 12);
            Fx.pillar(sl, at, 0.9f, 8, fall.ticks, 0x6E6A66);
            Telegraph.circle(fall, at, 2.6f, p("anvils_units"));
            fall.at(fall.ticks, () -> sl.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.ANVIL_LAND,
                    SoundSource.HOSTILE, 1.4f, 0.7f));
        }
    }

    /**
     * Раскалённый слиток (с 66%): слиток у бойца; через {@code ingot_seconds} он взрывается в руках.
     * Подойдите к союзнику вплотную — слиток перейдёт к нему, и счёт начнётся заново. После
     * {@code ingot_passes} передач слиток остывает.
     */
    private void ingot(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Telegraph.note(c, "pass", 0);
        Fx.beam((ServerLevel) level(), this, p, 0.25f, c.ticks, 0xFF8A3D);
        c.at(c.ticks, () -> {
            if (!p.isAlive()) return;
            ingot = p.getUUID();
            ingotPasses = 0;
            ingotUntil = level().getGameTime() + Math.round(p("ingot_seconds") * 20);
            ingotPassAt = level().getGameTime() + 20;
            tell(Component.literal("У " + p.getGameProfile().getName() + " раскалённый слиток — передайте его, подойдя вплотную к союзнику!")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        });
    }

    private void tickIngot() {
        if (ingot == null || !(level() instanceof ServerLevel sl)) return;
        ServerPlayer holder = getServer().getPlayerList().getPlayer(ingot);
        long now = level().getGameTime();
        if (holder == null || !holder.isAlive() || holder.isSpectator() || !engaged()) {
            ingot = null;
            return;
        }
        if (now % 4 == 0) Fx.orbOn(sl, holder, 0.5f, 5, 0xFF8A3D);
        if (now % 20 == 0) {
            holder.displayClientMessage(Component.literal("Слиток взорвётся через " + Math.max(0, (ingotUntil - now) / 20) + " с — к союзнику!")
                    .withStyle(ChatFormatting.GOLD), true);
        }
        if (now >= ingotPassAt) {
            for (ServerPlayer o : playersInArena()) {
                if (o == holder || o.distanceTo(holder) > 2.2) continue;
                ingotPasses++;
                Fx.beam(sl, holder.position().add(0, 1, 0), o.position().add(0, 1, 0), 0.2f, 10, 0xFF8A3D);
                if (ingotPasses >= (int) p("ingot_passes")) {
                    ingot = null;
                    tell(Component.literal("Слиток остыл.").withStyle(ChatFormatting.GREEN));
                    sl.playSound(null, o.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.2f, 1f);
                    return;
                }
                ingot = o.getUUID();
                ingotUntil = now + Math.round(p("ingot_seconds") * 20);
                ingotPassAt = now + 20;
                sl.playSound(null, o.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.8f, 1.6f);
                return;
            }
        }
        if (now >= ingotUntil) {
            ingot = null;
            com.andrey.litrpg.boss.BossDamage.hit(this, holder, p("ingot_units"));
            for (ServerPlayer o : playersInArena()) {
                if (o != holder && o.distanceTo(holder) <= 3) com.andrey.litrpg.boss.BossDamage.hit(this, o, p("ingot_units") * 0.4);
            }
            impact(holder.position(), 3);
            sl.playSound(null, holder.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 1.2f);
        }
    }

    /** Разлив металла: к трём бойцам растекаются реки металла и стынут {spill_seconds} — арена режется на части. */
    private void spill(Cast c) {
        triggerAnim("main", "cast");
        List<ServerPlayer> all = new java.util.ArrayList<>(playersInArena());
        java.util.Collections.shuffle(all, new java.util.Random(random.nextLong()));
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            Vec3 d = all.get(i).position().subtract(position());
            com.andrey.litrpg.boss.Ground.line(c, position(), position().add(d.multiply(1, 0, 1).normalize().scale(arenaRadius() + 2)), 2.4f,
                    (int) p("spill_seconds"), p("spill_units"), 0xFF8A3D, null);
        }
    }

    /** Горячий пол (с 66%): горят два сектора по 60°, потом пол поворачивается на четверть — идите за холодом. */
    private void hotFloor(Cast c) {
        triggerAnim("main", "cast");
        double a0 = random.nextDouble() * 360;
        for (int k = 0; k < 2; k++) {
            Cast part = testCast(c.ticks + k * 80);
            for (int q = 0; q < 2; q++) {
                com.andrey.litrpg.boss.Ground.sector(part, home(), dir(a0 + k * 90 + q * 180), (float) arenaRadius() + 3, 60, 4, p("hotfloor_units"), 0xFF6A1A, null);
            }
        }
    }
}
