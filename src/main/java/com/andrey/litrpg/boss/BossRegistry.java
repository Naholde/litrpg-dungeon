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
            "rampart_units", 0.55, "rampart_cd", 9, "rampart_cast", 2.4,
            "call_cd", 12, "call_cast", 1,
            "shield_share", 0.08, "shield_cd", 12, "shield_cast", 1.4,
            "salvo_units", 0.45, "salvo_cd", 17, "salvo_cast", 1.3, "salvo_shots", 6,
            "lunge_units", 0.9, "lunge_cd", 21, "lunge_cast", 1.4, "lunge_push", 1.1,
            "grip_units", 0.35, "grip_pull", 1.5, "grip_cd", 15, "grip_cast", 1.3,
            "walls_units", 0.45, "walls_count", 4, "walls_cd", 20, "walls_cast", 1.5,
            "palisade_units", 0.15, "palisade_seconds", 8, "palisade_cd", 16, "palisade_cast", 1.4,
            "crypt_units", 0.15, "crypt_seconds", 10, "crypt_cd", 18, "crypt_cast", 1.3));

    /** Сёстры: Морра (бьёт) — хранитель из схемы; Весса (колдует) появляется вместе с ней. */
    public static final Type MORRA = reg("morra", "Морра", "fight_club", p(
            "level", 7, "hp_units", 24, "melee_units", 0.352,
            "reap_units", 0.8, "reap_cd", 4, "reap_cast", 1,
            "spears_units", 0.5, "spears_cd", 8, "spears_cast", 1,
            "revive_seconds", 12, "link_cut", 0.5, "link_units", 0.06,
            "bond_units", 0.3, "bond_cd", 20, "bond_cast", 1.2, "bond_seconds", 8, "bond_dist", 7,
            "waves_units", 0.75, "waves_cd", 18, "waves_cast", 1.4, "waves_waves", 3, "waves_gap", 70,
            "souls_share", 0.025, "souls_units", 0.06, "souls_speed", 2.2, "souls_cd", 22, "souls_cast", 1.2,
            "harvest_units", 0.15, "harvest_cd", 20, "harvest_cast", 1.4,
            "scythetrail_units", 0.15, "scythetrail_cd", 18, "scythetrail_cast", 1.0,
            "bondline_units", 0.2, "bondline_seconds", 8, "bondline_cd", 22, "bondline_cast", 1.2));
    public static final Type VESSA = reg("vessa", "Весса", "fight_club", p(
            "level", 7, "hp_units", 20, "melee_units", 0.176,
            "mend_share", 0.08, "mend_cd", 9, "mend_cast", 1.4,
            "curse_units", 0.05, "curse_seconds", 10, "curse_cd", 6, "curse_cast", 1,
            "nova_units", 0.5, "nova_cd", 9, "nova_cast", 1.8,
            "creep_units", 0.85, "creep_cd", 19, "creep_cast", 1.3, "creep_seconds", 6, "creep_radius", 3,
            "stare_units", 0.3, "stare_cd", 22, "stare_cast", 1.4, "stare_seconds", 5, "stare_angle", 80,
            "candles_units", 0.25, "candles_hp_units", 0.5, "candles_seconds", 16, "candles_every", 3,
            "candles_cd", 26, "candles_cast", 1.3,
            "blight_units", 0.15, "blight_seconds", 8, "blight_cd", 16, "blight_cast", 1.3,
            "darkring_units", 0.18, "darkring_seconds", 8, "darkring_cd", 20, "darkring_cast", 1.4,
            "shade_units", 0.2, "shade_seconds", 10, "shade_cd", 22, "shade_cast", 1.2));

    public static final Type LICH = reg("lich", "Король-лич Карн", "kurayami", p(
            "level", 10, "hp_units", 56, "melee_units", 0.396,
            "scythe_units", 0.9, "scythe_cd", 5, "scythe_cast", 1,
            "storm_units", 0.5, "storm_cd", 7, "storm_cast", 1.4,
            "tomb_units", 0.75, "tomb_seconds", 8, "tomb_hp_units", 1.0, "tomb_cd", 11, "tomb_cast", 1,
            "raise_cd", 12, "raise_cast", 1.1,
            "dark_units", 0.07, "dark_seconds", 8, "dark_cd", 12, "dark_cast", 1,
            "phylactery_hp_units", 5,
            "frost_units", 0.8, "frost_cd", 24, "frost_cast", 1.6, "frost_waves", 3, "frost_safe", 4, "frost_radius", 3,
            "frostpath_units", 0.45, "frostpath_speed", 7, "frostpath_cd", 13, "frostpath_cast", 1.2,
            "blizzard_units", 0.12, "blizzard_seconds", 6, "blizzard_move", 1.5, "blizzard_cd", 26, "blizzard_cast", 1.4,
            "icefield_units", 0.15, "icefield_seconds", 8, "icefield_cd", 20, "icefield_cast", 1.5,
            "icewalls_units", 0.18, "icewalls_seconds", 10, "icewalls_cd", 22, "icewalls_cast", 1.5));

    // ---------------------------------------------------------------- Этап 2 · Паучья чаща
    public static final Type ARACHNA = reg("arachna", "Аракна, Королева пауков", "blud", p(
            "level", 14, "hp_units", 60, "melee_units", 0.352,
            "bite_units", 0.85, "poison_units", 0.04, "bite_cd", 4, "bite_cast", 1,
            "web_units", 0.5, "web_cd", 6, "web_cast", 1,
            "cocoon_units", 0.8, "cocoon_seconds", 9, "cocoon_hp_units", 1.1, "cocoon_poison_units", 0.04, "cocoon_cd", 10, "cocoon_cast", 1,
            "eggs_cd", 12, "eggs_cast", 1, "egg_hp_units", 0.7, "egg_seconds", 12,
            "aloft_seconds", 12, "rain_units", 0.5, "drop_units", 0.85,
            "skitter_units", 0.9, "skitter_cd", 18, "skitter_cast", 1.2, "skitter_seconds", 6, "skitter_radius", 3,
            "threads_units", 0.3, "threads_cd", 21, "threads_cast", 1.3, "threads_seconds", 8, "threads_dist", 7,
            "pounce_units", 0.7, "pounce_cd", 16, "pounce_cast", 1.5,
            "venom_units", 0.45, "venom_cd", 18, "venom_cast", 1.2,
            "venompools_units", 0.15, "venompools_seconds", 12, "venompools_cd", 16, "venompools_cast", 1.3,
            "net_units", 0.08, "net_seconds", 8, "net_cd", 22, "net_cast", 1.6));

    public static final Type ROT_ENT = reg("rot_ent", "Гнилой Древень", "jane", p(
            "level", 17, "hp_units", 66, "melee_units", 0.396,
            "slam_units", 0.95, "slam_cd", 4, "slam_cast", 1,
            "roots_units", 0.5, "roots_cd", 7, "roots_cast", 1.3,
            "spore_units", 0.08, "spore_grow", 0.25, "spore_seconds", 26, "spores_cd", 8, "spores_cast", 1,
            "heart_share", 0.1, "heart_cd", 12, "heart_cast", 1.6,
            "circles_units", 0.7, "circles_cd", 19, "circles_cast", 1.4, "circles_waves", 3, "circles_gap", 75,
            "bind_units", 0.3, "bind_cd", 22, "bind_cast", 1.3, "bind_seconds", 8, "bind_dist", 6,
            "fruit_hp_units", 0.55, "fruit_seconds", 7, "fruit_cd", 20, "fruit_cast", 1.2,
            "grasp_units", 0.8, "grasp_pull", 0.07, "grasp_seconds", 4, "grasp_cd", 24, "grasp_cast", 1.2,
            "rootgrid_units", 0.15, "rootgrid_seconds", 6, "rootgrid_cd", 20, "rootgrid_cast", 1.5,
            "sporetrail_units", 0.12, "sporetrail_cd", 22, "sporetrail_cast", 1.0));

    public static final Type MORENA = reg("morena", "Морена, Ведьма Изнанки", "fear", p(
            "level", 20, "hp_units", 74, "melee_units", 0.396,
            "hex_units", 0.55, "hex_cd", 4, "hex_cast", 1,
            "fear_seconds", 7, "fear_cd", 8, "fear_cast", 1,
            "cauldron_units", 0.09, "cauldron_seconds", 20, "cauldron_cd", 7, "cauldron_cast", 1,
            "inside_units", 0.7, "inside_cd", 10, "inside_cast", 2.4,
            "eye_units", 0.32, "eye_cd", 20, "eye_cast", 1.4, "eye_seconds", 5, "eye_angle", 80,
            "crawl_units", 0.9, "crawl_cd", 19, "crawl_cast", 1.2, "crawl_seconds", 6, "crawl_radius", 3.2,
            "grove_cd", 26, "grove_cast", 1.4, "grove_count", 6, "grove_height", 4, "grove_seconds", 16,
            "evil_seconds", 4, "evil_units", 0.15, "evil_cd", 19, "evil_cast", 1.2,
            "dance_units", 0.35, "dance_seconds", 7, "dance_cd", 22, "dance_cast", 1.3,
            "witchcircle_units", 0.15, "witchcircle_seconds", 7, "witchcircle_cd", 18, "witchcircle_cast", 1.3,
            "checker_units", 0.2, "checker_cd", 24, "checker_cast", 1.5));

    // ---------------------------------------------------------------- Этап 3 · Затопленный храм
    public static final Type TIDE_WARDEN = reg("tide_warden", "Страж Прилива", "woops", p(
            "level", 24, "hp_units", 152, "melee_units", 0.56,
            "trident_units", 1.425, "trident_cd", 4, "trident_cast", 1,
            "wave_units", 1.275, "wave_cd", 9, "wave_cast", 1.6,
            "whirl_units", 0.15, "whirl_seconds", 12, "whirl_cd", 12, "whirl_cast", 1,
            "bubble_units", 0.75, "bubbles_cd", 6, "bubbles_cast", 1.3,
            "salvo_units", 0.75, "salvo_cd", 17, "salvo_cast", 1.3, "salvo_shots", 7,
            "surge_units", 1.2, "surge_cd", 19, "surge_cast", 1.4, "surge_waves", 3, "surge_gap", 70,
            "columns_cd", 26, "columns_cast", 1.4, "columns_count", 6, "columns_height", 4, "columns_seconds", 18,
            "spray_units", 0.6, "spray_cd", 14, "spray_cast", 1.1,
            "backlash_units", 0.9, "backlash_push", 1.3, "backlash_cd", 20, "backlash_cast", 1.5,
            "frenzy_surge", 16, "frenzy_window", 6, "frenzy_bonus", 2.8, "frenzy_armor", 0.4, "frenzy_pace", 1.6,
            "currents_units", 0.1, "currents_seconds", 10, "currents_cd", 20, "currents_cast", 1.4,
            "foam_units", 0.15, "foam_seconds", 10, "foam_cd", 16, "foam_cast", 1.2));

    public static final Type LEVIATHAN = reg("leviathan", "Левиафан", "red", p(
            "level", 27, "hp_units", 153, "melee_units", 0.44,
            "bite_units", 1.5, "bite_cd", 4, "bite_cast", 1,
            "tail_units", 0.9, "tail_cd", 6, "tail_cast", 1.6,
            "emerge_units", 1.2, "dive_cd", 10, "dive_cast", 1,
            "tide_units", 0.18, "tide_seconds", 22,
            "ram_units", 1.5, "ram_cd", 18, "ram_cast", 1.3, "ram_push", 1.2,
            "flood_units", 1.2, "flood_cd", 24, "flood_cast", 1.6, "flood_waves", 3, "flood_safe", 4, "flood_radius", 3,
            "wall_cd", 24, "wall_cast", 1.4, "wall_height", 3, "wall_seconds", 10,
            "deluge_cd", 28, "deluge_cast", 1.6, "deluge_seconds", 12, "deluge_units", 0.25,
            "spout_units", 0.22, "spout_seconds", 8, "spout_cd", 20, "spout_cast", 1.2,
            "roar_units", 0.45, "roar_silence", 2.5, "roar_cd", 22, "roar_cast", 2.2,
            "ink_units", 0.15, "ink_seconds", 10, "ink_cd", 20, "ink_cast", 1.3,
            "undertow_units", 0.08, "undertow_seconds", 6, "undertow_cd", 22, "undertow_cast", 1.5));

    public static final Type NAIAD = reg("naiad", "Наяра, Жрица Глубин", "aura", p(
            "level", 30, "hp_units", 156, "melee_units", 0.44,
            "staff_units", 1.425, "staff_cd", 4, "staff_cast", 1,
            "spears_units", 0.825, "spears_cd", 6, "spears_cast", 1,
            "song_units", 0.9, "song_cd", 12, "song_cast", 1.6,
            "radiance_seconds", 10, "radiance_cut", 0.9, "radiance_cd", 12, "radiance_cast", 1,
            "chase_units", 1.425, "chase_cd", 18, "chase_cast", 1.2, "chase_seconds", 6, "chase_radius", 3.2,
            "duet_units", 0.48, "duet_cd", 21, "duet_cast", 1.3, "duet_seconds", 8, "duet_dist", 7,
            "ray_cd", 26, "ray_cast", 1.5, "ray_seconds", 6, "ray_units", 0.3,
            "coral_cd", 27, "coral_cast", 1.4, "coral_count", 7, "coral_height", 4, "coral_seconds", 16,
            "spiral_units", 0.6, "spiral_cd", 15, "spiral_cast", 1.2,
            "tears_share", 0.03, "tears_units", 0.3, "tears_cd", 24, "tears_cast", 1.3,
            "pearlrings_units", 0.2, "pearlrings_cd", 20, "pearlrings_cast", 1.4,
            "holywater_share", 0.01, "holywater_seconds", 12, "holywater_cd", 26, "holywater_cast", 1.2));

    // ---------------------------------------------------------------- Этап 4 · Кузни Пепла
    public static final Type SMELTER = reg("smelter", "Плавильный голем", "ring_frei", p(
            "level", 34, "hp_units", 180, "melee_units", 0.44,
            "hammer_units", 1.5, "hammer_cd", 4, "hammer_cast", 1,
            "pour_units", 0.825, "pour_burn_units", 0.15, "pour_seconds", 10, "pour_cd", 7, "pour_cast", 1,
            "heat_radius", 5, "heat_max", 6, "melt_units", 0.825, "rivet_hp_units", 3.0,
            "sparks_units", 0.75, "sparks_cd", 16, "sparks_cast", 1.2, "sparks_shots", 7,
            "rivers_units", 1.275, "rivers_cd", 23, "rivers_cast", 1.6, "rivers_waves", 3, "rivers_safe", 4, "rivers_radius", 3,
            "molten_cd", 25, "molten_cast", 1.5, "molten_sectors", 6, "molten_seconds", 12, "molten_units", 0.3,
            "castwall_cd", 26, "castwall_cast", 1.4, "castwall_height", 3, "castwall_seconds", 10,
            "anvils_units", 0.8, "anvils_cd", 16, "anvils_cast", 1.2,
            "ingot_units", 0.9, "ingot_seconds", 5, "ingot_passes", 3, "ingot_cd", 26, "ingot_cast", 1.2,
            "spill_units", 0.18, "spill_seconds", 10, "spill_cd", 18, "spill_cast", 1.4,
            "hotfloor_units", 0.2, "hotfloor_cd", 22, "hotfloor_cast", 1.5));

    public static final Type CHARIOT = reg("chariot", "Колесница Пламени", "formula", p(
            "level", 37, "hp_units", 195, "melee_units", 0.44,
            "charge_units", 1.275, "charge_cd", 4, "charge_cast", 1,
            "ring_units", 0.9, "ring_cd", 7, "ring_cast", 1.5,
            "trail_units", 0.15, "trail_seconds", 9, "pit_seconds", 7, "pit_damage", 1.6,
            "wheels_units", 1.2, "wheels_cd", 18, "wheels_cast", 1.3, "wheels_waves", 3, "wheels_gap", 70,
            "trample_units", 1.575, "trample_cd", 17, "trample_cast", 1.2, "trample_push", 1.3,
            "firesweep_cd", 24, "firesweep_cast", 1.4, "firesweep_seconds", 6, "firesweep_units", 0.32,
            "exhaust_units", 0.8, "exhaust_cd", 12, "exhaust_cast", 1.1,
            "phantoms_units", 0.9, "phantoms_cd", 22, "phantoms_cast", 1.6,
            "ruts_units", 0.18, "ruts_seconds", 12, "ruts_cd", 20, "ruts_cast", 1.3,
            "track_units", 0.18, "track_seconds", 8, "track_cd", 24, "track_cast", 1.5));

    public static final Type ASH_LORD = reg("ash_lord", "Владыка Пепла", "blue_lock", p(
            "level", 40, "hp_units", 213, "melee_units", 0.484,
            "sword_units", 1.575, "sword_cd", 4, "sword_cast", 1,
            "storm_units", 0.9, "storm_cd", 8, "storm_cast", 2.4,
            "collapse_units", 0.75, "lava_units", 0.195, "lava_seconds", 16, "collapse_cd", 6, "collapse_cast", 1.5,
            "duel_units", 0.75, "duel_seconds", 10, "duel_cut", 0.9, "ego_cd", 12, "ego_cast", 1,
            "erupt_units", 0.75, "erupt_cd", 4, "erupt_cast", 1.4,
            "ashfall_units", 1.35, "ashfall_cd", 24, "ashfall_cast", 1.6, "ashfall_waves", 3, "ashfall_safe", 4, "ashfall_radius", 3,
            "veins_cd", 26, "veins_cast", 1.5, "veins_sectors", 6, "veins_seconds", 12, "veins_units", 0.32,
            "closing_cd", 30, "closing_cast", 1.6, "closing_seconds", 10, "closing_units", 0.4,
            "brand_seconds", 6, "brand_units", 0.12, "brand_cd", 20, "brand_cast", 1.2,
            "crown_units", 0.75, "crown_cd", 22, "crown_cast", 1.3,
            "ashcloud_units", 0.15, "ashcloud_seconds", 10, "ashcloud_cd", 20, "ashcloud_cast", 1.3,
            "lavalakes_units", 0.25, "lavalakes_seconds", 15, "lavalakes_cd", 26, "lavalakes_cast", 1.5));

    // ---------------------------------------------------------------- Этап 5 · Шпиль Бездны
    public static final Type DOPPEL = reg("doppel", "Двойник", "next_opponent", p(
            "level", 44, "hp_units", 240, "melee_units", 0.484,
            "strike_units", 1.575, "strike_cd", 4, "strike_cast", 1,
            "mirror_seconds", 4, "mirror_share", 0.6, "mirror_cd", 11, "mirror_cast", 1.1,
            "swap_cd", 9, "swap_cast", 1, "shadow_cut", 0.9,
            "link_units", 0.51, "link_cd", 20, "link_cast", 1.3, "link_seconds", 8, "link_dist", 7,
            "copies_units", 0.75, "copies_cd", 18, "copies_cast", 1.3, "copies_shots", 7,
            "places_cd", 20, "places_cast", 1.2,
            "glasswall_cd", 25, "glasswall_cast", 1.4, "glasswall_height", 3, "glasswall_seconds", 10,
            "shards_units", 0.6, "shards_count", 10, "shards_cd", 16, "shards_cast", 1.2,
            "fan_units", 1.0, "fan_cd", 14, "fan_cast", 1.2,
            "mirrorpools_units", 0.18, "mirrorpools_seconds", 8, "mirrorpools_cd", 18, "mirrorpools_cast", 1.3,
            "corridor_units", 0.25, "corridor_seconds", 5, "corridor_cd", 26, "corridor_cast", 1.8));

    public static final Type CHRONOPHAGE = reg("chronophage", "Хронофаг", "no_time", p(
            "level", 47, "hp_units", 255, "melee_units", 0.484,
            "slash_units", 1.575, "slash_cd", 4, "slash_cast", 1,
            "glass_hp_units", 1.4, "glass_seconds", 16, "stop_units", 0.75, "glass_cd", 12, "glass_cast", 1,
            "rewind_units", 1.05, "rewind_seconds", 6, "rewind_cd", 10, "rewind_cast", 1,
            "waves_units", 1.275, "waves_cd", 18, "waves_cast", 1.4, "waves_waves", 3, "waves_gap", 70,
            "clock_units", 1.5, "clock_cd", 19, "clock_cast", 1.2, "clock_seconds", 6, "clock_radius", 3.2,
            "closing_cd", 30, "closing_cast", 1.6, "closing_seconds", 10, "closing_units", 0.42,
            "hand_cd", 24, "hand_cast", 1.4, "hand_seconds", 7, "hand_units", 0.34,
            "echo_units", 0.65, "echo_delay", 1.6, "echo_cd", 15, "echo_cast", 1.2,
            "stasis_units", 0.08, "stasis_seconds", 9, "stasis_cd", 22, "stasis_cast", 1.2,
            "clockhand_units", 0.2, "clockhand_cd", 22, "clockhand_cast", 1.2,
            "frozensteps_units", 0.18, "frozensteps_cd", 22, "frozensteps_cast", 1.0));

    public static final Type XALATHAR = reg("xalathar", "Ксалатар, Голод Бездны", "wutiwant", p(
            "level", 50, "hp_units", 264, "melee_units", 0.528,
            "smash_units", 1.65, "smash_cd", 4, "smash_cast", 1,
            "gaze_units", 1.05, "gaze_cd", 5, "gaze_cast", 1,
            "hunger_units", 1.2, "hunger_cd", 8, "hunger_cast", 1.1,
            "edge_units", 1.05, "void_units", 0.75, "edge_cd", 12, "edge_cast", 1.8,
            "end_units", 0.27, "end_period", 10,
            "voidfall_units", 1.425, "voidfall_cd", 24, "voidfall_cast", 1.6, "voidfall_waves", 3, "voidfall_safe", 4, "voidfall_radius", 3,
            "edgefall_cd", 30, "edgefall_cast", 1.6, "edgefall_seconds", 10, "edgefall_units", 0.45,
            "ray_cd", 24, "ray_cast", 1.4, "ray_seconds", 7, "ray_units", 0.36,
            "pillars_cd", 28, "pillars_cast", 1.4, "pillars_count", 6, "pillars_height", 4, "pillars_seconds", 16,
            "maw_units", 1.0, "maw_cd", 16, "maw_cast", 1.5,
            "eyes_units", 0.5, "eyes_hp_units", 0.8, "eyes_seconds", 18, "eyes_every", 3, "eyes_cd", 28, "eyes_cast", 1.3,
            "rifts_units", 0.2, "rifts_seconds", 10, "rifts_cd", 20, "rifts_cast", 1.5,
            "gloom_units", 0.22, "gloom_seconds", 6, "gloom_cd", 24, "gloom_cast", 1.8));

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
