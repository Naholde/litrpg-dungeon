package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientRun;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Сервер → клиент: этап, попытка, цель этапа, «вы — дух», духи отряда (где и сколько осталось до воскрешения). */
public record RunSyncPayload(int stage, String stageName, int attempt, String goal, boolean spirit, List<Spirit> spirits)
        implements CustomPacketPayload {
    public static final RunSyncPayload EMPTY = new RunSyncPayload(0, "", 0, "", false, List.of());

    public record Spirit(UUID id, String name, double x, double y, double z, float progress) {
    }

    public static final Type<RunSyncPayload> TYPE = new Type<>(LitRPG.id("run_sync"));
    public static final StreamCodec<FriendlyByteBuf, RunSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.stage);
                buf.writeUtf(p.stageName);
                buf.writeVarInt(p.attempt);
                buf.writeUtf(p.goal);
                buf.writeBoolean(p.spirit);
                buf.writeVarInt(p.spirits.size());
                for (Spirit s : p.spirits) {
                    UUIDUtil.STREAM_CODEC.encode(buf, s.id);
                    buf.writeUtf(s.name);
                    buf.writeDouble(s.x);
                    buf.writeDouble(s.y);
                    buf.writeDouble(s.z);
                    buf.writeFloat(s.progress);
                }
            },
            buf -> {
                int stage = buf.readVarInt();
                String name = buf.readUtf();
                int attempt = buf.readVarInt();
                String goal = buf.readUtf();
                boolean spirit = buf.readBoolean();
                int n = buf.readVarInt();
                List<Spirit> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(new Spirit(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readDouble(), buf.readDouble(),
                            buf.readDouble(), buf.readFloat()));
                }
                return new RunSyncPayload(stage, name, attempt, goal, spirit, list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RunSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRun.set(p));
    }
}
