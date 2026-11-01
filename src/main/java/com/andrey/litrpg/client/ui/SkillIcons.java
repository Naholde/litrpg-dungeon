package com.andrey.litrpg.client.ui;

import com.andrey.litrpg.LitRPG;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Нарисованные значки навыков 16×16 (tools/gen_icons.py). Нет значка — рисующий покажет символ навыка. */
public final class SkillIcons {
    private static final Map<String, ResourceLocation> FOUND = new HashMap<>();
    private static final ResourceLocation NONE = LitRPG.id("none");

    private SkillIcons() {
    }

    private static ResourceLocation of(String skillId) {
        return FOUND.computeIfAbsent(skillId, id -> {
            ResourceLocation loc = LitRPG.id("textures/gui/skill/" + id.replace('.', '_') + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(loc).isPresent() ? loc : NONE;
        });
    }

    /** Нарисовать значок навыка размером {@code size}; false — значка нет. */
    public static boolean draw(GuiGraphics g, String skillId, int x, int y, int size) {
        ResourceLocation loc = of(skillId);
        if (loc == NONE) return false;
        g.blit(loc, x, y, size, size, 0, 0, 16, 16, 16, 16);
        return true;
    }
}
