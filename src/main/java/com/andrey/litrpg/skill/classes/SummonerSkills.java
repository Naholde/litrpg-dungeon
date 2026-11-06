package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.prop.SummonEntity;
import com.andrey.litrpg.skill.prop.SummonKind;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Призыватель — играет армией. Помощники — настоящие существа: ходят за хозяином, сами бросаются на врагов,
 * их видно и их можно убить. Приказ бросает всех на одну цель, «Разрыв душ» лопает их ради удара по площади,
 * а «Легион» поднимает всех разом.
 */
public final class SummonerSkills {
    private SummonerSkills() {
    }

    /** Поднять скелета: помощник с мечом. */
    public static final class RaiseSkeleton extends Skill {
        public RaiseSkeleton() {
            super("summon.skeleton", "Поднять скелета", 0xBFE8FF, "☗", 14, 0);
            blocks("range", 12);
            shield("hp", 4.0);
            damage("damage", 1.015);
            seconds("duration", 40);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Поднимает скелета на {duration}: {hp} здоровья, бьёт на {damage} и сам идёт на врагов. "
                    + "Его можно убить — тогда он рассыпается.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.summon(SummonKind.SKELETON, ctx.point, "hp", "damage", "duration");
            ctx.soundAt(ctx.point, SoundEvents.SKELETON_CONVERTED_TO_STRAY, 1f, 0.8f);
        }
    }

    /** Тени-волки: двое быстрых. */
    public static final class ShadowWolves extends Skill {
        public ShadowWolves() {
            super("summon.wolves", "Тени-волки", 0x9B5CFF, "⛤", 20, 0);
            blocks("range", 12);
            shield("hp", 2.2);
            damage("damage", 0.738);
            count("count", 2);
            seconds("duration", 30);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "{count} теневых волка на {duration}: по {hp} здоровья и {damage} за укус. "
                    + "Быстрые — догоняют стрелков и лекарей.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int n = (int) Math.round(ctx.v("count"));
            for (int i = 0; i < n; i++) {
                Vec3 at = ctx.point.add((i - (n - 1) / 2.0) * 1.5, 0, 0);
                ctx.summon(SummonKind.WOLF, at, "hp", "damage", "duration");
            }
            ctx.soundAt(ctx.point, SoundEvents.WOLF_GROWL, 1f, 0.7f);
        }
    }

    /** Приказ: все помощники — на одну цель. */
    public static final class Order extends Skill {
        public Order() {
            super("summon.order", "Приказ", 0xFFD27A, "➤", 10, 0);
            blocks("range", 25);
            blocks("search", 30);
            seconds("duration", 8);
            percent("power", 0.6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Все помощники в {search} бросаются на цель и {duration} бьют на {power} сильнее. "
                    + "Так армия убивает по одному, а не бегает кто куда.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<SummonEntity> army = ctx.mySummons(ctx.v("search"));
            if (army.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Приказывать некому — помощников рядом нет.");
                return;
            }
            for (SummonEntity s : army) {
                s.order(ctx.target);
                com.andrey.litrpg.status.StatusService.apply(s, com.andrey.litrpg.status.Status.of(
                        StatusType.DAMAGE_DEALT, "summon.order", ctx.caster.getUUID(), 1 + ctx.v("power"),
                        ctx.now(), ctx.ticks("duration")));
                Fx.beam(ctx.level, s.position().add(0, 1, 0), ctx.target.position().add(0, 1, 0), 0.1f, 10, 0xFFD27A);
            }
            Fx.mark(ctx.level, ctx.target, "➤", ctx.ticks("duration"), 0xFFD27A);
            ctx.sound(SoundEvents.RAID_HORN.value(), 0.7f, 1.4f);
        }
    }

    /** Костяной голем: большой и тяжёлый, держит врагов на себе. */
    public static final class BoneGolem extends Skill {
        public BoneGolem() {
            super("summon.golem", "Костяной голем", 0xE8E8D8, "⛨", 50, 0);
            blocks("range", 10);
            shield("hp", 10.0);
            damage("damage", 1.292);
            seconds("duration", 30);
            seconds("hold", 6);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Голем на {duration}: {hp} здоровья, {damage} за удар. Враги рядом бьют его, а не вас "
                    + "({hold} за раз) — это почти второй танк.";
        }

        @Override
        public void execute(SkillContext ctx) {
            SummonEntity golem = ctx.summon(SummonKind.GOLEM, ctx.point, "hp", "damage", "duration");
            for (LivingEntity e : ctx.enemiesAround(ctx.point, 8)) ctx.decoy(e, golem, "hold");
            Fx.pillar(ctx.level, ctx.point, 1.2f, 3.5f, 20, 0xE8E8D8);
            ctx.soundAt(ctx.point, SoundEvents.IRON_GOLEM_REPAIR, 1f, 0.6f);
        }
    }

    /** Разрыв душ: лопнуть всех помощников ради удара по площади. */
    public static final class SoulBurst extends Skill {
        public SoulBurst() {
            super("summon.burst", "Разрыв душ", 0x8B5CFF, "✹", 40, 0);
            blocks("search", 30);
            blocks("radius", 4.5);
            damage("damage", 3.692);
            count("souls", 15);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Каждый ваш помощник в {search} лопается: {damage} урона всем в {radius} от него "
                    + "и {souls} душ вам за каждого. Армия кончилась — зато комната пустая.";
        }

        @Override
        public void execute(SkillContext ctx) {
            List<SummonEntity> army = ctx.mySummons(ctx.v("search"));
            if (army.isEmpty()) {
                com.andrey.litrpg.core.SystemChat.send(ctx.caster, "Рвать некого — помощников нет.");
                return;
            }
            for (SummonEntity s : army) {
                for (LivingEntity e : ctx.enemiesAround(s.position(), ctx.v("radius"))) ctx.hit(e, "damage");
                Fx.ring(ctx.level, s.position(), (float) ctx.v("radius"), 10, 0x8B5CFF);
                ctx.gain(ctx.v("souls"));
                s.burst();
            }
            ctx.sound(SoundEvents.SOUL_ESCAPE.value(), 1.1f, 0.6f);
        }
    }

    /** Легион: вся армия разом. */
    public static final class Legion extends Skill {
        public Legion() {
            super("summon.legion", "Легион", 0xFFE8A6, "✺", 120, 0);
            blocks("radius", 4);
            count("cost", 40);
            shield("hp", 5.0);
            damage("damage", 1.108);
            count("skeletons", 3);
            count("wolves", 2);
            seconds("duration", 25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} душ поднимает легион на {duration}: {skeletons} скелета, {wolves} волка "
                    + "и костяного голема вокруг себя (по {hp} здоровья, {damage} за удар).";
        }

        @Override
        public void execute(SkillContext ctx) {
            int sk = (int) Math.round(ctx.v("skeletons")), wo = (int) Math.round(ctx.v("wolves"));
            double r = ctx.v("radius");
            int total = sk + wo + 1;
            int i = 0;
            for (int n = 0; n < sk; n++, i++) {
                ctx.summon(SummonKind.SKELETON, around(ctx, r, i, total), "hp", "damage", "duration");
            }
            for (int n = 0; n < wo; n++, i++) {
                ctx.summon(SummonKind.WOLF, around(ctx, r, i, total), "hp", "damage", "duration");
            }
            ctx.summon(SummonKind.GOLEM, around(ctx, r, i, total), "hp", "damage", "duration");
            Fx.ring(ctx.level, ctx.caster.position(), (float) r + 1, 16, 0xFFE8A6);
            Fx.wave(ctx.level, ctx.caster.position(), (float) r + 1, 2f, 18, 0xFFE8A6);
            ctx.sound(SoundEvents.WITHER_SPAWN, 0.8f, 1.2f);
        }

        private static Vec3 around(SkillContext ctx, double r, int i, int total) {
            double a = Math.PI * 2 * i / Math.max(1, total);
            return ctx.caster.position().add(Math.cos(a) * r, 0, Math.sin(a) * r);
        }
    }
}
