package com.andrey.litrpg.skill.role;

import com.andrey.litrpg.combat.threat.ThreatService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

/** Навыки роли «Танк» — до получения класса. Провокация остаётся и у классов-танков. */
public final class TankSkills {
    private TankSkills() {
    }

    /** Провокация: враг бросает всех и бьёт танка. */
    public static final class Taunt extends Skill {
        public Taunt() {
            super("tank.taunt", "Провокация", Fx.SHIELD, "!", 8, 0);
            blocks("range", 14);
            seconds("duration", 4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Враг бросает всех и бьёт вас {duration}: не отвлечётся, даже если другие нанесут больше урона. "
                    + "После провокации ваша угроза выше всех.";
        }

        @Override
        public void execute(SkillContext ctx) {
            if (!(ctx.target instanceof Mob mob)) return;
            ThreatService.taunt(mob, ctx.caster, ctx.ticks("duration"));
            Fx.beam(ctx.level, ctx.caster, mob, 0.25f, 10, Fx.SHIELD);
            Fx.mark(ctx.level, mob, "!", ctx.ticks("duration"), Fx.SHIELD);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.6f, 1.4f);
        }
    }

    /** Удар щитом: урон и короткое оглушение. */
    public static final class ShieldBash extends Skill {
        public ShieldBash() {
            super("tank.shield_bash", "Удар щитом", 0xC9D6E8, "⛨", 10, 0);
            blocks("range", 4);
            damage("damage", 1.2);
            seconds("stun", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Бьёт врага щитом на {damage} урона и оглушает на {stun}. "
                    + "Сбивает подготовку удара у врагов, которых можно сбить.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.STUN, 1, "stun");
            Fx.burst(ctx.level, ctx.target.position().add(0, ctx.target.getBbHeight() * 0.6, 0), 0.9f, 8, 0xC9D6E8);
            Fx.mark(ctx.level, ctx.target, "✦", ctx.ticks("stun"), 0xFFE27A);
            ctx.level.playSound(null, ctx.target.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1f, 0.7f);
        }
    }

    /** Стойкость: на несколько секунд меньше урона. */
    public static final class Bulwark extends Skill {
        public Bulwark() {
            super("tank.bulwark", "Стойкость", 0x8FB8E8, "▣", 20, 0);
            seconds("duration", 5);
            percent("reduce", 0.525);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "На {duration} получаемый урон снижен на {reduce}. Мгновенно, можно применить под ударом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            Fx.discOn(ctx.level, ctx.caster, 1.2f, ctx.ticks("duration"), Fx.SHIELD, false);
            ctx.level.playSound(null, ctx.caster.blockPosition(), SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.PLAYERS, 1f, 0.8f);
        }
    }
}
