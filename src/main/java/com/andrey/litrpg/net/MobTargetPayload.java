package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.fx.ClientAggro;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер → клиент: кого сейчас бьёт враг ({@code targetId = -1} — никого). Для показа «на ком агро». */
public record MobTargetPayload(int mobId, int targetId) implements CustomPacketPayload {
    public static final Type<MobTargetPayload> TYPE = new Type<>(LitRPG.id("mob_target"));
    public static final StreamCodec<FriendlyByteBuf, MobTargetPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.mobId);
                buf.writeVarInt(p.targetId);
            },
            buf -> new MobTargetPayload(buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MobTargetPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAggro.set(p.mobId, p.targetId));
    }
}
