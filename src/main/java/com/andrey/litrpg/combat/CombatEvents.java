package com.andrey.litrpg.combat;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.party.PartyService;
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
        game.addListener((LivingChangeTargetEvent e) -> ThreatService.onChangeTarget(e));
        game.addListener((EntityTickEvent.Post e) -> {
            if (e.getEntity() instanceof Mob m && !m.level().isClientSide && m.tickCount % 10 == 0) ThreatService.tick(m);
        });
        game.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 10 == 0) PartyService.sync(e.getServer());
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
        if (victim instanceof Mob mob && attacker instanceof ServerPlayer p) ThreatService.add(mob, p, dealt);
    }
}
