package com.andrey.litrpg.gear.item;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Эликсир жизни: пить 1,6 с; сразу 20% здоровья и ещё 15% за 3 с. Общий откат зелий — 60 с.
 * Слабее лекаря намеренно: зелье — на крайний случай, а не замена поддержке.
 */
public class ElixirItem extends Item {
    private static final double NOW = 0.2, OVER = 0.15;
    private static final int COOLDOWN = 60 * 20, DRINK = 32;

    public ElixirItem(Properties props) {
        super(props.stacksTo(16));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, p, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DRINK;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity e) {
        if (e instanceof ServerPlayer p) {
            float max = p.getMaxHealth();
            HealService.healFixed(null, p, max * NOW);
            StatusService.apply(p, Status.periodic(StatusType.HOT, "elixir", p.getUUID(), max * OVER / 3,
                    level.getGameTime(), 60, 20));
            p.getCooldowns().addCooldown(this, COOLDOWN);
            level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1.1f);
            if (!p.getAbilities().instabuild) stack.shrink(1);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Сразу " + Derived.trim(NOW * 100) + "% здоровья и ещё " + Derived.trim(OVER * 100)
                + "% за 3 с").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Пить " + Derived.trim(DRINK / 20.0) + " с · откат зелий 60 с").withStyle(ChatFormatting.GRAY));
    }
}
