package com.andrey.litrpg;

import com.andrey.litrpg.core.balance.Progression;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionTest {
    private static final String FILE = "/data/litrpg/balance/progression.json";

    @Test
    void fileParsesAndMatchesDefaults() throws Exception {
        var json = JsonParser.parseReader(new InputStreamReader(
                Objects.requireNonNull(getClass().getResourceAsStream(FILE), FILE), StandardCharsets.UTF_8));
        Progression p = Progression.CODEC.codec().parse(JsonOps.INSTANCE, json).getOrThrow();
        assertEquals(Progression.DEFAULT, p, "значения по умолчанию в коде должны совпадать с progression.json");
    }

    @Test
    void xpFormula() {
        Progression p = Progression.DEFAULT;
        assertEquals(190, p.xpToNext(1)); // 100 + 90 × 1^1,85
        assertEquals(0, p.xpToNext(p.maxLevel()), "на максимальном уровне опыт не нужен");
        for (int l = 1; l < p.maxLevel() - 1; l++) {
            assertTrue(p.xpToNext(l + 1) > p.xpToNext(l), "опыт до уровня растёт: " + l);
        }
    }
}
