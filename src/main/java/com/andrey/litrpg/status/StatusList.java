package com.andrey.litrpg.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/** Метки на одной сущности. Без Minecraft — проверяется тестами. */
public final class StatusList {
    private final List<Status> list = new ArrayList<>();
    private boolean dirty;

    /** Наложить: та же метка (вид + ключ + источник) обновляется, а не копится. */
    public void add(Status s) {
        list.removeIf(o -> o.type() == s.type() && o.key().equals(s.key()) && Objects.equals(o.source(), s.source()));
        list.add(s);
        dirty = true;
    }

    public void remove(Predicate<Status> p) {
        if (list.removeIf(p)) dirty = true;
    }

    public List<Status> all() {
        return Collections.unmodifiableList(list);
    }

    public boolean has(StatusType t, long now) {
        for (Status s : list) if (s.type() == t && !s.expired(now)) return true;
        return false;
    }

    /** Произведение множителей вида (для DAMAGE_TAKEN, DAMAGE_DEALT, HEAL_TAKEN). Нет меток — 1. */
    public double product(StatusType t, long now) {
        double m = 1;
        for (Status s : list) if (s.type() == t && !s.expired(now)) m *= s.magnitude();
        return m;
    }

    /** Сумма сил вида (для SLOW, SPEED, REFLECT, LEECH). */
    public double sum(StatusType t, long now) {
        double m = 0;
        for (Status s : list) if (s.type() == t && !s.expired(now)) m += s.magnitude();
        return m;
    }

    /** Щиты впитывают урон по очереди (старые первыми). Возвращает урон, который прошёл сквозь. */
    public double absorb(double amount, long now) {
        for (int i = 0; i < list.size() && amount > 0; i++) {
            Status s = list.get(i);
            if (s.type() != StatusType.SHIELD || s.expired(now)) continue;
            double take = Math.min(amount, s.magnitude());
            amount -= take;
            list.set(i, s.withMagnitude(s.magnitude() - take));
            dirty = true;
        }
        list.removeIf(s -> s.type() == StatusType.SHIELD && s.magnitude() <= 1e-6);
        return amount;
    }

    public double shieldTotal(long now) {
        return sum(StatusType.SHIELD, now);
    }

    /** Убрать истёкшие; вернуть, изменилось ли что-то с прошлого раза. */
    public boolean expire(long now) {
        if (list.removeIf(s -> s.expired(now))) dirty = true;
        boolean d = dirty;
        dirty = false;
        return d;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }

    public void clear() {
        if (!list.isEmpty()) dirty = true;
        list.clear();
    }
}
