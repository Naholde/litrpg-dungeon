package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.fx.ClientPopups;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер → клиент: всплывающая цифра над сущностью (урон, крит, лечение, уклонение). */
public record PopupPayload(int entityId, float amount, Kind kind) implements CustomPacketPayload {
    /** BLOCK — щит врага принял удар, MISS — удар ушёл мимо (враг под «ударами мимо»), WARD — несокрушимость спасла. */
    public enum Kind { DEALT, CRIT, TAKEN, HEAL, DODGE, ABSORB, BLOCK, MISS, WARD }

    public static final Type<PopupPayload> TYPE = new Type<>(LitRPG.id("popup"));
    public static final StreamCodec<FriendlyByteBuf, PopupPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId);
                buf.writeFloat(p.amount);
                buf.writeEnum(p.kind);
            },
            buf -> new PopupPayload(buf.readVarInt(), buf.readFloat(), buf.readEnum(Kind.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PopupPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPopups.add(p));
    }
}
