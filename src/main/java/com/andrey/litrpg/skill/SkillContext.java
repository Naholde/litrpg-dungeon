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
        boolean hit = DamageService.deal(caster, null, victim, share * unit(), WeaponKind.SKILL);
        if (hit) com.andrey.litrpg.fx.Spell.impact(level, victim, skill.color);
        return hit;
    }

    /** Урон, посчитанный сейчас (для отложенных ударов и урона со временем): усиление на момент применения. */
    public double precomputed(String damageKey) {
        StatBreakdown b = PlayerService.breakdown(caster);
        return v(damageKey) * unit() * (1 + b.get(Derived.SKILL_POWER)) * b.get(Derived.DAMAGE_MULT)
                * StatusService.product(caster, StatusType.DAMAGE_DEALT);
    }

    /** Лечение: доля единицы × сила лечения лекаря. */
    public float heal(LivingEntity who, String healKey) {
        float healed = HealService.heal(caster, who, v(healKey) * unit());
        if (healed > 0) com.andrey.litrpg.fx.Spell.mend(level, who, skill.color);
        return healed;
    }

    /** Щит-поглощение на союзника. */
    public void shield(LivingEntity who, String shieldKey, String secondsKey) {
        double amount = v(shieldKey) * unit() * PlayerService.breakdown(caster).get(Derived.HEAL_POWER);
        StatusService.apply(who, Status.of(StatusType.SHIELD, skill.id, caster.getUUID(), amount, now(), ticks(secondsKey)));
        com.andrey.litrpg.fx.Spell.shield(level, who, skill.color);
    }

    /** Щит на готовую величину (когда её считает сам навык: за каждый тотем, за каждую душу). */
    public void shieldFixed(LivingEntity who, double amount, String secondsKey) {
        StatusService.apply(who, Status.of(StatusType.SHIELD, skill.id, caster.getUUID(), amount, now(),
                ticks(secondsKey)));
        com.andrey.litrpg.fx.Spell.shield(level, who, skill.color);
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

    /** Враг — не игрок и не наш объект/помощник: по тотемам и двойникам свои навыки по площади не бьют. */
    public boolean isEnemy(LivingEntity e) {
        return e != caster && !(e instanceof Player) && e.isAlive() && !e.isSpectator()
                && !(e instanceof com.andrey.litrpg.skill.prop.ClassProp)
                && !(e instanceof com.andrey.litrpg.skill.prop.SummonEntity);
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

    // ------------------------------------------------------------------ метки-счётчики

    /** Добавить слой метки (горение, обморожение, статика, зараза) на {@code secondsKey}; вернуть, сколько стало. */
    public int addStack(LivingEntity e, StatusType type, String secondsKey, int max) {
        return StatusService.addStack(e, type, skill.id, caster.getUUID(), ticks(secondsKey), max);
    }

    public int stacks(LivingEntity e, StatusType type) {
        return StatusService.stacks(e, type);
    }

    // ------------------------------------------------------------------ ресурс класса

    /** Дать ресурс класса (стекло зерцала, пакт чернокнижника, сферы астроманта). */
    public void gain(double amount) {
        SkillService.add(caster, amount);
    }

    // ------------------------------------------------------------------ движение целей и своё

    /** Тянуть к точке: сила 1 — как рывок. Хранителей с места не сдвинуть. */
    public void pull(LivingEntity e, Vec3 to, double strength) {
        if (com.andrey.litrpg.combat.DamageService.isBoss(e)) return;
        Vec3 v = to.subtract(e.position());
        double d = v.horizontalDistance();
        if (d < 0.1) return;
        Vec3 dir = v.multiply(1, 0, 1).normalize().scale(Math.min(1.6, 0.25 + d * 0.08) * strength);
        e.setDeltaMovement(dir.x, Math.max(e.getDeltaMovement().y, 0.12), dir.z);
        e.hurtMarked = true;
    }

    /** Отбросить от точки. */
    public void push(LivingEntity e, Vec3 from, double strength) {
        if (com.andrey.litrpg.combat.DamageService.isBoss(e)) return;
        Vec3 v = e.position().subtract(from).multiply(1, 0, 1);
        Vec3 dir = (v.lengthSqr() < 0.01 ? caster.getViewVector(1).multiply(1, 0, 1) : v).normalize().scale(strength);
        e.setDeltaMovement(dir.x, 0.35, dir.z);
        e.hurtMarked = true;
    }

    /** Подбросить вверх. */
    public void toss(LivingEntity e, double up) {
        if (com.andrey.litrpg.combat.DamageService.isBoss(e)) return;
        e.setDeltaMovement(e.getDeltaMovement().x, up, e.getDeltaMovement().z);
        e.hurtMarked = true;
    }

    /** Перенести заклинателя (телепорт с эффектом). */
    public void teleport(Vec3 to) {
        Vec3 from = caster.position();
        caster.teleportTo(to.x, to.y, to.z);
        com.andrey.litrpg.fx.Fx.beam(level, from.add(0, 1, 0), to.add(0, 1, 0), 0.5f, 8, skill.color);
    }

    /** Точка за спиной цели (для телепорта в спину): на {@code dist} блоков позади её взгляда. */
    public Vec3 behind(LivingEntity target, double dist) {
        Vec3 back = target.getViewVector(1).multiply(1, 0, 1).normalize().scale(-dist);
        Vec3 at = target.position().add(back);
        return new Vec3(at.x, target.position().y, at.z);
    }

    /** Бьём ли со спины: угол между взглядом цели и направлением на нас больше 100°. */
    public boolean fromBehind(LivingEntity target) {
        Vec3 look = target.getViewVector(1).multiply(1, 0, 1).normalize();
        Vec3 to = caster.position().subtract(target.position()).multiply(1, 0, 1);
        return to.lengthSqr() > 0.01 && to.normalize().dot(look) < -0.17;
    }

    // ------------------------------------------------------------------ объекты класса и помощники

    /** Поставить объект класса: здоровье — доля единицы силы, радиус и срок — параметры навыка. */
    public com.andrey.litrpg.skill.prop.ClassProp prop(com.andrey.litrpg.skill.prop.PropKind kind, Vec3 at,
                                                       String hpKey, double radius, String secondsKey) {
        return com.andrey.litrpg.skill.prop.ClassProp.spawn(level,
                com.andrey.litrpg.skill.prop.ModProps.type(kind), at, caster,
                v(hpKey) * unit(), radius, ticks(secondsKey));
    }

    /** Свои объекты этого вида рядом (чтобы навык работал по уже поставленным). */
    public java.util.List<com.andrey.litrpg.skill.prop.ClassProp> myProps(com.andrey.litrpg.skill.prop.PropKind kind,
                                                                          double radius) {
        Vec3 c = caster.position();
        return level.getEntitiesOfClass(com.andrey.litrpg.skill.prop.ClassProp.class,
                new AABB(c, c).inflate(radius),
                p -> p.isAlive() && caster.getUUID().equals(p.ownerId()) && (kind == null || p.kind() == kind));
    }

    /** Призвать помощника: урон и здоровье считаются сейчас, от силы навыков хозяина. */
    public com.andrey.litrpg.skill.prop.SummonEntity summon(com.andrey.litrpg.skill.prop.SummonKind kind, Vec3 at,
                                                            String hpKey, String damageKey, String secondsKey) {
        return com.andrey.litrpg.skill.prop.SummonEntity.spawn(level,
                com.andrey.litrpg.skill.prop.ModProps.type(kind), at, caster,
                v(hpKey) * unit(), precomputed(damageKey), ticks(secondsKey));
    }

    /** Свои помощники рядом. */
    public java.util.List<com.andrey.litrpg.skill.prop.SummonEntity> mySummons(double radius) {
        Vec3 c = caster.position();
        return level.getEntitiesOfClass(com.andrey.litrpg.skill.prop.SummonEntity.class, new AABB(c, c).inflate(radius),
                s -> s.isAlive() && s.ownerPlayer() == caster);
    }

    // ------------------------------------------------------------------ угроза

    /** Добавить угрозы (навыки защиты и приманки). */
    public void threat(LivingEntity e, double raw) {
        if (e instanceof net.minecraft.world.entity.Mob m) {
            com.andrey.litrpg.combat.threat.ThreatService.add(m, caster, raw);
        }
    }

    /** Враги рядом теряют заклинателя из виду (невидимость, дым, тьма). */
    public void forgetMe(double radius) {
        com.andrey.litrpg.combat.threat.ThreatService.forget(caster, radius);
    }

    /** Перевести врагов на приманку — объект или помощника. */
    public void decoy(LivingEntity enemy, LivingEntity decoy, String secondsKey) {
        if (enemy instanceof net.minecraft.world.entity.Mob m) {
            com.andrey.litrpg.combat.threat.ThreatService.decoy(m, decoy, ticks(secondsKey));
        }
    }

    // ------------------------------------------------------------------ отложенные действия и звук

    /** Снять перезарядку этого навыка (добивание, которое обнуляет откат, если цель умерла). */
    public void readyNow() {
        readyNow(skill.id);
    }

    /** Снять перезарядку другого навыка класса (на «Изнанке» шаг в пустоту всегда готов). */
    public void readyNow(String skillId) {
        SkillService.state(caster).cooldowns.remove(skillId);
        SkillService.state(caster).dirty = true;
    }

    /** Невидимость заклинателя на {@code secondsKey}: враги рядом тоже теряют его из виду. */
    public void invisible(String secondsKey) {
        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.INVISIBILITY, ticks(secondsKey), 0, false, false, true));
        forgetMe(30);
    }

    /** Сделать через {@code ticks} тиков (отложенный удар, мина, приговор). */
    public void later(int ticks, Runnable action) {
        Later.run(level, ticks, action);
    }

    public void sound(net.minecraft.sounds.SoundEvent s, float volume, float pitch) {
        level.playSound(null, caster.blockPosition(), s, net.minecraft.sounds.SoundSource.PLAYERS, volume, pitch);
    }

    public void soundAt(Vec3 at, net.minecraft.sounds.SoundEvent s, float volume, float pitch) {
        level.playSound(null, net.minecraft.core.BlockPos.containing(at), s, net.minecraft.sounds.SoundSource.PLAYERS,
                volume, pitch);
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
