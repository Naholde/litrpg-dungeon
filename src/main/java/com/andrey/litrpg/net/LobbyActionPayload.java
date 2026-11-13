package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер (обновление 15): действие в окне сбора отряда. Сервер сам проверяет, можно ли. */
public record LobbyActionPayload(Action action, String arg) implements CustomPacketPayload {

    public enum Action {
        /** Открыть окно (прислать состояние сразу). */
        OPEN,
        /** Готов / не готов. */
        READY,
        /** Выбрать сложность (arg — id). */
        DIFFICULTY,
        /** Голос «покинуть забег» (участник). */
        ABANDON,
        /** Наблюдать за идущим забегом (не участник). */
        SPECTATE,
        /** Вернуться из наблюдения в лобби. */
        RETURN
    }

    public static final Type<LobbyActionPayload> TYPE = new Type<>(LitRPG.id("lobby_action"));
    public static final StreamCodec<FriendlyByteBuf, LobbyActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.action);
                buf.writeUtf(p.arg, 64);
            },
            buf -> new LobbyActionPayload(buf.readEnum(Action.class), buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LobbyActionPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) com.andrey.litrpg.run.LobbyService.act(sp, p.action, p.arg);
        });
    }
}
