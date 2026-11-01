package com.andrey.litrpg.party;

import com.andrey.litrpg.core.SystemChat;
import com.andrey.litrpg.net.Net;
import com.andrey.litrpg.net.PartySyncPayload;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Отряды на сервере. Пока отряд собирается командой; в фазе прохождения его соберёт печать у врат
 * и сохранит вместе с состоянием прохождения.
 */
public final class PartyService {
    private static final Map<UUID, Party> BY_PLAYER = new HashMap<>();

    private PartyService() {
    }

    public static Optional<Party> of(UUID player) {
        return Optional.ofNullable(BY_PLAYER.get(player));
    }

    public static boolean sameParty(ServerPlayer a, ServerPlayer b) {
        Party p = BY_PLAYER.get(a.getUUID());
        return p != null && p.contains(b.getUUID());
    }

    /** Собрать отряд из игроков (каждый выходит из прежнего). Больше шести — лишние не входят. */
    public static Party form(List<ServerPlayer> players) {
        Party party = new Party();
        for (ServerPlayer p : players) {
            if (party.members.size() >= Party.MAX) break;
            leaveQuietly(p.getUUID());
            party.members.add(p.getUUID());
            BY_PLAYER.put(p.getUUID(), party);
        }
        for (ServerPlayer p : players) {
            if (party.contains(p.getUUID())) SystemChat.send(p, "Отряд собран: " + party.members.size() + " чел.");
        }
        return party;
    }

    /** Отряд прохождения после перезапуска сервера (по сохранённым UUID, в сети или нет). */
    public static void restore(List<UUID> members) {
        Party party = BY_PLAYER.get(members.isEmpty() ? null : members.get(0));
        if (party != null && party.members.containsAll(members)) return;
        party = new Party();
        for (UUID id : members) {
            leaveQuietly(id);
            party.members.add(id);
            BY_PLAYER.put(id, party);
        }
    }

    public static void leave(ServerPlayer p) {
        if (leaveQuietly(p.getUUID())) {
            SystemChat.send(p, "Вы покинули отряд.");
            Net.send(p, new PartySyncPayload(List.of()));
        }
    }

    private static boolean leaveQuietly(UUID id) {
        Party old = BY_PLAYER.remove(id);
        if (old == null) return false;
        old.members.remove(id);
        return true;
    }

    public static void clear() {
        BY_PLAYER.clear();
    }

    /** Онлайн-участники отряда игрока (включая его самого). */
    public static List<ServerPlayer> online(MinecraftServer server, ServerPlayer p) {
        List<ServerPlayer> out = new ArrayList<>();
        of(p.getUUID()).ifPresent(party -> {
            for (UUID id : party.members) {
                ServerPlayer m = server.getPlayerList().getPlayer(id);
                if (m != null) out.add(m);
            }
        });
        return out;
    }

    /** Раз в полсекунды: каждому участнику — состояние отряда для рамок группы. */
    public static void sync(MinecraftServer server) {
        java.util.Set<Party> parties = new java.util.HashSet<>(BY_PLAYER.values());
        for (Party party : parties) {
            List<PartySyncPayload.Member> list = new ArrayList<>();
            for (UUID id : party.members) {
                ServerPlayer m = server.getPlayerList().getPlayer(id);
                if (m == null) {
                    list.add(new PartySyncPayload.Member(id, "…", com.andrey.litrpg.player.Role.NONE, 0, 0, 1, 0, false, -1));
                    continue;
                }
                var d = PlayerService.data(m);
                list.add(new PartySyncPayload.Member(id, m.getGameProfile().getName(), d.role(), d.level(),
                        m.getHealth(), m.getMaxHealth(), m.getAbsorptionAmount(), true, m.getId()));
            }
            var payload = new PartySyncPayload(list);
            for (UUID id : party.members) {
                ServerPlayer m = server.getPlayerList().getPlayer(id);
                if (m != null) Net.send(m, payload);
            }
        }
    }
}
