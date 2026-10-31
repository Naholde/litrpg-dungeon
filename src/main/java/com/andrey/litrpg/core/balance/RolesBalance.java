package com.andrey.litrpg.core.balance;

import com.andrey.litrpg.player.Role;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.Map;

/** Особенности ролей. Файл {@code balance/roles.json}: по разделу на каждую роль, включая {@code none}. */
public record RolesBalance(Map<Role, RoleBalance> roles) {

    /**
     * @param healthMult          множитель итогового здоровья
     * @param vitalityHealthMult  во сколько раз больше здоровья даёт Выносливость
     * @param weaponDamageMult    множитель своего урона (оружие и навыки)
     * @param skillPowerBonus     прибавка к силе навыков (доля)
     * @param critBonus           прибавка к шансу крита (доля)
     * @param toughnessBonus      стойкость — доля снижения любого урона
     * @param threatMult          множитель угрозы
     * @param healMult            множитель исходящего лечения
     * @param bossDamageTakenMult множитель урона от хранителей
     * @param growth              рост характеристик за уровень (до получения класса)
     */
    public record RoleBalance(double healthMult, double vitalityHealthMult, double weaponDamageMult,
                              double skillPowerBonus, double critBonus, double toughnessBonus, double threatMult,
                              double healMult, double bossDamageTakenMult, StatSet growth) {
        static final Codec<RoleBalance> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("health_mult").forGetter(RoleBalance::healthMult),
                Codec.DOUBLE.fieldOf("vitality_health_mult").forGetter(RoleBalance::vitalityHealthMult),
                Codec.DOUBLE.fieldOf("weapon_damage_mult").forGetter(RoleBalance::weaponDamageMult),
                Codec.DOUBLE.fieldOf("skill_power_bonus").forGetter(RoleBalance::skillPowerBonus),
                Codec.DOUBLE.fieldOf("crit_bonus").forGetter(RoleBalance::critBonus),
                Codec.DOUBLE.fieldOf("toughness_bonus").forGetter(RoleBalance::toughnessBonus),
                Codec.DOUBLE.fieldOf("threat_mult").forGetter(RoleBalance::threatMult),
                Codec.DOUBLE.fieldOf("heal_mult").forGetter(RoleBalance::healMult),
                Codec.DOUBLE.fieldOf("boss_damage_taken_mult").forGetter(RoleBalance::bossDamageTakenMult),
                StatSet.CODEC.fieldOf("growth").forGetter(RoleBalance::growth)
        ).apply(i, RoleBalance::new));
    }

    private static final Codec<Map<Role, RoleBalance>> MAP = Codec.unboundedMap(Role.CODEC, RoleBalance.CODEC)
            .validate(m -> {
                for (Role r : Role.values()) {
                    if (!m.containsKey(r)) return DataResult.error(() -> "нет раздела для роли " + r.id);
                }
                return DataResult.success(m);
            });

    public static final MapCodec<RolesBalance> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            MAP.fieldOf("roles").forGetter(RolesBalance::roles)
    ).apply(i, RolesBalance::new));

    public RoleBalance of(Role role) {
        return roles.get(role);
    }

    public static final RolesBalance DEFAULT;

    static {
        Map<Role, RoleBalance> m = new EnumMap<>(Role.class);
        m.put(Role.NONE, new RoleBalance(1, 1, 1, 0, 0, 0, 1, 0.5, 1, new StatSet(1, 1, 1, 1, 0.5)));
        m.put(Role.TANK, new RoleBalance(1.3, 2.5, 0.6, 0, 0, 0.08, 5, 0.5, 0.5, new StatSet(1.2, 0.5, 2.0, 0.5, 0.5)));
        m.put(Role.DAMAGE, new RoleBalance(1, 1, 1.15, 0.08, 0.03, 0, 1, 0.5, 1, new StatSet(1.4, 1.4, 0.8, 1.0, 0.6)));
        m.put(Role.SUPPORT, new RoleBalance(1, 1, 0.75, 0, 0, 0, 1, 2.2, 1, new StatSet(0.5, 0.8, 1.0, 2.0, 0.7)));
        DEFAULT = new RolesBalance(Map.copyOf(m));
    }
}
