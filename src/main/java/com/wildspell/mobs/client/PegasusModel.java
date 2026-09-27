package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Pegasus;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The vanilla horse with a pair of feathered wings at the shoulders, each an inner and an outer panel.
 * Folded along its flanks on the ground; spread and beating in the air, hard when it climbs.
 * The texture is the horse's 64x64 sheet with the wings to its right (128x64).
 */
public class PegasusModel extends HorseModel<Pegasus> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("pegasus"), "main");

    // Folded (left wing; the right mirrors it): swung back along the flank, draped outward and down,
    // the outer panel turned back under the inner one and lifted clear of it.
    private static final float FOLDED_YAW = -Mth.HALF_PI;
    private static final float FOLDED_DRAPE = -2.0F;
    private static final float FOLDED_TIP = -3.0F;
    private static final float FOLDED_TIP_LIFT = 1.05F;

    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;

    public PegasusModel(ModelPart root) {
        super(root);
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("tip");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HorseModel.createBodyMesh(CubeDeformation.NONE);
        PartDefinition body = mesh.getRoot().getChild("body");
        // Hinged at the top of each shoulder; the panels run outward along x and trail back along +z.
        PartDefinition left = body.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(64, 0).addBox(0.0F, -0.5F, -1.0F, 14.0F, 1.0F, 12.0F),
                PartPose.offset(4.0F, -7.5F, -11.0F));
        left.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(64, 14).addBox(0.0F, -0.5F, -1.0F, 14.0F, 1.0F, 14.0F),
                PartPose.offset(14.0F, 0.0F, 0.0F));
        PartDefinition right = body.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(64, 0).mirror().addBox(-14.0F, -0.5F, -1.0F, 14.0F, 1.0F, 12.0F),
                PartPose.offset(-4.0F, -7.5F, -11.0F));
        right.addOrReplaceChild("tip",
                CubeListBuilder.create().texOffs(64, 14).mirror().addBox(-14.0F, -0.5F, -1.0F, 14.0F, 1.0F, 14.0F),
                PartPose.offset(-14.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void prepareMobModel(Pegasus pegasus, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(pegasus, limbSwing, limbSwingAmount, partialTick);
        float spread = pegasus.getWingSpread(partialTick);
        float flap = pegasus.getFlap(partialTick);
        // Spread: a beat of about +-40 degrees about level, the outer panel lagging the inner.
        float beat = -Mth.sin(flap) * 0.7F;
        float tipBeat = -Mth.sin(flap - 0.8F) * 0.5F;
        this.leftWing.yRot = Mth.lerp(spread, FOLDED_YAW, 0.0F);
        this.leftWing.zRot = Mth.lerp(spread, FOLDED_DRAPE, beat);
        this.leftWingTip.zRot = Mth.lerp(spread, FOLDED_TIP, tipBeat);
        this.leftWingTip.y = Mth.lerp(spread, FOLDED_TIP_LIFT, 0.0F);
        this.rightWing.yRot = -this.leftWing.yRot;
        this.rightWing.zRot = -this.leftWing.zRot;
        this.rightWingTip.zRot = -this.leftWingTip.zRot;
        this.rightWingTip.y = this.leftWingTip.y;
    }
}
