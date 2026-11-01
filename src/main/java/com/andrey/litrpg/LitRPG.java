package com.andrey.litrpg;

import com.andrey.litrpg.core.LitrpgCommand;
import com.andrey.litrpg.combat.CombatEvents;
import com.andrey.litrpg.core.ModAttachments;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.core.ModItems;
import com.andrey.litrpg.core.balance.Balance;
import com.andrey.litrpg.core.balance.BalanceLoader;
import com.andrey.litrpg.dev.DevFiles;
import com.andrey.litrpg.net.ModNetwork;
import com.andrey.litrpg.player.PlayerEvents;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;

/** Точка входа мода: подключает модули к шинам событий. Логика модулей — в их пакетах. */
@Mod(LitRPG.MOD_ID)
public final class LitRPG {
    public static final String MOD_ID = "litrpg";
    public static final Logger LOG = LogUtils.getLogger();

    public LitRPG(IEventBus modBus, ModContainer container) {
        Balance.bootstrap();
        ModAttachments.register(modBus);
        ModItems.register(modBus);
        ModEntities.register(modBus);
        modBus.addListener(ModNetwork::register);

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener((AddReloadListenerEvent e) -> e.addListener(new BalanceLoader()));
        game.addListener((OnDatapackSyncEvent e) -> ModNetwork.syncBalance(e));
        game.addListener((RegisterCommandsEvent e) -> LitrpgCommand.register(e.getDispatcher()));
        PlayerEvents.register(modBus, game);
        CombatEvents.register(game);
        DevFiles.register(game);

        LOG.info("[LitRPG] Мод загружен, версия {}", container.getModInfo().getVersion());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
