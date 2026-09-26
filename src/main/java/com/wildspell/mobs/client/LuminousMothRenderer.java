package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class LuminousMothRenderer extends MobRenderer<LuminousMoth, LuminousMothModel> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/luminous_moth.png");
    private static final RenderType GLOW = RenderType.eyes(WildspellMobs.id("textures/entity/luminous_moth_glow.png"));
    private static final float SCALE = 1.0F;

    public LuminousMothRenderer(EntityRendererProvider.Context context) {
        super(context, new LuminousMothModel(context.bakeLayer(LuminousMothModel.LAYER)), 0.25F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LuminousMoth moth) {
        return TEXTURE;
    }

    @Override
    protected void scale(LuminousMoth moth, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }

    /** It shines by its own light. */
    @Override
    protected int getBlockLightLevel(LuminousMoth moth, BlockPos pos) {
        return Math.max(12, super.getBlockLightLevel(moth, pos));
    }
}
