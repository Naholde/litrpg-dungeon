package com.andrey.litrpg.client.look;

import com.andrey.litrpg.LitRPG;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Облики классов: {@code geo/entity/look_<класс>.geo.json}, {@code textures/entity/look_<класс>.png}
 * (+ {@code _glowmask.png}). Читаются при первой надобности и сбрасываются при перезагрузке ресурсов (F3+T).
 */
public final class LookModels {
    private static final Logger LOG = LoggerFactory.getLogger("litrpg/look");

    public record Entry(LookModel model, ResourceLocation texture, ResourceLocation glow) {
    }

    /** null в значении — облика у класса нет (не искать каждый кадр заново). */
    private static final Map<String, Optional<Entry>> CACHE = new HashMap<>();

    private LookModels() {
    }

    public static Entry get(String classId) {
        if (classId == null || classId.isEmpty()) return null;
        return CACHE.computeIfAbsent(classId, LookModels::load).orElse(null);
    }

    private static Optional<Entry> load(String classId) {
        String name = "look_" + classId;
        var rm = Minecraft.getInstance().getResourceManager();
        Optional<Resource> geo = rm.getResource(LitRPG.id("geo/entity/" + name + ".geo.json"));
        if (geo.isEmpty()) return Optional.empty();
        try (Reader r = geo.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(r).getAsJsonObject();
            LookModel m = LookModel.parse(json);
            ResourceLocation tex = LitRPG.id("textures/entity/" + name + ".png");
            ResourceLocation glow = LitRPG.id("textures/entity/" + name + "_glowmask.png");
            boolean hasGlow = rm.getResource(glow).isPresent();
            return Optional.of(new Entry(m, tex, hasGlow ? glow : null));
        } catch (Exception e) {
            LOG.error("облик {} не читается", name, e);
            return Optional.empty();
        }
    }

    public static void clear() {
        CACHE.clear();
    }
}
