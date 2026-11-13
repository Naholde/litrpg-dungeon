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

    private static void pressButton(Minecraft mc, String label) {
        if (mc.screen == null) return;
        for (var child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.Button b && b.getMessage().getString().contains(label)) {
                b.onPress();
                LitRPG.LOG.info("[LitRPG] dev: нажата кнопка «{}»", b.getMessage().getString());
                return;
            }
        }
        LitRPG.LOG.info("[LitRPG] dev: кнопки «{}» нет", label);
    }

    private static void openScreen(Minecraft mc) {
        Path file = mc.gameDirectory.toPath().resolve("dev_open.txt");
        if (!Files.exists(file)) return;
        String[] cmd;
        try {
            String line = Files.readString(file, StandardCharsets.UTF_8).strip();
            // «button <надпись>» — нажать кнопку текущего окна (и до входа в мир: предупреждения, меню).
            if (line.startsWith("button ")) {
                Files.delete(file);
                pressButton(mc, line.substring(7).strip());
                return;
            }
            if (mc.player == null) return;
            cmd = line.split("\\s+");
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
            // окно сбора: lobby ready | lobby difficulty heroic | lobby spectate | lobby return | lobby abandon
            case "lobby" -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.andrey.litrpg.net.LobbyActionPayload(
                    com.andrey.litrpg.net.LobbyActionPayload.Action.valueOf(cmd[1].toUpperCase(java.util.Locale.ROOT)),
                    cmd.length > 2 ? cmd[2] : ""));
            case "codex" -> mc.setScreen(new com.andrey.litrpg.client.screen.CodexScreen(cmd.length > 1 ? Integer.parseInt(cmd[1]) : 0));
            case "credits" -> mc.setScreen(new com.andrey.litrpg.client.screen.CreditsScreen(
                    new com.andrey.litrpg.net.CreditsPayload(java.util.List.of("Dev", "Друг"), 3 * 3600 + 12 * 60, 4)));
            case "camera" -> mc.options.setCameraType(cmd.length > 1 && cmd[1].equals("back")
                    ? net.minecraft.client.CameraType.THIRD_PERSON_BACK
                    : cmd.length > 1 && cmd[1].equals("front") ? net.minecraft.client.CameraType.THIRD_PERSON_FRONT
                    : net.minecraft.client.CameraType.FIRST_PERSON);
            case "respawn" -> {
                mc.player.respawn();
                mc.setScreen(null);
            }
            // написать в чат (текстовые загадки)
            case "chat" -> mc.player.connection.sendChat(String.join(" ", java.util.Arrays.copyOfRange(cmd, 1, cmd.length)));
            // ПКМ по блоку под прицелом (рычаги, кнопки, тайники, Печать)
            case "use" -> {
                if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                        && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                    mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
                }
            }
            // зажать/отпустить Shift (воскрешение духа)
            case "sneak" -> mc.options.keyShift.setDown(cmd.length > 1 && cmd[1].equals("on"));
            case "walk" -> mc.options.keyUp.setDown(cmd.length > 1 && cmd[1].equals("on"));
            // бег: зажать «вперёд» и «бег» (проверка карты — разбег в шахту, прыжки по мосту)
            case "sprint" -> {
                boolean on = cmd.length > 1 && cmd[1].equals("on");
                mc.options.keyUp.setDown(on);
                mc.options.keySprint.setDown(on);
                if (!on) mc.player.setSprinting(false);
            }
            // зажать прыжок (с «sprint on» — прыжки с разбега по паркуру)
            case "jump" -> mc.options.keyJump.setDown(cmd.length > 1 && cmd[1].equals("on"));
            default -> LitRPG.LOG.warn("[LitRPG] dev: неизвестное окно {}", cmd[0]);
        }
    }
}
