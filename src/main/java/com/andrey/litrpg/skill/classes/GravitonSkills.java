package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Гравитон — танк весом. Он не бьёт толпу по одному: воронка стягивает врагов в точку, пресс придавливает их
 * к земле, подброс роняет, инверсия сворачивает летящие снаряды в него самого, а сингулярность держит и давит
 * всё, что рядом, и в конце лопается.
 */
public final class GravitonSkills {
    private GravitonSkills() {
    }

    /** Воронка: врагов тянет к центру зоны. */
    public static final class Funnel extends Skill {
        public Funnel() {
            super("graviton.funnel", "Воронка", 0x9B8CFF, "◉", 14, 0);
            blocks("range", 14);
            blocks("radius", 6);
            seconds("duration", 3);
            damage("damage", 0.35);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Воронка радиусом {radius} на {duration}: врагов тянет к её центру и бьёт на {damage} каждую "
                    + "секунду. Собирает толпу в одну точку — чтобы бить её разом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 c = ctx.point;
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.disc(ctx.level, c, (float) r, dur, 0x9B8CFF, false);
            Fx.pillar(ctx.level, c, 0.6f, 3.5f, dur, 0x9B8CFF);
            Zones.add(ctx.level, c, r, ctx.caster.getUUID(), dur, 10, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.pull(e, z.center, 0.8);
                    ctx.threat(e, 2);
                    if ((z.level.getGameTime() / 10) % 2 == 0) ctx.hit(e, "damage");
                }
                Fx.ring(z.level, z.center, (float) z.radius, 10, 0x9B8CFF);
            });
            ctx.soundAt(c, SoundEvents.ENDERMAN_TELEPORT, 0.9f, 0.5f);
        }
    }

    /** Пресс: в зоне тяжело — нельзя прыгать, медленно и больнее. */
    public static final class Press extends Skill {
        public Press() {
            super("graviton.press", "Пресс", 0x7A6BD8, "⬓", 22, 0);
            blocks("range", 14);
            blocks("radius", 6);
            seconds("duration", 8);
            percent("extra", 0.25);
            percent("slow", 0.35);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Придавливает место радиусом {radius} на {duration}: враги внутри не прыгают, замедлены "
                    + "на {slow} и получают на {extra} больше урона.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 c = ctx.point;
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.8, ctx.v("slow"));
            double extra = 1 + ctx.v("extra");
            Fx.disc(ctx.level, c, (float) r, dur, 0x7A6BD8, false);
            Zones.add(ctx.level, c, r, ctx.caster.getUUID(), dur, 10, z -> {
                long now = z.level.getGameTime();
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    StatusService.apply(e, Status.of(StatusType.SLOW, "graviton.press", ctx.caster.getUUID(), slow, now, 14));
                    StatusService.apply(e, Status.of(StatusType.DAMAGE_TAKEN, "graviton.press", ctx.caster.getUUID(),
                            extra, now, 14));
                    // Прыгать нельзя: тяжесть прибивает к земле.
                    if (!e.onGround() && e.getDeltaMovement().y > 0) {
                        e.setDeltaMovement(e.getDeltaMovement().x, -0.4, e.getDeltaMovement().z);
                        e.hurtMarked = true;
                    }
                }
            });
            ctx.soundAt(c, SoundEvents.ANVIL_LAND, 0.7f, 0.5f);
        }
    }

    /** Подброс: врагов вокруг подбрасывает, и падение их ранит и оглушает. */
    public static final class Toss extends Skill {
        public Toss() {
            super("graviton.toss", "Подброс", 0xB8A6FF, "⤒", 18, 0);
            blocks("radius", 5);
            count("height", 1);
            damage("damage", 0.9);
            seconds("stun", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Подбрасывает всех врагов в {radius} и роняет: падение бьёт на {damage} и оглушает на {stun}. "
                    + "Хранителя не поднять — его тяжесть сильнее вашей.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var targets = ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"));
            for (LivingEntity e : targets) ctx.toss(e, 0.75 * ctx.v("height"));
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 10, 0xB8A6FF);
            ctx.later(22, () -> {
                for (LivingEntity e : targets) {
                    if (!e.isAlive()) continue;
                    ctx.hit(e, "damage");
                    ctx.status(e, StatusType.STUN, 1, "stun");
                    Fx.burst(ctx.level, e.position(), 1.0f, 8, 0xB8A6FF);
                }
            });
            ctx.sound(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.4f);
        }
    }

    /** Инверсия: снаряды, летящие в гравитона, сворачивают в него и гаснут. */
    public static final class Inversion extends Skill {
        public Inversion() {
            super("graviton.inversion", "Инверсия", 0x6B5CD8, "◎", 30, 0);
            blocks("radius", 7);
            seconds("duration", 5);
            shield("shield", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: вражеские снаряды в {radius} сворачивают в гравитона и гаснут, а каждый "
                    + "погашенный снаряд даёт щит на {shield}. Стрелки бьют в пустоту.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.pillar(ctx.level, ctx.caster.position(), (float) r, 3.5f, dur, 0x6B5CD8);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 2, z -> {
                Vec3 me = ctx.caster.position().add(0, 1, 0);
                for (Projectile pr : z.level.getEntitiesOfClass(Projectile.class, new AABB(me, me).inflate(r),
                        p -> !(p.getOwner() instanceof Player))) {
                    Vec3 to = me.subtract(pr.position());
                    if (to.length() > 1.2) {
                        pr.setDeltaMovement(to.normalize().scale(0.9));
                        pr.hasImpulse = true;
                        continue;
                    }
                    pr.discard();
                    ctx.shield(ctx.caster, "shield", "duration");
                    z.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.03);
                }
            });
            ctx.sound(SoundEvents.SHULKER_SHOOT, 0.8f, 0.6f);
        }
    }

    /**
     * Чёрная дыра — главный приём гравитона. Он встаёт на месте и держит дыру: всех врагов вокруг стягивает
     * в точку и держит оглушёнными, пока она горит. Гравитона сбили (оглушили, замолчали или убили) — дыра
     * гаснет досрочно; дожила до конца — схлопывается тяжёлым ударом.
     */
    public static final class BlackHole extends Skill {
        public BlackHole() {
            super("graviton.blackhole", "Чёрная дыра", 0x3A2A9B, "✹", 150, 0);
            blocks("range", 14);
            blocks("radius", 9);
            seconds("duration", 4);
            damage("tick", 0.55);
            damage("collapse", 3.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Держит дыру {duration}, стоя на месте: всех врагов в {radius} стягивает в её центр, держит "
                    + "оглушёнными и бьёт на {tick} два раза в секунду. В конце дыра схлопывается на {collapse}. "
                    + "Гравитона сбили — дыра гаснет, и схлопывания не будет.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 c = ctx.point.add(0, 1, 0);
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            // Гравитон держит дыру: он прикован к месту, пока она горит.
            StatusService.apply(ctx.caster, Status.of(StatusType.ROOT, "graviton.blackhole", ctx.caster.getUUID(), 1,
                    ctx.now(), dur));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x3A2A9B, false);
            Fx.pillar(ctx.level, ctx.point, 1.6f, 8f, dur, 0x3A2A9B);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1, 0), c, 0.3f, dur, 0x9B8CFF);
            boolean[] alive = {true};
            Zones.add(ctx.level, c, r, ctx.caster.getUUID(), dur, 5, z -> {
                long now = z.level.getGameTime();
                // Держать дыру можно, только пока гравитон в сознании.
                if (!ctx.caster.isAlive() || StatusService.has(ctx.caster, StatusType.STUN)
                        || StatusService.has(ctx.caster, StatusType.SILENCE)) {
                    if (alive[0]) {
                        alive[0] = false;
                        StatusService.removeFrom(ctx.caster, StatusType.ROOT, ctx.caster.getUUID());
                        Fx.burst(z.level, z.center, (float) r, 10, 0x9B8CFF);
                        ctx.soundAt(ctx.point, SoundEvents.BEACON_DEACTIVATE, 1f, 0.7f);
                    }
                    return;
                }
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.pull(e, z.center, 2.0);
                    ctx.threat(e, 4);
                    StatusService.apply(e, Status.of(StatusType.STUN, "graviton.blackhole", ctx.caster.getUUID(), 1,
                            now, 12));
                    if (now % 10 == 0) ctx.hit(e, "tick");
                }
                Fx.ring(z.level, z.center, (float) (r * 0.9), 6, 0x6B5CD8);
                Fx.burst(z.level, z.center, 2.0f, 6, 0x3A2A9B);
            });
            ctx.later(dur, () -> {
                if (!alive[0]) return;
                for (LivingEntity e : ctx.enemiesAround(c, r)) {
                    ctx.hit(e, "collapse");
                    ctx.toss(e, 0.5);
                }
                Fx.ring(ctx.level, ctx.point, (float) r, 16, 0xB8A6FF);
                Fx.burst(ctx.level, c, 4f, 14, 0x3A2A9B);
                ctx.soundAt(ctx.point, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.5f);
            });
            ctx.soundAt(ctx.point, SoundEvents.PORTAL_TRIGGER, 1f, 0.4f);
            ctx.sound(SoundEvents.WARDEN_SONIC_CHARGE, 0.9f, 0.6f);
        }
    }
}
