package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.IceLich;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** A tall, frost-rimed skeleton in a dark robe and ice crown, with glowing eyes. */
public class IceLichRenderer extends HumanoidMobRenderer<IceLich, SkeletonModel<IceLich>> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/ice_lich.png");
    private static final RenderType EYES = RenderType.eyes(WildspellMobs.id("textures/entity/ice_lich_eyes.png"));
    private static final float SCALE = 1.3F;

    public IceLichRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.6F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }
        });
    }

    @Override
    protected void scale(IceLich lich, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(IceLich lich) {
        return TEXTURE;
    }

    /** The lich glows faintly, so it's visible in unlit caves. */
    @Override
    protected int getBlockLightLevel(IceLich lich, BlockPos pos) {
        return Math.max(8, super.getBlockLightLevel(lich, pos));
    }
}
