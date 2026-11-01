package com.andrey.litrpg.combat.threat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Таблица угрозы одного врага (как в MMO). Без Minecraft — проверяется тестами.
 * <ul>
 *   <li>урон и лечение добавляют угрозу;</li>
 *   <li>сменить цель можно, только перебив угрозу текущей цели на 10% вблизи или на 30% издалека;</li>
 *   <li>провокация сразу делает провокатора первым и «приклеивает» врага на несколько секунд;</li>
 *   <li>угроза «свежая» — понемногу тает.</li>
 * </ul>
 */
public final class ThreatTable {
    private final Map<UUID, Double> threat = new LinkedHashMap<>();
    private UUID current;
    private UUID taunter;
    private long tauntUntil;

    public void add(UUID who, double amount) {
        if (amount <= 0) return;
        threat.merge(who, amount, Double::sum);
    }

    public double get(UUID who) {
        return threat.getOrDefault(who, 0.0);
    }

    public boolean isEmpty() {
        return threat.isEmpty();
    }

    public boolean contains(UUID who) {
        return threat.containsKey(who);
    }

    public Map<UUID, Double> view() {
        return Collections.unmodifiableMap(threat);
    }

    public UUID current() {
        return current;
    }

    public void remove(UUID who) {
        threat.remove(who);
        if (who.equals(current)) current = null;
        if (who.equals(taunter)) taunter = null;
    }

    public void clear() {
        threat.clear();
        current = null;
        taunter = null;
    }

    /** Провокация: провокатор становится первым (его угроза = максимум + 10%) и держит врага до {@code until}. */
    public void taunt(UUID who, long now, long until) {
        double top = threat.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        threat.put(who, Math.max(get(who), top * 1.1 + 1));
        taunter = who;
        tauntUntil = until;
        current = who;
    }

    public boolean taunted(long now) {
        return taunter != null && now < tauntUntil;
    }

    /** Угроза тает: каждому умножить на (1 − доля). Совсем малая — вылетает. */
    public void decay(double fraction) {
        threat.replaceAll((k, v) -> v * (1 - fraction));
        threat.values().removeIf(v -> v < 0.01);
        if (current != null && !threat.containsKey(current)) current = null;
    }

    /**
     * Выбрать цель.
     *
     * @param now     текущее время (тики)
     * @param inMelee стоит ли кандидат вблизи врага
     * @param switchMelee  порог смены цели вблизи (1,1 = на 10% больше)
     * @param switchRanged порог издалека
     */
    public UUID choose(long now, Predicate<UUID> inMelee, double switchMelee, double switchRanged) {
        if (threat.isEmpty()) return current = null;
        if (taunted(now) && threat.containsKey(taunter)) return current = taunter;
        UUID top = null;
        double best = -1;
        for (var e : threat.entrySet()) {
            if (e.getValue() > best) {
                best = e.getValue();
                top = e.getKey();
            }
        }
        if (current == null || !threat.containsKey(current) || top.equals(current)) return current = top;
        double need = get(current) * (inMelee.test(top) ? switchMelee : switchRanged);
        if (best > need) current = top;
        return current;
    }
}
