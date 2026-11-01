package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Сервер → клиент: подземелье пройдено — показать титры (отряд, время, вайпы). */
public record CreditsPayload(List<String> party, long seconds, int wipes) implements CustomPacketPayload {
    public static final Type<CreditsPayload> TYPE = new Type<>(LitRPG.id("credits"));
    public static final StreamCodec<FriendlyByteBuf, CreditsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), CreditsPayload::party,
            ByteBufCodecs.VAR_LONG, CreditsPayload::seconds,
            ByteBufCodecs.VAR_INT, CreditsPayload::wipes,
            CreditsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreditsPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.andrey.litrpg.client.ClientHooks.credits(p));
    }
}
