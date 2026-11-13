package com.andrey.litrpg.run;

import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.LootService;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.gear.Wallet;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Тайники. Каждый участник открывает тайник один раз за прохождение и получает свою добычу:
 * 1 — монеты и вещь не ниже редкой; 2 — запертый (нужен ключ), не ниже эпической; 3 — секретка: не ниже эпической, шанс
 * легендарной, отряду — опыт «за загадки»; 4 — божественный: божественная вещь (их всего несколько на всё подземелье).
 */
public final class Caches {
    private Caches() {
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        RunState s = RunService.state(p.server);
        Layout.Stage st = RunService.currentStage(p.server);
        if (st == null || !s.member(p.getUUID()) || s.isSpirit(p.getUUID())) {
            SystemChat.send(p, "Тайник открывается только участникам прохождения.");
            return;
        }
        Layout.Cache cache = st.caches().stream().filter(c -> c.at().equals(pos)).findFirst().orElse(null);
        if (cache == null) return;
        String mine = "open:" + cache.id() + ":" + p.getUUID();
        if (s.claims.contains(mine)) {
            SystemChat.send(p, Component.literal("Этот тайник вы уже открыли.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (cache.tier() == 2 && !takeKey(p)) {
            SystemChat.send(p, Component.literal("Тайник заперт. Нужен «Ключ тайника» (торговец привала, мини-хранители).")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        RunStats.cache(p);
        s.claims.add(mine);
        s.setDirty();
        loot(p, st, cache);
        if (cache.secret()) Rewards.claim(p.server, s, "cache:" + cache.id(), Rewards.extraXp(st), "найдена секретка");
    }

    private static boolean takeKey(ServerPlayer p) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (it.is(ModItems.CACHE_KEY.get())) {
                it.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void loot(ServerPlayer p, Layout.Stage st, Layout.Cache cache) {
        int level = st.toLevel();
        double find = PlayerService.breakdown(p).get(Derived.FIND) + Difficulty.active(p.server).lootFind();
        var role = PlayerService.data(p).role();
        long coins = Math.round(Balance.get(Balance.GEAR).drops().coinsPerLevel() * level * (8 + 6 * cache.tier()) * (1 + find));
        Wallet.give(p, coins);
        Rarity min = switch (cache.tier()) {
            case 1 -> Rarity.RARE;
            case 4 -> Rarity.DIVINE;
            default -> Rarity.EPIC;
        };
        Rarity r = cache.tier() == 4 ? Rarity.DIVINE : GearGen.rollRarity(p.getRandom(), find + (cache.tier() == 3 ? 0.5 : 0), min);
        if (cache.tier() == 3 && r.ordinal() < Rarity.LEGENDARY.ordinal() && p.getRandom().nextDouble() < 0.25) r = Rarity.LEGENDARY;
        ItemStack item = GearGen.rollFor(p, level, r);
        LootService.dropFor(level, p, p, item);
        if (cache.tier() >= 3) {
            ItemEntity extra = new ItemEntity(p.level(), p.getX(), p.getY() + 0.5, p.getZ(), new ItemStack(ModItems.ELIXIR.get()));
            extra.setTarget(p.getUUID());
            p.level().addFreshEntity(extra);
        }
        Vec3 c = Vec3.atCenterOf(cache.at());
        Fx.burst(p.serverLevel(), c, 1.2f, 12, cache.tier() == 4 ? Fx.HOLY : Fx.ARCANE);
        p.serverLevel().playSound(null, cache.at(), SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
        if (cache.tier() >= 3) p.serverLevel().playSound(null, cache.at(), SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.6f);
        String[] names = {"", "Тайник", "Запертый тайник", "Тайник секретки", "Божественный тайник"};
        SystemChat.send(p, Component.literal(names[Math.min(4, cache.tier())] + " открыт: " + Wallet.format(coins) + ".")
                .withStyle(cache.tier() == 4 ? ChatFormatting.GOLD : ChatFormatting.AQUA));
    }

    /** Отряд победил мини-хранителя: каждому — ключ тайника. */
    static void giveKey(ServerPlayer p) {
        ItemStack key = new ItemStack(ModItems.CACHE_KEY.get());
        if (!p.getInventory().add(key)) p.drop(key, false);
        SystemChat.send(p, Component.literal("Получен ключ тайника.").withStyle(ChatFormatting.GOLD));
    }
}
