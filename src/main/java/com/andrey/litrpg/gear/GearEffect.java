package com.andrey.litrpg.gear;

import com.andrey.litrpg.player.Role;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.List;

/**
 * Особые свойства вещей (фаза 13): не «+3% к чему-то», а правило, которое меняет бой — лечит от урона,
 * отвечает шипами, спасает на краю гибели, усиливает окно «Изнурён». Бывают у эпических (с шансом),
 * легендарных (одно) и божественных (два) вещей; у каждого трофея хранителя — своё.
 * Сила — доля (0,2 = 20%), диапазон — в gear.json → effects; текст подсказки собирается из неё.
 */
public enum GearEffect implements StringRepresentable {
    // танк
    BULWARK("bulwark", "Оплот", Role.TANK, "−{v} урона, пока враг бьёт именно вас"),
    THORNS("thorns", "Шипы", Role.TANK, "{v} урона в ближнем бою возвращается врагу"),
    SECOND_WIND("second_wind", "Второе дыхание", Role.TANK, "Ниже 30% здоровья — щит в {v} здоровья (раз в 60 с)"),
    WARDEN("warden", "Опека", Role.TANK, "Союзники в 6 блоках получают на {v} меньше урона"),
    // боец
    EXECUTE("execute", "Добивание", Role.DAMAGE, "+{v} урона по врагам ниже 30% здоровья"),
    FIRST_STRIKE("first_strike", "Первый удар", Role.DAMAGE, "+{v} урона по врагам с полным здоровьем"),
    VAMPIRIC("vampiric", "Кровопийца", Role.DAMAGE, "{v} нанесённого урона лечит вас"),
    SPARK("spark", "Искра", Role.DAMAGE, "Удар с шансом 12% бьёт молнией ещё двоих рядом на {v} урона"),
    OPENING("opening", "Окно", Role.DAMAGE, "+{v} урона по хранителю, пока он готовит приём (и в его передышку)"),
    DASH_FURY("dash_fury", "Рывок ярости", Role.DAMAGE, "После рывка +{v} урона на 3 с"),
    // поддержка
    MERCY("mercy", "Милосердие", Role.SUPPORT, "+{v} лечения союзникам ниже половины здоровья"),
    OVERFLOW("overflow", "Избыток", Role.SUPPORT, "{v} перелечения становится щитом"),
    ECHO("echo", "Отклик", Role.SUPPORT, "Лечение союзников лечит и вас на {v}"),
    // любой
    TAILWIND("tailwind", "Попутный ветер", Role.NONE, "После победы над врагом +{v} к скорости на 4 с");

    public static final Codec<GearEffect> CODEC = StringRepresentable.fromEnum(GearEffect::values);

    public final String id;
    public final String title;
    /** Для какой роли свойство выпадает (NONE — для любой). */
    public final Role role;
    private final String text;

    GearEffect(String id, String title, Role role, String text) {
        this.id = id;
        this.title = title;
        this.role = role;
        this.text = text;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Подсказка с силой: «+20% урона по врагам ниже 30% здоровья». */
    public String describe(double value) {
        return text.replace("{v}", Math.round(value * 100) + "%");
    }

    /** Свойства, которые выпадают игроку этой роли: своей роли и общие. */
    public static List<GearEffect> forRole(Role role) {
        return Arrays.stream(values()).filter(e -> e.role == Role.NONE || role == Role.NONE || e.role == role).toList();
    }
}
