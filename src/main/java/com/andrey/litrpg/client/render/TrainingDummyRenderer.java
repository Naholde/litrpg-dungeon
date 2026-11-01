package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.combat.entity.TrainingDummy;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Манекен — соломенное чучело в форме человека (текстура из tools/gen_textures.py). */
public class TrainingDummyRenderer extends HumanoidMobRenderer<TrainingDummy, HumanoidModel<TrainingDummy>> {
    private static final ResourceLocation TEXTURE = LitRPG.id("textures/entity/training_dummy.png");

    public TrainingDummyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(TrainingDummy entity) {
        return TEXTURE;
    }
}
