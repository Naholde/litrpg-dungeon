package com.andrey.litrpg;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.mob.Bestiary;
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
                List<String> need = new ArrayList<>(List.of("phase", "enrage", "relocate", "death", "tired"));
                for (String k : t.params().keySet()) if (k.endsWith("_cast")) need.add("a_" + k.substring(0, k.length() - 5));
                for (String n : need) if (!anims.has("animation." + t.id() + "." + n)) missing.add(t.id() + "." + n);
            }
        }
        assertTrue(missing.isEmpty(), "нет анимаций (запустите tools/gen_models.py): " + missing);
    }

    /**
     * Объекты классов и помощники — настоящие модели: геометрия, текстура и движения (стоять, у помощников — идти
     * и бить, у двойников — бить: их удар повторяет «Отражённый удар»).
     */
    @Test
    void everyClassObjectHasModel() throws Exception {
        List<String> missing = new ArrayList<>();
        java.util.Map<String, List<String>> need = new java.util.LinkedHashMap<>();
        for (var k : com.andrey.litrpg.skill.prop.PropKind.all()) {
            need.put(k.id(), k == com.andrey.litrpg.skill.prop.PropKind.ILLUSION || k == com.andrey.litrpg.skill.prop.PropKind.MIRROR
                    ? List.of("idle", "attack") : List.of("idle"));
        }
        for (var k : com.andrey.litrpg.skill.prop.SummonKind.all()) need.put(k.id(), List.of("idle", "walk", "attack"));
        com.google.gson.JsonObject ru;
        try (var in = getClass().getResourceAsStream("/assets/litrpg/lang/ru_ru.json")) {
            ru = JsonParser.parseReader(new InputStreamReader(java.util.Objects.requireNonNull(in), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
        for (var e : need.entrySet()) {
            String id = e.getKey();
            if (!ru.has("entity.litrpg." + id)) missing.add(id + ": название в ru_ru.json");
            if (getClass().getResource("/assets/litrpg/geo/entity/" + id + ".geo.json") == null) missing.add(id + ": модель");
            if (getClass().getResource("/assets/litrpg/textures/entity/" + id + ".png") == null) missing.add(id + ": текстура");
            try (var in = getClass().getResourceAsStream("/assets/litrpg/animations/entity/" + id + ".animation.json")) {
                if (in == null) {
                    missing.add(id + ": анимации");
                    continue;
                }
                var anims = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonObject("animations");
                for (String n : e.getValue()) if (!anims.has("animation." + id + "." + n)) missing.add(id + "." + n);
            }
        }
        assertTrue(missing.isEmpty(), "у объектов классов не хватает (модели — tools/gen_models.py): " + missing);
    }

    /** У каждого вида существ — модель и полный набор движений: ходьба, удар, выстрел, заклинание, щит, прыжок, «больно». */
    @Test
    void everyCreatureHasModelAndMoves() throws Exception {
        List<String> missing = new ArrayList<>();
        for (Bestiary.Species sp : Bestiary.all()) {
            if (getClass().getResource("/assets/litrpg/geo/entity/" + sp.model() + ".geo.json") == null) missing.add(sp.id() + ": модель");
            try (var in = getClass().getResourceAsStream("/assets/litrpg/animations/entity/" + sp.model() + ".animation.json")) {
                if (in == null) {
                    missing.add(sp.id() + ": анимации");
                    continue;
                }
                var anims = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject()
                        .getAsJsonObject("animations");
                for (String n : List.of("idle", "walk", "attack", "shoot", "cast", "block", "leap", "hurt")) {
                    if (!anims.has("animation." + sp.model() + "." + n)) missing.add(sp.id() + "." + n);
                }
            }
        }
        assertTrue(missing.isEmpty(), "у существ не хватает (tools/gen_models.py): " + missing);
    }


    /**
     * Пустая карта свечения роняет игру прямо в бою: GeckoLib на ней кидает
     * «Invalid glow layer texture provided, must have at least one pixel!».
     * Поэтому такой файл не должен вообще лежать в моде (генератор их больше не пишет).
     */
    @Test
    void noEmptyGlowMasks() throws Exception {
        java.nio.file.Path dir = java.nio.file.Path.of(System.getProperty("litrpg.root", "."))
                .resolve("src/main/resources/assets/litrpg/textures/entity");
        if (!java.nio.file.Files.isDirectory(dir)) return;
        List<String> empty = new ArrayList<>();
        try (var files = java.nio.file.Files.list(dir)) {
            for (java.nio.file.Path f : files.filter(x -> x.getFileName().toString().endsWith("_glowmask.png")).toList()) {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(f.toFile());
                if (img == null) {
                    empty.add(f.getFileName() + " (не читается)");
                    continue;
                }
                boolean lit = false;
                for (int y = 0; y < img.getHeight() && !lit; y++) {
                    for (int x = 0; x < img.getWidth(); x++) {
                        if ((img.getRGB(x, y) >>> 24) != 0) {
                            lit = true;
                            break;
                        }
                    }
                }
                if (!lit) empty.add(f.getFileName().toString());
            }
        }
        assertTrue(empty.isEmpty(), "пустые карты свечения — игра упадёт при отрисовке: " + empty);
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
