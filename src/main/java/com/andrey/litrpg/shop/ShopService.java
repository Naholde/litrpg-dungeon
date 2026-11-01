package com.andrey.litrpg.shop;

import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.ShopBalance;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearGen;
import com.andrey.litrpg.gear.Rarity;
import com.andrey.litrpg.gear.Wallet;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.ShopPayload;
import com.andrey.litrpg.player.PlayerData;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Лавка торговца. Товар у каждого игрока свой: 6 вещей под его роль (2 необычных, 3 редких, 1 эпическая),
 * каждая — один раз за этап; плюс эликсиры и свитки; наковальня и услуги класса.
 */
public final class ShopService {
    private static final Rarity[] STOCK = {Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.RARE, Rarity.RARE, Rarity.EPIC};

    private ShopService() {
    }

    /** Товар игрока у этого торговца — одинаковый при каждом открытии (зерно: торговец + игрок + этап). */
    public static List<ItemStack> stock(ServerPlayer p, TraderEntity t) {
        RandomSource rnd = RandomSource.create(t.getUUID().getMostSignificantBits() ^ p.getUUID().getLeastSignificantBits()
                ^ (31L * t.stage()));
        List<ItemStack> out = new ArrayList<>();
        PlayerData d = PlayerService.data(p);
        for (Rarity r : STOCK) out.add(GearGen.roll(rnd, d.role(), t.goodsLevel(), r));
        return out;
    }

    private static String key(TraderEntity t, int i) {
        return t.getUUID() + "#" + t.stage() + "#" + i;
    }

    private static ShopBalance prices() {
        return Balance.get(Balance.SHOP);
    }

    public static void open(ServerPlayer p, TraderEntity t) {
        Net.send(p, payload(p, t));
    }

    static ShopPayload payload(ServerPlayer p, TraderEntity t) {
        ShopBalance s = prices();
        PlayerData d = PlayerService.data(p);
        List<ShopPayload.Entry> entries = new ArrayList<>();
        List<ItemStack> stock = stock(p, t);
        for (int i = 0; i < stock.size(); i++) {
            GearData g = stock.get(i).get(ModComponents.GEAR.get());
            long price = g != null ? s.itemPrice(g.rarity(), g.level()) : 0;
            entries.add(new ShopPayload.Entry(stock.get(i), price, d.bought(key(t, i))));
        }
        entries.add(new ShopPayload.Entry(new ItemStack(ModItems.ELIXIR.get()), elixirPrice(t), false));
        entries.add(new ShopPayload.Entry(new ItemStack(ModItems.SCROLL_OBLIVION.get()), Math.round(s.scroll()), false));
        entries.add(new ShopPayload.Entry(new ItemStack(ModItems.CACHE_KEY.get()), keyPrice(t), false));
        GearData held = p.getMainHandItem().get(ModComponents.GEAR.get());
        long reroll = held != null ? rerollPrice(held) : -1;
        long upgrade = held != null && held.level() < d.level() ? upgradePrice(held, d.level()) : -1;
        return new ShopPayload(t.getId(), t.stage(), Wallet.total(p), entries, reroll, upgrade,
                Math.round(s.classChangePerLevel() * d.level()), Math.round(s.rankResetPerLevel() * d.level()));
    }

    private static long keyPrice(TraderEntity t) {
        return Math.round(prices().keyPerLevel() * t.goodsLevel());
    }

    private static long elixirPrice(TraderEntity t) {
        return Math.round(prices().elixirPerLevel() * t.goodsLevel());
    }

    private static long rerollPrice(GearData g) {
        return Math.round(prices().rerollPerLevel() * g.level() * (1 + g.rerolls()) * prices().rarityPrice().getOrDefault(g.rarity(), 1.0));
    }

    private static long upgradePrice(GearData g, int playerLevel) {
        int to = Math.min(playerLevel, g.level() + 5);
        return Math.round(prices().upgradePerLevel() * to * prices().rarityPrice().getOrDefault(g.rarity(), 1.0));
    }

    // ------------------------------------------------------------------ действия

    public static void act(ServerPlayer p, int traderId, ShopPayload.Action action, int index) {
        if (!(p.level().getEntity(traderId) instanceof TraderEntity t) || t.distanceTo(p) > 8) return;
        ShopBalance s = prices();
        PlayerData d = PlayerService.data(p);
        switch (action) {
            case BUY -> {
                List<ItemStack> stock = stock(p, t);
                if (index < 0 || index >= stock.size() + 3) return;
                if (index == stock.size()) {
                    if (pay(p, elixirPrice(t))) give(p, new ItemStack(ModItems.ELIXIR.get()));
                } else if (index == stock.size() + 1) {
                    if (pay(p, Math.round(s.scroll()))) give(p, new ItemStack(ModItems.SCROLL_OBLIVION.get()));
                } else if (index == stock.size() + 2) {
                    if (pay(p, keyPrice(t))) give(p, new ItemStack(ModItems.CACHE_KEY.get()));
                } else {
                    if (d.bought(key(t, index))) return;
                    GearData g = stock.get(index).get(ModComponents.GEAR.get());
                    if (g != null && pay(p, s.itemPrice(g.rarity(), g.level()))) {
                        PlayerService.markBought(p, key(t, index));
                        give(p, stock.get(index));
                    }
                }
            }
            case REROLL -> {
                ItemStack held = p.getMainHandItem();
                GearData g = held.get(ModComponents.GEAR.get());
                if (g != null && pay(p, rerollPrice(g))) {
                    var fresh = GearGen.make(g.kind(), g.level(), g.rarity(), d.role(), p.getRandom());
                    GearData nd = fresh.get(ModComponents.GEAR.get());
                    nd = new GearData(g.kind(), g.level(), g.rarity(), g.base(), nd.stats(), nd.affixes(), g.unique(), g.rerolls() + 1);
                    held.set(ModComponents.GEAR.get(), nd);
                    SystemChat.send(p, Component.literal("Наковальня: вещь перекована — свойства новые.").withStyle(ChatFormatting.GOLD));
                    PlayerService.refresh(p);
                }
            }
            case UPGRADE -> {
                ItemStack held = p.getMainHandItem();
                GearData g = held.get(ModComponents.GEAR.get());
                if (g != null && g.level() < d.level() && pay(p, upgradePrice(g, d.level()))) {
                    int to = Math.min(d.level(), g.level() + 5);
                    double k = (double) to / g.level();
                    var stats = new java.util.EnumMap<com.andrey.litrpg.player.Stat, Double>(com.andrey.litrpg.player.Stat.class);
                    g.stats().forEach((st, v) -> stats.put(st, Math.round(v * k * 10) / 10.0));
                    double base = GearGen.base(g.kind(), to, Balance.get(Balance.GEAR).of(g.rarity()).power(), Balance.get(Balance.GEAR));
                    GearData nd = new GearData(g.kind(), to, g.rarity(), Math.round(base * 10) / 10.0, stats, g.affixes(), g.unique(), g.rerolls());
                    ItemStack fresh = GearGen.stack(nd, held.getHoverName().getString());
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fresh);
                    SystemChat.send(p, Component.literal("Наковальня: уровень вещи поднят до " + to + ".").withStyle(ChatFormatting.GOLD));
                    PlayerService.refresh(p);
                }
            }
            case CLASS_CHANGE -> {
                if (!d.classId().isEmpty() && pay(p, Math.round(s.classChangePerLevel() * d.level()))) {
                    PlayerService.setClass(p, "");
                }
            }
            case RANK_RESET -> {
                if (!d.ranksView().isEmpty() && pay(p, Math.round(s.rankResetPerLevel() * d.level()))) {
                    PlayerService.resetRanks(p);
                    SystemChat.send(p, Component.literal("Ранги навыков забыты — очки класса возвращены."));
                }
            }
        }
        open(p, t);
    }

    /** Shift+ПКМ: продать вещь в руке за долю цены. */
    public static void sell(ServerPlayer p, TraderEntity t) {
        ItemStack held = p.getMainHandItem();
        GearData g = held.get(ModComponents.GEAR.get());
        if (g == null) {
            SystemChat.send(p, Component.literal("Торговец покупает только вещи Системы. Возьмите вещь в руку и Shift+ПКМ.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        long price = Math.max(1, Math.round(prices().itemPrice(g.rarity(), g.level()) * prices().sellShare()));
        Component name = held.getHoverName();
        held.shrink(1);
        Wallet.give(p, price);
        SystemChat.send(p, Component.literal("Продано: ").append(name).append(Component.literal(" за " + Wallet.format(price))));
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.NEUTRAL, 1f, 1f);
    }

    private static boolean pay(ServerPlayer p, long price) {
        if (Wallet.take(p, price)) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 0.8f, 1f);
            return true;
        }
        SystemChat.send(p, Component.literal("Не хватает монет: нужно " + Wallet.format(price) + ", у вас "
                + Wallet.format(Wallet.total(p)) + ".").withStyle(ChatFormatting.RED));
        return false;
    }

    private static void give(ServerPlayer p, ItemStack s) {
        if (!p.getInventory().add(s.copy())) p.drop(s.copy(), false);
    }
}
