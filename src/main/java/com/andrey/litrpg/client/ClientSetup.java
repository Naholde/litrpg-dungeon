package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.screen.CharacterScreen;
import com.andrey.litrpg.client.screen.RoleScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.andrey.litrpg.client.fx.ClientAggro;
import com.andrey.litrpg.client.fx.ClientPopups;
import com.andrey.litrpg.client.render.MagicBoltRenderer;
import com.andrey.litrpg.client.render.TrainingDummyRenderer;
import com.andrey.litrpg.combat.item.StaffItem;
import com.andrey.litrpg.core.ModEntities;
import com.andrey.litrpg.net.StaffCastPayload;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/** Подключение клиентской части: клавиши, HUD, открытие окон. */
public final class ClientSetup {
    private ClientSetup() {
    }

    @EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
    public static final class ModBus {
        @SubscribeEvent
        public static void keys(RegisterKeyMappingsEvent e) {
            e.register(KeyBinds.CHARACTER);
            for (var k : KeyBinds.PARTY_TARGET) e.register(k);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(), MagicBoltRenderer::new);
            e.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), TrainingDummyRenderer::new);
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent e) {
            e.registerAbove(VanillaGuiLayers.HOTBAR, LitRPG.id("hud"), HudOverlay::render);
        }
    }

    @EventBusSubscriber(modid = LitRPG.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        /** Ванильные сердца, голод, броня и опыт заменены своим HUD. */
        private static final Set<net.minecraft.resources.ResourceLocation> HIDDEN = Set.of(
                VanillaGuiLayers.PLAYER_HEALTH, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.ARMOR_LEVEL,
                VanillaGuiLayers.EXPERIENCE_BAR, VanillaGuiLayers.EXPERIENCE_LEVEL);

        @SubscribeEvent
        public static void hideVanilla(RenderGuiLayerEvent.Pre e) {
            if (HIDDEN.contains(e.getName())) e.setCanceled(true);
        }

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            boolean needRole = ClientPlayerData.received() && !ClientPlayerData.role().chosen();
            for (int i = 0; i < KeyBinds.PARTY_TARGET.length; i++) {
                while (KeyBinds.PARTY_TARGET[i].consumeClick()) ClientParty.toggle(i);
            }
            while (KeyBinds.CHARACTER.consumeClick()) {
                if (mc.screen == null) mc.setScreen(needRole ? new RoleScreen() : new CharacterScreen());
            }
            // Роль не выбрана — Система сразу предлагает выбрать (как только закрыты другие окна).
            if (needRole && mc.screen == null && mc.getOverlay() == null) mc.setScreen(new RoleScreen());
        }

        /**
         * На сервере без лицензии клиент пугает «невозможно проверить сообщения чата». Для нашей игры это шум:
         * первые секунды после входа прячем эту всплывашку.
         */
        private static int quietTicks;

        @SubscribeEvent
        public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn e) {
            quietTicks = 200;
        }

        @SubscribeEvent
        public static void hideChatWarning(ClientTickEvent.Post e) {
            if (quietTicks > 0 && quietTicks-- % 5 == 0) {
                net.minecraft.client.gui.components.toasts.SystemToast.forceHide(Minecraft.getInstance().getToasts(),
                        net.minecraft.client.gui.components.toasts.SystemToast.SystemToastId.UNSECURE_SERVER_WARNING);
            }
        }

        /** Посох: ЛКМ — снаряд вместо удара. Откат проверяет и клиент (не слать лишнего), и сервер. */
        @SubscribeEvent
        public static void attack(InputEvent.InteractionKeyMappingTriggered e) {
            Minecraft mc = Minecraft.getInstance();
            if (!e.isAttack() || mc.player == null) return;
            if (mc.player.getMainHandItem().getItem() instanceof StaffItem staff) {
                e.setCanceled(true);
                e.setSwingHand(false);
                if (!mc.player.getCooldowns().isOnCooldown(staff)) PacketDistributor.sendToServer(StaffCastPayload.INSTANCE);
            }
        }

        @SubscribeEvent
        public static void world(RenderLevelStageEvent e) {
            if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            var buf = Minecraft.getInstance().renderBuffers().bufferSource();
            float partial = e.getPartialTick().getGameTimeDeltaPartialTick(true);
            ClientAggro.renderWorld(e.getPoseStack(), buf, e.getCamera(), partial);
            ClientPopups.renderWorld(e.getPoseStack(), buf, e.getCamera(), partial);
            buf.endBatch();
        }

        @SubscribeEvent
        public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
            ClientPlayerData.reset();
            ClientParty.reset();
            ClientAggro.clear();
            ClientPopups.clear();
        }
    }
}
