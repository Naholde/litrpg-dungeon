package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
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
import net.minecraft.world.phys.Vec3;

/**
 * Тень — боец из темноты. Он не стоит в строю: заходит за спину, бьёт отравленным клинком, прячется в
 * невидимость (враги теряют его из виду) и добивает раненых — убил, и «Перерезать» снова готово.
 * Ресурс «тень» копится за убитых.
 */
public final class ShadowSkills {
    private ShadowSkills() {
    }

    /** Из темноты: телепорт за спину и удар, который со спины вдвое больнее. */
    public static final class FromDark extends Skill {
        public FromDark() {
            super("shadow.from_dark", "Из темноты", 0x9B5CFF, "➹", 10, 0);
            blocks("range", 14);
            damage("damage", 1.3);
            percent("behind", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Шаг сквозь тень за спину врага (до {range}) и удар на {damage}; со спины — на {behind} больше.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.teleport(ctx.behind(ctx.target, 1.4));
            ctx.caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
                    ctx.target.position().add(0, 1, 0));
            boolean back = ctx.fromBehind(ctx.target);
            ctx.hitShare(ctx.target, ctx.v("damage") * (back ? 1 + ctx.v("behind") : 1));
            Fx.slash(ctx.level, ctx.caster, 2.2f, 50, 6, 0x9B5CFF);
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.3f);
        }
    }

    /** Веер ножей: несколько ножей по конусу. */
    public static final class KnifeFan extends Skill {
        public KnifeFan() {
            super("shadow.knives", "Веер ножей", 0xB8A6FF, "⁂", 9, 0);
            blocks("radius", 9);
            param("angle", 70, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            damage("damage", 0.75);
            count("knives", 5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "{knives} ножей веером на {radius}: каждому врагу в секторе {damage} урона. "
                    + "Если врагов мало — все ножи достанутся им.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var foes = ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"));
            int knives = (int) Math.round(ctx.v("knives"));
            if (!foes.isEmpty()) {
                for (int i = 0; i < knives; i++) {
                    LivingEntity e = foes.get(i % foes.size());
                    ctx.hit(e, "damage");
                    Fx.beam(ctx.level, ctx.caster.position().add(0, 1.2, 0), e.position().add(0, 1, 0), 0.08f, 6,
                            0xB8A6FF);
                }
            }
            Fx.cone(ctx.level, ctx.caster.position(), ctx.caster.getViewVector(1).multiply(1, 0, 1),
                    (float) ctx.v("radius"), (float) ctx.v("angle"), 8, 0xB8A6FF, false);
            ctx.sound(SoundEvents.TRIDENT_THROW.value(), 0.8f, 1.6f);
        }
    }

    /** Покров: невидимость, враги теряют цель, а первый удар после — сильнее. */
    public static final class Cloak extends Skill {
        public Cloak() {
            super("shadow.cloak", "Покров", 0x4A3A6B, "◖", 28, 0);
            seconds("duration", 5);
            percent("ambush", 0.8);
            seconds("ambush_time", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration} невидимости: враги рядом теряют тень из виду и забывают про неё. "
                    + "Следующие {ambush_time} удары тени сильнее на {ambush}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.invisible("duration");
            ctx.status(ctx.caster, StatusType.DAMAGE_DEALT, 1 + ctx.v("ambush"), "ambush_time");
            Fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), 1.4f, 10, 0x4A3A6B);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
        }
    }

    /** Ночь: зона тьмы — враги внутри никого не видят, союзники бьются мимо. */
    public static final class Night extends Skill {
        public Night() {
            super("shadow.night", "Ночь", 0x1A0A2A, "☾", 50, 0);
            blocks("range", 14);
            blocks("radius", 7);
            seconds("duration", 6);
            percent("miss", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Тьма радиусом {radius} на {duration}: враги внутри теряют цель и ищут её заново, "
                    + "а удары по союзникам внутри проходят мимо с вероятностью {miss}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double miss = Math.min(0.8, ctx.v("miss"));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x1A0A2A, false);
            Fx.pillar(ctx.level, ctx.point, (float) r, 4f, dur, 0x1A0A2A);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                for (LivingEntity a : ctx.alliesAround(z.center, z.radius)) {
                    StatusService.apply(a, Status.of(StatusType.BLUR, "shadow.night", ctx.caster.getUUID(), miss,
                            z.level.getGameTime(), 30));
                    if (a instanceof ServerPlayer sp) {
                        com.andrey.litrpg.combat.threat.ThreatService.forget(sp, z.radius + 2);
                    }
                }
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    if (e instanceof net.minecraft.world.entity.Mob m) m.setTarget(null);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.AMBIENT_CAVE.value(), 1f, 0.6f);
        }
    }

    /** Яд на клинке: отрава на цели, и по отравленным тень бьёт сильнее. */
    public static final class BladePoison extends Skill {
        public BladePoison() {
            super("shadow.poison", "Яд на клинке", 0x6BE38A, "☣", 16, 0);
            blocks("range", 4);
            damage("damage", 2.4);
            seconds("duration", 8);
            percent("extra", 0.25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Отравленный клинок: {damage} урона за {duration} и всё это время цель получает "
                    + "на {extra} больше урона. Яд работает, пока тень обходит остальных.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.dot(ctx.target, "damage", "duration");
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            Fx.mark(ctx.level, ctx.target, "☣", ctx.ticks("duration"), 0x6BE38A);
            ctx.soundAt(ctx.target.position(), SoundEvents.BOTTLE_EMPTY, 1f, 0.8f);
        }
    }

    /** Перерезать: добить раненого — и если получилось, навык сразу готов снова. */
    public static final class Cut extends Skill {
        public Cut() {
            super("shadow.cut", "Перерезать", 0x8B0000, "☠", 25, 0);
            blocks("range", 4);
            damage("damage", 2.2);
            percent("threshold", 0.3);
            percent("bonus", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона, а по врагу, у которого меньше {threshold} здоровья, — на {bonus} больше. "
                    + "Добил — перезарядка снимается, и можно резать следующего.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean low = ctx.target.getHealth() / ctx.target.getMaxHealth() < ctx.v("threshold");
            ctx.hitShare(ctx.target, ctx.v("damage") * (low ? 1 + ctx.v("bonus") : 1));
            Fx.slash(ctx.level, ctx.caster, 2.4f, 40, 8, low ? 0xFF2020 : 0x8B0000);
            boolean dead = !ctx.target.isAlive() || ctx.target.getHealth() <= 0;
            if (dead) {
                ctx.readyNow();
                Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 1.2f, 10, 0x8B0000);
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, net.minecraft.network.chat.Component
                        .literal("Перерезано. Навык снова готов.").withStyle(net.minecraft.ChatFormatting.DARK_RED));
            }
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.1f, 0.7f);
        }
    }

    /** Точка позади цели — вынесено для тестов. */
    public static Vec3 behindOf(SkillContext ctx, LivingEntity target, double dist) {
        return ctx.behind(target, dist);
    }
}
