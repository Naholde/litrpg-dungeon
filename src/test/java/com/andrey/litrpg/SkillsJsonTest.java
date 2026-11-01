package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.BossesBalance;
import com.andrey.litrpg.core.balance.GearBalance;
import com.andrey.litrpg.core.balance.MobsBalance;
import com.andrey.litrpg.core.balance.ShopBalance;
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
 * skills.json и bosses.json = все навыки и хранители со значениями из кода. Добавил навык, хранителя или параметр —
 * перегенерируй: {@code gradlew test -Pregen} (тест сам перепишет файлы), затем подкрути цифры в json.
 */
class SkillsJsonTest {
    @Test
    void skillsJsonListsEveryParameter() throws Exception {
        check("/data/litrpg/balance/skills.json",
                SkillsBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, SkillsBalance.defaults()).getOrThrow());
    }

    @Test
    void bossesJsonListsEveryParameter() throws Exception {
        check("/data/litrpg/balance/bosses.json",
                BossesBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, BossesBalance.defaults()).getOrThrow());
    }

    @Test
    void gearJsonListsEveryParameter() throws Exception {
        check("/data/litrpg/balance/gear.json",
                GearBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, GearBalance.DEFAULT).getOrThrow());
    }

    @Test
    void shopJsonListsEveryParameter() throws Exception {
        check("/data/litrpg/balance/shop.json",
                ShopBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, ShopBalance.DEFAULT).getOrThrow());
    }

    @Test
    void mobsJsonListsEveryParameter() throws Exception {
        check("/data/litrpg/balance/mobs.json",
                MobsBalance.CODEC.codec().encodeStart(JsonOps.INSTANCE, MobsBalance.defaults()).getOrThrow());
    }

    private void check(String res, JsonElement expected) throws Exception {
        if (Boolean.getBoolean("litrpg.regen")) {
            Path out = Path.of(System.getProperty("litrpg.root"), "src/main/resources" + res);
            Files.writeString(out, new GsonBuilder().setPrettyPrinting().create().toJson(expected) + "\n", StandardCharsets.UTF_8);
        }
        try (InputStream in = getClass().getResourceAsStream(res)) {
            assertNotNull(in, "нет " + res + " — запусти gradlew test -Pregen");
            JsonElement actual = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            // Сравниваем ключи (что все навыки/хранители и параметры есть); значения могут быть подкручены балансом.
            assertEquals(keys(expected), keys(actual), "состав " + res + " не совпадает с кодом — gradlew test -Pregen");
        }
    }

    private static String keys(JsonElement e) {
        if (!e.isJsonObject()) return "";
        StringBuilder sb = new StringBuilder("{");
        e.getAsJsonObject().keySet().stream().sorted().forEach(k -> sb.append(k).append(keys(e.getAsJsonObject().get(k))).append(','));
        return sb.append('}').toString();
    }
}
