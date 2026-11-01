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
 * Теневой жрец — лекарь, который лечит тем, что отнял. «Искупление» переводит весь его урон в здоровье
 * отмеченного союзника, «Сумерки» бьют врагов и лечат своих в одной зоне, «Сделка» платит его собственной
 * кровью, а «Пожирание тени» забирает здоровье у всех врагов разом и делит его на группу.
 */
public final class ShadowPriestSkills {
    private ShadowPriestSkills() {
    }

    /** Теневая плеть: рана, которая тлеет и кормит сумерки. */
    public static final class Lash extends Skill {
        public Lash() {
            super("priest.lash", "Теневая плеть", 0x6B4AA8, "≀", 6, 0);
            blocks("range", 22);
            damage("damage", 2.2);
            seconds("duration", 6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Плеть тьмы: {damage} урона за {duration}. Пока рана тлеет, жрец копит сумерки — "
                    + "и с них лечит отмеченных.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.dot(ctx.target, "damage", "duration");
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.16f, 10, 0x6B4AA8);
            Fx.mark(ctx.level, ctx.target, "≀", ctx.ticks("duration"), 0x6B4AA8);
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        }
    }

    /** Искупление: весь урон жреца лечит отмеченного союзника. */
    public static final class Atonement extends Skill {
        public Atonement() {
            super("priest.atonement", "Искупление", 0xB8A6FF, "✛", 12, 0);
            blocks("range", 30);
            seconds("duration", 10);
            percent("share", 0.45);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Метка на союзнике на {duration}: {share} любого урона, который наносит жрец, становится "
                    + "его здоровьем. Жрец лечит, пока бьёт.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity who = ctx.target != null ? ctx.target : ctx.caster;
            ctx.status(who, StatusType.ATONEMENT, ctx.v("share"), "duration");
            Fx.beam(ctx.level, ctx.caster, who, 0.14f, ctx.ticks("duration"), 0xB8A6FF);
            Fx.mark(ctx.level, who, "✛", ctx.ticks("duration"), 0xB8A6FF);
            ctx.soundAt(who.position(), SoundEvents.SOUL_ESCAPE.value(), 0.8f, 1.4f);
        }
    }

    /** Сумерки: зона, в которой чужая боль становится своим здоровьем. */
    public static final class Twilight extends Skill {
        public Twilight() {
            super("priest.twilight", "Сумерки", 0x4A3A6B, "☾", 28, 0);
            blocks("range", 18);
            blocks("radius", 6);
            seconds("duration", 8);
            damage("damage", 0.45);
            percent("share", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Сумерки радиусом {radius} на {duration}: каждую секунду врагам внутри — {damage} урона, "
                    + "а {share} снятого здоровья делится между союзниками внутри.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double share = ctx.v("share");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x4A3A6B, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                double taken = 0;
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    float before = e.getHealth();
                    ctx.hit(e, "damage");
                    taken += Math.max(0, before - e.getHealth());
                }
                var allies = ctx.alliesAround(z.center, z.radius);
                if (taken > 0 && !allies.isEmpty()) {
                    double each = taken * share / allies.size();
                    for (LivingEntity a : allies) {
                        HealService.healFixed(ctx.caster, a, each);
                        Fx.beam(z.level, z.center, a.position().add(0, 1, 0), 0.1f, 10, 0xB8A6FF);
                    }
                }
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0x4A3A6B);
            });
            ctx.soundAt(ctx.point, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 1f, 1.2f);
        }
    }

    /** Сделка: своё здоровье — вдвое чужим. */
    public static final class Bargain extends Skill {
        public Bargain() {
            super("priest.bargain", "Сделка", 0x8B1A8B, "⚖", 24, 0);
            blocks("radius", 25);
            percent("price", 0.2);
            percent("profit", 2.0);
            count("cost", 20);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} сумерек и {price} своего здоровья: самый раненый союзник в {radius} получает "
                    + "{profit} от отданного. Жрец платит собой — и платит охотно.";
        }

        @Override
        public void execute(SkillContext ctx) {
            float hp = ctx.caster.getHealth();
            float price = (float) Math.min(hp - 1, hp * ctx.v("price"));
            LivingEntity hurt = ctx.alliesAround(ctx.caster.position(), ctx.v("radius")).stream()
                    .filter(a -> a != ctx.caster && a.getHealth() < a.getMaxHealth())
                    .min(Comparator.comparingDouble(a -> a.getHealth() / a.getMaxHealth()))
                    .orElse(ctx.caster);
            if (hurt != ctx.caster && price > 0) {
                ctx.caster.setHealth(hp - price);
                HealService.healFixed(ctx.caster, hurt, price * ctx.v("profit"));
            } else {
                HealService.healFixed(ctx.caster, ctx.caster, price * ctx.v("profit") * 0.5);
            }
            Fx.beam(ctx.level, ctx.caster, hurt, 0.2f, 14, 0x8B1A8B);
            Fx.pillar(ctx.level, hurt.position(), 0.8f, 3.5f, 16, 0xB8A6FF);
            ctx.sound(SoundEvents.ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        }
    }

    /** Покров тьмы: щит, который лопается ударом по площади. */
    public static final class Veil extends Skill {
        public Veil() {
            super("priest.veil", "Покров тьмы", 0x3A2A5A, "◘", 30, 0);
            blocks("range", 25);
            shield("shield", 2.2);
            seconds("duration", 8);
            damage("burst", 1.3);
            blocks("burst_radius", 4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ALLY;
        }

        @Override
        public String template() {
            return "Покров на {shield} и {duration}. Разбили покров — тьма лопается и бьёт всех в "
                    + "{burst_radius} от союзника на {burst}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity who = ctx.target != null ? ctx.target : ctx.caster;
            ctx.shield(who, "shield", "duration");
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, who, 1.2f, dur, 0x3A2A5A, false);
            double br = ctx.v("burst_radius");
            boolean[] burst = {false};
            Zones.add(ctx.level, who.position(), 1, ctx.caster.getUUID(), dur, 10, z -> {
                if (burst[0] || !who.isAlive()) return;
                if (StatusService.list(who).shieldTotal(z.level.getGameTime()) <= 0) {
                    burst[0] = true;
                    for (LivingEntity e : ctx.enemiesAround(who.position(), br)) ctx.hit(e, "burst");
                    Fx.ring(z.level, who.position(), (float) br, 12, 0x3A2A5A);
                    ctx.soundAt(who.position(), SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.6f);
                }
            });
            ctx.soundAt(who.position(), SoundEvents.AMETHYST_BLOCK_HIT, 1f, 0.9f);
        }
    }

    /** Пожирание тени: отнять у всех врагов разом и раздать группе. */
    public static final class Devour extends Skill {
        public Devour() {
            super("priest.devour", "Пожирание тени", 0x1A0A2A, "✹", 112, 0);
            blocks("radius", 10);
            damage("damage", 2.6);
            percent("share", 0.8);
            count("cost", 40);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} сумерек: все враги в {radius} теряют {damage}, и {share} снятого делится между "
                    + "союзниками. Чем больше врагов, тем больше здоровья у группы.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double taken = 0;
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) {
                float before = e.getHealth();
                ctx.hit(e, "damage");
                taken += Math.max(0, before - e.getHealth());
                Fx.beam(ctx.level, e, ctx.caster, 0.14f, 12, 0x6B4AA8);
            }
            var allies = ctx.alliesAround(ctx.caster.position(), 25);
            if (taken > 0 && !allies.isEmpty()) {
                double each = taken * ctx.v("share") / allies.size();
                for (LivingEntity a : allies) {
                    HealService.healFixed(ctx.caster, a, each);
                    Fx.pillar(ctx.level, a.position(), 0.7f, 3.5f, 18, 0xB8A6FF);
                }
            }
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 16, 0x1A0A2A);
            ctx.sound(SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 1.1f);
            StatusService.apply(ctx.caster, Status.of(StatusType.LEECH, "priest.devour", ctx.caster.getUUID(), 0.15,
                    ctx.now(), 100));
        }
    }
}
