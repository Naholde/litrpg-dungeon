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
     * @param enrageMult      множитель урона за ступень
     * @param resetSeconds    сколько секунд без живых бойцов на арене до сброса боя
     * @param autoAttackSeconds как часто хранитель бьёт цель вблизи
     * @param hpScale         множитель здоровья для отряда из 1…6 человек (рассчитано на троих = 1)
     */
    public record Common(double enrageSeconds, double enrageMult, double resetSeconds, double autoAttackSeconds,
                         List<Double> hpScale) {
        static final Codec<Common> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("enrage_seconds").forGetter(Common::enrageSeconds),
                Codec.DOUBLE.fieldOf("enrage_mult").forGetter(Common::enrageMult),
                Codec.DOUBLE.fieldOf("reset_seconds").forGetter(Common::resetSeconds),
                Codec.DOUBLE.fieldOf("auto_attack_seconds").forGetter(Common::autoAttackSeconds),
                Codec.DOUBLE.listOf().validate(l -> l.size() == 6 ? DataResult.success(l)
                        : DataResult.error(() -> "hp_scale: нужно 6 чисел (отряд 1…6)")).fieldOf("hp_scale").forGetter(Common::hpScale)
        ).apply(i, Common::new));

        public double hpScale(int partySize) {
            return hpScale.get(Math.max(1, Math.min(6, partySize)) - 1);
        }
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

    public static final Common DEFAULT_COMMON = new Common(60, 1.3, 6, 2, List.of(0.4, 0.7, 1.0, 1.4, 1.8, 2.2));

    public static BossesBalance defaults() {
        return new BossesBalance(DEFAULT_COMMON, BossRegistry.defaultValues());
    }

    public double value(String boss, String key, double def) {
        Map<String, Double> m = bosses.get(boss);
        return m == null ? def : m.getOrDefault(key, def);
    }
}
