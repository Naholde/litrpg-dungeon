package com.andrey.litrpg.client.look;

import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.net.PlayerLooksPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Класс каждого игрока на клиенте — по нему рисуется облик ({@link LookLayer}). */
public final class ClientLooks {
    private static final Map<UUID, String> CLASSES = new HashMap<>();

    private ClientLooks() {
    }

    public static void update(boolean full, List<PlayerLooksPayload.Entry> entries) {
        if (full) CLASSES.clear();
        for (var e : entries) {
            if (e.classId().isEmpty()) CLASSES.remove(e.player());
            else CLASSES.put(e.player(), e.classId());
        }
    }

    /** Класс игрока или пустая строка. Свой — из данных персонажа (они приходят раньше таблицы). */
    public static String classOf(Player p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && p.getUUID().equals(mc.player.getUUID()) && ClientPlayerData.received()) {
            return ClientPlayerData.classId();
        }
        return CLASSES.getOrDefault(p.getUUID(), "");
    }

    /** Есть ли у сущности облик (тогда ванильная броня не рисуется — облик её заменяет). */
    public static boolean hasLook(Entity e) {
        return e instanceof Player p && LookModels.get(classOf(p)) != null;
    }

    public static void reset() {
        CLASSES.clear();
    }
}
