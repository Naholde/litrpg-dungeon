package com.andrey.litrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода (меняются в настройках управления, раздел «LitRPG»). */
public final class KeyBinds {
    public static final String CATEGORY = "key.categories.litrpg";

    public static final KeyMapping CHARACTER = new KeyMapping("key.litrpg.character", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    private KeyBinds() {
    }
}
