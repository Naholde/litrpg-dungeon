package com.andrey.litrpg.player;

import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.PlayerLooksPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Облик класса виден всем: сервер рассылает, у кого какой класс. Шлёт только изменения —
 * {@link PlayerService#sync} зовётся часто (опыт, очки), а облик меняется лишь со сменой класса.
 */
public final class LookService {
    private static final Map<UUID, String> SENT = new HashMap<>();

    private LookService() {
    }

    /** После синхронизации персонажа: класс сменился — сообщить всем. */
    public static void changed(ServerPlayer player) {
        String cls = PlayerService.data(player).classId;
        if (cls.equals(SENT.get(player.getUUID()))) return;
        SENT.put(player.getUUID(), cls);
        broadcast(player, new PlayerLooksPayload(false, List.of(new PlayerLooksPayload.Entry(player.getUUID(), cls))));
    }

    /** Вошедшему — вся таблица (кто уже в игре), остальным — его класс. */
    public static void login(ServerPlayer player) {
        SENT.remove(player.getUUID());
        changed(player);
        List<PlayerLooksPayload.Entry> all = new ArrayList<>();
        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
            all.add(new PlayerLooksPayload.Entry(p.getUUID(), PlayerService.data(p).classId));
        }
        Net.send(player, new PlayerLooksPayload(true, all));
    }

    public static void logout(ServerPlayer player) {
        SENT.remove(player.getUUID());
    }

    private static void broadcast(ServerPlayer from, PlayerLooksPayload payload) {
        if (from.server == null) return;
        for (ServerPlayer p : from.server.getPlayerList().getPlayers()) Net.send(p, payload);
    }
}
