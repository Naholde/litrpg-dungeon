package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import com.andrey.litrpg.combat.HealService;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * Лекарь — эталон поддержки: быстрая повязка, сильнее по раненым; долгая молитва; святилище на земле;
 * «Чудо» — спасает всю группу разом.
 */
public final class ClericSkills {
    private ClericSkills() {
    }

    /** Повязка: мгновенное лечение, тем сильнее, чем сильнее ранен союзник. */
    public static final class Bandage extends Skill {
        public Bandage() {
            super("cleric.bandage", "Повязка", 0x9BF5C8, "✚", 3, 0);
            blocks("range", 30);
            heal("heal", 1.323);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Мгновенно лечит союзника на {heal}; чем сильнее он ранен, тем больше — почти при нуле вдвое.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double missing = 1 - ctx.target.getHealth() / ctx.target.getMaxHealth();
            HealService.heal(ctx.caster, ctx.target, ctx.v("heal") * ctx.unit() * (1 + missing));
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 0.8f, 8, 0x9BF5C8);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 1.3f);
        }
    }

    /** Молитва: долгое чтение, большое лечение. */
    public static final class Prayer extends Skill {
        public Prayer() {
            super("cleric.prayer", "Молитва", 0xFFE27A, "☥", 0, 2.0);
            blocks("range", 30);
            heal("heal", 2.939);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Читается {cast}, стоя на месте: восстанавливает союзнику {heal} здоровья — спасает танка под ударом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.heal(ctx.target, "heal");
            Fx.pillar(ctx.level, ctx.target.position(), 0.8f, 4, 18, 0xFFE27A);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1f);
        }
    }

    /** Святилище: место на земле — лечит и защищает стоящих внутри. */
    public static final class Sanctuary extends Skill {
        public Sanctuary() {
            super("cleric.sanctuary", "Святилище", 0xFFF3B0, "✧", 30, 0);
            blocks("range", 20);
            blocks("radius", 5);
            seconds("duration", 8);
            heal("heal", 0.258);
            percent("reduce", 0.225);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Святилище радиусом {radius} на {duration}: союзники внутри каждую секунду восстанавливают {heal} "
                    + "и получают на {reduce} меньше урона.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double factor = 1 - Math.min(0.9, ctx.v("reduce"));
            double heal = ctx.v("heal") * ctx.unit();
            int dur = ctx.ticks("duration");
            Fx.disc(ctx.level, ctx.point, (float) ctx.v("radius"), dur, 0xFFF3B0, false);
            Zones.add(ctx.level, ctx.point, ctx.v("radius"), ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity a : ctx.alliesAround(z.center, z.radius)) {
                    HealService.heal(ctx.caster, a, heal);
                    StatusService.apply(a, Status.of(StatusType.DAMAGE_TAKEN, "cleric.sanctuary", ctx.caster.getUUID(),
                            factor, z.level.getGameTime(), 24));
                }
                Fx.ring(z.level, z.center, (float) z.radius, 14, 0xFFF3B0);
            });
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 1.6f);
        }
    }

    /** Чудо: вся группа рядом — большое лечение и щит. */
    public static final class Miracle extends Skill {
        public Miracle() {
            super("cleric.miracle", "Чудо", 0xFFFFFF, "✺", 144, 0);
            blocks("radius", 30);
            heal("heal", 3.674);
            shield("shield", 1.102);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Вся группа в {radius}: {heal} здоровья и щит на {shield} на {duration}. Раз в бою спасает всех.";
        }

        @Override
        public void execute(SkillContext ctx) {
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                ctx.heal(a, "heal");
                ctx.shield(a, "shield", "duration");
                Fx.pillar(ctx.level, a.position(), 0.7f, 5, 20, 0xFFFFFF);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 20, 0xFFE27A);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
    }
}
