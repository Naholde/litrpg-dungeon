package com.andrey.litrpg.combat.threat;

import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.CombatBalance;
import com.andrey.litrpg.net.MobTargetPayload;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.player.Derived;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Угроза на живых врагах. Цель врага выбирает таблица угрозы, а не ванильный ИИ:
 * попытки ИИ сменить цель перехватываются в {@link #onChangeTarget}.
 */
public final class ThreatService {
    /** Мы сами ставим цель — событие смены цели не трогаем. */
    private static boolean applying;

    private ThreatService() {
    }

    public static ThreatTable table(Mob mob) {
        return mob.getData(ModAttachments.THREAT);
    }

    /** Угроза от урона или лечения: сырое число × множитель угрозы игрока (танк ×5). */
    public static void add(Mob mob, ServerPlayer p, double raw) {
        if (raw <= 0 || p.isSpectator()) return;
        table(mob).add(p.getUUID(), raw * PlayerService.breakdown(p).get(Derived.THREAT));
    }

    /** Провокация: враг сразу бьёт провокатора и не отвлекается несколько секунд. */
    public static void taunt(Mob mob, ServerPlayer p) {
        CombatBalance.Threat t = Balance.get(Balance.COMBAT).threat();
        taunt(mob, p, (int) Math.round(t.tauntSeconds() * 20));
    }

    /** Провокация на {@code ticks} тиков (навык задаёт свою длительность). */
    public static void taunt(Mob mob, ServerPlayer p, int ticks) {
        long now = mob.level().getGameTime();
        table(mob).taunt(p.getUUID(), now, now + ticks);
        retarget(mob, p);
        broadcast(mob);
    }

    /** Приманка: несколько секунд враг бьёт объект (двойник зерцала, живое древо, двойник иллюзиониста). */
    private record Decoy(LivingEntity who, long until) {
    }

    private static final java.util.Map<Mob, Decoy> DECOYS = new java.util.WeakHashMap<>();

    /** Перевести врага на приманку: пока она цела и время не вышло, таблица угрозы молчит. */
    public static void decoy(Mob mob, LivingEntity decoy, int ticks) {
        DECOYS.put(mob, new Decoy(decoy, mob.level().getGameTime() + ticks));
        retarget(mob, decoy);
        broadcast(mob);
    }

    /** Действующая приманка этого врага или null. */
    public static LivingEntity decoyOf(Mob mob) {
        Decoy d = DECOYS.get(mob);
        if (d == null) return null;
        if (d.who() == null || !d.who().isAlive() || mob.level().getGameTime() >= d.until()) {
            DECOYS.remove(mob);
            return null;
        }
        return d.who();
    }

    /** Раз в полсекунды: забыть ушедших, растопить угрозу, выбрать цель. */
    public static void tick(Mob mob) {
        LivingEntity decoy = decoyOf(mob);
        if (decoy != null) {
            if (mob.getTarget() != decoy) retarget(mob, decoy);
            return;
        }
        if (!mob.hasData(ModAttachments.THREAT)) return;
        ThreatTable t = table(mob);
        if (t.isEmpty() || !(mob.level() instanceof ServerLevel level)) return;
        CombatBalance.Threat cfg = Balance.get(Balance.COMBAT).threat();
        List<UUID> gone = new ArrayList<>();
        for (UUID id : t.view().keySet()) {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            if (p == null || !p.isAlive() || p.isSpectator() || p.level() != level
                    || p.distanceTo(mob) > cfg.forgetRange()) gone.add(id);
        }
        gone.forEach(t::remove);
        t.decay(cfg.decayPerHalfSecond());
        UUID before = t.current();
        UUID chosen = t.choose(level.getGameTime(), id -> {
            ServerPlayer p = level.getServer().getPlayerList().getPlayer(id);
            return p != null && p.distanceTo(mob) <= cfg.meleeRange();
        }, cfg.switchMelee(), cfg.switchRanged());
        ServerPlayer target = chosen == null ? null : level.getServer().getPlayerList().getPlayer(chosen);
        if (target != null && mob.getTarget() != target) retarget(mob, target);
        if (chosen == null && mob.getTarget() instanceof ServerPlayer) retarget(mob, null);
        if (!java.util.Objects.equals(before, chosen) || mob.tickCount % 40 == 0) broadcast(mob);
    }

    private static void retarget(Mob mob, LivingEntity target) {
        applying = true;
        try {
            mob.setTarget(target);
        } finally {
            applying = false;
        }
    }

    /** ИИ хочет сменить цель — если у врага есть таблица угрозы, решает она. */
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        if (applying || !(e.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        LivingEntity decoy = decoyOf(mob);
        if (decoy != null) {
            if (e.getNewAboutToBeSetTarget() != decoy) e.setNewAboutToBeSetTarget(decoy);
            return;
        }
        // Таблицу заводим только тем, кто воюет с игроками — у мирных существ её нет вовсе.
        if (!mob.hasData(ModAttachments.THREAT) && !(e.getNewAboutToBeSetTarget() instanceof ServerPlayer)) return;
        ThreatTable t = table(mob);
        if (t.isEmpty()) {
            // Враг заметил игрока сам (без удара) — с этого момента цель ведёт таблица.
            if (e.getNewAboutToBeSetTarget() instanceof ServerPlayer p && !p.isSpectator()) t.add(p.getUUID(), 1);
            return;
        }
        UUID current = t.current();
        if (current == null) return;
        ServerPlayer chosen = mob.getServer().getPlayerList().getPlayer(current);
        if (chosen != null && e.getNewAboutToBeSetTarget() != chosen) e.setNewAboutToBeSetTarget(chosen);
    }

    /** Враги рядом забывают игрока (невидимость, дым, ослепление): угроза снята, цель сброшена. */
    public static void forget(ServerPlayer p, double radius) {
        if (!(p.level() instanceof ServerLevel level)) return;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(radius))) {
            if (!mob.hasData(ModAttachments.THREAT)) continue;
            table(mob).remove(p.getUUID());
            if (mob.getTarget() == p) {
                retarget(mob, null);
                broadcast(mob);
            }
        }
    }

    /** Всем рядом — кого бьёт этот враг (для значка над головой и рамки цели). */
    public static void broadcast(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        int targetId = mob.getTarget() != null ? mob.getTarget().getId() : -1;
        var payload = new MobTargetPayload(mob.getId(), targetId);
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(mob) < 64 * 64) Net.send(p, payload);
        }
    }

    /** Строки таблицы для команды проверки: ник → угроза, по убыванию. */
    public static List<Map.Entry<UUID, Double>> sorted(Mob mob) {
        List<Map.Entry<UUID, Double>> l = new ArrayList<>(table(mob).view().entrySet());
        l.sort(Map.Entry.<UUID, Double>comparingByValue().reversed());
        return l;
    }
}
