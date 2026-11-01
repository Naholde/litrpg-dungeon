package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.History;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;

/**
 * Хронолог — поддержка временем. Он не лечит раны, а отменяет их: возвращает союзнику здоровье, которое было
 * пять секунд назад, останавливает врага в стазисе, ускоряет своих и ставит на союзника петлю, которая
 * отбрасывает его назад вместо смерти. Ресурс — песок, он сыпется сам.
 */
public final class ChronologSkills {
    private ChronologSkills() {
    }

    /** Полный откат: здоровье союзника — как было несколько секунд назад. */
    public static final class Rewind extends Skill {
        public Rewind() {
            super("chrono.rewind", "Полный откат", 0xFFE27A, "↺", 5, 0);
            blocks("range", 30);
            seconds("back", 5);
            heal("heal", 7.0);
            count("cost", 30);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "За {cost} песка: у союзника снова столько здоровья, сколько было {back} назад, "
                    + "но не меньше чем +{heal}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity who = ctx.target != null ? ctx.target : ctx.caster;
            if (!(who instanceof ServerPlayer sp)) return;
            double then = History.healthAgo(sp, ctx.v("back"));
            double back = Math.max(then - sp.getHealth(), ctx.v("heal") * ctx.unit());
            if (back > 0) HealService.healFixed(ctx.caster, sp, back);
            Fx.pillar(ctx.level, sp.position(), 0.9f, 3.5f, 20, 0xFFE27A);
            Fx.mark(ctx.level, sp, "↺", 20, 0xFFE27A);
            ctx.soundAt(sp.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.7f);
        }
    }

    /** Стоп-кадр: враг застыл — и не бьёт, и не получает урона. */
    public static final class Stasis extends Skill {
        public Stasis() {
            super("chrono.stasis", "Стоп-кадр", 0xD6F3FF, "⏸", 22, 0);
            blocks("range", 20);
            seconds("duration", 3);
            count("cost", 25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "За {cost} песка останавливает врага на {duration}: он не двигается и не бьёт, но и урон по нему "
                    + "не проходит. Нужен, чтобы выключить одного и разобраться с другими.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.target, StatusType.STUN, 1, "duration");
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 0, "duration");
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.target, 1.2f, dur, 0xD6F3FF, false);
            Fx.mark(ctx.level, ctx.target, "⏸", dur, 0xD6F3FF);
            ctx.soundAt(ctx.target.position(), SoundEvents.GLASS_PLACE, 1f, 1.8f);
        }
    }

    /** Ускорить: союзники рядом двигаются быстрее и реже ждут перезарядки. */
    public static final class Haste extends Skill {
        public Haste() {
            super("chrono.haste", "Ускорить", 0xB8F5E0, "≫", 20, 0);
            blocks("radius", 10);
            seconds("duration", 6);
            percent("speed", 0.625);
            percent("haste", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: союзники в {radius} бегут на {speed} быстрее, а их перезарядки короче на {haste}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                ctx.status(a, StatusType.SPEED, ctx.v("speed"), "duration");
                ctx.status(a, StatusType.HASTE, ctx.v("haste"), "duration");
                Fx.mark(ctx.level, a, "≫", dur, 0xB8F5E0);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 14, 0xB8F5E0);
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 0.9f, 1.6f);
        }
    }

    /**
     * Петля времени — пассивная: хронолог всё время следит за группой. Кто-то упал ниже порога —
     * его само́й собой отбрасывает назад по здоровью, и петля закрывается до конца перезарядки.
     */
    public static final class Loop extends Skill {
        public Loop() {
            super("chrono.loop", "Петля времени", 0xFFD27A, "∞", 70, 0);
            blocks("radius", 25);
            percent("threshold", 0.2);
            seconds("back", 5);
            percent("least", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public boolean passive() {
            return true;
        }

        @Override
        public String template() {
            return "Работает сама: как только у вас или у союзника в {radius} остаётся меньше {threshold} здоровья, "
                    + "время для него откатывается на {back} назад — но не ниже {least} его здоровья. "
                    + "После этого петля закрывается до конца перезарядки.";
        }

        @Override
        public void execute(SkillContext ctx) {
            // Пассивный навык: с панели не применяется, вся работа — в passiveTick.
        }

        @Override
        public boolean passiveTick(SkillContext ctx) {
            double threshold = ctx.v("threshold"), least = ctx.v("least"), back = ctx.v("back");
            LivingEntity worst = ctx.alliesAround(ctx.caster.position(), ctx.v("radius")).stream()
                    .filter(a -> a.isAlive() && a.getHealth() < a.getMaxHealth() * threshold)
                    .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth())).orElse(null);
            if (!(worst instanceof ServerPlayer sp)) return false;
            double want = Math.max(History.healthAgo(sp, back), sp.getMaxHealth() * least);
            double heal = want - sp.getHealth();
            if (heal <= 0) return false;
            HealService.healFixed(ctx.caster, sp, heal);
            Fx.pillar(ctx.level, sp.position(), 1.2f, 4.5f, 24, 0xFFD27A);
            Fx.mark(ctx.level, sp, "∞", 30, 0xFFD27A);
            Fx.beam(ctx.level, ctx.caster, sp, 0.18f, 14, 0xFFD27A);
            ctx.soundAt(sp.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.6f);
            String who = sp == ctx.caster ? "вас" : sp.getGameProfile().getName();
            com.andrey.litrpg.core.SystemChat.send(ctx.caster, net.minecraft.network.chat.Component
                    .literal("Петля времени отбросила " + who + " назад.")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
            return true;
        }
    }

    /** Песочные часы: в зоне время идёт по-разному для своих и чужих. */
    public static final class Hourglass extends Skill {
        public Hourglass() {
            super("chrono.hourglass", "Песочные часы", 0xFFE8A6, "⧖", 50, 0);
            blocks("range", 16);
            blocks("radius", 6);
            seconds("duration", 8);
            percent("slow", 0.45);
            percent("haste", 0.375);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Зона радиусом {radius} на {duration}: враги внутри медленнее на {slow}, союзники — с "
                    + "перезарядками короче на {haste}. Время в ней идёт по-разному.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.8, ctx.v("slow")), haste = ctx.v("haste");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0xFFE8A6, false);
            Fx.pillar(ctx.level, ctx.point, (float) r, 4f, dur, 0xFFE8A6);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 10, z -> {
                long now = z.level.getGameTime();
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    StatusService.apply(e, Status.of(StatusType.SLOW, "chrono.hourglass", ctx.caster.getUUID(), slow,
                            now, 14));
                }
                for (LivingEntity a : ctx.alliesAround(z.center, z.radius)) {
                    StatusService.apply(a, Status.of(StatusType.HASTE, "chrono.hourglass", ctx.caster.getUUID(), haste,
                            now, 14));
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.SAND_PLACE, 1.1f, 0.8f);
        }
    }

    /** Перемотка: группе возвращается часть того, что она потеряла за последние секунды. */
    public static final class Replay extends Skill {
        public Replay() {
            super("chrono.replay", "Перемотка", 0xFFFFFF, "⟲", 120, 0);
            blocks("radius", 25);
            seconds("back", 4);
            percent("share", 0.7);
            count("cost", 40);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} песка перематывает последние {back} для всей группы в {radius}: каждый получает "
                    + "{share} того здоровья, что потерял за это время. Чем страшнее был удар, тем больше вернётся.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double share = ctx.v("share"), back = ctx.v("back");
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                if (!(a instanceof ServerPlayer sp)) continue;
                double lost = History.healthAgo(sp, back) - sp.getHealth();
                if (lost > 0) HealService.healFixed(ctx.caster, sp, lost * share);
                Fx.pillar(ctx.level, sp.position(), 0.8f, 4f, 20, 0xFFFFFF);
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 20, 0xFFE27A);
            ctx.sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.3f);
        }
    }
}
