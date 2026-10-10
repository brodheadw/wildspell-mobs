package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Scarab;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ScarabRenderer extends GeoEntityRenderer<Scarab> {
    private static final ResourceLocation DUNG = WildspellMobs.id("textures/entity/scarab_dung.png");

    public ScarabRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.scaleWidth = this.scaleHeight = 0.5F;
        this.shadowRadius = 0.15F;
    }

    private static class Model extends DefaultedEntityGeoModel<Scarab> {
        Model() {
            super(WildspellMobs.id("scarab"), false);
        }

        @Override
        public ResourceLocation getTextureResource(Scarab scarab) {
            return scarab.isDung() ? DUNG : super.getTextureResource(scarab);
        }
    }
}
