package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientStatuses;
import com.andrey.litrpg.status.StatusType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Сервер → клиент: свои метки (для полосы эффектов над здоровьем). */
public record StatusSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(StatusType type, String icon, int remainingTicks, float magnitude) {
    }

    public static final Type<StatusSyncPayload> TYPE = new Type<>(LitRPG.id("status_sync"));
    public static final StreamCodec<FriendlyByteBuf, StatusSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeCollection(p.entries, (b, e) -> {
                b.writeEnum(e.type);
                b.writeUtf(e.icon, 64);
                b.writeVarInt(e.remainingTicks);
                b.writeFloat(e.magnitude);
            }),
            buf -> new StatusSyncPayload(buf.readList(b -> new Entry(b.readEnum(StatusType.class), b.readUtf(64),
                    b.readVarInt(), b.readFloat()))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StatusSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientStatuses.update(p.entries));
    }
}
