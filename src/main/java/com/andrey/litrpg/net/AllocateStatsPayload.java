package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.player.PlayerService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: вложить очки (по числу на характеристику). Сервер проверяет, хватает ли очков. */
public record AllocateStatsPayload(int[] add) implements CustomPacketPayload {
    public static final Type<AllocateStatsPayload> TYPE = new Type<>(LitRPG.id("allocate_stats"));
    public static final StreamCodec<FriendlyByteBuf, AllocateStatsPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeVarIntArray(p.add), buf -> new AllocateStatsPayload(buf.readVarIntArray(16)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AllocateStatsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) PlayerService.allocate(sp, p.add);
        });
    }
}
