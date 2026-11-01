package com.andrey.litrpg.mob;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Виды существ подземелья. Цифры (живучесть и урон долями «единицы», скорость) — по умолчанию здесь,
 * переопределяются в {@code balance/mobs.json}. Особенность вида — в {@link #trait}.
 */
public final class Bestiary {
    /**
     * @param stage     этап, где встречается
     * @param model     модель GeckoLib
     * @param scale     масштаб модели
     * @param color     цвет снарядов/эффектов
     * @param trait     короткое описание особенности (для Кодекса)
     */
    public record Species(String id, String name, int stage, MobRole role, String model, float scale, float width,
                          float height, int color, String trait, Map<String, Double> params) {
        public double param(String key) {
            return params.getOrDefault(key, 0.0);
        }
    }

    private static final Map<String, Species> ALL = new LinkedHashMap<>();

    // ---------------------------------------------------------------- Этап 1 · Склеп Забытых (нежить)
    public static final Species CRYPT_WARRIOR = reg("crypt_warrior", "Скелет-воин", 1, MobRole.BRUTE, "crypt_warrior",
            1.0f, 0.6f, 1.95f, 0xE8DCC0, "Тяжёлый удар ржавым мечом; элитный бьёт по кругу с разметкой.",
            p("hp_units", 0.8, "dmg_units", 0.055, "speed", 0.24));
    public static final Species CRYPT_ARCHER = reg("crypt_archer", "Скелет-лучник", 1, MobRole.ARCHER, "crypt_archer",
            1.0f, 0.6f, 1.95f, 0xE8DCC0, "Держится на расстоянии и стреляет; вблизи слаб.",
            p("hp_units", 0.55, "dmg_units", 0.05, "speed", 0.26));
    public static final Species GHOUL = reg("ghoul", "Упырь", 1, MobRole.SKIRMISHER, "ghoul",
            1.0f, 0.6f, 1.6f, 0xA0C890, "Быстрый, бросается на лекарей и стрелков; укус оставляет кровоточащую рану.",
            p("hp_units", 0.65, "dmg_units", 0.04, "speed", 0.33));
    public static final Species BONE_GUARD = reg("bone_guard", "Костяной страж", 1, MobRole.GUARD, "bone_guard",
            1.1f, 0.7f, 2.1f, 0xC8D0E0, "Огромный щит спереди: урон в лицо втрое меньше — заходите со спины.",
            p("hp_units", 1.3, "dmg_units", 0.06, "speed", 0.2));
    public static final Species ACOLYTE = reg("acolyte", "Послушник некроманта", 1, MobRole.HEALER, "acolyte",
            1.0f, 0.6f, 1.95f, 0x7FF07F, "Поднимает раненых мертвецов зелёным светом — бейте его первым.",
            p("hp_units", 0.6, "dmg_units", 0.035, "speed", 0.25, "heal_share", 0.2));
    public static final Species BONE_RAT = reg("bone_rat", "Костяная крыса", 1, MobRole.SWARM, "bone_rat",
            0.8f, 0.6f, 0.5f, 0xE8DCC0, "Приходят стаей; каждая слабая, но вместе кусают больно.",
            p("hp_units", 0.25, "dmg_units", 0.02, "speed", 0.34));

    private Bestiary() {
    }

    private static Map<String, Double> p(Object... kv) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], ((Number) kv[i + 1]).doubleValue());
        return Collections.unmodifiableMap(m);
    }

    private static Species reg(String id, String name, int stage, MobRole role, String model, float scale, float w, float h,
                               int color, String trait, Map<String, Double> params) {
        Species s = new Species(id, name, stage, role, model, scale, w, h, color, trait, params);
        if (ALL.putIfAbsent(id, s) != null) throw new IllegalStateException("вид уже есть: " + id);
        return s;
    }

    public static Species get(String id) {
        return ALL.get(id);
    }

    public static Collection<Species> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    public static Map<String, Map<String, Double>> defaultValues() {
        Map<String, Map<String, Double>> out = new TreeMap<>();
        for (Species s : ALL.values()) out.put(s.id(), new LinkedHashMap<>(s.params()));
        return out;
    }
}
