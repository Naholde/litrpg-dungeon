package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Берсерк — чем больнее, тем злее. Ярость копится от ударов (своих и по себе) и тратится на самые сильные приёмы;
 * размах бьёт сильнее, когда у берсерка мало здоровья.
 */
public final class BerserkerSkills {
    private BerserkerSkills() {
    }

    /** Размах: удар по всем перед собой, сильнее при низком здоровье. */
    public static final class Cleave extends Skill {
        public Cleave() {
            super("berserker.cleave", "Размах", 0xD8343A, "⚔", 4, 0);
            blocks("radius", 4);
            param("angle", 120, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 1.185);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Размашистый удар по всем врагам перед собой в {radius}: {damage} урона. "
                    + "Чем меньше у вас здоровья, тем сильнее: при половине — в 1,5 раза, почти при нуле — вдвое.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double missing = 1 - ctx.caster.getHealth() / ctx.caster.getMaxHealth();
            double share = ctx.v("damage") * (1 + missing);
            for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) ctx.hitShare(e, share);
            Fx.slash(ctx.level, ctx.caster, (float) ctx.v("radius"), (float) ctx.v("angle"), 8, 0xD8343A);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        }
    }

    /** Рваная рана: урон со временем. */
    public static final class Rend extends Skill {
        public Rend() {
            super("berserker.rend", "Рваная рана", 0xA01818, "≋", 8, 0);
            blocks("range", 4);
            damage("damage", 2.188);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Рвёт врага: рана наносит {damage} урона за {duration}. Сила раны считается в момент удара.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.dot(ctx.target, "damage", "duration");
            Fx.slash(ctx.level, ctx.caster, 2.2f, 60, 6, 0xA01818);
            Fx.mark(ctx.level, ctx.target, "≋", ctx.ticks("duration"), 0xD8343A);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.6f);
        }
    }

    /** Прыжок: в точку, при приземлении — удар по площади и замедление. Падение в прыжке не ранит. */
    public static final class Leap extends Skill {
        public Leap() {
            super("berserker.leap", "Прыжок", 0xE07A3A, "⤴", 14, 0);
            blocks("range", 12);
            blocks("radius", 3);
            damage("damage", 1.094);
            percent("slow", 0.4);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Прыгает в точку до {range}. При приземлении — {damage} урона всем в {radius} и замедление на {slow} "
                    + "на {duration}. Падение в прыжке не ранит.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 from = ctx.caster.position(), to = ctx.point;
            Vec3 flat = to.subtract(from).multiply(1, 0, 1);
            double d = flat.length();
            // Баллистика: время полёта ~ 0,9 с, подъём 1,2 блока над прямой.
            double t = 18;
            Vec3 v = flat.scale(1 / t).add(0, 0.55 + (to.y - from.y) / t, 0);
            ctx.caster.setDeltaMovement(v.x * 1.25, v.y, v.z * 1.25);
            ctx.caster.hurtMarked = true;
            Fx.disc(ctx.level, to, (float) ctx.v("radius"), 18, 0xE07A3A, true);
            ctx.onLanding(() -> {
                Vec3 at = ctx.caster.position();
                for (LivingEntity e : ctx.enemiesAround(at, ctx.v("radius"))) {
                    ctx.hit(e, "damage");
                    ctx.status(e, StatusType.SLOW, Math.min(0.8, ctx.v("slow")), "duration");
                }
                Fx.ring(ctx.level, at, (float) ctx.v("radius") + 0.5f, 10, 0xE07A3A);
                ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.3f);
            });
            if (d < 0.5) ctx.caster.setDeltaMovement(0, 0.6, 0);
        }
    }

    /** Исступление: за ярость — вампиризм и больше урона. */
    public static final class Frenzy extends Skill {
        public Frenzy() {
            super("berserker.frenzy", "Исступление", 0xFF4A2E, "♨", 30, 0);
            count("cost", 40);
            seconds("duration", 8);
            percent("leech", 0.3);
            percent("bonus", 0.225);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} ярости: на {duration} урон выше на {bonus}, а {leech} нанесённого урона лечит вас.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.LEECH, ctx.v("leech"), "duration");
            ctx.status(ctx.caster, StatusType.DAMAGE_DEALT, 1 + ctx.v("bonus"), "duration");
            Fx.discOn(ctx.level, ctx.caster, 1.1f, ctx.ticks("duration"), 0xFF4A2E, false);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.9f, 0.9f);
        }
    }

    /** Добить: сильный удар, вдвое сильнее по раненым. */
    public static final class Execute extends Skill {
        public Execute() {
            super("berserker.execute", "Добить", 0x8B0000, "☠", 8, 0);
            blocks("range", 4);
            count("cost", 30);
            damage("damage", 1.823);
            param("threshold", 0.3, com.andrey.litrpg.skill.SkillParam.Unit.PERCENT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "За {cost} ярости: {damage} урона; по врагу, у которого меньше {threshold} здоровья, — вдвое больше.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean low = ctx.target.getHealth() / ctx.target.getMaxHealth() < ctx.v("threshold");
            ctx.hitShare(ctx.target, ctx.v("damage") * (low ? 2 : 1));
            Fx.slash(ctx.level, ctx.caster, 2.4f, 40, 8, low ? 0xFF2020 : 0x8B0000);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.5f);
        }
    }

    /** Кровавая баня: вся ярость — в удар по площади. */
    public static final class Bloodbath extends Skill {
        public Bloodbath() {
            super("berserker.bloodbath", "Кровавая баня", 0xB0001A, "✹", 36, 0);
            blocks("radius", 5);
            count("min_rage", 20);
            param("per_rage", 0.05, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE, com.andrey.litrpg.skill.SkillParam.Scale.POWER);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Сжигает всю ярость (нужно хотя бы {min_rage}): каждая единица — {per_rage} урона всем врагам в {radius}. "
                    + "При полной ярости — сильнейший удар берсерка.";
        }

        @Override
        public double cost(int rank) {
            return value("min_rage", rank);
        }

        @Override
        public void execute(SkillContext ctx) {
            double rage = ctx.spendAllResource() + ctx.v("min_rage"); // цена уже списана — вернуть её в расчёт
            double share = ctx.v("per_rage") * rage;
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) ctx.hitShare(e, share);
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 12, 0xB0001A);
            Fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), (float) ctx.v("radius") * 0.7f, 10, 0xB0001A);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.PLAYERS, 0.7f, 1.3f);
        }
    }
}
