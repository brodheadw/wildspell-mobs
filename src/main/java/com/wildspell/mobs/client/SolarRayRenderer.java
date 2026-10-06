package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.SolarRay;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class SolarRayRenderer extends EntityRenderer<SolarRay> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/solar_ray.png");
    private static final RenderType RENDER_TYPE = RenderType.entityTranslucentEmissive(TEXTURE);
    private static final float LENGTH = 1.6F;
    private static final float WIDTH = 0.2F;

    public SolarRayRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(SolarRay ray, float yaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        Vec3 motion = ray.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6) {
            return;
        }
        float heading = (float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG);
        float w = WIDTH * (1.0F + Mth.sin((ray.tickCount + partialTick) * 1.7F) * 0.12F);
        poseStack.pushPose();
        poseStack.translate(0.0F, ray.getBbHeight() / 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(heading - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch));
        VertexConsumer buffer = buffers.getBuffer(RENDER_TYPE);
        for (int i = 0; i < 2; ++i) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            Matrix4f pose = poseStack.last().pose();
            vertex(buffer, pose, poseStack, -LENGTH, -w, 0.0F, 1.0F);
            vertex(buffer, pose, poseStack, LENGTH * 0.25F, -w, 1.0F, 1.0F);
            vertex(buffer, pose, poseStack, LENGTH * 0.25F, w, 1.0F, 0.0F);
            vertex(buffer, pose, poseStack, -LENGTH, w, 0.0F, 0.0F);
            vertex(buffer, pose, poseStack, -LENGTH, w, 0.0F, 0.0F);
            vertex(buffer, pose, poseStack, LENGTH * 0.25F, w, 1.0F, 0.0F);
            vertex(buffer, pose, poseStack, LENGTH * 0.25F, -w, 1.0F, 1.0F);
            vertex(buffer, pose, poseStack, -LENGTH, -w, 0.0F, 1.0F);
        }
        poseStack.popPose();
        super.render(ray, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f pose, PoseStack poseStack, float x, float y, float u, float v) {
        buffer.addVertex(pose, x, y, 0.0F).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(0xF000F0).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(SolarRay ray) {
        return TEXTURE;
    }
}
