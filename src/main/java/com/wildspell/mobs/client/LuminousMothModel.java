package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * A small moth: a furry 2x2x6 body, a head with big eyes on its sides and two feathered antennae,
 * and flat fore- and hindwings. It holds itself upright: nose-up in flight with deep wingbeats,
 * settled on top of something with its wings raised together over its back, and on a wall head-up
 * with its wings spread flat against the stone.
 */
public class LuminousMothModel extends HierarchicalModel<LuminousMoth> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("luminous_moth"), "main");

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftHindwing;
    private final ModelPart rightHindwing;

    public LuminousMothModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
        ModelPart head = this.body.getChild("head");
        this.leftAntenna = head.getChild("left_antenna");
        this.rightAntenna = head.getChild("right_antenna");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftHindwing = this.body.getChild("left_hindwing");
        this.rightHindwing = this.body.getChild("right_hindwing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 6.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(16, 0).addBox(-1.0F, -1.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -2.0F));
        // Big round eyes bulging from either side of the head.
        head.addOrReplaceChild("left_eye", CubeListBuilder.create()
                        .texOffs(24, 0).addBox(0.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(1.0F, -0.2F, -1.1F));
        head.addOrReplaceChild("right_eye", CubeListBuilder.create()
                        .texOffs(24, 0).mirror().addBox(-1.0F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-1.0F, -0.2F, -1.1F));
        head.addOrReplaceChild("left_antenna", CubeListBuilder.create()
                        .texOffs(16, 4).addBox(-1.0F, 0.0F, -4.0F, 2.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(0.6F, -1.0F, -1.5F, -0.6F, -0.35F, 0.0F));
        head.addOrReplaceChild("right_antenna", CubeListBuilder.create()
                        .texOffs(16, 4).mirror().addBox(-1.0F, 0.0F, -4.0F, 2.0F, 0.0F, 4.0F),
                PartPose.offsetAndRotation(-0.6F, -1.0F, -1.5F, -0.6F, 0.35F, 0.0F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(0.0F, 0.0F, -2.0F, 7.0F, 0.0F, 6.0F),
                PartPose.offset(1.0F, -1.0F, -1.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(0, 8).mirror().addBox(-7.0F, 0.0F, -2.0F, 7.0F, 0.0F, 6.0F),
                PartPose.offset(-1.0F, -1.0F, -1.0F));
        body.addOrReplaceChild("left_hindwing", CubeListBuilder.create()
                        .texOffs(0, 14).addBox(0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F),
                PartPose.offset(1.0F, -0.5F, 1.0F));
        body.addOrReplaceChild("right_hindwing", CubeListBuilder.create()
                        .texOffs(0, 14).mirror().addBox(-5.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F),
                PartPose.offset(-1.0F, -0.5F, 1.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(LuminousMoth moth, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        // The antennae sway a little whatever the moth is doing.
        float sway = Mth.sin(ageInTicks * 0.13F) * 0.08F;
        this.leftAntenna.xRot += sway;
        this.rightAntenna.xRot -= sway;
        if (moth.isPerched() && moth.getPerchFace() != net.minecraft.core.Direction.UP) {
            // On a wall: body straight up with the belly to the stone, wings spread flat against it
            // in a delta, lifting off it a little now and then.
            float lift = Math.max(0.0F, Mth.sin(ageInTicks * 0.05F + moth.getId()) - 0.8F) / 0.2F;
            this.body.xRot = -Mth.HALF_PI;
            // Toward the wall: the moth sits a little off it (LuminousMoth.WALL_GAP).
            this.body.z -= 3.5F;
            this.body.y -= 2.0F;
            this.leftWing.zRot = -0.1F - lift * 0.5F;
            this.rightWing.zRot = 0.1F + lift * 0.5F;
            this.leftHindwing.zRot = -0.05F - lift * 0.4F;
            this.rightHindwing.zRot = 0.05F + lift * 0.4F;
            this.leftWing.yRot = -0.4F;
            this.rightWing.yRot = 0.4F;
            this.leftHindwing.yRot = -0.55F;
            this.rightHindwing.yRot = 0.55F;
            return;
        }
        if (moth.isPerched()) {
            // Settled upright: body raised nose-up, wings lifted together over the back. Every so
            // often it slowly opens them and closes them again.
            float open = Math.max(0.0F, Mth.sin(ageInTicks * 0.04F + moth.getId()) - 0.7F) / 0.3F;
            float lift = -1.35F + open * 1.0F;
            this.body.xRot = -0.5F;
            this.leftWing.zRot = lift;
            this.rightWing.zRot = -lift;
            this.leftHindwing.zRot = lift + 0.15F;
            this.rightHindwing.zRot = -lift - 0.15F;
            this.leftWing.yRot = -0.2F;
            this.rightWing.yRot = 0.2F;
            this.leftHindwing.yRot = -0.3F;
            this.rightHindwing.yRot = 0.3F;
            return;
        }
        // In flight: steeply nose-up, with deep beats from high over the back to below the body,
        // the hindwings a touch behind, and a bob.
        float beat = ageInTicks * 1.1F + moth.getId();
        float fore = -0.55F + Mth.sin(beat) * 0.9F;
        float hind = -0.5F + Mth.sin(beat - 0.6F) * 0.75F;
        this.leftWing.zRot = fore;
        this.rightWing.zRot = -fore;
        this.leftHindwing.zRot = hind;
        this.rightHindwing.zRot = -hind;
        this.leftWing.yRot = -0.1F;
        this.rightWing.yRot = 0.1F;
        this.body.xRot = -0.7F;
        this.body.y += Mth.sin(ageInTicks * 0.3F + moth.getId()) * 0.6F - 2.0F;
    }
}
