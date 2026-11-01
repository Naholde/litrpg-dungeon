package com.andrey.litrpg.client.anim;

import com.andrey.litrpg.LitRPG;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;

/** Подключение движений игрока к отрисовке. */
@EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
public final class AnimHooks {
    private AnimHooks() {
    }

    @SubscribeEvent
    public static void before(RenderPlayerEvent.Pre e) {
        ClientAnims.pushRoot(e.getEntity(), e.getPoseStack(), e.getPartialTick());
    }

    @SubscribeEvent
    public static void after(RenderPlayerEvent.Post e) {
        ClientAnims.popRoot(e.getPoseStack());
    }

    /** Пока игрок крутится, ник над головой не показываем — он крутился бы вместе с ним. */
    @SubscribeEvent
    public static void nameTag(RenderNameTagEvent e) {
        if (ClientAnims.spinning(e.getEntity())) e.setCanRender(TriState.FALSE);
    }

    @SubscribeEvent
    public static void hand(RenderHandEvent e) {
        ClientAnims.firstPerson(e.getHand(), e.getPoseStack());
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post e) {
        ClientAnims.tick();
        com.andrey.litrpg.client.fx.FxRenderer.tick();
    }

    @SubscribeEvent
    public static void leave(ClientPlayerNetworkEvent.LoggingOut e) {
        ClientAnims.clear();
    }
}
