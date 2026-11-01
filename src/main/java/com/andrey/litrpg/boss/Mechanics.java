package com.andrey.litrpg.boss;

import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Приёмы, из которых собираются идеи хранителей. Каждый требует от отряда чего-то своего:
 * спрятаться за колонной, увести погоню от своих, разбежаться или, наоборот, держаться вместе,
 * встать в просвет кольца, успеть на уцелевший пятачок, разорвать взгляд.
 * <p>Цифры берутся из {@code bosses.json} по ключам навыка, поэтому любой приём настраивается без кода.
 */
public final class Mechanics {
    private Mechanics() {
    }

    private static ServerLevel level(BossEntity.Cast c) {
        return (ServerLevel) c.boss().level();
    }

    /**
     * Залп: {@code shots} быстрых выстрелов подряд по случайным бойцам. Выстрел попадает только если
     * хранитель видит цель — за колонной или за углом залп не достаёт. Это главный приём «ищи укрытие».
     */
    public static void volley(BossEntity.Cast c, int shots, int everyTicks, double units, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": залп! Прячьтесь за укрытия — за ними не достанет.")
                .withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < shots; i++) {
            c.at(c.ticks + i * everyTicks, () -> {
                List<ServerPlayer> all = b.playersInArena();
                if (all.isEmpty()) return;
                ServerPlayer p = all.get(b.getRandom().nextInt(all.size()));
                Vec3 from = b.position().add(0, b.getBbHeight() * 0.7, 0);
                Vec3 to = p.position().add(0, 1, 0);
                Fx.bolt(level(b), from, to, 0.18f, 6, color);
                level(b).playSound(null, b.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 1.1f, 1.3f);
                if (b.hasLineOfSight(p)) {
                    BossDamage.hit(b, p, units);
                    Fx.burst(level(b), to, 1.1f, 6, color);
                } else {
                    Fx.burst(level(b), to.add(0, 0.2, 0), 0.6f, 6, 0x9BA6B8);
                }
            });
        }
    }

    private static ServerLevel level(BossEntity b) {
        return (ServerLevel) b.level();
    }

    /**
     * Погоня: пятно ползёт за выбранным бойцом {@code seconds} секунд и взрывается. Убегать нужно
     * в сторону от отряда — иначе взрыв накроет всех.
     */
    public static void hunt(BossEntity.Cast c, int seconds, double radius, double units, int color) {
        BossEntity b = c.boss();
        ServerPlayer prey = b.randomNonTarget();
        if (prey == null && b.getTarget() instanceof ServerPlayer t) prey = t;
        if (prey == null) return;
        final ServerPlayer fixed = prey;
        hunt(c, fixed, seconds, radius, units, color);
    }

    /** Погоня за конкретным бойцом. */
    public static void hunt(BossEntity.Cast c, ServerPlayer prey, int seconds, double radius, double units, int color) {
        BossEntity b = c.boss();
        if (prey == null) return;
        b.tell(Component.literal(b.type().name() + ": погоня за " + prey.getGameProfile().getName()
                + " — уводите её от отряда!").withStyle(ChatFormatting.GOLD));
        Vec3[] at = {prey.position()};
        int steps = seconds * 4;
        for (int i = 0; i < steps; i++) {
            c.at(c.ticks + i * 5, () -> {
                Vec3 goal = prey.isAlive() ? prey.position() : at[0];
                Vec3 dir = goal.subtract(at[0]);
                double len = dir.length();
                at[0] = len < 0.3 ? goal : at[0].add(dir.scale(Math.min(1, 0.75 / len)));
                Fx.disc(level(b), at[0], (float) radius, 6, color, false);
                Fx.orb(level(b), at[0].add(0, 0.6, 0), (float) radius * 0.4f, 6, color);
            });
        }
        c.at(c.ticks + steps * 5, () -> {
            Fx.burst(level(b), at[0].add(0, 0.5, 0), (float) radius * 1.4f, 10, color);
            Fx.wave(level(b), at[0], (float) radius * 1.6f, 1.6f, 12, color);
            level(b).playSound(null, net.minecraft.core.BlockPos.containing(at[0]), SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.HOSTILE, 1.4f, 0.7f);
            for (ServerPlayer p : b.playersInArena()) {
                if (p.position().distanceTo(at[0]) <= radius * 1.4) BossDamage.hit(b, p, units);
            }
        });
    }

    /**
     * Связь: двоих бойцов стягивает нитью. Разошлись дальше {@code maxDist} — обоим больно каждую секунду,
     * поэтому приходится двигаться парой и вместе уходить от остального.
     */
    public static void tether(BossEntity.Cast c, int seconds, double maxDist, double unitsPerSecond, int color) {
        List<ServerPlayer> all = new ArrayList<>(c.boss().playersInArena());
        if (all.size() < 2) return;
        java.util.Collections.shuffle(all, new java.util.Random(c.boss().level().getGameTime()));
        tether(c, all.get(0), all.get(1), seconds, maxDist, unitsPerSecond, color);
    }

    /** Связь между двумя названными бойцами. */
    public static void tether(BossEntity.Cast c, ServerPlayer a, ServerPlayer d, int seconds, double maxDist,
                              double unitsPerSecond, int color) {
        BossEntity b = c.boss();
        if (a == null || d == null || a == d) return;
        b.tell(Component.literal(b.type().name() + ": " + a.getGameProfile().getName() + " и "
                + d.getGameProfile().getName() + " связаны — держитесь рядом!").withStyle(ChatFormatting.GOLD));
        for (int i = 0; i < seconds; i++) {
            c.at(c.ticks + i * 20, () -> {
                if (!a.isAlive() || !d.isAlive()) return;
                double dist = a.position().distanceTo(d.position());
                boolean far = dist > maxDist;
                Fx.bolt(level(b), a, d, far ? 0.22f : 0.12f, 20, far ? Fx.DANGER : color);
                if (far) {
                    BossDamage.hit(b, a, unitsPerSecond);
                    BossDamage.hit(b, d, unitsPerSecond);
                    a.displayClientMessage(Component.literal("Нить рвётся — ближе к "
                            + d.getGameProfile().getName() + "!").withStyle(ChatFormatting.RED), true);
                }
            });
        }
    }

    /**
     * Кольца с просветом: {@code waves} волн расходятся от хранителя, в каждой есть безопасный сектор,
     * и он каждый раз в другом месте — надо успеть перебежать в просвет.
     */
    public static void rings(BossEntity.Cast c, int waves, double units, double gapDeg, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": кольца! Ищите просвет и бегите в него.")
                .withStyle(ChatFormatting.GOLD));
        for (int w = 0; w < waves; w++) {
            final double gap = b.getRandom().nextDouble() * 360;
            final float radius = (float) (3 + w * 3.2);
            c.at(c.ticks + w * 24, () -> {
                Vec3 center = b.position();
                Fx.ring(level(b), center, radius, 18, color);
                // Просвет видно заранее: в нём разметки нет.
                Fx.cone(level(b), center, new Vec3(Math.cos(Math.toRadians(gap)), 0, Math.sin(Math.toRadians(gap))),
                        radius + 1.5f, (float) gapDeg, 18, 0x7BF5A0, false);
                level(b).playSound(null, b.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 1.2f, 1.4f);
            });
            c.at(c.ticks + w * 24 + 18, () -> {
                Vec3 center = b.position();
                for (ServerPlayer p : b.playersInArena()) {
                    Vec3 to = p.position().subtract(center).multiply(1, 0, 1);
                    double d = to.length();
                    if (d < radius - 1.6 || d > radius + 1.6) continue;
                    double ang = Math.toDegrees(Math.atan2(to.z, to.x));
                    double diff = Math.abs(((ang - gap + 540) % 360) - 180);
                    if (180 - diff > gapDeg / 2) BossDamage.hit(b, p, units);
                }
                Fx.wave(level(b), center, radius + 1.5f, 1.2f, 10, color);
            });
        }
    }

    /**
     * Пол уходит: остаются {@code safe} пятачков, всё остальное бьёт. С каждой волной пятачков меньше —
     * отряд сходится всё теснее, а уйти из-под удара можно только рывком.
     */
    public static void crumble(BossEntity.Cast c, int waves, int safe, double radius, double units, int color) {
        BossEntity b = c.boss();
        b.tell(Component.literal(b.type().name() + ": пол рушится! Держитесь на уцелевших пятачках.")
                .withStyle(ChatFormatting.GOLD));
        for (int w = 0; w < waves; w++) {
            final int count = Math.max(1, safe - w);
            c.at(c.ticks + w * 50, () -> {
                List<Vec3> spots = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    double a = Math.PI * 2 * i / count + b.getRandom().nextDouble();
                    double r = b.arenaRadius() * (0.35 + b.getRandom().nextDouble() * 0.45);
                    Vec3 at = b.home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                    spots.add(at);
                    Fx.disc(level(b), at, (float) radius, 34, 0x7BF5A0, false);
                    Fx.spiral(level(b), at, (float) radius * 0.6f, 2f, 34, 0x7BF5A0);
                }
                b.schedule(34, () -> {
                    for (ServerPlayer p : b.playersInArena()) {
                        boolean ok = spots.stream().anyMatch(s -> p.position().distanceTo(s) <= radius);
                        if (!ok) BossDamage.hit(b, p, units);
                    }
                    Fx.wave(level(b), b.home(), (float) b.arenaRadius(), 2f, 14, color);
                    level(b).playSound(null, b.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.3f, 0.6f);
                });
            });
        }
    }

    /**
     * Взгляд: хранитель смотрит в одну сторону {@code seconds} секунд и жжёт всех в конусе каждую секунду.
     * Спасает либо выйти из сектора, либо разорвать взгляд — спрятаться за укрытием.
     */
    public static void gaze(BossEntity.Cast c, int seconds, double angleDeg, double unitsPerSecond, int color) {
        BossEntity b = c.boss();
        ServerPlayer first = b.getTarget() instanceof ServerPlayer p ? p : null;
        Vec3 dir = first != null ? first.position().subtract(b.position()).multiply(1, 0, 1).normalize()
                : b.getViewVector(1).multiply(1, 0, 1).normalize();
        b.tell(Component.literal(b.type().name() + ": взгляд! Уйдите из сектора или спрячьтесь за укрытие.")
                .withStyle(ChatFormatting.GOLD));
        double cos = Math.cos(Math.toRadians(angleDeg / 2));
        for (int i = 0; i < seconds; i++) {
            c.at(c.ticks + i * 20, () -> {
                Fx.cone(level(b), b.position(), dir, (float) b.arenaRadius(), (float) angleDeg, 20, color, false);
                Fx.bolt(level(b), b.position().add(0, b.getBbHeight() * 0.8, 0),
                        b.position().add(dir.scale(b.arenaRadius())).add(0, 1, 0), 0.25f, 20, color);
                for (ServerPlayer p : b.playersInArena()) {
                    Vec3 to = p.position().subtract(b.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() < 0.1 || to.normalize().dot(dir) < cos) continue;
                    if (!b.hasLineOfSight(p)) continue;
                    BossDamage.hit(b, p, unitsPerSecond);
                }
            });
        }
    }

    /**
     * Рывок: хранитель разгоняется по прямой и сбивает всех на пути. Линия видна заранее —
     * уходить вбок, а не бежать вперёд.
     */
    public static void charge(BossEntity.Cast c, ServerPlayer at, double units, double knockback, int color) {
        BossEntity b = c.boss();
        if (at == null) return;
        Vec3 from = b.position();
        Vec3 dir = at.position().subtract(from).multiply(1, 0, 1).normalize();
        Vec3 to = from.add(dir.scale(b.arenaRadius() * 1.2));
        Telegraph.line(c, from, to, 3.2f, units, p -> {
            p.setDeltaMovement(dir.x * knockback, 0.42, dir.z * knockback);
            p.hurtMarked = true;
        });
        c.at(c.ticks, () -> {
            b.teleportTo(to.x, b.getY(), to.z);
            Fx.wave(level(b), to, 4f, 1.4f, 12, color);
            level(b).playSound(null, b.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.4f, 0.7f);
        });
    }
}
