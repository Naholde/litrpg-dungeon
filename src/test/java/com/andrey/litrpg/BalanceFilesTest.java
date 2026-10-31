package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BalanceKey;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Каждый раздел баланса: файл есть, читается без ошибок и лишних полей, и совпадает со значениями в коде. */
class BalanceFilesTest {
    @Test
    void everySectionMatchesItsJson() throws Exception {
        Map<String, JsonElement> files = new HashMap<>();
        for (BalanceKey<?> key : Balance.keys()) {
            String path = "/data/litrpg/balance/" + key.name() + ".json";
            try (InputStream in = getClass().getResourceAsStream(path)) {
                assertNotNull(in, "нет файла " + path);
                files.put(key.name(), JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        }
        List<String> errors = Balance.apply(files);
        assertTrue(errors.isEmpty(), "ошибки баланса: " + errors);
        for (BalanceKey<?> key : Balance.keys()) {
            assertTrue(Balance.loadedFromData(key), key.name());
            assertEquals(key.defaults(), Balance.get(key),
                    "значения по умолчанию в коде должны совпадать с " + key.name() + ".json");
        }
    }

    @Test
    void typoInNestedFieldIsReported() {
        var json = JsonParser.parseString("{\"max_level\":50,\"class_level\":10,\"xp_base\":100,\"xp_per_level\":90,"
                + "\"xp_exponent\":1.85,\"stat_points_per_level\":10,\"stat_point_value\":0.3,\"class_points_per_level\":2,"
                + "\"xp_bse\":5}");
        List<String> errors = Balance.apply(Map.of("progression", json));
        assertTrue(errors.stream().anyMatch(e -> e.contains("xp_bse")), errors.toString());
    }
}
