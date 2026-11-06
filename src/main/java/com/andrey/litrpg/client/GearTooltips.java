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

    /**
     * Сравнение с надетым того же вида (фаза 13): основа, сумма характеристик и число свойств — зелёным,
     * если лучше, красным, если хуже. Для украшений — с самым слабым надетым того же вида.
     */
    private static java.util.Optional<Component> compare(GearData d, net.minecraft.world.item.ItemStack self) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return java.util.Optional.empty();
        GearData[] worn = {null};
        boolean[] isSelf = {false};
        com.andrey.litrpg.gear.PlayerGear.forEachWorn(mc.player, w -> {
            if (w.kind() != d.kind()) return;
            if (w == d) isSelf[0] = true;
            if (worn[0] == null || w.level() < worn[0].level()) worn[0] = w;
        });
        if (isSelf[0] || worn[0] == null || worn[0].equals(d)) return java.util.Optional.empty();
        GearData w = worn[0];
        double base = d.base() - w.base();
        double stats = d.stats().values().stream().mapToDouble(Double::doubleValue).sum()
                - w.stats().values().stream().mapToDouble(Double::doubleValue).sum();
        int props = d.affixes().size() + d.effects().size() * 2 - w.affixes().size() - w.effects().size() * 2;
        var line = Component.literal("Против надетого (ур. " + w.level() + "): ").withStyle(ChatFormatting.GRAY);
        boolean any = false;
        if (Math.abs(base) >= 0.05) {
            line.append(delta((d.kind().isWeapon() ? "урон " : "броня ") , base));
            any = true;
        }
        if (Math.abs(stats) >= 0.05) {
            if (any) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            line.append(delta("характеристики ", stats));
            any = true;
        }
        if (props != 0) {
            if (any) line.append(Component.literal(", ").withStyle(ChatFormatting.GRAY));
            line.append(Component.literal((props > 0 ? "свойства лучше" : "свойства слабее")).withColor(props > 0 ? 0x7BF5A0 : 0xFF7B7B));
            any = true;
        }
        return any ? java.util.Optional.of(line) : java.util.Optional.empty();
    }

    private static Component delta(String what, double v) {
        return Component.literal(what + (v > 0 ? "+" : "") + Derived.trim(v)).withColor(v > 0 ? 0x7BF5A0 : 0xFF7B7B);
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
        for (GearData.Effect ef : d.effects()) {
            add.add(Component.literal("✦ " + ef.type().title + ": ").withColor(0xFF9CF0)
                    .append(Component.literal(ef.type().describe(ef.value())).withColor(0xFFD0F6)));
        }
        compare(d, e.getItemStack()).ifPresent(add::add);
        if (!d.unique().isEmpty()) {
            var trophy = com.andrey.litrpg.gear.Trophies.get(d.unique());
            add.add(Component.literal(trophy != null ? "Трофей: " + com.andrey.litrpg.boss.BossRegistry.get(trophy.boss()).name() : "Уникальная вещь")
                    .withColor(0xFFA733));
            if (trophy != null) {
                add.add(Component.literal("«" + trophy.lore() + "»").withStyle(net.minecraft.ChatFormatting.ITALIC, net.minecraft.ChatFormatting.GRAY));
            }
        }
        if (d.rerolls() > 0) add.add(Component.literal("Перекована: " + d.rerolls() + " раз").withStyle(ChatFormatting.DARK_GRAY));
        if (!d.effects().isEmpty() || d.rarity().ordinal() >= com.andrey.litrpg.gear.Rarity.EPIC.ordinal()) {
            add.add(Component.literal("Особые свойства работают, пока вещь надета.").withStyle(ChatFormatting.DARK_GRAY));
        }
        t.addAll(at, add);
        t.removeIf(line -> line.getString().startsWith("Неразрушим") || line.getString().equals("Unbreakable"));
    }
}
