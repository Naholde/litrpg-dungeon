package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.HealService;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Optional;
import java.util.UUID;

/** Клиент → сервер: выбранная цель лечения (Ctrl+1…6 по рамкам группы); пусто — снять выбор. */
public record SelectTargetPayload(Optional<UUID> target) implements CustomPacketPayload {
    public static final Type<SelectTargetPayload> TYPE = new Type<>(LitRPG.id("select_target"));
    public static final StreamCodec<FriendlyByteBuf, SelectTargetPayload> STREAM_CODEC =
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC).<FriendlyByteBuf>cast()
                    .map(SelectTargetPayload::new, SelectTargetPayload::target);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectTargetPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) HealService.select(sp, p.target.orElse(null));
        });
    }
}
