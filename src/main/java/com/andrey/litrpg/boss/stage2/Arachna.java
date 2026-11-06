package com.andrey.litrpg.boss.stage2;

import com.andrey.litrpg.boss.BossDamage;
import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.PowerNode;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.mob.Bestiary;
import com.andrey.litrpg.mob.MobRank;
import com.andrey.litrpg.status.Status;
import com.andrey.litrpg.status.StatusService;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Аракна, Королева пауков. Ядовитый укус по танку, паутина полосами (приковывает), кокон (союзники разрезают,
 * внутри — яд), кладки яиц (разбить до вылупления). На 66% и 33% уходит под потолок: неуязвима, сверху ядовитый
 * дождь, затем падает на одного из отряда — разметку видно заранее.
 */
public class Arachna extends BossEntity {
    private final Map<UUID, UUID> cocoons = new HashMap<>();
    private boolean aloft;
    private Vec3 ground;

    public Arachna(EntityType<? extends Arachna> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.ARACHNA;
    }

    @Override
    public String modelId() {
        return "arachna";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.66, 0.33};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Ядовитые лужи", "venompools", 1, false, b -> true, this::venomPools),
            new Ability("Сеть на полу", "net", 1, false, b -> b.phase() >= 1, this::net),
            new Ability("Паучья погоня", "skitter", 0, false, b -> true, this::skitter),
            new Ability("Прыжок охотницы", "pounce", 1, false, b -> farthest() != null, this::pounce),
            new Ability("Ядовитый веер", "venom", 1, false, b -> b.phase() >= 1 && b.getTarget() != null, this::venom),
            new Ability("Нити паутины", "threads", 1, false, b -> true, this::threads),
            new Ability("Кладка", "eggs", 1, false, b -> spawnedAlive().size() < 8, this::eggs),
            new Ability("Кокон", "cocoon", 2, false, b -> b.playersInArena().size() > 1, this::cocoon),
            new Ability("Паутина", "web", 3, false, b -> true, this::web),
            new Ability("Ядовитый укус", "bite", 4, false, b -> b.getTarget() != null, this::bite));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    @Override
    protected boolean dormant() {
        return aloft;
    }

    private void bite(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 6, 70, p("bite_units"));
        schedule(c.ticks - 6, () -> triggerAnim("main", "attack"));
        // яд — тому, кто к моменту укуса остался перед паучихой
        c.at(c.ticks, () -> {
            if (c.target instanceof ServerPlayer p && p.isAlive() && p.distanceTo(this) <= 6.5) poison(p, p("poison_units"), 6);
        });
    }

    private void poison(ServerPlayer p, double units, int seconds) {
        if (p == null) return;
        StatusService.apply(p, Status.periodic(StatusType.DOT, "arachna_poison", getUUID(), units * unit(), level().getGameTime(),
                seconds * 20, 20));
    }

    private void web(Cast c) {
        triggerAnim("main", "attack");
        int n = Math.min(3, playersInArena().size());
        for (int i = 0; i < n; i++) {
            ServerPlayer p = i == 0 ? randomNonTarget() : playersInArena().get(random.nextInt(playersInArena().size()));
            if (p == null) continue;
            Vec3 dir = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 4)), 2f, p("web_units"),
                    hit -> StatusService.apply(hit, Status.of(StatusType.ROOT, "arachna_web", getUUID(), 1, level().getGameTime(), 50)));
        }
        level().playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    /** Кокон: игрок скован и травится, пока союзники не разрежут паутину; не успели — тяжёлый удар. */
    private void cocoon(Cast c) {
        ServerPlayer p = randomNonTarget();
        if (p == null) return;
        Fx.beam((ServerLevel) level(), this, p, 0.25f, c.ticks, 0xE8E8F0);
        c.at(c.ticks, () -> {
            if (!p.isAlive() || p.isSpectator()) return;
            int dur = (int) Math.round(p("cocoon_seconds") * 20);
            long now = level().getGameTime();
            StatusService.apply(p, Status.of(StatusType.ROOT, "arachna_cocoon", getUUID(), 1, now, dur));
            StatusService.apply(p, Status.of(StatusType.SILENCE, "arachna_cocoon", getUUID(), 1, now, dur));
            poison(p, p("cocoon_poison_units"), dur / 20);
            PowerNode node = spawnNode(ModEntities.COCOON.get(), p.position(), p("cocoon_hp_units"), "Кокон");
            if (node == null) return;
            cocoons.put(node.getUUID(), p.getUUID());
            tell(Component.literal(p.getGameProfile().getName() + " в коконе — разрежьте паутину!").withStyle(ChatFormatting.WHITE));
            schedule(dur, () -> {
                if (!node.isAlive()) return;
                node.discard();
                cocoons.remove(node.getUUID());
                BossDamage.hit(this, p, p("cocoon_units"));
            });
        });
    }

    /** Кладка: яйца по арене; не разбили за {@code egg_seconds} — из каждого вылупляются прыгуны. */
    private void eggs(Cast c) {
        int n = 2 + partySize() / 2;
        c.at(c.ticks, () -> {
            for (int i = 0; i < n; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = arenaRadius() * (0.4 + random.nextDouble() * 0.45);
                Vec3 at = home().add(Math.cos(a) * r, 0, Math.sin(a) * r);
                PowerNode egg = spawnNode(ModEntities.EGG.get(), at, p("egg_hp_units"), "Кладка");
                if (egg == null) continue;
                schedule((int) Math.round(p("egg_seconds") * 20), () -> {
                    if (!egg.isAlive()) return;
                    egg.discard();
                    for (int k = 0; k < 2; k++) summon(Bestiary.JUMPER, egg.position(), MobRank.NORMAL);
                    level().playSound(null, egg.blockPosition(), SoundEvents.TURTLE_EGG_HATCH, SoundSource.HOSTILE, 1.5f, 0.6f);
                });
            }
            tell(Component.literal("Аракна откладывает яйца — разбейте их, пока не вылупились!").withStyle(ChatFormatting.GREEN));
        });
    }

    @Override
    protected void onPhase(int phase) {
        ascend();
    }

    /** Под потолок: неуязвима; ядовитый дождь под каждым; затем падение на одного из отряда. */
    private void ascend() {
        if (aloft || !(level() instanceof ServerLevel sl)) return;
        cancelCast();
        aloft = true;
        ground = position();
        setNoGravity(true);
        teleportTo(getX(), getY() + 9, getZ());
        tell(Component.literal("Аракна уходит под своды! Берегитесь ядовитого дождя.").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        level().playSound(null, blockPosition(), SoundEvents.SPIDER_STEP, SoundSource.HOSTILE, 2f, 0.4f);
        int rains = (int) Math.round(p("aloft_seconds") / 3);
        for (int i = 0; i < rains; i++) {
            schedule(20 + i * 60, () -> {
                Cast rain = testCast(30);
                for (ServerPlayer p : playersInArena()) Telegraph.circle(rain, p.position(), 2.5f, p("rain_units"));
            });
        }
        schedule(20 + rains * 60, () -> {
            ServerPlayer victim = playersInArena().isEmpty() ? null : playersInArena().get(random.nextInt(playersInArena().size()));
            Vec3 at = victim != null ? victim.position() : ground;
            Cast drop = testCast(40);
            Telegraph.circle(drop, at, 4.5f, p("drop_units"));
            tell(Component.literal("Аракна падает!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            schedule(40, () -> {
                aloft = false;
                setNoGravity(false);
                teleportTo(at.x, ground.y, at.z);
                Fx.burst(sl, at.add(0, 0.5, 0), 4.5f, 12, 0xE8E8F0);
            });
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (aloft) setDeltaMovement(Vec3.ZERO);
        if (cocoons.isEmpty() || tickCount % 5 != 0) return;
        for (var e : Map.copyOf(cocoons).entrySet()) {
            var node = ((ServerLevel) level()).getEntity(e.getKey());
            if (node != null && node.isAlive()) continue;
            cocoons.remove(e.getKey());
            ServerPlayer p = getServer().getPlayerList().getPlayer(e.getValue());
            if (p != null) {
                StatusService.removeFrom(p, StatusType.ROOT, getUUID());
                StatusService.removeFrom(p, StatusType.SILENCE, getUUID());
                StatusService.removeFrom(p, StatusType.DOT, getUUID());
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (aloft && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void resetFight() {
        cocoons.clear();
        if (aloft) {
            aloft = false;
            setNoGravity(false);
        }
        super.resetFight();
    }

    /** Паучья погоня */
    private void skitter(Cast c) {
        Mechanics.hunt(c, (int) p("skitter_seconds"), p("skitter_radius"), p("skitter_units"), 0x7FBF5C);
    }

    /** Нити паутины */
    private void threads(Cast c) {
        Mechanics.tether(c, (int) p("threads_seconds"), p("threads_dist"), p("threads_units"), 0xD6F3C0);
    }


    /** Яд: брызги кислоты и зелёные пузыри. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.burst(sl, at.add(0, 0.3, 0), radius * 0.8f, 8, 0x7CE36B);
        Fx.shards(sl, at.add(0, 0.2, 0), radius, Math.max(4, Math.round(radius * 3)), 12, 0x4F8A2B);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ITEM_SLIME, at.x, at.y + 0.4, at.z,
                Math.round(8 + radius * 4), radius * 0.4, 0.2, radius * 0.4, 0.05);
    }

    private ServerPlayer farthest() {
        ServerPlayer best = null;
        double far = 6;
        for (ServerPlayer p : playersInArena()) {
            if (p == getTarget()) continue;
            double d = distanceTo(p);
            if (d > far) {
                far = d;
                best = p;
            }
        }
        return best;
    }

    /** Прыжок охотницы: Аракна прыгает на дальнего бойца — круг приземления виден заранее; после — лужи яда. */
    private void pounce(Cast c) {
        ServerPlayer p = farthest();
        if (p == null) return;
        Vec3 at = p.position();
        tell(Component.literal("Аракна целится в " + p.getGameProfile().getName() + " — уходите из круга!").withStyle(ChatFormatting.GREEN));
        Telegraph.circle(c, at, 3.2f, p("pounce_units"), pl -> poison(pl, p("poison_units"), 4));
        c.at(c.ticks - 4, () -> {
            teleportTo(at.x, at.y, at.z);
            level().playSound(null, blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 2f, 0.6f);
        });
        c.at(c.ticks, () -> {
            for (int i = 0; i < 3; i++) {
                double a = Math.PI * 2 * i / 3 + random.nextDouble();
                addPool(at.add(Math.cos(a) * 3, 0, Math.sin(a) * 3), 1.4, 0, 1.4, 8, p("poison_units") * 2, 0x7CE36B, null);
            }
        });
    }

    /** Ядовитый веер (с 66%): три струи веером по танку и вбок — слева, по центру, справа, по очереди. */
    private void venom(Cast c) {
        triggerAnim("main", "attack");
        Vec3 dir = c.target.position().subtract(position()).multiply(1, 0, 1).normalize();
        int[] order = {-1, 1, 0};
        for (int i = 0; i < 3; i++) {
            double a = Math.toRadians(32 * order[i]);
            Vec3 d = new Vec3(dir.x * Math.cos(a) - dir.z * Math.sin(a), 0, dir.x * Math.sin(a) + dir.z * Math.cos(a));
            Telegraph.line(testCast(c.ticks + i * 10), position(), position().add(d.scale(arenaRadius() + 2)), 2.4f,
                    p("venom_units"), pl -> poison(pl, p("poison_units"), 5));
        }
        level().playSound(null, blockPosition(), SoundEvents.LLAMA_SPIT, SoundSource.HOSTILE, 2f, 0.5f);
    }

    /** Ядовитые лужи: под тремя бойцами разливается яд и стоит {venompools_seconds} — арена становится теснее. */
    private void venomPools(Cast c) {
        List<ServerPlayer> all = new java.util.ArrayList<>(playersInArena());
        java.util.Collections.shuffle(all, new java.util.Random(random.nextLong()));
        for (int i = 0; i < Math.min(3, all.size()); i++) {
            com.andrey.litrpg.boss.Ground.circle(c, all.get(i).position(), 2.5f, (int) p("venompools_seconds"), p("venompools_units"), 0x7CE36B,
                    pl -> poison(pl, p("poison_units"), 3));
        }
    }

    /** Сеть на полу (с 66%): половина арены затянута паутиной на {net_seconds} — там вязнут и травятся. */
    private void net(Cast c) {
        triggerAnim("main", "attack");
        com.andrey.litrpg.boss.Ground.half(c, home(), dir(random.nextDouble() * 360), (int) p("net_seconds"), p("net_units"), 0xD8D8D8, pl -> slow(pl, 0.5, 25));
        tell(Component.literal("Половину логова затянуло паутиной — уходите на чистую сторону.").withStyle(ChatFormatting.GREEN));
    }
}
