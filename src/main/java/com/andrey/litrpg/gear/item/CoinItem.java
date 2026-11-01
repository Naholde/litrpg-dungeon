package com.andrey.litrpg.gear.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Монета: медная = 1, серебряная = 9 медных, золотая = 81 медная. Кошелёк — сами монеты в инвентаре. */
public class CoinItem extends Item {
    public final int value;

    public CoinItem(Properties props, int value) {
        super(props.stacksTo(99));
        this.value = value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        if (value > 1) tooltip.add(Component.literal("= " + value + " медных").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Платят торговцу на привале").withStyle(ChatFormatting.DARK_GRAY));
    }
}
