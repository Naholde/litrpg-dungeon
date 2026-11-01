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

import java.util.Comparator;
import java.util.List;

/**
 * Чумной владыка — зараза, которая работает за него. Слои заразы копятся на враге, перескакивают на соседей,
 * мешают его лечить, а «Пандемия» разом взыскивает с каждого заражённого всё, что он успел накопить.
 * «Обратить» и вовсе отправляет заражённого бить своих.
 */
public final class PlagueSkills {
    private PlagueSkills() {
    }

    /** Слой заразы с тлеющим уроном. */
    static void infect(SkillContext ctx, LivingEntity e, String secondsKey, String dotKey) {
        int stacks = ctx.addStack(e, StatusType.PLAGUE, secondsKey, 5);
        StatusService.apply(e, Status.periodic(StatusType.DOT, "plague.rot", ctx.caster.getUUID(),
                ctx.precomputed(dotKey) / 5 * stacks, ctx.now(), 100, 20));
        Fx.mark(ctx.level, e, stacks >= 5 ? "☠" : "☣", 30, 0x8FA83A);
    }

    /** Мор: заразить цель. */
    public static final class Plague extends Skill {
        public Plague() {
            super("plague.plague", "Мор", 0x8FA83A, "☣", 3, 0);
            blocks("range", 20);
            damage("damage", 0.478);
            damage("rot", 1.092);
            seconds("duration", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона и слой заразы на {duration}: зараза тлеет на {rot} и тем сильнее, "
                    + "чем больше слоёв (до пяти).";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            infect(ctx, ctx.target, "duration", "rot");
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.3, 0), ctx.target.position().add(0, 1, 0), 0.12f, 6,
                    0x8FA83A);
            ctx.sound(SoundEvents.BOTTLE_EMPTY, 0.9f, 0.7f);
        }
    }

    /** Распространение: зараза перескакивает с цели на соседей. */
    public static final class Spread extends Skill {
        public Spread() {
            super("plague.spread", "Распространение", 0x6B8B2A, "⁂", 10, 0);
            blocks("range", 20);
            blocks("radius", 7);
            count("targets", 4);
            damage("rot", 0.819);
            seconds("duration", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Зараза с цели перескакивает на {targets} ближайших врагов в {radius}: каждому столько же "
                    + "слоёв, сколько у неё. Так заражается вся комната.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int have = Math.max(1, ctx.stacks(ctx.target, StatusType.PLAGUE));
            List<LivingEntity> near = ctx.enemiesAround(ctx.target.position(), ctx.v("radius")).stream()
                    .filter(e -> e != ctx.target)
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(ctx.target)))
                    .limit((long) ctx.v("targets")).toList();
            for (LivingEntity e : near) {
                for (int i = 0; i < have; i++) infect(ctx, e, "duration", "rot");
                Fx.beam(ctx.level, ctx.target, e, 0.12f, 10, 0x6B8B2A);
            }
            if (near.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Рядом с целью некого заражать.");
            }
            ctx.soundAt(ctx.target.position(), SoundEvents.SPORE_BLOSSOM_FALL, 1.2f, 0.8f);
        }
    }

    /** Увядание: заражённого почти нельзя лечить. */
    public static final class Wither extends Skill {
        public Wither() {
            super("plague.wither", "Увядание", 0x4A5A2A, "⊘", 16, 0);
            blocks("range", 20);
            seconds("duration", 8);
            percent("heal_cut", 0.7);
            damage("damage", 0.683);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона и {duration}: лечение цели срезано на {heal_cut}. Против лекарей "
                    + "в отрядах монстров — самое нужное.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.HEAL_TAKEN, 1 - Math.min(0.95, ctx.v("heal_cut")), "duration");
            Fx.mark(ctx.level, ctx.target, "⊘", ctx.ticks("duration"), 0x4A5A2A);
            ctx.soundAt(ctx.target.position(), SoundEvents.WITHER_SHOOT, 0.6f, 0.8f);
        }
    }

    /** Рой мух: зона, которая заражает и грызёт. */
    public static final class FlySwarm extends Skill {
        public FlySwarm() {
            super("plague.swarm", "Рой мух", 0x5A5A3A, "✻", 24, 0);
            blocks("range", 16);
            blocks("radius", 6);
            seconds("duration", 10);
            damage("damage", 0.205);
            damage("rot", 0.546);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Рой в {radius} на {duration}: каждую секунду {damage} урона и слой заразы всем внутри.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x5A5A3A, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.hit(e, "damage");
                    infect(ctx, e, "duration", "rot");
                }
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0x5A5A3A);
            });
            ctx.soundAt(ctx.point, SoundEvents.BEE_LOOP_AGGRESSIVE, 1.2f, 0.7f);
        }
    }

    /** Обратить: заражённый бьёт своих. */
    public static final class Turn extends Skill {
        public Turn() {
            super("plague.turn", "Обратить", 0x7A5AA8, "⟲", 45, 0);
            blocks("range", 20);
            blocks("search", 10);
            seconds("duration", 6);
            percent("power", 0.75);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Заражённый на {duration} бросается на своих: бьёт ближайшего врага в {search} и наносит "
                    + "на {power} больше урона. Нужна хотя бы одна зараза на цели.";
        }

        @Override
        public void execute(SkillContext ctx) {
            if (ctx.stacks(ctx.target, StatusType.PLAGUE) <= 0) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Цель не заражена — сначала «Мор».");
                return;
            }
            LivingEntity victim = ctx.enemiesAround(ctx.target.position(), ctx.v("search")).stream()
                    .filter(e -> e != ctx.target)
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(ctx.target))).orElse(null);
            if (victim == null) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Рядом нет того, на кого его обратить.");
                return;
            }
            ctx.status(ctx.target, StatusType.DAMAGE_DEALT, 1 + ctx.v("power"), "duration");
            ctx.decoy(ctx.target, victim, "duration");
            Fx.beam(ctx.level, ctx.target, victim, 0.18f, ctx.ticks("duration"), 0x7A5AA8);
            Fx.mark(ctx.level, ctx.target, "⟲", ctx.ticks("duration"), 0x7A5AA8);
            ctx.soundAt(ctx.target.position(), SoundEvents.ZOMBIE_VILLAGER_CURE, 1f, 0.7f);
        }
    }

    /** Пандемия: со всех заражённых разом — по счёту их слоёв. */
    public static final class Pandemic extends Skill {
        public Pandemic() {
            super("plague.pandemic", "Пандемия", 0x2A3A1A, "✹", 104, 0);
            blocks("radius", 14);
            param("per_stack", 0.85, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
            count("spread", 2);
            damage("rot", 0.819);
            seconds("duration", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Каждый заражённый в {radius} получает {per_stack} урона за каждый свой слой заразы, "
                    + "а потом зараза прыгает ещё на {spread} соседей. Главный приём владыки — после того, "
                    + "как комната уже заражена.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var foes = ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"));
            int touched = 0;
            for (LivingEntity e : foes) {
                int stacks = ctx.stacks(e, StatusType.PLAGUE);
                if (stacks <= 0) continue;
                touched++;
                ctx.hitShare(e, ctx.v("per_stack") * stacks);
                Fx.burst(ctx.level, e.position().add(0, 1, 0), 1.4f, 10, 0x2A3A1A);
                List<LivingEntity> near = ctx.enemiesAround(e.position(), 6).stream()
                        .filter(n -> n != e && ctx.stacks(n, StatusType.PLAGUE) == 0)
                        .limit((long) ctx.v("spread")).toList();
                for (LivingEntity n : near) {
                    infect(ctx, n, "duration", "rot");
                    Fx.beam(ctx.level, e, n, 0.12f, 10, 0x6B8B2A);
                }
            }
            if (touched == 0) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Заражённых рядом нет — пандемии не из чего начаться.");
                return;
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 18, 0x2A3A1A);
            ctx.sound(SoundEvents.WITHER_DEATH, 0.7f, 1.3f);
        }
    }
}
