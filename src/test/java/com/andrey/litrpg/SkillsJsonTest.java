package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.SkillsBalance;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * skills.json = все навыки со значениями из кода. Добавил навык или параметр — перегенерируй:
 * {@code gradlew test -Pregen} (тест сам перепишет файл), затем подкрути цифры в json.
 */
class SkillsJsonTest {
    private static final String RES = "/data/litrpg/balance/skills.json";

    @Test
    void skillsJsonListsEveryParameter() throws Exception {
        JsonElement expected = SkillsBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, SkillsBalance.defaults()).getOrThrow();
        if (Boolean.getBoolean("litrpg.regen")) {
            Path out = Path.of(System.getProperty("litrpg.root"), "src/main/resources" + RES);
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(expected) + "\n", StandardCharsets.UTF_8);
        }
        try (InputStream in = getClass().getResourceAsStream(RES)) {
            assertNotNull(in, "нет skills.json — запусти gradlew test -Pregen");
            JsonElement actual = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            // Сравниваем ключи (что все навыки и параметры есть); значения могут быть подкручены балансом.
            assertEquals(keys(expected), keys(actual), "состав skills.json не совпадает с кодом — gradlew test -Pregen");
        }
    }

    private static String keys(JsonElement e) {
        if (!e.isJsonObject()) return "";
        StringBuilder sb = new StringBuilder("{");
        e.getAsJsonObject().keySet().stream().sorted().forEach(k -> sb.append(k).append(keys(e.getAsJsonObject().get(k))).append(','));
        return sb.append('}').toString();
    }
}
