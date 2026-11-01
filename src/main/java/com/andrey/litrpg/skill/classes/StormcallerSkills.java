package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Громовержец — цепи, статика и туча над головой. Статика копится слоями на враге и на пятом разряжается
 * по всем рядом; «Разряд» по заряженной цели бьёт вдвое; туча ходит за выбранным врагом и бьёт его сама,
 * а «Буря» превращает место боя в грозу.
 */
public final class StormcallerSkills {
    private StormcallerSkills() {
    }

    /** Слой статики; на пятом — разряд по всем рядом. */
    static void charge(SkillContext ctx, LivingEntity e, String secondsKey, String burstKey) {
        int stacks = ctx.addStack(e, StatusType.STATIC, secondsKey, 5);
        Fx.mark(ctx.level, e, stacks >= 5 ? "⚡" : "⌁", 25, 0xB8C6FF);
        if (stacks >= 5) {
            StatusService.removeAll(e, StatusType.STATIC);
            for (LivingEntity n : ctx.enemiesAround(e.position(), 4)) ctx.hit(n, burstKey);
            Fx.ring(ctx.level, e.position(), 4f, 10, 0xB8C6FF);
            ctx.soundAt(e.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.3f);
        }
    }

    /** Цепь молний: прыгает по целям, слабея. */
    public static final class ChainLightning extends Skill {
        public ChainLightning() {
            super("storm.chain", "Цепь молний", 0x8FA6FF, "↯", 7, 0);
            blocks("range", 24);
            damage("damage", 1.3);
            damage("burst", 0.8);
            count("targets", 4);
            percent("falloff", 0.2);
            blocks("jump", 8);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Молния бьёт на {damage} и прыгает до {targets} целей (каждая слабее на {falloff}, "
                    + "прыжок до {jump}). Каждому попаданию — слой статики.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int max = (int) Math.round(ctx.v("targets"));
            double falloff = ctx.v("falloff"), jump = ctx.v("jump");
            List<LivingEntity> hit = new ArrayList<>();
            LivingEntity cur = ctx.target;
            Vec3 from = ctx.caster.position().add(0, 1.4, 0);
            for (int i = 0; i < max && cur != null; i++) {
                ctx.hitShare(cur, ctx.v("damage") * Math.pow(1 - falloff, i));
                charge(ctx, cur, "duration", "burst");
                Fx.beam(ctx.level, from, cur.position().add(0, 1, 0), 0.16f, 8, 0x8FA6FF);
                hit.add(cur);
                from = cur.position().add(0, 1, 0);
                LivingEntity next = null;
                double best = Double.MAX_VALUE;
                for (LivingEntity e : ctx.enemiesAround(cur.position(), jump)) {
                    if (hit.contains(e)) continue;
                    double d = e.distanceToSqr(cur);
                    if (d < best) {
                        best = d;
                        next = e;
                    }
                }
                cur = next;
            }
            ctx.sound(SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5f, 1.7f);
        }
    }

    /** Разряд: мгновенно, а по заряженной цели — вдвое. */
    public static final class Jolt extends Skill {
        public Jolt() {
            super("storm.jolt", "Разряд", 0xD6E8FF, "⚡", 4, 0);
            blocks("range", 20);
            damage("damage", 1.1);
            percent("charged", 1.0);
            damage("burst", 0.8);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "{damage} урона мгновенно; если на цели есть статика — на {charged} больше. "
                    + "Добавляет слой статики.";
        }

        @Override
        public void execute(SkillContext ctx) {
            boolean charged = ctx.stacks(ctx.target, StatusType.STATIC) > 0;
            ctx.hitShare(ctx.target, ctx.v("damage") * (charged ? 1 + ctx.v("charged") : 1));
            charge(ctx, ctx.target, "duration", "burst");
            Fx.beam(ctx.level, ctx.caster.position().add(0, 1.4, 0), ctx.target.position().add(0, 1, 0), 0.14f, 6,
                    0xD6E8FF);
            ctx.soundAt(ctx.target.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6f, 1.8f);
        }
    }

    /** Статика: сразу два слоя — чтобы быстрее дойти до разряда. */
    public static final class Static extends Skill {
        public Static() {
            super("storm.static", "Статика", 0xB8C6FF, "⌁", 10, 0);
            blocks("range", 20);
            blocks("radius", 5);
            count("stacks", 2);
            damage("burst", 1.0);
            seconds("duration", 10);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Кладёт {stacks} слоя статики на цель и по одному на врагов в {radius} от неё. "
                    + "Пятый слой разряжается на {burst} по всем рядом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("stacks"));
            for (int i = 0; i < n; i++) charge(ctx, ctx.target, "duration", "burst");
            for (LivingEntity e : ctx.enemiesAround(ctx.target.position(), ctx.v("radius"))) {
                if (e != ctx.target) charge(ctx, e, "duration", "burst");
            }
            Fx.ring(ctx.level, ctx.target.position(), (float) ctx.v("radius"), 10, 0xB8C6FF);
            ctx.soundAt(ctx.target.position(), SoundEvents.BEACON_POWER_SELECT, 0.8f, 1.9f);
        }
    }

    /** Грозовая туча: висит над врагом и бьёт его и соседей. */
    public static final class StormCloud extends Skill {
        public StormCloud() {
            super("storm.cloud", "Грозовая туча", 0x6B7AB8, "☁", 34, 0);
            blocks("range", 22);
            blocks("radius", 4);
            seconds("duration", 8);
            damage("damage", 0.6);
            damage("burst", 0.8);
            seconds("static_time", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Туча идёт за целью {duration}: каждую секунду бьёт её и всех в {radius} на {damage} "
                    + "и кладёт статику. Цель умерла — туча остаётся на её месте.";
        }

        @Override
        public void execute(SkillContext ctx) {
            LivingEntity victim = ctx.target;
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.mark(ctx.level, victim, "☁", dur, 0x6B7AB8);
            Zones.add(ctx.level, victim.position(), r, ctx.caster.getUUID(), dur, 20, z -> {
                Vec3 at = victim.isAlive() ? victim.position() : z.center;
                for (LivingEntity e : ctx.enemiesAround(at, r)) {
                    ctx.hit(e, "damage");
                    charge(ctx, e, "static_time", "burst");
                    Fx.beam(z.level, at.add(0, 5, 0), e.position().add(0, 1, 0), 0.12f, 6, 0x8FA6FF);
                }
                Fx.disc(z.level, at, (float) r, 20, 0x6B7AB8, false);
            });
            ctx.soundAt(victim.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.8f);
        }
    }

    /** Громовой шаг: телепорт с ударом молнии в точке. */
    public static final class ThunderStep extends Skill {
        public ThunderStep() {
            super("storm.step", "Громовой шаг", 0xE8F0FF, "⇅", 22, 0);
            blocks("range", 16);
            blocks("radius", 3.5);
            damage("damage", 1.6);
            seconds("stun", 1);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Шаг в точку до {range} с ударом молнии: {damage} урона и оглушение на {stun} всем "
                    + "в {radius} вокруг места выхода.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.teleport(ctx.point);
            for (LivingEntity e : ctx.enemiesAround(ctx.point, ctx.v("radius"))) {
                ctx.hit(e, "damage");
                ctx.status(e, StatusType.STUN, 1, "stun");
            }
            Fx.pillar(ctx.level, ctx.point, 1.0f, 6f, 10, 0xE8F0FF);
            Fx.bolt(ctx.level, ctx.point.add(0, 10, 0), ctx.point, 0.3f, 10, 0xE8F0FF);
            Fx.ring(ctx.level, ctx.point, (float) ctx.v("radius"), 10, 0xE8F0FF);
            ctx.soundAt(ctx.point, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 1f);
        }
    }

    /** Буря: гроза над площадкой боя. */
    public static final class Tempest extends Skill {
        public Tempest() {
            super("storm.tempest", "Буря", 0x4A5A9B, "✹", 120, 0);
            blocks("range", 18);
            blocks("radius", 9);
            seconds("duration", 8);
            damage("damage", 1.1);
            count("strikes", 2);
            damage("burst", 0.8);
            seconds("static_time", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Буря в {radius} на {duration}: каждую секунду {strikes} молнии бьют случайных врагов "
                    + "внутри на {damage} и кладут статику.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            int strikes = (int) Math.round(ctx.v("strikes"));
            Fx.disc(ctx.level, ctx.point, (float) r, dur, 0x4A5A9B, false);
            Fx.pillar(ctx.level, ctx.point, (float) r, 7f, dur, 0x4A5A9B);
            Fx.dome(ctx.level, ctx.point, (float) r, dur, 0x6B7AB8);
            Zones.add(ctx.level, ctx.point, r, ctx.caster.getUUID(), dur, 20, z -> {
                var foes = ctx.enemiesAround(z.center, z.radius);
                if (foes.isEmpty()) return;
                for (int i = 0; i < strikes; i++) {
                    LivingEntity e = foes.get(z.level.random.nextInt(foes.size()));
                    ctx.hit(e, "damage");
                    charge(ctx, e, "static_time", "burst");
                    Fx.pillar(z.level, e.position(), 0.6f, 6f, 8, 0xE8F0FF);
                    Fx.bolt(z.level, z.center.add(0, 7, 0), e.position().add(0, 1, 0), 0.22f, 8, 0xE8F0FF);
                    ctx.soundAt(e.position(), SoundEvents.LIGHTNING_BOLT_IMPACT, 0.7f, 1.2f);
                }
            });
            ctx.soundAt(ctx.point, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 0.6f);
        }
    }
}
