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

    public static void dodge(ServerPlayer victim) {
        Net.send(victim, new PopupPayload(victim.getId(), 0, PopupPayload.Kind.DODGE));
    }
}
