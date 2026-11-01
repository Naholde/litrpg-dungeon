package com.andrey.litrpg.skill.classes;

import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.skill.Skill;
import com.andrey.litrpg.skill.SkillContext;
import com.andrey.litrpg.skill.Targeting;
import com.andrey.litrpg.skill.Zones;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ледяной бастион — танк, который меняет саму площадку боя. Обморожение копится слоями и на пятом сковывает,
 * стена из настоящего льда перекрывает проход и снаряды, вечная мерзлота отнимает у врагов силу удара,
 * а гробница делает бастиона неуязвимым, но неподвижным — и держит всех вокруг.
 */
public final class BastionSkills {
    private BastionSkills() {
    }

    /** Обморожение: слой; на пятом слое — сковывает. Общий для всех навыков бастиона. */
    static void chill(SkillContext ctx, LivingEntity e, String secondsKey) {
        int st = ctx.addStack(e, StatusType.CHILL, secondsKey, 5);
        Fx.mark(ctx.level, e, "❅", 20, 0x8FE3FF);
        if (st >= 5) {
            StatusService.removeAll(e, StatusType.CHILL);
            StatusService.apply(e, Status.of(StatusType.FROZEN, "bastion.freeze", ctx.caster.getUUID(), 1,
                    ctx.now(), 50));
            Fx.pillar(ctx.level, e.position(), 0.9f, 2.5f, 50, 0x8FE3FF);
            ctx.soundAt(e.position(), SoundEvents.GLASS_PLACE, 1f, 0.6f);
        }
    }

    /** Ледяной панцирь: щит, бьющие мёрзнут, а разбитый панцирь взрывается льдом. */
    public static final class IceArmor extends Skill {
        public IceArmor() {
            super("bastion.armor", "Ледяной панцирь", 0x8FE3FF, "⛉", 24, 0);
            shield("shield", 2.6);
            seconds("duration", 10);
            blocks("radius", 2.5);
            damage("burst", 1.1);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Панцирь на {shield} и {duration}: кто бьёт вас вблизи ({radius}), получает слой обморожения. "
                    + "Разбили панцирь — лёд лопается на {burst} по всем вокруг.";
        }

        @Override
        public void execute(SkillContext ctx) {
            ctx.shield(ctx.caster, "shield", "duration");
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            Fx.discOn(ctx.level, ctx.caster, (float) r, dur, 0x8FE3FF, false);
            Fx.domeOn(ctx.level, ctx.caster, 1.3f, dur, 0x8FE3FF);
            boolean[] burst = {false};
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 10, z -> {
                if (burst[0]) return;
                Vec3 me = ctx.caster.position();
                for (LivingEntity e : ctx.enemiesAround(me, r)) chill(ctx, e, "duration");
                boolean gone = StatusService.list(ctx.caster).shieldTotal(z.level.getGameTime()) <= 0;
                if (gone) {
                    burst[0] = true;
                    for (LivingEntity e : ctx.enemiesAround(me, r + 2)) {
                        ctx.hit(e, "burst");
                        chill(ctx, e, "duration");
                    }
                    Fx.ring(z.level, me, (float) r + 2, 12, 0x8FE3FF);
                    ctx.sound(SoundEvents.GLASS_BREAK, 1.2f, 0.7f);
                }
            });
            ctx.sound(SoundEvents.POWDER_SNOW_PLACE, 1f, 0.8f);
        }
    }

    /** Стужа: аура обморожения вокруг бастиона. */
    public static final class Frost extends Skill {
        public Frost() {
            super("bastion.frost", "Стужа", 0xB0E8FF, "❅", 18, 0);
            blocks("radius", 6);
            seconds("duration", 8);
            percent("slow", 0.2);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "Стужа в {radius} на {duration}: каждую секунду врагам рядом — слой обморожения и замедление "
                    + "на {slow}. Пять слоёв сковывают врага льдом.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double slow = Math.min(0.7, ctx.v("slow"));
            Fx.discOn(ctx.level, ctx.caster, (float) r, dur, 0xB0E8FF, false);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 20, z -> {
                Vec3 me = ctx.caster.position();
                for (LivingEntity e : ctx.enemiesAround(me, r)) {
                    chill(ctx, e, "duration");
                    StatusService.apply(e, Status.of(StatusType.SLOW, "bastion.frost", ctx.caster.getUUID(), slow,
                            z.level.getGameTime(), 30));
                    ctx.threat(e, 1.5);
                }
                Fx.ring(z.level, me, (float) r, 18, 0xB0E8FF);
            });
            ctx.sound(SoundEvents.PLAYER_HURT_FREEZE, 0.8f, 1.2f);
        }
    }

    /** Ледяная стена: настоящие блоки льда, которые потом тают. */
    public static final class IceWall extends Skill {
        public IceWall() {
            super("bastion.wall", "Ледяная стена", 0xD6F3FF, "▦", 35, 0);
            blocks("range", 10);
            count("width", 5);
            count("height", 3);
            seconds("duration", 8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.GROUND;
        }

        @Override
        public String template() {
            return "Поднимает стену льда {width} в ширину и {height} в высоту на {duration}: перекрывает проход "
                    + "и снаряды. Лёд потом тает, и место остаётся как было.";
        }

        @Override
        public void execute(SkillContext ctx) {
            Vec3 dir = ctx.caster.getViewVector(1).multiply(1, 0, 1).normalize();
            Vec3 side = new Vec3(-dir.z, 0, dir.x);
            int w = (int) Math.round(ctx.v("width")), h = (int) Math.round(ctx.v("height"));
            List<BlockPos> placed = new ArrayList<>();
            List<BlockState> before = new ArrayList<>();
            BlockState ice = Blocks.PACKED_ICE.defaultBlockState();
            for (int i = -w / 2; i <= w / 2; i++) {
                for (int y = 0; y < h; y++) {
                    BlockPos pos = BlockPos.containing(ctx.point.add(side.scale(i)).add(0, y + 0.1, 0));
                    BlockState was = ctx.level.getBlockState(pos);
                    if (!was.isAir() && !was.canBeReplaced()) continue;
                    ctx.level.setBlock(pos, ice, Block.UPDATE_ALL);
                    placed.add(pos);
                    before.add(was);
                }
            }
            Fx.line(ctx.level, ctx.point.add(side.scale(-w / 2.0)), ctx.point.add(side.scale(w / 2.0)), 1.2f, 20,
                    0xD6F3FF, false);
            ctx.later(ctx.ticks("duration"), () -> {
                for (int i = 0; i < placed.size(); i++) {
                    BlockPos pos = placed.get(i);
                    if (ctx.level.getBlockState(pos).is(Blocks.PACKED_ICE)) {
                        ctx.level.setBlock(pos, before.get(i), Block.UPDATE_ALL);
                    }
                }
                ctx.soundAt(ctx.point, SoundEvents.GLASS_BREAK, 0.8f, 1.2f);
            });
            ctx.soundAt(ctx.point, SoundEvents.POWDER_SNOW_PLACE, 1.2f, 0.7f);
        }
    }

    /** Вечная мерзлота: врагам в радиусе тяжело бить. */
    public static final class Permafrost extends Skill {
        public Permafrost() {
            super("bastion.permafrost", "Вечная мерзлота", 0x7FB8FF, "✶", 50, 0);
            blocks("radius", 8);
            seconds("duration", 8);
            percent("weaken", 0.375);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: всё в {radius} сковано холодом — враги бьют на {weaken} слабее и двигаются "
                    + "вполсилы. Площадка боя становится вашей.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            double weaken = 1 - Math.min(0.8, ctx.v("weaken"));
            Fx.disc(ctx.level, ctx.caster.position(), (float) r, dur, 0x7FB8FF, false);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 10, z -> {
                long now = z.level.getGameTime();
                for (LivingEntity e : ctx.enemiesAround(z.center, z.radius)) {
                    StatusService.apply(e, Status.of(StatusType.DAMAGE_DEALT, "bastion.permafrost",
                            ctx.caster.getUUID(), weaken, now, 14));
                    StatusService.apply(e, Status.of(StatusType.SLOW, "bastion.permafrost", ctx.caster.getUUID(),
                            0.4, now, 14));
                }
            });
            ctx.sound(SoundEvents.BEACON_ACTIVATE, 0.9f, 0.7f);
        }
    }

    /** Ледяная гробница: неуязвим и неподвижен, всех рядом держит. */
    public static final class Tomb extends Skill {
        public Tomb() {
            super("bastion.tomb", "Ледяная гробница", 0xFFFFFF, "⬣", 104, 0);
            blocks("radius", 7);
            seconds("duration", 6);
            damage("burst", 1.8);
        }

        @Override
        public Targeting targeting() {
            return Targeting.SELF;
        }

        @Override
        public String template() {
            return "{duration}: бастион в глыбе льда — урон по нему не проходит, но и сам он не двигается. "
                    + "Врагов в {radius} тянет к нему и держит; в конце лёд лопается на {burst}.";
        }

        @Override
        public void execute(SkillContext ctx) {
            int dur = ctx.ticks("duration");
            double r = ctx.v("radius");
            ctx.status(ctx.caster, StatusType.DAMAGE_TAKEN, 0, "duration");
            StatusService.apply(ctx.caster, Status.of(StatusType.ROOT, "bastion.tomb", ctx.caster.getUUID(), 1,
                    ctx.now(), dur));
            Fx.pillar(ctx.level, ctx.caster.position(), 1.4f, 4f, dur, 0xFFFFFF);
            Fx.dome(ctx.level, ctx.caster.position(), (float) r, dur, 0xD6F3FF);
            Fx.disc(ctx.level, ctx.caster.position(), (float) r, dur, 0xD6F3FF, false);
            Zones.add(ctx.level, ctx.caster.position(), r, ctx.caster.getUUID(), dur, 10, z -> {
                for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), r)) {
                    ctx.pull(e, ctx.caster.position(), 0.9);
                    ctx.threat(e, 4);
                    StatusService.apply(e, Status.of(StatusType.SLOW, "bastion.tomb", ctx.caster.getUUID(), 0.5,
                            z.level.getGameTime(), 14));
                }
            });
            ctx.later(dur, () -> {
                for (LivingEntity e : ctx.enemiesAround(ctx.caster.position(), r)) {
                    ctx.hit(e, "burst");
                    chill(ctx, e, "duration");
                }
                Fx.ring(ctx.level, ctx.caster.position(), (float) r, 14, 0xFFFFFF);
                ctx.sound(SoundEvents.GLASS_BREAK, 1.3f, 0.6f);
            });
            ctx.sound(SoundEvents.PLAYER_HURT_FREEZE, 1f, 0.6f);
        }
    }
}
