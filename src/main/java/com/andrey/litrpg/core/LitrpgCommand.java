package com.andrey.litrpg.core;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BalanceKey;
import com.andrey.litrpg.net.Protocol;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
        d.register(Commands.literal("litrpg")
                .then(Commands.literal("version").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(
                            "LitRPG, протокол сети " + Protocol.VERSION), false);
                    return 1;
                }))
                .then(Commands.literal("balance").requires(s -> s.hasPermission(2))
                        .executes(c -> listBalance(c.getSource()))
                        .then(Commands.argument("section", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        Balance.keys().stream().map(BalanceKey::name), b))
                                .executes(c -> showSection(c.getSource(), StringArgumentType.getString(c, "section"))))));
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
