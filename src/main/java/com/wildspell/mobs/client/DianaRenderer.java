package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Diana;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class DianaRenderer extends GeoEntityRenderer<Diana> {
    private static final ResourceLocation[] FACES = faces("diana_");
    private static final ResourceLocation[] GRIEF = faces("diana_grief_");

    public DianaRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static ResourceLocation[] faces(String prefix) {
        ResourceLocation[] out = new ResourceLocation[Diana.PHASES];
        for (int i = 0; i < Diana.PHASES; ++i) {
            out[i] = WildspellMobs.id("textures/entity/" + prefix + i + ".png");
        }
        return out;
    }

    @Override
    protected int getBlockLightLevel(Diana diana, BlockPos pos) {
        return 15;
    }

    @Override
    protected float getDeathMaxRotation(Diana diana) {
        return 0.0F;
    }

    private static class Model extends DefaultedEntityGeoModel<Diana> {
        Model() {
            super(WildspellMobs.id("diana"), false);
        }

        @Override
        public ResourceLocation getTextureResource(Diana diana) {
            return (diana.isGrieving() ? GRIEF : FACES)[diana.moonPhase()];
        }
    }
}
