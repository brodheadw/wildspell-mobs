package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Apollo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ApolloRenderer extends GeoEntityRenderer<Apollo> {
    public ApolloRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected int getBlockLightLevel(Apollo apollo, BlockPos pos) {
        return 15;
    }

    @Override
    protected float getDeathMaxRotation(Apollo apollo) {
        return 0.0F;
    }

    private static class Model extends DefaultedEntityGeoModel<Apollo> {
        Model() {
            super(WildspellMobs.id("apollo"), false);
        }

        @Override
        public void setCustomAnimations(Apollo apollo, long instanceId, AnimationState<Apollo> state) {
            super.setCustomAnimations(apollo, instanceId, state);
            int rays = apollo.rays();
            for (int i = 0; i < Apollo.MAX_RAYS; ++i) {
                int index = i;
                this.getBone("ray_" + i).ifPresent(bone -> bone.setHidden(index >= rays));
            }
        }
    }
}
