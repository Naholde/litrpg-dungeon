package com.andrey.litrpg.boss.stage5;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.DungeonMob;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;
import com.andrey.litrpg.boss.MapMechanics;
import net.minecraft.world.level.block.Blocks;

/**
 * Двойник: «Твой следующий соперник — ты». На 80% и 40% у каждого в отряде появляется своя тень — бьёт только его,
 * а по-настоящему ранить её может только он сам. Пока тени живы, Двойник почти неуязвим. «Зеркало» — Двойник отражает
 * урон: прекратить бить, пока сияет. Удар по танку конусом, «Обмен» — два игрока меняются местами.
 */
public class Doppel extends BossEntity {
    private final Map<UUID, UUID> shadows = new HashMap<>();

    public Doppel(EntityType<? extends Doppel> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.DOPPEL;
    }

    @Override
    public String modelId() {
        return "doppel";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.8, 0.4};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Подмена мест", "places", 0, false, b -> true, this::places),
            new Ability("Зеркальный веер", "fan", 1, false, b -> b.phase() >= 1 && b.getTarget() != null, this::fan),
            new Ability("Осколки зеркала", "shards", 1, false, b -> true, this::glassShards),
            new Ability("Зеркальная стена", "glasswall", 0, false, b -> true, this::glasswall),
            new Ability("Отражённая связь", "link", 0, false, b -> true, this::link),
            new Ability("Залп отражений", "copies", 1, false, b -> true, this::copies),
            new Ability("Зеркало", "mirror", 0, false, b -> true, this::mirror),
            new Ability("Обмен", "swap", 1, false, b -> b.playersInArena().size() > 2, this::swap),
            new Ability("Зеркальный удар", "strike", 2, false, b -> b.getTarget() != null, this::strike));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void strike(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 80, p("strike_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    /** Зеркало: несколько секунд отражает урон по ударившему. */
    private void mirror(Cast c) {
        Fx.pillar((ServerLevel) level(), position(), 1.4f, 5, c.ticks, Fx.ARCANE);
        tell(Component.literal("Двойник поднимает зеркало — прекратите бить, когда засияет!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        c.at(c.ticks, () -> {
            int dur = (int) Math.round(p("mirror_seconds") * 20);
            StatusService.apply(this, Status.of(StatusType.REFLECT, "doppel_mirror", getUUID(), p("mirror_share"), level().getGameTime(), dur));
            Fx.pillar((ServerLevel) level(), position(), 1.6f, 6, dur, Fx.ICE);
            level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 1.4f);
        });
    }

    /** Обмен: двое (не танк) меняются местами. */
    private void swap(Cast c) {
        ServerPlayer a = randomNonTarget(), b = randomNonTarget();
        if (a == null || b == null || a == b) return;
        Fx.beam((ServerLevel) level(), a, b, 0.2f, c.ticks, Fx.ARCANE);
        c.at(c.ticks, () -> {
            Vec3 pa = a.position(), pb = b.position();
            a.teleportTo(pb.x, pb.y, pb.z);
            b.teleportTo(pa.x, pa.y, pa.z);
            level().playSound(null, a.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1.2f);
        });
    }

    /** Тень каждому: она охотится на хозяина; ранить её всерьёз может только он. */
    private void shadows() {
        for (ServerPlayer p : playersInArena()) {
            Vec3 at = p.position().add(p.getLookAngle().multiply(-3, 0, -3));
            DungeonMob s = summon(Bestiary.SHADOW, at, MobRank.ELITE);
            if (s == null) continue;
            s.setTitle("Тень " + p.getGameProfile().getName());
            s.getPersistentData().putUUID("litrpg_owner", p.getUUID());
            if (s.hasData(com.andrey.litrpg.core.ModAttachments.THREAT)) ThreatService.table(s).clear();
            ThreatService.add(s, p, 1_000_000);
            s.setTarget(p);
            shadows.put(s.getUUID(), p.getUUID());
        }
        tell(Component.literal("Твой следующий соперник — ты. Каждому — своя тень; бейте СВОЮ.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        level().playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2f, 0.6f);
    }

    private boolean shadowsAlive() {
        if (!(level() instanceof ServerLevel sl)) return false;
        shadows.keySet().removeIf(id -> sl.getEntity(id) == null || !sl.getEntity(id).isAlive());
        return !shadows.isEmpty();
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (!level().isClientSide && shadowsAlive()) amount *= (float) (1 - p("shadow_cut"));
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 20 != 0 || shadows.isEmpty() || !(level() instanceof ServerLevel sl)) return;
        // тень держится своего хозяина
        for (var e : Map.copyOf(shadows).entrySet()) {
            if (!(sl.getEntity(e.getKey()) instanceof DungeonMob s) || !s.isAlive()) continue;
            ServerPlayer owner = getServer().getPlayerList().getPlayer(e.getValue());
            if (owner != null && owner.isAlive() && !owner.isSpectator() && s.getTarget() != owner) s.setTarget(owner);
        }
    }

    @Override
    protected void onPhase(int phase) {
        shadows();
    }

    @Override
    public void resetFight() {
        shadows.clear();
        super.resetFight();
    }

    /** Отражённая связь */
    private void link(Cast c) {
        Mechanics.tether(c, (int) p("link_seconds"), p("link_dist"), p("link_units"), 0xA6E1FF);
    }

    /** Залп отражений */
    private void copies(Cast c) {
        Mechanics.volley(c, (int) p("copies_shots"), 8, p("copies_units"), 0xD6F3FF);
    }


    /** Подмена мест */
    private void places(Cast c) {
        MapMechanics.swapPlaces(c, 0xA6E1FF);
    }

    /** Зеркальная стена */
    private void glasswall(Cast c) {
        MapMechanics.wall(c, (int) p("glasswall_height"), (int) p("glasswall_seconds"), Blocks.GLASS.defaultBlockState(), 0xD6F3FF);
    }


    /** Зеркало: стеклянные осколки и холодные блики. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.shards(sl, at.add(0, 0.4, 0), radius, Math.max(6, Math.round(radius * 4)), 16, 0xA8F0FF);
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.6f, 8, 0xFFFFFF);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, at.x, at.y + 0.5, at.z,
                Math.round(6 + radius * 2), radius * 0.4, 0.3, radius * 0.4, 0.03);
    }

    /** Осколки зеркала: с потолка один за другим сыплются осколки — круги загораются по очереди, смотрите под ноги. */
    private void glassShards(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Зеркальный потолок трескается — осколки!").withStyle(ChatFormatting.AQUA));
        int n = (int) p("shards_count") + partySize();
        List<ServerPlayer> all = playersInArena();
        for (int i = 0; i < n; i++) {
            Vec3 at;
            if (i % 3 == 0 && !all.isEmpty()) {
                at = all.get(random.nextInt(all.size())).position();
            } else {
                double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * (arenaRadius() - 1);
                at = home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
            }
            Telegraph.circle(testCast(c.ticks + i * 5), at, 1.7f, p("shards_units"));
        }
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
    }

    /** Зеркальный веер (с 80%): удар конусом по танку — и его отражение за спиной Двойника. Стойте сбоку. */
    private void fan(Cast c) {
        Vec3 dir = c.target.position().subtract(position()).multiply(1, 0, 1);
        Telegraph.cone(c, position(), dir, 9, 70, p("fan_units"));
        Telegraph.cone(c, position(), dir.scale(-1), 9, 70, p("fan_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
        level().playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.6f);
    }
}
