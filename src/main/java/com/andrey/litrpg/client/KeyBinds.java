package com.andrey.litrpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Клавиши мода (меняются в настройках управления, раздел «LitRPG»). */
public final class KeyBinds {
    public static final String CATEGORY = "key.categories.litrpg";

    public static final KeyMapping CHARACTER = new KeyMapping("key.litrpg.character", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY);

    /** Ctrl+1…5 — выбрать участника отряда целью лечения. */
    public static final KeyMapping[] PARTY_TARGET = new KeyMapping[5];

    static {
        for (int i = 0; i < PARTY_TARGET.length; i++) {
            PARTY_TARGET[i] = new KeyMapping("key.litrpg.party_target_" + (i + 1), KeyConflictContext.IN_GAME,
                    KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + i, CATEGORY);
        }
    }

    private KeyBinds() {
    }
}
