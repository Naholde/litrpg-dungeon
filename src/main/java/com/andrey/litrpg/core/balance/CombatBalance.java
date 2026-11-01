package com.andrey.litrpg.core.balance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Бой: урон по своим, окружение, угроза, манекен. Файл {@code balance/combat.json}. */
public record CombatBalance(double friendlyFireMult, boolean environmentScalesWithHealth, Threat threat,
                           double healThreatShare, Dummy dummy) {

    /**
     * @param tauntSeconds       сколько секунд провокация «приклеивает» врага
     * @param switchMelee        во сколько раз нужно перебить угрозу текущей цели вблизи
     * @param switchRanged       … издалека
     * @param meleeRange         что считается «вблизи», блоков
     * @param decayPerHalfSecond доля угрозы, которая тает каждые 0,5 с
     * @param forgetRange        дальше этого игрок вылетает из таблицы угрозы
     */
    public record Threat(double tauntSeconds, double switchMelee, double switchRanged, double meleeRange,
                         double decayPerHalfSecond, double forgetRange) {
        static final Codec<Threat> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("taunt_seconds").forGetter(Threat::tauntSeconds),
                Codec.DOUBLE.fieldOf("switch_melee").forGetter(Threat::switchMelee),
                Codec.DOUBLE.fieldOf("switch_ranged").forGetter(Threat::switchRanged),
                Codec.DOUBLE.fieldOf("melee_range").forGetter(Threat::meleeRange),
                Codec.DOUBLE.fieldOf("decay_per_half_second").forGetter(Threat::decayPerHalfSecond),
                Codec.DOUBLE.fieldOf("forget_range").forGetter(Threat::forgetRange)
        ).apply(i, Threat::new));
    }

    /**
     * Боевой манекен для проверки: держит удар, считает урон в секунду, «агрессивный» бьёт свою цель.
     *
     * @param resetSeconds       через сколько секунд без ударов манекен восстанавливается
     * @param hitShareOfMaxHealth сила удара агрессивного манекена — доля здоровья цели
     * @param hitIntervalSeconds как часто бьёт
     * @param hitRange           дальность удара
     */
    public record Dummy(double resetSeconds, double hitShareOfMaxHealth, double hitIntervalSeconds, double hitRange) {
        static final Codec<Dummy> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("reset_seconds").forGetter(Dummy::resetSeconds),
                Codec.DOUBLE.fieldOf("hit_share_of_max_health").forGetter(Dummy::hitShareOfMaxHealth),
                Codec.DOUBLE.fieldOf("hit_interval_seconds").forGetter(Dummy::hitIntervalSeconds),
                Codec.DOUBLE.fieldOf("hit_range").forGetter(Dummy::hitRange)
        ).apply(i, Dummy::new));
    }

    public static final MapCodec<CombatBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("friendly_fire_mult").forGetter(CombatBalance::friendlyFireMult),
            Codec.BOOL.fieldOf("environment_scales_with_health").forGetter(CombatBalance::environmentScalesWithHealth),
            Threat.CODEC.fieldOf("threat").forGetter(CombatBalance::threat),
            Codec.DOUBLE.fieldOf("heal_threat_share").forGetter(CombatBalance::healThreatShare),
            Dummy.CODEC.fieldOf("dummy").forGetter(CombatBalance::dummy)
    ).apply(i, CombatBalance::new));

    public static final CombatBalance DEFAULT = new CombatBalance(0.25, true,
            new Threat(4, 1.1, 1.3, 4, 0.015, 48), 0.5,
            new Dummy(4, 0.2, 2, 6));
}
