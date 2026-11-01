package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Шаман — поддержка тотемами. Каждый тотем стоит в мире настоящей моделью, рисует на земле свой радиус,
 * имеет здоровье — и враги могут его сломать: сломали, значит лечение или бафф кончились. «Духи предков»
 * заставляют все тотемы сработать разом, а «Поглотить тотемы» снимает их и отдаёт силу отряду.
 */
public final class ShamanSkills {
    private ShamanSkills() {
    }

    private static ServerLevel levelOf(ClassProp p, SkillContext ctx) {
        return p.level() instanceof ServerLevel sl ? sl : ctx.level;
    }

    /** Тотем исцеления: лечит всех союзников в своём радиусе. */
    public static final class TotemHeal extends Skill {
        public TotemHeal() {
            super("shaman.totem_heal", "Тотем исцеления", 0x7BF5A0, "⌂", 22, 0);
            blocks("range", 10);
            blocks("radius", 6);
            shield("hp", 3.0);
            heal("heal", 0.55);
            seconds("duration", 14);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Тотем с {hp} здоровья на {duration}: каждую секунду лечит союзников в {radius} на {heal}. "
                    + "Радиус нарисован на земле; враги могут тотем сломать.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double heal = ctx.v("heal") * ctx.unit();
            var totem = ctx.prop(PropKind.TOTEM_HEAL, ctx.point, "hp", ctx.v("radius"), "duration");
            totem.onPulse(20, p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    HealService.healFixed(ctx.caster, a, heal);
                    Fx.beam(levelOf(p, ctx), p.position().add(0, 1.4, 0), a.position().add(0, 1, 0), 0.1f, 10, 0x7BF5A0);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.WOOD_PLACE, 1.1f, 1.2f);
        }
    }

    /** Тотем бури: сам бьёт молнией по врагам в радиусе. */
    public static final class TotemStorm extends Skill {
        public TotemStorm() {
            super("shaman.totem_storm", "Тотем бури", 0xB8C6FF, "⌁", 26, 0);
            blocks("range", 10);
            blocks("radius", 7);
            shield("hp", 2.6);
            damage("damage", 0.8);
            seconds("duration", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Тотем с {hp} здоровья на {duration}: раз в секунду бьёт молнией по врагу в {radius} "
                    + "на {damage}. Бьёт сам, пока стоит.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var totem = ctx.prop(PropKind.TOTEM_STORM, ctx.point, "hp", ctx.v("radius"), "duration");
            totem.onPulse(20, p -> {
                List<LivingEntity> foes = ctx.enemiesAround(p.position(), p.radius());
                if (foes.isEmpty()) return;
                LivingEntity foe = foes.get(ctx.level.random.nextInt(foes.size()));
                ctx.hit(foe, "damage");
                Fx.beam(levelOf(p, ctx), p.position().add(0, 1.6, 0), foe.position().add(0, 1, 0), 0.18f, 8, 0xB8C6FF);
                Fx.pillar(levelOf(p, ctx), foe.position(), 0.5f, 3f, 8, 0xB8C6FF);
                ctx.soundAt(foe.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.6f);
            });
            ctx.soundAt(ctx.point, SoundEvents.WOOD_PLACE, 1.1f, 1f);
        }
    }

    /** Удар молнии: цепь по нескольким врагам. */
    public static final class Lightning extends Skill {
        public Lightning() {
            super("shaman.lightning", "Удар молнии", 0x8FA6FF, "↯", 7, 0);
            blocks("range", 22);
            damage("damage", 1.5);
            count("targets", 3);
            percent("falloff", 0.25);
            blocks("jump", 7);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Молния бьёт врага на {damage} и прыгает до {targets} целей (каждая следующая в {jump} блоках "
                    + "и слабее на {falloff}).";
        }

        @Override
        public void execute(SkillContext ctx) {
            int max = (int) Math.round(ctx.v("targets"));
            double share = ctx.v("damage"), falloff = ctx.v("falloff"), jump = ctx.v("jump");
            List<LivingEntity> hit = new ArrayList<>();
            LivingEntity cur = ctx.target;
            net.minecraft.world.phys.Vec3 from = ctx.caster.position().add(0, 1.4, 0);
            for (int i = 0; i < max && cur != null; i++) {
                ctx.hitShare(cur, share * Math.pow(1 - falloff, i));
                Fx.beam(ctx.level, from, cur.position().add(0, 1, 0), 0.16f, 8, 0x8FA6FF);
                hit.add(cur);
                from = cur.position().add(0, 1, 0);
                LivingEntity next = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : ctx.enemiesAround(cur.position(), jump)) {
                    if (hit.contains(e)) continue;
                    double d = e.distanceToSqr(cur);
                    if (d < best) {
                        best = d;
                        next = e;
                    }
                }
                cur = next;
            }
            ctx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.8f);
        }
    }

    /** Тотем земли: союзники в радиусе получают меньше урона. */
    public static final class TotemEarth extends Skill {
        public TotemEarth() {
            super("shaman.totem_earth", "Тотем земли", 0xC9A055, "⌸", 32, 0);
            blocks("range", 10);
            blocks("radius", 7);
            shield("hp", 4.0);
            percent("reduce", 0.15);
            seconds("duration", 14);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Тотем с {hp} здоровья на {duration}: союзники в {radius} получают на {reduce} меньше урона. "
                    + "Сломали тотем — бафф пропал.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double factor = 1 - Math.min(0.9, ctx.v("reduce"));
            var totem = ctx.prop(PropKind.TOTEM_EARTH, ctx.point, "hp", ctx.v("radius"), "duration");
            totem.onPulse(10, p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_TAKEN, "shaman.totem_earth",
                            ctx.caster.getUUID(), factor, levelOf(p, ctx).getGameTime(), 14));
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.STONE_PLACE, 1.1f, 0.8f);
        }
    }

    /** Духи предков: все тотемы срабатывают разом и с двойным радиусом. */
    public static final class Ancestors extends Skill {
        public Ancestors() {
            super("shaman.ancestors", "Духи предков", 0xFFE8A6, "✵", 60, 0);
            blocks("radius", 30);
            count("times", 3);
            percent("wider", 1.0);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Духи входят в тотемы: каждый ваш тотем в {radius} срабатывает {times} раза за {duration} "
                    + "и на это время его радиус шире на {wider}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> totems = ctx.myProps(null, ctx.v("radius")).stream()
                    .filter(p -> p.kind() == PropKind.TOTEM_HEAL || p.kind() == PropKind.TOTEM_STORM
                            || p.kind() == PropKind.TOTEM_EARTH)
                    .toList();
            if (totems.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Рядом нет ваших тотемов.");
                return;
            }
            int times = (int) Math.round(ctx.v("times"));
            double wider = 1 + ctx.v("wider");
            int step = Math.max(10, ctx.ticks("duration") / Math.max(1, times));
            for (ClassProp p : totems) {
                double was = p.radius();
                p.setRadius(was * wider);
                Fx.pillar(ctx.level, p.position(), (float) p.radius(), 4f, ctx.ticks("duration"), 0xFFE8A6);
                for (int i = 0; i < times; i++) {
                    ctx.later(1 + i * step, p::pulseNow);
                }
                ctx.later(ctx.ticks("duration"), () -> p.setRadius(was));
            }
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 0.8f);
        }
    }

    /** Поглотить тотемы: снять свои тотемы и отдать их силу отряду. */
    public static final class Consume extends Skill {
        public Consume() {
            super("shaman.consume", "Поглотить тотемы", 0xFFD27A, "✹", 100, 0);
            blocks("radius", 30);
            percent("power", 0.08);
            shield("shield", 1.2);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Снимает все ваши тотемы в {radius}: за каждый отряд получает +{power} урона и щит на {shield} "
                    + "на {duration}. Чем больше тотемов стояло, тем сильнее отряд.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> totems = ctx.myProps(null, ctx.v("radius")).stream()
                    .filter(p -> p.kind() == PropKind.TOTEM_HEAL || p.kind() == PropKind.TOTEM_STORM
                            || p.kind() == PropKind.TOTEM_EARTH)
                    .toList();
            if (totems.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Поглощать нечего — тотемов рядом нет.");
                return;
            }
            int n = totems.size();
            for (ClassProp p : totems) {
                Fx.beam(ctx.level, p.position().add(0, 1.4, 0), ctx.caster.position().add(0, 1, 0), 0.2f, 12, 0xFFD27A);
                p.vanish();
            }
            double power = 1 + ctx.v("power") * n;
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), 25)) {
                ctx.status(a, StatusType.DAMAGE_DEALT, power, "duration");
                ctx.shieldFixed(a, ctx.v("shield") * ctx.unit() * n, "duration");
                Fx.pillar(ctx.level, a.position(), 0.8f, 3.5f, 20, 0xFFD27A);
            }
            ctx.sound(SoundEvents.TOTEM_USE, 1f, 1.1f);
        }
    }
}
