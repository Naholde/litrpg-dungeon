package com.andrey.litrpg;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.skill.Skill;
import com.google.gson.JsonParser;
import com.andrey.litrpg.skill.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фаза «Визуал»: у каждого навыка — нарисованный значок (tools/gen_icons.py). Нет — сборка падает. */
class VisualAssetsTest {
    /** У каждого навыка хранителя — своя анимация «a_<навык>», и есть фаза, неистовство, перенос боя. */
    @Test
    void everyGuardianAbilityHasItsOwnAnimation() throws Exception {
        List<String> missing = new ArrayList<>();
        for (BossRegistry.Type t : BossRegistry.all()) {
            String path = "/assets/litrpg/animations/entity/" + t.id() + ".animation.json";
            try (var in = getClass().getResourceAsStream(path)) {
                if (in == null) {
                    missing.add(path);
                    continue;
                }
                var anims = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonObject("animations");
                List<String> need = new ArrayList<>(List.of("phase", "enrage", "relocate", "death"));
                for (String k : t.params().keySet()) if (k.endsWith("_cast")) need.add("a_" + k.substring(0, k.length() - 5));
                for (String n : need) if (!anims.has("animation." + t.id() + "." + n)) missing.add(t.id() + "." + n);
            }
        }
        assertTrue(missing.isEmpty(), "нет анимаций (запустите tools/gen_models.py): " + missing);
    }

    @Test
    void everySkillHasAnIcon() {
        List<String> missing = new ArrayList<>();
        for (Skill s : SkillRegistry.all()) {
            String path = "/assets/litrpg/textures/gui/skill/" + s.id.replace('.', '_') + ".png";
            if (getClass().getResource(path) == null) missing.add(s.id);
        }
        assertTrue(missing.isEmpty(), "нет значков (запустите tools/gen_icons.py): " + missing);
    }
}
