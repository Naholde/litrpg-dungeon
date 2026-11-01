package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.skill.SkillRegistry;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Навыки: общие правила (ранги, общая перезарядка, единица силы, рывок) и цифры каждого навыка.
 * Файл {@code balance/skills.json}. Навык или параметр, которого нет в коде, — ошибка (опечатка).
 * Полный файл со значениями по умолчанию пишет тест {@code SkillsJsonTest} при {@code -Dlitrpg.regen=true}.
 */
public record SkillsBalance(Common common, Map<String, Map<String, Double>> skills) {

    /**
     * @param gcdSeconds      общая перезарядка между навыками
     * @param powerBase       «единица силы» навыка на 1 уровне (урон/лечение при доле 1,0)
     * @param powerPerLevel   прибавка единицы за уровень
     * @param rankPower       +доля силы за ранг
     * @param rankCooldown    −доля перезарядки за ранг
     * @param rankDuration    +доля длительности за ранг
     * @param maxRank         предел ранга
     * @param dashCooldown    перезарядка рывка, с
     * @param dashStrength    сила рывка (скорость, блоков за тик)
     */
    public record Common(double gcdSeconds, double powerBase, double powerPerLevel, double rankPower, double rankCooldown,
                         double rankDuration, int maxRank, double dashCooldown, double dashStrength) {
        static final Codec<Common> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("gcd_seconds").forGetter(Common::gcdSeconds),
                Codec.DOUBLE.fieldOf("power_base").forGetter(Common::powerBase),
                Codec.DOUBLE.fieldOf("power_per_level").forGetter(Common::powerPerLevel),
                Codec.DOUBLE.fieldOf("rank_power").forGetter(Common::rankPower),
                Codec.DOUBLE.fieldOf("rank_cooldown").forGetter(Common::rankCooldown),
                Codec.DOUBLE.fieldOf("rank_duration").forGetter(Common::rankDuration),
                Codec.intRange(0, 10).fieldOf("max_rank").forGetter(Common::maxRank),
                Codec.DOUBLE.fieldOf("dash_cooldown").forGetter(Common::dashCooldown),
                Codec.DOUBLE.fieldOf("dash_strength").forGetter(Common::dashStrength)
        ).apply(i, Common::new));
    }

    private static final Codec<Map<String, Map<String, Double>>> SKILLS =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)).validate(m -> {
                for (var e : m.entrySet()) {
                    var skill = SkillRegistry.get(e.getKey());
                    if (skill == null) return DataResult.error(() -> "нет навыка «" + e.getKey() + "» (опечатка?)");
                    for (String k : e.getValue().keySet()) {
                        if (!skill.params().containsKey(k)) {
                            return DataResult.error(() -> e.getKey() + ": у навыка нет параметра «" + k + "»");
                        }
                    }
                }
                return DataResult.success(m);
            });

    public static final MapCodec<SkillsBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Common.CODEC.fieldOf("common").forGetter(SkillsBalance::common),
            SKILLS.fieldOf("skills").forGetter(SkillsBalance::skills)
    ).apply(i, SkillsBalance::new));

    public static final Common DEFAULT_COMMON = new Common(1.0, 8, 2.4, 0.08, 0.04, 0.06, 5, 5, 1.6);

    /** По умолчанию — все навыки со значениями из кода (так же выглядит и файл). */
    public static SkillsBalance defaults() {
        return new SkillsBalance(DEFAULT_COMMON, SkillRegistry.defaultValues());
    }

    public double value(String skill, String key, double def) {
        Map<String, Double> m = skills.get(skill);
        if (m == null) return def;
        return m.getOrDefault(key, def);
    }
}
