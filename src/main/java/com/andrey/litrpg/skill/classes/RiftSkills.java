package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.skill.prop.ClassProp;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Разломщик — боец пустоты. Он рвёт мир: разломы стоят трещинами и тянут врагов, «Шаг в пустоту» переносит
 * сквозь стены, пустотная метка наказывает тех, кто пытается отойти, «Схлопывание» закрывает все разломы
 * разом, а на «Изнанке» шаг в пустоту работает без перезарядки.
 */
public final class RiftSkills {
    private RiftSkills() {
    }

    /** Разлом: трещина в мире тянет и режет. */
    public static final class Rift extends Skill {
        public Rift() {
            super("rift.rift", "Разлом", 0x9B5CFF, "◇", 14, 0);
            blocks("range", 16);
            blocks("radius", 5);
            shield("hp", 2.0);
            damage("damage", 0.5);
            seconds("duration", 14);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Разлом с {hp} прочности на {duration}: каждую секунду тянет врагов в {radius} к себе "
                    + "и режет на {damage}. Разломы потом можно схлопнуть.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var rift = ctx.prop(PropKind.VOID_RIFT, ctx.point.add(0, 0.6, 0), "hp", ctx.v("radius"), "duration");
            rift.onPulse(20, p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                    ctx.pull(e, p.position(), 0.7);
                    ctx.hit(e, "damage");
                    Fx.beam(ctx.level, e.position().add(0, 1, 0), p.position(), 0.1f, 8, 0x9B5CFF);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.PORTAL_TRIGGER, 0.8f, 1.2f);
        }
    }

    /** Шаг в пустоту: телепорт, которому стены не мешают. */
    public static final class VoidStep extends Skill {
        public VoidStep() {
            super("rift.step", "Шаг в пустоту", 0xD6A6FF, "⇅", 9, 0);
            blocks("range", 14);
            damage("damage", 0.8);
            blocks("radius", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Шаг до {range} — сквозь стены, если за ними есть куда встать: {damage} урона всем "
                    + "в {radius} от места выхода.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 at = ctx.point;
            ctx.teleport(at);
            for (LivingEntity e : ctx.enemiesAround(at, ctx.v("radius"))) ctx.hit(e, "damage");
            Fx.burst(ctx.level, at.add(0, 1, 0), (float) ctx.v("radius"), 10, 0xD6A6FF);
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 1f, 1.2f);
        }
    }

    /** Пустотная метка: отойдёшь от места — получишь. */
    public static final class VoidMark extends Skill {
        public VoidMark() {
            super("rift.mark", "Пустотная метка", 0x7A4AA8, "⌖", 20, 0);
            blocks("range", 20);
            blocks("leash", 5);
            seconds("duration", 4);
            damage("damage", 2.6);
            percent("extra", 0.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Метка на {duration}: цель получает на {extra} больше урона, а когда метка гаснет — если "
                    + "цель ушла дальше {leash} от места, где её поставили, она получает {damage}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity victim = ctx.target;
            Vec3 where = victim.position();
            ctx.status(victim, StatusType.MARK, 0, "duration");
            ctx.status(victim, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            int dur = ctx.ticks("duration");
            Fx.disc(ctx.level, where, (float) ctx.v("leash"), dur, 0x7A4AA8, false);
            Fx.mark(ctx.level, victim, "⌖", dur, 0x7A4AA8);
            double leash = ctx.v("leash");
            ctx.later(dur - 1, () -> {
                if (!victim.isAlive()) return;
                if (victim.position().distanceTo(where) > leash) {
                    ctx.hit(victim, "damage");
                    Fx.beam(ctx.level, where.add(0, 1, 0), victim.position().add(0, 1, 0), 0.3f, 10, 0x7A4AA8);
                    Fx.burst(ctx.level, victim.position().add(0, 1, 0), 1.6f, 10, 0x7A4AA8);
                    ctx.soundAt(victim.position(), SoundEvents.ENDERMAN_HURT, 1f, 0.7f);
                }
            });
            ctx.soundAt(where, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.9f);
        }
    }

    /** Схлопывание: все разломы закрываются разом. */
    public static final class Collapse extends Skill {
        public Collapse() {
            super("rift.collapse", "Схлопывание", 0x4A2A8B, "✸", 30, 0);
            blocks("search", 40);
            blocks("radius", 6);
            damage("damage", 2.2);
            seconds("stun", 1);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Все ваши разломы в {search} закрываются: в {radius} от каждого — {damage} урона "
                    + "и оглушение на {stun}. Сначала наставить, потом схлопнуть.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> rifts = ctx.myProps(PropKind.VOID_RIFT, ctx.v("search"));
            if (rifts.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Схлопывать нечего — разломов рядом нет.");
                return;
            }
            for (ClassProp r : rifts) {
                for (LivingEntity e : ctx.enemiesAround(r.position(), ctx.v("radius"))) {
                    ctx.hit(e, "damage");
                    ctx.status(e, StatusType.STUN, 1, "stun");
                }
                Fx.ring(ctx.level, r.position(), (float) ctx.v("radius"), 12, 0x4A2A8B);
                Fx.burst(ctx.level, r.position(), 2f, 12, 0x4A2A8B);
                r.vanish();
            }
            ctx.sound(SoundEvents.PORTAL_TRAVEL, 0.6f, 1.4f);
        }
    }

    /** Зев: в зоне гаснут снаряды, а всё живое тает. */
    public static final class Maw extends Skill {
        public Maw() {
            super("rift.maw", "Зев", 0x2A1A4A, "◉", 55, 0);
            blocks("range", 16);
            blocks("radius", 7);
            seconds("duration", 10);
            damage("damage", 0.5);
            percent("slow", 0.3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Зев в {radius} на {duration}: вражеские снаряды внутри гаснут, враги замедлены на {slow} "
                    + "и каждую секунду теряют {damage}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.8, ctx.v("slow"));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x2A1A4A, false);
            Fx.pillar(ctx.level, ctx.point, (float) r, 4f, dur, 0x2A1A4A);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 10, z -> {
                long now = z.level.getGameTime();
                for (Projectile pr : z.level.getEntitiesOfClass(Projectile.class,
                        new AABB(z.center, z.center).inflate(z.radius), p -> !(p.getOwner() instanceof Player))) {
                    z.level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
                            pr.getX(), pr.getY(), pr.getZ(), 6, 0.1, 0.1, 0.1, 0.05);
                    pr.discard();
                }
                if (now % 20 != 0) return;
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.hit(e, "damage");
                    StatusService.apply(e, Status.of(StatusType.SLOW, "rift.maw", ctx.caster.getUUID(), slow, now, 30));
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.PORTAL_AMBIENT, 1.2f, 0.6f);
        }
    }

    /** Изнанка: разломщик уходит в пустоту — снаряды мимо, шаг без перезарядки. */
    public static final class Inside extends Skill {
        public Inside() {
            super("rift.inside", "Изнанка", 0xE8D6FF, "✺", 120, 0);
            seconds("duration", 6);
            percent("reduce", 0.5);
            percent("speed", 0.25);
            percent("miss", 0.4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration} на изнанке мира: урон по разломщику меньше на {reduce}, удары проходят мимо "
                    + "с вероятностью {miss}, он быстрее на {speed}, а «Шаг в пустоту» всё это время "
                    + "работает без перезарядки.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 1 - Math.min(0.9, ctx.v("reduce")), "duration");
            ctx.status(ctx.caster, StatusType.BLUR, Math.min(0.9, ctx.v("miss")), "duration");
            ctx.status(ctx.caster, StatusType.SPEED, ctx.v("speed"), "duration");
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.caster, 1.4f, dur, 0xE8D6FF, false);
            Fx.mark(ctx.level, ctx.caster, "✺", dur, 0xE8D6FF);
            Zones.add(ctx.level, ctx.caster.position(), 1, ctx.caster.getUUID(), dur, 5, z -> ctx.readyNow("rift.step"));
            ctx.sound(SoundEvents.PORTAL_TRAVEL, 0.8f, 1.3f);
        }
    }
}
