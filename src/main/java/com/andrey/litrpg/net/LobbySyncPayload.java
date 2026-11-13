package com.andrey.litrpg.net;

import com.andrey.litrpg.LitRPG;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервер → клиент (обновление 15): состояние сбора отряда в лобби — кто в лобби и готов ли, выбранная сложность,
 * отсчёт старта; если забег идёт — отряд, этап, время, бой, голоса «покинуть». {@code open} — открыть окно сбора.
 */
public record LobbySyncPayload(boolean open, String difficulty, int minPlayers, int maxPlayers, int countdownTicks,
                               List<Member> players, Run run, List<String> records) implements CustomPacketPayload {

    /** Игрок в лобби (или участник идущего забега). */
    public record Member(UUID id, String name, boolean ready, boolean inLobby) {
    }

    /** Идущий забег: этап, отряд, время, хранитель в бою, голоса за выход. */
    public record Run(boolean active, int stage, String stageName, String difficulty, long seconds, int wipes, String fighting,
                      List<UUID> party, int votes, int votesNeed) {
        public static final Run NONE = new Run(false, 0, "", "", 0, 0, "", List.of(), 0, 0);
    }

    public static final LobbySyncPayload EMPTY = new LobbySyncPayload(false, "normal", 3, 6, -1, List.of(), Run.NONE, List.of());

    public static final Type<LobbySyncPayload> TYPE = new Type<>(LitRPG.id("lobby_sync"));
    public static final StreamCodec<FriendlyByteBuf, LobbySyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.open);
                buf.writeUtf(p.difficulty);
                buf.writeVarInt(p.minPlayers);
                buf.writeVarInt(p.maxPlayers);
                buf.writeVarInt(p.countdownTicks);
                buf.writeVarInt(p.players.size());
                for (Member m : p.players) {
                    UUIDUtil.STREAM_CODEC.encode(buf, m.id);
                    buf.writeUtf(m.name);
                    buf.writeBoolean(m.ready);
                    buf.writeBoolean(m.inLobby);
                }
                Run r = p.run;
                buf.writeBoolean(r.active);
                buf.writeVarInt(r.stage);
                buf.writeUtf(r.stageName);
                buf.writeUtf(r.difficulty);
                buf.writeVarLong(r.seconds);
                buf.writeVarInt(r.wipes);
                buf.writeUtf(r.fighting);
                buf.writeVarInt(r.party.size());
                for (UUID id : r.party) UUIDUtil.STREAM_CODEC.encode(buf, id);
                buf.writeVarInt(r.votes);
                buf.writeVarInt(r.votesNeed);
                buf.writeVarInt(p.records.size());
                for (String s : p.records) buf.writeUtf(s);
            },
            buf -> {
                boolean open = buf.readBoolean();
                String diff = buf.readUtf();
                int min = buf.readVarInt(), max = buf.readVarInt(), cd = buf.readVarInt();
                int n = buf.readVarInt();
                List<Member> players = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    players.add(new Member(UUIDUtil.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readBoolean(), buf.readBoolean()));
                }
                boolean active = buf.readBoolean();
                int stage = buf.readVarInt();
                String stageName = buf.readUtf(), rdiff = buf.readUtf();
                long secs = buf.readVarLong();
                int wipes = buf.readVarInt();
                String fighting = buf.readUtf();
                int pn = buf.readVarInt();
                List<UUID> party = new ArrayList<>(pn);
                for (int i = 0; i < pn; i++) party.add(UUIDUtil.STREAM_CODEC.decode(buf));
                int votes = buf.readVarInt(), need = buf.readVarInt();
                int rn = buf.readVarInt();
                List<String> records = new ArrayList<>(rn);
                for (int i = 0; i < rn; i++) records.add(buf.readUtf());
                return new LobbySyncPayload(open, diff, min, max, cd, players,
                        new Run(active, stage, stageName, rdiff, secs, wipes, fighting, party, votes, need), records);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LobbySyncPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> com.andrey.litrpg.client.ClientLobby.set(p));
    }
}
