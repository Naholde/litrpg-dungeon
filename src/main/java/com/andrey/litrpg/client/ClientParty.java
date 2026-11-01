package com.andrey.litrpg.client;

import com.andrey.litrpg.net.PartySyncPayload;
import com.andrey.litrpg.net.SelectTargetPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Отряд на клиенте и выбранная цель лечения (Ctrl+1…5). */
public final class ClientParty {
    private static List<PartySyncPayload.Member> members = List.of();
    private static UUID selected;

    private ClientParty() {
    }

    public static void update(List<PartySyncPayload.Member> list) {
        members = List.copyOf(list);
        if (selected != null && members.stream().noneMatch(m -> m.id().equals(selected))) select(null);
    }

    public static List<PartySyncPayload.Member> members() {
        return members;
    }

    public static UUID selected() {
        return selected;
    }

    /** Ctrl+N: выбрать N-го участника; повторное нажатие снимает выбор. */
    public static void toggle(int index) {
        if (index >= members.size()) return;
        UUID id = members.get(index).id();
        select(id.equals(selected) ? null : id);
    }

    private static void select(UUID id) {
        selected = id;
        PacketDistributor.sendToServer(new SelectTargetPayload(Optional.ofNullable(id)));
    }

    public static void reset() {
        members = List.of();
        selected = null;
    }
}
