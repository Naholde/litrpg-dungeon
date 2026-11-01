package com.andrey.litrpg.combat.item;

import com.andrey.litrpg.combat.entity.MagicBolt;
import com.andrey.litrpg.player.Derived;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Посох: ЛКМ выпускает магический снаряд (вместо удара). Урон усиливается Интеллектом (сила навыков).
 * Нажатие ловит клиент и шлёт {@code StaffCastPayload}; сервер проверяет откат и стреляет.
 */
public class StaffItem extends Item {
    private final double damage;
    private final int cooldownTicks;
    private final int color;

    public StaffItem(Properties props, double damage, int cooldownTicks, int color) {
        super(props.stacksTo(1));
        this.damage = damage;
        this.cooldownTicks = cooldownTicks;
        this.color = color;
    }

    public double damage() {
        return damage;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    /** Выстрел с сервера: проверка отката, снаряд, звук. */
    public boolean cast(ServerPlayer p, ItemStack stack) {
        if (p.getCooldowns().isOnCooldown(this)) return false;
        p.getCooldowns().addCooldown(this, cooldownTicks);
        MagicBolt bolt = new MagicBolt(p.level(), p, damage, color);
        bolt.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.8f, 0.4f);
        p.level().addFreshEntity(bolt);
        p.swing(InteractionHand.MAIN_HAND, true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.6f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("ЛКМ — магический снаряд").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Урон: " + Derived.trim(damage) + " + сила навыков (Интеллект)")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Перезарядка: " + Derived.trim(cooldownTicks / 20.0) + " с")
                .withStyle(ChatFormatting.GRAY));
    }
}
