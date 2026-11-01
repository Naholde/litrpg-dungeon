package com.andrey.litrpg.status;

import java.util.UUID;

/**
 * Одна метка со сроком.
 *
 * @param key       ключ (обычно id навыка): повторное наложение с тем же ключом и источником обновляет метку
 * @param source    кто наложил (для угрозы, лечения и «защитника»); может быть null
 * @param magnitude сила — смысл зависит от вида ({@link StatusType})
 * @param until     игровой тик, когда метка спадёт
 * @param interval  для урона/лечения со временем — раз во сколько тиков срабатывает (иначе 0)
 * @param icon      ключ значка для HUD (id навыка или вида)
 */
public record Status(StatusType type, String key, UUID source, double magnitude, long until, int interval, String icon) {
    public Status withMagnitude(double m) {
        return new Status(type, key, source, m, until, interval, icon);
    }

    public boolean expired(long now) {
        return now >= until;
    }

    public static Status of(StatusType type, String key, UUID source, double magnitude, long now, int durationTicks) {
        return new Status(type, key, source, magnitude, now + durationTicks, 0, key);
    }

    public static Status periodic(StatusType type, String key, UUID source, double perTick, long now, int durationTicks,
                                  int interval) {
        return new Status(type, key, source, perTick, now + durationTicks, interval, key);
    }
}
