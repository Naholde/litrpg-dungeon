package com.andrey.litrpg.combat;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Лечение. Цель: выбранный союзник (Ctrl+1…6) → тот, на кого смотришь → иначе сам.
 * Лечение в бою даёт угрозу (половину от вылеченного), она делится между врагами, которые воюют с целью.
 */
public final class HealService {
    private static final Map<ServerPlayer, UUID> SELECTED = new WeakHashMap<>();

    private HealService() {
    }

    public static void select(ServerPlayer healer, UUID target) {
        if (target == null) SELECTED.remove(healer);
        else SELECTED.put(healer, target);
    }

    public static UUID selected(ServerPlayer healer) {
        return SELECTED.get(healer);
    }

    /** Кого лечить: выбранного (если жив, рядом и в одном мире), иначе того, на кого смотришь, иначе себя. */
    public static LivingEntity resolveTarget(ServerPlayer healer, double range) {
        UUID sel = SELECTED.get(healer);
        if (sel != null) {
            ServerPlayer p = healer.server.getPlayerList().getPlayer(sel);
            if (p != null && p.isAlive() && !p.isSpectator() && p.level() == healer.level() && p.distanceTo(healer) <= range) {
                return p;
            }
        }
        LivingEntity looked = lookedAtAlly(healer, range);
        return looked != null ? looked : healer;
    }

    private static LivingEntity lookedAtAlly(ServerPlayer healer, double range) {
        Vec3 eye = healer.getEyePosition();
        Vec3 end = eye.add(healer.getViewVector(1).scale(range));
        HitResult block = healer.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, healer));
        if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(healer, eye, end,
                healer.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1),
                e -> e instanceof Player p && p != healer && p.isAlive() && !p.isSpectator(), eye.distanceToSqr(end));
        return hit != null ? (LivingEntity) hit.getEntity() : null;
    }

    /**
     * Вылечить: {@code base} × сила лечения лекаря. Возвращает сколько реально восстановлено (без перелечения).
     */
    public static float heal(ServerPlayer healer, LivingEntity target, double base) {
        return healFixed(healer, target, base * PlayerService.breakdown(healer).get(Derived.HEAL_POWER));
    }

    /**
     * Вылечить уже посчитанное количество (лечение со временем, вампиризм) — без силы лечения лекаря,
     * но с метками цели («нельзя лечиться» и т.п.).
     */
    public static float healFixed(ServerPlayer healer, LivingEntity target, double amount) {
        amount *= StatusService.product(target, StatusType.HEAL_TAKEN);
        amount *= com.andrey.litrpg.gear.GearEffects.healBonus(healer, target);
        if (amount <= 0 || !target.isAlive()) return 0;
        float before = target.getHealth();
        target.heal((float) amount);
        float real = target.getHealth() - before;
        com.andrey.litrpg.gear.GearEffects.afterHeal(healer, target, amount, real);
        com.andrey.litrpg.boss.FightLog.healed(healer, real);
        if (healer != null && real > 0 && target != healer) {
            com.andrey.litrpg.skill.StyleTracker.add(healer, com.andrey.litrpg.skill.Style.HEALING, 1);
        }
        Popups.heal(target, healer, (float) amount);
        if (real > 0) addHealThreat(healer, target, real);
        return real;
    }

    private static void addHealThreat(ServerPlayer healer, LivingEntity target, float real) {
        double range = Balance.get(Balance.COMBAT).threat().forgetRange();
        List<Mob> engaged = target.level().getEntitiesOfClass(Mob.class, target.getBoundingBox().inflate(range),
                m -> m.isAlive() && m.hasData(ModAttachments.THREAT)
                        && ThreatService.table(m).contains(target.getUUID()));
        if (engaged.isEmpty()) return;
        double share = Balance.get(Balance.COMBAT).healThreatShare() * real / engaged.size();
        for (Mob m : engaged) ThreatService.add(m, healer, share);
        PlayerService.markCombat(healer);
    }

    /** Союзник ли: тот же отряд (или сам). Для навыков, которые лечат «своих». */
    public static boolean ally(ServerPlayer a, LivingEntity b) {
        return a == b || (b instanceof ServerPlayer bp && PartyService.sameParty(a, bp));
    }
}
