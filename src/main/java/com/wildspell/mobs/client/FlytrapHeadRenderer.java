package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.FlytrapHead;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class FlytrapHeadRenderer extends GeoEntityRenderer<FlytrapHead> {
    public FlytrapHeadRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(WildspellMobs.id("flytrap_head"), true));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(FlytrapHead head, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.scaleWidth = this.scaleHeight = head.getScale();
        super.render(head, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    protected float getDeathMaxRotation(FlytrapHead head) {
        return 0.0F;
    }
}
