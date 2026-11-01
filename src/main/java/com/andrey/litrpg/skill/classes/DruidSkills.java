package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;

/**
 * Друид — лекарь, который не стоит над раненым. Его лечение растёт само: цветок тлеет на одном, лоза
 * перескакивает на следующего, живое древо лечит всех в своём радиусе (и его можно сломать), цветение
 * тем сильнее, чем больше всего уже растёт, а весна заставляет всё это биться вдвое чаще.
 */
public final class DruidSkills {
    private DruidSkills() {
    }

    /** Цветок жизни: лечение со временем. */
    public static final class Flower extends Skill {
        public Flower() {
            super("druid.flower", "Цветок жизни", 0x6BE38A, "✿", 5, 0);
            blocks("range", 30);
            heal("heal", 3.2);
            seconds("duration", 9);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Цветок на союзнике: {heal} здоровья за {duration}, понемногу каждую секунду. "
                    + "Чем больше цветов в группе, тем сильнее «Цветение».";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity who = ctx.target != null ? ctx.target : ctx.caster;
            ctx.hot(who, "heal", "duration");
            Fx.discOn(ctx.level, who, 0.9f, ctx.ticks("duration"), 0x6BE38A, false);
            Fx.mark(ctx.level, who, "✿", ctx.ticks("duration"), 0x6BE38A);
            ctx.soundAt(who.position(), SoundEvents.GRASS_PLACE, 0.8f, 1.5f);
        }
    }

    /** Лоза: лечит, а как вылечит — сама переходит к следующему раненому. */
    public static final class Vine extends Skill {
        public Vine() {
            super("druid.vine", "Лоза", 0x3AA85A, "⌇", 16, 0);
            blocks("range", 25);
            heal("heal", 0.6);
            seconds("duration", 12);
            count("jumps", 4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Лоза живёт {duration} и лечит по {heal} в секунду. Вылечила одного — сама перекидывается "
                    + "на следующего раненого (до {jumps} раз).";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity first = ctx.target != null ? ctx.target : ctx.caster;
            int dur = ctx.ticks("duration");
            double heal = ctx.v("heal") * ctx.unit();
            int maxJumps = (int) Math.round(ctx.v("jumps"));
            LivingEntity[] cur = {first};
            int[] jumps = {0};
            Fx.mark(ctx.level, first, "⌇", 40, 0x3AA85A);
            Zones.add(ctx.level, ctx.caster.position(), 1, ctx.caster.getUUID(), dur, 20, z -> {
                final LivingEntity current = cur[0];
                if (current == null || !current.isAlive()) return;
                LivingEntity target = current;
                boolean full = current.getHealth() >= current.getMaxHealth() - 0.5;
                if (full && jumps[0] < maxJumps) {
                    LivingEntity next = ctx.alliesAround(ctx.caster.position(), 25).stream()
                            .filter(a -> a != current && a.getHealth() < a.getMaxHealth())
                            .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
                    if (next != null) {
                        jumps[0]++;
                        Fx.beam(z.level, current, next, 0.14f, 10, 0x3AA85A);
                        Fx.mark(z.level, next, "⌇", 40, 0x3AA85A);
                        cur[0] = next;
                        target = next;
                    }
                }
                HealService.healFixed(ctx.caster, target, heal);
                Fx.burst(z.level, target.position().add(0, 1, 0), 0.5f, 6, 0x3AA85A);
            });
            ctx.soundAt(first.position(), SoundEvents.VINE_PLACE, 1f, 1.2f);
        }
    }

    /** Шипы: поле шипов, которое ранит и держит. */
    public static final class Thorns extends Skill {
        public Thorns() {
            super("druid.thorns", "Шипы", 0x8FA83A, "⁂", 12, 0);
            blocks("range", 16);
            blocks("radius", 4);
            seconds("duration", 6);
            damage("damage", 0.5);
            percent("slow", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Поле шипов радиусом {radius} на {duration}: врагам внутри {damage} урона в секунду "
                    + "и замедление на {slow}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.8, ctx.v("slow"));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x8FA83A, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.hit(e, "damage");
                    StatusService.apply(e, Status.of(StatusType.SLOW, "druid.thorns", ctx.caster.getUUID(), slow,
                            z.level.getGameTime(), 30));
                }
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0x8FA83A);
            });
            ctx.soundAt(ctx.point, SoundEvents.SWEET_BERRY_BUSH_PLACE, 1f, 0.8f);
        }
    }

    /** Живое древо: объект, который лечит всех в радиусе, пока его не сломают. */
    public static final class Tree extends Skill {
        public Tree() {
            super("druid.tree", "Живое древо", 0x4A8B3A, "♣", 55, 0);
            blocks("range", 12);
            blocks("radius", 7);
            shield("hp", 5.0);
            heal("heal", 0.5);
            seconds("duration", 16);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Выращивает древо с {hp} здоровья на {duration}: каждую секунду лечит всех союзников "
                    + "в {radius}. Враги могут его срубить — тогда лечение кончится.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double heal = ctx.v("heal") * ctx.unit();
            var tree = ctx.prop(PropKind.TREE, ctx.point, "hp", ctx.v("radius"), "duration");
            tree.onPulse(20, p -> {
                for (LivingEntity a : ctx.alliesAround(p.position(), p.radius())) {
                    HealService.healFixed(ctx.caster, a, heal);
                    Fx.beam(p.level() instanceof net.minecraft.server.level.ServerLevel sl ? sl : ctx.level,
                            p.position().add(0, 2, 0), a.position().add(0, 1, 0), 0.1f, 10, 0x6BE38A);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.AZALEA_PLACE, 1.2f, 0.8f);
        }
    }

    /** Цветение: мгновенное лечение, тем сильнее, чем больше всего уже растёт. */
    public static final class Bloom extends Skill {
        public Bloom() {
            super("druid.bloom", "Цветение", 0xB8F57A, "❀", 35, 0);
            blocks("radius", 25);
            heal("heal", 2.0);
            percent("bonus", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Всей группе в {radius} — {heal} здоровья, и ещё {bonus} сверху тем, у кого уже растёт "
                    + "лечение со временем. Сначала посадить, потом цвести.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double base = ctx.v("heal") * ctx.unit();
            double bonus = ctx.v("bonus");
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                boolean growing = StatusService.has(a, StatusType.HOT);
                HealService.heal(ctx.caster, a, base * (growing ? 1 + bonus : 1));
                Fx.pillar(ctx.level, a.position(), 0.7f, 3f, 16, growing ? 0xB8F57A : 0x6BE38A);
            }
            Fx.ring(ctx.level, ctx.caster.position(), 6f, 14, 0xB8F57A);
            ctx.sound(SoundEvents.AZALEA_LEAVES_PLACE, 1.1f, 1.3f);
        }
    }

    /** Весна: всё, что растёт, начинает биться вдвое чаще. */
    public static final class Spring extends Skill {
        public Spring() {
            super("druid.spring", "Весна", 0x7BF5A0, "✺", 120, 0);
            blocks("radius", 25);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: каждое лечение со временем в группе ({radius}) срабатывает вдвое чаще — "
                    + "цветы, лоза и древо работают на двойной скорости.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.ring(ctx.level, ctx.caster.position(), (float) r, 20, 0x7BF5A0);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 20, z -> {
                // Лишний «такт» между обычными: ровно вдвое чаще.
                for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), r)) {
                    double perTick = StatusService.sum(a, StatusType.HOT);
                    if (perTick > 0) {
                        HealService.healFixed(ctx.caster, a, perTick);
                        Fx.burst(z.level, a.position().add(0, 1, 0), 0.5f, 6, 0x7BF5A0);
                    }
                }
            });
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 1f, 1.5f);
        }
    }
}
