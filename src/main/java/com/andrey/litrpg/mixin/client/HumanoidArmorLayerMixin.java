package com.andrey.litrpg.mixin.client;

import com.andrey.litrpg.client.look.ClientLooks;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Облик класса заменяет вид брони: у игрока с классом железные/алмазные латы не рисуются поверх капюшона
 * и плаща (сила брони при этом остаётся — меняется только внешний вид). Без класса броня видна как обычно.
 */
@Mixin(HumanoidArmorLayer.class)
public abstract class HumanoidArmorLayerMixin {
    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void litrpg$classLook(PoseStack ps, MultiBufferSource buffers, int light, LivingEntity entity, float limbSwing,
                                  float limbSwingAmount, float partial, float ageInTicks, float netHeadYaw, float headPitch,
                                  CallbackInfo ci) {
        if (ClientLooks.hasLook(entity)) ci.cancel();
    }
}
