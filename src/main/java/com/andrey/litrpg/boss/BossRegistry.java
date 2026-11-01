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

    // ---------------------------------------------------------------- Этап 2 · Паучья чаща
    public static final Type ARACHNA = reg("arachna", "Аракна, Королева пауков", "blud", p(
            "level", 14, "hp_units", 62, "melee_units", 0.16,
            "bite_units", 0.85, "poison_units", 0.04, "bite_cd", 9, "bite_cast", 1.4,
            "web_units", 0.3, "web_cd", 14, "web_cast", 1.8,
            "cocoon_units", 0.8, "cocoon_seconds", 9, "cocoon_hp_units", 1.1, "cocoon_poison_units", 0.04, "cocoon_cd", 22, "cocoon_cast", 1.2,
            "eggs_cd", 32, "eggs_cast", 2.0, "egg_hp_units", 0.7, "egg_seconds", 12,
            "aloft_seconds", 12, "rain_units", 0.35, "drop_units", 1.1));

    public static final Type ROT_ENT = reg("rot_ent", "Гнилой Древень", "jane", p(
            "level", 17, "hp_units", 70, "melee_units", 0.18,
            "slam_units", 0.95, "slam_cd", 10, "slam_cast", 1.8,
            "roots_units", 0.35, "roots_cd", 15, "roots_cast", 2.0,
            "spore_units", 0.08, "spore_grow", 0.25, "spore_seconds", 26, "spores_cd", 18, "spores_cast", 1.5,
            "heart_share", 0.1, "heart_cd", 28, "heart_cast", 3.5));

    public static final Type MORENA = reg("morena", "Морена, Ведьма Изнанки", "fear", p(
            "level", 20, "hp_units", 80, "melee_units", 0.18,
            "hex_units", 0.55, "hex_cd", 8, "hex_cast", 1.5,
            "fear_seconds", 7, "fear_cd", 17, "fear_cast", 1.2,
            "cauldron_units", 0.09, "cauldron_seconds", 20, "cauldron_cd", 16, "cauldron_cast", 1.5,
            "inside_units", 0.7, "inside_cd", 22, "inside_cast", 2.2));

    // ---------------------------------------------------------------- Этап 3 · Затопленный храм
    public static final Type TIDE_WARDEN = reg("tide_warden", "Страж Прилива", "woops", p(
            "level", 24, "hp_units", 72, "melee_units", 0.18,
            "trident_units", 0.95, "trident_cd", 9, "trident_cast", 1.6,
            "wave_units", 1.0, "wave_cd", 20, "wave_cast", 3.5,
            "whirl_units", 0.1, "whirl_seconds", 12, "whirl_cd", 28, "whirl_cast", 1.5,
            "bubble_units", 0.4, "bubbles_cd", 13, "bubbles_cast", 2.0));

    public static final Type LEVIATHAN = reg("leviathan", "Левиафан", "red", p(
            "level", 27, "hp_units", 78, "melee_units", 0.2,
            "bite_units", 1.0, "bite_cd", 9, "bite_cast", 1.6,
            "tail_units", 0.6, "tail_cd", 14, "tail_cast", 2.2,
            "emerge_units", 0.9, "dive_cd", 22, "dive_cast", 1.0,
            "tide_units", 0.12, "tide_seconds", 22));

    public static final Type NAIAD = reg("naiad", "Наяра, Жрица Глубин", "aura", p(
            "level", 30, "hp_units", 88, "melee_units", 0.2,
            "staff_units", 0.95, "staff_cd", 9, "staff_cast", 1.6,
            "spears_units", 0.55, "spears_cd", 14, "spears_cast", 2.0,
            "song_units", 0.6, "song_cd", 26, "song_cast", 3.5,
            "radiance_seconds", 10, "radiance_cut", 0.9, "radiance_cd", 30, "radiance_cast", 2.0));

    // ---------------------------------------------------------------- Этап 4 · Кузни Пепла
    public static final Type SMELTER = reg("smelter", "Плавильный голем", "ring_frei", p(
            "level", 34, "hp_units", 80, "melee_units", 0.2,
            "hammer_units", 1.0, "hammer_cd", 9, "hammer_cast", 1.7,
            "pour_units", 0.55, "pour_burn_units", 0.1, "pour_seconds", 10, "pour_cd", 15, "pour_cast", 2.0,
            "heat_radius", 5, "heat_max", 6, "melt_units", 0.55, "rivet_hp_units", 3.0));

    public static final Type CHARIOT = reg("chariot", "Колесница Пламени", "formula", p(
            "level", 37, "hp_units", 76, "melee_units", 0.2,
            "charge_units", 0.85, "charge_cd", 7, "charge_cast", 2.2,
            "ring_units", 0.6, "ring_cd", 16, "ring_cast", 2.0,
            "trail_units", 0.1, "trail_seconds", 9, "pit_seconds", 7, "pit_damage", 1.6));

    public static final Type ASH_LORD = reg("ash_lord", "Владыка Пепла", "blue_lock", p(
            "level", 40, "hp_units", 95, "melee_units", 0.22,
            "sword_units", 1.05, "sword_cd", 9, "sword_cast", 1.7,
            "storm_units", 0.6, "storm_cd", 17, "storm_cast", 2.4,
            "collapse_units", 0.5, "lava_units", 0.13, "lava_seconds", 16, "collapse_cd", 14, "collapse_cast", 2.0,
            "duel_units", 0.32, "duel_seconds", 10, "duel_cut", 0.9, "ego_cd", 34, "ego_cast", 1.5,
            "erupt_units", 0.45, "erupt_cd", 9, "erupt_cast", 1.8));

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
