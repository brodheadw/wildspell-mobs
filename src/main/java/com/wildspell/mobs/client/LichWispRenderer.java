package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LichWisp;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The lich's soul: a camera-facing, fullbright wisp that pulses as it flies. Drawn with an entity
 * render type, so its glowing outline shows through rock and a player can follow it home.
 */
public class LichWispRenderer extends EntityRenderer<LichWisp> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/lich_wisp.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);

    public LichWispRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LichWisp wisp, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float size = 0.6F + Mth.sin((wisp.tickCount + partialTick) * 0.4F) * 0.08F;
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
        vertex(buffer, pose, poseStack, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(buffer, pose, poseStack, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(buffer, pose, poseStack, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(buffer, pose, poseStack, -0.5F, 0.5F, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(wisp, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, PoseStack poseStack, float x, float y, float u, float v) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(LichWisp wisp) {
        return TEXTURE;
    }
}
