package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.core.balance.Balance;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Сервер → клиент: все разделы баланса в json (подсказки навыков на клиенте строятся из тех же цифр). */
public record BalanceSyncPayload(Map<String, String> sections) implements CustomPacketPayload {
    public static final Type<BalanceSyncPayload> TYPE = new Type<>(LitRPG.id("balance_sync"));

    public static final StreamCodec<ByteBuf, BalanceSyncPayload> STREAM_CODEC = ByteBufCodecs
            .map(HashMap<String, String>::new, ByteBufCodecs.stringUtf8(64), ByteBufCodecs.stringUtf8(1 << 20))
            .map(BalanceSyncPayload::new, p -> new HashMap<>(p.sections()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BalanceSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Map<String, JsonElement> files = new HashMap<>();
            payload.sections().forEach((name, json) -> files.put(name, JsonParser.parseString(json)));
            Balance.apply(files);
            LitRPG.LOG.info("[LitRPG] Клиент получил баланс: {} разделов", files.size());
        });
    }
}
