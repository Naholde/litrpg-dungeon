package com.andrey.litrpg.skill;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/** Состояние навыков игрока на сервере (не сохраняется): перезарядки, чтение, ресурс, прыжок. */
public final class SkillState {
    /** Читаемый навык: стоять на месте до {@code end}. */
    public record Casting(Skill skill, int slot, LivingEntity target, Vec3 point, long start, long end, Vec3 startPos) {
    }

    /** Тик, когда навык снова готов, и полная длина его перезарядки (для полосы на клиенте). */
    final Map<String, long[]> cooldowns = new HashMap<>();
    long gcdReady;
    long gcdTotal;
    long dashReady;
    long dashTotal;
    Casting casting;
    double resource;
    long lastResourceGain;
    /** Прыжок берсерка: что сделать при приземлении. */
    Runnable onLanding;
    long landingSince;
    boolean dirty = true;

    public double resource() {
        return resource;
    }

    public Casting casting() {
        return casting;
    }

    public boolean leaping() {
        return onLanding != null;
    }

    public long readyAt(String skillId) {
        long[] c = cooldowns.get(skillId);
        return c == null ? 0 : c[0];
    }

    public long totalOf(String skillId) {
        long[] c = cooldowns.get(skillId);
        return c == null ? 0 : c[1];
    }
}
