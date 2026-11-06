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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;

/**
 * Знаменосец — поддержка, которая работает местом. Знамёна стоят в мире, рисуют свой радиус и ломаются:
 * пока знамя цело, у отряда внутри есть его аура. Клич снимает с группы оглушения и замедления, «Сомкнуть
 * ряды» собирает всех к знамени, а «Последний бой» не даёт никому умереть, пока знамя стоит.
 */
public final class BannermanSkills {
    private BannermanSkills() {
    }

    private static ServerLevel levelOf(ClassProp p, SkillContext ctx) {
        return p.level() instanceof ServerLevel sl ? sl : ctx.level;
    }

    /** Знамя силы: союзники рядом бьют больнее. */
    public static final class BannerPower extends Skill {
        public BannerPower() {
            super("banner.power", "Знамя силы", 0xD8343A, "⚑", 26, 0);
            blocks("range", 10);
            blocks("radius", 8);
            shield("hp", 7.428);
            percent("power", 0.225);
            heal("heal", 0.382);
            seconds("duration", 16);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Знамя с {hp} здоровья на {duration}: союзники в {radius} бьют на {power} сильнее и каждую "
                    + "секунду получают {heal} здоровья. Сломали знамя — аура пропала.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double power = 1 + ctx.v("power");
            double heal = ctx.v("heal") * ctx.unit() * 0.5;
            var banner = ctx.prop(PropKind.BANNER_POWER, ctx.point, "hp", ctx.v("radius"), "duration");
            banner.onPulse(10, p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_DEALT, "banner.power", ctx.caster.getUUID(),
                            power, levelOf(p, ctx).getGameTime(), 14));
                    com.andrey.litrpg.combat.HealService.healFixed(ctx.caster, a, heal);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.WOOL_PLACE, 1.1f, 1f);
        }
    }

    /** Знамя стойкости: союзники рядом получают меньше урона. */
    public static final class BannerGuard extends Skill {
        public BannerGuard() {
            super("banner.guard", "Знамя стойкости", 0x7FB8FF, "⚐", 26, 0);
            blocks("range", 10);
            blocks("radius", 8);
            shield("hp", 7.428);
            percent("reduce", 0.225);
            heal("heal", 0.382);
            seconds("duration", 16);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Знамя с {hp} здоровья на {duration}: союзники в {radius} получают на {reduce} меньше урона "
                    + "и каждую секунду восстанавливают {heal} здоровья.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double factor = 1 - Math.min(0.9, ctx.v("reduce"));
            double heal = ctx.v("heal") * ctx.unit() * 0.5;
            var banner = ctx.prop(PropKind.BANNER_GUARD, ctx.point, "hp", ctx.v("radius"), "duration");
            banner.onPulse(10, p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_TAKEN, "banner.guard", ctx.caster.getUUID(),
                            factor, levelOf(p, ctx).getGameTime(), 14));
                    com.andrey.litrpg.combat.HealService.healFixed(ctx.caster, a, heal);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.WOOL_PLACE, 1.1f, 0.9f);
        }
    }

    /** Клич: с группы слетают оглушения и замедления, и некоторое время не действуют. */
    public static final class Shout extends Skill {
        public Shout() {
            super("banner.shout", "Клич", 0xFFE27A, "❖", 40, 0);
            blocks("radius", 20);
            seconds("duration", 6);
            heal("heal", 2.547);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Клич на {radius}: с группы слетают оглушение, обездвиживание, замедление и обморожение, "
                    + "{duration} они не действуют вовсе, и каждый сразу получает {heal} здоровья.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                for (StatusType t : StatusType.values()) {
                    if (t.controlling()) StatusService.removeAll(a, t);
                }
                ctx.status(a, StatusType.STEADFAST, 1, "duration");
                ctx.heal(a, "heal");
                Fx.mark(ctx.level, a, "❖", dur, 0xFFE27A);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 16, 0xFFE27A);
            ctx.sound(SoundEvents.RAID_HORN.value(), 1f, 1.1f);
        }
    }

    /** Знамя скорости: союзники рядом быстрее и с короткими перезарядками. */
    public static final class BannerSpeed extends Skill {
        public BannerSpeed() {
            super("banner.speed", "Знамя скорости", 0x7FF0D0, "⚏", 30, 0);
            blocks("range", 10);
            blocks("radius", 8);
            shield("hp", 6.367);
            percent("speed", 0.5);
            percent("haste", 0.225);
            heal("heal", 0.382);
            seconds("duration", 16);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Знамя с {hp} здоровья на {duration}: союзники в {radius} бегут на {speed} быстрее, "
                    + "перезарядки у них короче на {haste}, и каждую секунду они получают {heal} здоровья.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double speed = ctx.v("speed"), haste = ctx.v("haste");
            double heal = ctx.v("heal") * ctx.unit() * 0.5;
            var banner = ctx.prop(PropKind.BANNER_SPEED, ctx.point, "hp", ctx.v("radius"), "duration");
            banner.onPulse(10, p -> {
                long now = levelOf(p, ctx).getGameTime();
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.apply(a, Status.of(StatusType.SPEED, "banner.speed", ctx.caster.getUUID(), speed,
                            now, 14));
                    StatusService.apply(a, Status.of(StatusType.HASTE, "banner.speed", ctx.caster.getUUID(), haste,
                            now, 14));
                    com.andrey.litrpg.combat.HealService.healFixed(ctx.caster, a, heal);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.WOOL_PLACE, 1.1f, 1.3f);
        }
    }

    /** Сомкнуть ряды: союзников тянет к знамени, и им щит. */
    public static final class CloseRanks extends Skill {
        public CloseRanks() {
            super("banner.ranks", "Сомкнуть ряды", 0xC9D6E8, "⊞", 45, 0);
            blocks("radius", 25);
            shield("shield", 4.244);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Союзников в {radius} тянет к ближайшему вашему знамени (нет знамени — к вам) и даёт каждому "
                    + "щит на {shield} на {duration}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ClassProp banner = ctx.myProps(null, ctx.v("radius")).stream()
                    .filter(p -> p.kind() == PropKind.BANNER_POWER || p.kind() == PropKind.BANNER_GUARD
                            || p.kind() == PropKind.BANNER_SPEED)
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(ctx.caster))).orElse(null);
            net.minecraft.world.phys.Vec3 to = banner != null ? banner.position() : ctx.caster.position();
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                if (a != ctx.caster) ctx.pull(a, to, 0.9);
                ctx.shield(a, "shield", "duration");
                Fx.beam(ctx.level, a.position().add(0, 1, 0), to.add(0, 1, 0), 0.12f, 12, 0xC9D6E8);
            }
            Fx.ring(ctx.level, to, 4f, 14, 0xC9D6E8);
            ctx.sound(SoundEvents.SHIELD_BLOCK, 1f, 0.8f);
        }
    }

    /** Последний бой: пока знамя стоит, никто в его радиусе не умирает. */
    public static final class LastStand extends Skill {
        public LastStand() {
            super("banner.last_stand", "Последний бой", 0xFFFFFF, "✠", 144, 0);
            blocks("radius", 10);
            shield("hp", 16.979);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Втыкает знамя с {hp} здоровья на {duration}: пока оно стоит, союзники в {radius} не умирают — "
                    + "любой смертельный удар оставляет 1 здоровья. Сломали знамя — защита кончилась.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var banner = ctx.prop(PropKind.BANNER_GUARD, ctx.caster.position(), "hp", ctx.v("radius"), "duration");
            banner.onPulse(10, p -> {
                long now = levelOf(p, ctx).getGameTime();
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.apply(a, Status.of(StatusType.WARD, "banner.last_stand", ctx.caster.getUUID(), 1,
                            now, 14));
                    Fx.mark(levelOf(p, ctx), a, "✠", 14, 0xFFFFFF);
                }
                Fx.ring(levelOf(p, ctx), p.position(), (float) p.radius(), 12, 0xFFFFFF);
            });
            banner.onBreak(p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.removeFrom(a, StatusType.WARD, ctx.caster.getUUID());
                }
            });
            Fx.pillarOn(ctx.level, banner, (float) ctx.v("radius"), 5f, ctx.ticks("duration"), 0xFFFFFF);
            ctx.sound(SoundEvents.RAID_HORN.value(), 1.2f, 0.8f);
        }
    }
}
