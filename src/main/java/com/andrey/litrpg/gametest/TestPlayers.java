package com.andrey.litrpg.gametest;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.UUID;

/**
 * Тестовый игрок. Как {@code makeMockServerPlayerInLevel}, но с настроенным соединением: иначе моды, которые при входе
 * шлют свои пакеты (Curios), роняют вход тестового игрока.
 */
public final class TestPlayers {
    private TestPlayers() {
    }

    public static ServerPlayer create(GameTestHelper h) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-player"), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        // Соединение «как у клиента со всеми модами»: пакеты Curios и наши уходят в никуда, а не роняют вход.
        NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }
}
