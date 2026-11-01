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

/**
 * Палач — медленно и окончательно. Его тяжёлый замах читается полторы секунды, но его невозможно сбить:
 * ни оглушением, ни движением. «Плаха» делает место, где замах бьёт вдвое, «Приговор исполнен» добивает
 * раненого и обнуляет перезарядку, а «Эшафот» не даёт палача ни сбить с ног, ни замедлить.
 */
public final class HeadsmanSkills {
    private HeadsmanSkills() {
    }

    /** Тяжёлый замах: долгое чтение, которое нельзя прервать, и очень сильный удар. */
    public static final class HeavySwing extends Skill {
        public HeavySwing() {
            super("headsman.swing", "Тяжёлый замах", 0x8B2A2A, "⚒", 10, 1.6);
            blocks("range", 4.5);
            blocks("radius", 3);
            damage("damage", 5.02);
            percent("block_bonus", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public boolean interruptible() {
            return false;
        }

        @Override
        public String template() {
            return "Замах {cast}, который невозможно сбить — ни оглушением, ни движением: {damage} урона цели "
                    + "и всем рядом с ней в {radius}. На «Плахе» бьёт на {block_bonus} больше. "
                    + "Пока стоит «Эшафот», замах без перезарядки — только сам замах и общая перезарядка.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean onBlock = ctx.myProps(com.andrey.litrpg.skill.prop.PropKind.BLOCK, 30).stream()
                    .anyMatch(p -> p.position().distanceTo(ctx.target.position()) <= p.radius());
            double mult = onBlock ? 1 + ctx.v("block_bonus") : 1;
            for (LivingEntity e : ctx.enemiesAround(ctx.target.position(), ctx.v("radius"))) {
                ctx.hitShare(e, ctx.v("damage") * mult);
            }
            Fx.slash(ctx.level, ctx.caster, 3.2f, 90, 10, onBlock ? 0xFF4A2E : 0x8B2A2A);
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), (float) ctx.v("radius"), 10,
                    onBlock ? 0xFF4A2E : 0x8B2A2A);
            ctx.soundAt(ctx.target.position(), SoundEvents.ANVIL_LAND, 1f, 0.6f);
            // «Эшафот»: замах сразу снова готов. Чаще одного раза в {cast} всё равно не выйдет — новый замах
            // не начать, пока читается прежний, а между ними ещё общая перезарядка.
            if (StatusService.magnitudeFrom(ctx.caster, StatusType.STEADFAST, ctx.caster.getUUID()) > 0) ctx.readyNow();
        }
    }

    /** Крюк палача: подтянуть и оглушить. */
    public static final class Hook extends Skill {
        public Hook() {
            super("headsman.hook", "Крюк палача", 0x6B4A2E, "⌐", 16, 0);
            blocks("range", 12);
            damage("damage", 1.255);
            seconds("stun", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Крюк тянет врага к палачу, бьёт на {damage} и оглушает на {stun} — ровно чтобы успеть "
                    + "замахнуться.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.2f, 10, 0x6B4A2E);
            ctx.pull(ctx.target, ctx.caster.position(), 1.2);
            ctx.hit(ctx.target, "damage");
            ctx.status(ctx.target, StatusType.STUN, 1, "stun");
            ctx.sound(SoundEvents.CHAIN_PLACE, 1f, 0.6f);
        }
    }

    /** Колесо: оборот топором по кругу. */
    public static final class Wheel extends Skill {
        public Wheel() {
            super("headsman.wheel", "Колесо", 0xA03A3A, "◍", 12, 0);
            blocks("radius", 4);
            damage("damage", 2.197);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Полный оборот топором: {damage} урона всем врагам в {radius} вокруг палача.";
        }

        @Override
        public void execute(SkillContext ctx) {
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) ctx.hit(e, "damage");
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 10, 0xA03A3A);
            Fx.slash(ctx.level, ctx.caster, (float) ctx.v("radius"), 360, 8, 0xA03A3A);
            ctx.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.1f, 0.5f);
        }
    }

    /** Приговор исполнен: казнь раненого; убил — перезарядка снимается. */
    public static final class Verdict extends Skill {
        public Verdict() {
            super("headsman.verdict", "Приговор исполнен", 0x4A0A0A, "☠", 22, 0);
            blocks("range", 4);
            damage("damage", 3.766);
            percent("threshold", 0.35);
            percent("bonus", 1.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона, а по врагу, у которого меньше {threshold} здоровья, — на {bonus} больше. "
                    + "Казнь удалась — перезарядка снимается сразу.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean low = ctx.target.getHealth() / ctx.target.getMaxHealth() < ctx.v("threshold");
            ctx.hitShare(ctx.target, ctx.v("damage") * (low ? 1 + ctx.v("bonus") : 1));
            Fx.slash(ctx.level, ctx.caster, 2.8f, 60, 10, low ? 0xFF2020 : 0x4A0A0A);
            if (!ctx.target.isAlive() || ctx.target.getHealth() <= 0) {
                ctx.readyNow();
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, net.minecraft.network.chat.Component
                        .literal("Приговор исполнен. Топор снова готов.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.2f, 0.5f);
        }
    }

    /** Плаха: место, где замах палача бьёт вдвое, а враги не могут убежать быстро. */
    public static final class Block extends Skill {
        public Block() {
            super("headsman.block", "Плаха", 0x6B2A2A, "⌗", 40, 0);
            blocks("range", 10);
            blocks("radius", 5);
            shield("seal_hp", 2.0);
            seconds("seal_time", 12);
            percent("slow", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Ставит плаху радиусом {radius} на {seal_time}: враги на ней замедлены на {slow}, "
                    + "а «Тяжёлый замах» по тем, кто стоит на плахе, бьёт вдвое.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double slow = Math.min(0.8, ctx.v("slow"));
            var block = ctx.prop(com.andrey.litrpg.skill.prop.PropKind.BLOCK, ctx.point, "seal_hp", ctx.v("radius"),
                    "seal_time");
            block.onPulse(20, p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                    StatusService.apply(e, Status.of(StatusType.SLOW, "headsman.block", ctx.caster.getUUID(), slow,
                            p.level().getGameTime(), 30));
                }
            });
            Fx.disc(ctx.level, ctx.point, (float) ctx.v("radius"), ctx.ticks("seal_time"), 0x6B2A2A, false);
            ctx.soundAt(ctx.point, SoundEvents.WOOD_PLACE, 1.2f, 0.6f);
        }
    }

    /** Эшафот: палача не сбить с ног и не замедлить. */
    public static final class Scaffold extends Skill {
        public Scaffold() {
            super("headsman.scaffold", "Эшафот", 0x8B6B2A, "⌸", 72, 0);
            seconds("duration", 10);
            percent("reduce", 0.375);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: палача нельзя ни оглушить, ни обездвижить, ни замедлить, и урон по нему меньше "
                    + "на {reduce}, а «Тяжёлый замах» не уходит в перезарядку. Он просто идёт и рубит.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.STEADFAST, 1, "duration");
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            for (StatusType t : StatusType.values()) {
                if (t.controlling()) StatusService.removeAll(ctx.caster, t);
            }
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.caster, 1.4f, dur, 0x8B6B2A, false);
            Fx.mark(ctx.level, ctx.caster, "⌸", dur, 0x8B6B2A);
            ctx.sound(SoundEvents.ANVIL_PLACE, 1f, 0.5f);
        }
    }
}
