package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.core.ModEntities;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Король-лич Карн — финал Склепа. Коса по танку, ледяной шторм под каждым, ледяная гробница (союзники разбивают лёд,
 * иначе заточённому — тяжёлый удар), поднимает мёртвых. На половине здоровья уходит в Тронный зал: там «Во тьме» —
 * свет гаснет, холод бьёт всех, кто не у душевных жаровен. На четверти закрывается филактерией — разбить её.
 */
public class LichKing extends BossEntity {
    /** Жаровни Тронного зала — по углам квадрата от центра второго зала (так же стоят в карте). */
    public static final int BRAZIER = 6;
    private final Map<UUID, UUID> tombs = new HashMap<>();

    public LichKing(EntityType<? extends LichKing> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.LICH;
    }

    @Override
    public String modelId() {
        return "lich";
    }

    @Override
    protected String secondArenaName() {
        return "Тронный зал";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.25};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Ледяная корка", "frost", 0, false, b -> true, this::frost),
            new Ability("Зов вьюги", "blizzard", 0, false, b -> relocated(), this::blizzard),
            new Ability("Ледяная тропа", "frostpath", 2, false, b -> true, this::frostpath),
            new Ability("Во тьме", "dark", 0, false, b -> relocated(), this::darkness),
            new Ability("Ледяная гробница", "tomb", 1, false, b -> b.playersInArena().size() > 1, this::tomb),
            new Ability("Поднять мёртвых", "raise", 2, false, b -> spawnedAlive().size() < 4, this::raise),
            new Ability("Ледяной шторм", "storm", 3, false, b -> true, this::storm),
            new Ability("Коса смерти", "scythe", 4, false, b -> b.getTarget() != null, this::scythe));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void scythe(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 90, p("scythe_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void storm(Cast c) {
        triggerAnim("main", "cast");
        for (ServerPlayer p : playersInArena()) Telegraph.circle(c, p.position(), 3, p("storm_units"));
        level().playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.5f);
    }

    /** Ледяная гробница: игрок скован, пока союзники не разобьют лёд; не успели — тяжёлый удар по нему. */
    private void tomb(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Fx.beam((ServerLevel) level(), this, p, 0.2f, c.ticks, Fx.ICE);
        c.at(c.ticks, () -> {
            if (!p.isAlive() || p.isSpectator()) return;
            int dur = (int) Math.round(p("tomb_seconds") * 20);
            long now = level().getGameTime();
            StatusService.apply(p, Status.of(StatusType.ROOT, "lich_tomb", getUUID(), 1, now, dur));
            StatusService.apply(p, Status.of(StatusType.SILENCE, "lich_tomb", getUUID(), 1, now, dur));
            PowerNode node = spawnNode(ModEntities.ICE_TOMB.get(), p.position(), p("tomb_hp_units"), "Ледяная гробница");
            if (node == null) return;
            tombs.put(node.getUUID(), p.getUUID());
            tell(Component.literal(p.getGameProfile().getName() + " в ледяной гробнице — разбейте лёд за "
                    + Math.round(p("tomb_seconds")) + " с!").withStyle(ChatFormatting.AQUA));
            level().playSound(null, p.blockPosition(), SoundEvents.GLASS_PLACE, SoundSource.HOSTILE, 2f, 0.5f);
            schedule(dur, () -> {
                if (!node.isAlive()) return;
                node.discard();
                tombs.remove(node.getUUID());
                BossDamage.hit(this, p, p("tomb_units"));
                Fx.burst((ServerLevel) level(), p.position().add(0, 1, 0), 2, 10, Fx.ICE);
            });
        });
    }

    private void raise(Cast c) {
        triggerAnim("main", "cast");
        int n = 1 + partySize() / 2;
        c.at(c.ticks, () -> {
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + 0.5;
                summon(Bestiary.CRYPT_WARRIOR, home().add(Math.cos(a) * (arenaRadius() - 3), 0.2, Math.sin(a) * (arenaRadius() - 3)),
                        MobRank.NORMAL);
            }
            tell(Component.literal("Карн поднимает павших стражей!").withStyle(ChatFormatting.DARK_AQUA));
        });
    }

    /** Во тьме: свет гаснет; каждую секунду холод бьёт всех, кто не стоит у душевной жаровни. */
    private void darkness(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Карн гасит свет! Встаньте к душевным жаровням.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        int secs = (int) Math.round(p("dark_seconds"));
        c.at(c.ticks, () -> {
            for (ServerPlayer p : playersInArena()) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, secs * 20 + 20, 0, false, false));
            for (int s = 1; s <= secs; s++) {
                schedule(s * 20, () -> {
                    for (ServerPlayer p : playersInArena()) {
                        if (!nearBrazier(p)) {
                            BossDamage.hit(this, p, p("dark_units"));
                            ((ServerLevel) level()).sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                                    p.getX(), p.getY() + 1, p.getZ(), 6, 0.3, 0.5, 0.3, 0.01);
                        }
                    }
                    for (Vec3 b : braziers()) Fx.ring((ServerLevel) level(), b, 3.5f, 21, Fx.ICE);
                });
            }
        });
    }

    private List<Vec3> braziers() {
        Vec3 h = home();
        return List.of(h.add(BRAZIER, 0, BRAZIER), h.add(-BRAZIER, 0, BRAZIER), h.add(BRAZIER, 0, -BRAZIER), h.add(-BRAZIER, 0, -BRAZIER));
    }

    private boolean nearBrazier(ServerPlayer p) {
        for (Vec3 b : braziers()) if (p.position().distanceTo(b) <= 3.5) return true;
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tombs.isEmpty() || tickCount % 5 != 0) return;
        // Лёд разбит — освободить узника.
        for (var e : Map.copyOf(tombs).entrySet()) {
            var node = ((ServerLevel) level()).getEntity(e.getKey());
            if (node != null && node.isAlive()) continue;
            tombs.remove(e.getKey());
            ServerPlayer p = getServer().getPlayerList().getPlayer(e.getValue());
            if (p != null) {
                StatusService.removeFrom(p, StatusType.ROOT, getUUID());
                StatusService.removeFrom(p, StatusType.SILENCE, getUUID());
            }
        }
    }

    @Override
    protected void onPhase(int phase) {
        if (phase != 1 || !(level() instanceof ServerLevel)) return;
        PowerNode ph = spawnNode(ModEntities.PHYLACTERY.get(), secondArenaCenter().add(0, 1, -secondArenaRadius() + 3), p("phylactery_hp_units"),
                "Филактерия Карна");
        if (ph != null) raiseShield(List.of(ph));
        tell(Component.literal("Карн прячет душу в филактерии — разбейте её!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        level().playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1f, 1.2f);
    }

    @Override
    public void resetFight() {
        tombs.clear();
        super.resetFight();
    }

    /** Ледяная корка */
    private void frost(Cast c) {
        Mechanics.crumble(c, (int) p("frost_waves"), (int) p("frost_safe"), p("frost_radius"), p("frost_units"), 0x8FE3FF);
    }


    /** Лёд: осколки, иней и снежинки. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.shards(sl, at.add(0, 0.3, 0), radius, Math.max(6, Math.round(radius * 4)), 16, Fx.ICE);
        Fx.spiral(sl, at, radius * 0.5f, 1.6f, 10, 0xD8F4FF);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, at.x, at.y + 0.6, at.z,
                Math.round(10 + radius * 4), radius * 0.4, 0.3, radius * 0.4, 0.02);
    }

    /**
     * Ледяная тропа: по ледяному сгустку к каждому — катятся по полу, дорожка видна заранее.
     * Задетого прихватывает инеем (замедление на 2 с).
     */
    private void frostpath(Cast c) {
        triggerAnim("main", "cast");
        List<ServerPlayer> all = new java.util.ArrayList<>(playersInArena());
        java.util.Collections.shuffle(all, new java.util.Random(random.nextLong()));
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            Vec3 dir = all.get(i).position().subtract(position());
            Cast lane = testCast(c.ticks + i * 8);
            Mechanics.missile(lane, position(), dir, p("frostpath_speed"), arenaRadius() * 2, 1.3f, p("frostpath_units"), Fx.ICE,
                    pl -> StatusService.apply(pl, Status.of(StatusType.SLOW, "lich_frostpath", getUUID(), 0.4,
                            level().getGameTime(), 40)));
        }
        level().playSound(null, blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /**
     * Зов вьюги (Тронный зал): {@code blizzard_seconds} секунд кто за секунду прошёл меньше
     * {@code blizzard_move} блоков, тот замерзает. Стоять нельзя — нужно кружить.
     */
    private void blizzard(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Карн зовёт вьюгу: кто стоит — замерзает. Двигайтесь!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        ServerLevel sl = (ServerLevel) level();
        int secs = (int) Math.round(p("blizzard_seconds"));
        Telegraph.note(c, "move", 0);
        Map<UUID, Vec3> last = new HashMap<>();
        Fx.vortex(sl, home(), (float) arenaRadius() * 0.7f, 7, c.ticks + secs * 20, Fx.ICE);
        c.at(c.ticks, () -> {
            for (ServerPlayer p : playersInArena()) last.put(p.getUUID(), p.position());
        });
        for (int s = 1; s <= secs; s++) {
            c.at(c.ticks + s * 20, () -> {
                for (ServerPlayer p : playersInArena()) {
                    Vec3 prev = last.put(p.getUUID(), p.position());
                    sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.6, p.getZ(),
                            8, 0.6, 0.4, 0.6, 0.02);
                    if (prev == null || prev.distanceTo(p.position()) >= p("blizzard_move")) continue;
                    BossDamage.hit(this, p, p("blizzard_units"));
                    StatusService.apply(p, Status.of(StatusType.SLOW, "lich_blizzard", getUUID(), 0.3, level().getGameTime(), 25));
                    p.displayClientMessage(Component.literal("Вы замерзаете — двигайтесь!").withStyle(ChatFormatting.AQUA), true);
                }
            });
        }
    }
}
