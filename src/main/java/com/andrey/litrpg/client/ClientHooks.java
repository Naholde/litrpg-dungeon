package com.andrey.litrpg.client;

import com.andrey.litrpg.client.screen.CreditsScreen;
import com.andrey.litrpg.net.CreditsPayload;
import net.minecraft.client.Minecraft;

/** Действия по пакетам сервера, которым нужны клиентские классы (окна). */
public final class ClientHooks {
    private ClientHooks() {
    }

    public static void credits(CreditsPayload p) {
        Minecraft.getInstance().setScreen(new CreditsScreen(p));
    }
}
