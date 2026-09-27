package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Pegasus;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PegasusRenderer extends AbstractHorseRenderer<Pegasus, PegasusModel> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/pegasus.png");
    /** The most it tilts nose-up or nose-down in flight, in degrees. */
    private static final float MAX_PITCH = 30.0F;

    public PegasusRenderer(EntityRendererProvider.Context context) {
        super(context, new PegasusModel(context.bakeLayer(PegasusModel.LAYER)), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(Pegasus pegasus) {
        return TEXTURE;
    }

    /** In the air it pitches with its climb or dive. */
    @Override
    protected void setupRotations(Pegasus pegasus, PoseStack poseStack, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(pegasus, poseStack, bob, yBodyRot, partialTick, scale);
        float spread = pegasus.getWingSpread(partialTick);
        if (spread > 0.0F) {
            double rise = pegasus.getY() - pegasus.yo;
            double run = Math.sqrt(Mth.square(pegasus.getX() - pegasus.xo) + Mth.square(pegasus.getZ() - pegasus.zo));
            float pitch = (float) Mth.clamp(Math.toDegrees(Math.atan2(rise, Math.max(run, 0.2))), -MAX_PITCH, MAX_PITCH);
            poseStack.translate(0.0F, pegasus.getBbHeight() * 0.5F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(pitch * spread));
            poseStack.translate(0.0F, -pegasus.getBbHeight() * 0.5F, 0.0F);
        }
    }
}
