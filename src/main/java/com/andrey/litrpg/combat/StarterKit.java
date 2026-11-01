package com.andrey.litrpg.combat;

import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.player.Role;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;

/**
 * Стартовый набор роли — выдаётся один раз при выборе роли. Вещи Системы (с уровнем и редкостью) придут
 * в фазе добычи; это — простое снаряжение новичка.
 */
public final class StarterKit {
    private StarterKit() {
    }

    public static List<ItemStack> items(Role role, ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        switch (role) {
            case TANK -> {
                out.add(named(new ItemStack(Items.IRON_SWORD), "Меч стража"));
                out.add(named(new ItemStack(Items.SHIELD), "Щит стража"));
            }
            case DAMAGE -> {
                out.add(named(new ItemStack(Items.IRON_SWORD), "Клинок новичка"));
                ItemStack bow = named(new ItemStack(Items.BOW), "Лук новичка");
                var infinity = p.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY);
                bow.enchant(infinity, 1);
                out.add(bow);
                out.add(new ItemStack(Items.ARROW));
            }
            case SUPPORT -> {
                out.add(new ItemStack(ModItems.APPRENTICE_STAFF.get()));
                out.add(named(new ItemStack(Items.WOODEN_SWORD), "Нож травника"));
            }
            case NONE -> {
            }
        }
        out.forEach(StarterKit::unbreakable);
        return out;
    }

    public static void give(ServerPlayer p, Role role) {
        for (ItemStack s : items(role, p)) {
            if (!p.getInventory().add(s)) p.drop(s, false);
        }
    }

    private static ItemStack named(ItemStack s, String name) {
        s.set(DataComponents.ITEM_NAME, Component.literal(name));
        return s;
    }

    /** Снаряжение не изнашивается — правило мира. */
    public static void unbreakable(ItemStack s) {
        if (s.isDamageableItem() && !s.has(DataComponents.UNBREAKABLE)) s.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
    }
}
