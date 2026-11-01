package com.andrey.litrpg.boss;

import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Испытатель Системы — пробный хранитель полигона. Проверяет всё, на чём держится бой с хранителями:
 * удар по танку конусом, кольцо «ближе к хранителю», метеоры под каждым, ноша «на разделение»,
 * подготовку, которую можно сбить; с 66% — щит до разрушения двух узлов; с 33% — рой искр.
 */
public class TrialWarden extends BossEntity {
    public TrialWarden(EntityType<? extends TrialWarden> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.TRIAL_WARDEN;
    }

    @Override
    public String modelId() {
        return "trial_warden";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.66, 0.33};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Восстановление", "mend", 0, true, b -> b.phase() >= 1 && b.getHealth() < b.getMaxHealth() * 0.9,
                    this::mend),
            new Ability("Разделённая ноша", "burden", 1, false, b -> b.playersInArena().size() >= 1, this::burden),
            new Ability("Сокрушающий удар", "smash", 2, false, b -> b.getTarget() != null, this::smash),
            new Ability("Волна", "wave", 3, false, b -> true, this::wave),
            new Ability("Метеоры", "meteor", 4, false, b -> true, this::meteors));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    /** Удар конусом по текущей цели: танк выдержит, остальным перед хранителем — почти смерть. */
    private void smash(Cast c) {
        Vec3 dir = c.target.position().subtract(position());
        Telegraph.cone(c, position(), dir, 7, 70, p("smash_units"));
        schedule(c.ticks - 10, () -> triggerAnim("main", "slam"));
    }

    /** Кольцо: безопасно только вплотную к хранителю. */
    private void wave(Cast c) {
        Telegraph.donut(c, position(), 3.5f, (float) arenaRadius(), p("wave_units"));
        schedule(c.ticks - 10, () -> triggerAnim("main", "slam"));
    }

    /** Метеоры под каждым игроком: разбежаться и не стоять друг на друге. */
    private void meteors(Cast c) {
        for (ServerPlayer p : playersInArena()) Telegraph.circle(c, p.position(), 3, p("meteor_units"));
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1f, 0.5f);
    }

    /** Ноша на разделение: встать вместе (нужно не меньше трёх, если в отряде трое и больше). */
    private void burden(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        int need = Math.min(3, playersInArena().size());
        Telegraph.soak(c, p.position(), 3.5f, p("burden_units"), need);
        tell(Component.literal("Ноша ляжет на " + p.getGameProfile().getName() + " — встаньте рядом (нужно " + need + ")!")
                .withStyle(ChatFormatting.YELLOW));
    }

    /** Долгая подготовка: если не сбить оглушением — хранитель восстановит часть здоровья. */
    private void mend(Cast c) {
        Fx.pillar((ServerLevel) level(), position(), 1.6f, 6, c.ticks, Fx.HEAL);
        c.at(c.ticks, () -> {
            heal((float) (getMaxHealth() * p("mend_share")));
            tell(Component.literal(type().name() + " восстановился!").withStyle(ChatFormatting.RED));
        });
    }

    @Override
    protected void onPhase(int phase) {
        if (!(level() instanceof ServerLevel sl)) return;
        double unit = BossDamage.unit(bossLevel()), scale = partySize() / 3.0;
        if (phase == 1) {
            List<PowerNode> nodes = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                PowerNode n = ModEntities.POWER_NODE.get().create(sl);
                if (n == null) continue;
                Vec3 at = home().add(i == 0 ? -arenaRadius() * 0.6 : arenaRadius() * 0.6, 1, 0);
                n.moveTo(at.x, at.y, at.z, 0, 0);
                n.setHp(p("node_hp_units") * unit * Math.max(0.5, scale));
                sl.addFreshEntity(n);
                nodes.add(n);
            }
            raiseShield(nodes);
        } else if (phase == 2) {
            tell(Component.literal("Испытатель зовёт искры!").withStyle(ChatFormatting.LIGHT_PURPLE));
            for (int i = 0; i < Math.max(2, partySize()); i++) {
                Vex v = net.minecraft.world.entity.EntityType.VEX.create(sl);
                if (v == null) continue;
                Vec3 at = position().add(random.nextGaussian() * 3, 2, random.nextGaussian() * 3);
                v.moveTo(at.x, at.y, at.z, 0, 0);
                v.setCustomName(Component.literal("Искра испытателя"));
                var hp = v.getAttribute(Attributes.MAX_HEALTH);
                if (hp != null) hp.setBaseValue(p("spark_hp_units") * unit);
                v.setHealth((float) (p("spark_hp_units") * unit));
                var dmg = v.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmg != null) dmg.setBaseValue(p("spark_units") * unit);
                v.setLimitedLife(20 * 120);
                sl.addFreshEntity(v);
                track(v);
                LivingEntity t = randomNonTarget();
                if (t != null) v.setTarget(t);
            }
        }
    }
}
