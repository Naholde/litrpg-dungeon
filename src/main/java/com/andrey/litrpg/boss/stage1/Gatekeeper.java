package com.andrey.litrpg.boss.stage1;

import com.andrey.litrpg.boss.BossEntity;
import com.andrey.litrpg.boss.BossRegistry;
import com.andrey.litrpg.boss.Telegraph;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Привратник Склепа — первый хранитель, учит основам: удар цепью конусом по танку (остальным — не стоять перед ним),
 * «Костяной вал» кольцом (вплотную безопасно), «Зов костей» — крысы из ниш (бить по площади),
 * «Костяной щит» — сбить оглушением, иначе хранитель закроется щитом. С половины здоровья цепь бьёт дважды.
 */
public class Gatekeeper extends BossEntity {
    public Gatekeeper(EntityType<? extends Gatekeeper> type, Level level) {
        super(type, level);
    }

    @Override
    public BossRegistry.Type type() {
        return BossRegistry.GATEKEEPER;
    }

    @Override
    public String modelId() {
        return "gatekeeper";
    }

    @Override
    protected double[] phaseThresholds() {
        return new double[]{0.5};
    }

    private final List<Ability> abilities = List.of(
            new Ability("Костяной щит", "shield", 0, true, b -> b.getHealth() < b.getMaxHealth() * 0.9, this::boneShield),
            new Ability("Зов костей", "call", 1, false, b -> spawnedAlive().size() < 6, this::callBones),
            new Ability("Костяной вал", "rampart", 2, false, b -> true, this::rampart),
            new Ability("Удар цепью", "chain", 3, false, b -> b.getTarget() != null, this::chain));

    @Override
    protected List<Ability> abilities() {
        return abilities;
    }

    private void chain(Cast c) {
        Vec3 dir = c.target.position().subtract(position());
        Telegraph.cone(c, position(), dir, 8, 80, p("chain_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
        level().playSound(null, blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5f, 0.6f);
        if (phase() >= 1) {
            // Ярость: второй взмах через секунду — по другому игроку.
            ServerPlayer other = randomNonTarget();
            if (other == null) return;
            Vec3 dir2 = other.position().subtract(position());
            schedule(c.ticks, () -> {
                Cast second = testCast(28);
                Telegraph.cone(second, position(), dir2, 8, 60, p("chain_units") * 0.8);
            });
        }
    }

    private void rampart(Cast c) {
        Telegraph.donut(c, position(), 3.5f, (float) arenaRadius() + 2, p("rampart_units"));
        schedule(c.ticks - 8, () -> triggerAnim("main", "slam"));
        tell(Component.literal("Вал костей! Держитесь вплотную к Привратнику.").withStyle(ChatFormatting.GOLD));
    }

    private void callBones(Cast c) {
        triggerAnim("main", "cast");
        int n = 2 + partySize();
        c.at(c.ticks, () -> {
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + random.nextDouble() * 0.4;
                Vec3 at = home().add(Math.cos(a) * (arenaRadius() - 2), 0.2, Math.sin(a) * (arenaRadius() - 2));
                summon(Bestiary.BONE_RAT, at, MobRank.NORMAL);
            }
            level().playSound(null, blockPosition(), SoundEvents.SKELETON_AMBIENT, SoundSource.HOSTILE, 2f, 0.5f);
        });
    }

    private void boneShield(Cast c) {
        Fx.pillar((ServerLevel) level(), position(), 1.4f, 5, c.ticks, 0xE8DCC0);
        c.at(c.ticks, () -> {
            double amount = getMaxHealth() * p("shield_share");
            StatusService.apply(this, Status.of(StatusType.SHIELD, "gatekeeper_shield", getUUID(), amount,
                    level().getGameTime(), 20 * 20));
            tell(Component.literal("Привратник оброс костяным щитом — пробейте его!").withStyle(ChatFormatting.RED));
            level().playSound(null, blockPosition(), SoundEvents.BONE_BLOCK_PLACE, SoundSource.HOSTILE, 2f, 0.5f);
        });
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 1) {
            tell(Component.literal("Привратник рвёт цепи: теперь цепь бьёт дважды!").withStyle(ChatFormatting.RED));
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
    }
}
