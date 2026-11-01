package com.andrey.litrpg.skill;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.SkillsBalance;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.SkillSyncPayload;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerData;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Применение навыков на сервере: проверки → цель → (чтение) → цена → перезарядка → механика навыка.
 * Здесь же рывок, ресурс класса и отправка состояния панели клиенту.
 */
public final class SkillService {
    /** Конус «доводки» прицела: промах — ближайший к линии взгляда враг в 14°. */
    private static final double AIM_CONE_COS = Math.cos(Math.toRadians(14));

    private SkillService() {
    }

    public static SkillState state(ServerPlayer p) {
        return p.getData(ModAttachments.SKILL_STATE);
    }

    public static List<Skill> loadout(PlayerData d) {
        return ClassRegistry.loadout(d.role(), d.classId(), d.level());
    }

    public static Resource resourceOf(PlayerData d) {
        PlayerClass c = d.playerClass();
        return c != null ? c.resource() : Resource.NONE;
    }

    // ------------------------------------------------------------------ применение

    /** Нажата ячейка панели. Возвращает, сработал ли (или начал читаться) навык. */
    public static boolean use(ServerPlayer p, int slot) {
        PlayerData d = PlayerService.data(p);
        List<Skill> loadout = loadout(d);
        if (slot < 0 || slot >= loadout.size() || !p.isAlive() || p.isSpectator()) return false;
        Skill skill = loadout.get(slot);
        SkillState st = state(p);
        long now = p.level().getGameTime();
        if (skill.passive()) {
            long left = Math.max(0, st.readyAt(skill.id) - now);
            return fail(p, skill.name + ": пассивный навык" + (left > 0 ? " · готов через " + secs(left) : " · наготове"));
        }
        if (StatusService.has(p, StatusType.STUN)) return fail(p, "Вы оглушены");
        if (StatusService.has(p, StatusType.SILENCE)) return fail(p, "Немота — навыки недоступны");
        if (st.casting != null) {
            if (st.casting.skill() == skill) return cancelCast(p, "Чтение отменено");
            return fail(p, "Идёт чтение");
        }
        if (now < st.readyAt(skill.id)) return fail(p, skill.name + ": ещё " + secs(st.readyAt(skill.id) - now));
        if (now < st.gcdReady) return false; // общая перезарядка — молча
        int rank = d.rank(skill.id);
        double cost = skill.cost(rank);
        if (cost > 0 && st.resource + 1e-6 < cost) {
            return fail(p, "Мало: " + resourceOf(d).title.toLowerCase() + " " + Math.round(st.resource) + " из " + Math.round(cost));
        }
        LivingEntity target = null;
        Vec3 point = p.position();
        switch (skill.targeting()) {
            case ENEMY -> {
                target = enemyTarget(p, skill.range(rank));
                if (target == null) return fail(p, "Нет цели");
            }
            case ALLY -> target = HealService.resolveTarget(p, skill.range(rank));
            case GROUND -> point = groundTarget(p, skill.range(rank));
            case SELF, DIRECTION -> {
            }
        }
        SkillsBalance.Common c = Balance.get(Balance.SKILLS).common();
        st.gcdTotal = Math.round(c.gcdSeconds() * 20);
        st.gcdReady = now + st.gcdTotal;
        int cast = skill.ticks("cast", rank);
        if (cast > 0) {
            st.casting = new SkillState.Casting(skill, slot, target, point, now, now + cast, p.position());
            st.dirty = true;
            sync(p);
            return true;
        }
        finish(p, skill, rank, target, point);
        return true;
    }

    /** Навык срабатывает: цена, перезарядка, механика. */
    private static void finish(ServerPlayer p, Skill skill, int rank, LivingEntity target, Vec3 point) {
        SkillState st = state(p);
        long now = p.level().getGameTime();
        st.resource = Math.max(0, st.resource - skill.cost(rank));
        double haste = PlayerService.breakdown(p).get(Derived.HASTE) + StatusService.sum(p, StatusType.HASTE);
        int cd = SkillMath.cooldownTicks(skill.value("cooldown", rank), haste);
        st.cooldowns.put(skill.id, new long[]{now + cd, cd});
        st.dirty = true;
        StyleTracker.add(p, Style.SKILLS, 1);
        if (skill.cost(rank) <= 0) gain(p, Resource.Gain.CAST);
        skill.execute(new SkillContext(p, skill, rank, target, point));
        sync(p);
    }

    private static boolean cancelCast(ServerPlayer p, String why) {
        state(p).casting = null;
        state(p).dirty = true;
        fail(p, why);
        sync(p);
        return false;
    }

    private static boolean fail(ServerPlayer p, String why) {
        p.displayClientMessage(Component.literal(why).withStyle(ChatFormatting.RED), true);
        return false;
    }

    private static String secs(long ticks) {
        return Derived.trim(Math.ceil(ticks / 2.0) / 10.0) + " с";
    }

    // ------------------------------------------------------------------ выбор цели

    /** Враг под прицелом; промах — ближайший к линии взгляда в конусе 14°, которого видно. */
    public static LivingEntity enemyTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1);
        Vec3 end = eye.add(look.scale(range));
        HitResult block = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 stop = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, eye, stop, p.getBoundingBox().expandTowards(stop.subtract(eye)).inflate(1),
                e -> e instanceof LivingEntity l && isEnemy(p, l), eye.distanceToSqr(stop));
        if (hit != null) return (LivingEntity) hit.getEntity();
        LivingEntity best = null;
        double bestCos = AIM_CONE_COS;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye).inflate(range),
                l -> isEnemy(p, l))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range) continue;
            double cos = to.normalize().dot(look);
            if (cos > bestCos && p.hasLineOfSight(e)) {
                bestCos = cos;
                best = e;
            }
        }
        return best;
    }

    private static boolean isEnemy(ServerPlayer p, LivingEntity e) {
        return e != p && !(e instanceof Player) && e.isAlive() && !e.isSpectator();
    }

    /** Точка на земле под прицелом; если смотрит в небо — на пределе дальности, опущенная на землю. */
    public static Vec3 groundTarget(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getViewVector(1).scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 at = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
        BlockHitResult down = p.level().clip(new ClipContext(at.add(0, 0.5, 0), at.add(0, -12, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return down.getType() == HitResult.Type.MISS ? at : down.getLocation();
    }

    // ------------------------------------------------------------------ рывок

    /** Рывок (Alt): в сторону движения (dx, dz — направление с клиента), есть у всех. */
    public static boolean dash(ServerPlayer p, double dx, double dz) {
        SkillState st = state(p);
        long now = p.level().getGameTime();
        if (!p.isAlive() || p.isSpectator() || now < st.dashReady) return false;
        if (StatusService.has(p, StatusType.STUN) || StatusService.has(p, StatusType.ROOT)) return fail(p, "Нельзя сдвинуться");
        Vec3 dir = new Vec3(dx, 0, dz);
        if (dir.lengthSqr() < 1e-4) dir = p.getViewVector(1).multiply(1, 0, 1);
        dir = dir.normalize();
        SkillsBalance.Common c = Balance.get(Balance.SKILLS).common();
        double s = c.dashStrength();
        p.setDeltaMovement(dir.x * s, Math.max(0.15, p.getDeltaMovement().y), dir.z * s);
        p.hurtMarked = true;
        st.dashTotal = Math.round(c.dashCooldown() * 20);
        st.dashReady = now + st.dashTotal;
        st.dirty = true;
        if (st.casting != null) cancelCast(p, "Чтение прервано рывком");
        Fx.line(p.serverLevel(), p.position(), p.position().add(dir.scale(4)), 0.8f, 8, 0xA6E1FF, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.4f);
        sync(p);
        return true;
    }

    // ------------------------------------------------------------------ тик, ресурс

    public static void tick(ServerPlayer p) {
        SkillState st = state(p);
        long now = p.level().getGameTime();
        if (st.casting != null) tickCast(p, st, now);
        if (st.onLanding != null && now - st.landingSince > 4 && (p.onGround() || now - st.landingSince > 60)) {
            Runnable r = st.onLanding;
            st.onLanding = null;
            r.run();
        }
        tickResource(p, st, now);
        if (now % 10 == 0) tickPassives(p, st, now);
        if (st.dirty || (now % 10 == 0 && anyTimer(st, now))) sync(p);
    }

    private static void tickCast(ServerPlayer p, SkillState st, long now) {
        SkillState.Casting c = st.casting;
        boolean moved = p.position().distanceToSqr(c.startPos()) > 0.04 || (!p.onGround() && p.getDeltaMovement().y > 0.1);
        if (c.skill().interruptible()) {
            if (moved) {
                cancelCast(p, "Чтение прервано: нужно стоять на месте");
                return;
            }
            if (StatusService.has(p, StatusType.STUN) || StatusService.has(p, StatusType.SILENCE)) {
                cancelCast(p, "Чтение сбито");
                return;
            }
        }
        if (now < c.end()) return;
        st.casting = null;
        int rank = PlayerService.data(p).rank(c.skill().id);
        LivingEntity target = c.target();
        if (target != null && (!target.isAlive() || target.distanceTo(p) > c.skill().range(rank) * 1.25)) {
            fail(p, "Цель потеряна");
            st.dirty = true;
            return;
        }
        if (c.skill().cost(rank) > st.resource + 1e-6) {
            fail(p, "Не хватило ресурса");
            st.dirty = true;
            return;
        }
        finish(p, c.skill(), rank, target, c.point());
    }

    /**
     * Пассивные навыки класса: раз в полсекунды каждому даётся шанс сработать. Сработал — уходит
     * в перезарядку, как обычный навык (с ускорением от Интеллекта и меток).
     */
    private static void tickPassives(ServerPlayer p, SkillState st, long now) {
        PlayerData d = PlayerService.data(p);
        for (Skill s : loadout(d)) {
            if (!s.passive() || now < st.readyAt(s.id)) continue;
            int rank = d.rank(s.id);
            if (!s.passiveTick(new SkillContext(p, s, rank, null, p.position()))) continue;
            double haste = PlayerService.breakdown(p).get(Derived.HASTE) + StatusService.sum(p, StatusType.HASTE);
            int cd = SkillMath.cooldownTicks(s.value("cooldown", rank), haste);
            st.cooldowns.put(s.id, new long[]{now + cd, cd});
            st.dirty = true;
        }
    }

    /** Сам копящийся ресурс (песок, дыхание) и таяние тех, что тают через 5 с без пополнения. */
    private static void tickResource(ServerPlayer p, SkillState st, long now) {
        Resource r = resourceOf(PlayerService.data(p));
        if (r == Resource.NONE) {
            if (st.resource != 0) {
                st.resource = 0;
                st.dirty = true;
            }
            return;
        }
        if (now % 20 != 0) return;
        if (r.gain == Resource.Gain.REGEN && st.resource < r.max) {
            // Дыхание снайпера копится только пока стоишь на месте — в этом его идея.
            boolean still = r != Resource.BREATH || p.getDeltaMovement().horizontalDistanceSqr() < 1e-4;
            if (still) {
                st.resource = Math.min(r.max, st.resource + r.amount);
                st.dirty = true;
            }
        }
        if (r.decay > 0 && st.resource > 0 && now - st.lastResourceGain > 100) {
            st.resource = Math.max(0, st.resource - r.decay);
            st.dirty = true;
        }
    }

    /** Ресурс от нанесённого удара (ярость, заряд, сумерки). */
    public static void onDealt(ServerPlayer p) {
        gain(p, Resource.Gain.DEALT);
    }

    /** Ресурс от полученного удара (ярость, кровь). */
    public static void onTaken(ServerPlayer p) {
        gain(p, Resource.Gain.TAKEN);
    }

    /** Ресурс за убитого врага (души, тень). */
    public static void onKill(ServerPlayer p) {
        gain(p, Resource.Gain.KILL);
    }

    /** Пополнить ресурс, если класс копит его этим способом. */
    private static void gain(ServerPlayer p, Resource.Gain how) {
        Resource r = resourceOf(PlayerService.data(p));
        if (r.gain != how && !(r.gain == Resource.Gain.FIGHT && (how == Resource.Gain.DEALT || how == Resource.Gain.TAKEN))) {
            return;
        }
        add(p, how == Resource.Gain.TAKEN && r.gain == Resource.Gain.FIGHT ? r.amount * 0.7 : r.amount);
    }

    /** Пополнить ресурс, только если у класса именно этот ресурс (стекло зерцала — от полученного урона). */
    public static void addIf(ServerPlayer p, Resource which, double amount) {
        if (resourceOf(PlayerService.data(p)) == which) add(p, amount);
    }

    /** Добавить ресурс напрямую (навыки класса: стекло зерцала, пакт чернокнижника, сферы астроманта). */
    public static void add(ServerPlayer p, double amount) {
        Resource r = resourceOf(PlayerService.data(p));
        if (r == Resource.NONE || amount == 0) return;
        SkillState st = state(p);
        st.resource = Math.max(0, Math.min(r.max, st.resource + amount));
        st.lastResourceGain = p.level().getGameTime();
        st.dirty = true;
    }

    private static boolean anyTimer(SkillState st, long now) {
        if (st.casting != null || now < st.gcdReady || now < st.dashReady || st.resource > 0) return true;
        for (long[] c : st.cooldowns.values()) if (now < c[0]) return true;
        return false;
    }

    /** Сбросить перезарядки (смена класса, вайп). */
    public static void reset(ServerPlayer p) {
        SkillState st = state(p);
        st.cooldowns.clear();
        st.casting = null;
        st.resource = 0;
        st.onLanding = null;
        st.dirty = true;
        sync(p);
    }

    public static void sync(ServerPlayer p) {
        SkillState st = state(p);
        st.dirty = false;
        long now = p.level().getGameTime();
        PlayerData d = PlayerService.data(p);
        List<SkillSyncPayload.Slot> slots = new ArrayList<>();
        for (Skill s : loadout(d)) {
            slots.add(new SkillSyncPayload.Slot(s.id, (int) Math.max(0, st.readyAt(s.id) - now), (int) st.totalOf(s.id)));
        }
        SkillState.Casting c = st.casting;
        Net.send(p, new SkillSyncPayload(slots,
                (int) Math.max(0, st.gcdReady - now), (int) st.gcdTotal,
                (int) Math.max(0, st.dashReady - now), (int) st.dashTotal,
                (float) st.resource,
                c != null ? c.skill().id : "", c != null ? (int) (c.end() - c.start()) : 0,
                c != null ? (int) Math.max(0, c.end() - now) : 0));
    }
}
