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
            Cast second = testCast(30);
            Telegraph.circle(second, at, 8, p("inside_units"));
            schedule(40, () -> {
                Cast third = testCast(30);
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
}
