package com.andrey.litrpg.combat;

import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.PopupPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Кому показывать цифры. Каждый видит свой урон и урон по себе, лечащий — своё лечение:
 * при пяти игроках чужие цифры превращаются в кашу.
 */
public final class Popups {
    private Popups() {
    }

    public static void damage(LivingEntity victim, LivingEntity attacker, float amount, boolean crit) {
        if (amount <= 0) return;
        if (attacker instanceof ServerPlayer p && attacker != victim) {
            Net.send(p, new PopupPayload(victim.getId(), amount, crit ? PopupPayload.Kind.CRIT : PopupPayload.Kind.DEALT));
        }
        if (victim instanceof ServerPlayer vp) {
            Net.send(vp, new PopupPayload(vp.getId(), amount, PopupPayload.Kind.TAKEN));
        }
    }

    public static void heal(LivingEntity target, ServerPlayer healer, float amount) {
        if (amount <= 0) return;
        var payload = new PopupPayload(target.getId(), amount, PopupPayload.Kind.HEAL);
        if (healer != null) Net.send(healer, payload);
        if (target instanceof ServerPlayer tp && tp != healer) Net.send(tp, payload);
    }

    /** Удар целиком ушёл в щит. */
    public static void absorbed(LivingEntity victim, LivingEntity attacker) {
        if (victim instanceof ServerPlayer vp) Net.send(vp, new PopupPayload(vp.getId(), 0, PopupPayload.Kind.ABSORB));
        if (attacker instanceof ServerPlayer ap) Net.send(ap, new PopupPayload(victim.getId(), 0, PopupPayload.Kind.ABSORB));
    }

    private static final java.util.Map<LivingEntity, Long> LAST_WARD = new java.util.WeakHashMap<>();

    /** Щит врага принял удар игрока (страж бестиария спереди). */
    public static void blocked(LivingEntity victim, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer ap) Net.send(ap, new PopupPayload(victim.getId(), 0, PopupPayload.Kind.BLOCK));
    }

    /** Удар игрока прошёл мимо. */
    public static void missed(LivingEntity victim, LivingEntity attacker) {
        if (attacker instanceof ServerPlayer ap) Net.send(ap, new PopupPayload(victim.getId(), 0, PopupPayload.Kind.MISS));
    }

    /** Несокрушимость оставила 1 здоровья: видно и самому, и тому, кто его держит. */
    public static void ward(LivingEntity victim) {
        long now = victim.level().getGameTime();
        Long last = LAST_WARD.get(victim);
        if (last != null && now - last < 15) return; // под градом ударов — не чаще раза в 0,75 с
        LAST_WARD.put(victim, now);
        if (victim instanceof ServerPlayer vp) Net.send(vp, new PopupPayload(vp.getId(), 0, PopupPayload.Kind.WARD));
    }

    public static void dodge(ServerPlayer victim) {
        Net.send(victim, new PopupPayload(victim.getId(), 0, PopupPayload.Kind.DODGE));
    }
}
