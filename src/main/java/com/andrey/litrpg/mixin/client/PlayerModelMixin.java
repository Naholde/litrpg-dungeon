package com.andrey.litrpg.mixin.client;

import com.andrey.litrpg.client.anim.ClientAnims;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Примесь: сразу после того, как игра расставила руки и ноги игрока, мы доворачиваем их
 * под движение навыка. Иначе никак — поза задаётся внутри модели, и события для этого нет.
 * За частями тела сами идут рукава, броня и оружие в руке, поэтому отдельный мод анимаций не нужен.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void litrpg$skillPose(LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                  float netHeadYaw, float headPitch, CallbackInfo ci) {
        ClientAnims.pose((PlayerModel<?>) (Object) this, entity, ageInTicks);
    }
}
