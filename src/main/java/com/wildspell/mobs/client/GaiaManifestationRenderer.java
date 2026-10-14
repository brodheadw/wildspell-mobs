package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.GaiaManifestation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class GaiaManifestationRenderer extends GeoEntityRenderer<GaiaManifestation> {
    public GaiaManifestationRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("gaia_manifestation"), false));
        this.shadowRadius = 0.0F;
    }
}
