package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.item.StaffItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: игрок нажал ЛКМ с посохом в руке. Сервер сам проверяет посох и откат. */
public record StaffCastPayload() implements CustomPacketPayload {
    public static final StaffCastPayload INSTANCE = new StaffCastPayload();
    public static final Type<StaffCastPayload> TYPE = new Type<>(LitRPG.id("staff_cast"));
    public static final StreamCodec<ByteBuf, StaffCastPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StaffCastPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.isAlive() && !sp.isSpectator()
                    && sp.getMainHandItem().getItem() instanceof StaffItem staff) {
                staff.cast(sp, sp.getMainHandItem());
            }
        });
    }
}
