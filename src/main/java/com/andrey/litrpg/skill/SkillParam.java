package com.andrey.litrpg.skill;

/**
 * Параметр навыка. Объявляется в коде навыка один раз; значение можно переопределить в {@code skills.json}.
 *
 * @param key   имя в json и в шаблоне описания ({@code {key}})
 * @param def   значение по умолчанию
 * @param unit  как показывать в описании
 * @param scale как растёт с рангом навыка
 */
public record SkillParam(String key, double def, Unit unit, Scale scale) {
    public enum Unit {
        /** Доля «единицы силы» (урон/лечение/щит) — в описании показывается готовым числом для этого игрока. */
        DAMAGE, HEAL, SHIELD,
        SECONDS, PERCENT, BLOCKS, COUNT, MULT, FLAT
    }

    public enum Scale {
        /** Не меняется с рангом. */
        NONE,
        /** +N% за ранг (урон, лечение, щиты, сила меток). */
        POWER,
        /** −N% за ранг (перезарядка). */
        COOLDOWN,
        /** +N% за ранг (длительность меток, зон, оглушений). Время чтения не растёт. */
        DURATION
    }
}
