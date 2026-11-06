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
import com.andrey.litrpg.boss.Mechanics;

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
            new Ability("Костяной частокол", "palisade", 1, false, b -> true, this::palisade),
            new Ability("Склеп оживает", "crypt", 1, false, b -> b.phase() >= 1, this::crypt),
            new Ability("Костяной залп", "salvo", 0, false, b -> true, this::salvo),
            new Ability("Цепная хватка", "grip", 1, false, b -> farthest() != null, this::grip),
            new Ability("Цепи из стен", "walls", 1, false, b -> b.phase() >= 1, this::wallChains),
            new Ability("Рывок на цепи", "lunge", 1, false, b -> true, this::lunge),
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
            tell(Component.literal("Привратник рвёт цепи: теперь цепь бьёт дважды, а из стен бьют цепи склепа!")
                    .withStyle(ChatFormatting.RED));
            level().playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 0.6f);
        }
    }

    /** Костяной залп */
    private void salvo(Cast c) {
        Mechanics.volley(c, (int) p("salvo_shots"), 8, p("salvo_units"), 0xE8DCC0);
    }

    /** Рывок на цепи */
    private void lunge(Cast c) {
        Mechanics.charge(c, randomNonTarget() != null ? randomNonTarget()
                : (getTarget() instanceof ServerPlayer t ? t : null), p("lunge_units"), p("lunge_push"), 0xC9A055);
    }


    /** Кость и цепь: осколки костей и звенья вместо общей вспышки. */
    @Override
    public void impact(Vec3 at, float radius) {
        ServerLevel sl = (ServerLevel) level();
        Fx.shards(sl, at.add(0, 0.3, 0), radius, Math.max(5, Math.round(radius * 3)), 14, 0xE8DCC0);
        Fx.burst(sl, at.add(0, 0.4, 0), radius * 0.7f, 8, 0xC9A055);
        Fx.dust(sl, at.add(0, 0.2, 0), 0xBFB59A, Math.round(8 + radius * 3), radius * 0.6, 0.04, 1.4f);
    }

    /** Самый дальний боец (не танк) дальше 5 блоков — его цепь и достаёт. */
    private ServerPlayer farthest() {
        ServerPlayer best = null;
        double far = 5;
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

    /** Цепная хватка: цепь летит к дальнему бойцу — кого застала, того дёргает к Привратнику. */
    private void grip(Cast c) {
        ServerPlayer p = farthest();
        if (p == null) return;
        tell(Component.literal("Привратник закидывает цепь на " + p.getGameProfile().getName() + " — шаг в сторону!")
                .withStyle(ChatFormatting.GOLD));
        Mechanics.grip(c, p, 2.2f, p("grip_units"), p("grip_pull"), 0xC9A055);
    }

    /**
     * Цепи из стен (с половины): цепи склепа одна за другой бьют поперёк арены. Все линии видны сразу,
     * а срабатывают по очереди — кого задело, того цепь держит полторы секунды.
     */
    private void wallChains(Cast c) {
        tell(Component.literal("Цепи рвутся из стен! Уходите с линий — бьют по очереди.").withStyle(ChatFormatting.GOLD));
        ServerLevel sl = (ServerLevel) level();
        int n = (int) p("walls_count");
        double base = random.nextDouble() * Math.PI;
        double len = arenaRadius() + 1;
        for (int i = 0; i < n; i++) {
            double a = base + Math.PI * i / n;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            Vec3 side = new Vec3(-Math.sin(a), 0, Math.cos(a));
            double off = (random.nextDouble() * 2 - 1) * 2 + (i % 2 == 0 ? 3 : -3);
            Vec3 mid = home().add(side.scale(off));
            Vec3 from = mid.subtract(dir.scale(len)), to = mid.add(dir.scale(len));
            Cast line = testCast(c.ticks + i * 14);
            Telegraph.line(line, from, to, 2.0f, p("walls_units"), pl -> StatusService.apply(pl,
                    Status.of(StatusType.ROOT, "gatekeeper_walls", getUUID(), 1, level().getGameTime(), 30)));
            line.at(line.ticks, () -> {
                Fx.bolt(sl, from.add(0, 1.1, 0), to.add(0, 1.1, 0), 0.26f, 10, 0xC9A055);
                level().playSound(null, net.minecraft.core.BlockPos.containing(from), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.6f, 0.5f);
            });
        }
    }

    /** Костяной частокол: три гряды костей расходятся от Привратника и стоят {palisade_seconds} — арена делится на клинья. */
    private void palisade(Cast c) {
        triggerAnim("main", "slam");
        double a0 = random.nextDouble() * 360;
        for (int i = 0; i < 3; i++) {
            Vec3 d = dir(a0 + i * 120);
            com.andrey.litrpg.boss.Ground.line(c, position(), position().add(d.scale(arenaRadius() + 2)), 2.2f, (int) p("palisade_seconds"),
                    p("palisade_units"), 0xE8DCC0, pl -> slow(pl, 0.3, 25));
        }
        tell(Component.literal("Костяной частокол делит арену — не стойте на грядах.").withStyle(ChatFormatting.GOLD));
    }

    /** Склеп оживает (с половины): из пола встают четыре круга костяной трухи и не уходят {crypt_seconds}. */
    private void crypt(Cast c) {
        triggerAnim("main", "cast");
        for (int i = 0; i < 4; i++) {
            com.andrey.litrpg.boss.Ground.circle(c, randomSpot(0.25, 0.85), 2.6f, (int) p("crypt_seconds"), p("crypt_units"), 0xBFB59A, null);
        }
    }
}
