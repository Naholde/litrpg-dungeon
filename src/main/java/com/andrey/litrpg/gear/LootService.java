package com.andrey.litrpg.gear;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.GearBalance;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Личная добыча: с убитого монстра каждый участник боя получает своё. Монеты — сразу в кошелёк,
 * вещь — предметом на земле, который может поднять только он (и подсвеченным). Ванильный дроп убран.
 */
public final class LootService {
    private LootService() {
    }

    /** Ванильная добыча мобов (гнилая плоть, кости) в подземелье не нужна. */
    public static void onDrops(LivingDropsEvent e) {
        if (e.getEntity() instanceof Mob && !(e.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            e.getDrops().clear();
        }
    }

    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
        if (mob instanceof BossEntity) return; // трофеи хранителей — отдельно (фаза хранителей подземелья)
        Set<ServerPlayer> winners = participants(mob, e);
        if (winners.isEmpty()) return;
        GearBalance g = Balance.get(Balance.GEAR);
        for (ServerPlayer p : winners) {
            int level = PlayerService.data(p).level();
            double find = PlayerService.breakdown(p).get(Derived.FIND);
            long coins = Math.round(g.drops().coinsPerLevel() * level * (0.8 + level % 3 * 0.1 + p.getRandom().nextDouble() * 0.4)
                    * (1 + find));
            Wallet.give(p, coins);
            if (p.getRandom().nextDouble() < g.drops().gearChance() * (1 + find)) {
                Rarity r = GearGen.rollRarity(p.getRandom(), find, Rarity.COMMON);
                ItemStack item = GearGen.roll(p.getRandom(), PlayerService.data(p).role(), level, r);
                dropFor(level, p, mob, item);
            }
        }
    }

    /** Кто получает добычу: все, кто есть в таблице угрозы (дрался), плюс добивший и его отряд рядом. */
    private static Set<ServerPlayer> participants(Mob mob, LivingDeathEvent e) {
        Set<ServerPlayer> out = new LinkedHashSet<>();
        var server = mob.getServer();
        if (server == null) return out;
        if (mob.hasData(ModAttachments.THREAT)) {
            for (UUID id : ThreatService.table(mob).view().keySet()) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) out.add(p);
            }
        }
        if (e.getSource().getEntity() instanceof ServerPlayer killer) {
            out.add(killer);
            for (ServerPlayer m : PartyService.online(server, killer)) if (m.distanceTo(mob) < 32) out.add(m);
        }
        return out;
    }

    /** Вещь на земле только для одного игрока: поднять может лишь он, светится цветом редкости. */
    public static void dropFor(int level, ServerPlayer p, LivingEntity at, ItemStack item) {
        ItemEntity ie = new ItemEntity(at.level(), at.getX(), at.getY() + 0.5, at.getZ(), item);
        ie.setTarget(p.getUUID());
        ie.setGlowingTag(true);
        ie.setPickUpDelay(10);
        ie.setUnlimitedLifetime();
        at.level().addFreshEntity(ie);
        GearData d = item.get(ModComponents.GEAR.get());
        Component name = item.getHoverName();
        SystemChat.send(p, Component.literal("Добыча: ").append(name)
                .append(Component.literal(d != null ? "  (" + d.rarity().title.toLowerCase() + ", ур. " + d.level() + ")" : "")
                        .withColor(d != null ? d.rarity().color & 0xFFFFFF : 0xFFFFFF)));
    }
}
