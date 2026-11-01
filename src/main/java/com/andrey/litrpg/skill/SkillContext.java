package com.andrey.litrpg.skill;

import com.andrey.litrpg.combat.DamageService;
import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.combat.WeaponKind;
import com.andrey.litrpg.party.PartyService;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.StatBreakdown;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Всё, что нужно навыку при срабатывании: кто, с каким рангом, по кому/куда, и помощники,
 * чтобы класс не писал свой урон, лечение и выбор целей.
 */
public final class SkillContext {
    public final ServerPlayer caster;
    public final Skill skill;
    public final int rank;
    /** Цель (для ENEMY/ALLY), иначе null. */
    public final LivingEntity target;
    /** Точка (для GROUND), иначе позиция заклинателя. */
    public final Vec3 point;
    public final ServerLevel level;

    public SkillContext(ServerPlayer caster, Skill skill, int rank, LivingEntity target, Vec3 point) {
        this.caster = caster;
        this.skill = skill;
        this.rank = rank;
        this.target = target;
        this.point = point;
        this.level = caster.serverLevel();
    }

    /** Параметр навыка (с рангом и skills.json). */
    public double v(String key) {
        return skill.value(key, rank);
    }

    public int ticks(String key) {
        return skill.ticks(key, rank);
    }

    public long now() {
        return level.getGameTime();
    }

    /** Единица силы этого игрока: доля 1,0 параметра урона/лечения = столько очков. */
    public double unit() {
        return SkillMath.unit(PlayerService.data(caster).level());
    }

    // ------------------------------------------------------------------ урон и лечение

    /** Ударить навыком: доля единицы × (1 + сила навыков) × роль, с критом — через общий конвейер. */
    public boolean hit(LivingEntity victim, String damageKey) {
        return hitShare(victim, v(damageKey));
    }

    public boolean hitShare(LivingEntity victim, double share) {
        if (victim == null || !victim.isAlive() || !isEnemy(victim)) return false;
        return DamageService.deal(caster, null, victim, share * unit(), WeaponKind.SKILL);
    }

    /** Урон, посчитанный сейчас (для отложенных ударов и урона со временем): усиление на момент применения. */
    public double precomputed(String damageKey) {
        StatBreakdown b = PlayerService.breakdown(caster);
        return v(damageKey) * unit() * (1 + b.get(Derived.SKILL_POWER)) * b.get(Derived.DAMAGE_MULT)
                * StatusService.product(caster, StatusType.DAMAGE_DEALT);
    }

    /** Лечение: доля единицы × сила лечения лекаря. */
    public float heal(LivingEntity who, String healKey) {
        return HealService.heal(caster, who, v(healKey) * unit());
    }

    /** Щит-поглощение на союзника. */
    public void shield(LivingEntity who, String shieldKey, String secondsKey) {
        double amount = v(shieldKey) * unit() * PlayerService.breakdown(caster).get(Derived.HEAL_POWER);
        StatusService.apply(who, Status.of(StatusType.SHIELD, skill.id, caster.getUUID(), amount, now(), ticks(secondsKey)));
    }

    /** Наложить метку: сила — значение параметра (или готовое число). */
    public void status(LivingEntity who, StatusType type, double magnitude, String secondsKey) {
        StatusService.apply(who, Status.of(type, skill.id, caster.getUUID(), magnitude, now(), ticks(secondsKey)));
    }

    /** Урон со временем: всего {@code damageKey} за {@code secondsKey}, срабатывает раз в секунду. */
    public void dot(LivingEntity victim, String damageKey, String secondsKey) {
        int dur = ticks(secondsKey);
        int pulses = Math.max(1, dur / 20);
        StatusService.apply(victim, Status.periodic(StatusType.DOT, skill.id, caster.getUUID(),
                precomputed(damageKey) / pulses, now(), pulses * 20, 20));
    }

    /** Лечение со временем: всего {@code healKey} за {@code secondsKey}, раз в секунду. */
    public void hot(LivingEntity who, String healKey, String secondsKey) {
        int dur = ticks(secondsKey);
        int pulses = Math.max(1, dur / 20);
        double total = v(healKey) * unit() * PlayerService.breakdown(caster).get(Derived.HEAL_POWER);
        StatusService.apply(who, Status.periodic(StatusType.HOT, skill.id, caster.getUUID(), total / pulses,
                now(), pulses * 20, 20));
    }

    // ------------------------------------------------------------------ ресурс класса

    public double resource() {
        return SkillService.state(caster).resource;
    }

    /** Потратить весь ресурс; вернуть, сколько было. */
    public double spendAllResource() {
        SkillState st = SkillService.state(caster);
        double r = st.resource;
        st.resource = 0;
        st.dirty = true;
        return r;
    }

    /** Действие при приземлении (прыжок): сработает, когда игрок коснётся земли. */
    public void onLanding(Runnable r) {
        SkillState st = SkillService.state(caster);
        st.onLanding = r;
        st.landingSince = now();
    }

    // ------------------------------------------------------------------ кто свой, кто враг

    public boolean isEnemy(LivingEntity e) {
        return e != caster && !(e instanceof Player) && e.isAlive() && !e.isSpectator();
    }

    public boolean isAlly(LivingEntity e) {
        return e instanceof ServerPlayer p && p.isAlive() && !p.isSpectator()
                && (p == caster || PartyService.sameParty(caster, p));
    }

    public List<LivingEntity> enemiesAround(Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> isEnemy(e) && e.position().distanceToSqr(center) <= radius * radius);
    }

    public List<LivingEntity> alliesAround(Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> isAlly(e) && e.position().distanceToSqr(center) <= radius * radius);
    }

    /** Враги в секторе перед заклинателем. */
    public List<LivingEntity> enemiesInCone(double radius, double angleDeg) {
        Vec3 o = caster.position();
        Vec3 dir = caster.getViewVector(1).multiply(1, 0, 1).normalize();
        double cos = Math.cos(Math.toRadians(angleDeg / 2));
        return enemiesAround(o, radius).stream().filter(e -> {
            Vec3 to = e.position().subtract(o).multiply(1, 0, 1);
            return to.lengthSqr() < 0.25 || to.normalize().dot(dir) >= cos;
        }).toList();
    }
}
