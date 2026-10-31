package com.andrey.litrpg.net;

import com.andrey.litrpg.core.balance.Balance;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Регистрация пакетов. Список и версия — в {@link Protocol}. */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(Integer.toString(Protocol.VERSION));
        r.playToClient(BalanceSyncPayload.TYPE, BalanceSyncPayload.STREAM_CODEC, BalanceSyncPayload::handle);
    }

    /** При входе игрока и после {@code /reload} — разослать баланс. */
    public static void syncBalance(OnDatapackSyncEvent event) {
        var payload = new BalanceSyncPayload(Balance.snapshot());
        event.getRelevantPlayers().forEach(p -> PacketDistributor.sendToPlayer(p, payload));
    }
}
