package com.andrey.litrpg.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * «На ком агро». Над врагом: красный «⚠», если он бьёт вас; имя союзника — если бьёт его.
 * Сервер присылает цель при смене и раз в 2 с; без обновлений 5 с — запись устаревает.
 */
public final class ClientAggro {
    private record Entry(int targetId, long seen) {
    }

    private static final Map<Integer, Entry> TARGETS = new HashMap<>();
    private static final int STALE = 100;

    private ClientAggro() {
    }

    public static void set(int mobId, int targetId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (targetId < 0) TARGETS.remove(mobId);
        else TARGETS.put(mobId, new Entry(targetId, mc.level.getGameTime()));
    }

    /** Кого бьёт враг (id сущности) или −1. */
    public static int targetOf(int mobId) {
        Entry e = TARGETS.get(mobId);
        Minecraft mc = Minecraft.getInstance();
        if (e == null || mc.level == null || mc.level.getGameTime() - e.seen > STALE) return -1;
        return e.targetId;
    }

    /** Сколько живых врагов сейчас бьют эту сущность. */
    public static int countTargeting(int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return 0;
        int n = 0;
        for (var e : TARGETS.entrySet()) {
            if (e.getValue().targetId != entityId || mc.level.getGameTime() - e.getValue().seen > STALE) continue;
            Entity mob = mc.level.getEntity(e.getKey());
            if (mob instanceof LivingEntity l && l.isAlive()) n++;
        }
        return n;
    }

    public static void renderWorld(PoseStack ps, MultiBufferSource buf, Camera cam, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        long now = mc.level.getGameTime();
        TARGETS.values().removeIf(e -> now - e.seen > STALE);
        for (var e : TARGETS.entrySet()) {
            Entity mob = mc.level.getEntity(e.getKey());
            Entity target = mc.level.getEntity(e.getValue().targetId);
            if (!(mob instanceof LivingEntity l) || !l.isAlive() || target == null) continue;
            if (mob.distanceToSqr(mc.player) > 40 * 40) continue;
            double extra = mob.shouldShowName() || mob.hasCustomName() ? 0.75 : 0.45;
            var pos = mob.getPosition(partial).add(0, mob.getBbHeight() + extra, 0);
            if (target == mc.player) {
                boolean blink = (now / 6) % 2 == 0;
                WorldText.draw(ps, buf, cam, pos, "⚠", blink ? 0xFFFF4040 : 0xFFFF9A9A, 2.2f, false);
            } else if (target instanceof Player p) {
                WorldText.draw(ps, buf, cam, pos, "→ " + p.getGameProfile().getName(), 0xFFFFD36B, 1.0f, false);
            }
        }
    }

    public static void clear() {
        TARGETS.clear();
    }
}
