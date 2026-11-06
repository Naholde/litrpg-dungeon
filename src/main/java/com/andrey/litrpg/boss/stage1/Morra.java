package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.gear.LootService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import com.andrey.litrpg.boss.Mechanics;

/**
 * Морра, старшая сестра: коса по танку и костяные копья полосами к каждому. Вступая в бой, зовёт Вессу.
 * Её смерть (а значит, смерть обеих) — победа над Сёстрами.
 */
public class Morra extends TwinSister {
    public Morra(EntityType<? extends Morra> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.MORRA;
    }

    @Override
    public String modelId() {
        return "morra";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[0];
    }

    @Override
    protected void onPhase(int phase) {
    }

    @Override
    protected double reviveSeconds() {
        return p("revive_seconds");
    }

    @Override
    protected double linkCut() {
        return p("link_cut");
    }

    private final List<Ability> abilities = List.of(
            new Ability("Поле жатвы", "harvest", 1, false, b -> true, this::harvestField),
            new Ability("След косы", "scythetrail", 1, false, b -> true, this::scytheTrail),
            new Ability("Сестринская черта", "bondline", 1, false, b -> sister() != null && !sister().fallen(), this::bondLine),
            new Ability("Сестринская нить", "bond", 0, false, b -> true, this::bond),
            new Ability("Волны косы", "waves", 1, false, b -> true, this::waves),
            new Ability("Костяные копья", "spears", 1, false, b -> true, this::spears),
            new Ability("Жатва", "reap", 2, false, b -> b.getTarget() != null, this::reap),
            new Ability("Рой душ", "souls", 1, false, b -> sister() != null && !sister().fallen(), this::souls));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    @Override
    protected void onEngage(List<ServerPlayer> players) {
        if (!(level() instanceof ServerLevel sl) || sister() != null) return;
        Vessa v = ModEntities.VESSA.get().create(sl);
        if (v == null) return;
        Vec3 at = home().add(4, 0, 0);
        v.moveTo(at.x, at.y, at.z, 0, 0);
        v.setArena(home(), arenaRadius());
        v.setRun(null, partySize());
        v.getPersistentData().putBoolean(LootService.NO_LOOT, true);
        v.setSister(this);
        setSister(v);
        sl.addFreshEntity(v);
        track(v);
        tell(Component.literal("Морра: «Сестра, у нас гости». Между сёстрами — связь: пока цела, урон по ним вдвое меньше. "
                + "Кто встанет в луч — разорвёт её, но поплатится.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void reap(Cast c) {
        Telegraph.cone(c, position(), c.target.position().subtract(position()), 7, 100, p("reap_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "attack"));
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
    }

    private void spears(Cast c) {
        triggerAnim("main", "cast");
        for (ServerPlayer p : playersInArena()) {
            Vec3 dir = p.position().subtract(position()).multiply(1, 0, 1).normalize();
            Telegraph.line(c, position(), position().add(dir.scale(arenaRadius() + 4)), 2.2f, p("spears_units"));
        }
        level().playSound(null, blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 1.5f, 0.4f);
    }

    /** Сестринская нить */
    private void bond(Cast c) {
        Mechanics.tether(c, (int) p("bond_seconds"), p("bond_dist"), p("bond_units"), 0xD8343A);
    }

    /** Волны косы */
    private void waves(Cast c) {
        Mechanics.rings(c, (int) p("waves_waves"), p("waves_units"), p("waves_gap"), 0xB0001A);
    }


    /**
     * Рой душ: с края арены к Вессе плывут души. Дошла — Весса лечится; бойцы перехватывают души, встав
     * на пути (лопаются о бойца, слегка обжигают).
     */
    private void souls(Cast c) {
        TwinSister target = sister();
        if (target == null) return;
        triggerAnim("main", "cast");
        tell(Component.literal("Морра зовёт души к сестре — перехватите их, встав на пути!").withStyle(ChatFormatting.LIGHT_PURPLE));
        ServerLevel sl = (ServerLevel) level();
        int n = 3 + partySize() / 2;
        Vec3[] pos = new Vec3[n];
        boolean[] gone = new boolean[n];
        double base = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < n; i++) {
            double a = base + Math.PI * 2 * i / n;
            pos[i] = home().add(Math.cos(a) * (arenaRadius() - 1), 0, Math.sin(a) * (arenaRadius() - 1));
            Fx.orb(sl, pos[i].add(0, 1, 0), 0.5f, c.ticks, 0xB98CFF);
        }
        com.andrey.litrpg.boss.Telegraph.note(c, "intercept", 0);
        double step = p("souls_speed") / 10;
        for (int t = 0; t < 90; t++) {
            c.at(c.ticks + t * 2, () -> {
                if (!target.isAlive() || target.fallen()) return;
                for (int i = 0; i < n; i++) {
                    if (gone[i]) continue;
                    Vec3 to = target.position().subtract(pos[i]).multiply(1, 0, 1);
                    if (to.length() < 1.5) {
                        gone[i] = true;
                        target.heal((float) (target.getMaxHealth() * p("souls_share")));
                        Fx.beam(sl, pos[i].add(0, 1, 0), target.position().add(0, 1.5, 0), 0.2f, 10, 0xB98CFF);
                        continue;
                    }
                    pos[i] = pos[i].add(to.normalize().scale(step));
                    Fx.orb(sl, pos[i].add(0, 1, 0), 0.45f, 3, 0xB98CFF);
                    for (ServerPlayer p : playersInArena()) {
                        if (p.position().distanceTo(pos[i]) > 1.3) continue;
                        gone[i] = true;
                        com.andrey.litrpg.boss.BossDamage.hit(this, p, p("souls_units"));
                        Fx.burst(sl, pos[i].add(0, 1, 0), 0.8f, 8, 0xB98CFF);
                        sl.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2f, 1.2f);
                        break;
                    }
                }
            });
        }
    }

    /** Поле жатвы: горят две противоположные четверти арены, потом — две другие. Переходите вместе с полем. */
    private void harvestField(Cast c) {
        triggerAnim("main", "cast");
        double a0 = random.nextDouble() * 90;
        for (int k = 0; k < 2; k++) {
            Cast part = testCast(c.ticks + k * 100);
            for (int q = 0; q < 2; q++) {
                com.andrey.litrpg.boss.Ground.sector(part, home(), dir(a0 + k * 90 + q * 180), (float) arenaRadius() + 3, 90, 5, p("harvest_units"), 0xB98CFF, null);
            }
        }
        tell(Component.literal("Поле жатвы! Две четверти горят — потом две другие.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** След косы: восемь секунд за Моррой остаётся горящая полоса — танк, водите её по краю, а не через отряд. */
    private void scytheTrail(Cast c) {
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Ground.trail(c, this, 8, 10, 1.7f, 7, p("scythetrail_units"), 0xB0001A, null);
        tell(Component.literal("За Моррой тянется след косы — водите её по краю.").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /**
     * Сестринская черта: между сёстрами {bondline_seconds} секунд горит черта — кто стоит между ними, тот горит.
     * Разведите сестёр, чтобы черта не резала арену пополам.
     */
    private void bondLine(Cast c) {
        TwinSister s = sister();
        if (s == null) return;
        triggerAnim("main", "cast");
        com.andrey.litrpg.boss.Telegraph.note(c, "zone", 0);
        Fx.beam((ServerLevel) level(), this, s, 0.3f, c.ticks, 0xB98CFF);
        c.at(c.ticks, () -> addZone((int) (p("bondline_seconds") * 20), p("bondline_units"), pt -> {
            if (!s.isAlive()) return false;
            Vec3 a = position(), b = s.position();
            Vec3 ab = b.subtract(a).multiply(1, 0, 1), ap = pt.subtract(a).multiply(1, 0, 1);
            double len = ab.length();
            if (len < 1) return false;
            double along = ap.dot(ab) / len;
            return along >= 0 && along <= len && ap.subtract(ab.scale(along / len)).length() <= 1.4;
        }, sl -> {
            if (s.isAlive()) Fx.line(sl, position(), s.position(), 2.8f, 12, 0xB98CFF, false);
        }, null));
        tell(Component.literal("Между сёстрами горит черта — не стойте между ними!").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
