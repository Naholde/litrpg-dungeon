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
import net.minecraft.world.phys.Vec3;

/**
 * Пиромант — огонь слоями. Каждый его приём кладёт на врага слой жара (до пяти), «Взрыв» детонирует их все,
 * а «Пекло» бьёт тем сильнее, чем больше врагов горит. Его собственный жар копится от каждого заклинания:
 * на максимуме пиромант горит сам, но бьёт заметно больнее.
 */
public final class PyromancerSkills {
    private PyromancerSkills() {
    }

    /** Положить слой горения и подогреть самого пироманта. */
    static void ignite(SkillContext ctx, LivingEntity e, String secondsKey, String dotKey) {
        int stacks = ctx.addStack(e, StatusType.BURN, secondsKey, 5);
        StatusService.apply(e, Status.periodic(StatusType.DOT, "pyro.burn", ctx.caster.getUUID(),
                ctx.precomputed(dotKey) / 4, ctx.now(), 80, 20));
        Fx.mark(ctx.level, e, stacks >= 5 ? "✹" : "♨", 30, 0xFF7A2E);
    }

    /** Искра: слой жара на враге. */
    public static final class Spark extends Skill {
        public Spark() {
            super("pyro.spark", "Искра", 0xFF7A2E, "♨", 3, 0);
            blocks("range", 22);
            damage("damage", 0.9);
            damage("burn", 0.8);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона и слой жара: цель горит на {burn} за {duration}. Слоёв бывает до пяти — "
                    + "их детонирует «Взрыв».";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            ignite(ctx, ctx.target, "duration", "burn");
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.3, 0), ctx.target.position().add(0, 1, 0), 0.12f, 6,
                    0xFF7A2E);
            ctx.sound(SoundEvents.FIRECHARGE_USE, 0.8f, 1.4f);
        }
    }

    /** Горящая земля: полоса огня под ногами. */
    public static final class BurningGround extends Skill {
        public BurningGround() {
            super("pyro.ground", "Горящая земля", 0xE07A3A, "≋", 16, 0);
            blocks("range", 16);
            blocks("radius", 4);
            seconds("duration", 8);
            damage("damage", 0.35);
            damage("burn", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Земля горит в {radius} на {duration}: каждую секунду {damage} урона всем внутри "
                    + "и слой жара на {burn}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0xE07A3A, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.hit(e, "damage");
                    ignite(ctx, e, "duration", "burn");
                }
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0xE07A3A);
            });
            ctx.soundAt(ctx.point, SoundEvents.FIRE_AMBIENT, 1.2f, 0.8f);
        }
    }

    /** Взрыв: детонировать все слои жара на цели. */
    public static final class Detonate extends Skill {
        public Detonate() {
            super("pyro.detonate", "Взрыв", 0xFF4A2E, "✺", 9, 0);
            blocks("range", 22);
            blocks("radius", 3.5);
            param("per_stack", 0.7, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Детонирует жар на цели: {per_stack} урона за каждый слой, по всем в {radius} от неё. "
                    + "Слои при этом сгорают — копите их заново.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int stacks = ctx.stacks(ctx.target, StatusType.BURN);
            if (stacks <= 0) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Цель не горит — сначала положите жар «Искрой».");
                return;
            }
            double share = ctx.v("per_stack") * stacks;
            Vec3 at = ctx.target.position();
            for (LivingEntity e : ctx.enemiesAround(at, ctx.v("radius"))) ctx.hitShare(e, share);
            StatusService.removeAll(ctx.target, StatusType.BURN);
            Fx.ring(ctx.level, at, (float) ctx.v("radius"), 12, 0xFF4A2E);
            Fx.burst(ctx.level, at.add(0, 1, 0), 2f, 10, 0xFF4A2E);
            ctx.soundAt(at, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.2f);
        }
    }

    /** Выдох пламени: конус огня, слой жара каждому. */
    public static final class FireBreath extends Skill {
        public FireBreath() {
            super("pyro.breath", "Выдох пламени", 0xFF9A4A, "⌁", 14, 0);
            blocks("radius", 8);
            param("angle", 70, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 1.3);
            damage("burn", 0.6);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Струя огня в секторе {radius}: {damage} урона и слой жара каждому.";
        }

        @Override
        public void execute(SkillContext ctx) {
            for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) {
                ctx.hit(e, "damage");
                ignite(ctx, e, "duration", "burn");
            }
            Fx.cone(ctx.level, ctx.caster.position(), ctx.caster.getViewVector(1).multiply(1, 0, 1),
                    (float) ctx.v("radius"), (float) ctx.v("angle"), 10, 0xFF9A4A, true);
            ctx.sound(SoundEvents.BLAZE_SHOOT, 1f, 0.8f);
        }
    }

    /** Огненный шторм: метеоры с разметкой. */
    public static final class Firestorm extends Skill {
        public Firestorm() {
            super("pyro.firestorm", "Огненный шторм", 0xFF6A2A, "✸", 55, 0);
            blocks("range", 20);
            blocks("radius", 8);
            blocks("meteor", 3);
            count("meteors", 5);
            damage("damage", 1.5);
            seconds("delay", 1.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "{meteors} метеоров падают по площади {radius}: каждый через {delay} после разметки бьёт "
                    + "на {damage} в {meteor} блоках.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("meteors"));
            int delay = ctx.ticks("delay");
            double spread = ctx.v("radius"), mr = ctx.v("meteor");
            for (int i = 0; i < n; i++) {
                double a = ctx.level.random.nextDouble() * Math.PI * 2;
                double d = ctx.level.random.nextDouble() * spread;
                Vec3 at = ctx.point.add(Math.cos(a) * d, 0, Math.sin(a) * d);
                int when = delay + i * 6;
                Fx.disc(ctx.level, at, (float) mr, when, 0xFF6A2A, true);
                ctx.later(when, () -> {
                    for (LivingEntity e : ctx.enemiesAround(at, mr)) ctx.hit(e, "damage");
                    Fx.pillar(ctx.level, at, (float) mr * 0.5f, 5f, 8, 0xFF6A2A);
                    Fx.burst(ctx.level, at.add(0, 0.6, 0), (float) mr, 8, 0xFF6A2A);
                    Fx.wave(ctx.level, at, (float) mr * 1.3f, 1.4f, 12, 0xFF6A2A);
                    ctx.soundAt(at, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.8f);
                });
            }
            ctx.soundAt(ctx.point, SoundEvents.FIRECHARGE_USE, 1.2f, 0.5f);
        }
    }

    /** Пекло: тем сильнее, чем больше врагов уже горит; на полном жаре пиромант горит сам. */
    public static final class Inferno extends Skill {
        public Inferno() {
            super("pyro.inferno", "Пекло", 0xFFD27A, "✹", 88, 0);
            blocks("radius", 10);
            damage("damage", 2.0);
            percent("per_burning", 0.35);
            percent("self_burn", 0.1);
            seconds("duration", 6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Всем врагам в {radius} — {damage} урона, и на {per_burning} больше за каждого, кто горит. "
                    + "На полном жаре пиромант вспыхивает сам: {duration} он теряет {self_burn} здоровья, "
                    + "но пекло бьёт в полтора раза сильнее.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var foes = ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"));
            long burning = foes.stream().filter(e -> StatusService.has(e, StatusType.BURN)).count();
            boolean full = ctx.resource() >= 95;
            double share = ctx.v("damage") * (1 + ctx.v("per_burning") * burning) * (full ? 1.5 : 1);
            for (LivingEntity e : foes) {
                ctx.hitShare(e, share);
                ignite(ctx, e, "duration", "damage");
            }
            if (full) {
                double tick = ctx.caster.getMaxHealth() * ctx.v("self_burn") / 6;
                for (int i = 0; i < 6; i++) {
                    ctx.later(20 + i * 20, () -> {
                        if (ctx.caster.isAlive()) ctx.caster.setHealth((float) Math.max(1, ctx.caster.getHealth() - tick));
                    });
                }
                Fx.pillarOn(ctx.level, ctx.caster, 1.2f, 4f, ctx.ticks("duration"), 0xFF4A2E);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 16, 0xFFD27A);
            Fx.wave(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 2.6f, 18, 0xFF4A2E);
            ctx.sound(SoundEvents.BLAZE_DEATH, 1f, 0.7f);
        }
    }
}
