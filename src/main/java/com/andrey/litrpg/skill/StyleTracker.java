package com.andrey.litrpg.skill;

import com.andrey.litrpg.player.PlayerData;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.server.level.ServerPlayer;

/** Копит стиль игры, пока у игрока нет класса (дальше — незачем). */
public final class StyleTracker {
    private StyleTracker() {
    }

    public static void add(ServerPlayer p, Style s, int n) {
        PlayerData d = PlayerService.data(p);
        if (!d.classId().isEmpty() || !d.role().chosen()) return;
        PlayerService.addStyle(p, s, n);
    }
}
