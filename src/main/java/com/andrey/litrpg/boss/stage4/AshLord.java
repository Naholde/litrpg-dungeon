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
            new Ability("Пепельное облако", "ashcloud", 1, false, b -> b.playersInArena().size() > 0, this::ashCloud),
            new Ability("Озёра лавы", "lavalakes", 0, false, b -> relocated(), this::lavaLakes),
            new Ability("Пепельные жилы", "veins", 0, false, b -> true, this::veins),
            new Ability("Пепельная корона", "crown", 0, false, b -> relocated() || b.getHealth() < b.getMaxHealth() * 0.6, this::crown),
            new Ability("Клеймо пепла", "brand", 1, false, b -> duelist == null && b.playersInArena().size() > 1, this::brand),
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


    /** Пепел: угли, серые хлопья, жар. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, 0xFF5A2A);
        Fx.shards(sl, at.add(0, 0.2, 0), radius, Math.max(4, Math.round(radius * 3)), 14, 0x5A5450);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ASH, at.x, at.y + 0.6, at.z,
                Math.round(20 + radius * 8), radius * 0.6, 0.5, radius * 0.6, 0.02);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 3, radius * 0.3, 0.1, radius * 0.3, 0.1);
    }

    /**
     * Клеймо пепла: меченый {@code brand_seconds} секунд оставляет за собой горящий след. Уводите след
     * вдоль края, подальше от отряда, — лужи горят ещё долго.
     */
    private void brand(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Telegraph.note(c, "lead", 0);
        ServerLevel sl = (ServerLevel) level();
        Fx.beam(sl, this, p, 0.2f, c.ticks, 0xFF5A2A);
        int dur = (int) Math.round(p("brand_seconds") * 20);
        c.at(c.ticks, () -> {
            Fx.mark(sl, p, "♨", dur, 0xFF5A2A);
            tell(Component.literal(p.getGameProfile().getName() + " заклеймён пеплом — уводи горящий след вдоль края!")
                    .withStyle(ChatFormatting.GOLD));
        });
        for (int t = 10; t <= dur; t += 10) {
            c.at(c.ticks + t, () -> {
                if (!p.isAlive() || p.isSpectator()) return;
                addPool(p.position(), 1.3, 0, 1.3, 9, p("brand_units"), 0xFF5A2A, null);
                Fx.disc(sl, p.position(), 1.3f, 20, 0xFF5A2A, true);
            });
        }
    }

    /**
     * Пепельная корона (с 60% и в Кальдере): кольцо огненных глыб падает вокруг Владыки по часовой — одна
     * за другой, затем второе кольцо ближе. Держитесь за падающей глыбой, а не перед ней.
     */
    private void crown(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Пепельная корона! Глыбы падают по кругу — идите за ними следом.").withStyle(ChatFormatting.GOLD));
        ServerLevel sl = (ServerLevel) level();
        double a0 = random.nextDouble() * Math.PI * 2;
        Vec3 center = position();
        for (int ring = 0; ring < 2; ring++) {
            double r = ring == 0 ? 7.5 : 4;
            int n = ring == 0 ? 10 : 6;
            for (int i = 0; i < n; i++) {
                double a = a0 + Math.PI * 2 * i / n;
                Vec3 at = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
                Cast rock = testCast(c.ticks + ring * 40 + i * 5);
                Fx.pillar(sl, at, 0.7f, 6, rock.ticks, 0xFF5A2A);
                Telegraph.circle(rock, at, 2.0f, p("crown_units"));
            }
        }
        level().playSound(null, blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Пепельное облако: облако пепла ползёт за дальним бойцом {ashcloud_seconds} — уводи по краю. */
    private void ashCloud(Cast c) {
        List<ServerPlayer> by = playersByDistance();
        if (by.isEmpty()) return;
        ServerPlayer far = by.get(by.size() - 1);
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.moving(c, position(), () -> far.isAlive() ? far.position() : null, 3f, 2.0, (int) p("ashcloud_seconds"),
                p("ashcloud_units"), 0x5A5450, pl -> slow(pl, 0.3, 25));
    }

    /** Озёра лавы (Кальдера): три озера лавы на {lavalakes_seconds} — места становится всё меньше. */
    private void lavaLakes(Cast c) {
        triggerAnim("main", "cast");
        for (int i = 0; i < 3; i++) {
            com.andrey.litrpg.boss.Ground.circle(c, randomSpot(0.25, 0.8), 3f, (int) p("lavalakes_seconds"), p("lavalakes_units"), 0xFF5A2A, null);
        }
    }
}
