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

/**
 * Криомант — холод до хруста. Обморожение копится слоями, на пятом враг скован льдом, а «Раскол» по
 * скованному бьёт втрое и разлетается осколками. Вся игра класса — довести цель до заморозки и успеть
 * разбить её, пока лёд держит.
 */
public final class CryomancerSkills {
    private CryomancerSkills() {
    }

    /** Слой обморожения; на пятом — заморозка. */
    static void chill(SkillContext ctx, LivingEntity e, String secondsKey, int add) {
        int stacks = 0;
        for (int i = 0; i < add; i++) stacks = ctx.addStack(e, StatusType.CHILL, secondsKey, 5);
        StatusService.apply(e, Status.of(StatusType.SLOW, "cryo.chill", ctx.caster.getUUID(), 0.08 * stacks,
                ctx.now(), ctx.ticks(secondsKey)));
        Fx.mark(ctx.level, e, stacks >= 5 ? "❆" : "❅", 25, 0x8FE3FF);
        if (stacks >= 5) freeze(ctx, e);
    }

    /** Сковать льдом: не двигается, и «Раскол» по нему бьёт втрое. */
    static void freeze(SkillContext ctx, LivingEntity e) {
        StatusService.removeAll(e, StatusType.CHILL);
        StatusService.apply(e, Status.of(StatusType.FROZEN, "cryo.freeze", ctx.caster.getUUID(), 1, ctx.now(), 60));
        Fx.pillar(ctx.level, e.position(), 1.0f, 2.6f, 60, 0x8FE3FF);
        ctx.soundAt(e.position(), SoundEvents.GLASS_PLACE, 1.1f, 0.5f);
    }

    /** Ледяная игла: урон и слой обморожения. */
    public static final class IceNeedle extends Skill {
        public IceNeedle() {
            super("cryo.needle", "Ледяная игла", 0x8FE3FF, "❄", 3, 0);
            blocks("range", 22);
            damage("damage", 1.0);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона и слой обморожения на {duration}: каждый слой замедляет, а на пятом "
                    + "враг скован льдом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            chill(ctx, ctx.target, "duration", 1);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.3, 0), ctx.target.position().add(0, 1, 0), 0.1f, 6,
                    0x8FE3FF);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.9f, 1.8f);
        }
    }

    /** Вьюга: зона, которая морозит всех внутри. */
    public static final class Blizzard extends Skill {
        public Blizzard() {
            super("cryo.blizzard", "Вьюга", 0xB0E8FF, "✺", 24, 0);
            blocks("range", 16);
            blocks("radius", 6);
            seconds("duration", 8);
            damage("damage", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Вьюга в {radius} на {duration}: каждую секунду {damage} урона и слой обморожения всем внутри. "
                    + "Кто задержался — скован.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0xB0E8FF, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.hit(e, "damage");
                    chill(ctx, e, "duration", 1);
                }
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0xB0E8FF);
            });
            ctx.soundAt(ctx.point, SoundEvents.POWDER_SNOW_PLACE, 1.2f, 0.9f);
        }
    }

    /** Заморозка: добавляет сразу два слоя, а по готовой цели — сковывает. */
    public static final class Freeze extends Skill {
        public Freeze() {
            super("cryo.freeze", "Заморозка", 0xD6F3FF, "❆", 14, 0);
            blocks("range", 20);
            damage("damage", 0.8);
            seconds("duration", 10);
            count("stacks", 2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона и сразу {stacks} слоя обморожения. Если слоёв стало пять — цель скована "
                    + "льдом на три секунды.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            chill(ctx, ctx.target, "duration", (int) Math.round(ctx.v("stacks")));
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_HURT_FREEZE, 1f, 1.2f);
        }
    }

    /** Раскол: по скованному — страшный удар и осколки по соседям. */
    public static final class Shatter extends Skill {
        public Shatter() {
            super("cryo.shatter", "Раскол", 0x7FB8FF, "✹", 16, 0);
            blocks("range", 20);
            blocks("radius", 4);
            damage("damage", 1.6);
            percent("frozen", 2.0);
            damage("shards", 0.8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона, а по скованному врагу — на {frozen} больше, лёд разлетается и бьёт всех "
                    + "в {radius} на {shards}. Главный приём криоманта.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean frozen = StatusService.has(ctx.target, StatusType.FROZEN);
            ctx.hitShare(ctx.target, ctx.v("damage") * (frozen ? 1 + ctx.v("frozen") : 1));
            if (frozen) {
                StatusService.removeAll(ctx.target, StatusType.FROZEN);
                for (LivingEntity e : ctx.enemiesAround(ctx.target.position(), ctx.v("radius"))) {
                    if (e != ctx.target) ctx.hit(e, "shards");
                }
                Fx.ring(ctx.level, ctx.target.position(), (float) ctx.v("radius"), 12, 0x7FB8FF);
            }
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 1.8f, 10, 0x7FB8FF);
            Fx.shards(ctx.level, ctx.target.position().add(0, 1, 0), 3.4f, 18, 14, 0x8FE3FF);
            ctx.soundAt(ctx.target.position(), SoundEvents.GLASS_BREAK, 1.3f, 0.6f);
        }
    }

    /** Ледяные шипы: полоса шипов из земли. */
    public static final class IceSpikes extends Skill {
        public IceSpikes() {
            super("cryo.spikes", "Ледяные шипы", 0xA6E1FF, "⁂", 26, 0);
            blocks("length", 12);
            blocks("width", 2);
            damage("damage", 1.5);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Шипы вырастают полосой на {length}: {damage} урона и слой обморожения всем на линии.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var from = ctx.caster.position();
            var dir = ctx.caster.getViewVector(1).multiply(1, 0, 1).normalize();
            var to = from.add(dir.scale(ctx.v("length")));
            double width = ctx.v("width");
            for (LivingEntity e : ctx.enemiesAround(from.add(to).scale(0.5), ctx.v("length"))) {
                var ab = to.subtract(from).multiply(1, 0, 1);
                double t = Math.max(0, Math.min(1, e.position().subtract(from).multiply(1, 0, 1).dot(ab) / ab.lengthSqr()));
                if (e.position().multiply(1, 0, 1).distanceTo(from.multiply(1, 0, 1).add(ab.scale(t))) <= width) {
                    ctx.hit(e, "damage");
                    chill(ctx, e, "duration", 1);
                    Fx.pillar(ctx.level, e.position(), 0.5f, 2f, 16, 0xA6E1FF);
                }
            }
            Fx.line(ctx.level, from, to, (float) width * 2, 12, 0xA6E1FF, false);
            ctx.sound(SoundEvents.GLASS_PLACE, 1.1f, 0.7f);
        }
    }

    /** Абсолютный ноль: всё в радиусе сковано разом. */
    public static final class AbsoluteZero extends Skill {
        public AbsoluteZero() {
            super("cryo.zero", "Абсолютный ноль", 0xFFFFFF, "⬣", 120, 0);
            blocks("radius", 9);
            damage("damage", 1.8);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Всё в {radius} замерзает: {damage} урона и {duration} все враги скованы льдом. "
                    + "Дальше — «Раскол» по самому опасному.";
        }

        @Override
        public void execute(SkillContext ctx) {
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) {
                ctx.hit(e, "damage");
                StatusService.apply(e, Status.of(StatusType.FROZEN, "cryo.zero", ctx.caster.getUUID(), 1, ctx.now(),
                        ctx.ticks("duration")));
                Fx.pillar(ctx.level, e.position(), 0.9f, 2.6f, ctx.ticks("duration"), 0xFFFFFF);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 16, 0xFFFFFF);
            Fx.wave(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 2.6f, 18, 0xD6F3FF);
            Fx.disc(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 20, 0xD6F3FF, false);
            ctx.sound(SoundEvents.PLAYER_HURT_FREEZE, 1.2f, 0.5f);
        }
    }
}
