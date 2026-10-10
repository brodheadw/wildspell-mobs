package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Scorpion;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ScorpionRenderer extends GeoEntityRenderer<Scorpion> {
    public ScorpionRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("scorpion"), false));
        this.scaleWidth = this.scaleHeight = 0.5F;
        this.shadowRadius = 0.25F;
    }

    @Override
    protected float getShadowRadius(Scorpion scorpion) {
        return scorpion.isBuried() ? 0.0F : super.getShadowRadius(scorpion);
    }
}
