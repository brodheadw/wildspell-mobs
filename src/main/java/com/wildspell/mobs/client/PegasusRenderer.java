package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Pegasus;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class PegasusRenderer extends AbstractHorseRenderer<Pegasus, PegasusModel> {
    private static final Map<Pegasus.Variant, ResourceLocation> TEXTURES = new EnumMap<>(Pegasus.Variant.class);
    static {
        for (Pegasus.Variant variant : Pegasus.Variant.values()) {
            TEXTURES.put(variant, WildspellMobs.id("textures/entity/pegasus_" + variant.name + ".png"));
        }
    }
    /** A little bigger than a horse (1.1). */
    private static final float SCALE = 1.25F;
    /** The most it tilts nose-up or nose-down in flight, in degrees. */
    private static final float MAX_PITCH = 30.0F;

    public PegasusRenderer(EntityRendererProvider.Context context) {
        super(context, new PegasusModel(context.bakeLayer(PegasusModel.LAYER)), SCALE);
    }

    @Override
    public ResourceLocation getTextureLocation(Pegasus pegasus) {
        return TEXTURES.get(pegasus.getVariant());
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
