package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.boss.BossRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;

/**
 * Хранители: общие правила (неистовство, сброс боя, масштаб под отряд) и цифры каждого хранителя.
 * Файл {@code balance/bosses.json}, создаётся из кода: {@code gradlew test -Pregen}.
 */
public record BossesBalance(Common common, Map<String, Map<String, Double>> bosses) {

    /**
     * @param enrageSeconds   раз во сколько секунд боя хранитель впадает в неистовство (ступень)
     * @param enrageMult      урон за ступень: множитель 1 + (enrageMult − 1) × ступень
     * @param resetSeconds    сколько секунд без живых бойцов на арене до сброса боя
     * @param autoAttackSeconds как часто хранитель бьёт цель вблизи
     * @param hpScale         множитель здоровья для отряда из 1…6 человек (рассчитано на троих = 1)
     * @param pressure        общее давление всех хранителей: волна по отряду и метка на бойце
     */
    public record Common(double enrageSeconds, double enrageMult, double resetSeconds, double autoAttackSeconds,
                         List<Double> hpScale, Pressure pressure, Rush rush) {
        static final Codec<Common> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("enrage_seconds").forGetter(Common::enrageSeconds),
                Codec.DOUBLE.fieldOf("enrage_mult").forGetter(Common::enrageMult),
                Codec.DOUBLE.fieldOf("reset_seconds").forGetter(Common::resetSeconds),
                Codec.DOUBLE.fieldOf("auto_attack_seconds").forGetter(Common::autoAttackSeconds),
                Codec.DOUBLE.listOf().validate(l -> l.size() == 6 ? DataResult.success(l)
                        : DataResult.error(() -> "hp_scale: нужно 6 чисел (отряд 1…6)")).fieldOf("hp_scale").forGetter(Common::hpScale),
                Pressure.CODEC.fieldOf("pressure").forGetter(Common::pressure),
                Rush.CODEC.fieldOf("rush").forGetter(Common::rush)
        ).apply(i, Common::new));

        public double hpScale(int partySize) {
            return hpScale.get(Math.max(1, Math.min(6, partySize)) - 1);
        }
    }

    /**
     * Давление (фаза 12): у каждого хранителя, кроме его собственных навыков. Волна раз в {@code pulseSeconds} бьёт
     * весь отряд на {@code pulseUnits} (видна заранее — расходящееся кольцо), лекарю есть кого держать. Метка раз
     * в {@code markSeconds} ложится на бойца не-цель (на 5+ — на двоих): {@code markUnits} в секунду
     * {@code markDuration} секунд, а в конце — {@code markBurstUnits} всем, кто стоит ближе {@code markRadius}.
     */
    public record Pressure(double pulseSeconds, double pulseUnits, double markSeconds, double markUnits, double markDuration,
                           double markBurstUnits, double markRadius) {
        static final Codec<Pressure> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("pulse_seconds").forGetter(Pressure::pulseSeconds),
                Codec.DOUBLE.fieldOf("pulse_units").forGetter(Pressure::pulseUnits),
                Codec.DOUBLE.fieldOf("mark_seconds").forGetter(Pressure::markSeconds),
                Codec.DOUBLE.fieldOf("mark_units").forGetter(Pressure::markUnits),
                Codec.DOUBLE.fieldOf("mark_duration").forGetter(Pressure::markDuration),
                Codec.DOUBLE.fieldOf("mark_burst_units").forGetter(Pressure::markBurstUnits),
                Codec.DOUBLE.fieldOf("mark_radius").forGetter(Pressure::markRadius)
        ).apply(i, Pressure::new));
    }

    /**
     * Давка: между своими навыками хранитель не даёт стоять. Раз в {@code seconds} он бьёт одним из двух способов,
     * и разметка горит всего {@code warn} секунд — шагом из неё не выйти, нужен рывок или заранее занятое место.
     * <ul>
     *   <li>выпад: круги {@code radius} под бойцами (не под танком) на {@code units};</li>
     *   <li>круг у ног: {@code meleeRadius} вокруг самого хранителя на {@code meleeUnits} — чтобы ближний бой
     *       не стоял в куче, а выходил и возвращался.</li>
     * </ul>
     */
    public record Rush(double seconds, double warn, double radius, double units, double meleeRadius, double meleeUnits) {
        static final Codec<Rush> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("seconds").forGetter(Rush::seconds),
                Codec.DOUBLE.fieldOf("warn").forGetter(Rush::warn),
                Codec.DOUBLE.fieldOf("radius").forGetter(Rush::radius),
                Codec.DOUBLE.fieldOf("units").forGetter(Rush::units),
                Codec.DOUBLE.fieldOf("melee_radius").forGetter(Rush::meleeRadius),
                Codec.DOUBLE.fieldOf("melee_units").forGetter(Rush::meleeUnits)
        ).apply(i, Rush::new));
    }

    private static final Codec<Map<String, Map<String, Double>>> BOSSES =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)).validate(m -> {
                for (var e : m.entrySet()) {
                    var type = BossRegistry.get(e.getKey());
                    if (type == null) return DataResult.error(() -> "нет хранителя «" + e.getKey() + "»");
                    for (String k : e.getValue().keySet()) {
                        if (!type.params().containsKey(k)) return DataResult.error(() -> e.getKey() + ": нет параметра «" + k + "»");
                    }
                }
                return DataResult.success(m);
            });

    public static final MapCodec<BossesBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Common.CODEC.fieldOf("common").forGetter(BossesBalance::common),
            BOSSES.fieldOf("bosses").forGetter(BossesBalance::bosses)
    ).apply(i, BossesBalance::new));

    /**
     * Неистовство — ровная прибавка (+15% урона за минуту боя, а не ×1,3 от прежнего): к шестой минуте ×1,9,
     * давит, но не душит. Хранитель бьёт танка раз в 1,5 с.
     */
    public static final Common DEFAULT_COMMON = new Common(60, 1.15, 6, 1.5, List.of(0.4, 0.7, 1.0, 1.4, 1.8, 2.2),
            new Pressure(9, 0.3, 14, 0.08, 6, 0.5, 3),
            new Rush(3.2, 0.7, 3.2, 0.55, 4.5, 0.75));

    public static BossesBalance defaults() {
        return new BossesBalance(DEFAULT_COMMON, BossRegistry.defaultValues());
    }

    public double value(String boss, String key, double def) {
        Map<String, Double> m = bosses.get(boss);
        return m == null ? def : m.getOrDefault(key, def);
    }
}
