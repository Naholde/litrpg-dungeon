package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Стриж — дуэлянт в движении. Его сила — разгон: он копится от его же приёмов и быстро тает, если стоять.
 * На разгоне удары сильнее, а «Сквозняк» и «Последний взмах» тратят его целиком.
 */
public final class SwiftSkills {
    private SwiftSkills() {
    }

    /** Порыв: проскочить сквозь врага с ударом — и оказаться у него за спиной. */
    public static final class Rush extends Skill {
        public Rush() {
            super("swift.rush", "Порыв", 0x7FF0D0, "➤", 6, 0);
            blocks("range", 12);
            damage("damage", 1.1);
            percent("behind", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Проскакивает сквозь врага до {range}: {damage} урона, а со спины — на {behind} больше. "
                    + "Стриж оказывается позади цели и получает разгон.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 behind = ctx.behind(ctx.target, 1.6);
            boolean back = true;
            Fx.line(ctx.level, ctx.caster.position(), behind, 1.0f, 8, 0x7FF0D0, false);
            ctx.teleport(behind);
            ctx.caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    ctx.target.position().add(0, 1, 0));
            ctx.hitShare(ctx.target, ctx.v("damage") * (back ? 1 + ctx.v("behind") : 1));
            Fx.slash(ctx.level, ctx.caster, 2.2f, 80, 6, 0x7FF0D0);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        }
    }

    /** Веер клинков: три быстрых удара подряд. */
    public static final class Fan extends Skill {
        public Fan() {
            super("swift.fan", "Веер клинков", 0xB8F5E0, "⋔", 8, 0);
            blocks("radius", 4);
            param("angle", 100, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 0.6);
            count("hits", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "{hits} быстрых удара по всем перед собой в {radius}, каждый на {damage}. "
                    + "Каждый удар добавляет разгон.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int hits = (int) Math.round(ctx.v("hits"));
            for (int i = 0; i < hits; i++) {
                ctx.later(1 + i * 4, () -> {
                    for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) ctx.hit(e, "damage");
                    Fx.slash(ctx.level, ctx.caster, (float) ctx.v("radius"), (float) ctx.v("angle"), 5, 0xB8F5E0);
                    ctx.gain(4);
                });
            }
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.8f);
        }
    }

    /** Пируэт: полторы секунды все удары мимо, потом — рывок скорости. */
    public static final class Pirouette extends Skill {
        public Pirouette() {
            super("swift.pirouette", "Пируэт", 0xD6FFF5, "◐", 20, 0);
            seconds("duration", 1.5);
            seconds("speed_time", 4);
            percent("speed", 0.75);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration} ни один удар не попадает по стрижу, а после — {speed_time} он быстрее на {speed}. "
                    + "Уйти от разметки хранителя — ровно для этого.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.BLUR, 1.0, "duration");
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.caster, 1.3f, dur, 0xD6FFF5, false);
            ctx.later(dur, () -> {
                ctx.status(ctx.caster, StatusType.SPEED, ctx.v("speed"), "speed_time");
                Fx.mark(ctx.level, ctx.caster, "≫", ctx.ticks("speed_time"), 0xD6FFF5);
            });
            ctx.sound(SoundEvents.BREEZE_JUMP, 0.9f, 1.6f);
        }
    }

    /** Танец клинков: пока держится, урон растёт от разгона. */
    public static final class Dance extends Skill {
        public Dance() {
            super("swift.dance", "Танец клинков", 0x7FF0D0, "✸", 30, 0);
            seconds("duration", 8);
            percent("per_momentum", 0.004);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: за каждую единицу разгона урон стрижа выше на {per_momentum}. "
                    + "На полном разгоне — почти вдвое; стоять на месте во время танца нельзя.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double per = ctx.v("per_momentum");
            com.andrey.litrpg.skill.Zones.add(ctx.level, ctx.caster.position(), 1, ctx.caster.getUUID(), dur, 10, z -> {
                double mult = 1 + per * ctx.resource();
                com.andrey.litrpg.status.StatusService.apply(ctx.caster, com.andrey.litrpg.status.Status.of(
                        StatusType.DAMAGE_DEALT, "swift.dance", ctx.caster.getUUID(), mult, z.level.getGameTime(), 14));
            });
            Fx.discOn(ctx.level, ctx.caster, 1.2f, dur, 0x7FF0D0, false);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 2f);
        }
    }

    /** Сквозняк: за весь разгон — рывок по прямой сквозь всех. */
    public static final class Draft extends Skill {
        public Draft() {
            super("swift.draft", "Сквозняк", 0xA6FFE8, "⇶", 26, 0);
            blocks("length", 14);
            blocks("width", 2.5);
            count("cost", 20);
            param("per_momentum", 0.02, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Тратит весь разгон (нужно хотя бы {cost}): рывок на {length} по прямой — каждому на пути "
                    + "в полосе {width} по {per_momentum} за единицу разгона.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double momentum = ctx.v("cost") + ctx.spendAllResource();
            double share = ctx.v("per_momentum") * momentum;
            Vec3 from = ctx.caster.position();
            Vec3 dir = ctx.caster.getViewVector(1).multiply(1, 0, 1).normalize();
            Vec3 to = from.add(dir.scale(ctx.v("length")));
            var wall = ctx.level.clip(new net.minecraft.world.level.ClipContext(from.add(0, 0.6, 0), to.add(0, 0.6, 0),
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE, ctx.caster));
            if (wall.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                to = wall.getLocation().subtract(dir.scale(0.8)).add(0, -0.6, 0);
            }
            double width = ctx.v("width");
            Vec3 end = to;
            for (LivingEntity e : ctx.enemiesAround(from.add(end).scale(0.5), ctx.v("length"))) {
                double d = distanceToSegment(e.position(), from, end);
                if (d <= width) ctx.hitShare(e, share);
            }
            Fx.line(ctx.level, from, end, (float) width * 2, 10, 0xA6FFE8, false);
            ctx.teleport(end);
            ctx.sound(SoundEvents.BREEZE_JUMP, 1f, 1.4f);
        }

        private static double distanceToSegment(Vec3 p, Vec3 a, Vec3 b) {
            Vec3 ab = b.subtract(a).multiply(1, 0, 1);
            if (ab.lengthSqr() < 1e-6) return p.distanceTo(a);
            double t = Math.max(0, Math.min(1, p.subtract(a).multiply(1, 0, 1).dot(ab) / ab.lengthSqr()));
            return p.multiply(1, 0, 1).distanceTo(a.multiply(1, 0, 1).add(ab.scale(t)));
        }
    }

    /** Последний взмах: один удар, сила которого — весь накопленный разгон. */
    public static final class LastSwing extends Skill {
        public LastSwing() {
            super("swift.last_swing", "Последний взмах", 0x3AD8B0, "✹", 48, 0);
            blocks("range", 4);
            count("cost", 50);
            damage("damage", 2.2);
            param("per_momentum", 0.03, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "За {cost} разгона: {damage} урона плюс {per_momentum} за каждую единицу разгона, что осталась. "
                    + "Самый сильный удар стрижа — и он же обнуляет разгон.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double left = ctx.spendAllResource();
            ctx.hitShare(ctx.target, ctx.v("damage") + ctx.v("per_momentum") * left);
            Fx.slash(ctx.level, ctx.caster, 3f, 120, 10, 0x3AD8B0);
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 1.6f, 10, 0x3AD8B0);
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 1.2f);
        }
    }
}
