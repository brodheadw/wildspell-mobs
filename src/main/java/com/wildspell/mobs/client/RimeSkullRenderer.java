package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.RimeSkull;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class RimeSkullRenderer extends MobRenderer<RimeSkull, RimeSkullModel> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[RimeSkull.VARIANTS];
    private static final RenderType[] EYES = new RenderType[RimeSkull.VARIANTS];

    static {
        for (int i = 0; i < RimeSkull.VARIANTS; ++i) {
            TEXTURES[i] = WildspellMobs.id("textures/entity/rime_skull_" + i + ".png");
            EYES[i] = RenderType.eyes(WildspellMobs.id("textures/entity/rime_skull_eyes_" + i + ".png"));
        }
    }

    public RimeSkullRenderer(EntityRendererProvider.Context context) {
        super(context, new RimeSkullModel(context.bakeLayer(RimeSkullModel.LAYER)), 0.3F);
        this.addLayer(new EyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(RimeSkull skull) {
        return TEXTURES[skull.getVariant()];
    }

    @Override
    protected int getBlockLightLevel(RimeSkull skull, BlockPos pos) {
        return Math.max(7, super.getBlockLightLevel(skull, pos));
    }

    private static class EyesLayer extends RenderLayer<RimeSkull, RimeSkullModel> {
        EyesLayer(RenderLayerParent<RimeSkull, RimeSkullModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, RimeSkull skull,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(EYES[skull.getVariant()]), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
