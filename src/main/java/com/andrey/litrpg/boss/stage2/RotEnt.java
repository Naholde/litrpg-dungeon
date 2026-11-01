package com.andrey.litrpg.boss.stage2;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.MobRank;
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
 * Гнилой Древень. Удар ветвью по танку; корни-капканы полосами и кругами (попавшего держат корни); споровые облака —
 * растущие лужи (отводить бой от них); «Сердцевина» — лечение, которое нужно сбить. С половины — зовёт древней-стражей
 * и гниёт быстрее (споры чаще).
 */
public class RotEnt extends BossEntity {
    private static final int SPORE = 0x9A5ABF;

    public RotEnt(EntityType<? extends RotEnt> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.ROT_ENT;
    }

    @Override
    public String modelId() {
        return "rot_ent";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.5};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Кольца спор", "circles", 0, false, b -> true, this::circles),
            new Ability("Корни-путы", "bind", 1, false, b -> true, this::bind),
            new Ability("Сердцевина", "heart", 0, true, b -> b.getHealth() < b.getMaxHealth() * 0.85, this::heartwood),
            new Ability("Споровое облако", "spores", 1, false, b -> poolCount() < 4, this::spores),
            new Ability("Корни-капканы", "roots", 2, false, b -> true, this::roots),
            new Ability("Удар ветвью", "slam", 3, false, b -> b.getTarget() != null, this::slam));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void slam(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 80, p("slam_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
    }

    private void root(ServerPlayer p) {
        StatusService.apply(p, Status.of(StatusType.ROOT, "ent_roots", getUUID(), 1, level().getGameTime(), 60));
    }

    private void roots(Cast c) {
        triggerAnim("main", "cast");
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 2)), 2.5f, p("roots_units"), this::root);
        }
        List<ServerPlayer> ps = playersInArena();
        for (int i = 0; i < Math.min(2, ps.size()); i++) {
            Telegraph.circle(c, ps.get(random.nextInt(ps.size())).position(), 2.5f, p("roots_units"), this::root);
        }
        level().playSound(null, blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Споры: лужа под случайным игроком, растёт; стоящих внутри травит. */
    private void spores(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        Vec3 at = p.position();
        Fx.disc((ServerLevel) level(), at, 2, c.ticks, SPORE, true);
        c.at(c.ticks, () -> {
            addPool(at, 2.5, p("spore_grow"), 5.5, (int) Math.round(p("spore_seconds")), p("spore_units"), SPORE, null);
            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR, at.x, at.y + 1, at.z,
                    40, 1.5, 0.5, 1.5, 0.01);
        });
    }

    private void heartwood(Cast c) {
        Fx.pillar((ServerLevel) level(), position(), 1.6f, 6, c.ticks, Fx.NATURE);
        c.at(c.ticks, () -> {
            heal((float) (getMaxHealth() * p("heart_share")));
            tell(Component.literal("Сердцевина Древня затянула раны!").withStyle(ChatFormatting.RED));
        });
    }

    @Override
    protected void onPhase(int phase) {
        tell(Component.literal("Древень гниёт изнутри — споры чаще, и к нему идут стражи леса!").withStyle(ChatFormatting.DARK_PURPLE));
        for (int i = 0; i < 2; i++) {
            double a = Math.PI * i;
            summon(Bestiary.TREANT_GUARD, home().add(Math.cos(a) * (arenaRadius() - 3), 0.2, Math.sin(a) * (arenaRadius() - 3)), MobRank.NORMAL);
        }
    }

    @Override
    public double p(String key) {
        // Гниль: после половины здоровья споры приходят вдвое чаще.
        if (key.equals("spores_cd") && phase() >= 1) return super.p(key) * 0.5;
        return super.p(key);
    }

    /** Кольца спор */
    private void circles(Cast c) {
        Mechanics.rings(c, (int) p("circles_waves"), p("circles_units"), p("circles_gap"), 0x9BD86B);
    }

    /** Корни-путы */
    private void bind(Cast c) {
        Mechanics.tether(c, (int) p("bind_seconds"), p("bind_dist"), p("bind_units"), 0x6B8A3A);
    }

}
