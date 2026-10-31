package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.Balance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Жизненный цикл персонажа: вход, перерождение, голод, восстановление вне боя. */
public final class PlayerEvents {
    private PlayerEvents() {
    }

    public static void register(IEventBus modBus, IEventBus game) {
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(PlayerEvents::raiseHealthCap));
        game.addListener((PlayerEvent.PlayerLoggedInEvent e) -> refresh(e.getEntity()));
        game.addListener((PlayerEvent.PlayerRespawnEvent e) -> refresh(e.getEntity()));
        game.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> refresh(e.getEntity()));
        game.addListener((ServerStartedEvent e) -> {
            // Здоровье восстанавливается только вне боя (своё правило) и от лечения — не от сытости.
            e.getServer().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false, e.getServer());
        });
        game.addListener(PlayerEvents::onTick);
        game.addListener(PlayerEvents::onDamage);
    }

    /** У Minecraft предел здоровья — 1024; у танка на 50-м уровне больше. */
    private static void raiseHealthCap() {
        if (Attributes.MAX_HEALTH.value() instanceof RangedAttribute r) r.maxValue = 1_000_000;
    }

    private static void refresh(net.minecraft.world.entity.player.Player p) {
        if (p instanceof ServerPlayer sp) PlayerService.refresh(sp);
    }

    private static void onTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0 || !p.isAlive()) return;
        // Голода нет: еда всегда полная (бег разрешён), но сытость не лечит — правило мира выше.
        p.getFoodData().setFoodLevel(20);
        p.getFoodData().setSaturation(0);
        if (!PlayerService.inCombat(p) && p.getHealth() < p.getMaxHealth()) {
            double rate = Balance.get(Balance.CHARACTER).regen().outOfCombatPerSecond();
            p.setHealth((float) Math.min(p.getMaxHealth(), p.getHealth() + p.getMaxHealth() * rate));
        }
    }

    private static void onDamage(LivingIncomingDamageEvent e) {
        if (e.getEntity() instanceof ServerPlayer victim) PlayerService.markCombat(victim);
        if (e.getSource().getEntity() instanceof ServerPlayer attacker) PlayerService.markCombat(attacker);
    }
}
