package com.andrey.litrpg.core;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BalanceKey;
import com.andrey.litrpg.net.Protocol;
import com.mojang.brigadier.CommandDispatcher;
import com.andrey.litrpg.combat.CombatCommands;
import com.andrey.litrpg.player.PlayerCommands;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/** Служебная команда {@code /litrpg}: версия, просмотр баланса. Игровые подкоманды добавляются модулями. */
public final class LitrpgCommand {
    private LitrpgCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("litrpg");
        root.then(Commands.literal("version").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(
                            "LitRPG, протокол сети " + Protocol.VERSION), false);
                    return 1;
                }));
        root.then(Commands.literal("balance").requires(s -> s.hasPermission(2))
                        .executes(c -> listBalance(c.getSource()))
                        .then(Commands.argument("section", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Balance.keys().stream().map(BalanceKey::name), b))
                                .executes(c -> showSection(c.getSource(), StringArgumentType.getString(c, "section")))));
        PlayerCommands.addTo(root);
        CombatCommands.addTo(root);
        com.andrey.litrpg.run.RunCommands.addTo(root);
        d.register(root);
    }

    private static int listBalance(CommandSourceStack src) {
        for (BalanceKey<?> key : Balance.keys()) {
            boolean ok = Balance.loadedFromData(key);
            src.sendSuccess(() -> Component.literal((ok ? "✔ " : "✘ ") + key.name())
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
        for (String e : Balance.errors()) {
            src.sendFailure(Component.literal(e));
        }
        return Balance.keys().size();
    }

    private static int showSection(CommandSourceStack src, String name) {
        BalanceKey<?> key = Balance.key(name);
        if (key == null) {
            src.sendFailure(Component.literal("Нет раздела баланса «" + name + "»"));
            return 0;
        }
        String json = Balance.encode(key).toString();
        src.sendSuccess(() -> Component.literal(name + ": " + json), false);
        return 1;
    }
}
