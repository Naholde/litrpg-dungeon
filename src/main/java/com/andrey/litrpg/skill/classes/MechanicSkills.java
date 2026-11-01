package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.prop.ClassProp;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Механик — боец станками. Турель, мина и мортира стоят в мире сами: у них есть здоровье, их видно,
 * и враги могут их сломать. Заряд копится от попаданий и уходит в «Разгон станков», а «Полный арсенал»
 * ставит всё сразу — но потом станки перегреваются.
 */
public final class MechanicSkills {
    private MechanicSkills() {
    }

    /** Турель: стоит и стреляет по ближнему врагу. */
    public static final class Turret extends Skill {
        public Turret() {
            super("mech.turret", "Турель", 0xC9D6E8, "⌂", 30, 0);
            blocks("range", 8);
            blocks("radius", 12);
            shield("hp", 3.0);
            damage("damage", 0.6);
            seconds("duration", 20);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Турель с {hp} здоровья на {duration}: сама стреляет по ближайшему врагу в {radius} "
                    + "на {damage} дважды в секунду. Её можно сломать.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var turret = ctx.prop(PropKind.TURRET, ctx.point, "hp", ctx.v("radius"), "duration");
            turret.onPulse(10, p -> {
                LivingEntity foe = ctx.enemiesAround(p.position(), p.radius()).stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
                if (foe == null) return;
                ctx.hit(foe, "damage");
                Fx.beam(ctx.level, p.position().add(0, 1, 0), foe.position().add(0, 1, 0), 0.1f, 5, 0xFF7A2E);
                ctx.soundAt(p.position(), SoundEvents.CROSSBOW_SHOOT, 0.6f, 1.4f);
            });
            ctx.soundAt(ctx.point, SoundEvents.IRON_DOOR_CLOSE, 1f, 1.4f);
        }
    }

    /** Дробовик: в упор, по конусу. */
    public static final class Shotgun extends Skill {
        public Shotgun() {
            super("mech.shotgun", "Дробовик", 0xFF7A2E, "⁂", 7, 0);
            blocks("radius", 6);
            param("angle", 55, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 1.5);
            percent("close", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Залп дробью в секторе {radius}: {damage} урона, а в упор (ближе половины) — на {close} больше.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double half = ctx.v("radius") * 0.5;
            for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) {
                boolean close = e.distanceTo(ctx.caster) <= half;
                ctx.hitShare(e, ctx.v("damage") * (close ? 1 + ctx.v("close") : 1));
            }
            Fx.cone(ctx.level, ctx.caster.position(), ctx.caster.getViewVector(1).multiply(1, 0, 1),
                    (float) ctx.v("radius"), (float) ctx.v("angle"), 8, 0xFF7A2E, true);
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.8f);
        }
    }

    /** Мина: лежит и ждёт того, кто подойдёт. */
    public static final class Mine extends Skill {
        public Mine() {
            super("mech.mine", "Мина", 0xFF4A2E, "◉", 18, 0);
            blocks("range", 10);
            blocks("radius", 3);
            shield("hp", 1.2);
            damage("damage", 2.2);
            seconds("duration", 30);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Мина лежит до {duration}. Враг подошёл ближе {radius} — взрыв на {damage} по всем рядом. "
                    + "Мин можно расставить несколько.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var mine = ctx.prop(PropKind.MINE, ctx.point, "hp", ctx.v("radius"), "duration");
            mine.onPulse(5, p -> {
                if (ctx.enemiesAround(p.position(), p.radius() * 0.6).isEmpty()) return;
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) ctx.hit(e, "damage");
                Fx.ring(ctx.level, p.position(), (float) p.radius(), 10, 0xFF4A2E);
                Fx.burst(ctx.level, p.position().add(0, 0.6, 0), 2f, 10, 0xFF4A2E);
                ctx.soundAt(p.position(), SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.1f);
                p.vanish();
            });
            ctx.soundAt(ctx.point, SoundEvents.STONE_BUTTON_CLICK_ON, 1f, 1.6f);
        }
    }

    /** Мортира: три снаряда по точке с разметкой. */
    public static final class Mortar extends Skill {
        public Mortar() {
            super("mech.mortar", "Мортира", 0xE07A3A, "⇡", 35, 0);
            blocks("range", 25);
            blocks("radius", 3.5);
            damage("damage", 1.6);
            count("shells", 3);
            seconds("delay", 1.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "{shells} снаряда по точке: каждый через {delay} после разметки бьёт на {damage} в {radius}. "
                    + "Разметку видно — уйти можно.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int shells = (int) Math.round(ctx.v("shells"));
            int delay = ctx.ticks("delay");
            double r = ctx.v("radius");
            for (int i = 0; i < shells; i++) {
                double angle = i * 2.1;
                Vec3 at = ctx.point.add(Math.cos(angle) * (i == 0 ? 0 : 2.5), 0, Math.sin(angle) * (i == 0 ? 0 : 2.5));
                Fx.disc(ctx.level, at, (float) r, delay + i * 8, 0xE07A3A, true);
                ctx.later(delay + i * 8, () -> {
                    for (LivingEntity e : ctx.enemiesAround(at, r)) ctx.hit(e, "damage");
                    Fx.burst(ctx.level, at.add(0, 0.6, 0), (float) r, 10, 0xE07A3A);
                    ctx.soundAt(at, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 0.9f);
                });
            }
            ctx.sound(SoundEvents.FIRECHARGE_USE, 1f, 0.7f);
        }
    }

    /** Разгон станков: за заряд все станки работают втрое чаще. */
    public static final class Overdrive extends Skill {
        public Overdrive() {
            super("mech.overdrive", "Разгон станков", 0xFFD27A, "≫", 40, 0);
            blocks("radius", 30);
            count("cost", 40);
            count("times", 4);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} заряда: каждый ваш станок в {radius} срабатывает {times} раза за {duration} — "
                    + "турели стреляют очередью, мортира сыплет снарядами.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> machines = ctx.myProps(null, ctx.v("radius")).stream()
                    .filter(p -> p.kind() == PropKind.TURRET || p.kind() == PropKind.MINE)
                    .toList();
            if (machines.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Разгонять нечего — станков рядом нет.");
                return;
            }
            int times = (int) Math.round(ctx.v("times"));
            int step = Math.max(5, ctx.ticks("duration") / Math.max(1, times));
            for (ClassProp p : machines) {
                Fx.pillar(ctx.level, p.position(), 0.8f, 2.5f, ctx.ticks("duration"), 0xFFD27A);
                for (int i = 0; i < times; i++) ctx.later(1 + i * step, p::pulseNow);
            }
            ctx.sound(SoundEvents.DISPENSER_LAUNCH, 1f, 1.4f);
        }
    }

    /** Полный арсенал: всё сразу, а потом перегрев. */
    public static final class FullArsenal extends Skill {
        public FullArsenal() {
            super("mech.arsenal", "Полный арсенал", 0xFF4A2E, "✹", 150, 0);
            blocks("radius", 12);
            shield("hp", 3.0);
            damage("damage", 1.2);
            seconds("duration", 14);
            count("turrets", 3);
            percent("overheat", 0.3);
            seconds("overheat_time", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Ставит {turrets} турели вокруг себя (по {hp} здоровья, на {duration}, каждая бьёт на {damage}). "
                    + "После арсенала станки перегреты: {overheat_time} механик бьёт на {overheat} слабее.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("turrets"));
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                Vec3 at = ctx.caster.position().add(Math.cos(a) * 2.5, 0, Math.sin(a) * 2.5);
                var turret = ctx.prop(PropKind.TURRET, at, "hp", ctx.v("radius"), "duration");
                turret.onPulse(10, p -> {
                    LivingEntity foe = ctx.enemiesAround(p.position(), p.radius()).stream()
                            .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
                    if (foe == null) return;
                    ctx.hit(foe, "damage");
                    Fx.beam(ctx.level, p.position().add(0, 1, 0), foe.position().add(0, 1, 0), 0.1f, 5, 0xFF7A2E);
                });
            }
            ctx.status(ctx.caster, StatusType.DAMAGE_DEALT, 1 - Math.min(0.9, ctx.v("overheat")), "overheat_time");
            Fx.ring(ctx.level, ctx.caster.position(), 3f, 14, 0xFF4A2E);
            ctx.sound(SoundEvents.IRON_GOLEM_REPAIR, 1f, 0.8f);
        }
    }
}
