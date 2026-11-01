package com.andrey.litrpg.player;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.andrey.litrpg.skill.SkillService;

import java.util.Arrays;
import java.util.Collection;

/** Команды персонажа для проверки и администрирования: {@code /litrpg level|xp|role|stats}. */
public final class PlayerCommands {
    private PlayerCommands() {
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("level").requires(s -> s.hasPermission(2))
                .then(Commands.literal("set").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 200)).executes(c -> each(c.getSource(),
                                EntityArgument.getPlayers(c, "targets"), p -> PlayerService.setLevel(p,
                                        IntegerArgumentType.getInteger(c, "level"))))))));
        root.then(Commands.literal("xp").requires(s -> s.hasPermission(2))
                .then(Commands.literal("add").then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("amount", LongArgumentType.longArg(0)).executes(c -> each(c.getSource(),
                                EntityArgument.getPlayers(c, "targets"), p -> PlayerService.addXp(p,
                                        LongArgumentType.getLong(c, "amount"))))))));
        root.then(Commands.literal("role").requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("role", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Role.values()).map(r -> r.id), b))
                                .executes(c -> each(c.getSource(), EntityArgument.getPlayers(c, "targets"),
                                        p -> PlayerService.setRole(p, Role.byId(StringArgumentType.getString(c, "role"))))))));
        root.then(Commands.literal("class").requires(s -> s.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("class", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        ClassRegistry.all().stream().map(PlayerClass::id), b))
                                .executes(c -> each(c.getSource(), EntityArgument.getPlayers(c, "targets"),
                                        p -> PlayerService.setClass(p, StringArgumentType.getString(c, "class")))))));
        root.then(Commands.literal("cast").then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                .executes(c -> SkillService.use(c.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(c, "slot") - 1) ? 1 : 0)));
        root.then(Commands.literal("dash").executes(c -> SkillService.dash(c.getSource().getPlayerOrException(), 0, 0) ? 1 : 0));
        root.then(Commands.literal("stats").requires(s -> s.hasPermission(2))
                .then(Commands.literal("reset").then(Commands.argument("targets", EntityArgument.players())
                        .executes(c -> each(c.getSource(), EntityArgument.getPlayers(c, "targets"), PlayerService::resetStats))))
                .then(Commands.literal("show").then(Commands.argument("target", EntityArgument.player())
                        .executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "target"))))));
    }

    private interface Action {
        void run(ServerPlayer p) throws CommandSyntaxException;
    }

    private static int each(CommandSourceStack src, Collection<ServerPlayer> players, Action a) throws CommandSyntaxException {
        for (ServerPlayer p : players) a.run(p);
        src.sendSuccess(() -> Component.literal("Готово: " + players.size() + " игрок(ов)"), true);
        return players.size();
    }

    private static int show(CommandSourceStack src, ServerPlayer p) {
        PlayerData d = PlayerService.data(p);
        StatBreakdown b = PlayerService.breakdown(d);
        src.sendSuccess(() -> Component.literal(p.getScoreboardName() + ": ур. " + d.level() + ", " + d.role().title
                + ", опыт " + d.xp()).withStyle(ChatFormatting.AQUA), false);
        for (Stat s : Stat.ALL) {
            src.sendSuccess(() -> Component.literal("  " + s.title + ": " + Derived.trim(b.get(s))
                    + " (вложено " + d.allocated(s) + ")"), false);
        }
        for (Derived x : Derived.values()) {
            src.sendSuccess(() -> Component.literal("  " + x.title + ": " + x.format(b.get(x))), false);
        }
        return 1;
    }
}
