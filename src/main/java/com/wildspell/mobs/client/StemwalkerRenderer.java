package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Stemwalker;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class StemwalkerRenderer extends GeoEntityRenderer<Stemwalker> {
    public StemwalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("stemwalker"), false));
        this.shadowRadius = 0.45F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(Stemwalker stem) {
        return 0.0F;
    }
}
