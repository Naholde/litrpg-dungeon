package com.andrey.litrpg.gear.item;

import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Свиток забвения: вернуть все вложенные очки характеристик. */
public class ScrollItem extends Item {
    public ScrollItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        if (p instanceof ServerPlayer sp) {
            PlayerService.resetStats(sp);
            SystemChat.send(sp, Component.literal("Свиток забвения: все очки характеристик возвращены (окно K)."));
            level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ПКМ — вернуть все вложенные очки характеристик").withStyle(ChatFormatting.AQUA));
    }
}
