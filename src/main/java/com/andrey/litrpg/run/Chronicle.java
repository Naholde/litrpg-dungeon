package com.andrey.litrpg.run;

import com.andrey.litrpg.core.SystemChat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Летопись (обновление 15): переносит итог забега в вечные {@link Records} — личные суммы, рекорды «за забег»,
 * лучшее время забега и боёв с хранителями, трофеи — и выдаёт титулы. Вызывается, когда забег закончился (пройден)
 * или брошен (сброс, голосование «покинуть»).
 */
public final class Chronicle {
    private Chronicle() {
    }

    /** Отряд победил хранителя: трофей (первая победа на сервере), счёт побед у каждого. */
    static void bossDefeated(MinecraftServer server, RunState s, String bossKind, String bossName) {
        Records r = Records.get(server);
        List<String> names = new ArrayList<>(s.names.values());
        if (r.trophy(bossKind, names, s.difficulty)) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                SystemChat.send(p, Component.literal("Первая победа над хранителем «" + bossName
                        + "» на этом сервере! Его чучело встанет в Зале трофеев.").withStyle(ChatFormatting.GOLD));
            }
        }
        for (UUID id : s.party) {
            Records.Profile prof = r.profile(id, s.names.get(id));
            prof.bosses++;
            prof.bossKinds.add(bossKind);
            if (prof.bossKinds.size() >= Titles.ALL_BOSSES) Titles.grant(server, id, prof.name, Titles.SLAYER);
        }
        r.changed();
    }

    /**
     * Итог забега. {@code completed} — подземелье пройдено до конца (только тогда — время забега и титулы прохождения).
     */
    static void commit(MinecraftServer server, RunState s, boolean completed) {
        if (s.party.isEmpty()) return;
        Records r = Records.get(server);
        Difficulty d = s.difficulty;
        long ticks = server.overworld().getGameTime() - s.runStart;
        List<String> names = new ArrayList<>(s.names.values());

        for (UUID id : s.party) {
            String name = s.names.getOrDefault(id, "?");
            Records.Profile p = r.profile(id, name);
            RunStats.Line l = s.stats.getOrDefault(id, new RunStats.Line());
            p.runs++;
            p.dealt += l.dealt;
            p.healed += l.healed;
            p.taken += l.taken;
            p.deaths += l.deaths;
            p.revives += l.revives;
            p.kills += l.kills;
            p.caches += l.caches;
            if (completed) {
                if (d == Difficulty.HEROIC) p.completedHeroic++;
                else p.completedNormal++;
            }
            if (r.offer(d, Records.Single.DEALT, l.dealt, name)) Titles.grant(server, id, name, Titles.DESTROYER);
            if (r.offer(d, Records.Single.HEALED, l.healed, name)) Titles.grant(server, id, name, Titles.LIGHTBEARER);
            if (r.offer(d, Records.Single.TAKEN, l.taken, name)) Titles.grant(server, id, name, Titles.BULWARK);
            r.offer(d, Records.Single.REVIVES, l.revives, name);
            r.offer(d, Records.Single.BIGGEST, l.biggestHit, name);
            r.offer(d, Records.Single.KILLS, l.kills, name);
            if (p.revives >= Titles.ANGEL_REVIVES) Titles.grant(server, id, name, Titles.ANGEL);
            if (p.caches >= Titles.TREASURE_CACHES) Titles.grant(server, id, name, Titles.TREASURE);
            if (completed) {
                Titles.grant(server, id, name, Titles.PATHFINDER);
                if (d == Difficulty.HEROIC) Titles.grant(server, id, name, Titles.HERO);
                if (s.wipes == 0) Titles.grant(server, id, name, Titles.FLAWLESS);
                if (l.deaths == 0) Titles.grant(server, id, name, Titles.IMMORTAL);
            }
        }
        for (Map.Entry<String, Long> e : s.bossTimes.entrySet()) r.offerBoss(d, e.getKey(), e.getValue(), names, s.wipes);
        Records.RunRecord run = new Records.RunRecord(ticks, names, s.wipes, Records.today(), d.id, completed, s.stage);
        r.addHistory(run);
        if (completed && r.offerRun(d, run)) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                SystemChat.send(p, Component.literal("Новый рекорд! Самый быстрый забег (" + d.title + "): "
                        + time(ticks) + " — " + String.join(", ", names)).withStyle(ChatFormatting.GOLD));
            }
            for (UUID id : s.party) Titles.grant(server, id, s.names.getOrDefault(id, "?"), Titles.SWIFT);
        }
        r.changed();
    }

    /** «1 ч 05 мин 12 с» / «12 мин 03 с» / «45,3 с». */
    public static String time(long ticks) {
        long secs = ticks / 20;
        if (secs >= 3600) return secs / 3600 + " ч " + String.format("%02d", secs / 60 % 60) + " мин " + String.format("%02d", secs % 60) + " с";
        if (secs >= 60) return secs / 60 + " мин " + String.format("%02d", secs % 60) + " с";
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0).replace('.', ',') + " с";
    }
}
