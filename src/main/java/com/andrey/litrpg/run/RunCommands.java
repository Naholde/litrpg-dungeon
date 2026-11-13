package com.andrey.litrpg.run;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /litrpg run …} — прохождение для оператора и проверки: состояние, старт без Печати, переход на этап,
 * вайп, сброс, наименьший отряд, «сделано» (пропустить отряд или хранителя).
 */
public final class RunCommands {
    private RunCommands() {
    }

    public static void addTo(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("run").requires(s -> s.hasPermission(2))
                .then(Commands.literal("status").executes(RunCommands::status))
                .then(Commands.literal("start").executes(c -> start(c, null, Difficulty.HEROIC))
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(c -> start(c, new ArrayList<>(EntityArgument.getPlayers(c, "players")), Difficulty.HEROIC)))
                        .then(Commands.literal("normal").executes(c -> start(c, null, Difficulty.NORMAL)))
                        .then(Commands.literal("heroic").executes(c -> start(c, null, Difficulty.HEROIC))))
                .then(Commands.literal("stage").then(Commands.argument("n", IntegerArgumentType.integer(1, 6))
                        .executes(c -> {
                            if (!RunService.active(c.getSource().getServer())) {
                                c.getSource().sendFailure(Component.literal("Прохождение не идёт: сначала /litrpg run start"));
                                return 0;
                            }
                            RunService.enterStage(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "n"));
                            return 1;
                        })))
                .then(Commands.literal("wipe").executes(c -> {
                    RunService.wipe(c.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("reset").executes(c -> {
                    RunService.reset(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Прохождение сброшено, все — в преддверии."), true);
                    return 1;
                }))
                .then(Commands.literal("minplayers").then(Commands.argument("n", IntegerArgumentType.integer(0, 6))
                        .executes(c -> {
                            int n = IntegerArgumentType.getInteger(c, "n");
                            RunService.setMinPlayers(c.getSource().getServer(), n);
                            c.getSource().sendSuccess(() -> Component.literal("Наименьший отряд у Печати: " + n + " (0 — по балансу)"), true);
                            return 1;
                        })))
                .then(Commands.literal("done").then(Commands.argument("what", StringArgumentType.greedyString())
                        .executes(c -> {
                            RunState s = RunService.state(c.getSource().getServer());
                            String what = StringArgumentType.getString(c, "what");
                            s.done.add(what);
                            s.setDirty();
                            c.getSource().sendSuccess(() -> Component.literal("Отмечено: " + what), true);
                            return 1;
                        })))
                .then(Commands.literal("solve").executes(c -> {
                    int n = Mechanisms.solveAll(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal("Загадок снято: " + n), true);
                    return n;
                }))
                .then(Commands.literal("lobby").executes(c -> {
                    RunService.toLobby(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int start(CommandContext<CommandSourceStack> c, List<ServerPlayer> players, Difficulty difficulty)
            throws CommandSyntaxException {
        var server = c.getSource().getServer();
        if (players == null) {
            players = new ArrayList<>();
            for (ServerPlayer p : RunService.dungeon(server).players()) if (!p.isSpectator()) players.add(p);
            if (players.isEmpty()) players.add(c.getSource().getPlayerOrException());
        }
        if (players.size() > com.andrey.litrpg.party.Party.MAX) players = players.subList(0, com.andrey.litrpg.party.Party.MAX);
        RunService.start(server, players, difficulty);
        return players.size();
    }

    private static int status(CommandContext<CommandSourceStack> c) {
        var server = c.getSource().getServer();
        RunState s = RunService.state(server);
        Layout.Stage st = RunService.currentStage(server);
        String name = st != null ? " «" + st.name() + "»" : "";
        c.getSource().sendSuccess(() -> Component.literal("Этап " + s.stage + name + ", попытка " + s.attempt + ", вайпов " + s.wipes
                + ", отряд " + s.names.values() + " (" + s.partySize + ")"), false);
        c.getSource().sendSuccess(() -> Component.literal("Сделано: " + s.done + "; живы: " + s.alive + "; духи: " + s.spirits.size()
                + "; наград: " + s.claims.size()), false);
        return 1;
    }
}
