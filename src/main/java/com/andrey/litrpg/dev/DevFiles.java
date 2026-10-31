package com.andrey.litrpg.dev;

import com.andrey.litrpg.LitRPG;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Управление игрой через файлы — для проверки без участия человека.
 * Сервер раз в секунду читает {@code dev_commands.txt} в папке игры, выполняет строки как оператор и удаляет файл.
 * Работает только с {@code -Dlitrpg.dev=true} (dev-запуски и тестовый сервер), у игроков выключено.
 */
public final class DevFiles {
    public static final boolean ENABLED = Boolean.getBoolean("litrpg.dev");
    private static final String COMMANDS = "dev_commands.txt";

    private DevFiles() {
    }

    public static void register(IEventBus game) {
        if (!ENABLED) return;
        game.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 20 == 0) runCommands(e.getServer());
        });
        LitRPG.LOG.info("[LitRPG] Dev-режим: команды из {}", FMLPaths.GAMEDIR.get().resolve(COMMANDS));
    }

    private static void runCommands(MinecraftServer server) {
        Path file = FMLPaths.GAMEDIR.get().resolve(COMMANDS);
        if (!Files.exists(file)) return;
        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            Files.delete(file);
        } catch (IOException ex) {
            LitRPG.LOG.warn("[LitRPG] dev: не прочитать {}: {}", file, ex.toString());
            return;
        }
        var source = server.createCommandSourceStack().withPermission(4);
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            LitRPG.LOG.info("[LitRPG] dev> {}", line);
            server.getCommands().performPrefixedCommand(source, line);
        }
    }
}
