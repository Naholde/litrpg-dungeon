package com.andrey.litrpg.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/** Голос «Системы»: сообщения в чат с приставкой [Система] и крупные надписи на экране. */
public final class SystemChat {
    private SystemChat() {
    }

    public static MutableComponent prefix() {
        return Component.literal("[Система] ").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
    }

    public static void send(ServerPlayer player, Component text) {
        // Текст — соседний узел, а не потомок приставки: иначе унаследует её жирный голубой стиль.
        player.sendSystemMessage(Component.empty().append(prefix()).append(text));
    }

    public static void send(ServerPlayer player, String text) {
        send(player, Component.literal(text));
    }

    /** Крупная надпись по центру экрана. */
    public static void title(ServerPlayer player, Component title, Component subtitle) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 15));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }
}
