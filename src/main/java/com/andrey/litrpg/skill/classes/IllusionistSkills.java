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
 * Иллюзионист — бой двойниками. Двойник стоит в мире объектом, сам бьёт по тому, кого бьёт хозяин, тянет
 * на себя врагов, и с ним можно поменяться местами. «Отражённый удар» повторяют все двойники сразу,
 * а «Исчезновение» лопает их всех и ослепляет врагов. Морок копится от его же приёмов.
 */
public final class IllusionistSkills {
    private IllusionistSkills() {
    }

    /** Больше двойников сразу не держится: новый гасит самого старого. */
    static final int MAX_DOUBLES = 5;
    /** Сколько секунд враг, бивший иллюзиониста, бьёт появившегося рядом двойника. */
    private static final int LURE_TICKS = 60;

    /**
     * Поставить двойника, который сам бьёт врагов рядом. Появившись, он перехватывает обычных врагов в своём
     * радиусе, которые били иллюзиониста, — на 3 с (хранителя не обмануть).
     */
    private static ClassProp makeDouble(SkillContext ctx, Vec3 at) {
        List<ClassProp> mine = ctx.myProps(PropKind.ILLUSION, 128);
        if (mine.size() >= MAX_DOUBLES) {
            mine.stream().max(Comparator.comparingInt(p -> p.tickCount)).ifPresent(ClassProp::vanish);
        }
        ClassProp d = ctx.prop(PropKind.ILLUSION, at, "hp", ctx.v("radius"), "duration");
        d.onPulse(20, p -> {
            LivingEntity foe = ctx.enemiesAround(p.position(), p.radius()).stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
            if (foe == null) return;
            ctx.hit(foe, "damage");
            p.strikeAt(foe);
            Fx.arc(ctx.level, p, 2f, 120, 6, 0xD6A6FF);
        });
        for (LivingEntity e : ctx.enemiesAround(d.position(), ctx.v("radius"))) {
            if (e instanceof net.minecraft.world.entity.Mob m && m.getTarget() == ctx.caster) {
                com.andrey.litrpg.combat.threat.ThreatService.decoy(m, d, LURE_TICKS);
            }
        }
        Fx.burst(ctx.level, at.add(0, 1, 0), 1.0f, 8, 0xD6A6FF);
        return d;
    }

    /** Двойник: копия с здоровьем, которая бьёт сама. */
    public static final class Phantom extends Skill {
        public Phantom() {
            super("illusion.double", "Двойник", 0xD6A6FF, "⧈", 5, 0);
            blocks("range", 10);
            blocks("radius", 6);
            shield("hp", 2.0);
            damage("damage", 0.842);
            seconds("duration", 22);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Двойник с {hp} здоровья на {duration}: сам бьёт ближайшего врага в {radius} на {damage}, "
                    + "а враги рядом, что били вас, 3 с бьют его. Перезарядка короче жизни двойника — держится до пяти "
                    + "сразу (шестой гасит самого старого). Его можно разбить — он для этого и стоит.";
        }

        @Override
        public void execute(SkillContext ctx) {
            makeDouble(ctx, ctx.point);
            ctx.soundAt(ctx.point, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.5f);
        }
    }

    /** Подмена: поменяться местами с ближайшим двойником. */
    public static final class Swap extends Skill {
        public Swap() {
            super("illusion.swap", "Подмена", 0xB8A6FF, "⇄", 6, 0);
            blocks("search", 30);
            seconds("blur_time", 3);
            percent("miss", 0.45);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Меняется местами с ближайшим двойником в {search} и {blur_time} уходит от ударов "
                    + "с вероятностью {miss}. Выйти из окружения — одним нажатием.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ClassProp d = ctx.myProps(PropKind.ILLUSION, ctx.v("search")).stream()
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(ctx.caster))).orElse(null);
            if (d == null) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Меняться не с кем — двойников рядом нет.");
                return;
            }
            Vec3 mine = ctx.caster.position(), his = d.position();
            d.moveTo(mine.x, mine.y, mine.z, ctx.caster.getYRot(), 0);
            ctx.teleport(his);
            ctx.status(ctx.caster, StatusType.BLUR, Math.min(0.8, ctx.v("miss")), "blur_time");
            Fx.beam(ctx.level, mine.add(0, 1, 0), his.add(0, 1, 0), 0.3f, 10, 0xB8A6FF);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.9f, 1.5f);
        }
    }

    /** Морок: враг переключается на двойника. */
    public static final class Charm extends Skill {
        public Charm() {
            super("illusion.charm", "Морок", 0x9B5CFF, "☍", 10, 0);
            blocks("range", 20);
            blocks("search", 25);
            seconds("duration", 6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Враг {duration} считает двойника настоящим и бьёт его. Нет двойника — морок не сработает, "
                    + "хранителя — не обманет.";
        }

        @Override
        public void execute(SkillContext ctx) {
            if (com.andrey.litrpg.combat.DamageService.isBoss(ctx.target)) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Хранителя морок не обманывает.");
                ctx.readyNow();
                return;
            }
            ClassProp d = ctx.myProps(PropKind.ILLUSION, ctx.v("search")).stream()
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(ctx.target))).orElse(null);
            if (d == null) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Сначала поставьте двойника.");
                return;
            }
            ctx.decoy(ctx.target, d, "duration");
            Fx.beam(ctx.level, ctx.target.position().add(0, 1, 0), d.position().add(0, 1, 0), 0.14f,
                    ctx.ticks("duration"), 0x9B5CFF);
            Fx.mark(ctx.level, ctx.target, "☍", ctx.ticks("duration"), 0x9B5CFF);
            ctx.soundAt(ctx.target.position(), SoundEvents.ENDERMAN_AMBIENT, 1f, 1.3f);
        }
    }

    /** Калейдоскоп: три двойника сразу. */
    public static final class Kaleidoscope extends Skill {
        public Kaleidoscope() {
            super("illusion.kaleidoscope", "Калейдоскоп", 0xE8D6FF, "✤", 24, 0);
            blocks("radius", 6);
            shield("hp", 1.6);
            damage("damage", 0.842);
            count("count", 3);
            seconds("duration", 12);
            count("cost", 25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} морока — {count} двойника вокруг себя на {duration}: каждый с {hp} здоровья "
                    + "бьёт на {damage}. Врагу есть из кого выбирать.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("count"));
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                makeDouble(ctx, ctx.caster.position().add(Math.cos(a) * 2.2, 0, Math.sin(a) * 2.2));
            }
            Fx.ring(ctx.level, ctx.caster.position(), 3f, 12, 0xE8D6FF);
            ctx.sound(SoundEvents.AMETHYST_CLUSTER_PLACE, 1.1f, 1.6f);
        }
    }

    /** Отражённый удар: удар, который повторяют все двойники. */
    public static final class MirroredStrike extends Skill {
        public MirroredStrike() {
            super("illusion.strike", "Отражённый удар", 0xC9A6FF, "✶", 8, 0);
            blocks("range", 18);
            blocks("search", 30);
            damage("damage", 2.997);
            percent("echo", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона по цели, и каждый ваш двойник в {search} повторяет удар на {echo} от этого. "
                    + "Три двойника — почти три удара сверху.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            List<ClassProp> doubles = ctx.myProps(PropKind.ILLUSION, ctx.v("search"));
            double echo = ctx.v("damage") * ctx.v("echo");
            for (int i = 0; i < doubles.size(); i++) {
                ClassProp d = doubles.get(i);
                ctx.later(3 + i * 3, () -> {
                    if (!d.isAlive() || !ctx.target.isAlive()) return;
                    ctx.hitShare(ctx.target, echo);
                    // Повтор удара видно: двойник замахивается к цели, перед ним — полумесяц, к цели — росчерк.
                    d.strikeAt(ctx.target);
                    Fx.arc(ctx.level, d, 2.4f, 140, 8, 0xC9A6FF);
                    Fx.bolt(ctx.level, d.position().add(0, 1.2, 0), ctx.target.position().add(0, 1, 0), 0.14f, 6,
                            0xC9A6FF);
                    Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 0.8f, 6, 0xC9A6FF);
                    ctx.soundAt(d.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.7f, 1.6f);
                });
            }
            Fx.slash(ctx.level, ctx.caster, 2.4f, 60, 6, 0xC9A6FF);
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.5f);
        }
    }

    /** Исчезновение: двойники лопаются, враги теряют всех из виду. */
    public static final class Vanish extends Skill {
        public Vanish() {
            super("illusion.vanish", "Исчезновение", 0xFFFFFF, "✺", 68, 0);
            blocks("search", 30);
            blocks("radius", 6);
            damage("damage", 3.371);
            seconds("duration", 4);
            percent("miss", 0.75);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Все двойники в {search} лопаются: {damage} урона в {radius} от каждого, враги теряют цель, "
                    + "а иллюзионист {duration} уходит от ударов с вероятностью {miss}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> doubles = ctx.myProps(PropKind.ILLUSION, ctx.v("search"));
            for (ClassProp d : doubles) {
                for (LivingEntity e : ctx.enemiesAround(d.position(), ctx.v("radius"))) {
                    ctx.hit(e, "damage");
                    if (e instanceof net.minecraft.world.entity.Mob m) m.setTarget(null);
                }
                Fx.ring(ctx.level, d.position(), (float) ctx.v("radius"), 12, 0xFFFFFF);
                d.vanish();
            }
            ctx.forgetMe(ctx.v("radius") + 8);
            ctx.status(ctx.caster, StatusType.BLUR, Math.min(0.9, ctx.v("miss")), "duration");
            Fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), 2.5f, 12, 0xFFFFFF);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.8f);
        }
    }
}
