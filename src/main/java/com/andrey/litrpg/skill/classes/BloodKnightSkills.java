package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.combat.HealService;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;

/**
 * Кровавый рыцарь — танк, который платит кровью и пьёт её обратно. Ресурс «Кровь» копится от полученного урона;
 * удары стоят здоровья, но лечат, а на последней половине жизни лечат вдвое. Клеймо поит рыцаря с каждого удара
 * по цели, а «Неумирающий» за всю кровь не даёт умереть — но после приходит расплата.
 */
public final class BloodKnightSkills {
    private BloodKnightSkills() {
    }

    /** Кровавый удар: стоит здоровья, но лечит; вдвое сильнее лечит, когда здоровья меньше половины. */
    public static final class BloodStrike extends Skill {
        public BloodStrike() {
            super("blood.strike", "Кровавый удар", 0x8B1A1A, "†", 5, 0);
            blocks("range", 4);
            damage("damage", 1.4);
            heal("heal", 0.8);
            percent("price", 0.08);
            count("blood", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Стоит {price} текущего здоровья: {damage} урона и {heal} здоровья обратно — а если у вас меньше "
                    + "половины жизни, лечит вдвое. Даёт {blood} крови.";
        }

        @Override
        public void execute(SkillContext ctx) {
            float hp = ctx.caster.getHealth();
            float price = (float) Math.min(hp - 1, hp * ctx.v("price"));
            if (price > 0) ctx.caster.setHealth(hp - price);
            boolean low = ctx.caster.getHealth() < ctx.caster.getMaxHealth() * 0.5;
            ctx.hit(ctx.target, "damage");
            HealService.heal(ctx.caster, ctx.caster, ctx.v("heal") * ctx.unit() * (low ? 2 : 1));
            ctx.gain(ctx.v("blood"));
            Fx.slash(ctx.level, ctx.caster, 2.6f, 70, 8, low ? 0xFF2020 : 0x8B1A1A);
            Fx.beam(ctx.level, ctx.target, ctx.caster, 0.14f, 10, 0xD8343A);
            ctx.sound(SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
        }
    }

    /** Клеймо: все бьют цель сильнее, и каждый удар по ней поит рыцаря. */
    public static final class Brand extends Skill {
        public Brand() {
            super("blood.brand", "Клеймо", 0xD8343A, "⛒", 16, 0);
            blocks("range", 20);
            percent("extra", 0.12);
            percent("siphon", 0.1);
            seconds("duration", 12);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Клеймит врага на {duration}: весь отряд бьёт его на {extra} больнее, а {siphon} любого урона "
                    + "по нему возвращается вам здоровьем.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.target, StatusType.DAMAGE_TAKEN, 1 + ctx.v("extra"), "duration");
            ctx.status(ctx.target, StatusType.SIPHON, ctx.v("siphon"), "duration");
            Fx.mark(ctx.level, ctx.target, "⛒", ctx.ticks("duration"), 0xD8343A);
            Fx.discOn(ctx.level, ctx.target, 1.0f, ctx.ticks("duration"), 0xD8343A, false);
            ctx.soundAt(ctx.target.position(), SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
        }
    }

    /** Кровавые цепи: врага тянет к рыцарю, и он бьёт только его. */
    public static final class Chains extends Skill {
        public Chains() {
            super("blood.chains", "Кровавые цепи", 0xA01818, "⛓", 13, 0);
            blocks("range", 14);
            damage("damage", 0.6);
            seconds("taunt", 4);
        }

        @Override
        public Targeting targeting() {
            return Targeting.ENEMY;
        }

        @Override
        public String template() {
            return "Цепь из крови тянет врага к вам, бьёт на {damage} и {taunt} держит его на себе. "
                    + "Хранителя с места не сдвинуть — но провокация действует.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Fx.beam(ctx.level, ctx.caster, ctx.target, 0.2f, 12, 0xA01818);
            ctx.pull(ctx.target, ctx.caster.position(), 1.0);
            ctx.hit(ctx.target, "damage");
            if (ctx.target instanceof net.minecraft.world.entity.Mob m) {
                com.andrey.litrpg.combat.threat.ThreatService.taunt(m, ctx.caster, ctx.ticks("taunt"));
            }
            ctx.sound(SoundEvents.CHAIN_PLACE, 1.1f, 0.6f);
        }
    }

    /** Багряный пир: весь полученный урон запоминается и в конце возвращается здоровьем. */
    public static final class Feast extends Skill {
        public Feast() {
            super("blood.feast", "Багряный пир", 0xB0001A, "✹", 45, 0);
            seconds("duration", 10);
            percent("back", 0.5);
            count("blood", 25);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration} ведёт счёт всему урону по вам, а в конце возвращает {back} этого числа здоровьем "
                    + "и {blood} крови. Чем тяжелее бой, тем больше пир.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.status(ctx.caster, StatusType.RECORD, 0, "duration");
            int dur = ctx.ticks("duration");
            Fx.discOn(ctx.level, ctx.caster, 1.3f, dur, 0xB0001A, false);
            double back = ctx.v("back");
            double blood = ctx.v("blood");
            ctx.later(dur - 1, () -> {
                double sum = StatusService.sum(ctx.caster, StatusType.RECORD);
                StatusService.removeAll(ctx.caster, StatusType.RECORD);
                if (sum > 0) {
                    HealService.healFixed(ctx.caster, ctx.caster, (float) (sum * back));
                    Fx.pillar(ctx.level, ctx.caster.position(), 1.0f, 4, 20, 0xB0001A);
                }
                ctx.gain(blood);
                ctx.sound(SoundEvents.PLAYER_LEVELUP, 0.7f, 0.6f);
            });
            ctx.sound(SoundEvents.RAVAGER_ROAR, 0.8f, 0.7f);
        }
    }

    /**
     * Кровавые нити — главный приём рыцаря. Он протягивает нить к каждому врагу вокруг и тянет по ней их
     * здоровье себе: врагам — урон долей их здоровья, рыцарю — лечение с каждой нити. Чем больше врагов,
     * тем сильнее он лечится; с хранителей тянется меньше, но тоже тянется.
     */
    public static final class Threads extends Skill {
        public Threads() {
            super("blood.threads", "Кровавые нити", 0xB0001A, "⁂", 120, 0);
            blocks("radius", 10);
            seconds("duration", 6);
            percent("drain", 0.03);
            percent("back", 0.7);
            percent("boss_part", 0.25);
            count("cost", 40);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "За {cost} крови протягивает нити ко всем врагам в {radius} на {duration}: два раза в секунду "
                    + "каждый теряет {drain} своего здоровья, а {back} отнятого становится здоровьем рыцаря. "
                    + "С хранителей тянется {boss_part} от этого. Чем больше врагов на нитях, тем больше лечение.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double drain = ctx.v("drain"), back = ctx.v("back"), bossPart = ctx.v("boss_part");
            Fx.disc(ctx.level, ctx.caster.position(), (float) r, dur, 0xB0001A, false);
            Fx.pillar(ctx.level, ctx.caster.position(), 1.2f, 4f, dur, 0xB0001A);
            com.andrey.litrpg.skill.Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 10, z -> {
                if (!ctx.caster.isAlive()) return;
                for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), r)) {
                    boolean boss = com.andrey.litrpg.combat.DamageService.isBoss(e);
                    double part = e.getMaxHealth() * drain * (boss ? bossPart : 1);
                    float before = e.getHealth();
                    com.andrey.litrpg.combat.DamageService.deal(ctx.caster, null, e, part,
                            com.andrey.litrpg.combat.WeaponKind.FIXED);
                    double taken = Math.max(0, before - e.getHealth());
                    if (taken > 0) HealService.healFixed(ctx.caster, ctx.caster, taken * back);
                    ctx.threat(e, taken);
                    Fx.beam(z.level, e, ctx.caster, 0.14f, 12, 0xD8343A);
                }
            });
            ctx.sound(SoundEvents.WITHER_SPAWN, 0.7f, 1.4f);
        }
    }

    /** Общий расчёт для подсказок: сколько лечит удар по клеймённому (используется в тестах). */
    public static double siphonHeal(LivingEntity victim, double dealt, java.util.UUID knight) {
        return dealt * StatusService.magnitudeFrom(victim, StatusType.SIPHON, knight);
    }
}
