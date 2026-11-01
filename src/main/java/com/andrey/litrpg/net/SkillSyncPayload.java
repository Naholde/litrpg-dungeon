package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientSkills;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/** Сервер → клиент: состояние панели — перезарядки, общая перезарядка, рывок, ресурс, чтение. */
public record SkillSyncPayload(List<Slot> slots, int gcdLeft, int gcdTotal, int dashLeft, int dashTotal, float resource,
                               String castSkill, int castTotal, int castLeft) implements CustomPacketPayload {
    public record Slot(String skill, int left, int total) {
    }

    public static final Type<SkillSyncPayload> TYPE = new Type<>(LitRPG.id("skill_sync"));
    public static final StreamCodec<FriendlyByteBuf, SkillSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeCollection(p.slots, (b, s) -> {
                    b.writeUtf(s.skill, 64);
                    b.writeVarInt(s.left);
                    b.writeVarInt(s.total);
                });
                buf.writeVarInt(p.gcdLeft);
                buf.writeVarInt(p.gcdTotal);
                buf.writeVarInt(p.dashLeft);
                buf.writeVarInt(p.dashTotal);
                buf.writeFloat(p.resource);
                buf.writeUtf(p.castSkill, 64);
                buf.writeVarInt(p.castTotal);
                buf.writeVarInt(p.castLeft);
            },
            buf -> new SkillSyncPayload(buf.readList(b -> new Slot(b.readUtf(64), b.readVarInt(), b.readVarInt())),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(),
                    buf.readUtf(64), buf.readVarInt(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillSyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientSkills.update(p));
    }
}
