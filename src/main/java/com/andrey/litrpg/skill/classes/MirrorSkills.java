package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Зерцало — танк, который живёт чужими ударами. Под «Стеклом» урон по нему меньше, но копится в ресурс
 * (полная полоса — впитана целая его жизнь) и возвращается конусом. «Грань» разворачивает снаряды в стрелка,
 * «Зеркальный зал» заставляет врагов бить мимо, а двойник стоит в мире и держит врагов на себе, пока цел.
 */
public final class MirrorSkills {
    private MirrorSkills() {
    }

    /** Стекло: часть урона уходит в ресурс вместо здоровья. */
    public static final class Glass extends Skill {
        public Glass() {
            super("mirror.glass", "Стекло", 0xB0E8FF, "◫", 20, 0);
            seconds("duration", 6);
            percent("reduce", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: урон по вам меньше на {reduce}, и всё, что прошло, копится в стекло — "
                    + "полная полоса стекла равна вашей жизни.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            ctx.status(ctx.caster, StatusType.RECORD, 0, "duration");
            Fx.discOn(ctx.level, ctx.caster, 1.4f, ctx.ticks("duration"), 0xB0E8FF, false);
            ctx.sound(SoundEvents.GLASS_PLACE, 1f, 1.4f);
        }
    }

    /** Вернуть сполна: всё стекло — в удар конусом. */
    public static final class Repay extends Skill {
        public Repay() {
            super("mirror.repay", "Вернуть сполна", 0x8FE3FF, "◆", 12, 0);
            blocks("radius", 6);
            param("angle", 110, com.andrey.litrpg.skill.SkillParam.Unit.FLAT, com.andrey.litrpg.skill.SkillParam.Scale.NONE);
            count("cost", 10);
            percent("back", 0.9);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "Тратит всё стекло (нужно хотя бы {cost}): каждому врагу перед собой в {radius} — {back} "
                    + "накопленного урона. Полная полоса — почти вся ваша жизнь, возвращённая обидчикам.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double glass = ctx.v("cost") + ctx.spendAllResource();
            double damage = glass / 100.0 * ctx.caster.getMaxHealth() * ctx.v("back");
            for (LivingEntity e : ctx.enemiesInCone(ctx.v("radius"), ctx.v("angle"))) {
                com.andrey.litrpg.combat.DamageService.deal(ctx.caster, null, e, damage,
                        com.andrey.litrpg.combat.WeaponKind.FIXED);
            }
            Fx.cone(ctx.level, ctx.caster.position(), ctx.caster.getViewVector(1).multiply(1, 0, 1),
                    (float) ctx.v("radius"), (float) ctx.v("angle"), 10, 0x8FE3FF, true);
            ctx.sound(SoundEvents.GLASS_BREAK, 1.2f, 0.8f);
        }
    }

    /** Грань: вражеские снаряды рядом разворачиваются и летят обратно. */
    public static final class Edge extends Skill {
        public Edge() {
            super("mirror.edge", "Грань", 0xD6F3FF, "◇", 24, 0);
            blocks("radius", 4);
            seconds("duration", 5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: вражеские снаряды в {radius} от вас разворачиваются и летят обратно в того, "
                    + "кто стрелял — теперь это ваши снаряды.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.discOn(ctx.level, ctx.caster, (float) r, dur, 0xD6F3FF, false);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 2, z -> {
                Vec3 c = ctx.caster.position();
                for (Projectile pr : z.level.getEntitiesOfClass(Projectile.class, new AABB(c, c).inflate(r),
                        p -> !(p.getOwner() instanceof Player))) {
                    pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.1));
                    pr.setOwner(ctx.caster);
                    pr.hasImpulse = true;
                    z.level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                            pr.getX(), pr.getY(), pr.getZ(), 4, 0.05, 0.05, 0.05, 0.02);
                }
            });
            ctx.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.5f);
        }
    }

    /** Зеркальный зал: отражений столько, что враги бьют мимо — и по вам, и по союзникам рядом. */
    public static final class Hall extends Skill {
        public Hall() {
            super("mirror.hall", "Зеркальный зал", 0xA6E1FF, "❖", 44, 0);
            blocks("radius", 5);
            seconds("duration", 6);
            percent("miss", 0.525);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: вы и союзники в {radius} окружены отражениями — удары по вам проходят мимо "
                    + "с вероятностью {miss}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double miss = Math.min(0.8, ctx.v("miss"));
            int dur = ctx.ticks("duration");
            for (LivingEntity a : ctx.alliesAround(ctx.caster.position(), ctx.v("radius"))) {
                ctx.status(a, StatusType.BLUR, miss, "duration");
                Fx.mark(ctx.level, a, "❖", dur, 0xA6E1FF);
            }
            Fx.disc(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), dur, 0xA6E1FF, false);
            Fx.pillar(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 3f, dur, 0xA6E1FF);
            Fx.dome(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), dur, 0xA6E1FF);
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 0.9f, 1.6f);
        }
    }

    /** Двойник: зеркальная копия стоит в мире, тянет врагов на себя и бьёт осколками, когда её разбивают. */
    public static final class Reflection extends Skill {
        public Reflection() {
            super("mirror.double", "Двойник", 0xB0E8FF, "⧈", 48, 0);
            blocks("radius", 10);
            shield("hp", 6.0);
            seconds("duration", 12);
            seconds("hold", 6);
            damage("shards", 1.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Ставит зеркального двойника с {hp} здоровья на {duration}: враги в {radius} бьют его, "
                    + "а не вас ({hold} за раз). Разбили — осколки бьют всех вокруг на {shards}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double shards = ctx.precomputed("shards");
            var prop = ctx.prop(PropKind.MIRROR, ctx.caster.position(), "hp", ctx.v("radius"), "duration");
            int hold = ctx.ticks("hold");
            prop.onPulse(20, p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                    if (e instanceof net.minecraft.world.entity.Mob m) {
                        com.andrey.litrpg.combat.threat.ThreatService.decoy(m, p, hold);
                    }
                }
            });
            prop.onBreak(p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), 4)) {
                    com.andrey.litrpg.combat.DamageService.deal(ctx.caster, null, e, shards,
                            com.andrey.litrpg.combat.WeaponKind.FIXED);
                }
                Fx.ring(ctx.level, p.position(), 4f, 10, 0xB0E8FF);
            });
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) {
                if (e instanceof net.minecraft.world.entity.Mob m) {
                    com.andrey.litrpg.combat.threat.ThreatService.decoy(m, prop, hold);
                }
            }
            ctx.sound(SoundEvents.GLASS_PLACE, 1.1f, 0.9f);
        }
    }
}
