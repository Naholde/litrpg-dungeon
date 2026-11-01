package com.andrey.litrpg.skill.prop;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Виды призванных помощников: у каждого своя модель, размер и повадка. */
public record SummonKind(String id, String title, float width, float height, float scale) {
    private static final Map<String, SummonKind> ALL = new LinkedHashMap<>();

    private static SummonKind reg(String id, String title, float w, float h, float scale) {
        SummonKind k = new SummonKind(id, title, w, h, scale);
        if (ALL.putIfAbsent(id, k) != null) throw new IllegalStateException("помощник уже есть: " + id);
        return k;
    }

    public static final SummonKind SKELETON = reg("minion_skeleton", "Поднятый скелет", 0.6f, 1.95f, 1.0f);
    public static final SummonKind WOLF = reg("minion_wolf", "Тень-волк", 0.9f, 0.9f, 1.0f);
    public static final SummonKind GOLEM = reg("minion_golem", "Костяной голем", 1.9f, 3.3f, 1.25f);

    public static SummonKind get(String id) {
        return ALL.get(id);
    }

    public static Collection<SummonKind> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }
}
