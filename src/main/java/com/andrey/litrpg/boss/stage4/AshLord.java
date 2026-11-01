package com.andrey.litrpg.boss.stage4;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

/**
 * Владыка Пепла — финал Кузен. «Эго»: вызывает на дуэль лучшего бойца (не танка) — в круге вдвоём, урон по Владыке
 * от остальных почти не проходит, а дуэлянта он бьёт каждые 2 с: лекарю — держать, дуэлянту — выжить и бить.
 * «Обвал» — круги, после которых остаётся лава; «Пепельный шквал» — кольцо. На половине — Кальдера: «Извержение» под всеми.
 */
public class AshLord extends BossEntity {
    private UUID duelist;
    private long duelUntil;

    public AshLord(EntityType<? extends AshLord> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.ASH_LORD;
    }

    @Override
    public String modelId() {
        return "ash_lord";
    }

    @Override
    protected String secondArenaName() {
        return "Кальдеру";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[0];
    }

    @Override
    protected void onPhase(int phase) {
    }

    private final List<Ability> abilities = List.of(
            new Ability("Пепельные жилы", "veins", 0, false, b -> true, this::veins),
            new Ability("Пепел сужает круг", "closing", 0, false, b -> true, this::closing),
            new Ability("Пепельный обвал", "ashfall", 0, false, b -> true, this::ashfall),
            new Ability("Эго", "ego", 0, false, b -> b.playersInArena().size() > 1 && duelist == null, this::ego),
            new Ability("Извержение", "erupt", 1, false, b -> relocated(), this::erupt),
            new Ability("Обвал", "collapse", 2, false, b -> poolCount() < 6, this::collapse),
            new Ability("Пепельный шквал", "storm", 3, false, b -> true, this::storm),
            new Ability("Меч Пепла", "sword", 4, false, b -> b.getTarget() != null, this::sword));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void sword(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 8, 80, p("sword_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void storm(Cast c) {
        Telegraph.donut(c, position(), 4, (float) arenaRadius() + 2, p("storm_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
    }

    private void collapse(Cast c) {
        triggerAnim("main", "cast");
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = arenaRadius() * (0.2 + random.nextDouble() * 0.6);
            Vec3 at = home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            Telegraph.circle(c, at, 3.5f, p("collapse_units"));
            c.at(c.ticks, () -> addPool(at, 3.5, 0, 3.5, (int) Math.round(p("lava_seconds")), p("lava_units"), Fx.FIRE, null));
        }
    }

    private void erupt(Cast c) {
        for (ServerPlayer p : playersInArena()) Telegraph.circle(c, p.position(), 3, p("erupt_units"));
        triggerAnim("main", "cast");
    }

    /** Эго: дуэль с лучшим бойцом (вторым по угрозе — после танка). */
    private void ego(Cast c) {
        ServerPlayer d = null;
        var sorted = ThreatService.sorted(this);
        for (var e : sorted) {
            ServerPlayer p = getServer().getPlayerList().getPlayer(e.getKey());
            if (p != null && p != getTarget() && playersInArena().contains(p)) {
                d = p;
                break;
            }
        }
        if (d == null) d = randomNonTarget();
        if (d == null) return;
        ServerPlayer duel = d;
        Fx.beam((ServerLevel) level(), this, duel, 0.3f, c.ticks, Fx.FIRE);
        tell(Component.literal("Владыка Пепла вызывает " + duel.getGameProfile().getName() + " на дуэль! Остальные — не мешайте, лекарь — держите его.")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        c.at(c.ticks, () -> {
            int dur = (int) Math.round(p("duel_seconds") * 20);
            duelist = duel.getUUID();
            duelUntil = level().getGameTime() + dur;
            teleportTo(duel.getX() + 2, duel.getY(), duel.getZ());
            for (int t = 20; t <= dur; t += 40) {
                schedule(t, () -> {
                    if (duelist == null || !duel.isAlive() || duel.isSpectator()) return;
                    Fx.ring((ServerLevel) level(), duel.position(), 6, 41, Fx.FIRE);
                    if (distanceTo(duel) > 4) teleportTo(duel.getX() + 1.5, duel.getY(), duel.getZ());
                    triggerAnim("main", "attack");
                    BossDamage.hit(this, duel, p("duel_units"));
                    level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.5f, 0.6f);
                });
            }
            schedule(dur, () -> {
                duelist = null;
                tell(Component.literal("Дуэль окончена.").withStyle(ChatFormatting.GRAY));
            });
        });
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && duelist != null && level().getGameTime() < duelUntil && source.getEntity() instanceof ServerPlayer p
                && !p.getUUID().equals(duelist)) {
            amount *= (float) (1 - p("duel_cut"));
        }
        return super.hurt(source, amount);
    }

    @Override
    public void resetFight() {
        duelist = null;
        super.resetFight();
    }

    /** Пепельный обвал */
    private void ashfall(Cast c) {
        Mechanics.crumble(c, (int) p("ashfall_waves"), (int) p("ashfall_safe"), p("ashfall_radius"), p("ashfall_units"), 0xC96B3A);
    }


    /** Пепельные жилы */
    private void veins(Cast c) {
        MapMechanics.molten(c, (int) p("veins_sectors"), (int) p("veins_seconds"), p("veins_units"), 0xC96B3A);
    }

    /** Пепел сужает круг */
    private void closing(Cast c) {
        MapMechanics.shrink(c, (int) p("closing_seconds"), p("closing_units"), 0x8A3A2A);
    }

}
