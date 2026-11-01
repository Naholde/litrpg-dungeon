package com.andrey.litrpg.client;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.screen.CharacterScreen;
import com.andrey.litrpg.client.screen.ClassChoiceScreen;
import com.andrey.litrpg.client.screen.RoleScreen;
import com.andrey.litrpg.net.SkillActionPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.andrey.litrpg.client.fx.ClientAggro;
import com.andrey.litrpg.client.fx.ClientPopups;
import com.andrey.litrpg.client.fx.FxRenderer;
import com.andrey.litrpg.client.render.GeoRenderers;
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

import net.minecraft.world.phys.Vec3;

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
            for (var k : KeyBinds.SKILL) e.register(k);
            e.register(KeyBinds.DASH);
            e.register(KeyBinds.CODEX);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
            e.registerEntityRenderer(ModEntities.MAGIC_BOLT.get(), MagicBoltRenderer::new);
            e.registerEntityRenderer(ModEntities.TRAINING_DUMMY.get(), TrainingDummyRenderer::new);
            e.registerEntityRenderer(ModEntities.TRIAL_WARDEN.get(), ctx -> GeoRenderers.glowing(ctx, "trial_warden", 1.6f));
            e.registerEntityRenderer(ModEntities.POWER_NODE.get(), ctx -> GeoRenderers.glowing(ctx, "power_node", 1.0f));
            e.registerEntityRenderer(ModEntities.TRADER.get(), ctx -> GeoRenderers.glowing(ctx, "trader", 1.0f));
            e.registerEntityRenderer(ModEntities.GATEKEEPER.get(), ctx -> GeoRenderers.glowing(ctx, "gatekeeper", 2.2f));
            e.registerEntityRenderer(ModEntities.MORRA.get(), ctx -> GeoRenderers.glowing(ctx, "morra", 1.35f));
            e.registerEntityRenderer(ModEntities.VESSA.get(), ctx -> GeoRenderers.glowing(ctx, "vessa", 1.35f));
            e.registerEntityRenderer(ModEntities.LICH.get(), ctx -> GeoRenderers.glowing(ctx, "lich", 1.9f));
            e.registerEntityRenderer(ModEntities.ICE_TOMB.get(), ctx -> GeoRenderers.glowing(ctx, "ice_tomb", 1.0f));
            e.registerEntityRenderer(ModEntities.DOPPEL.get(), ctx -> GeoRenderers.glowing(ctx, "doppel", 1.5f));
            e.registerEntityRenderer(ModEntities.CHRONOPHAGE.get(), ctx -> GeoRenderers.glowing(ctx, "chronophage", 1.8f));
            e.registerEntityRenderer(ModEntities.XALATHAR.get(), ctx -> GeoRenderers.glowing(ctx, "xalathar", 2.6f));
            e.registerEntityRenderer(ModEntities.HOURGLASS.get(), ctx -> GeoRenderers.glowing(ctx, "hourglass", 1.0f));
            e.registerEntityRenderer(ModEntities.SMELTER.get(), ctx -> GeoRenderers.glowing(ctx, "smelter", 2.4f));
            e.registerEntityRenderer(ModEntities.CHARIOT.get(), ctx -> GeoRenderers.glowing(ctx, "chariot", 1.5f));
            e.registerEntityRenderer(ModEntities.ASH_LORD.get(), ctx -> GeoRenderers.glowing(ctx, "ash_lord", 1.7f));
            e.registerEntityRenderer(ModEntities.TIDE_WARDEN.get(), ctx -> GeoRenderers.glowing(ctx, "tide_warden", 2.0f));
            e.registerEntityRenderer(ModEntities.LEVIATHAN.get(), ctx -> GeoRenderers.glowing(ctx, "leviathan", 1.6f));
            e.registerEntityRenderer(ModEntities.NAIAD.get(), ctx -> GeoRenderers.glowing(ctx, "naiad", 1.5f));
            e.registerEntityRenderer(ModEntities.ARACHNA.get(), ctx -> GeoRenderers.glowing(ctx, "arachna", 3.0f));
            e.registerEntityRenderer(ModEntities.ROT_ENT.get(), ctx -> GeoRenderers.glowing(ctx, "rot_ent", 2.4f));
            e.registerEntityRenderer(ModEntities.MORENA.get(), ctx -> GeoRenderers.glowing(ctx, "morena", 1.5f));
            e.registerEntityRenderer(ModEntities.COCOON.get(), ctx -> GeoRenderers.glowing(ctx, "cocoon", 1.0f));
            e.registerEntityRenderer(ModEntities.EGG.get(), ctx -> GeoRenderers.glowing(ctx, "egg", 1.0f));
            e.registerEntityRenderer(ModEntities.PHYLACTERY.get(), ctx -> GeoRenderers.glowing(ctx, "phylactery", 1.0f));
            for (var s : com.andrey.litrpg.mob.Bestiary.all()) {
                e.registerEntityRenderer(com.andrey.litrpg.mob.ModMobs.TYPES.get(s.id()).get(),
                        ctx -> GeoRenderers.glowing(ctx, s.model(), s.scale()));
            }
            for (var k : com.andrey.litrpg.skill.prop.PropKind.all()) {
                e.registerEntityRenderer(com.andrey.litrpg.skill.prop.ModProps.PROPS.get(k.id()).get(),
                        ctx -> GeoRenderers.glowing(ctx, k.id(), k.scale()));
            }
            for (var k : com.andrey.litrpg.skill.prop.SummonKind.all()) {
                e.registerEntityRenderer(com.andrey.litrpg.skill.prop.ModProps.SUMMONS.get(k.id()).get(),
                        ctx -> GeoRenderers.glowing(ctx, k.id(), k.scale()));
            }
        }

        @SubscribeEvent
        public static void layers(RegisterGuiLayersEvent e) {
            e.registerAbove(VanillaGuiLayers.HOTBAR, LitRPG.id("hud"), HudOverlay::render);
            e.registerAbove(LitRPG.id("hud"), LitRPG.id("run"), ClientRun::render);
        }

        /** Небо подземелья: ни облаков, ни солнца — цвет тумана биома этапа. */
        @SubscribeEvent
        public static void skies(net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent e) {
            e.register(LitRPG.id("dungeon"), new net.minecraft.client.renderer.DimensionSpecialEffects(Float.NaN, false,
                    net.minecraft.client.renderer.DimensionSpecialEffects.SkyType.NONE, false, false) {
                @Override
                public Vec3 getBrightnessDependentFogColor(Vec3 fog, float brightness) {
                    return fog;
                }

                @Override
                public boolean isFoggyAt(int x, int y) {
                    return false;
                }
            });
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

        private static boolean classWindowShown;

        @SubscribeEvent
        public static void tick(ClientTickEvent.Post e) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            boolean needRole = ClientPlayerData.received() && !ClientPlayerData.role().chosen();
            for (int i = 0; i < KeyBinds.PARTY_TARGET.length; i++) {
                while (KeyBinds.PARTY_TARGET[i].consumeClick()) ClientParty.toggle(i);
            }
            for (int i = 0; i < KeyBinds.SKILL.length; i++) {
                while (KeyBinds.SKILL[i].consumeClick()) {
                    if (i < ClientPlayerData.loadout().size()) PacketDistributor.sendToServer(SkillActionPayload.use(i));
                }
            }
            while (KeyBinds.DASH.consumeClick()) {
                // Рывок — в сторону, куда игрок идёт (WASD); стоит — вперёд по взгляду.
                var in = mc.player.input;
                float fw = in.forwardImpulse, st = in.leftImpulse;
                double yaw = Math.toRadians(mc.player.getYRot());
                double x = -Math.sin(yaw) * fw + Math.cos(yaw) * st, z = Math.cos(yaw) * fw + Math.sin(yaw) * st;
                PacketDistributor.sendToServer(SkillActionPayload.dash((float) x, (float) z));
            }
            boolean needClass = ClientPlayerData.received() && ClientPlayerData.classId().isEmpty()
                    && !ClientPlayerData.offers().isEmpty();
            while (KeyBinds.CODEX.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new com.andrey.litrpg.client.screen.CodexScreen());
            }
            com.andrey.litrpg.client.music.MusicController.tick();
            while (KeyBinds.CHARACTER.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(needRole ? new RoleScreen() : needClass ? new ClassChoiceScreen() : new CharacterScreen());
                }
            }
            // Роль не выбрана — Система сразу предлагает выбрать (как только закрыты другие окна).
            if (needRole && mc.screen == null && mc.getOverlay() == null) mc.setScreen(new RoleScreen());
            // Предложены классы — окно открывается само один раз; закрыли — вернутся клавишей K.
            if (needClass && !classWindowShown && mc.screen == null && mc.getOverlay() == null) {
                classWindowShown = true;
                mc.setScreen(new ClassChoiceScreen());
            }
            if (!needClass) classWindowShown = false;
        }

        /**
         * На сервере без лицензии клиент пугает «невозможно проверить сообщения чата». Для нашей игры это шум:
         * первые секунды после входа прячем эту всплывашку.
         */
        private static int quietTicks;

        /** В подземелье ванильная музыка молчит — играет своя (хранители, преддверие, титры). */
        @SubscribeEvent
        public static void music(net.neoforged.neoforge.client.event.SelectMusicEvent e) {
            if (com.andrey.litrpg.client.music.MusicController.inDungeon()) e.setMusic(null);
        }

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
            FxRenderer.render(e.getPoseStack(), buf, e.getCamera(), partial);
            ClientAggro.renderWorld(e.getPoseStack(), buf, e.getCamera(), partial);
            ClientPopups.renderWorld(e.getPoseStack(), buf, e.getCamera(), partial);
            ClientRun.renderWorld(e.getPoseStack(), buf, e.getCamera());
            buf.endBatch();
        }

        @SubscribeEvent
        public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut e) {
            ClientPlayerData.reset();
            ClientParty.reset();
            ClientAggro.clear();
            ClientPopups.clear();
            FxRenderer.clear();
            ClientStatuses.reset();
            ClientSkills.reset();
            ClientBoss.reset();
            ClientRun.reset();
            com.andrey.litrpg.client.music.MusicController.stopAll();
        }
    }
}
