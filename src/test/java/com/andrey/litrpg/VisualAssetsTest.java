package com.andrey.litrpg;

import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фаза «Визуал»: у каждого навыка — нарисованный значок (tools/gen_icons.py). Нет — сборка падает. */
class VisualAssetsTest {
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
