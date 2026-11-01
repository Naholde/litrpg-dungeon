package com.andrey.litrpg.anim;

import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.PlayerAnimPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Сервер показывает движение игрока всем, кто его видит. Движение — только картинка: механика навыка
 * считается отдельно, поэтому потерянный пакет ничего не ломает.
 */
public final class AnimService {
    private static final double RANGE = 72;

    private AnimService() {
    }

    /** Показать движение навыка. */
    public static void play(ServerPlayer p, String anim) {
        play(p, anim, 1);
    }

    /**
     * Показать движение, растянутое в {@code speed} раз. Для чтения навыка: {@code speed} такой,
     * чтобы движение шло ровно столько, сколько читается навык.
     */
    public static void play(ServerPlayer p, String anim, float speed) {
        if (!(p.level() instanceof ServerLevel level)) return;
        Anim a = Anims.orSwing(anim);
        if (a == null) return;
        var payload = new PlayerAnimPayload(p.getId(), a.id, Math.max(0.1f, speed));
        for (ServerPlayer o : level.players()) {
            if (o.position().distanceToSqr(p.position()) < RANGE * RANGE) Net.send(o, payload);
        }
    }

    /** Движение, растянутое ровно на {@code ticks} тиков (чтение навыка, долгая поза). */
    public static void stretch(ServerPlayer p, String anim, int ticks) {
        Anim a = Anims.get(anim);
        if (a == null) return;
        play(p, anim, Math.max(0.1f, ticks / (float) Math.max(1, a.ticks)));
    }

    /** Поза чтения навыка: своя, если есть {@code <навык>.cast}, иначе общая. */
    public static void cast(ServerPlayer p, com.andrey.litrpg.skill.Skill skill, int ticks) {
        String id = skill.id + ".cast";
        stretch(p, Anims.has(id) ? id : "common.cast", ticks);
    }

    /** Оборвать движение (чтение сбито). */
    public static void stop(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        var payload = new PlayerAnimPayload(p.getId(), "", 1);
        for (ServerPlayer o : level.players()) {
            if (o.position().distanceToSqr(p.position()) < RANGE * RANGE) Net.send(o, payload);
        }
    }
}
