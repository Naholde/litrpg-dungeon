package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.player.Role;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиент → сервер: игрок выбрал роль в окне роли. Сервер сам проверяет, можно ли. */
public record ChooseRolePayload(Role role) implements CustomPacketPayload {
    public static final Type<ChooseRolePayload> TYPE = new Type<>(LitRPG.id("choose_role"));
    public static final StreamCodec<FriendlyByteBuf, ChooseRolePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeEnum(p.role), buf -> new ChooseRolePayload(buf.readEnum(Role.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ChooseRolePayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) PlayerService.chooseRole(sp, p.role);
        });
    }
}
