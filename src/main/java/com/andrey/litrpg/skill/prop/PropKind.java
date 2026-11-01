package com.andrey.litrpg.skill.prop;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Виды объектов классов, которые стоят в мире: тотем, знамя, турель, двойник, светило, печать…
 * У каждого свой размер, своя модель (по {@code id}) и свой цвет радиуса. Объект можно сломать —
 * этим он и отличается от эффекта: его видно, у него есть здоровье и место.
 */
public record PropKind(String id, String title, float width, float height, float scale, int color) {
    private static final Map<String, PropKind> ALL = new LinkedHashMap<>();

    private static PropKind reg(String id, String title, float w, float h, float scale, int color) {
        PropKind k = new PropKind(id, title, w, h, scale, color);
        if (ALL.putIfAbsent(id, k) != null) throw new IllegalStateException("объект уже есть: " + id);
        return k;
    }

    // ---------------------------------------------------------------- танки
    /** Зеркальный двойник зерцала: тянет угрозу на себя, пока его не разбили. */
    public static final PropKind MIRROR = reg("mirror_double", "Зеркальный двойник", 0.8f, 2.0f, 1.0f, 0xB0E8FF);
    /** Оберег купола стража: пока цел, купол держится. */
    public static final PropKind DOME = reg("dome_core", "Оберег купола", 0.8f, 1.3f, 1.0f, 0x8FE3FF);

    // ---------------------------------------------------------------- поддержка
    public static final PropKind TOTEM_HEAL = reg("totem_heal", "Тотем исцеления", 0.7f, 1.8f, 1.0f, 0x7BF5A0);
    public static final PropKind TOTEM_STORM = reg("totem_storm", "Тотем бури", 0.7f, 1.8f, 1.0f, 0xB8C6FF);
    public static final PropKind TOTEM_EARTH = reg("totem_earth", "Тотем земли", 0.7f, 1.8f, 1.0f, 0xC9A055);
    public static final PropKind BANNER_POWER = reg("banner_power", "Знамя силы", 0.6f, 2.4f, 1.0f, 0xD8343A);
    public static final PropKind BANNER_GUARD = reg("banner_guard", "Знамя стойкости", 0.6f, 2.4f, 1.0f, 0x7FB8FF);
    public static final PropKind BANNER_SPEED = reg("banner_speed", "Знамя скорости", 0.6f, 2.4f, 1.0f, 0x7FF0D0);
    public static final PropKind TREE = reg("tree_life", "Живое древо", 1.4f, 3.2f, 1.0f, 0x6BE38A);

    // ---------------------------------------------------------------- бойцы
    public static final PropKind SEAL = reg("monk_seal", "Печать", 1.0f, 0.3f, 1.0f, 0xFFD27A);
    public static final PropKind BLOCK = reg("headsman_block", "Плаха", 1.4f, 0.5f, 1.0f, 0x6B2A2A);
    public static final PropKind TRAP = reg("hunter_trap", "Капкан", 1.0f, 0.4f, 1.0f, 0xC9D6E8);
    public static final PropKind TURRET = reg("turret", "Турель", 0.9f, 1.3f, 1.0f, 0xC9D6E8);
    public static final PropKind MINE = reg("mine", "Мина", 0.7f, 0.4f, 1.0f, 0xFF7A2E);
    public static final PropKind STAR = reg("star", "Светило", 0.8f, 0.8f, 1.0f, 0x7FD8FF);
    public static final PropKind TENTACLE = reg("tentacle", "Щупальце бездны", 0.8f, 2.6f, 1.0f, 0x9B5CFF);
    public static final PropKind ILLUSION = reg("illusion", "Двойник", 0.8f, 2.0f, 1.0f, 0xD6A6FF);
    public static final PropKind VOID_RIFT = reg("void_rift", "Разлом", 1.2f, 1.2f, 1.0f, 0x9B5CFF);

    public static PropKind get(String id) {
        return ALL.get(id);
    }

    public static Collection<PropKind> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }
}
