package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.screen.ShopScreen;
import com.andrey.litrpg.shop.ShopService;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Сервер → клиент: лавка (товар, кошелёк, цены услуг; −1 — услуга недоступна). Клиент → сервер — {@link Act}.
 */
public record ShopPayload(int traderId, int stage, long wallet, List<Entry> entries, long reroll, long upgrade,
                          long classChange, long rankReset) implements CustomPacketPayload {
    public record Entry(ItemStack stack, long price, boolean bought) {
    }

    public enum Action { BUY, REROLL, UPGRADE, CLASS_CHANGE, RANK_RESET }

    public static final Type<ShopPayload> TYPE = new Type<>(LitRPG.id("shop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShopPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.traderId);
                buf.writeVarInt(p.stage);
                buf.writeVarLong(p.wallet);
                buf.writeVarInt(p.entries.size());
                for (Entry e : p.entries) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.stack);
                    buf.writeVarLong(e.price);
                    buf.writeBoolean(e.bought);
                }
                buf.writeLong(p.reroll);
                buf.writeLong(p.upgrade);
                buf.writeLong(p.classChange);
                buf.writeLong(p.rankReset);
            },
            buf -> {
                int id = buf.readVarInt(), stage = buf.readVarInt();
                long wallet = buf.readVarLong();
                int n = buf.readVarInt();
                List<Entry> list = new java.util.ArrayList<>();
                for (int i = 0; i < n; i++) {
                    list.add(new Entry(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf), buf.readVarLong(), buf.readBoolean()));
                }
                return new ShopPayload(id, stage, wallet, list, buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong());
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShopPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ShopScreen.open(p));
    }

    /** Клиент → сервер: действие в лавке. */
    public record Act(int traderId, Action action, int index) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(LitRPG.id("shop_act"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, a) -> {
                    buf.writeVarInt(a.traderId);
                    buf.writeEnum(a.action);
                    buf.writeVarInt(a.index);
                },
                buf -> new Act(buf.readVarInt(), buf.readEnum(Action.class), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Act a, IPayloadContext ctx) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer sp) ShopService.act(sp, a.traderId, a.action, a.index);
            });
        }
    }
}
