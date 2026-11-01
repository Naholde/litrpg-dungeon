package com.andrey.litrpg.boss;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** Типы хранителей и их цифры по умолчанию (переопределяются в bosses.json). */
public final class BossRegistry {
    /** Тип хранителя: имя, трек (sounds/music/&lt;id&gt;.ogg) и параметры (уровень, «живучесть», сила ударов, перезарядки…). */
    public record Type(String id, String name, String music, Map<String, Double> params) {
    }

    private static final Map<String, Type> ALL = new LinkedHashMap<>();

    public static final Type TRIAL_WARDEN = reg("trial_warden", "Испытатель Системы", "", p(
            "level", 10, "hp_units", 60, "melee_units", 0.15,
            "smash_units", 0.9, "smash_cd", 12, "smash_cast", 1.8,
            "wave_units", 0.6, "wave_cd", 20, "wave_cast", 2.5,
            "meteor_units", 0.5, "meteor_cd", 16, "meteor_cast", 2.0,
            "burden_units", 2.4, "burden_cd", 24, "burden_cast", 3.0,
            "mend_share", 0.1, "mend_cd", 30, "mend_cast", 4.0,
            "node_hp_units", 4, "spark_hp_units", 1.2, "spark_units", 0.06));

    // ---------------------------------------------------------------- Этап 1 · Склеп Забытых
    public static final Type GATEKEEPER = reg("gatekeeper", "Привратник Склепа", "fragment", p(
            "level", 4, "hp_units", 38, "melee_units", 0.14,
            "chain_units", 0.75, "chain_cd", 9, "chain_cast", 1.6,
            "rampart_units", 0.55, "rampart_cd", 21, "rampart_cast", 2.6,
            "call_cd", 32, "call_cast", 2.0,
            "shield_share", 0.08, "shield_cd", 26, "shield_cast", 3.0));

    /** Сёстры: Морра (бьёт) — хранитель из схемы; Весса (колдует) появляется вместе с ней. */
    public static final Type MORRA = reg("morra", "Морра", "fight_club", p(
            "level", 7, "hp_units", 30, "melee_units", 0.16,
            "reap_units", 0.8, "reap_cd", 10, "reap_cast", 1.6,
            "spears_units", 0.5, "spears_cd", 17, "spears_cast", 2.2,
            "revive_seconds", 12, "link_cut", 0.5, "link_units", 0.06));
    public static final Type VESSA = reg("vessa", "Весса", "fight_club", p(
            "level", 7, "hp_units", 26, "melee_units", 0.08,
            "mend_share", 0.08, "mend_cd", 20, "mend_cast", 3.0,
            "curse_units", 0.05, "curse_seconds", 10, "curse_cd", 14, "curse_cast", 1.5,
            "nova_units", 0.45, "nova_cd", 19, "nova_cast", 2.4));

    public static final Type LICH = reg("lich", "Король-лич Карн", "kurayami", p(
            "level", 10, "hp_units", 66, "melee_units", 0.18,
            "scythe_units", 0.9, "scythe_cd", 11, "scythe_cast", 1.8,
            "storm_units", 0.45, "storm_cd", 15, "storm_cast", 2.2,
            "tomb_units", 0.9, "tomb_seconds", 8, "tomb_hp_units", 1.0, "tomb_cd", 24, "tomb_cast", 1.5,
            "raise_cd", 40, "raise_cast", 2.5,
            "dark_units", 0.07, "dark_seconds", 8, "dark_cd", 30, "dark_cast", 2.0,
            "phylactery_hp_units", 5));

    private BossRegistry() {
    }

    private static Map<String, Double> p(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return Collections.unmodifiableMap(m);
    }

    static Type reg(String id, String name, String music, Map<String, Double> params) {
        Type t = new Type(id, name, music, params);
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
