package com.andrey.litrpg.skill;

import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.player.Role;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.andrey.litrpg.skill.SkillRegistry.*;

/**
 * Все классы. Полный задуманный список (~10 на роль) — в {@code docs/CLASSES.md}; здесь — уже сделанные.
 * Навыки открываются: три — на уровне получения класса, остальные — по уровням до 48.
 */
public final class ClassRegistry {
    private static final Map<String, PlayerClass> ALL = new LinkedHashMap<>();

    public static final PlayerClass GUARDIAN = reg(new PlayerClass("guardian", Role.TANK, "Стражник", 0xFF7FB8FF, "⛨",
            "Держит место: забирает урон союзников на себя и гасит вражеские снаряды куполом.",
            "Стражники не гонятся за славой. Их дело — стоять там, где другим не выстоять, и чтобы за спиной было тихо.",
            Resource.NONE, new StatSet(1.2, 0.5, 2.2, 0.5, 0.5),
            List.of(u(10, TAUNT), u(10, COVER), u(10, CHARGE), u(20, BRACE), u(32, DOME), u(44, UNBREAKABLE)),
            Map.of(Style.TANKING, 1.0, Style.PROTECTION, 0.8, Style.MELEE, 0.6)));

    public static final PlayerClass BERSERKER = reg(new PlayerClass("berserker", Role.DAMAGE, "Берсерк", 0xFFD8343A, "⚔",
            "Чем больнее, тем злее: копит ярость от ударов и сжигает её в сокрушительные приёмы.",
            "Берсерк не считает раны — он считает врагов. Каждый пропущенный удар только подливает масла в огонь.",
            Resource.RAGE, new StatSet(1.8, 1.0, 1.0, 0.5, 0.6),
            List.of(u(10, CLEAVE), u(10, REND), u(10, LEAP), u(20, FRENZY), u(32, EXECUTE), u(44, BLOODBATH)),
            Map.of(Style.MELEE, 1.0, Style.SKILLS, 0.5, Style.TANKING, 0.4)));

    public static final PlayerClass CLERIC = reg(new PlayerClass("cleric", Role.SUPPORT, "Лекарь", 0xFFFFE27A, "✚",
            "Эталон лекаря: быстрая повязка по раненым, долгая молитва, святилище и «Чудо» для всей группы.",
            "Лекарь знает цену каждой секунде: одна повязка вовремя стоит десятка молитв запоздавших.",
            Resource.NONE, new StatSet(0.4, 0.7, 1.1, 2.2, 0.7),
            List.of(u(10, BANDAGE), u(10, PRAYER), u(10, RENEW), u(20, SANCTUARY), u(32, SMITE), u(44, MIRACLE)),
            Map.of(Style.HEALING, 1.0, Style.MAGIC, 0.5, Style.SKILLS, 0.3)));

    /** Навыки роли до получения класса. */
    public static final Map<Role, List<Skill>> ROLE_KITS = Map.of(
            Role.TANK, List.of(TAUNT, SHIELD_BASH, BULWARK),
            Role.DAMAGE, List.of(HEAVY_STRIKE, WHIRLWIND, THROWING_KNIFE),
            Role.SUPPORT, List.of(MEND, RENEW, SMITE),
            Role.NONE, List.of());

    private ClassRegistry() {
    }

    private static PlayerClass.Unlock u(int level, Skill s) {
        return new PlayerClass.Unlock(level, s);
    }

    private static PlayerClass reg(PlayerClass c) {
        if (ALL.putIfAbsent(c.id(), c) != null) throw new IllegalStateException("класс уже есть: " + c.id());
        return c;
    }

    public static PlayerClass get(String id) {
        return id == null ? null : ALL.get(id);
    }

    public static Collection<PlayerClass> all() {
        return Collections.unmodifiableCollection(ALL.values());
    }

    public static List<PlayerClass> ofRole(Role r) {
        return ALL.values().stream().filter(c -> c.role() == r).toList();
    }

    /** Навыки на панели: навыки класса (открытые) или, пока класса нет, навыки роли. Не больше 6. */
    public static List<Skill> loadout(Role role, String classId, int level) {
        PlayerClass c = get(classId);
        List<Skill> list = c != null ? c.unlocked(level) : ROLE_KITS.getOrDefault(role, List.of());
        return list.size() > 6 ? list.subList(0, 6) : list;
    }
}
