package com.andrey.litrpg.combat;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.skill.SkillService;
import com.andrey.litrpg.skill.Style;
import com.andrey.litrpg.skill.StyleTracker;
import com.andrey.litrpg.skill.Zones;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import com.andrey.litrpg.status.StatusService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Подключение боя к событиям игры. */
public final class CombatEvents {
    private CombatEvents() {
    }

    public static void register(IEventBus game) {
        game.addListener(DamageService::onIncoming);
        game.addListener(CombatEvents::afterDamage);
        game.addListener(com.andrey.litrpg.gear.LootService::onDeath);
        game.addListener(com.andrey.litrpg.gear.LootService::onDrops);
        // Стрела из лука Системы бьёт силой лука (ванильная стрела ≈ 3 × базовый урон при полном натяжении).
        game.addListener((net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) -> {
            if (e.getLevel().isClientSide() || !(e.getEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow a)) return;
            if (!(a.getOwner() instanceof ServerPlayer p)) return;
            var bow = p.getMainHandItem().get(com.andrey.litrpg.core.ModComponents.GEAR.get());
            if (bow == null) bow = p.getOffhandItem().get(com.andrey.litrpg.core.ModComponents.GEAR.get());
            if (bow != null && bow.kind() == com.andrey.litrpg.gear.GearKind.BOW) a.setBaseDamage(bow.base() / 3.0);
        });
        game.addListener((LivingChangeTargetEvent e) -> ThreatService.onChangeTarget(e));
        game.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity().level().isClientSide || !(e.getEntity() instanceof LivingEntity l)) return;
            StatusService.tick(l);
            if (l instanceof Mob m && m.tickCount % 10 == 0) ThreatService.tick(m);
        });
        game.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 10 == 0) PartyService.sync(e.getServer());
            for (var level : e.getServer().getAllLevels()) Zones.tick(level);
        });
    }

    /** Урон уже нанесён (после брони и поглощения): цифры и угроза. */
    private static void afterDamage(LivingDamageEvent.Post e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        float dealt = e.getNewDamage();
        boolean crit = DamageService.wasCrit(victim);
        LivingEntity attacker = e.getSource().getEntity() instanceof LivingEntity l ? l : null;
        Popups.damage(victim, attacker, dealt, crit);
        DamageService.afterHit(victim, e.getSource(), dealt);
        if (victim instanceof Mob mob && attacker instanceof ServerPlayer p) ThreatService.add(mob, p, dealt);
        if (dealt > 0 && attacker instanceof ServerPlayer p && attacker != victim && !(victim instanceof ServerPlayer)) {
            SkillService.onDealt(p);
            Entity direct = e.getSource().getDirectEntity();
            StyleTracker.add(p, direct instanceof AbstractArrow ? Style.RANGED
                    : direct instanceof MagicBolt ? Style.MAGIC : direct == p ? Style.MELEE : Style.SKILLS, 1);
        }
        if (dealt > 0 && victim instanceof ServerPlayer vp && attacker != null && !(attacker instanceof ServerPlayer)) {
            SkillService.onTaken(vp);
            StyleTracker.add(vp, Style.TANKING, 1);
        }
    }
}
