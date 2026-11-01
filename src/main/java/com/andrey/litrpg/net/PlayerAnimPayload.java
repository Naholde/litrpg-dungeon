package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.anim.ClientAnims;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер → клиент: игрок {@code entity} начал движение {@code anim} (имя из {@code Anims}).
 * {@code speed} растягивает движение (2 — вдвое дольше, для долгого чтения), пустое имя — прекратить движение.
 */
public record PlayerAnimPayload(int entity, String anim, float speed) implements CustomPacketPayload {
    public static final Type<PlayerAnimPayload> TYPE = new Type<>(LitRPG.id("player_anim"));
    public static final StreamCodec<FriendlyByteBuf, PlayerAnimPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entity);
                buf.writeUtf(p.anim, 64);
                buf.writeFloat(p.speed);
            },
            buf -> new PlayerAnimPayload(buf.readVarInt(), buf.readUtf(64), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerAnimPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientAnims.play(p.entity, p.anim, p.speed));
    }
}
