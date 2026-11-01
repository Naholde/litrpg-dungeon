package com.andrey.litrpg.client.render;

import com.andrey.litrpg.LitRPG;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Отрисовщики моделей GeckoLib: модель/анимации/текстура по имени + светящаяся маска ({@code _glowmask}). */
public final class GeoRenderers {
    private GeoRenderers() {
    }

    public static <T extends Entity & GeoEntity> GeoEntityRenderer<T> glowing(EntityRendererProvider.Context ctx, String model,
                                                                             float scale) {
        GeoEntityRenderer<T> r = new GeoEntityRenderer<>(ctx, new DefaultedEntityGeoModel<>(LitRPG.id(model)));
        r.addRenderLayer(new AutoGlowingGeoLayer<>(r));
        r.withScale(scale);
        return r;
    }
}
