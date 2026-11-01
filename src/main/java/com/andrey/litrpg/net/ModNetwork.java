package com.andrey.litrpg.net;

import com.andrey.litrpg.core.balance.Balance;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация пакетов. Список и версия — в {@link Protocol}. */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(Integer.toString(Protocol.VERSION));
        r.playToClient(BalanceSyncPayload.TYPE, BalanceSyncPayload.STREAM_CODEC, BalanceSyncPayload::handle);
        r.playToClient(PlayerSyncPayload.TYPE, PlayerSyncPayload.STREAM_CODEC, PlayerSyncPayload::handle);
        r.playToServer(ChooseRolePayload.TYPE, ChooseRolePayload.STREAM_CODEC, ChooseRolePayload::handle);
        r.playToServer(AllocateStatsPayload.TYPE, AllocateStatsPayload.STREAM_CODEC, AllocateStatsPayload::handle);
        r.playToClient(PopupPayload.TYPE, PopupPayload.STREAM_CODEC, PopupPayload::handle);
        r.playToClient(MobTargetPayload.TYPE, MobTargetPayload.STREAM_CODEC, MobTargetPayload::handle);
        r.playToClient(PartySyncPayload.TYPE, PartySyncPayload.STREAM_CODEC, PartySyncPayload::handle);
        r.playToServer(SelectTargetPayload.TYPE, SelectTargetPayload.STREAM_CODEC, SelectTargetPayload::handle);
        r.playToServer(StaffCastPayload.TYPE, StaffCastPayload.STREAM_CODEC, StaffCastPayload::handle);
    }

    /** При входе игрока и после {@code /reload} — разослать баланс. */
    public static void syncBalance(OnDatapackSyncEvent event) {
        var payload = new BalanceSyncPayload(Balance.snapshot());
        event.getRelevantPlayers().forEach(p -> Net.send(p, payload));
    }
}
