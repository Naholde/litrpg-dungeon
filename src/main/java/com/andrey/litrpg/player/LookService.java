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
 * Облик класса (или роли до выбора класса) виден всем: сервер рассылает, у кого какой облик. Шлёт только изменения —
 * {@link PlayerService#sync} зовётся часто (опыт, очки), а облик меняется лишь со сменой класса.
 */
public final class LookService {
    private static final Map<UUID, String> SENT = new HashMap<>();

    private LookService() {
    }

    /**
     * Какой облик носит игрок: облик класса, а до выбора класса (ур. 1–9) — облик роли {@code role_<роль>};
     * без роли — никакого. Один расчёт для сервера и клиента.
     */
    public static String lookId(String classId, Role role) {
        if (classId != null && !classId.isEmpty()) return classId;
        return role != null && role.chosen() ? "role_" + role.id : "";
    }

    static String lookOf(ServerPlayer p) {
        PlayerData d = PlayerService.data(p);
        return lookId(d.classId, d.role());
    }

    /** После синхронизации персонажа: облик сменился (класс или роль) — сообщить всем. */
    public static void changed(ServerPlayer player) {
        String cls = lookOf(player);
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
            all.add(new PlayerLooksPayload.Entry(p.getUUID(), lookOf(p)));
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
