package com.andrey.litrpg.client;

import com.andrey.litrpg.client.ui.UiKit;
import com.andrey.litrpg.net.LobbySyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Состояние сбора отряда на клиенте (обновление 15): что прислал сервер. Пока игрок в лобби и забег не идёт — в
 * правом верхнем углу строка «Сбор: 2/3 готовы · P — окно сбора»; во время отсчёта — крупный счёт.
 */
public final class ClientLobby {
    private static LobbySyncPayload data = LobbySyncPayload.EMPTY;

    private ClientLobby() {
    }

    public static void set(LobbySyncPayload p) {
        data = p;
        Minecraft mc = Minecraft.getInstance();
        if (p.open() && mc.player != null) {
            if (!(mc.screen instanceof com.andrey.litrpg.client.screen.LobbyScreen)) {
                mc.setScreen(new com.andrey.litrpg.client.screen.LobbyScreen());
            }
        }
    }

    public static LobbySyncPayload data() {
        return data;
    }

    public static void reset() {
        data = LobbySyncPayload.EMPTY;
    }

    /** Я в лобби (есть в списке сбора, а забег не идёт). */
    public static boolean inLobby() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && !data.run().active()
                && data.players().stream().anyMatch(m -> m.id().equals(mc.player.getUUID()));
    }

    public static boolean meReady() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && data.players().stream().anyMatch(m -> m.id().equals(mc.player.getUUID()) && m.ready());
    }

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null || !inLobby()) return;
        Font f = mc.font;
        int w = g.guiWidth();
        long ready = data.players().stream().filter(LobbySyncPayload.Member::ready).count();
        int here = data.players().size();
        String line1 = "Сбор отряда: " + ready + "/" + here + " готовы";
        String line2;
        if (data.countdownTicks() >= 0) line2 = "Старт через " + (data.countdownTicks() + 19) / 20 + " с";
        else if (here < data.minPlayers()) line2 = "Ждём ещё " + (data.minPlayers() - here) + " (нужно " + data.minPlayers() + ")";
        else line2 = meReady() ? "Ждём остальных…" : "Окно сбора — клавиша P";
        int tw = Math.max(f.width(line1), f.width(line2)) + 12;
        int x = w - tw - 4, y = 4;
        UiKit.panel(g, x, y, tw, 26);
        UiKit.text(g, f, line1, x + 6, y + 4, UiKit.GOLD);
        UiKit.text(g, f, line2, x + 6, y + 15, data.countdownTicks() >= 0 ? UiKit.GOOD : UiKit.MUTED);
        if (data.countdownTicks() >= 0) {
            String n = String.valueOf((data.countdownTicks() + 19) / 20);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(g.guiWidth() / 2f, g.guiHeight() / 2f - 40, 0);
            pose.scale(4f, 4f, 1);
            g.drawString(f, n, -f.width(n) / 2, 0, UiKit.GOLD, true);
            pose.popPose();
        }
    }
}
