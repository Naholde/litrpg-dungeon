package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

/**
 * Чернокнижник — платит своим здоровьем за чужую боль. «Пакт» превращает его кровь в ресурс и в урон,
 * порча при смерти цели перескакивает на соседа, кровавый круг забирает здоровье у всех внутри и отдаёт
 * ему, щупальца держат врагов, а «Жертвенный приговор» стоит половины его жизни — и стоит того.
 */
public final class WarlockSkills {
    private WarlockSkills() {
    }

    /** Проклятие слабости: цель бьёт слабее и получает больше. */
    public static final class CurseOfWeakness extends Skill {
        public CurseOfWeakness() {
            super("warlock.curse", "Проклятие слабости", 0x8B5CFF, "⊗", 14, 0);
            blocks("range", 22);
            seconds("duration", 12);
            percent("weaken", 0.45);
            percent("extra", 0.33);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Проклятие на {duration}: цель бьёт на {weaken} слабее и получает на {extra} больше урона "
                    + "от всех.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.target, StatusType.DAMAGE_DEALT, 1 - Math.min(0.9, ctx.v("weaken")), "duration");
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            Fx.mark(ctx.level, ctx.target, "⊗", ctx.ticks("duration"), 0x8B5CFF);
            ctx.soundAt(ctx.target.position(), SoundEvents.WITCH_CELEBRATE, 0.8f, 0.7f);
        }
    }

    /** Порча: тлеет, а когда цель умирает — перескакивает на соседа. */
    public static final class Corruption extends Skill {
        public Corruption() {
            super("warlock.corruption", "Порча", 0x5A2A6B, "≀", 8, 0);
            blocks("range", 22);
            blocks("jump", 8);
            damage("damage", 1.975);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Порча тлеет на {damage} за {duration}. Цель умерла, а порча ещё горит — она прыгает "
                    + "на ближайшего врага в {jump} и начинается заново.";
        }

        @Override
        public void execute(SkillContext ctx) {
            apply(ctx, ctx.target, 0);
            ctx.soundAt(ctx.target.position(), SoundEvents.SOUL_ESCAPE.value(), 0.9f, 0.6f);
        }

        private void apply(SkillContext ctx, LivingEntity victim, int jumps) {
            ctx.dot(victim, "damage", "duration");
            Fx.mark(ctx.level, victim, "≀", ctx.ticks("duration"), 0x5A2A6B);
            int dur = ctx.ticks("duration");
            Zones.add(ctx.level, victim.position(), 1, ctx.caster.getUUID(), dur, 10, z -> {
                if (victim.isAlive() || jumps >= 3) return;
                LivingEntity next = ctx.enemiesAround(victim.position(), ctx.v("jump")).stream()
                        .min(Comparator.comparingDouble(e -> e.distanceToSqr(victim))).orElse(null);
                if (next == null) return;
                Fx.beam(z.level, victim.position().add(0, 1, 0), next.position().add(0, 1, 0), 0.14f, 10, 0x5A2A6B);
                apply(ctx, next, jumps + 1);
            });
        }
    }

    /** Пакт: отдать здоровье за силу. */
    public static final class Pact extends Skill {
        public Pact() {
            super("warlock.pact", "Пакт", 0xB0001A, "⚯", 20, 0);
            percent("price", 0.2);
            percent("power", 0.525);
            seconds("duration", 10);
            count("pact", 40);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Отдаёт {price} своего текущего здоровья: {duration} урон выше на {power}, и приходит "
                    + "{pact} пакта — им платят за «Жертвенный приговор».";
        }

        @Override
        public void execute(SkillContext ctx) {
            float hp = ctx.caster.getHealth();
            float price = (float) Math.min(hp - 1, hp * ctx.v("price"));
            if (price > 0) ctx.caster.setHealth(hp - price);
            ctx.gain(ctx.v("pact"));
            ctx.status(ctx.caster, StatusType.DAMAGE_DEALT, 1 + ctx.v("power"), "duration");
            Fx.pillarOn(ctx.level, ctx.caster, 1.0f, 3.5f, ctx.ticks("duration"), 0xB0001A);
            ctx.sound(SoundEvents.WITHER_SPAWN, 0.5f, 1.6f);
        }
    }

    /** Кровавый круг: зона, которая тянет здоровье врагов в чернокнижника. */
    public static final class BloodCircle extends Skill {
        public BloodCircle() {
            super("warlock.circle", "Кровавый круг", 0x8B1A1A, "◯", 36, 0);
            blocks("range", 14);
            blocks("radius", 6);
            seconds("duration", 8);
            damage("damage", 0.342);
            percent("back", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Круг в {radius} на {duration}: каждую секунду враги внутри теряют {damage}, и {back} "
                    + "отнятого становится здоровьем чернокнижника.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius"), back = ctx.v("back");
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x8B1A1A, false);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                double taken = 0;
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    float before = e.getHealth();
                    ctx.hit(e, "damage");
                    taken += Math.max(0, before - e.getHealth());
                    Fx.beam(z.level, e.position().add(0, 1, 0), ctx.caster.position().add(0, 1, 0), 0.1f, 8, 0xD8343A);
                }
                if (taken > 0) HealService.healFixed(ctx.caster, ctx.caster, taken * back);
                Fx.ring(z.level, z.center, (float) z.radius, 18, 0x8B1A1A);
            });
            ctx.soundAt(ctx.point, SoundEvents.WITHER_SHOOT, 0.7f, 0.6f);
        }
    }

    /** Щупальца бездны: объекты из земли, которые держат врагов. */
    public static final class Tentacles extends Skill {
        public Tentacles() {
            super("warlock.tentacles", "Щупальца бездны", 0x9B5CFF, "⁂", 50, 0);
            blocks("range", 14);
            blocks("radius", 4);
            shield("hp", 2.0);
            damage("damage", 0.456);
            count("count", 3);
            seconds("duration", 12);
            seconds("root", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "{count} щупальца с {hp} здоровья на {duration}: каждое бьёт врагов в {radius} на {damage} "
                    + "и держит их на месте {root}. Щупальца можно срубить.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("count"));
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n;
                Vec3 at = ctx.point.add(Math.cos(a) * 2.0, 0, Math.sin(a) * 2.0);
                var t = ctx.prop(PropKind.TENTACLE, at, "hp", ctx.v("radius"), "duration");
                t.onPulse(20, p -> {
                    for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                        ctx.hit(e, "damage");
                        StatusService.apply(e, Status.of(StatusType.ROOT, "warlock.tentacles", ctx.caster.getUUID(), 1,
                                p.level().getGameTime(), ctx.ticks("root")));
                        Fx.beam(ctx.level, p.position().add(0, 1.6, 0), e.position().add(0, 1, 0), 0.12f, 8, 0x9B5CFF);
                    }
                });
            }
            Fx.ring(ctx.level, ctx.point, 3f, 14, 0x9B5CFF);
            ctx.soundAt(ctx.point, SoundEvents.SCULK_CATALYST_BLOOM, 1f, 0.7f);
        }
    }

    /** Жертвенный приговор: половина своей жизни — в один удар. */
    public static final class Sacrifice extends Skill {
        public Sacrifice() {
            super("warlock.sacrifice", "Жертвенный приговор", 0x4A0A4A, "✹", 80, 0);
            blocks("range", 20);
            count("cost", 40);
            percent("price", 0.5);
            damage("damage", 4.1);
            percent("per_health", 1.35);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "За {cost} пакта и {price} своего текущего здоровья: {damage} урона плюс {per_health} "
                    + "от отданного здоровья. Самый сильный одиночный удар в игре — и самый дорогой.";
        }

        @Override
        public void execute(SkillContext ctx) {
            float hp = ctx.caster.getHealth();
            float price = (float) Math.min(hp - 1, hp * ctx.v("price"));
            if (price > 0) ctx.caster.setHealth(hp - price);
            ctx.hit(ctx.target, "damage");
            com.andrey.litrpg.combat.DamageService.deal(ctx.caster, null, ctx.target, price * ctx.v("per_health"),
                    com.andrey.litrpg.combat.WeaponKind.FIXED);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.4, 0), ctx.target.position().add(0, 1, 0), 0.4f, 14,
                    0x4A0A4A);
            Fx.burst(ctx.level, ctx.target.position().add(0, 1, 0), 2.4f, 14, 0x4A0A4A);
            ctx.soundAt(ctx.target.position(), SoundEvents.WITHER_BREAK_BLOCK, 1.2f, 0.5f);
        }
    }
}
