package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.mob.Bestiary;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Map;

/**
 * Монстры: разница уровней, награды и цифры каждого вида. Файл {@code balance/mobs.json} (генерируется).
 *
 * @param gapPlayerDamage  на сколько меньше урона наносит игрок за каждый уровень, на который монстр выше (доля)
 * @param gapMinDamage     ниже этой доли урон не падает
 * @param gapMobDamage     на сколько больше бьёт монстр за каждый уровень выше игрока
 * @param xpPerKillShare   опыт за обычного монстра — доля опыта до следующего уровня (до точного бюджета этапа)
 * @param packRadius       на каком расстоянии сородичи подхватывают агро
 * @param aggroRadius      на каком расстоянии монстр замечает игрока
 */
public record MobsBalance(double gapPlayerDamage, double gapMinDamage, double gapMobDamage, double xpPerKillShare,
                          double packRadius, double aggroRadius, Map<String, Map<String, Double>> species) {

    private static final Codec<Map<String, Map<String, Double>>> SPECIES =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.DOUBLE)).validate(m -> {
                for (var e : m.entrySet()) {
                    var s = Bestiary.get(e.getKey());
                    if (s == null) return DataResult.error(() -> "нет вида «" + e.getKey() + "»");
                    for (String k : e.getValue().keySet()) {
                        if (!s.params().containsKey(k)) return DataResult.error(() -> e.getKey() + ": нет параметра «" + k + "»");
                    }
                }
                return DataResult.success(m);
            });

    public static final MapCodec<MobsBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("gap_player_damage").forGetter(MobsBalance::gapPlayerDamage),
            Codec.DOUBLE.fieldOf("gap_min_damage").forGetter(MobsBalance::gapMinDamage),
            Codec.DOUBLE.fieldOf("gap_mob_damage").forGetter(MobsBalance::gapMobDamage),
            Codec.DOUBLE.fieldOf("xp_per_kill_share").forGetter(MobsBalance::xpPerKillShare),
            Codec.DOUBLE.fieldOf("pack_radius").forGetter(MobsBalance::packRadius),
            Codec.DOUBLE.fieldOf("aggro_radius").forGetter(MobsBalance::aggroRadius),
            SPECIES.fieldOf("species").forGetter(MobsBalance::species)
    ).apply(i, MobsBalance::new));

    public static MobsBalance defaults() {
        return new MobsBalance(0.06, 0.4, 0.09, 0.035, 10, 12, Bestiary.defaultValues());
    }

    public double value(String species, String key, double def) {
        Map<String, Double> m = this.species.get(species);
        return m == null ? def : m.getOrDefault(key, def);
    }
}
