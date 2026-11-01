package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientBoss;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Сервер → игрокам на арене: рамка хранителя. {@code cast} — «Имя|можно_сбить|длина|тик_конца» или пусто.
 * {@code name} пустое — хранителя больше нет (рамку убрать).
 */
public record BossSyncPayload(int entityId, String name, String music, float health, float maxHealth, boolean shielded, int enrage,
                              String cast, long serverTime, List<ThreatRow> threat) implements CustomPacketPayload {
    public record ThreatRow(String name, float share) {
    }

    public static BossSyncPayload gone(int entityId) {
        return new BossSyncPayload(entityId, "", "", 0, 0, false, 0, "", 0, List.of());
    }

    public static final Type<BossSyncPayload> TYPE = new Type<>(LitRPG.id("boss_sync"));
    public static final StreamCodec<FriendlyByteBuf, BossSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entityId);
                buf.writeUtf(p.name, 64);
                buf.writeUtf(p.music, 32);
                buf.writeFloat(p.health);
                buf.writeFloat(p.maxHealth);
                buf.writeBoolean(p.shielded);
                buf.writeVarInt(p.enrage);
                buf.writeUtf(p.cast, 128);
                buf.writeVarLong(p.serverTime);
                buf.writeCollection(p.threat, (b, r) -> {
                    b.writeUtf(r.name, 32);
                    b.writeFloat(r.share);
                });
            },
            buf -> new BossSyncPayload(buf.readVarInt(), buf.readUtf(64), buf.readUtf(32), buf.readFloat(), buf.readFloat(), buf.readBoolean(),
                    buf.readVarInt(), buf.readUtf(128), buf.readVarLong(),
                    buf.readList(b -> new ThreatRow(b.readUtf(32), b.readFloat()))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BossSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientBoss.update(p));
    }
}
