package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientPlayerData;
import com.andrey.litrpg.player.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Сервер → клиент: данные своего персонажа. Характеристики клиент считает сам тем же {@code StatBreakdown}. */
public record PlayerSyncPayload(Role role, int level, long xp, int[] allocated) implements CustomPacketPayload {
    public static final Type<PlayerSyncPayload> TYPE = new Type<>(LitRPG.id("player_sync"));
    public static final StreamCodec<FriendlyByteBuf, PlayerSyncPayload> STREAM_CODEC =
            StreamCodec.ofMember(PlayerSyncPayload::write, PlayerSyncPayload::read);

    private void write(FriendlyByteBuf buf) {
        buf.writeEnum(role);
        buf.writeVarInt(level);
        buf.writeVarLong(xp);
        buf.writeVarIntArray(allocated);
    }

    private static PlayerSyncPayload read(FriendlyByteBuf buf) {
        return new PlayerSyncPayload(buf.readEnum(Role.class), buf.readVarInt(), buf.readVarLong(), buf.readVarIntArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPlayerData.update(p));
    }
}
