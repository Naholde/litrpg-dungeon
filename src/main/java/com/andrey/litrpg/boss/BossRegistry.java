package com.andrey.litrpg.boss;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Типы хранителей и их цифры по умолчанию (переопределяются в bosses.json). */
public final class BossRegistry {
    /** Тип хранителя: имя и параметры (уровень, «живучесть», сила ударов, перезарядки…). */
    public record Type(String id, String name, Map<String, Double> params) {
    }

    private static final Map<String, Type> ALL = new LinkedHashMap<>();

    public static final Type TRIAL_WARDEN = reg("trial_warden", "Испытатель Системы", p(
            "level", 10, "hp_units", 60, "melee_units", 0.15,
            "smash_units", 0.9, "smash_cd", 12, "smash_cast", 1.8,
            "wave_units", 0.6, "wave_cd", 20, "wave_cast", 2.5,
            "meteor_units", 0.5, "meteor_cd", 16, "meteor_cast", 2.0,
            "burden_units", 2.4, "burden_cd", 24, "burden_cast", 3.0,
            "mend_share", 0.1, "mend_cd", 30, "mend_cast", 4.0,
            "node_hp_units", 4, "spark_hp_units", 1.2, "spark_units", 0.06));

    private BossRegistry() {
    }

    private static Map<String, Double> p(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return Collections.unmodifiableMap(m);
    }

    private static Type reg(String id, String name, Map<String, Double> params) {
        Type t = new Type(id, name, params);
        if (ALL.putIfAbsent(id, t) != null) throw new IllegalStateException("хранитель уже есть: " + id);
        return t;
    }

    public static Type get(String id) {
        return ALL.get(id);
    }

    public static Collection<Type> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    public static Map<String, Map<String, Double>> defaultValues() {
        Map<String, Map<String, Double>> out = new TreeMap<>();
        for (Type t : ALL.values()) out.put(t.id(), new LinkedHashMap<>(t.params()));
        return out;
    }
}
