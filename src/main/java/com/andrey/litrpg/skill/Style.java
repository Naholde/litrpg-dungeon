package com.andrey.litrpg.skill;

/**
 * Стили игры. До получения класса Система считает, что игрок делает (удары вблизи, выстрелы, лечение…),
 * и на 10 уровне предлагает классы, чей стиль ближе.
 */
public enum Style {
    MELEE("ближний бой"),
    RANGED("стрельба"),
    MAGIC("магия посохом"),
    HEALING("лечение"),
    PROTECTION("защита союзников"),
    TANKING("принимать удары"),
    SKILLS("частые навыки");

    public final String title;

    Style(String title) {
        this.title = title;
    }
}
