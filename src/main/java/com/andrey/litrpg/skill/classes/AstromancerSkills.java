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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Астромант — боец светилами. Он ставит звёзды в мире: они висят, пульсируют уроном, тянут врагов и
 * усиливают его луч, если тот проходит рядом. «Созвездие» соединяет светила линиями, которые режут всё
 * между ними, а «Сверхновая» гасит их все разом, превращая каждое в взрыв. Чем лучше расставлены звёзды,
 * тем сильнее астромант — ресурса у него нет, вся игра в расстановке.
 */
public final class AstromancerSkills {
    private AstromancerSkills() {
    }

    /** Падающая звезда: удар по точке с разметкой. */
    public static final class FallingStar extends Skill {
        public FallingStar() {
            super("astro.falling", "Падающая звезда", 0xFFD27A, "✦", 8, 0);
            blocks("range", 24);
            blocks("radius", 4);
            damage("damage", 1.8);
            seconds("delay", 1.0);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Через {delay} после разметки звезда падает в точку: {damage} урона всем в {radius}. "
                    + "Разметку видно — по ней и бьют вместе с отрядом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 at = ctx.point;
            double r = ctx.v("radius");
            int delay = ctx.ticks("delay");
            Fx.disc(ctx.level, at, (float) r, delay, 0xFFD27A, true);
            ctx.later(delay, () -> {
                for (LivingEntity e : ctx.enemiesAround(at, r)) ctx.hit(e, "damage");
                Fx.beam(ctx.level, at.add(0, 18, 0), at, 1.2f, 8, 0xFFD27A);
                Fx.burst(ctx.level, at.add(0, 0.6, 0), (float) r, 12, 0xFFD27A);
                Fx.wave(ctx.level, at, (float) r * 1.4f, 1.6f, 14, 0xFFD27A);
                ctx.soundAt(at, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.3f);
            });
            ctx.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 1.5f);
        }
    }

    /** Светило: звезда остаётся в мире и бьёт сама. */
    public static final class Luminary extends Skill {
        public Luminary() {
            super("astro.luminary", "Светило", 0x7FD8FF, "✧", 6, 0);
            blocks("range", 16);
            blocks("radius", 5);
            shield("hp", 2.0);
            damage("damage", 0.45);
            seconds("duration", 24);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Светило с {hp} прочности висит {duration} и каждую секунду бьёт врагов в {radius} на {damage}. "
                    + "Перезарядка короче, чем срок жизни светила, поэтому их можно держать четыре сразу — "
                    + "на них держатся «Созвездие» и «Сверхновая». Враги могут их разбить.";
        }

        @Override
        public void execute(SkillContext ctx) {
            var star = ctx.prop(PropKind.STAR, ctx.point.add(0, 1.2, 0), "hp", ctx.v("radius"), "duration");
            star.onPulse(20, p -> {
                for (LivingEntity e : ctx.enemiesAround(p.position(), p.radius())) {
                    ctx.hit(e, "damage");
                    Fx.beam(ctx.level, p.position(), e.position().add(0, 1, 0), 0.1f, 6, 0x7FD8FF);
                }
            });
            Fx.orbOn(ctx.level, star, 0.6f, ctx.ticks("duration"), 0xFFE27A);
            ctx.soundAt(ctx.point, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.1f, 1.4f);
        }
    }

    /** Звёздный луч: бьёт сильнее, если проходит рядом со светилом. */
    public static final class StarBeam extends Skill {
        public StarBeam() {
            super("astro.beam", "Звёздный луч", 0xBFE8FF, "➤", 5, 0);
            blocks("range", 24);
            blocks("near", 6);
            damage("damage", 1.3);
            percent("amplify", 0.5);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Луч по врагу на {damage}. За каждое ваше светило в {near} от цели луч сильнее "
                    + "на {amplify} — светила усиливают всё, что рядом с ними.";
        }

        @Override
        public void execute(SkillContext ctx) {
            long near = ctx.myProps(PropKind.STAR, 50).stream()
                    .filter(p -> p.position().distanceTo(ctx.target.position()) <= ctx.v("near")).count();
            double mult = 1 + ctx.v("amplify") * near;
            ctx.hitShare(ctx.target, ctx.v("damage") * mult);
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.4, 0), ctx.target.position().add(0, 1, 0),
                    (float) (0.12 + 0.06 * near), 8, 0xBFE8FF);
            for (ClassProp p : ctx.myProps(PropKind.STAR, 50)) {
                if (p.position().distanceTo(ctx.target.position()) <= ctx.v("near")) {
                    Fx.beam(ctx.level, p.position(), ctx.target.position().add(0, 1, 0), 0.1f, 6, 0x7FD8FF);
                }
            }
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.7f);
        }
    }

    /** Созвездие: линии между светилами режут всё, что между ними. */
    public static final class Constellation extends Skill {
        public Constellation() {
            super("astro.constellation", "Созвездие", 0xFFFFFF, "✶", 20, 0);
            blocks("search", 40);
            blocks("width", 2);
            damage("damage", 1.6);
            count("times", 3);
            seconds("duration", 3);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Соединяет все ваши светила в {search} линиями и {times} раза за {duration} режет всё "
                    + "на этих линиях на {damage} в полосе {width}. Два светила — одна линия, три — уже три.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> stars = ctx.myProps(PropKind.STAR, ctx.v("search"));
            if (stars.size() < 2) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Нужны хотя бы два светила.");
                return;
            }
            int times = (int) Math.round(ctx.v("times"));
            int step = Math.max(10, ctx.ticks("duration") / Math.max(1, times));
            double width = ctx.v("width");
            for (int t = 0; t < times; t++) {
                ctx.later(1 + t * step, () -> {
                    for (int i = 0; i < stars.size(); i++) {
                        for (int j = i + 1; j < stars.size(); j++) {
                            ClassProp a = stars.get(i), b = stars.get(j);
                            if (!a.isAlive() || !b.isAlive()) continue;
                            Vec3 from = a.position(), to = b.position();
                            Fx.beam(ctx.level, from, to, (float) width, 10, 0xFFFFFF);
                            Vec3 ab = to.subtract(from).multiply(1, 0, 1);
                            for (LivingEntity e : ctx.enemiesAround(from.add(to).scale(0.5), from.distanceTo(to))) {
                                double k = Math.max(0, Math.min(1, e.position().subtract(from).multiply(1, 0, 1)
                                        .dot(ab) / Math.max(1e-6, ab.lengthSqr())));
                                if (e.position().multiply(1, 0, 1).distanceTo(from.multiply(1, 0, 1).add(ab.scale(k)))
                                        <= width) {
                                    ctx.hit(e, "damage");
                                }
                            }
                        }
                    }
                });
            }
            ctx.sound(SoundEvents.BEACON_POWER_SELECT, 1.1f, 0.8f);
        }
    }

    /** Притяжение светил: звёзды тянут врагов к себе и держат. */
    public static final class StarPull extends Skill {
        public StarPull() {
            super("astro.pull", "Притяжение светил", 0x8FA6FF, "◎", 35, 0);
            blocks("search", 40);
            seconds("duration", 4);
            percent("slow", 0.35);
            damage("damage", 0.4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: каждое ваше светило в {search} тянет врагов к себе, замедляет их на {slow} "
                    + "и режет на {damage} в секунду. Враги собираются там, где вы хотите.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> stars = ctx.myProps(PropKind.STAR, ctx.v("search"));
            if (stars.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Тянуть нечем — светил рядом нет.");
                return;
            }
            int dur = ctx.ticks("duration");
            double slow = Math.min(0.8, ctx.v("slow"));
            for (ClassProp star : stars) {
                Fx.disc(ctx.level, star.position(), (float) star.radius(), dur, 0x8FA6FF, false);
                com.andrey.litrpg.skill.Zones.add(ctx.level, star.position(), star.radius(), ctx.caster.getUUID(),
                        dur, 10, z -> {
                            if (!star.isAlive()) return;
                            long now = z.level.getGameTime();
                            for (LivingEntity e : ctx.enemiesAround(star.position(), star.radius())) {
                                ctx.pull(e, star.position(), 0.9);
                                StatusService.apply(e, Status.of(StatusType.SLOW, "astro.pull", ctx.caster.getUUID(),
                                        slow, now, 14));
                                if (now % 20 == 0) ctx.hit(e, "damage");
                            }
                        });
            }
            ctx.sound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
        }
    }

    /** Сверхновая: все светила гаснут, каждое — взрывом. */
    public static final class Supernova extends Skill {
        public Supernova() {
            super("astro.supernova", "Сверхновая", 0xFFF3B0, "✺", 68, 0);
            blocks("search", 40);
            blocks("radius", 7);
            damage("damage", 2.8);
            percent("center", 1.0);
            seconds("stun", 1);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Все ваши светила в {search} взрываются: {damage} урона в {radius} от каждого (в центре — "
                    + "на {center} больше) и оглушение на {stun}. Светила тратятся: это итог всей расстановки.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<ClassProp> stars = ctx.myProps(PropKind.STAR, ctx.v("search"));
            if (stars.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Взрывать нечего — поставьте светила.");
                return;
            }
            double r = ctx.v("radius");
            for (ClassProp star : stars) {
                Vec3 at = star.position();
                for (LivingEntity e : ctx.enemiesAround(at, r)) {
                    boolean center = e.position().distanceTo(at) < r * 0.4;
                    ctx.hitShare(e, ctx.v("damage") * (center ? 1 + ctx.v("center") : 1));
                    ctx.status(e, StatusType.STUN, 1, "stun");
                }
                Fx.burst(ctx.level, at, (float) r, 14, 0xFFF3B0);
                Fx.shards(ctx.level, at.add(0, 0.5, 0), (float) r, 22, 16, 0xFFD27A);
                Fx.ring(ctx.level, at, (float) r, 14, 0xFFD27A);
                star.vanish();
            }
            ctx.sound(SoundEvents.GENERIC_EXPLODE.value(), 1.2f, 0.6f);
        }
    }
}
