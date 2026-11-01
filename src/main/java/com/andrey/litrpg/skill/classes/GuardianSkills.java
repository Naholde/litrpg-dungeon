package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.prop.ClassProp;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Стражник — танк, который держит место. Забирает часть урона союзника на себя, врезается в строй врага,
 * отражает удары и ставит купол, в котором гаснут вражеские снаряды.
 */
public final class GuardianSkills {
    private GuardianSkills() {
    }

    /** Под прикрытие: доля урона по союзнику уходит стражнику; между ними светится связь. */
    public static final class Cover extends Skill {
        public Cover() {
            super("guardian.cover", "Под прикрытие", 0x7FB8FF, "⛓", 15, 0);
            blocks("range", 20);
            percent("share", 0.3);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Берёт союзника под защиту на {duration}: {share} урона по нему получаете вы "
                    + "(с вашей стойкостью и бронёй танка).";
        }

        @Override
        public void execute(SkillContext ctx) {
            if (ctx.target == ctx.caster) return;
            ctx.status(ctx.target, StatusType.GUARD, Math.min(0.9, ctx.v("share")), "duration");
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.18f, ctx.ticks("duration"), 0x7FB8FF);
            Fx.mark(ctx.level, ctx.target, "⛨", ctx.ticks("duration"), 0x7FB8FF);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
        }
    }

    /** Натиск: рывок к врагу, удар и оглушение. Стена на пути останавливает рывок. */
    public static final class Charge extends Skill {
        public Charge() {
            super("guardian.charge", "Натиск", 0xC9D6E8, "➤", 14, 0);
            blocks("range", 12);
            damage("damage", 1.0);
            seconds("stun", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Рывок к врагу до {range}: {damage} урона и оглушение на {stun}. Стена на пути останавливает рывок.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 from = ctx.caster.position();
            Vec3 to = ctx.target.position();
            Vec3 dir = to.subtract(from).multiply(1, 0, 1);
            double dist = dir.length();
            if (dist > 1.6) {
                Vec3 stop = from.add(dir.normalize().scale(dist - 1.3));
                HitResult wall = ctx.level.clip(new ClipContext(from.add(0, 0.5, 0), stop.add(0, 0.5, 0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster));
                if (wall.getType() != HitResult.Type.MISS) stop = wall.getLocation().subtract(dir.normalize().scale(0.6)).add(0, -0.5, 0);
                Fx.line(ctx.level, from, stop, 1.2f, 8, 0xC9D6E8, false);
                ctx.caster.teleportTo(stop.x, stop.y, stop.z);
            }
            if (ctx.caster.distanceTo(ctx.target) < 3) {
                ctx.hit(ctx.target, "damage");
                ctx.status(ctx.target, StatusType.STUN, 1, "stun");
                Fx.mark(ctx.level, ctx.target, "✦", ctx.ticks("stun"), 0xFFE27A);
            }
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.RAVAGER_STEP, SoundSource.PLAYERS, 1.2f, 1f);
        }
    }

    /** Упор: меньше урона и отражение ударов вблизи. */
    public static final class Brace extends Skill {
        public Brace() {
            super("guardian.brace", "Упор", 0xB0C4DE, "◈", 18, 0);
            seconds("duration", 4);
            percent("reduce", 0.3);
            percent("reflect", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Упирается щитом на {duration}: урон по вам меньше на {reduce}, а {reflect} урона от ударов вблизи "
                    + "возвращается бьющему.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            ctx.status(ctx.caster, StatusType.REFLECT, ctx.v("reflect"), "duration");
            Fx.discOn(ctx.level, ctx.caster, 1.4f, ctx.ticks("duration"), 0xB0C4DE, false);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    /** Купол: оберег в мире держит купол — союзники внутри получают меньше урона, вражеские снаряды гаснут. */
    public static final class Dome extends Skill {
        public Dome() {
            super("guardian.dome", "Купол", 0x8FE3FF, "◯", 40, 0);
            blocks("radius", 5);
            shield("hp", 4.0);
            seconds("duration", 6);
            percent("reduce", 0.375);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Ставит оберег с {hp} прочности, он держит купол радиусом {radius} на {duration}: союзники внутри "
                    + "получают на {reduce} меньше урона, вражеские снаряды гаснут, едва влетев внутрь. "
                    + "Разбили оберег — купол пал.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double r = ctx.v("radius");
            double factor = 1 - Math.min(0.9, ctx.v("reduce"));
            int dur = ctx.ticks("duration");
            ClassProp core = ctx.prop(PropKind.DOME, ctx.caster.position(), "hp", r, "duration");
            Fx.domeOn(ctx.level, core, (float) r, dur, 0x8FE3FF);
            core.onPulse(5, p -> {
                Vec3 c = p.position();
                for (LivingEntity a : ctx.alliesAround(c, r)) {
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_TAKEN, "guardian.dome", ctx.caster.getUUID(),
                            factor, p.level().getGameTime(), 8));
                }
                for (Projectile pr : p.level().getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r),
                        pr -> !(pr.getOwner() instanceof Player) && pr.position().distanceToSqr(c) <= r * r)) {
                    ((net.minecraft.server.level.ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD,
                            pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                    pr.discard();
                }
            });
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    /** Несокрушимый: вдвое меньше урона, и смертельный удар оставляет 1 здоровья. */
    public static final class Unbreakable extends Skill {
        public Unbreakable() {
            super("guardian.unbreakable", "Несокрушимый", 0xFFE27A, "✠", 96, 0);
            seconds("duration", 8);
            percent("reduce", 0.75);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Последний рубеж на {duration}: урон по вам меньше на {reduce}, и никакой удар не убьёт — "
                    + "в худшем случае останется 1 здоровья.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            ctx.status(ctx.caster, StatusType.WARD, 1, "duration");
            Fx.pillarOn(ctx.level, ctx.caster, 1.0f, 4, ctx.ticks("duration"), 0xFFE27A);
            Fx.domeOn(ctx.level, ctx.caster, 1.3f, ctx.ticks("duration"), 0xFFE27A);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }
}
