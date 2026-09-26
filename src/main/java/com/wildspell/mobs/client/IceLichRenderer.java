package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.IceLich;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * GeckoLib model: a robed, legless frost lich with an ice crown and staff (geo/entity/ice_lich.geo.json,
 * animations/entity/ice_lich.animation.json). Its eyes, soul-light and staff crystal glow from
 * the texture's glowmask; its head turns to look.
 */
public class IceLichRenderer extends GeoEntityRenderer<IceLich> {
    public IceLichRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("ice_lich"), true));
        this.shadowRadius = 0.6F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    /** The lich glows faintly, so it's visible in unlit caves. */
    @Override
    protected int getBlockLightLevel(IceLich lich, BlockPos pos) {
        return Math.max(8, super.getBlockLightLevel(lich, pos));
    }
}
