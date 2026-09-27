package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.FlytrapHead;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/**
 * GeckoLib model: a green-and-red jawed pod on a short neck (geo/entity/flytrap_head.geo.json,
 * animations/entity/flytrap_head.animation.json), scaled per head: a sprout's small head, the young
 * plant's and the side heads, and the grown plant's big top head. The rest of the plant (leaves, stalk)
 * is the block it sits on. Its jaws (the head bone) pitch toward what it's watching; the whole head
 * turns with them.
 */
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

    /** It withers where it stands (the wither animation) rather than toppling over like a mob. */
    @Override
    protected float getDeathMaxRotation(FlytrapHead head) {
        return 0.0F;
    }
}
