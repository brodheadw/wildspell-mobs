package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ElectricEel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class ElectricEelRenderer extends MobRenderer<ElectricEel, ElectricEelModel> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/electric_eel.png");
    private static final RenderType GLOW = RenderType.eyes(WildspellMobs.id("textures/entity/electric_eel_glow.png"));

    public ElectricEelRenderer(EntityRendererProvider.Context context) {
        super(context, new ElectricEelModel(context.bakeLayer(ElectricEelModel.LAYER)), 0.3F);
        this.addLayer(new OrganLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ElectricEel eel) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(ElectricEel eel, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(eel, poseStack, bob, yBodyRot, partialTick, scale);
        if (!eel.isInWater()) {
            poseStack.translate(0.1F, 0.1F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }

    @Override
    protected int getBlockLightLevel(ElectricEel eel, BlockPos pos) {
        return Math.max(Mth.floor(eel.getGlow(0.0F) * 15.0F), super.getBlockLightLevel(eel, pos));
    }

    private static class OrganLayer extends RenderLayer<ElectricEel, ElectricEelModel> {
        OrganLayer(RenderLayerParent<ElectricEel, ElectricEelModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, ElectricEel eel, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            float glow = eel.getGlow(partialTick);
            if (glow <= 0.0F) {
                return;
            }
            this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(GLOW), 0xF000F0, OverlayTexture.NO_OVERLAY,
                    FastColor.ARGB32.colorFromFloat(1.0F, glow, glow, glow));
        }
    }
}
