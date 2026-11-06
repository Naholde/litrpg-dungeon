package com.andrey.litrpg.gear;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Как работают особые свойства вещей ({@link GearEffect}). Конвейер урона и лечения спрашивает здесь
 * множители и сообщает о событиях (удар, рывок, победа над врагом). Сумма свойств надетого считается раз в тик.
 */
public final class GearEffects {
    private GearEffects() {
    }

    private record Cached(long tick, Map<GearEffect, Double> sum) {
    }

    private static final Map<UUID, Cached> CACHE = new HashMap<>();
    /** «Второе дыхание»: когда снова можно. */
    private static final Map<UUID, Long> SECOND_WIND_READY = new HashMap<>();
    private static final ThreadLocal<Boolean> SPARKING = ThreadLocal.withInitial(() -> false);

    /** Сила свойства у игрока (сумма по надетым вещам; 0 — нет). */
    public static double of(LivingEntity e, GearEffect fx) {
        if (!(e instanceof Player p)) return 0;
        long now = p.level().getGameTime();
        Cached c = CACHE.get(p.getUUID());
        if (c == null || c.tick != now) {
            Map<GearEffect, Double> sum = new EnumMap<>(GearEffect.class);
            PlayerGear.forEachWorn(p, d -> {
                for (GearData.Effect ef : d.effects()) sum.merge(ef.type(), ef.value(), Double::sum);
            });
            c = new Cached(now, sum);
            CACHE.put(p.getUUID(), c);
        }
        return c.sum.getOrDefault(fx, 0.0);
    }

    /** Множитель урона игрока по цели: добивание, первый удар, окно «Изнурён». */
    public static double outgoing(ServerPlayer p, LivingEntity victim) {
        double k = 1;
        double hp = victim.getHealth() / Math.max(1, victim.getMaxHealth());
        if (hp < 0.3) k += of(p, GearEffect.EXECUTE);
        if (hp >= 0.999) k += of(p, GearEffect.FIRST_STRIKE);
        if (victim instanceof BossEntity b && b.tired()) k += of(p, GearEffect.OPENING);
        return k;
    }

    /** Множитель урона по игроку: оплот (враг бьёт именно его) и опека союзников рядом. Не меньше ×0,75. */
    public static double incoming(ServerPlayer victim, LivingEntity attacker) {
        double cut = 0;
        if (attacker instanceof Mob m && m.getTarget() == victim) cut += of(victim, GearEffect.BULWARK);
        double ward = 0;
        for (ServerPlayer ally : victim.serverLevel().players()) {
            if (ally == victim || !ally.isAlive() || ally.distanceToSqr(victim) > 36) continue;
            ward = Math.max(ward, of(ally, GearEffect.WARDEN));
        }
        return Math.max(0.75, 1 - cut - ward);
    }

    /** После удара игрока: кровопийца и искра. */
    public static void afterDealt(ServerPlayer p, LivingEntity victim, float dealt) {
        double leech = of(p, GearEffect.VAMPIRIC);
        if (leech > 0) HealService.healFixed(p, p, dealt * leech);
        double spark = of(p, GearEffect.SPARK);
        if (spark <= 0 || SPARKING.get() || p.getRandom().nextDouble() >= 0.12 || !(p.level() instanceof ServerLevel sl)) return;
        SPARKING.set(true);
        try {
            var near = sl.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(5),
                    e -> e != victim && e != p && e.isAlive() && !(e instanceof Player) && e instanceof net.minecraft.world.entity.monster.Enemy);
            near.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(victim)));
            for (int i = 0; i < Math.min(2, near.size()); i++) {
                LivingEntity t = near.get(i);
                Fx.bolt(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0), t.position().add(0, t.getBbHeight() * 0.6, 0),
                        0.12f, 8, 0x9FD8FF);
                DamageService.deal(p, null, t, dealt * spark, WeaponKind.FIXED);
            }
            if (!near.isEmpty()) sl.playSound(null, victim.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.4f, 1.8f);
        } finally {
            SPARKING.set(false);
        }
    }

    /** После удара по игроку: шипы (ближний бой) и второе дыхание. */
    public static void afterTaken(ServerPlayer victim, LivingEntity attacker, boolean melee, float dealt) {
        double thorns = of(victim, GearEffect.THORNS);
        if (thorns > 0 && melee && attacker != null && attacker != victim) {
            DamageService.deal(victim, null, attacker, dealt * thorns, WeaponKind.FIXED);
        }
        double wind = of(victim, GearEffect.SECOND_WIND);
        long now = victim.level().getGameTime();
        if (wind > 0 && victim.isAlive() && victim.getHealth() < victim.getMaxHealth() * 0.3
                && now >= SECOND_WIND_READY.getOrDefault(victim.getUUID(), 0L)) {
            SECOND_WIND_READY.put(victim.getUUID(), now + 60 * 20);
            StatusService.apply(victim, Status.of(StatusType.SHIELD, "gear_second_wind", victim.getUUID(),
                    victim.getMaxHealth() * wind, now, 8 * 20));
            Fx.domeOn(victim.serverLevel(), victim, 1.4f, 20, 0x9CFFB0);
            victim.level().playSound(null, victim.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.4f);
        }
    }

    /** Множитель лечения от лекаря: милосердие к тяжело раненым. */
    public static double healBonus(ServerPlayer healer, LivingEntity target) {
        if (healer == null || healer == target) return 1;
        return target.getHealth() < target.getMaxHealth() * 0.5 ? 1 + of(healer, GearEffect.MERCY) : 1;
    }

    /** После лечения: перелечение — в щит (избыток), часть лечения союзника — себе (отклик). */
    public static void afterHeal(ServerPlayer healer, LivingEntity target, double amount, float real) {
        if (healer == null) return;
        double over = amount - real;
        double overflow = of(healer, GearEffect.OVERFLOW);
        if (overflow > 0 && over > 1) {
            double cap = target.getMaxHealth() * 0.3;
            double have = StatusService.magnitudeFrom(target, StatusType.SHIELD, healer.getUUID());
            double add = Math.min(cap - have, over * overflow);
            if (add > 0) {
                StatusService.apply(target, Status.of(StatusType.SHIELD, "gear_overflow", healer.getUUID(), have + add,
                        target.level().getGameTime(), 8 * 20));
            }
        }
        double echo = of(healer, GearEffect.ECHO);
        if (echo > 0 && target != healer && real > 0) HealService.healFixed(healer, healer, real * echo);
    }

    /** Рывок: ярость рывка — урон выше на 3 с. */
    public static void onDash(ServerPlayer p) {
        double fury = of(p, GearEffect.DASH_FURY);
        if (fury <= 0) return;
        StatusService.apply(p, Status.of(StatusType.DAMAGE_DEALT, "gear_dash_fury", p.getUUID(), 1 + fury, p.level().getGameTime(), 60));
    }

    /** Победа над врагом: попутный ветер. */
    public static void onKill(ServerPlayer p) {
        double wind = of(p, GearEffect.TAILWIND);
        if (wind <= 0) return;
        StatusService.apply(p, Status.of(StatusType.SPEED, "gear_tailwind", p.getUUID(), wind, p.level().getGameTime(), 80));
    }

    /** Игрок вышел — забыть его. */
    public static void forget(UUID id) {
        CACHE.remove(id);
        SECOND_WIND_READY.remove(id);
    }
}
