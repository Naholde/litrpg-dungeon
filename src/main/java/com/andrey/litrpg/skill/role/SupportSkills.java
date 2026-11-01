package com.andrey.litrpg.skill.role;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;

/** Навыки роли «Поддержка» — до получения класса: читаемое лечение, лечение со временем, свет, который лечит. */
public final class SupportSkills {
    private SupportSkills() {
    }

    public static final class Mend extends Skill {
        public Mend() {
            super("sup.mend", "Исцеление", Fx.HEAL, "✚", 0, 1.5);
            blocks("range", 30);
            heal("heal", 2.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Читается {cast}, стоя на месте: восстанавливает союзнику {heal} здоровья. "
                    + "Цель: выбранный (Ctrl+1…5), под прицелом или вы.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.heal(ctx.target, "heal");
            Fx.pillar(ctx.level, ctx.target.position(), 0.6f, 2.5f, 14, Fx.HEAL);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    public static final class Renew extends Skill {
        public Renew() {
            super("sup.renew", "Обновление", 0x9BF5C8, "❋", 6, 0);
            blocks("range", 30);
            heal("heal", 3.0);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Мгновенно: союзник восстанавливает {heal} здоровья за {duration}, понемногу каждую секунду.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hot(ctx.target, "heal", "duration");
            Fx.discOn(ctx.level, ctx.target, 0.9f, ctx.ticks("duration"), 0x9BF5C8, false);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    public static final class Smite extends Skill {
        public Smite() {
            super("sup.smite", "Священный свет", Fx.HOLY, "☀", 4, 0);
            blocks("range", 25);
            damage("damage", 1.6);
            percent("share", 0.5);
            blocks("heal_range", 20);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Луч света бьёт врага на {damage} урона; {share} нанесённого лечит самого раненого союзника "
                    + "в {heal_range}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            float before = ctx.target.getHealth();
            ctx.hit(ctx.target, "damage");
            float dealt = Math.max(0, before - ctx.target.getHealth());
            Fx.pillar(ctx.level, ctx.target.position(), 0.5f, 4, 10, Fx.HOLY);
            LivingEntity hurt = ctx.alliesAround(ctx.caster.position(), ctx.v("heal_range")).stream()
                    .filter(a -> a.getHealth() < a.getMaxHealth())
                    .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (hurt != null && dealt > 0) {
                HealService.healFixed(ctx.caster, hurt, dealt * ctx.v("share"));
                Fx.beam(ctx.level, ctx.target, hurt, 0.12f, 8, Fx.HOLY);
            }
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.7f, 1.8f);
        }
    }
}
