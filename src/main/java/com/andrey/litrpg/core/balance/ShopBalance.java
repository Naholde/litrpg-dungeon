package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.gear.Rarity;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;

/** Цены торговца привала (в медных монетах). Файл {@code balance/shop.json}. */
public record ShopBalance(double pricePerLevel, Map<Rarity, Double> rarityPrice, double sellShare, double elixirPerLevel,
                          double scroll, double rerollPerLevel, double upgradePerLevel, double classChangePerLevel,
                          double rankResetPerLevel, double keyPerLevel) {

    public static final MapCodec<ShopBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("price_per_level").forGetter(ShopBalance::pricePerLevel),
            Codec.unboundedMap(Rarity.CODEC, Codec.DOUBLE).fieldOf("rarity_price").forGetter(ShopBalance::rarityPrice),
            Codec.DOUBLE.fieldOf("sell_share").forGetter(ShopBalance::sellShare),
            Codec.DOUBLE.fieldOf("elixir_per_level").forGetter(ShopBalance::elixirPerLevel),
            Codec.DOUBLE.fieldOf("scroll").forGetter(ShopBalance::scroll),
            Codec.DOUBLE.fieldOf("reroll_per_level").forGetter(ShopBalance::rerollPerLevel),
            Codec.DOUBLE.fieldOf("upgrade_per_level").forGetter(ShopBalance::upgradePerLevel),
            Codec.DOUBLE.fieldOf("class_change_per_level").forGetter(ShopBalance::classChangePerLevel),
            Codec.DOUBLE.fieldOf("rank_reset_per_level").forGetter(ShopBalance::rankResetPerLevel),
            Codec.DOUBLE.fieldOf("key_per_level").forGetter(ShopBalance::keyPerLevel)
    ).apply(i, ShopBalance::new));

    public static final ShopBalance DEFAULT;

    static {
        Map<Rarity, Double> r = new EnumMap<>(Rarity.class);
        r.put(Rarity.COMMON, 1.0);
        r.put(Rarity.UNCOMMON, 2.0);
        r.put(Rarity.RARE, 4.0);
        r.put(Rarity.EPIC, 10.0);
        r.put(Rarity.LEGENDARY, 25.0);
        r.put(Rarity.DIVINE, 60.0);
        DEFAULT = new ShopBalance(20, Map.copyOf(r), 0.25, 10, 300, 15, 25, 40, 15, 30);
    }

    public long itemPrice(Rarity rarity, int level) {
        return Math.round(pricePerLevel * Math.max(1, level) * rarityPrice.getOrDefault(rarity, 1.0));
    }
}
