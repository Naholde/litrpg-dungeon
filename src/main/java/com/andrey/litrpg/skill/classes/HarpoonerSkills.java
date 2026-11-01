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

import java.util.Comparator;

/**
 * Гарпунёр — боец на цепях. Он решает, кто где стоит: гарпун тянет врага к себе, двойная цепь связывает
 * двух врагов между собой, кандалы не дают уйти от гарпунёра, а «Тяга бездны» собирает всех в одну точку.
 */
public final class HarpoonerSkills {
    private HarpoonerSkills() {
    }

    /** Гарпун: врага тянет к гарпунёру. */
    public static final class Harpoon extends Skill {
        public Harpoon() {
            super("harpoon.throw", "Гарпун", 0x8B8B8B, "⚓", 8, 0);
            blocks("range", 18);
            damage("damage", 1.0);
            seconds("slow_time", 2);
            percent("slow", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Гарпун до {range}: тянет врага к вам, бьёт на {damage} и замедляет на {slow} на {slow_time}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.2f, 12, 0x8B8B8B);
            ctx.pull(ctx.target, ctx.caster.position(), 1.2);
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.SLOW, Math.min(0.8, ctx.v("slow")), "slow_time");
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.8f);
        }
    }

    /** Двойная цепь: двух врагов стягивает друг к другу, и растяжение их ранит. */
    public static final class DoubleChain extends Skill {
        public DoubleChain() {
            super("harpoon.double", "Двойная цепь", 0xC9D6E8, "⛓", 18, 0);
            blocks("range", 16);
            blocks("link", 14);
            seconds("duration", 6);
            damage("damage", 0.45);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Связывает цель с ближайшим врагом (до {link} между ними) на {duration}: их тянет друг к другу, "
                    + "и пока они растягивают цепь, оба получают {damage} в секунду.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity a = ctx.target;
            LivingEntity b = ctx.enemiesAround(a.position(), ctx.v("link")).stream()
                    .filter(e -> e != a)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(a))).orElse(null);
            if (b == null) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Рядом с целью некого связать.");
                return;
            }
            int dur = ctx.ticks("duration");
            Fx.beam(ctx.level, a, b, 0.22f, dur, 0xC9D6E8);
            Zones.add(ctx.level, a.position(), 1, ctx.caster.getUUID(), dur, 10, z -> {
                if (!a.isAlive() || !b.isAlive()) return;
                double d = a.distanceTo(b);
                Vec3 mid = a.position().add(b.position()).scale(0.5);
                ctx.pull(a, mid, 0.5);
                ctx.pull(b, mid, 0.5);
                if (d > 4 && z.level.getGameTime() % 20 == 0) {
                    ctx.hit(a, "damage");
                    ctx.hit(b, "damage");
                }
            });
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.1f, 0.7f);
        }
    }

    /** Намотка: раскрутить цепь — урон по кругу и подброс. */
    public static final class Windup extends Skill {
        public Windup() {
            super("harpoon.windup", "Намотка", 0xB0B0B0, "✸", 14, 0);
            blocks("radius", 4.5);
            damage("damage", 1.1);
            count("spins", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{spins} оборота цепью вокруг себя: каждому врагу в {radius} по {damage}, а последний оборот "
                    + "подбрасывает их.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int spins = (int) Math.round(ctx.v("spins"));
            for (int i = 0; i < spins; i++) {
                boolean last = i == spins - 1;
                ctx.later(1 + i * 6, () -> {
                    for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) {
                        ctx.hit(e, "damage");
                        if (last) ctx.toss(e, 0.55);
                    }
                    Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 6, 0xB0B0B0);
                });
            }
            ctx.sound(SoundEvents.CHAIN_BREAK, 1f, 0.8f);
        }
    }

    /** Кандалы: цель прикована и не может уйти от гарпунёра. */
    public static final class Shackles extends Skill {
        public Shackles() {
            super("harpoon.shackles", "Кандалы", 0x6B7A8B, "⊟", 26, 0);
            blocks("range", 14);
            blocks("leash", 6);
            seconds("duration", 6);
            damage("damage", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Кандалы на {duration}: цель прикована к гарпунёру — дальше {leash} блоков не отойдёт, "
                    + "её тянет обратно и бьёт на {damage}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity victim = ctx.target;
            int dur = ctx.ticks("duration");
            double leash = ctx.v("leash");
            StatusService.apply(victim, Status.of(StatusType.ROOT, "harpoon.shackles", ctx.caster.getUUID(), 1,
                    ctx.now(), 20));
            Fx.beam(ctx.level, ctx.caster, victim, 0.2f, dur, 0x6B7A8B);
            Fx.mark(ctx.level, victim, "⊟", dur, 0x6B7A8B);
            Zones.add(ctx.level, ctx.caster.position(), 1, ctx.caster.getUUID(), dur, 10, z -> {
                if (!victim.isAlive() || !ctx.caster.isAlive()) return;
                if (victim.distanceTo(ctx.caster) > leash) {
                    ctx.pull(victim, ctx.caster.position(), 1.4);
                    ctx.hit(victim, "damage");
                    Fx.beam(z.level, ctx.caster, victim, 0.16f, 8, 0x6B7A8B);
                }
            });
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.2f, 0.5f);
        }
    }

    /** Рывок на цепи: гарпунёр сам летит к цели. */
    public static final class ChainPull extends Skill {
        public ChainPull() {
            super("harpoon.pull_self", "Рывок на цепи", 0xD6E8FF, "➤", 12, 0);
            blocks("range", 20);
            damage("damage", 1.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Цепь цепляется за врага до {range}, и гарпунёр сам летит к нему с удара на {damage}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 to = ctx.target.position();
            Vec3 dir = to.subtract(ctx.caster.position()).multiply(1, 0, 1);
            Vec3 stop = to.subtract(dir.normalize().scale(1.4));
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.2f, 10, 0xD6E8FF);
            ctx.teleport(new Vec3(stop.x, to.y, stop.z));
            ctx.hit(ctx.target, "damage");
            Fx.slash(ctx.level, ctx.caster, 2.2f, 60, 6, 0xD6E8FF);
            ctx.sound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1f, 1.2f);
        }
    }

    /** Тяга бездны: всех врагов в радиусе собирает в одну точку. */
    public static final class AbyssPull extends Skill {
        public AbyssPull() {
            super("harpoon.abyss", "Тяга бездны", 0x3A4A6B, "✺", 75, 0);
            blocks("range", 16);
            blocks("radius", 10);
            seconds("duration", 3);
            damage("damage", 1.4);
            percent("slow", 0.4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Все враги в {radius} стягиваются к точке {duration}, получают {damage} урона и замедление "
                    + "на {slow}. Собрать толпу — и дать товарищам ударить по ней разом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.8, ctx.v("slow"));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x3A4A6B, false);
            Fx.pillar(ctx.level, ctx.point, 1.0f, 4f, dur, 0x3A4A6B);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 5, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.pull(e, z.center, 1.6);
                    StatusService.apply(e, Status.of(StatusType.SLOW, "harpoon.abyss", ctx.caster.getUUID(), slow,
                            z.level.getGameTime(), 14));
                    Fx.beam(z.level, e.position().add(0, 1, 0), z.center.add(0, 0.5, 0), 0.1f, 6, 0x3A4A6B);
                }
            });
            ctx.later(dur, () -> {
                for (LivingEntity e : ctx.enemiesAround(ctx.point, r * 0.6)) ctx.hit(e, "damage");
                Fx.ring(ctx.level, ctx.point, (float) r * 0.6f, 12, 0x3A4A6B);
            });
            ctx.soundAt(ctx.point, SoundEvents.CHAIN_PLACE, 1.3f, 0.5f);
        }
    }
}
