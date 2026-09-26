package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Mossback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

public class MossbackRenderer extends MobRenderer<Mossback, MossbackModel> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/mossback.png");
    private static final RenderType EYES = RenderType.eyes(WildspellMobs.id("textures/entity/mossback_eyes.png"));

    public MossbackRenderer(EntityRendererProvider.Context context) {
        super(context, new MossbackModel(context.bakeLayer(MossbackModel.LAYER)), 1.2F);
        this.addLayer(new GlowLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Mossback mossback) { return TEXTURE; }

    private static class GlowLayer extends RenderLayer<Mossback, MossbackModel> {
        GlowLayer(RenderLayerParent<Mossback, MossbackModel> parent) { super(parent); }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, Mossback mossback,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            this.getParentModel().renderToBuffer(poseStack, buffer.getBuffer(EYES),
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        }
    }
}
