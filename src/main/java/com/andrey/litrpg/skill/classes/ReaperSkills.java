package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * Жнец — боец, который считает чужие смерти. Души копятся за убитых и за отмеченных: метка смерти отдаёт
 * их жнецу, когда цель падает. Души уходят в «Жатву» и в «Проводника», а «Приговор» через пять секунд
 * повторяет врагу долю всего, что тот успел получить.
 */
public final class ReaperSkills {
    private ReaperSkills() {
    }

    /** Коса: широкий взмах по конусу. */
    public static final class Scythe extends Skill {
        public Scythe() {
            super("reaper.scythe", "Коса", 0x6B7A8B, "⚒", 5, 0);
            blocks("radius", 5);
            param("angle", 150, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 2.757);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Взмах косой по всем перед собой в {radius}: {damage} урона каждому.";
        }

        @Override
        public void execute(SkillContext ctx) {
            for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) ctx.hit(e, "damage");
            Fx.slash(ctx.level, ctx.caster, (float) ctx.v("radius"), (float) ctx.v("angle"), 8, 0x6B7A8B);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.6f);
        }
    }

    /** Серп на цепи: подтянуть врага к себе. */
    public static final class ChainSickle extends Skill {
        public ChainSickle() {
            super("reaper.sickle", "Серп на цепи", 0x8B8B8B, "⌇", 11, 0);
            blocks("range", 15);
            damage("damage", 1.985);
            percent("slow", 0.3);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Серп цепляет врага до {range} и тянет к жнецу: {damage} урона и замедление на {slow} "
                    + "на {duration}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.18f, 12, 0x8B8B8B);
            ctx.pull(ctx.target, ctx.caster.position(), 1.1);
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.SLOW, Math.min(0.8, ctx.v("slow")), "duration");
            ctx.sound(SoundEvents.CHAIN_PLACE, 1f, 0.9f);
        }
    }

    /** Метка смерти: когда отмеченный падёт, жнец получит души и здоровье. */
    public static final class DeathMark extends Skill {
        public DeathMark() {
            super("reaper.mark", "Метка смерти", 0x3A3A4A, "✝", 14, 0);
            blocks("range", 20);
            seconds("duration", 15);
            count("souls", 25);
            percent("extra", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Метка на {duration}: цель получает на {extra} больше урона, а когда падёт — жнец получит "
                    + "{souls} душ и немного здоровья. Метьте того, кто точно умрёт.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.target, StatusType.MARK, ctx.v("souls"), "duration");
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            Fx.mark(ctx.level, ctx.target, "✝", ctx.ticks("duration"), 0x3A3A4A);
            ctx.soundAt(ctx.target.position(), SoundEvents.SOUL_ESCAPE.value(), 1f, 0.7f);
        }
    }

    /** Жатва: все души — в удар по площади. */
    public static final class Harvest extends Skill {
        public Harvest() {
            super("reaper.harvest", "Жатва", 0x4A5A6B, "✹", 24, 0);
            blocks("radius", 6);
            count("cost", 20);
            param("per_soul", 0.03, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Тратит все души (нужно хотя бы {cost}): всем врагам в {radius} по {per_soul} за каждую душу. "
                    + "Чем больше вы собрали, тем шире жатва.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double souls = ctx.v("cost") + ctx.spendAllResource();
            double share = ctx.v("per_soul") * souls;
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) ctx.hitShare(e, share);
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 12, 0x4A5A6B);
            Fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), 2f, 10, 0x4A5A6B);
            ctx.sound(SoundEvents.WITHER_SHOOT, 0.8f, 0.7f);
        }
    }

    /** Проводник: за души — скорость и неуязвимость к замедлению. */
    public static final class Ferryman extends Skill {
        public Ferryman() {
            super("reaper.ferryman", "Проводник", 0xBFE8FF, "≫", 35, 0);
            count("cost", 25);
            seconds("duration", 8);
            percent("speed", 0.875);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} душ: {duration} жнец быстрее на {speed}, и его нельзя ни оглушить, ни замедлить, "
                    + "ни сковать.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.SPEED, ctx.v("speed"), "duration");
            ctx.status(ctx.caster, StatusType.STEADFAST, 1, "duration");
            for (StatusType t : StatusType.values()) {
                if (t.controlling()) StatusService.removeAll(ctx.caster, t);
            }
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.caster, 1.2f, dur, 0xBFE8FF, false);
            Fx.mark(ctx.level, ctx.caster, "≫", dur, 0xBFE8FF);
            ctx.sound(SoundEvents.SOUL_ESCAPE.value(), 1f, 1.3f);
        }
    }

    /** Приговор: через несколько секунд врагу возвращается доля всего, что он получил. */
    public static final class Sentence extends Skill {
        public Sentence() {
            super("reaper.sentence", "Приговор", 0x2A2A3A, "⌛", 64, 0);
            blocks("range", 20);
            seconds("duration", 5);
            percent("share", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Приговор на {duration}: жнец ведёт счёт всему урону по этой цели, а когда время выйдет — "
                    + "цель получает {share} этого числа ещё раз. Бейте её всей группой.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity victim = ctx.target;
            ctx.status(victim, StatusType.RECORD, 0, "duration");
            int dur = ctx.ticks("duration");
            Fx.mark(ctx.level, victim, "⌛", dur, 0x9B5CFF);
            Fx.discOn(ctx.level, victim, 1.3f, dur, 0x2A2A3A, false);
            double share = ctx.v("share");
            ctx.later(dur - 1, () -> {
                double sum = StatusService.sum(victim, StatusType.RECORD);
                StatusService.removeAll(victim, StatusType.RECORD);
                if (!victim.isAlive() || sum <= 0) return;
                com.andrey.litrpg.combat.DamageService.deal(ctx.caster, null, victim, sum * share,
                        com.andrey.litrpg.combat.WeaponKind.FIXED);
                Fx.pillar(ctx.level, victim.position(), 1.2f, 5f, 16, 0x9B5CFF);
                Fx.burst(ctx.level, victim.position().add(0, 1, 0), 2f, 12, 0x2A2A3A);
                ctx.soundAt(victim.position(), SoundEvents.WITHER_BREAK_BLOCK, 1f, 0.6f);
            });
            ctx.soundAt(victim.position(), SoundEvents.AMETHYST_CLUSTER_PLACE, 1f, 0.6f);
        }
    }
}
