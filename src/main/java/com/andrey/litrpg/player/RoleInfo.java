package com.andrey.litrpg.player;

import com.andrey.litrpg.core.balance.RolesBalance;

import java.util.ArrayList;
import java.util.List;

/**
 * Описание роли строится из тех же цифр, что использует расчёт: поменяли roles.json — поменялось описание.
 * Сравнение — с «нейтральной» ролью none.
 */
public final class RoleInfo {
    private RoleInfo() {
    }

    public record Line(String text, boolean good) {
    }

    public static List<Line> describe(Role role, RolesBalance rb) {
        RolesBalance.RoleBalance r = rb.of(role), n = rb.of(Role.NONE);
        List<Line> out = new ArrayList<>();
        mult(out, "Здоровье", r.healthMult(), n.healthMult(), true);
        if (r.vitalityHealthMult() != n.vitalityHealthMult()) {
            out.add(new Line("Здоровье от ВЫН ×" + Derived.trim2(r.vitalityHealthMult()),
                    r.vitalityHealthMult() > n.vitalityHealthMult()));
        }
        mult(out, "Угроза", r.threatMult(), n.threatMult(), true);
        add(out, "Стойкость", r.toughnessBonus() - n.toughnessBonus());
        mult(out, "Урон от хранителей", r.bossDamageTakenMult(), n.bossDamageTakenMult(), false);
        mult(out, "Лечение", r.healMult(), n.healMult(), true);
        add(out, "Сила навыков", r.skillPowerBonus() - n.skillPowerBonus());
        add(out, "Шанс крита", r.critBonus() - n.critBonus());
        mult(out, "Свой урон", r.weaponDamageMult(), n.weaponDamageMult(), true);
        return out;
    }

    private static void mult(List<Line> out, String what, double v, double base, boolean higherIsGood) {
        if (v == base) return;
        out.add(new Line(what + " ×" + Derived.trim2(v), (v > base) == higherIsGood));
    }

    private static void add(List<Line> out, String what, double delta) {
        if (delta == 0) return;
        out.add(new Line(what + " " + (delta > 0 ? "+" : "−") + Derived.trim(Math.abs(delta) * 100) + "%", delta > 0));
    }
}
