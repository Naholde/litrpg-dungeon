package com.andrey.litrpg.skill.role;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

/** Навыки роли «Боец» — до получения класса: удар по одной цели, по всем вокруг и издалека. */
public final class DamageSkills {
    private DamageSkills() {
    }

    public static final class HeavyStrike extends Skill {
        public HeavyStrike() {
            super("dps.heavy_strike", "Мощный удар", Fx.BLOOD, "⚔", 6, 0);
            blocks("range", 4);
            damage("damage", 2.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Сокрушительный удар по врагу вблизи: {damage} урона.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            Fx.slash(ctx.level, ctx.caster, 2.6f, 110, 8, Fx.BLOOD);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);
        }
    }

    public static final class Whirlwind extends Skill {
        public Whirlwind() {
            super("dps.whirlwind", "Вихрь", 0xE8C9A0, "✺", 9, 0);
            blocks("radius", 4);
            damage("damage", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Круговой удар: {damage} урона всем врагам в {radius} вокруг.";
        }

        @Override
        public void execute(SkillContext ctx) {
            float r = (float) ctx.v("radius");
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), r)) ctx.hit(e, "damage");
            Fx.ring(ctx.level, ctx.caster.position(), r, 10, 0xE8C9A0);
            Fx.slash(ctx.level, ctx.caster, r * 0.8f, 360, 8, 0xE8C9A0);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.9f);
        }
    }

    public static final class ThrowingKnife extends Skill {
        public ThrowingKnife() {
            super("dps.throwing_knife", "Метательный нож", 0xB0B8C8, "➶", 7, 0);
            blocks("range", 20);
            damage("damage", 1.4);
            percent("slow", 0.3);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Нож в цель до {range}: {damage} урона и замедление на {slow} на {duration}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.SLOW, Math.min(0.8, ctx.v("slow")), "duration");
            Fx.beam(ctx.level, ctx.caster.getEyePosition(), ctx.target.position().add(0, ctx.target.getBbHeight() * 0.6, 0),
                    0.08f, 4, 0xB0B8C8);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
        }
    }
}
