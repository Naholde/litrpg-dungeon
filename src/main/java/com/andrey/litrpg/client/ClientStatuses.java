package com.andrey.litrpg.client;

import com.andrey.litrpg.net.StatusSyncPayload;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Свои метки на клиенте: что и до какого тика действует. */
public final class ClientStatuses {
    public record Shown(StatusSyncPayload.Entry entry, long until) {
    }

    private static List<Shown> shown = List.of();

    private ClientStatuses() {
    }

    public static void update(List<StatusSyncPayload.Entry> entries) {
        var mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0;
        shown = entries.stream().map(e -> new Shown(e, now + e.remainingTicks())).toList();
    }

    public static List<Shown> shown() {
        return shown;
    }

    public static void reset() {
        shown = List.of();
    }
}
