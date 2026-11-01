package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
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
            new Ability("Костяные копья", "spears", 1, false, b -> true, this::spears),
            new Ability("Жатва", "reap", 2, false, b -> b.getTarget() != null, this::reap));

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
}
