package com.andrey.litrpg.fx;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Готовые «подписи» эффектов: попадание, лечение, щит, взрыв, зона, луч. Их зовёт сам каркас навыков,
 * поэтому у любой способности есть объёмный эффект под её цветом, а не только разметка на полу.
 * <p>Цвет берётся у навыка — у каждого класса он свой, поэтому огонь виден оранжевым, лёд голубым,
 * а тень фиолетовой без лишних настроек.
 */
public final class Spell {
    private Spell() {
    }

    /** Середина тела — туда бьют эффекты попадания. */
    private static Vec3 chest(Entity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    /** Частицы под цвет: искры, которые разлетаются. */
    private static void sparks(ServerLevel l, Vec3 at, int color, int count, double spread, double speed, float size) {
        Fx.dust(l, at, color, count, spread, speed, size);
    }

    // ------------------------------------------------------------------ бой

    /** Попадание навыком: вспышка-шар, искры под цвет навыка и крошка удара. */
    public static void impact(ServerLevel l, LivingEntity victim, int color) {
        Vec3 at = chest(victim);
        Fx.orb(l, at, 0.55f, 5, color);
        sparks(l, at, color, 8, 0.35, 0.08, 1.1f);
        Fx.puff(l, ParticleTypes.CRIT, at, 5, 0.25, 0.12);
    }

    /** Сильное попадание (добивание, ульта): шар крупнее, искры во все стороны и волна у ног. */
    public static void heavy(ServerLevel l, LivingEntity victim, int color) {
        Vec3 at = chest(victim);
        Fx.orb(l, at, 1.0f, 8, color);
        Fx.shards(l, at, 1.8f, 12, 14, color);
        sparks(l, at, color, 20, 0.5, 0.2, 1.5f);
        Fx.puff(l, ParticleTypes.ENCHANTED_HIT, at, 10, 0.4, 0.25);
        Fx.wave(l, victim.position(), 2.2f, 1.2f, 12, color);
    }

    /** Взмах оружием в воздухе: полумесяц по направлению взгляда и искры по дуге. */
    public static void swing(ServerLevel l, LivingEntity e, int color, float reach, float arcDeg) {
        Fx.arc(l, e, reach, arcDeg, 9, color);
        Vec3 dir = e.getViewVector(1).multiply(1, 0, 1).normalize();
        sparks(l, e.position().add(dir.scale(reach * 0.7)).add(0, e.getBbHeight() * 0.6, 0), color, 10, 0.5, 0.05, 1.2f);
    }

    /** Взрыв в точке: ударная волна, осколки и вспышка. */
    public static void boom(ServerLevel l, Vec3 at, float radius, int color) {
        Fx.wave(l, at, radius, Math.min(2.5f, radius * 0.6f), 14, color);
        Fx.shards(l, at.add(0, 0.4, 0), radius * 0.8f, (int) (6 + radius * 3), 16, color);
        Fx.orb(l, at.add(0, 0.6, 0), radius * 0.35f, 8, color);
        sparks(l, at.add(0, 0.5, 0), color, (int) (12 + radius * 4), radius * 0.35, 0.25, 1.4f);
    }

    /** Луч от заклинателя к цели: молния под цвет и дорожка искр. */
    public static void ray(ServerLevel l, Vec3 from, Vec3 to, int color) {
        Fx.bolt(l, from, to, 0.16f, 7, color);
        Fx.trail(l, from, to, ParticleTypes.END_ROD, 8);
    }

    /** Нить между двумя сущностями (цепь, искупление, кровавая нить). */
    public static void link(ServerLevel l, Entity a, Entity b, int color, int ticks) {
        Fx.bolt(l, a, b, 0.14f, ticks, color);
    }

    // ------------------------------------------------------------------ лечение и защита

    /** Лечение: восходящая спираль и зелёные искры. */
    public static void mend(ServerLevel l, LivingEntity who, int color) {
        Fx.spiralOn(l, who, 0.75f, who.getBbHeight() + 0.3f, 16, color);
        Fx.puff(l, ParticleTypes.HAPPY_VILLAGER, chest(who), 6, 0.4, 0.02);
        sparks(l, chest(who), color, 6, 0.4, 0.05, 1f);
    }

    /** Щит: купол на сущности и блики. */
    public static void shield(ServerLevel l, LivingEntity who, int color) {
        Fx.domeOn(l, who, 1.2f, 14, color);
        sparks(l, chest(who), color, 10, 0.5, 0.02, 1.2f);
    }

    /** Усиление союзника: руна под ногами и столбик искр. */
    public static void buff(ServerLevel l, LivingEntity who, int color, int ticks) {
        Fx.runeOn(l, who, 0.9f, 6, ticks, color);
        Fx.spiralOn(l, who, 0.6f, who.getBbHeight(), Math.min(40, ticks), color);
    }

    // ------------------------------------------------------------------ чтение и зоны

    /** Навык сработал: вспышка у рук заклинателя. */
    public static void cast(ServerLevel l, LivingEntity caster, int color) {
        Vec3 hands = caster.position().add(caster.getViewVector(1).multiply(0.45, 0, 0.45))
                .add(0, caster.getBbHeight() * 0.75, 0);
        Fx.orb(l, hands, 0.3f, 5, color);
        sparks(l, hands, color, 5, 0.18, 0.03, 0.9f);
    }

    /** Пока навык читается: руна под ногами и искры, которые сходятся к рукам. */
    public static void channel(ServerLevel l, LivingEntity caster, int color, int ticks) {
        Fx.rune(l, caster.position().add(0, 0.05, 0), 1.1f, 8, ticks, color);
        Fx.spiralOn(l, caster, 0.7f, caster.getBbHeight(), ticks, color);
    }

    /** Зона на земле: граница, восходящие искры и спираль в центре. */
    public static void zone(ServerLevel l, Vec3 at, float radius, int color, int ticks) {
        Fx.disc(l, at, radius, ticks, color, false);
        Fx.spiral(l, at, radius * 0.55f, 2.2f, ticks, color);
        Fx.ringOf(l, dustOf(color), at, radius, (int) (radius * 6), 0.02);
    }

    /** Срабатывание зоны: лёгкая волна искр по кругу. */
    public static void pulse(ServerLevel l, Vec3 at, float radius, int color) {
        Fx.ringOf(l, dustOf(color), at, radius * 0.9, (int) (radius * 5), 0.05);
    }

    /** Тик урона со временем на цели. */
    public static void burn(ServerLevel l, LivingEntity victim, int color) {
        sparks(l, chest(victim), color, 4, 0.3, 0.03, 0.9f);
    }

    /** Тик лечения со временем на цели. */
    public static void bloom(ServerLevel l, LivingEntity who, int color) {
        Fx.puff(l, ParticleTypes.HAPPY_VILLAGER, chest(who), 2, 0.35, 0.01);
        sparks(l, chest(who), color, 3, 0.3, 0.02, 0.9f);
    }

    private static ParticleOptions dustOf(int color) {
        return new net.minecraft.core.particles.DustParticleOptions(
                new org.joml.Vector3f(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f), 1.2f);
    }
}
