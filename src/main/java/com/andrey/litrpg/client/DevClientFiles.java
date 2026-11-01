package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.dev.DevFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Клиентская часть dev-файлов: {@code dev_screenshot.txt} с именем → снимок экрана в {@code screenshots/<имя>.png};
 * {@code dev_open.txt}: {@code character [вкладка]}, {@code role [роль]}, {@code close}, {@code respawn} — открыть/закрыть окно.
 */
@EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
public final class DevClientFiles {
    private static int ticks;

    private DevClientFiles() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!DevFiles.ENABLED || ++ticks % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance();
        openScreen(mc);
        Path file = mc.gameDirectory.toPath().resolve("dev_screenshot.txt");
        if (!Files.exists(file)) return;
        String name;
        try {
            name = Files.readString(file, StandardCharsets.UTF_8).strip();
            Files.delete(file);
        } catch (IOException ex) {
            return;
        }
        if (name.isEmpty()) name = "dev";
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                msg -> LitRPG.LOG.info("[LitRPG] dev: {}", msg.getString()));
    }

    private static void openScreen(Minecraft mc) {
        Path file = mc.gameDirectory.toPath().resolve("dev_open.txt");
        if (!Files.exists(file) || mc.player == null) return;
        String[] cmd;
        try {
            cmd = Files.readString(file, StandardCharsets.UTF_8).strip().split("\\s+");
            Files.delete(file);
        } catch (IOException ex) {
            return;
        }
        switch (cmd[0]) {
            case "character" -> mc.setScreen(new com.andrey.litrpg.client.screen.CharacterScreen(
                    cmd.length > 1 ? Integer.parseInt(cmd[1]) : 0));
            case "role" -> mc.setScreen(cmd.length > 1
                    ? new com.andrey.litrpg.client.screen.RoleScreen(com.andrey.litrpg.player.Role.byId(cmd[1]))
                    : new com.andrey.litrpg.client.screen.RoleScreen());
            case "close" -> mc.setScreen(null);
            case "camera" -> mc.options.setCameraType(cmd.length > 1 && cmd[1].equals("back")
                    ? net.minecraft.client.CameraType.THIRD_PERSON_BACK
                    : cmd.length > 1 && cmd[1].equals("front") ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                    : net.minecraft.client.CameraType.FIRST_PERSON);
            case "respawn" -> {
                mc.player.respawn();
                mc.setScreen(null);
            }
            default -> LitRPG.LOG.warn("[LitRPG] dev: неизвестное окно {}", cmd[0]);
        }
    }
}
