package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.player.PlayerService;
import com.andrey.litrpg.skill.SkillService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Клиент → сервер: действие с навыками. Сервер всё проверяет сам.
 * <ul>
 *   <li>USE — нажата ячейка {@code slot};</li>
 *   <li>DASH — рывок в сторону ({@code x}, {@code z});</li>
 *   <li>RANK_UP — поднять ранг навыка {@code text};</li>
 *   <li>CHOOSE_CLASS — выбрать предложенный класс {@code text}.</li>
 * </ul>
 */
public record SkillActionPayload(Action action, int slot, float x, float z, String text) implements CustomPacketPayload {
    public enum Action { USE, DASH, RANK_UP, CHOOSE_CLASS }

    public static SkillActionPayload use(int slot) {
        return new SkillActionPayload(Action.USE, slot, 0, 0, "");
    }

    public static SkillActionPayload dash(float x, float z) {
        return new SkillActionPayload(Action.DASH, 0, x, z, "");
    }

    public static SkillActionPayload rankUp(String skill) {
        return new SkillActionPayload(Action.RANK_UP, 0, 0, 0, skill);
    }

    public static SkillActionPayload chooseClass(String classId) {
        return new SkillActionPayload(Action.CHOOSE_CLASS, 0, 0, 0, classId);
    }

    public static final Type<SkillActionPayload> TYPE = new Type<>(LitRPG.id("skill_action"));
    public static final StreamCodec<FriendlyByteBuf, SkillActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.action);
                buf.writeVarInt(p.slot);
                buf.writeFloat(p.x);
                buf.writeFloat(p.z);
                buf.writeUtf(p.text, 64);
            },
            buf -> new SkillActionPayload(buf.readEnum(Action.class), buf.readVarInt(), buf.readFloat(), buf.readFloat(),
                    buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillActionPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            switch (p.action) {
                case USE -> SkillService.use(sp, p.slot);
                case DASH -> SkillService.dash(sp, p.x, p.z);
                case RANK_UP -> PlayerService.rankUp(sp, p.text);
                case CHOOSE_CLASS -> PlayerService.chooseClass(sp, p.text);
            }
        });
    }
}
