package com.andrey.litrpg.boss.stage2;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
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
 * Морена, Ведьма Изнанки — финал Чащи. Порча полосой, «Страх» (напуганный не может бить и его гонит прочь, пока рядом
 * нет союзника — подойдите к нему), котёл-лужа, отражения (бить отражения, пока не залечили ведьму). На половине —
 * Изнанка: там «Вывернутая земля» — кольцо, круг, кольцо подряд, нужно танцевать.
 */
public class Morena extends BossEntity {
    private static final int HEX = 0xB04AFF;
    /** Напуганные: игрок → когда страх кончится. */
    private final Map<UUID, Long> feared = new HashMap<>();

    public Morena(EntityType<? extends Morena> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.MORENA;
    }

    @Override
    public String modelId() {
        return "morena";
    }

    @Override
    protected String secondArenaName() {
        return "Изнанку";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.75, 0.25};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Круг ведьмы", "witchcircle", 1, false, b -> true, this::witchCircle),
            new Ability("Изнаночные клетки", "checker", 0, false, b -> b.phase() >= 1, this::checker),
            new Ability("Лес Изнанки", "grove", 0, false, b -> true, this::grove),
            new Ability("Хоровод теней", "dance", 0, false, b -> b.phase() >= 1, this::dance),
            new Ability("Сглаз", "evil", 1, false, b -> b.playersInArena().size() > 1, this::evilEye),
            new Ability("Око Изнанки", "eye", 0, false, b -> true, this::eye),
            new Ability("Ползучий котёл", "crawl", 1, false, b -> true, this::crawl),
            new Ability("Вывернутая земля", "inside", 0, false, b -> relocated(), this::insideOut),
            new Ability("Страх", "fear", 1, false, b -> b.playersInArena().size() > 1, this::fear),
            new Ability("Котёл", "cauldron", 2, false, b -> poolCount() < 3, this::cauldron),
            new Ability("Порча", "hex", 3, false, b -> true, this::hex));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void hex(Cast c) {
        triggerAnim("main", "cast");
        ServerPlayer p = randomNonTarget();
        Vec3 to = p != null ? p.position() : c.target.position();
        Vec3 dir = to.subtract(position()).multiply(1, 0, 1).normalize();
        Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 4)), 3f, p("hex_units"));
    }

    /** Страх: напуганный не бьёт и бежит от ведьмы; союзник рядом (3 блока) снимает страх. */
    private void fear(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        triggerAnim("main", "cast");
        Fx.beam((ServerLevel) level(), this, p, 0.2f, c.ticks, HEX);
        c.at(c.ticks, () -> {
            if (!p.isAlive() || p.isSpectator()) return;
            int dur = (int) Math.round(p("fear_seconds") * 20);
            StatusService.apply(p, Status.of(StatusType.DAMAGE_DEALT, "morena_fear", getUUID(), 0.0, level().getGameTime(), dur));
            feared.put(p.getUUID(), level().getGameTime() + dur);
            Fx.mark((ServerLevel) level(), p, "!", dur, HEX);
            tell(Component.literal(p.getGameProfile().getName() + " охвачен страхом — подойдите к нему, чтобы снять!")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.8f);
        });
    }

    private void cauldron(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        Vec3 at = p.position();
        Fx.disc((ServerLevel) level(), at, 3, c.ticks, HEX, true);
        c.at(c.ticks, () -> addPool(at, 3, 0, 3, (int) Math.round(p("cauldron_seconds")), p("cauldron_units"), HEX,
                pl -> StatusService.apply(pl, Status.of(StatusType.SLOW, "morena_cauldron", getUUID(), 0.4, level().getGameTime(), 30))));
    }

    /** Изнанка: кольцо (встать к ведьме) → круг (отойти) → снова кольцо. */
    private void insideOut(Cast c) {
        float r = (float) arenaRadius();
        Telegraph.donut(c, position(), 4, r + 2, p("inside_units"));
        Vec3 at = position();
        schedule(c.ticks + 10, () -> {
            // Из-под ведьмы до края круга — 8 блоков: 0,5 с на реакцию и 1,6 с бега.
            Cast second = testCast(55);
            Telegraph.circle(second, at, 8, p("inside_units"));
            schedule(55, () -> {
                Cast third = testCast(48);
                Telegraph.donut(third, at, 4, r + 2, p("inside_units"));
            });
        });
        tell(Component.literal("Земля выворачивается: к ведьме — от ведьмы — к ведьме!").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
    }

    private void images() {
        int n = Math.max(2, partySize() - 1);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            DungeonMob m = summon(Bestiary.FOREST_WITCH, home().add(Math.cos(a) * 6, 0.2, Math.sin(a) * 6), MobRank.ELITE);
            if (m != null) m.setTitle("Отражение Морены");
        }
        tell(Component.literal("Морена множится! Отражения лечат её — убейте их.").withStyle(ChatFormatting.LIGHT_PURPLE));
        level().playSound(null, blockPosition(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 2f, 0.8f);
    }

    @Override
    protected void onPhase(int phase) {
        images();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || feared.isEmpty() || tickCount % 20 != 0) return;
        long now = level().getGameTime();
        for (var e : Map.copyOf(feared).entrySet()) {
            ServerPlayer p = getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null || now >= e.getValue() || !p.isAlive()) {
                feared.remove(e.getKey());
                continue;
            }
            boolean comforted = playersInArena().stream().anyMatch(o -> o != p && o.distanceTo(p) <= 3);
            if (comforted) {
                feared.remove(e.getKey());
                StatusService.removeFrom(p, StatusType.DAMAGE_DEALT, getUUID());
                Fx.burst((ServerLevel) level(), p.position().add(0, 1, 0), 1.2f, 8, Fx.HOLY);
                continue;
            }
            // гонит прочь от ведьмы
            Vec3 away = p.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.9);
            p.push(away.x, 0.25, away.z);
            p.hurtMarked = true;
        }
    }

    @Override
    public void resetFight() {
        feared.clear();
        super.resetFight();
    }

    /** Око Изнанки */
    private void eye(Cast c) {
        Mechanics.gaze(c, (int) p("eye_seconds"), p("eye_angle"), p("eye_units"), 0xB84AD8);
    }

    /** Ползучий котёл */
    private void crawl(Cast c) {
        Mechanics.hunt(c, (int) p("crawl_seconds"), p("crawl_radius"), p("crawl_units"), 0x7A2A8A);
    }


    /** Лес Изнанки */
    private void grove(Cast c) {
        MapMechanics.pillars(c, (int) p("grove_count"), (int) p("grove_height"), (int) p("grove_seconds"), Blocks.DARK_OAK_LOG.defaultBlockState());
    }


    /** Изнанка: лиловый дым, тени, хлопья пепла. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, HEX);
        Fx.spiral(sl, at, radius * 0.5f, 2f, 12, 0x2A0F3A);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SQUID_INK, at.x, at.y + 0.5, at.z,
                Math.round(6 + radius * 3), radius * 0.4, 0.3, radius * 0.4, 0.02);
    }

    /**
     * Сглаз: на одного-двух бойцов ложится глаз. {@code evil_seconds} секунд нельзя смотреть на Морену:
     * кто посмотрел — получает урон и страх (снимает союзник рядом, как обычно).
     */
    private void evilEye(Cast c) {
        List<ServerPlayer> pool = new java.util.ArrayList<>(playersInArena());
        pool.remove(getTarget());
        java.util.Collections.shuffle(pool, new java.util.Random(random.nextLong()));
        int n = Math.min(pool.size(), partySize() >= 5 ? 2 : 1);
        if (n == 0) return;
        triggerAnim("main", "cast");
        int dur = (int) Math.round(p("evil_seconds") * 20);
        Telegraph.note(c, "look", 0);
        for (ServerPlayer p : pool.subList(0, n)) {
            Fx.beam((ServerLevel) level(), this, p, 0.15f, c.ticks, HEX);
            c.at(c.ticks, () -> {
                Fx.mark((ServerLevel) level(), p, "◉", dur, HEX);
                p.displayClientMessage(Component.literal("Сглаз! " + Math.round(p("evil_seconds")) + " с не смотрите на Морену!")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
            });
            boolean[] caught = {false};
            for (int t = 10; t <= dur; t += 10) {
                c.at(c.ticks + t, () -> {
                    if (caught[0] || !p.isAlive() || p.isSpectator()) return;
                    Vec3 to = position().add(0, getBbHeight() * 0.6, 0).subtract(p.getEyePosition()).normalize();
                    if (p.getViewVector(1).dot(to) < 0.55 || !p.hasLineOfSight(this)) return;
                    caught[0] = true;
                    com.andrey.litrpg.boss.BossDamage.hit(this, p, p("evil_units"));
                    int fearDur = (int) Math.round(p("fear_seconds") * 20);
                    StatusService.apply(p, Status.of(StatusType.DAMAGE_DEALT, "morena_fear", getUUID(), 0.0, level().getGameTime(), fearDur));
                    feared.put(p.getUUID(), level().getGameTime() + fearDur);
                    tell(Component.literal(p.getGameProfile().getName() + " взглянул на Морену — страх! Подойдите к нему.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                    p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 0.8f);
                });
            }
        }
    }

    /** Хоровод теней (с 75%): пять огней кружат вокруг ведьмы — пропускайте их, шагая в просветы. */
    private void dance(Cast c) {
        triggerAnim("main", "cast");
        tell(Component.literal("Хоровод теней! Огни кружат вокруг Морены — шагайте в просветы.").withStyle(ChatFormatting.LIGHT_PURPLE));
        Mechanics.orbit(c, 5, 6.5, (int) p("dance_seconds"), 45, 1.4f, p("dance_units"), HEX,
                p -> StatusService.apply(p, Status.of(StatusType.SLOW, "morena_dance", getUUID(), 0.35, level().getGameTime(), 30)));
    }

    /** Круг ведьмы: вокруг Морены на {witchcircle_seconds} встаёт круг порчи — танк, выведите её из него. */
    private void witchCircle(Cast c) {
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.circle(c, position(), 5.5f, (int) p("witchcircle_seconds"), p("witchcircle_units"), HEX, pl -> slow(pl, 0.3, 25));
        tell(Component.literal("Морена чертит круг — выведите её из него!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Изнаночные клетки (с 75%): пол — шахматная доска, горят то чёрные, то белые клетки. Перепрыгивайте. */
    private void checker(Cast c) {
        triggerAnim("main", "cast");
        for (int k = 0; k < 3; k++) {
            com.andrey.litrpg.boss.Ground.tiles(testCast(c.ticks + k * 60), home(), 6, 5f, k % 2 == 0, 3, p("checker_units"), HEX, null);
        }
        tell(Component.literal("Пол Изнанки — шахматная доска! Переходите на тёмные клетки.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
