package com.andrey.litrpg.run;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Статистика и титулы (обновление 15): {@code /litrpg stats [игрок]} — личная статистика за всё время,
 * {@code /litrpg title} — список своих титулов, {@code /litrpg title <id|none>} — выбрать активный.
 * Доступны всем (не только операторам). Окно статистики в игре — вкладка окна персонажа.
 */
public final class RecordsCommands {
    private RecordsCommands() {
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("stats").executes(c -> stats(c, c.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> stats(c, EntityArgument.getPlayer(c, "player")))));
        root.then(Commands.literal("title").executes(RecordsCommands::titles)
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> {
                            var prof = Records.get(c.getSource().getServer()).profileOrNull(c.getSource().getPlayerOrException().getUUID());
                            return SharedSuggestionProvider.suggest(prof == null ? java.util.List.of("none")
                                    : java.util.stream.Stream.concat(prof.titles.stream(), java.util.stream.Stream.of("none")).toList(), b);
                        })
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(c, "id");
                            if (!Titles.choose(p, id.equals("none") ? "" : id)) {
                                c.getSource().sendFailure(Component.literal("Такого титула у вас нет. Ваши титулы: /litrpg title"));
                                return 0;
                            }
                            c.getSource().sendSuccess(() -> Component.literal(id.equals("none") ? "Титул снят." : "Титул выбран."), false);
                            return 1;
                        })));
    }

    private static int stats(CommandContext<CommandSourceStack> c, ServerPlayer who) {
        Records.Profile p = Records.get(c.getSource().getServer()).profileOrNull(who.getUUID());
        if (p == null) {
            c.getSource().sendFailure(Component.literal("У " + who.getGameProfile().getName() + " ещё нет статистики."));
            return 0;
        }
        line(c, Component.literal("— " + p.name + " —").withStyle(ChatFormatting.GOLD));
        line(c, Component.literal("Забегов: " + p.runs + " · пройдено: обычная " + p.completedNormal + ", героическая " + p.completedHeroic));
        line(c, Component.literal("Хранителей повержено: " + p.bosses + " (видов " + p.bossKinds.size() + " из 15)"));
        line(c, Component.literal("Урон: " + num(p.dealt) + " · лечение: " + num(p.healed) + " · принято: " + num(p.taken)));
        line(c, Component.literal("Монстров: " + p.kills + " · смертей: " + p.deaths + " · поднято павших: " + p.revives
                + " · тайников: " + p.caches));
        if (p.parkourBest > 0) line(c, Component.literal("Паркур: " + Chronicle.time(p.parkourBest)));
        if (p.polygonBest > 0) line(c, Component.literal("Полигон: серия уворотов " + p.polygonBest));
        line(c, Component.literal("Титулов: " + p.titles.size() + " из " + Titles.values().length));
        return 1;
    }

    private static int titles(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer me = c.getSource().getPlayerOrException();
        Records.Profile p = Records.get(me.server).profile(me.getUUID(), me.getGameProfile().getName());
        for (Titles t : Titles.values()) {
            boolean has = p.titles.contains(t.id);
            Component row = (has ? t.badge().copy() : Component.literal("[" + t.name + "]").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" " + t.how + (has ? "  → /litrpg title " + t.id : ""))
                            .withStyle(has ? ChatFormatting.GRAY : ChatFormatting.DARK_GRAY));
            if (t.id.equals(p.title)) row = Component.literal("★ ").withStyle(ChatFormatting.GOLD).append(row);
            line(c, row);
        }
        return p.titles.size();
    }

    private static void line(CommandContext<CommandSourceStack> c, Component text) {
        c.getSource().sendSuccess(() -> text, false);
    }

    /** 1 234 567 → «1,2 млн», 12 345 → «12 345». */
    public static String num(double v) {
        if (v >= 1_000_000) return String.format(Locale.ROOT, "%.1f", v / 1_000_000).replace('.', ',') + " млн";
        return String.format(Locale.forLanguageTag("ru"), "%,d", Math.round(v)).replace(' ', ' ');
    }
}
