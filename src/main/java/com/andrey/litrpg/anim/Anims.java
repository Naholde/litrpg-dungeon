package com.andrey.litrpg.anim;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Все движения игрока. У каждого навыка своё движение с его именем: {@code blood.strike} — удар кровавого рыцаря.
 * Если у навыка есть движение {@code <навык>.cast}, оно играет, пока навык читается.
 * <p>Новое движение: строка в {@code Anims<Роль>}, кадры — через {@link Anim#of} и {@link Moves}.
 * Автотест {@code PlayerAnimTest} не даст собрать мод, если у навыка нет движения.
 */
public final class Anims {
    private static final Map<String, Anim> MAP = new LinkedHashMap<>();

    static {
        AnimsCommon.register();
        AnimsTank.register();
        AnimsMelee.register();
        AnimsRanged.register();
        AnimsMagic.register();
        AnimsSupport.register();
    }

    private Anims() {
    }

    public static void add(Anim a) {
        if (MAP.put(a.id, a) != null) throw new IllegalStateException("движение «" + a.id + "» уже есть");
    }

    public static void add(Anim.Builder b) {
        add(b.build());
    }

    public static Anim get(String id) {
        return MAP.get(id);
    }

    public static boolean has(String id) {
        return MAP.containsKey(id);
    }

    /** Движение навыка, а если его нет — обычный замах (мод не падает из-за забытой строки). */
    public static Anim orSwing(String id) {
        Anim a = MAP.get(id);
        return a != null ? a : MAP.get("common.swing");
    }

    public static Collection<Anim> all() {
        return Collections.unmodifiableCollection(MAP.values());
    }

    public static Map<String, Anim> map() {
        return Collections.unmodifiableMap(MAP);
    }
}
