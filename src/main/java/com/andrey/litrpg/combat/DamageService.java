package com.andrey.litrpg.combat;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.StatBreakdown;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Конвейер урона. Любой урон в игре проходит через {@link #onIncoming}: усиление характеристиками атакующего,
 * крит, уклонение, стойкость, урон по своим, окружение в масштабе здоровья.
 * Урон навыков и посоха наносится через {@link #deal} — он помечает род урона и снимает ванильную неуязвимость.
 */
public final class DamageService {
    /** Что знает конвейер о текущем ударе, нанесённом через {@link #deal}. */
    private record Context(WeaponKind kind) {
    }

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    /** Удар был критическим — узнаём в {@code LivingDamageEvent.Post}, чтобы показать цифру крита. */
    private static final Map<LivingEntity, Boolean> CRIT = new WeakHashMap<>();

    private DamageService() {
    }

    /**
     * Нанести урон от имени атакующего (навык, посох). {@code base} — урон до усиления характеристиками.
     * Ванильная неуязвимость после удара (полсекунды) сбрасывается: иначе частые удары навыков «отскакивают».
     */
    public static boolean deal(LivingEntity attacker, Entity direct, LivingEntity target, double base, WeaponKind kind) {
        DamageSource src = kind == WeaponKind.MELEE
                ? (attacker instanceof net.minecraft.world.entity.player.Player p
                    ? attacker.damageSources().playerAttack(p) : attacker.damageSources().mobAttack(attacker))
                : attacker.damageSources().indirectMagic(direct != null ? direct : attacker, attacker);
        target.invulnerableTime = 0;
        CONTEXT.set(new Context(kind));
        try {
            return target.hurt(src, (float) base);
        } finally {
            CONTEXT.remove();
        }
    }

    public static boolean wasCrit(LivingEntity victim) {
        return Boolean.TRUE.equals(CRIT.remove(victim));
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource src = e.getSource();
        Entity attacker = src.getEntity();
        double amount = e.getAmount();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return; // /kill и падение в пустоту — как есть

        // 1. Атакующий — игрок: усиление характеристиками и крит.
        if (attacker instanceof ServerPlayer p && attacker != victim) {
            StatBreakdown b = PlayerService.breakdown(p);
            Context ctx = CONTEXT.get();
            WeaponKind kind = ctx != null ? ctx.kind() : kindOf(src, p);
            amount = DamageMath.outgoing(amount, kind, b);
            if (kind != WeaponKind.PLAIN && p.getRandom().nextDouble() < b.get(Derived.CRIT_CHANCE)) {
                amount *= b.get(Derived.CRIT_DAMAGE);
                CRIT.put(victim, true);
            }
        }

        // 2. Жертва — игрок.
        if (victim instanceof ServerPlayer vp) {
            StatBreakdown b = PlayerService.breakdown(vp);
            if (attacker instanceof ServerPlayer && attacker != victim) {
                amount *= Balance.get(Balance.COMBAT).friendlyFireMult();
            } else if (attacker == null) {
                if (Balance.get(Balance.COMBAT).environmentScalesWithHealth()) {
                    amount = DamageMath.environment(amount, vp.getMaxHealth());
                }
            } else if (attacker instanceof LivingEntity && !isBoss(attacker) && vp.invulnerableTime <= 10
                    && vp.getRandom().nextDouble() < b.get(Derived.DODGE)) {
                // Уклонение — только от монстров; удары хранителей читаются по разметке, от них уходят ногами.
                e.setCanceled(true);
                Popups.dodge(vp);
                return;
            }
            amount = DamageMath.incoming(amount, b, isBoss(attacker));
        }
        e.setAmount((float) amount);
    }

    private static WeaponKind kindOf(DamageSource src, ServerPlayer p) {
        if (src.getDirectEntity() instanceof AbstractArrow) return WeaponKind.RANGED;
        if (src.getDirectEntity() == p) return WeaponKind.MELEE;
        return WeaponKind.PLAIN;
    }

    /** Хранитель — сущность из тега {@code litrpg:bosses} (появятся в фазе хранителей). */
    public static boolean isBoss(Entity e) {
        return e != null && e.getType().is(com.andrey.litrpg.core.ModTags.BOSSES);
    }
}
