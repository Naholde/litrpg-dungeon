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

/** Клиентская часть dev-файлов: {@code dev_screenshot.txt} с именем → снимок экрана в {@code screenshots/<имя>.png}. */
@EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
public final class DevClientFiles {
    private static int ticks;

    private DevClientFiles() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!DevFiles.ENABLED || ++ticks % 10 != 0) return;
        Minecraft mc = Minecraft.getInstance();
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
}
