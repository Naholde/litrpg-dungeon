package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.History;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Снайпер — боец одного выстрела. Его ресурс «дыхание» копится, только пока он стоит на месте, и чем дольше
 * он стоял, тем больнее бьёт «Прицелиться». Метка делает цель удобной для всей группы, а «Контрольный»
 * тратит метку и бьёт так, что мало кто встаёт.
 */
public final class SniperSkills {
    private SniperSkills() {
    }

    /** Прицелиться: урон тем выше, чем дольше снайпер стоял на месте. */
    public static final class Aim extends Skill {
        public Aim() {
            super("sniper.aim", "Прицелиться", 0xC9D6E8, "◎", 6, 0);
            blocks("range", 40);
            damage("damage", 1.4);
            seconds("full", 3);
            percent("bonus", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Выстрел на {damage}. Если снайпер стоял на месте, урон растёт — за {full} неподвижности "
                    + "до +{bonus}. Сдвинулся — считай заново.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double still = Math.min(ctx.v("full"), History.stillSeconds(ctx.caster));
            double mult = 1 + ctx.v("bonus") * (still / Math.max(0.1, ctx.v("full")));
            ctx.hitShare(ctx.target, ctx.v("damage") * mult);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.4, 0), ctx.target.position().add(0, 1, 0),
                    (float) (0.08 + 0.12 * (still / ctx.v("full"))), 8, 0xC9D6E8);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1f, 0.8f);
        }
    }

    /** Задержать дыхание: за дыхание — следующие выстрелы сильнее. */
    public static final class HoldBreath extends Skill {
        public HoldBreath() {
            super("sniper.breath", "Задержать дыхание", 0xE8F0FF, "◌", 24, 0);
            count("cost", 40);
            seconds("duration", 4);
            percent("power", 0.8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} дыхания: {duration} все выстрелы снайпера сильнее на {power}. "
                    + "Дыхание копится, только пока он стоит.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_DEALT, 1 + ctx.v("power"), "duration");
            Fx.mark(ctx.level, ctx.caster, "◌", ctx.ticks("duration"), 0xE8F0FF);
            ctx.sound(SoundEvents.PLAYER_BREATH, 0.8f, 1.2f);
        }
    }

    /** Метка: цель удобна всей группе, а снайперу — обязательна для «Контрольного». */
    public static final class Mark extends Skill {
        public Mark() {
            super("sniper.mark", "Метка", 0xFFD27A, "⌖", 12, 0);
            blocks("range", 45);
            seconds("duration", 20);
            percent("extra", 0.12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Метка на {duration}: весь отряд бьёт цель на {extra} больнее. По отмеченному работает "
                    + "«Контрольный».";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.target, StatusType.MARK, 0, "duration");
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            Fx.mark(ctx.level, ctx.target, "⌖", ctx.ticks("duration"), 0xFFD27A);
            ctx.soundAt(ctx.target.position(), SoundEvents.NOTE_BLOCK_PLING.value(), 1f, 1.8f);
        }
    }

    /** Капкан: ловушка на земле держит того, кто в неё вошёл. */
    public static final class Trap extends Skill {
        public Trap() {
            super("sniper.trap", "Капкан", 0xC9D6E8, "⊻", 26, 0);
            blocks("range", 12);
            blocks("radius", 1.8);
            shield("hp", 1.5);
            seconds("duration", 30);
            damage("damage", 1.2);
            seconds("hold", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Капкан стоит до {duration} и ждёт. Вошёл враг — {damage} урона и {hold} на месте. "
                    + "Срабатывает один раз.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var trap = ctx.prop(PropKind.TRAP, ctx.point, "hp", ctx.v("radius"), "duration");
            trap.onPulse(5, p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                    ctx.hit(e, "damage");
                    StatusService.apply(e, Status.of(StatusType.ROOT, "sniper.trap", ctx.caster.getUUID(), 1,
                            p.level().getGameTime(), ctx.ticks("hold")));
                    Fx.burst(ctx.level, p.position().add(0, 0.5, 0), 1.2f, 10, 0xC9D6E8);
                    ctx.soundAt(p.position(), SoundEvents.IRON_TRAPDOOR_CLOSE, 1.2f, 0.7f);
                    p.vanish();
                    return;
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.IRON_TRAPDOOR_OPEN, 0.8f, 1.2f);
        }
    }

    /** Навылет: выстрел пробивает всех на линии. */
    public static final class Pierce extends Skill {
        public Pierce() {
            super("sniper.pierce", "Навылет", 0xA6B8D8, "⇢", 18, 0.8);
            blocks("length", 30);
            blocks("width", 1.2);
            damage("damage", 2.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Выстрел с чтением {cast}: пробивает всех на {length} по прямой в полосе {width}, "
                    + "каждому {damage}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 from = ctx.caster.position().add(0, 1.4, 0);
            Vec3 dir = ctx.caster.getViewVector(1).normalize();
            Vec3 to = from.add(dir.scale(ctx.v("length")));
            double width = ctx.v("width");
            for (LivingEntity e : ctx.enemiesAround(from.add(to).scale(0.5), ctx.v("length"))) {
                Vec3 ab = to.subtract(from);
                double t = Math.max(0, Math.min(1, e.getBoundingBox().getCenter().subtract(from).dot(ab) / ab.lengthSqr()));
                if (e.getBoundingBox().getCenter().distanceTo(from.add(ab.scale(t))) <= width) ctx.hit(e, "damage");
            }
            Fx.beam(ctx.level, from, to, (float) width * 2, 10, 0xA6B8D8);
            ctx.sound(SoundEvents.ARROW_SHOOT, 1.2f, 0.5f);
        }
    }

    /** Контрольный: по отмеченному — самый сильный выстрел, метка тратится. */
    public static final class Finisher extends Skill {
        public Finisher() {
            super("sniper.finisher", "Контрольный", 0x8B0000, "✹", 45, 0);
            blocks("range", 50);
            damage("damage", 4.5);
            percent("marked", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона, а по отмеченному — на {marked} больше, и метка тратится. "
                    + "Без метки это просто очень сильный выстрел.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean marked = StatusService.has(ctx.target, StatusType.MARK);
            ctx.hitShare(ctx.target, ctx.v("damage") * (marked ? 1 + ctx.v("marked") : 1));
            if (marked) StatusService.removeAll(ctx.target, StatusType.MARK);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.4, 0), ctx.target.position().add(0, 1, 0), 0.3f, 12,
                    marked ? 0xFF2020 : 0x8B0000);
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 1.6f, 12, 0x8B0000);
            ctx.sound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f, 0.6f);
        }
    }
}
