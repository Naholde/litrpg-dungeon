package com.andrey.litrpg;

import com.andrey.litrpg.player.LookService;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.skill.ClassRegistry;
import com.andrey.litrpg.skill.PlayerClass;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Обновление 14, пункт 7: у каждого класса — свой облик (tools/models/looks.py), прикреплённый к частям тела игрока. */
class LookAssetsTest {
    private static final Set<String> PARTS = Set.of("head", "body", "right_arm", "left_arm", "right_leg", "left_leg");

    @Test
    void everyClassHasALookOnPlayerParts() throws Exception {
        List<String> bad = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (PlayerClass c : ClassRegistry.all()) ids.add(c.id());
        for (Role r : Role.values()) if (r.chosen()) ids.add(LookService.lookId("", r));   // облик роли до класса
        for (String id : ids) {
            String name = "look_" + id;
            if (getClass().getResource("/assets/litrpg/textures/entity/" + name + ".png") == null) bad.add(name + ": текстура");
            var in = getClass().getResourceAsStream("/assets/litrpg/geo/entity/" + name + ".geo.json");
            if (in == null) {
                bad.add(name + ": модель");
                continue;
            }
            JsonObject geo;
            try (in) {
                geo = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            }
            Map<String, String> parent = new HashMap<>();
            int cubes = 0;
            for (JsonElement e : geo.getAsJsonArray("bones")) {
                JsonObject b = e.getAsJsonObject();
                parent.put(b.get("name").getAsString(), b.has("parent") ? b.get("parent").getAsString() : null);
                if (b.has("cubes")) cubes += b.getAsJsonArray("cubes").size();
            }
            // Каждая кость должна вести к части тела: иначе игра её не нарисует (LookLayer идёт от частей тела).
            for (String bone : parent.keySet()) {
                if (bone.equals("root")) continue;
                String top = bone;
                while (parent.get(top) != null && !"root".equals(parent.get(top))) top = parent.get(top);
                if (!PARTS.contains(top)) bad.add(name + ": кость " + bone + " не на части тела (" + top + ")");
            }
            if (cubes < 20) bad.add(name + ": всего " + cubes + " кубов — облик не доделан");
        }
        assertTrue(bad.isEmpty(), "Облики классов: " + bad);
    }

    @Test
    void lookIsClassThenRoleThenNothing() {
        assertEquals("berserker", LookService.lookId("berserker", Role.TANK));
        assertEquals("role_tank", LookService.lookId("", Role.TANK));
        assertEquals("role_support", LookService.lookId(null, Role.SUPPORT));
        assertEquals("", LookService.lookId("", Role.NONE));
    }
}
