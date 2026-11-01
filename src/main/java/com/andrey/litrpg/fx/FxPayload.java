package com.andrey.litrpg.fx;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.fx.FxRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Сервер → клиент: одна фигура эффекта. Смысл полей зависит от {@link Shape}:
 * <ul>
 *   <li>{@code a} — центр/начало, {@code b} — конец (полоса, луч) или направление (конус, взмах);</li>
 *   <li>{@code r1}, {@code r2} — радиусы (кольцо: от → до; конус: радиус, угол в градусах);</li>
 *   <li>{@code width} — толщина (кольцо, полоса, луч); {@code height} — высота (столб);</li>
 *   <li>{@code fill} — заполняется ли фигура к концу срока (разметка удара: «когда заполнится — бьёт»);</li>
 *   <li>{@code entityA}/{@code entityB} — привязка к сущностям (луч между ними, знак над головой), иначе −1;</li>
 *   <li>{@code text} — символ знака.</li>
 * </ul>
 */
public record FxPayload(Shape shape, Vec3 a, Vec3 b, float r1, float r2, float width, float height, int duration,
                        int color, boolean fill, int entityA, int entityB, String text) implements CustomPacketPayload {

    public enum Shape {
        /** Расходящаяся волна-кольцо по земле. */
        RING,
        /** Круг на земле: разметка «здесь ударит» (с заполнением) или зона. */
        DISC,
        /** Кольцо на земле с безопасным центром (разметка «уйди к хранителю»). */
        DONUT,
        /** Сектор на земле. b — направление. */
        CONE,
        /** Полоса на земле от a до b шириной width. */
        LINE,
        /** Светящийся луч между a и b (или сущностями). */
        BEAM,
        /** Столб света. */
        PILLAR,
        /** Вспышка-шар. */
        BURST,
        /** Взмах-полумесяц перед сущностью. */
        SLASH,
        /** Знак над головой сущности. */
        MARK
    }

    public static final Type<FxPayload> TYPE = new Type<>(LitRPG.id("fx"));
    public static final StreamCodec<FriendlyByteBuf, FxPayload> STREAM_CODEC = StreamCodec.of(FxPayload::write, FxPayload::read);

    private static void write(FriendlyByteBuf buf, FxPayload p) {
        buf.writeEnum(p.shape);
        buf.writeVec3(p.a);
        buf.writeVec3(p.b);
        buf.writeFloat(p.r1);
        buf.writeFloat(p.r2);
        buf.writeFloat(p.width);
        buf.writeFloat(p.height);
        buf.writeVarInt(p.duration);
        buf.writeInt(p.color);
        buf.writeBoolean(p.fill);
        buf.writeVarInt(p.entityA + 1);
        buf.writeVarInt(p.entityB + 1);
        buf.writeUtf(p.text, 8);
    }

    private static FxPayload read(FriendlyByteBuf buf) {
        return new FxPayload(buf.readEnum(Shape.class), buf.readVec3(), buf.readVec3(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readInt(), buf.readBoolean(),
                buf.readVarInt() - 1, buf.readVarInt() - 1, buf.readUtf(8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FxPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> FxRenderer.add(p));
    }
}
