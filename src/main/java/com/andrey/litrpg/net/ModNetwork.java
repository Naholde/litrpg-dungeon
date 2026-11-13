package com.andrey.litrpg.net;

import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.fx.FxPayload;
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
        r.playToClient(StatusSyncPayload.TYPE, StatusSyncPayload.STREAM_CODEC, StatusSyncPayload::handle);
        r.playToClient(FxPayload.TYPE, FxPayload.STREAM_CODEC, FxPayload::handle);
        r.playToClient(SkillSyncPayload.TYPE, SkillSyncPayload.STREAM_CODEC, SkillSyncPayload::handle);
        r.playToServer(SkillActionPayload.TYPE, SkillActionPayload.STREAM_CODEC, SkillActionPayload::handle);
        r.playToClient(BossSyncPayload.TYPE, BossSyncPayload.STREAM_CODEC, BossSyncPayload::handle);
        r.playToClient(ShopPayload.TYPE, ShopPayload.STREAM_CODEC, ShopPayload::handle);
        r.playToServer(ShopPayload.Act.TYPE, ShopPayload.Act.STREAM_CODEC, ShopPayload.Act::handle);
        r.playToClient(RunSyncPayload.TYPE, RunSyncPayload.STREAM_CODEC, RunSyncPayload::handle);
        r.playToClient(CreditsPayload.TYPE, CreditsPayload.STREAM_CODEC, CreditsPayload::handle);
        r.playToClient(PlayerAnimPayload.TYPE, PlayerAnimPayload.STREAM_CODEC, PlayerAnimPayload::handle);
        r.playToClient(PlayerLooksPayload.TYPE, PlayerLooksPayload.STREAM_CODEC, PlayerLooksPayload::handle);
        r.playToClient(LobbySyncPayload.TYPE, LobbySyncPayload.STREAM_CODEC, LobbySyncPayload::handle);
        r.playToServer(LobbyActionPayload.TYPE, LobbyActionPayload.STREAM_CODEC, LobbyActionPayload::handle);
    }

    /** При входе игрока и после {@code /reload} — разослать баланс. */
    public static void syncBalance(OnDatapackSyncEvent event) {
        var payload = new BalanceSyncPayload(Balance.snapshot());
        event.getRelevantPlayers().forEach(p -> Net.send(p, payload));
    }
}
