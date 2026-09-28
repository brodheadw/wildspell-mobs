package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.IceLich;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class IceLichRenderer extends GeoEntityRenderer<IceLich> {
    public IceLichRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("ice_lich"), true));
        this.shadowRadius = 0.6F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected int getBlockLightLevel(IceLich lich, BlockPos pos) {
        return Math.max(8, super.getBlockLightLevel(lich, pos));
    }
}
