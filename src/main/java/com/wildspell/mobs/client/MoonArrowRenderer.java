package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.MoonArrow;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MoonArrowRenderer extends ArrowRenderer<MoonArrow> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/moon_arrow.png");

    public MoonArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MoonArrow arrow, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        super.render(arrow, yaw, partialTick, poseStack, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(MoonArrow arrow) {
        return TEXTURE;
    }
}
