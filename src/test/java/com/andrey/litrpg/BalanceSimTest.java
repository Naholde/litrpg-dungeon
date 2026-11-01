package com.andrey.litrpg;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.GearBalance;
import com.andrey.litrpg.core.balance.Progression;
import com.andrey.litrpg.core.balance.StatSet;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.Role;
import com.andrey.litrpg.player.Stat;
import com.andrey.litrpg.player.StatBreakdown;
import com.andrey.litrpg.run.Layout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Симуляция баланса по настоящим формулам игры: отряд из 3/4/5 (танк, бойцы, поддержка) с ожидаемыми на этом этапе
 * уровнем, вещами и вложенными очками против каждого отряда монстров и хранителя схемы. Считает: сколько длится бой,
 * сколько урона в секунду приходит на танка и на отряд (с неистовством и долей ошибок в разметке), успевает ли лекарь,
 * переживёт ли танк сильнейший удар. Отчёт — build/reports/balance.md (с -Pregen копируется в docs/BALANCE.md).
 *
 * <p>Цель «тяжело, но не критично»: хранитель 2,5–6 мин; танк держит сильнейший удар с запасом; лекарь закрывает
 * входящий урон, но занят (нагрузка 45–95%); отряд монстров — 15–50 с.
 */
class BalanceSimTest {
    /** Доля «ошибок» в разметке: каждый не-танк ловит такую долю ударов по площади. */
    private static final double MISTAKES = 0.2;
    /** Время в бою, которое уходит на атаку (остальное — беготня от разметки). */
    private static final double UPTIME = 0.75;
    private static final Set<String> BUSTERS = Set.of("chain", "reap", "scythe", "bite", "slam", "trident", "hammer", "sword",
            "strike", "smash", "staff", "slash");

    private record Member(Role role, StatBreakdown b, int level) {
    }

    /** Редкость вещей, которая ожидается к этому уровню (по добыче и торговцу). */
    private static double gearPower(int level) {
        if (level < 10) return 1.04;
        if (level < 20) return 1.12;
        if (level < 30) return 1.19;
        if (level < 40) return 1.25;
        return 1.31;
    }

    private static Member member(Role role, int level) {
        Progression prog = Balance.get(Balance.PROGRESSION);
        GearBalance g = Balance.get(Balance.GEAR);
        double power = gearPower(level);
        // 9 вещей: оружие, 4 брони (+щит у танка), 4 украшения — бюджет характеристик на каждую
        double statBudget = 9 * g.statPerLevel() * level * power;
        int[] alloc = new int[Stat.ALL.length];
        int points = (int) Math.round(prog.statPointsPerLevel() * (level - 1));
        StatSet gear;
        Map<Derived, Double> der = new EnumMap<>(Derived.class);
        double scale = Math.max(0.25, Math.min(1, level / 50.0));
        double affix = 2.2 * scale; // ~2,2 свойства на вещь × 9 вещей / разные виды → столько «средних» на нужное
        switch (role) {
            case TANK -> {
                alloc[Stat.VITALITY.ordinal()] = points * 7 / 10;
                alloc[Stat.STRENGTH.ordinal()] = points * 3 / 10;
                gear = new StatSet(statBudget * 0.35, 0, statBudget * 0.65, 0, 0);
                der.put(Derived.TOUGHNESS, affix * 0.02);
            }
            case SUPPORT -> {
                alloc[Stat.INTELLECT.ordinal()] = points * 7 / 10;
                alloc[Stat.VITALITY.ordinal()] = points * 3 / 10;
                gear = new StatSet(0, 0, statBudget * 0.3, statBudget * 0.7, 0);
                der.put(Derived.HEAL_POWER, affix * 0.055);
                der.put(Derived.HASTE, affix * 0.025);
            }
            default -> {
                alloc[Stat.STRENGTH.ordinal()] = points * 45 / 100;
                alloc[Stat.AGILITY.ordinal()] = points * 30 / 100;
                alloc[Stat.VITALITY.ordinal()] = points * 25 / 100;
                gear = new StatSet(statBudget * 0.55, statBudget * 0.35, statBudget * 0.1, 0, 0);
                der.put(Derived.WEAPON_POWER, affix * 0.055);
                der.put(Derived.CRIT_CHANCE, affix * 0.025);
                der.put(Derived.CRIT_DAMAGE, affix * 0.1);
            }
        }
        var a = g.armor();
        double armor = a.perLevel() * level * power * (a.helmet() + a.chest() + a.legs() + a.boots() + (role == Role.TANK ? a.shield() : 0));
        var in = new StatBreakdown.Inputs(role, level, alloc, gear, null, null, der, armor);
        return new Member(role, StatBreakdown.compute(in, prog, Balance.get(Balance.CHARACTER), Balance.get(Balance.ROLES)), level);
    }

    private static double skillPower(int level) {
        var c = Balance.get(Balance.SKILLS).common();
        return c.powerBase() + c.powerPerLevel() * level;
    }

    /** Урон в секунду: оружие + навыки (у класса после 10 уровня навыков больше). */
    private static double dps(Member m) {
        StatBreakdown b = m.b();
        GearBalance.Weapons w = Balance.get(Balance.GEAR).weapons();
        double weapon = (w.meleeBase() + w.meleePerLevel() * (m.level() - 1)) * gearPower(m.level());
        double crit = 1 + b.get(Derived.CRIT_CHANCE) * (b.get(Derived.CRIT_DAMAGE) - 1);
        double mult = b.get(Derived.DAMAGE_MULT);
        double haste = 1 + b.get(Derived.HASTE);
        double auto = weapon * (1 + b.get(Derived.WEAPON_POWER)) * mult * crit * 1.6;
        // навыки: доля «единицы силы» в секунду
        double skillCoef = switch (m.role()) {
            case DAMAGE -> 0.68;
            case TANK -> 0.12;
            default -> 0.4;
        };
        if (m.level() >= 10) skillCoef *= 1.35;
        double skills = skillCoef * haste * skillPower(m.level()) * (1 + b.get(Derived.SKILL_POWER)) * mult * crit;
        double share = m.role() == Role.SUPPORT ? 0.3 : 1.0; // лекарь бьёт треть времени
        return (auto + skills) * UPTIME * share;
    }

    /** Лечение в секунду, если лекарь только лечит. */
    private static double hps(Member m) {
        double coef = 2.5 / 1.5 + 3.0 / 6.0;
        if (m.level() >= 10) coef *= 1.25;
        return coef * (1 + m.b().get(Derived.HASTE)) * skillPower(m.level()) * m.b().get(Derived.HEAL_POWER);
    }

    // ------------------------------------------------------------------ стенд классов (gradlew runGameTestServer -Pbench)

    /** Медиана роли со стенда в «единицах»: уровень → [урон по цели, лечение+щиты]. Пусто — стенд не запускали. */
    private static final Map<Role, java.util.TreeMap<Integer, double[]>> BENCH = new EnumMap<>(Role.class);

    static {
        Path dir = Path.of(System.getProperty("litrpg.root", "."), "build/reports/bench");
        Map<String, List<double[]>> raw = new java.util.HashMap<>();
        if (Files.isDirectory(dir)) {
            try (var files = Files.list(dir)) {
                for (Path f : files.filter(f -> !f.getFileName().toString().startsWith("boss_")).toList()) {
                    Map<String, String> d = new java.util.HashMap<>();
                    for (String l : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                        int i = l.indexOf('=');
                        if (i > 0) d.put(l.substring(0, i), l.substring(i + 1));
                    }
                    double u = Double.parseDouble(d.get("unit"));
                    raw.computeIfAbsent(d.get("role") + ":" + d.get("level"), k -> new ArrayList<>()).add(new double[]{
                            Double.parseDouble(d.get("dps_one")) / u,
                            (Double.parseDouble(d.get("hps")) + Double.parseDouble(d.get("sps"))) / u});
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for (var e : raw.entrySet()) {
            String[] k = e.getKey().split(":");
            List<double[]> l = e.getValue();
            double[] med = new double[2];
            for (int j = 0; j < 2; j++) {
                final int jj = j;
                double[] v = l.stream().mapToDouble(a -> a[jj]).sorted().toArray();
                med[j] = v[v.length / 2];
            }
            BENCH.computeIfAbsent(Role.valueOf(k[0]), r -> new java.util.TreeMap<>()).put(Integer.parseInt(k[1]), med);
        }
    }

    private static boolean bench() {
        return BENCH.containsKey(Role.TANK) && BENCH.containsKey(Role.DAMAGE) && BENCH.containsKey(Role.SUPPORT);
    }

    /** Значение стенда для роли на уровне (между замерами — по прямой). */
    private static double benchAt(Role r, int level, int what) {
        var m = BENCH.get(r);
        var lo = m.floorEntry(level);
        var hi = m.ceilingEntry(level);
        if (lo == null) return hi.getValue()[what];
        if (hi == null || hi.getKey().equals(lo.getKey())) return lo.getValue()[what];
        double t = (level - lo.getKey()) / (double) (hi.getKey() - lo.getKey());
        return lo.getValue()[what] + (hi.getValue()[what] - lo.getValue()[what]) * t;
    }

    /** Голый персонаж (как на стенде): без вещей и вложенных очков. */
    private static Member naked(Role role, int level) {
        var in = new StatBreakdown.Inputs(role, level, new int[Stat.ALL.length], StatSet.ZERO);
        return new Member(role, StatBreakdown.compute(in, Balance.get(Balance.PROGRESSION), Balance.get(Balance.CHARACTER),
                Balance.get(Balance.ROLES)), level);
    }

    /** Урон в секунду в бою: стенд × (вещи и очки) × время в атаке; без стенда — прежняя оценка. */
    private static double fightDps(Member m) {
        if (!bench()) return dps(m);
        double gear = dps(m) / dps(naked(m.role(), m.level()));
        double share = m.role() == Role.SUPPORT ? 0.5 : 1.0; // лекарь бьёт, когда лечить некого
        return benchAt(m.role(), m.level(), 0) * BossDamage.unit(m.level()) * gear * UPTIME * share;
    }

    /** Лечение в секунду: стенд × сила лечения от вещей; без стенда — прежняя оценка. */
    private static double fightHps(Member m) {
        if (!bench()) return hps(m);
        double gear = hps(m) / hps(naked(m.role(), m.level()));
        return benchAt(Role.SUPPORT, m.level(), 1) * BossDamage.unit(m.level()) * gear * 0.85; // 15% — бег от разметки
    }

    private static List<Member> party(int n, int level) {
        List<Member> out = new ArrayList<>();
        out.add(member(Role.TANK, level));
        out.add(member(Role.SUPPORT, level));
        for (int i = 2; i < n; i++) out.add(member(Role.DAMAGE, level));
        return out;
    }

    private static double armorCut(Member m) {
        return Balance.get(Balance.GEAR).armor().reduction(m.b().get(Derived.ARMOR), m.level());
    }

    /** Насколько урон хранителя по этому игроку меньше «единицы»: роль, стойкость, броня. */
    private static double bossTaken(Member m) {
        return m.b().get(Derived.BOSS_TAKEN) * (1 - m.b().get(Derived.TOUGHNESS)) * (1 - armorCut(m));
    }

    private record BossRow(String name, int level, int n, double ttk, int enrage, double tankLoad, double healLoad,
                           double busterTank, double bigHitOthers) {
    }

    private static BossRow boss(BossRegistry.Type t, int n) {
        var p = t.params();
        int level = (int) Math.round(p.get("level"));
        int partyLevel = Math.max(1, level - 1);
        List<Member> party = party(n, partyLevel);
        double unit = BossDamage.unit(level);
        var common = Balance.get(Balance.BOSSES).common();
        var pr = common.pressure();
        double hp = p.get("hp_units") * unit * common.hpScale(n);
        double dps = party.stream().mapToDouble(BalanceSimTest::fightDps).sum();
        double ttk = hp / dps;
        int enrage = (int) (ttk / common.enrageSeconds());
        // средний множитель неистовства за бой (ровная прибавка за каждую ступень)
        double avgEnrage = 0;
        for (int s = 0; s <= enrage; s++) {
            double len = Math.min(common.enrageSeconds(), ttk - s * common.enrageSeconds());
            avgEnrage += (1 + (common.enrageMult() - 1) * s) * len;
        }
        avgEnrage /= ttk;
        double endEnrage = 1 + (common.enrageMult() - 1) * enrage;
        Member tank = party.get(0), healer = party.get(1);
        double buster = 0, busterCd = 1, areaPerSec = 0, bigHit = 0;
        for (var e : p.entrySet()) {
            if (!e.getKey().endsWith("_units") || e.getKey().equals("hp_units") || e.getKey().equals("melee_units")
                    || e.getKey().contains("hp_units")) continue;
            String k = e.getKey().replace("_units", "");
            Double cd = p.get(k + "_cd");
            double u = e.getValue();
            if (BUSTERS.contains(k) && cd != null) {
                if (u > buster) {
                    buster = u;
                    busterCd = cd;
                }
            } else {
                // по площади: ошибаются MISTAKES, длинные лужи/волны считаем раз в 20 с
                areaPerSec += u / (cd != null ? cd : 20) * MISTAKES;
                bigHit = Math.max(bigHit, u);
            }
        }
        double tankDtps = (p.get("melee_units") / common.autoAttackSeconds() + buster / busterCd) * unit * bossTaken(tank) * avgEnrage;
        // Давление: волна по всем, метки на бойцах (на 5+ — по две), ошибки в разметке.
        double grow = com.andrey.litrpg.boss.BossEntity.pressureGrow(level, n);
        double pulse = 0;
        for (Member m : party) pulse += pr.pulseUnits() * grow / pr.pulseSeconds() * unit * bossTaken(m);
        Member dd = party.get(Math.min(2, n - 1));
        double marks = (n >= 5 ? 2 : 1) * pr.markUnits() * grow * pr.markDuration() / pr.markSeconds() * unit * bossTaken(dd);
        double others = areaPerSec * unit * (n - 1) * bossTaken(dd);
        double incoming = tankDtps + (pulse + marks + others) * avgEnrage;
        double healLoad = incoming / fightHps(healer);
        double busterTank = buster * unit * bossTaken(tank) * endEnrage / tank.b().get(Derived.MAX_HEALTH);
        double bigOthers = bigHit * unit * bossTaken(dd) * endEnrage / dd.b().get(Derived.MAX_HEALTH);
        return new BossRow(t.name(), level, n, ttk, enrage, tankDtps / tank.b().get(Derived.MAX_HEALTH), healLoad, busterTank, bigOthers);
    }

    private record PackRow(String id, int n, double ttk, double tankLoad) {
    }

    private static PackRow pack(Layout.Pack pack, int n) {
        int level = pack.mobs().stream().mapToInt(Layout.Mob::level).max().orElse(1);
        List<Member> party = party(n, level);
        double hp = 0, dmg = 0;
        var mb = Balance.get(Balance.MOBS);
        for (Layout.Mob m : pack.mobs()) {
            if (m.minParty() > n) continue;
            Bestiary.Species s = Bestiary.get(m.species());
            MobRank r = pack.title().isPresent() && m == pack.mobs().get(0) ? MobRank.CHAMPION
                    : m.rank().equals("elite") ? MobRank.ELITE : MobRank.NORMAL;
            double unit = BossDamage.unit(m.level());
            hp += mb.value(s.id(), "hp_units", s.param("hp_units")) * unit * r.health
                    * com.andrey.litrpg.mob.DungeonMob.growHp(m.level());
            dmg += mb.value(s.id(), "dmg_units", s.param("dmg_units")) * unit * r.damage
                    * com.andrey.litrpg.mob.DungeonMob.growDamage(m.level());
        }
        double dps = party.stream().mapToDouble(BalanceSimTest::fightDps).sum() * 1.15; // часть урона — по площади
        Member tank = party.get(0);
        // монстры бьют примерно раз в секунду; по танку — с бронёй и стойкостью; урон тает по мере гибели отряда (×0,6)
        double tankDtps = dmg * 0.6 * (1 - armorCut(tank)) * (1 - tank.b().get(Derived.TOUGHNESS));
        return new PackRow(pack.id(), n, hp / dps, tankDtps / fightHps(party.get(1)));
    }

    @Test
    void stagesAreHardButNotCritical() throws IOException {
        StringBuilder r = new StringBuilder();
        r.append("# Баланс подземелья — симуляция\n\n");
        r.append("Считано по формулам игры (`BalanceSimTest`): отряд — танк, поддержка и бойцы; уровень — на 1 ниже хранителя;\n");
        r.append("вещи — ожидаемой к этапу редкости; очки вложены в главные характеристики роли. Бой: 75% времени в атаке,\n");
        r.append("каждый не-танк ловит 20% ударов по площади. Нагрузка лекаря — доля его лечения, которая уходит на входящий урон.\n");
        r.append(bench() ? "Урон и лечение ролей — медианы стенда классов (`gradlew runGameTestServer -Pbench`) × вещи и очки.\n\n"
                : "Стенд классов не запускали — урон и лечение оценены формулой (грубо). " + BENCH.keySet() + "\n\n");
        r.append("## Хранители\n\n| Хранитель | Ур. | Отряд | Бой | Неистовство | Танк теряет в с | Нагрузка лекаря | Сильнейший удар по танку | Сильнейший удар по бойцу |\n");
        r.append("|---|---|---|---|---|---|---|---|---|\n");
        List<String> problems = new ArrayList<>();
        List<String> suggest = new ArrayList<>();
        Layout layout = Layout.get();
        for (Layout.Stage st : layout.stages()) {
            for (Layout.Boss b : st.bosses()) {
                BossRegistry.Type t = BossRegistry.get(b.type());
                for (int n = 3; n <= 6; n++) {
                    BossRow row = boss(t, n);
                    // Сколько «единиц» здоровья нужно, чтобы втроём бой шёл 3:45.
                    if (n == 3) suggest.add(t.id() + "=" + Math.round(t.params().get("hp_units") * 225 / row.ttk()));
                    r.append(String.format(Locale.ROOT, "| %s | %d | %d | %d:%02d | ×%d | %.1f%% | %.0f%% | %.0f%% | %.0f%% |%n", row.name(),
                            row.level(), n, (int) row.ttk() / 60, (int) row.ttk() % 60, row.enrage(), row.tankLoad() * 100,
                            row.healLoad() * 100, row.busterTank() * 100, row.bigHitOthers() * 100));
                    if (!t.id().equals("morra") && (row.ttk() < 150 || row.ttk() > 360)) problems.add(t.name() + " (" + n + "): бой " + Math.round(row.ttk()) + " с");
                    if (row.busterTank() > 0.75) problems.add(t.name() + " (" + n + "): удар по танку " + Math.round(row.busterTank() * 100) + "% здоровья");
                    if (row.healLoad() > 1.05) problems.add(t.name() + " (" + n + "): лекарь не успевает (" + Math.round(row.healLoad() * 100) + "%)");
                    if (row.healLoad() < 0.5) problems.add(t.name() + " (" + n + "): лекарю скучно (" + Math.round(row.healLoad() * 100) + "%)");
                }
            }
        }
        r.append("\n## Отряды монстров (отряд из трёх)\n\n| Отряд | Бой, с | Нагрузка лекаря |\n|---|---|---|\n");
        for (Layout.Stage st : layout.stages()) {
            for (Layout.Pack p : st.packs()) {
                PackRow row = pack(p, 3);
                r.append(String.format(Locale.ROOT, "| %s%s | %.0f | %.0f%% |%n", p.id(), p.title().map(s -> " («" + s + "»)").orElse(""),
                        row.ttk(), row.tankLoad() * 100));
                if (row.ttk() > (p.title().isPresent() ? 100 : 60)) problems.add(p.id() + ": отряд бьётся " + Math.round(row.ttk()) + " с");
                if (row.tankLoad() > 1.1) problems.add(p.id() + ": лекарь не держит танка на отряде (" + Math.round(row.tankLoad() * 100) + "%)");
            }
        }
        r.append("\nЗдоровье хранителей для боя втроём 3:45 (hp_units): ").append(String.join(", ", suggest)).append("\n");
        r.append("\n## Замечания\n\n");
        if (problems.isEmpty()) r.append("Всё в пределах: хранители 2,5–6,5 мин, танк держит удары, лекарь занят, но успевает.\n");
        for (String p : problems) r.append("- ").append(p).append('\n');
        Path root = Path.of(System.getProperty("litrpg.root", "."));
        Path out = root.resolve("build/reports/balance.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, r.toString(), StandardCharsets.UTF_8);
        if (Boolean.getBoolean("litrpg.regen")) Files.writeString(root.resolve("docs/BALANCE.md"), r.toString(), StandardCharsets.UTF_8);
        // Строгая проверка — в фазе баланса (gradlew test -Pbalance); до того отчёт только пишется.
        if (Boolean.getBoolean("litrpg.balance")) {
            assertTrue(problems.isEmpty(), "баланс вне пределов:\n" + String.join("\n", problems));
        }
    }
}
