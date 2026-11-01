package com.andrey.litrpg.boss.stage3;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Наяра, Жрица Глубин — финал храма. «Сияние глубин»: на полу два-три пятна света; пока сияние идёт, по жрице
 * проходит урон только от тех, кто стоит в свете (остальным — почти ничего). «Песнь глубин» — сбить, иначе удар по всем
 * и замедление. Водяные копья полосами к каждому, стражи-утопленники на 66%. На половине — Святилище на дне.
 */
public class Naiad extends BossEntity {
    private static final int LIGHT = 0xFFF4C0;
    private final List<Vec3> lights = new ArrayList<>();
    private long radianceUntil;

    public Naiad(EntityType<? extends Naiad> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.NAIAD;
    }

    @Override
    public String modelId() {
        return "naiad";
    }

    @Override
    protected String secondArenaName() {
        return "Святилище на дне";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.66};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Водоворот по следу", "chase", 0, false, b -> true, this::chase),
            new Ability("Дуэт глубин", "duet", 1, false, b -> true, this::duet),
            new Ability("Песнь глубин", "song", 0, true, b -> true, this::song),
            new Ability("Сияние глубин", "radiance", 1, false, b -> true, this::radiance),
            new Ability("Водяные копья", "spears", 2, false, b -> true, this::spears),
            new Ability("Удар посохом", "staff", 3, false, b -> b.getTarget() != null, this::staff));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void staff(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 6, 90, p("staff_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void spears(Cast c) {
        triggerAnim("main", "cast");
        for (ServerPlayer p : playersInArena()) {
            Vec3 dir = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 4)), 2.2f, p("spears_units"));
        }
    }

    /** Песнь: если не сбить — удар по всем и замедление. */
    private void song(Cast c) {
        Fx.pillar((ServerLevel) level(), position(), 1.6f, 7, c.ticks, Fx.ICE);
        triggerAnim("main", "cast");
        c.at(c.ticks, () -> {
            for (ServerPlayer p : playersInArena()) {
                com.andrey.litrpg.boss.BossDamage.hit(this, p, p("song_units"));
                StatusService.apply(p, Status.of(StatusType.SLOW, "naiad_song", getUUID(), 0.5, level().getGameTime(), 80));
            }
            tell(Component.literal("Песнь глубин оглушила отряд!").withStyle(ChatFormatting.RED));
            level().playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 2f, 1f);
        });
    }

    /** Сияние: пятна света на полу; пока идёт — урон по жрице проходит только от стоящих в свете. */
    private void radiance(Cast c) {
        lights.clear();
        int n = Math.max(2, partySize() - 1);
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = arenaRadius() * (0.3 + random.nextDouble() * 0.45);
            lights.add(home().add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
        int dur = (int) Math.round(p("radiance_seconds") * 20);
        for (Vec3 l : lights) Fx.disc((ServerLevel) level(), l, 2.5f, c.ticks + dur, LIGHT, true);
        c.at(c.ticks, () -> {
            radianceUntil = level().getGameTime() + dur;
            tell(Component.literal("Сияние глубин: урон по Наяре проходит только из пятен света!").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        });
    }

    private boolean inLight(ServerPlayer p) {
        for (Vec3 l : lights) {
            double dx = p.getX() - l.x, dz = p.getZ() - l.z;
            if (dx * dx + dz * dz <= 2.5 * 2.5) return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && level().getGameTime() < radianceUntil && source.getEntity() instanceof ServerPlayer p && !inLight(p)) {
            amount *= (float) (1 - p("radiance_cut"));
            if (tickCount % 10 == 0) Fx.burst((ServerLevel) level(), position().add(0, 1.5, 0), 1, 4, LIGHT);
        }
        return super.hurt(source, amount);
    }

    @Override
    protected void onPhase(int phase) {
        for (int i = 0; i < 2; i++) {
            double a = Math.PI * i;
            summon(Bestiary.DROWNED_GUARD, home().add(Math.cos(a) * 6, 0.2, Math.sin(a) * 6), MobRank.NORMAL);
        }
        tell(Component.literal("Наяра зовёт стражей глубин!").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    protected void onRelocated() {
        lights.clear();
        radianceUntil = 0;
    }

    @Override
    public void resetFight() {
        lights.clear();
        radianceUntil = 0;
        super.resetFight();
    }

    /** Водоворот по следу */
    private void chase(Cast c) {
        Mechanics.hunt(c, (int) p("chase_seconds"), p("chase_radius"), p("chase_units"), 0x7FE8FF);
    }

    /** Дуэт глубин */
    private void duet(Cast c) {
        Mechanics.tether(c, (int) p("duet_seconds"), p("duet_dist"), p("duet_units"), 0x5CD8E8);
    }

}
