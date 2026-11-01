package com.andrey.litrpg.skill;

/**
 * Свой ресурс класса. Маны нет: у большинства классов навыки работают только на перезарядке;
 * ресурс есть лишь там, где он и есть идея класса.
 */
public enum Resource {
    NONE("", 0, 0),
    /** Ярость: копится от ударов (своих и по себе), тает вне боя. Начинается с нуля. */
    RAGE("Ярость", 100, 0xFFD8343A);

    public final String title;
    public final double max;
    public final int color;

    Resource(String title, double max, int color) {
        this.title = title;
        this.max = max;
        this.color = color;
    }
}
