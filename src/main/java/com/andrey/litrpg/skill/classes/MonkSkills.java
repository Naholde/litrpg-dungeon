package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.prop.ClassProp;
import com.andrey.litrpg.skill.prop.PropKind;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

/**
 * Монах — боец печатей. Печати стоят на земле настоящими объектами: первая бьёт, вторая стягивает врагов между
 * печатями, а когда их три — «Круг сомкнулся» взрывает всю фигуру. Ци копится от его приёмов и уходит в
 * «Ладонь ци» и «Гора падает».
 */
public final class MonkSkills {
    private MonkSkills() {
    }

    /** Поставить печать под точкой (общее для всех печатей монаха). */
    private static ClassProp seal(SkillContext ctx, Vec3 at) {
        ClassProp p = ctx.prop(PropKind.SEAL, at, "seal_hp", 0, "seal_time");
        Fx.disc(ctx.level, at, 1.6f, ctx.ticks("seal_time"), 0xFFD27A, false);
        return p;
    }

    /** Печать удара: удар по врагу и печать под ним. */
    public static final class SealStrike extends Skill {
        public SealStrike() {
            super("monk.seal_strike", "Печать удара", 0xFFD27A, "✶", 5, 0);
            blocks("range", 4);
            damage("damage", 1.2);
            shield("seal_hp", 1.0);
            seconds("seal_time", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Удар на {damage} и печать под врагом на {seal_time}. Печати — основа монаха: две стягивают "
                    + "врагов между собой, три складываются в «Круг сомкнулся».";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.hit(ctx.target, "damage");
            seal(ctx, ctx.target.position());
            Fx.slash(ctx.level, ctx.caster, 2.2f, 60, 6, 0xFFD27A);
            ctx.soundAt(ctx.target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 1.4f);
        }
    }

    /** Печать связи: между двумя печатями натягивается связь, которая стягивает врагов. */
    public static final class SealBind extends Skill {
        public SealBind() {
            super("monk.seal_bind", "Печать связи", 0xFFE8A6, "⌖", 10, 0);
            blocks("range", 12);
            shield("seal_hp", 1.0);
            seconds("seal_time", 12);
            percent("slow", 0.35);
            damage("damage", 0.4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Ставит печать. Пока их две, между ними натянута связь: врагов на линии тянет к середине, "
                    + "замедляет на {slow} и бьёт на {damage} в секунду.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ClassProp mine = seal(ctx, ctx.point);
            List<ClassProp> seals = ctx.myProps(PropKind.SEAL, 40);
            ClassProp other = seals.stream().filter(p -> p != mine)
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(mine))).orElse(null);
            if (other == null) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Печать стоит. Поставьте вторую — между ними "
                        + "натянется связь.");
                return;
            }
            double slow = Math.min(0.8, ctx.v("slow"));
            Vec3 mid = mine.position().add(other.position()).scale(0.5);
            double half = mine.position().distanceTo(other.position()) * 0.5 + 1.5;
            int dur = Math.min(ctx.ticks("seal_time"), 240);
            Fx.beam(ctx.level, mine.position().add(0, 0.3, 0), other.position().add(0, 0.3, 0), 0.5f, dur, 0xFFE8A6);
            com.andrey.litrpg.skill.Zones.add(ctx.level, mid, half, ctx.caster.getUUID(), dur, 20, z -> {
                if (!mine.isAlive() || !other.isAlive()) return;
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    ctx.pull(e, z.center, 0.7);
                    ctx.hit(e, "damage");
                    StatusService.apply(e, Status.of(StatusType.SLOW, "monk.seal_bind", ctx.caster.getUUID(), slow,
                            z.level.getGameTime(), 30));
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        }
    }

    /** Круг сомкнулся: три печати складываются в фигуру и взрываются. */
    public static final class CircleClosed extends Skill {
        public CircleClosed() {
            super("monk.circle", "Круг сомкнулся", 0xFFF3B0, "◎", 18, 0);
            blocks("search", 40);
            count("need", 3);
            damage("damage", 2.6);
            seconds("stun", 1.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Нужно {need} ваших печатей: фигура между ними вспыхивает — всем врагам внутри {damage} урона "
                    + "и оглушение на {stun}. Печати при этом тратятся.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> seals = ctx.myProps(PropKind.SEAL, ctx.v("search")).stream()
                    .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(ctx.caster)))
                    .limit((long) ctx.v("need")).toList();
            if (seals.size() < Math.round(ctx.v("need"))) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Печатей мало: нужно "
                        + Math.round(ctx.v("need")) + ", стоит " + seals.size() + ".");
                return;
            }
            Vec3 center = Vec3.ZERO;
            for (ClassProp p : seals) center = center.add(p.position());
            center = center.scale(1.0 / seals.size());
            double radius = 2;
            for (ClassProp p : seals) radius = Math.max(radius, p.position().distanceTo(center) + 1.5);
            for (LivingEntity e : ctx.enemiesAround(center, radius)) {
                ctx.hit(e, "damage");
                ctx.status(e, StatusType.STUN, 1, "stun");
            }
            for (int i = 0; i < seals.size(); i++) {
                ClassProp a = seals.get(i), b = seals.get((i + 1) % seals.size());
                Fx.beam(ctx.level, a.position().add(0, 0.3, 0), b.position().add(0, 0.3, 0), 0.6f, 14, 0xFFF3B0);
            }
            Fx.disc(ctx.level, center, (float) radius, 16, 0xFFF3B0, true);
            Fx.burst(ctx.level, center.add(0, 1, 0), (float) radius * 0.6f, 12, 0xFFF3B0);
            seals.forEach(ClassProp::vanish);
            ctx.soundAt(center, SoundEvents.BEACON_POWER_SELECT, 1.2f, 0.7f);
            ctx.gain(20);
        }
    }

    /** Ладонь ци: удар насквозь по линии. */
    public static final class PalmOfChi extends Skill {
        public PalmOfChi() {
            super("monk.palm", "Ладонь ци", 0xFFD27A, "☛", 12, 0);
            blocks("length", 10);
            blocks("width", 1.6);
            damage("damage", 1.8);
            count("cost", 20);
        }

        @Override
        public Targeting targeting() {
            return Targeting.DIRECTION;
        }

        @Override
        public String template() {
            return "За {cost} ци: волна ладони пробивает всех на {length} по прямой в полосе {width} "
                    + "на {damage} урона каждому.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 from = ctx.caster.position().add(0, 1, 0);
            Vec3 dir = ctx.caster.getViewVector(1).multiply(1, 0, 1).normalize();
            Vec3 to = from.add(dir.scale(ctx.v("length")));
            double width = ctx.v("width");
            for (LivingEntity e : ctx.enemiesAround(from.add(to).scale(0.5), ctx.v("length"))) {
                Vec3 ab = to.subtract(from).multiply(1, 0, 1);
                double t = Math.max(0, Math.min(1, e.position().subtract(from).multiply(1, 0, 1).dot(ab) / ab.lengthSqr()));
                if (e.position().multiply(1, 0, 1).distanceTo(from.multiply(1, 0, 1).add(ab.scale(t))) <= width) {
                    ctx.hit(e, "damage");
                }
            }
            Fx.line(ctx.level, from, to, (float) width * 2, 10, 0xFFD27A, false);
            ctx.sound(SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.9f);
        }
    }

    /** Сто ладоней: серия быстрых удара по цели, каждый даёт ци. */
    public static final class HundredPalms extends Skill {
        public HundredPalms() {
            super("monk.hundred", "Сто ладоней", 0xFFE8A6, "⁙", 20, 0);
            blocks("range", 4);
            damage("damage", 0.45);
            count("hits", 6);
            count("chi", 6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{hits} ладоней подряд по цели, каждая на {damage} и {chi} ци. Пока монах бьёт — ци копится.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int hits = (int) Math.round(ctx.v("hits"));
            LivingEntity target = ctx.target;
            double chi = ctx.v("chi");
            for (int i = 0; i < hits; i++) {
                ctx.later(1 + i * 4, () -> {
                    if (!target.isAlive() || ctx.caster.distanceTo(target) > ctx.v("range") + 2) return;
                    ctx.hit(target, "damage");
                    ctx.gain(chi);
                    Fx.slash(ctx.level, ctx.caster, 2f, 40, 4, 0xFFE8A6);
                });
            }
            ctx.sound(SoundEvents.PLAYER_ATTACK_NODAMAGE, 1f, 1.6f);
        }
    }

    /** Гора падает: вся ци — в один удар по площади с оглушением. */
    public static final class MountainFalls extends Skill {
        public MountainFalls() {
            super("monk.mountain", "Гора падает", 0xC9A055, "⛰", 70, 0);
            blocks("radius", 6);
            count("cost", 30);
            param("per_chi", 0.035, com.andrey.litrpg.skill.SkillParam.Unit.DAMAGE,
                    com.andrey.litrpg.skill.SkillParam.Scale.POWER);
            seconds("stun", 2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Тратит всю ци (нужно хотя бы {cost}): удар с небес по всем в {radius} — {per_chi} за каждую "
                    + "единицу ци — и оглушение на {stun}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            double chi = ctx.v("cost") + ctx.spendAllResource();
            double share = ctx.v("per_chi") * chi;
            for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), ctx.v("radius"))) {
                ctx.hitShare(e, share);
                ctx.status(e, StatusType.STUN, 1, "stun");
            }
            Fx.pillar(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 6f, 14, 0xC9A055);
            Fx.ring(ctx.level, ctx.caster.position(), (float) ctx.v("radius"), 14, 0xC9A055);
            ctx.level.playSound(null, BlockPos.containing(ctx.caster.position()), SoundEvents.ANVIL_LAND,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 0.5f);
        }
    }
}
