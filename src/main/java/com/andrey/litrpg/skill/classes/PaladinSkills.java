package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
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

/**
 * Паладин клятвы — танк святого света. Пока он держит клятву, отряду рядом легче; его удары лечат самого
 * раненого, щит делает его неуязвимым на три секунды, а «Возложение рук» спасает союзника ценой своего
 * здоровья. «Суд света» — одна зона, которая врагов бьёт, а своих лечит.
 */
public final class PaladinSkills {
    private PaladinSkills() {
    }

    /** Клятва света: аура защиты вокруг паладина, пока он жив. */
    public static final class Oath extends Skill {
        public Oath() {
            super("paladin.oath", "Клятва света", 0xFFE27A, "✥", 26, 0);
            blocks("radius", 8);
            seconds("duration", 10);
            percent("reduce", 0.18);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Клятва на {duration}: союзники в {radius} получают на {reduce} меньше урона — пока паладин "
                    + "держится на ногах. Он падёт — клятва кончится.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double factor = 1 - Math.min(0.9, ctx.v("reduce"));
            Fx.discOn(ctx.level, ctx.caster, (float) r, dur, 0xFFE27A, false);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 10, z -> {
                if (!ctx.caster.isAlive()) return;
                for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), r)) {
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_TAKEN, "paladin.oath", ctx.caster.getUUID(),
                            factor, z.level.getGameTime(), 14));
                }
            });
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1f, 1.1f);
        }
    }

    /** Священный удар: бьёт врага и лечит самого раненого союзника. */
    public static final class HolyStrike extends Skill {
        public HolyStrike() {
            super("paladin.strike", "Священный удар", 0xFFF3B0, "✦", 7, 0);
            blocks("range", 4);
            damage("damage", 1.2);
            heal("heal", 1.0);
            blocks("heal_range", 20);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Удар светом на {damage}: самый раненый союзник в {heal_range} получает {heal} здоровья. "
                    + "Паладин лечит, не отходя от боя.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            LivingEntity hurt = ctx.alliesAround(ctx.caster.position(), ctx.v("heal_range")).stream()
                    .filter(a -> a.getHealth() < a.getMaxHealth())
                    .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (hurt != null) {
                ctx.heal(hurt, "heal");
                Fx.beam(ctx.level, ctx.target, hurt, 0.12f, 10, 0xFFF3B0);
            }
            Fx.slash(ctx.level, ctx.caster, 2.4f, 60, 8, 0xFFF3B0);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.2f);
        }
    }

    /** Божественный щит: три секунды полной неуязвимости. */
    public static final class DivineShield extends Skill {
        public DivineShield() {
            super("paladin.divine", "Божественный щит", 0xFFFFFF, "⛊", 90, 0);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration} полной неуязвимости: ни один удар не проходит. Нужен ровно в тот миг, когда "
                    + "хранитель бьёт по разметке.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 0, "duration");
            ctx.status(ctx.caster, StatusType.WARD, 1, "duration");
            int dur = ctx.ticks("duration");
            Fx.pillar(ctx.level, ctx.caster.position(), 1.1f, 4f, dur, 0xFFFFFF);
            Fx.discOn(ctx.level, ctx.caster, 1.5f, dur, 0xFFFFFF, false);
            Fx.domeOn(ctx.level, ctx.caster, 1.4f, dur, 0xFFFFFF);
            ctx.sound(SoundEvents.TOTEM_USE, 1f, 1.4f);
        }
    }

    /** Возложение рук: большое лечение союзника за своё здоровье. */
    public static final class LayOnHands extends Skill {
        public LayOnHands() {
            super("paladin.hands", "Возложение рук", 0xFFE27A, "✚", 60, 0);
            blocks("range", 8);
            heal("heal", 6.0);
            percent("price", 0.25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Отдаёт {price} своего текущего здоровья и возвращает союзнику {heal}. Паладин платит собой — "
                    + "и поэтому успевает там, где лекарь уже не успел.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity who = ctx.target != null ? ctx.target : ctx.caster;
            float hp = ctx.caster.getHealth();
            float price = (float) Math.min(hp - 1, hp * ctx.v("price"));
            if (who != ctx.caster && price > 0) ctx.caster.setHealth(hp - price);
            HealService.heal(ctx.caster, who, ctx.v("heal") * ctx.unit());
            Fx.beam(ctx.level, ctx.caster, who, 0.2f, 14, 0xFFE27A);
            Fx.pillar(ctx.level, who.position(), 0.8f, 3.5f, 16, 0xFFE27A);
            Fx.spiralOn(ctx.level, who, 0.9f, 2.2f, 16, 0xFFE27A);
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 0.9f);
        }
    }

    /** Суд света: одна зона — врагам урон, своим лечение. */
    public static final class Judgment extends Skill {
        public Judgment() {
            super("paladin.judgment", "Суд света", 0xFFFACD, "✺", 45, 0);
            blocks("range", 14);
            blocks("radius", 6);
            seconds("duration", 6);
            damage("damage", 0.5);
            heal("heal", 0.4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Столб света радиусом {radius} на {duration}: каждую секунду врагам внутри — {damage} урона, "
                    + "а союзникам — {heal} здоровья.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0xFFFACD, false);
            Fx.pillar(ctx.level, ctx.point, (float) r, 5f, dur, 0xFFFACD);
            Fx.spiral(ctx.level, ctx.point, (float) r * 0.6f, 3.5f, dur, 0xFFE27A);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) ctx.hit(e, "damage");
                for (LivingEntity a : ctx.alliesAround(z.center, z.radius)) ctx.heal(a, "heal");
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0xFFFACD);
            });
            ctx.soundAt(ctx.point, SoundEvents.BEACON_ACTIVATE, 1f, 1.3f);
        }
    }
}
