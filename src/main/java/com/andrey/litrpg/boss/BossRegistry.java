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
            "level", 10, "hp_units", 60, "melee_units", 0.33,
            "smash_units", 0.9, "smash_cd", 5, "smash_cast", 1,
            "wave_units", 0.6, "wave_cd", 9, "wave_cast", 1.1,
            "meteor_units", 0.5, "meteor_cd", 7, "meteor_cast", 1,
            "burden_units", 2.4, "burden_cd", 11, "burden_cast", 1.4,
            "mend_share", 0.1, "mend_cd", 12, "mend_cast", 1.6,
            "node_hp_units", 4, "spark_hp_units", 1.2, "spark_units", 0.06,
            "salvo_units", 0.4, "salvo_cd", 18, "salvo_cast", 1.3, "salvo_shots", 6));

    // ---------------------------------------------------------------- Этап 1 · Склеп Забытых
    public static final Type GATEKEEPER = reg("gatekeeper", "Привратник Склепа", "fragment", p(
            "level", 4, "hp_units", 38, "melee_units", 0.308,
            "chain_units", 0.75, "chain_cd", 4, "chain_cast", 1,
            "rampart_units", 0.55, "rampart_cd", 9, "rampart_cast", 1.2,
            "call_cd", 12, "call_cast", 1,
            "shield_share", 0.08, "shield_cd", 12, "shield_cast", 1.4,
            "salvo_units", 0.45, "salvo_cd", 17, "salvo_cast", 1.3, "salvo_shots", 6,
            "lunge_units", 0.9, "lunge_cd", 21, "lunge_cast", 1.4, "lunge_push", 1.1));

    /** Сёстры: Морра (бьёт) — хранитель из схемы; Весса (колдует) появляется вместе с ней. */
    public static final Type MORRA = reg("morra", "Морра", "fight_club", p(
            "level", 7, "hp_units", 24, "melee_units", 0.352,
            "reap_units", 0.8, "reap_cd", 4, "reap_cast", 1,
            "spears_units", 0.5, "spears_cd", 8, "spears_cast", 1,
            "revive_seconds", 12, "link_cut", 0.5, "link_units", 0.06,
            "bond_units", 0.3, "bond_cd", 20, "bond_cast", 1.2, "bond_seconds", 8, "bond_dist", 7,
            "waves_units", 0.75, "waves_cd", 18, "waves_cast", 1.4, "waves_waves", 3, "waves_gap", 70));
    public static final Type VESSA = reg("vessa", "Весса", "fight_club", p(
            "level", 7, "hp_units", 20, "melee_units", 0.176,
            "mend_share", 0.08, "mend_cd", 9, "mend_cast", 1.4,
            "curse_units", 0.05, "curse_seconds", 10, "curse_cd", 6, "curse_cast", 1,
            "nova_units", 0.5, "nova_cd", 9, "nova_cast", 1.1,
            "creep_units", 0.85, "creep_cd", 19, "creep_cast", 1.3, "creep_seconds", 6, "creep_radius", 3,
            "stare_units", 0.3, "stare_cd", 22, "stare_cast", 1.4, "stare_seconds", 5, "stare_angle", 80));

    public static final Type LICH = reg("lich", "Король-лич Карн", "kurayami", p(
            "level", 10, "hp_units", 56, "melee_units", 0.396,
            "scythe_units", 0.9, "scythe_cd", 5, "scythe_cast", 1,
            "storm_units", 0.5, "storm_cd", 7, "storm_cast", 1,
            "tomb_units", 0.75, "tomb_seconds", 8, "tomb_hp_units", 1.0, "tomb_cd", 11, "tomb_cast", 1,
            "raise_cd", 12, "raise_cast", 1.1,
            "dark_units", 0.07, "dark_seconds", 8, "dark_cd", 12, "dark_cast", 1,
            "phylactery_hp_units", 5,
            "frost_units", 0.8, "frost_cd", 24, "frost_cast", 1.6, "frost_waves", 3, "frost_safe", 4, "frost_radius", 3));

    // ---------------------------------------------------------------- Этап 2 · Паучья чаща
    public static final Type ARACHNA = reg("arachna", "Аракна, Королева пауков", "blud", p(
            "level", 14, "hp_units", 60, "melee_units", 0.352,
            "bite_units", 0.85, "poison_units", 0.04, "bite_cd", 4, "bite_cast", 1,
            "web_units", 0.5, "web_cd", 6, "web_cast", 1,
            "cocoon_units", 0.8, "cocoon_seconds", 9, "cocoon_hp_units", 1.1, "cocoon_poison_units", 0.04, "cocoon_cd", 10, "cocoon_cast", 1,
            "eggs_cd", 12, "eggs_cast", 1, "egg_hp_units", 0.7, "egg_seconds", 12,
            "aloft_seconds", 12, "rain_units", 0.5, "drop_units", 0.85,
            "skitter_units", 0.9, "skitter_cd", 18, "skitter_cast", 1.2, "skitter_seconds", 6, "skitter_radius", 3,
            "threads_units", 0.3, "threads_cd", 21, "threads_cast", 1.3, "threads_seconds", 8, "threads_dist", 7));

    public static final Type ROT_ENT = reg("rot_ent", "Гнилой Древень", "jane", p(
            "level", 17, "hp_units", 66, "melee_units", 0.396,
            "slam_units", 0.95, "slam_cd", 4, "slam_cast", 1,
            "roots_units", 0.5, "roots_cd", 7, "roots_cast", 1,
            "spore_units", 0.08, "spore_grow", 0.25, "spore_seconds", 26, "spores_cd", 8, "spores_cast", 1,
            "heart_share", 0.1, "heart_cd", 12, "heart_cast", 1.6,
            "circles_units", 0.7, "circles_cd", 19, "circles_cast", 1.4, "circles_waves", 3, "circles_gap", 75,
            "bind_units", 0.3, "bind_cd", 22, "bind_cast", 1.3, "bind_seconds", 8, "bind_dist", 6));

    public static final Type MORENA = reg("morena", "Морена, Ведьма Изнанки", "fear", p(
            "level", 20, "hp_units", 74, "melee_units", 0.396,
            "hex_units", 0.55, "hex_cd", 4, "hex_cast", 1,
            "fear_seconds", 7, "fear_cd", 8, "fear_cast", 1,
            "cauldron_units", 0.09, "cauldron_seconds", 20, "cauldron_cd", 7, "cauldron_cast", 1,
            "inside_units", 0.7, "inside_cd", 10, "inside_cast", 1,
            "eye_units", 0.32, "eye_cd", 20, "eye_cast", 1.4, "eye_seconds", 5, "eye_angle", 80,
            "crawl_units", 0.9, "crawl_cd", 19, "crawl_cast", 1.2, "crawl_seconds", 6, "crawl_radius", 3.2));

    // ---------------------------------------------------------------- Этап 3 · Затопленный храм
    public static final Type TIDE_WARDEN = reg("tide_warden", "Страж Прилива", "woops", p(
            "level", 24, "hp_units", 85, "melee_units", 0.396,
            "trident_units", 0.95, "trident_cd", 4, "trident_cast", 1,
            "wave_units", 0.85, "wave_cd", 9, "wave_cast", 1.6,
            "whirl_units", 0.1, "whirl_seconds", 12, "whirl_cd", 12, "whirl_cast", 1,
            "bubble_units", 0.5, "bubbles_cd", 6, "bubbles_cast", 1,
            "salvo_units", 0.5, "salvo_cd", 17, "salvo_cast", 1.3, "salvo_shots", 7,
            "surge_units", 0.8, "surge_cd", 19, "surge_cast", 1.4, "surge_waves", 3, "surge_gap", 70));

    public static final Type LEVIATHAN = reg("leviathan", "Левиафан", "red", p(
            "level", 27, "hp_units", 93, "melee_units", 0.44,
            "bite_units", 1.0, "bite_cd", 4, "bite_cast", 1,
            "tail_units", 0.6, "tail_cd", 6, "tail_cast", 1,
            "emerge_units", 0.8, "dive_cd", 10, "dive_cast", 1,
            "tide_units", 0.12, "tide_seconds", 22,
            "ram_units", 1, "ram_cd", 18, "ram_cast", 1.3, "ram_push", 1.2,
            "flood_units", 0.8, "flood_cd", 24, "flood_cast", 1.6, "flood_waves", 3, "flood_safe", 4, "flood_radius", 3));

    public static final Type NAIAD = reg("naiad", "Наяра, Жрица Глубин", "aura", p(
            "level", 30, "hp_units", 100, "melee_units", 0.44,
            "staff_units", 0.95, "staff_cd", 4, "staff_cast", 1,
            "spears_units", 0.55, "spears_cd", 6, "spears_cast", 1,
            "song_units", 0.6, "song_cd", 12, "song_cast", 1.6,
            "radiance_seconds", 10, "radiance_cut", 0.9, "radiance_cd", 12, "radiance_cast", 1,
            "chase_units", 0.95, "chase_cd", 18, "chase_cast", 1.2, "chase_seconds", 6, "chase_radius", 3.2,
            "duet_units", 0.32, "duet_cd", 21, "duet_cast", 1.3, "duet_seconds", 8, "duet_dist", 7));

    // ---------------------------------------------------------------- Этап 4 · Кузни Пепла
    public static final Type SMELTER = reg("smelter", "Плавильный голем", "ring_frei", p(
            "level", 34, "hp_units", 112, "melee_units", 0.44,
            "hammer_units", 1.0, "hammer_cd", 4, "hammer_cast", 1,
            "pour_units", 0.55, "pour_burn_units", 0.1, "pour_seconds", 10, "pour_cd", 7, "pour_cast", 1,
            "heat_radius", 5, "heat_max", 6, "melt_units", 0.55, "rivet_hp_units", 3.0,
            "sparks_units", 0.5, "sparks_cd", 16, "sparks_cast", 1.2, "sparks_shots", 7,
            "rivers_units", 0.85, "rivers_cd", 23, "rivers_cast", 1.6, "rivers_waves", 3, "rivers_safe", 4, "rivers_radius", 3));

    public static final Type CHARIOT = reg("chariot", "Колесница Пламени", "formula", p(
            "level", 37, "hp_units", 121, "melee_units", 0.44,
            "charge_units", 0.85, "charge_cd", 4, "charge_cast", 1,
            "ring_units", 0.6, "ring_cd", 7, "ring_cast", 1,
            "trail_units", 0.1, "trail_seconds", 9, "pit_seconds", 7, "pit_damage", 1.6,
            "wheels_units", 0.8, "wheels_cd", 18, "wheels_cast", 1.3, "wheels_waves", 3, "wheels_gap", 70,
            "trample_units", 1.05, "trample_cd", 17, "trample_cast", 1.2, "trample_push", 1.3));

    public static final Type ASH_LORD = reg("ash_lord", "Владыка Пепла", "blue_lock", p(
            "level", 40, "hp_units", 129, "melee_units", 0.484,
            "sword_units", 1.05, "sword_cd", 4, "sword_cast", 1,
            "storm_units", 0.6, "storm_cd", 8, "storm_cast", 1.4,
            "collapse_units", 0.5, "lava_units", 0.13, "lava_seconds", 16, "collapse_cd", 6, "collapse_cast", 1,
            "duel_units", 0.5, "duel_seconds", 10, "duel_cut", 0.9, "ego_cd", 12, "ego_cast", 1,
            "erupt_units", 0.5, "erupt_cd", 4, "erupt_cast", 1,
            "ashfall_units", 0.9, "ashfall_cd", 24, "ashfall_cast", 1.6, "ashfall_waves", 3, "ashfall_safe", 4, "ashfall_radius", 3));

    // ---------------------------------------------------------------- Этап 5 · Шпиль Бездны
    public static final Type DOPPEL = reg("doppel", "Двойник", "next_opponent", p(
            "level", 44, "hp_units", 143, "melee_units", 0.484,
            "strike_units", 1.05, "strike_cd", 4, "strike_cast", 1,
            "mirror_seconds", 4, "mirror_share", 0.6, "mirror_cd", 11, "mirror_cast", 1.1,
            "swap_cd", 9, "swap_cast", 1, "shadow_cut", 0.9,
            "link_units", 0.34, "link_cd", 20, "link_cast", 1.3, "link_seconds", 8, "link_dist", 7,
            "copies_units", 0.5, "copies_cd", 18, "copies_cast", 1.3, "copies_shots", 7));

    public static final Type CHRONOPHAGE = reg("chronophage", "Хронофаг", "no_time", p(
            "level", 47, "hp_units", 151, "melee_units", 0.484,
            "slash_units", 1.05, "slash_cd", 4, "slash_cast", 1,
            "glass_hp_units", 1.4, "glass_seconds", 16, "stop_units", 0.5, "glass_cd", 12, "glass_cast", 1,
            "rewind_units", 0.7, "rewind_seconds", 6, "rewind_cd", 10, "rewind_cast", 1,
            "waves_units", 0.85, "waves_cd", 18, "waves_cast", 1.4, "waves_waves", 3, "waves_gap", 70,
            "clock_units", 1, "clock_cd", 19, "clock_cast", 1.2, "clock_seconds", 6, "clock_radius", 3.2));

    public static final Type XALATHAR = reg("xalathar", "Ксалатар, Голод Бездны", "wutiwant", p(
            "level", 50, "hp_units", 155, "melee_units", 0.528,
            "smash_units", 1.1, "smash_cd", 4, "smash_cast", 1,
            "gaze_units", 0.7, "gaze_cd", 5, "gaze_cast", 1,
            "hunger_units", 0.8, "hunger_cd", 8, "hunger_cast", 1.1,
            "edge_units", 0.7, "void_units", 0.5, "edge_cd", 12, "edge_cast", 1.1,
            "end_units", 0.18, "end_period", 10,
            "voidfall_units", 0.95, "voidfall_cd", 24, "voidfall_cast", 1.6, "voidfall_waves", 3, "voidfall_safe", 4, "voidfall_radius", 3));

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
