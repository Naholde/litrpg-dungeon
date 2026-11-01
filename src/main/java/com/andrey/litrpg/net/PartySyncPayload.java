package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientParty;
import com.andrey.litrpg.player.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;
import java.util.UUID;

/** Сервер → участникам отряда: состояние всех для рамок группы. Пустой список — отряда нет. */
public record PartySyncPayload(List<Member> members) implements CustomPacketPayload {
    public record Member(UUID id, String name, Role role, int level, float health, float maxHealth, float absorb,
                         boolean online, int entityId) {
    }

    public static final Type<PartySyncPayload> TYPE = new Type<>(LitRPG.id("party_sync"));
    public static final StreamCodec<FriendlyByteBuf, PartySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeCollection(p.members, (b, m) -> {
                b.writeUUID(m.id);
                b.writeUtf(m.name, 32);
                b.writeEnum(m.role);
                b.writeVarInt(m.level);
                b.writeFloat(m.health);
                b.writeFloat(m.maxHealth);
                b.writeFloat(m.absorb);
                b.writeBoolean(m.online);
                b.writeVarInt(m.entityId);
            }),
            buf -> new PartySyncPayload(buf.readList(b -> new Member(b.readUUID(), b.readUtf(32), b.readEnum(Role.class),
                    b.readVarInt(), b.readFloat(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readVarInt()))));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PartySyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientParty.update(p.members));
    }
}
