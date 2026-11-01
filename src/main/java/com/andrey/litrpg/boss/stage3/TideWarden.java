package com.andrey.litrpg.boss.stage3;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Страж Прилива. «Волна» — главное: с одной из сторон арены идёт вода, спастись можно только за колонной
 * (между тобой и краем, откуда идёт волна, должен быть камень). Трезубец по танку, водоворот тянет к центру и бьёт,
 * пузыри под ногами. С половины волны идут чаще и иногда по две подряд.
 */
public class TideWarden extends BossEntity {
    private static final String[] SIDES = {"севера", "востока", "юга", "запада"};

    public TideWarden(EntityType<? extends TideWarden> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.TIDE_WARDEN;
    }

    @Override
    public String modelId() {
        return "tide_warden";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.5};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Залп прилива", "salvo", 0, false, b -> true, this::salvo),
            new Ability("Круги на воде", "surge", 1, false, b -> true, this::surge),
            new Ability("Волна", "wave", 0, false, b -> true, this::wave),
            new Ability("Водоворот", "whirl", 1, false, b -> poolCount() < 1, this::whirlpool),
            new Ability("Пузыри", "bubbles", 2, false, b -> true, this::bubbles),
            new Ability("Удар трезубцем", "trident", 3, false, b -> b.getTarget() != null, this::trident));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void trident(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 8, 50, p("trident_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
    }

    private void bubbles(Cast c) {
        List<ServerPlayer> ps = playersInArena();
        for (int i = 0; i < Math.min(3, ps.size()); i++) Telegraph.circle(c, ps.get(i).position(), 2.5f, p("bubble_units"));
        triggerAnim("main", "cast");
    }

    /** Водоворот: лужа в центре арены; стоящих внутри тянет к середине и бьёт. */
    private void whirlpool(Cast c) {
        Vec3 at = home();
        Fx.disc((ServerLevel) level(), at, 5, c.ticks, Fx.ICE, true);
        c.at(c.ticks, () -> addPool(at, 5, 0, 5, (int) Math.round(p("whirl_seconds")), p("whirl_units"), Fx.ICE, pl -> {
            Vec3 in = at.subtract(pl.position()).multiply(1, 0, 1);
            if (in.length() > 0.5) {
                Vec3 d = in.normalize().scale(0.35);
                pl.push(d.x, 0, d.z);
                pl.hurtMarked = true;
            }
        }));
    }

    /** Волна с одной из сторон: бьёт всех, кого «видно» с той стороны; спасает колонна между игроком и краем. */
    private void wave(Cast c) {
        int side = random.nextInt(4);
        Vec3 dir = switch (side) {
            case 0 -> new Vec3(0, 0, 1);    // с севера на юг
            case 1 -> new Vec3(-1, 0, 0);
            case 2 -> new Vec3(0, 0, -1);
            default -> new Vec3(1, 0, 0);
        };
        double r = arenaRadius();
        Vec3 from = home().subtract(dir.scale(r)), to = home().add(dir.scale(r));
        Fx.line((ServerLevel) level(), from.add(0, 0.1, 0), to.add(0, 0.1, 0), (float) (r * 2), c.ticks, Fx.ICE, true);
        tell(Component.literal("Волна с " + SIDES[side] + "! Укройтесь за колоннами!").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        triggerAnim("main", "cast");
        c.at(c.ticks, () -> crash(dir, p("wave_units")));
        if (phase() >= 1 && random.nextBoolean()) {
            // вторая волна — с противоположной стороны
            Vec3 back = dir.scale(-1);
            schedule(c.ticks + 30, () -> {
                tell(Component.literal("Отбойная волна!").withStyle(ChatFormatting.AQUA));
                Cast second = testCast(40);
                Fx.line((ServerLevel) level(), home().subtract(back.scale(r)).add(0, 0.1, 0), home().add(back.scale(r)).add(0, 0.1, 0),
                        (float) (r * 2), 40, Fx.ICE, true);
                second.at(40, () -> crash(back, p("wave_units")));
            });
        }
    }

    private void crash(Vec3 dir, double units) {
        ServerLevel sl = (ServerLevel) level();
        for (ServerPlayer p : playersInArena()) {
            Vec3 eye = p.getEyePosition();
            Vec3 src = eye.subtract(dir.scale(arenaRadius() * 2.2));
            var hit = sl.clip(new ClipContext(eye, src, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
            boolean covered = hit.getType() == HitResult.Type.BLOCK && hit.getLocation().distanceTo(eye) < arenaRadius() * 2;
            if (covered) continue;
            BossDamage.hit(this, p, units);
            StatusService.apply(p, Status.of(StatusType.SLOW, "tide_wave", getUUID(), 0.5, level().getGameTime(), 60));
            Vec3 push = dir.scale(1.2);
            p.push(push.x, 0.3, push.z);
            p.hurtMarked = true;
        }
        for (int i = -4; i <= 4; i++) {
            Vec3 at = home().subtract(dir.scale(arenaRadius())).add(new Vec3(dir.z, 0, -dir.x).scale(i * arenaRadius() / 4));
            sl.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 1, at.z, 30, 1, 0.8, 1, 0.3);
        }
        sl.playSound(null, blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 3f, 0.5f);
    }

    @Override
    protected void onPhase(int phase) {
        tell(Component.literal("Страж Прилива призывает шторм — волны идут чаще!").withStyle(ChatFormatting.DARK_AQUA));
    }

    @Override
    public double p(String key) {
        if (key.equals("wave_cd") && phase() >= 1) return super.p(key) * 0.75;
        return super.p(key);
    }

    /** Залп прилива */
    private void salvo(Cast c) {
        Mechanics.volley(c, (int) p("salvo_shots"), 8, p("salvo_units"), 0x8FE3FF);
    }

    /** Круги на воде */
    private void surge(Cast c) {
        Mechanics.rings(c, (int) p("surge_waves"), p("surge_units"), p("surge_gap"), 0x5CB8E8);
    }

}
