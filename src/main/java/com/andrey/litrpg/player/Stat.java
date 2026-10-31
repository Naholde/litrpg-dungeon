package com.andrey.litrpg.player;

/** Пять основных характеристик. Порядок важен: по нему хранятся вложенные очки. */
public enum Stat {
    STRENGTH("strength", "Сила", "СИЛ", "урон оружием"),
    AGILITY("agility", "Ловкость", "ЛОВ", "крит, уклонение, скорость"),
    VITALITY("vitality", "Выносливость", "ВЫН", "здоровье и стойкость"),
    INTELLECT("intellect", "Интеллект", "ИНТ", "сила навыков, лечение, откаты"),
    LUCK("luck", "Удача", "УДЧ", "крит и добыча");

    public final String id;
    public final String title;
    public final String shortTitle;
    public final String hint;

    Stat(String id, String title, String shortTitle, String hint) {
        this.id = id;
        this.title = title;
        this.shortTitle = shortTitle;
        this.hint = hint;
    }

    public static final Stat[] ALL = values();
}
