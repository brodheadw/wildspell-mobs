package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public class GlowSpriteRenderer<T extends Entity> extends EntityRenderer<T> {
    private final ResourceLocation texture;
    private final RenderType renderType;
    private final float size;

    public GlowSpriteRenderer(EntityRendererProvider.Context context, ResourceLocation texture, float size) {
        super(context);
        this.texture = texture;
        this.renderType = RenderType.entityTranslucentEmissive(texture);
        this.size = size;
    }

    @Override
    public void render(T entity, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float size = this.size * (1.0F + Mth.sin((entity.tickCount + partialTick) * 0.4F) * 0.13F);
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() / 2.0F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(size, size, size);
        Matrix4f pose = poseStack.last().pose();
        VertexConsumer buffer = buffers.getBuffer(this.renderType);
        vertex(buffer, pose, poseStack, -0.5F, -0.5F, 0.0F, 1.0F);
        vertex(buffer, pose, poseStack, 0.5F, -0.5F, 1.0F, 1.0F);
        vertex(buffer, pose, poseStack, 0.5F, 0.5F, 1.0F, 0.0F);
        vertex(buffer, pose, poseStack, -0.5F, 0.5F, 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, PoseStack poseStack, float x, float y, float u, float v) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
