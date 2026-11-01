package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.ModComponents;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Stat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Подсказка вещи Системы: редкость и уровень, урон или броня, характеристики, свойства. */
@EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
public final class GearTooltips {
    private GearTooltips() {
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) {
        GearData d = e.getItemStack().get(ModComponents.GEAR.get());
        if (d == null) return;
        List<Component> t = e.getToolTip();
        int at = Math.min(1, t.size());
        int c = d.rarity().color & 0xFFFFFF;
        List<Component> add = new java.util.ArrayList<>();
        add.add(Component.literal(d.rarity().title + " · " + d.kind().title.toLowerCase() + " · ур. " + d.level()).withColor(c));
        if (d.kind().isWeapon()) {
            String what = switch (d.kind().weapon) {
                case MELEE -> " (Сила)";
                case RANGED -> " (Ловкость)";
                default -> " (Интеллект)";
            };
            add.add(Component.literal("Урон: " + Derived.trim(d.base()) + what).withStyle(ChatFormatting.WHITE));
        } else if (d.base() > 0) {
            add.add(Component.literal("Броня: " + Derived.trim(d.base())).withStyle(ChatFormatting.WHITE));
        }
        for (Stat s : Stat.ALL) {
            double v = d.stat(s);
            if (v != 0) add.add(Component.literal("+" + Derived.trim(v) + " " + s.title).withColor(0x9FE3FF));
        }
        for (GearData.Affix a : d.affixes()) {
            String val = a.type().derived.format == Derived.Format.MULT
                    ? "+" + Derived.trim(a.value() * 100) + "%" : a.type().derived.formatDelta(a.value());
            add.add(Component.literal(val + " " + a.type().text).withColor(0xFFD36B));
        }
        if (!d.unique().isEmpty()) {
            var trophy = com.andrey.litrpg.gear.Trophies.get(d.unique());
            add.add(Component.literal(trophy != null ? "Трофей: " + com.andrey.litrpg.boss.BossRegistry.get(trophy.boss()).name() : "Уникальная вещь")
                    .withColor(0xFFA733));
            if (trophy != null) {
                add.add(Component.literal("«" + trophy.lore() + "»").withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.GRAY));
            }
        }
        if (d.rerolls() > 0) add.add(Component.literal("Перекована: " + d.rerolls() + " раз").withStyle(ChatFormatting.DARK_GRAY));
        t.addAll(at, add);
        t.removeIf(line -> line.getString().startsWith("Неразрушим") || line.getString().equals("Unbreakable"));
    }
}
