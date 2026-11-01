package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.CharacterBalance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.RolesBalance;
import com.andrey.litrpg.core.balance.StatSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Единственный расчёт характеристик персонажа. Его используют и бой (сервер), и окно «Расчёт» (клиент),
 * поэтому цифры в окне всегда совпадают с настоящими. Каждое значение — сумма строк «источник: вклад».
 * Класс без Minecraft — проверяется обычными тестами.
 */
public final class StatBreakdown {
    /** Слагаемое: откуда взялось и сколько дало. */
    public record Line(String source, double value) {
    }

    /** Всё, от чего зависит расчёт. Экипировка и класс добавятся в следующих фазах. */
    public record Inputs(Role role, int level, int[] allocated, StatSet gear, StatSet classGrowth, String className,
                         Map<Derived, Double> gearDerived, double armor) {
        public Inputs {
            if (allocated.length != Stat.ALL.length) throw new IllegalArgumentException("allocated");
            gearDerived = gearDerived == null ? Map.of() : gearDerived;
        }

        public Inputs(Role role, int level, int[] allocated, StatSet gear) {
            this(role, level, allocated, gear, null, null, Map.of(), 0);
        }
    }

    private final Map<Stat, List<Line>> primaryLines = new EnumMap<>(Stat.class);
    private final Map<Stat, Double> primary = new EnumMap<>(Stat.class);
    private final Map<Derived, List<Line>> derivedLines = new EnumMap<>(Derived.class);
    private final Map<Derived, Double> derived = new EnumMap<>(Derived.class);

    private StatBreakdown() {
    }

    public double get(Stat s) {
        return primary.getOrDefault(s, 0.0);
    }

    public double get(Derived d) {
        return derived.getOrDefault(d, 0.0);
    }

    public List<Line> lines(Stat s) {
        return Collections.unmodifiableList(primaryLines.getOrDefault(s, List.of()));
    }

    public List<Line> lines(Derived d) {
        return Collections.unmodifiableList(derivedLines.getOrDefault(d, List.of()));
    }

    public static StatBreakdown compute(Inputs in, Progression prog, CharacterBalance cb, RolesBalance rb) {
        StatBreakdown b = new StatBreakdown();
        RolesBalance.RoleBalance role = rb.of(in.role());
        String roleName = "Роль «" + in.role().title + "»";
        int lv = Math.max(1, in.level());

        // ---- основные характеристики
        for (Stat s : Stat.ALL) {
            List<Line> l = new ArrayList<>();
            l.add(new Line("Основа", cb.baseStat()));
            boolean byClass = in.classGrowth() != null;
            double growth = (byClass ? in.classGrowth() : role.growth()).get(s) * (lv - 1);
            if (growth != 0) {
                l.add(new Line((byClass ? "Рост класса «" + in.className() + "»" : "Рост роли") + " за " + (lv - 1) + " ур.",
                        growth));
            }
            int pts = in.allocated()[s.ordinal()];
            if (pts != 0) l.add(new Line("Вложено: " + pts + " очк. × " + Derived.trim(prog.statPointValue()),
                    pts * prog.statPointValue()));
            double gear = in.gear().get(s);
            if (gear != 0) l.add(new Line("Экипировка", gear));
            b.putPrimary(s, l);
        }
        double str = b.get(Stat.STRENGTH), agi = b.get(Stat.AGILITY), vit = b.get(Stat.VITALITY);
        double intel = b.get(Stat.INTELLECT), luck = b.get(Stat.LUCK);

        // ---- здоровье
        List<Line> hp = new ArrayList<>();
        hp.add(new Line("Основа", cb.health().base()));
        if (lv > 1) hp.add(new Line("Уровень " + lv, cb.health().perLevel() * (lv - 1)));
        double vitHp = vit * cb.health().perVitality();
        if (role.vitalityHealthMult() != 1) {
            hp.add(new Line("Выносливость " + Derived.trim(vit) + " × " + Derived.trim(cb.health().perVitality())
                    + " × " + Derived.trim(role.vitalityHealthMult()) + " (роль)", vitHp * role.vitalityHealthMult()));
        } else {
            hp.add(new Line("Выносливость " + Derived.trim(vit) + " × " + Derived.trim(cb.health().perVitality()), vitHp));
        }
        mult(hp, roleName, role.healthMult());
        b.putDerived(Derived.MAX_HEALTH, hp);

        // ---- урон
        b.putDerived(Derived.WEAPON_POWER, List.of(
                new Line("Сила " + Derived.trim(str) + " × " + pct(cb.power().weaponPerStrength()),
                        str * cb.power().weaponPerStrength())));
        b.putDerived(Derived.RANGED_POWER, List.of(
                new Line("Ловкость " + Derived.trim(agi) + " × " + pct(cb.power().rangedPerAgility()),
                        agi * cb.power().rangedPerAgility())));
        List<Line> sp = new ArrayList<>();
        sp.add(new Line("Интеллект " + Derived.trim(intel) + " × " + pct(cb.power().skillPerIntellect()),
                intel * cb.power().skillPerIntellect()));
        if (role.skillPowerBonus() != 0) sp.add(new Line(roleName, role.skillPowerBonus()));
        b.putDerived(Derived.SKILL_POWER, sp);
        List<Line> dm = new ArrayList<>();
        dm.add(new Line("Основа", 1));
        if (role.weaponDamageMult() != 1) dm.add(new Line(roleName, role.weaponDamageMult() - 1));
        b.putDerived(Derived.DAMAGE_MULT, dm);

        // ---- крит
        List<Line> cc = new ArrayList<>();
        cc.add(new Line("Основа", cb.crit().baseChance()));
        cc.add(new Line("Ловкость " + Derived.trim(agi), agi * cb.crit().chancePerAgility()));
        cc.add(new Line("Удача " + Derived.trim(luck), luck * cb.crit().chancePerLuck()));
        if (role.critBonus() != 0) cc.add(new Line(roleName, role.critBonus()));
        cap(cc, cb.crit().chanceCap());
        b.putDerived(Derived.CRIT_CHANCE, cc);
        b.putDerived(Derived.CRIT_DAMAGE, List.of(
                new Line("Основа", cb.crit().baseDamage()),
                new Line("Ловкость " + Derived.trim(agi), agi * cb.crit().damagePerAgility())));

        // ---- защита
        List<Line> dodge = new ArrayList<>();
        dodge.add(new Line("Ловкость " + Derived.trim(agi), agi * cb.defense().dodgePerAgility()));
        cap(dodge, cb.defense().dodgeCap());
        b.putDerived(Derived.DODGE, dodge);
        List<Line> tough = new ArrayList<>();
        if (role.toughnessBonus() != 0) tough.add(new Line(roleName, role.toughnessBonus()));
        tough.add(new Line("Выносливость " + Derived.trim(vit), vit * cb.defense().toughnessPerVitality()));
        cap(tough, cb.defense().toughnessCap());
        b.putDerived(Derived.TOUGHNESS, tough);

        // ---- поддержка и прочее
        List<Line> heal = new ArrayList<>();
        heal.add(new Line("Основа", 1));
        heal.add(new Line("Интеллект " + Derived.trim(intel), intel * cb.utility().healPerIntellect()));
        mult(heal, roleName, role.healMult());
        b.putDerived(Derived.HEAL_POWER, heal);
        List<Line> haste = new ArrayList<>();
        haste.add(new Line("Интеллект " + Derived.trim(intel), intel * cb.utility().hastePerIntellect()));
        cap(haste, cb.utility().hasteCap());
        b.putDerived(Derived.HASTE, haste);
        List<Line> speed = new ArrayList<>();
        speed.add(new Line("Ловкость " + Derived.trim(agi), agi * cb.utility().speedPerAgility()));
        cap(speed, cb.utility().speedCap());
        b.putDerived(Derived.MOVE_SPEED, speed);
        List<Line> threat = new ArrayList<>();
        threat.add(new Line("Основа", 1));
        if (role.threatMult() != 1) threat.add(new Line(roleName, role.threatMult() - 1));
        b.putDerived(Derived.THREAT, threat);
        List<Line> boss = new ArrayList<>();
        boss.add(new Line("Основа", 1));
        if (role.bossDamageTakenMult() != 1) boss.add(new Line(roleName, role.bossDamageTakenMult() - 1));
        b.putDerived(Derived.BOSS_TAKEN, boss);
        b.putDerived(Derived.FIND, List.of(new Line("Удача " + Derived.trim(luck), luck * cb.utility().findPerLuck())));
        b.putDerived(Derived.ARMOR, in.armor() > 0 ? List.of(new Line("Броня экипировки", in.armor())) : List.of());
        // Свойства вещей — отдельной строкой в каждой величине (с учётом пределов).
        for (var e : in.gearDerived().entrySet()) {
            List<Line> l = new ArrayList<>(b.lines(e.getKey()));
            l.removeIf(x -> x.source().startsWith("Предел"));
            l.add(new Line("Свойства вещей", e.getValue()));
            double cap = capOf(e.getKey(), cb);
            if (cap > 0) cap(l, cap);
            b.putDerived(e.getKey(), l);
        }
        return b;
    }

    private static double capOf(Derived d, CharacterBalance cb) {
        return switch (d) {
            case CRIT_CHANCE -> cb.crit().chanceCap();
            case DODGE -> cb.defense().dodgeCap();
            case TOUGHNESS -> cb.defense().toughnessCap();
            case HASTE -> cb.utility().hasteCap();
            case MOVE_SPEED -> cb.utility().speedCap();
            default -> 0;
        };
    }

    /** Множитель показываем отдельной строкой — приростом к тому, что набралось выше. */
    private static void mult(List<Line> lines, String source, double factor) {
        if (factor == 1) return;
        double sum = lines.stream().mapToDouble(Line::value).sum();
        lines.add(new Line(source + ": ×" + Derived.trim(factor), sum * (factor - 1)));
    }

    private static void cap(List<Line> lines, double cap) {
        double sum = lines.stream().mapToDouble(Line::value).sum();
        if (sum > cap) lines.add(new Line("Предел " + pct(cap), cap - sum));
    }

    private static String pct(double v) {
        return Derived.trim(v * 100) + "%";
    }

    private void putPrimary(Stat s, List<Line> lines) {
        primaryLines.put(s, lines);
        primary.put(s, lines.stream().mapToDouble(Line::value).sum());
    }

    private void putDerived(Derived d, List<Line> lines) {
        derivedLines.put(d, lines);
        derived.put(d, lines.stream().mapToDouble(Line::value).sum());
    }
}
