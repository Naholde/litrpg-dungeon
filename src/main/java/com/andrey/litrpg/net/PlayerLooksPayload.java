package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.look.ClientLooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/**
 * Сервер → клиент: класс игроков, чтобы рисовать облик класса на каждом (не только на себе).
 * {@code full} — полная таблица при входе (старое забыть), иначе — изменения; пустой класс — облика нет.
 */
public record PlayerLooksPayload(boolean full, List<Entry> entries) implements CustomPacketPayload {
    public record Entry(UUID player, String classId) {
    }

    public static final Type<PlayerLooksPayload> TYPE = new Type<>(LitRPG.id("player_looks"));
    public static final StreamCodec<FriendlyByteBuf, PlayerLooksPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.full);
                buf.writeCollection(p.entries, (b, e) -> {
                    b.writeUUID(e.player);
                    b.writeUtf(e.classId, 64);
                });
            },
            buf -> new PlayerLooksPayload(buf.readBoolean(),
                    buf.readList(b -> new Entry(b.readUUID(), b.readUtf(64)))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLooksPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientLooks.update(p.full, p.entries));
    }
}
