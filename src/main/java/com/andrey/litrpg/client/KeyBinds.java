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

    /** Панель навыков: Z X C V B G. */
    public static final KeyMapping[] SKILL = new KeyMapping[6];
    /** Рывок — левый Alt. */
    /** Кодекс Системы: правила, этапы, хранители, бестиарий, трофеи. */
    public static final KeyMapping CODEX = new KeyMapping("key.litrpg.codex", KeyConflictContext.IN_GAME,
            com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM, org.lwjgl.glfw.GLFW.GLFW_KEY_J, CATEGORY);
    public static final KeyMapping DASH = new KeyMapping("key.litrpg.dash", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);
    public static final String[] SKILL_LABELS = {"Z", "X", "C", "V", "B", "G"};

    /** Ctrl+1…6 — выбрать участника отряда целью лечения. */
    public static final KeyMapping[] PARTY_TARGET = new KeyMapping[com.andrey.litrpg.party.Party.MAX];

    static {
        int[] keys = {GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_X, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_G};
        for (int i = 0; i < SKILL.length; i++) {
            SKILL[i] = new KeyMapping("key.litrpg.skill_" + (i + 1), KeyConflictContext.IN_GAME,
                    InputConstants.Type.KEYSYM, keys[i], CATEGORY);
        }
        for (int i = 0; i < PARTY_TARGET.length; i++) {
            PARTY_TARGET[i] = new KeyMapping("key.litrpg.party_target_" + (i + 1), KeyConflictContext.IN_GAME,
                    KeyModifier.CONTROL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_1 + i, CATEGORY);
        }
    }

    private KeyBinds() {
    }
}
