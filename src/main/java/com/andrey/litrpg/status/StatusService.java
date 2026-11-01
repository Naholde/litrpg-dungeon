package com.andrey.litrpg.status;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.StatusSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Наложение и тик меток со сроком. Влияние на урон и лечение — в {@code DamageService} и {@code HealService}. */
public final class StatusService {
    private static final ResourceLocation MOVE = LitRPG.id("status_move");
    private static final ResourceLocation JUMP = LitRPG.id("status_jump");

    private StatusService() {
    }

    public static StatusList list(LivingEntity e) {
        return e.getData(ModAttachments.STATUSES);
    }

    public static boolean has(LivingEntity e, StatusType t) {
        return e.hasData(ModAttachments.STATUSES) && list(e).has(t, e.level().getGameTime());
    }

    public static double product(LivingEntity e, StatusType t) {
        return e.hasData(ModAttachments.STATUSES) ? list(e).product(t, e.level().getGameTime()) : 1;
    }

    public static double sum(LivingEntity e, StatusType t) {
        return e.hasData(ModAttachments.STATUSES) ? list(e).sum(t, e.level().getGameTime()) : 0;
    }

    public static long now(LivingEntity e) {
        return e.level().getGameTime();
    }

    public static void apply(LivingEntity e, Status s) {
        if (e.level().isClientSide) return;
        if (e instanceof com.andrey.litrpg.boss.BossEntity boss) {
            if (s.type() == StatusType.STUN) boss.onStunned();
            if (boss.immuneTo(s.type())) return;
        }
        list(e).add(s);
        updateMovement(e);
        if (s.type() == StatusType.STUN && e instanceof Mob m) m.getNavigation().stop();
        if (e instanceof ServerPlayer p) sync(p);
    }

    public static void clear(LivingEntity e) {
        if (!e.hasData(ModAttachments.STATUSES)) return;
        list(e).clear();
        updateMovement(e);
        if (e instanceof ServerPlayer p) sync(p);
    }

    /** Каждый тик для тех, у кого есть метки: срабатывания, истечение, движение. */
    public static void tick(LivingEntity e) {
        if (!e.hasData(ModAttachments.STATUSES) || !(e.level() instanceof ServerLevel level)) return;
        StatusList l = list(e);
        long now = level.getGameTime();
        for (Status s : new ArrayList<>(l.all())) {
            if (s.interval() <= 0 || s.expired(now) || (s.until() - now) % s.interval() != 0) continue;
            Entity src = s.source() != null ? level.getEntity(s.source()) : null;
            if (s.type() == StatusType.DOT && e.isAlive()) {
                LivingEntity attacker = src instanceof LivingEntity l2 ? l2 : e;
                DamageService.deal(attacker, null, e, s.magnitude(), WeaponKind.FIXED);
            } else if (s.type() == StatusType.HOT && e.isAlive()) {
                HealService.healFixed(src instanceof ServerPlayer sp ? sp : null, e, s.magnitude());
            }
        }
        if (has(e, StatusType.STUN) && e instanceof Mob m) m.getNavigation().stop();
        if (l.expire(now)) {
            updateMovement(e);
            if (e instanceof ServerPlayer p) sync(p);
        } else if (e instanceof ServerPlayer p && e.tickCount % 20 == 0 && !l.isEmpty()) {
            sync(p);
        }
    }

    /** Скорость: оглушение и обездвиживание — стоп; замедление и ускорение — доли. */
    private static void updateMovement(LivingEntity e) {
        long now = now(e);
        StatusList l = list(e);
        boolean stopped = l.has(StatusType.STUN, now) || l.has(StatusType.ROOT, now);
        double speed = stopped ? -1 : Math.max(-0.9, l.sum(StatusType.SPEED, now) - l.sum(StatusType.SLOW, now));
        set(e.getAttribute(Attributes.MOVEMENT_SPEED), MOVE, speed);
        set(e.getAttribute(Attributes.JUMP_STRENGTH), JUMP, stopped ? -1 : 0);
    }

    private static void set(AttributeInstance inst, ResourceLocation id, double amount) {
        if (inst == null) return;
        inst.removeModifier(id);
        if (amount != 0) {
            inst.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    public static void sync(ServerPlayer p) {
        long now = now(p);
        List<StatusSyncPayload.Entry> out = new ArrayList<>();
        if (p.hasData(ModAttachments.STATUSES)) {
            for (Status s : list(p).all()) {
                if (s.expired(now)) continue;
                out.add(new StatusSyncPayload.Entry(s.type(), s.icon(), (int) (s.until() - now), (float) s.magnitude()));
            }
        }
        Net.send(p, new StatusSyncPayload(out));
    }

    /** Снять метки одного вида, наложенные этим источником (например, «Под прикрытие» при смерти танка). */
    public static void removeFrom(LivingEntity e, StatusType t, UUID source) {
        if (!e.hasData(ModAttachments.STATUSES)) return;
        list(e).remove(s -> s.type() == t && source.equals(s.source()));
        updateMovement(e);
    }
}
