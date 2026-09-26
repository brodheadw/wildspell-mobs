package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ElectricEel;
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
 * A long eel: a blunt head with a hinged jaw, then four body segments tapering to the tail, each
 * hung off the one before so a wave can run down them. Like a real electric eel it has no dorsal
 * fin, just a long ribbon fin under the back half of the body.
 */
public class ElectricEelModel extends HierarchicalModel<ElectricEel> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("electric_eel"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    /** Front to back: the forebody (which carries the head), three more segments, the tail. */
    private final ModelPart[] segments;

    public ElectricEelModel(ModelPart root) {
        this.root = root;
        ModelPart body = root.getChild("body");
        this.head = body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        ModelPart mid = body.getChild("mid");
        ModelPart hind = mid.getChild("hind");
        ModelPart rear = hind.getChild("rear");
        ModelPart tail = rear.getChild("tail");
        this.segments = new ModelPart[] {body, mid, hind, rear, tail};
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition body = parts.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 22.5F, -7.0F));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 8).addBox(-1.5F, -1.5F, -5.0F, 3.0F, 2.0F, 5.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 1.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));
        PartDefinition mid = body.addOrReplaceChild("mid", CubeListBuilder.create()
                        .texOffs(16, 0).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F)
                        .texOffs(16, 15).addBox(0.0F, 1.5F, 0.0F, 0.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition hind = mid.addOrReplaceChild("hind", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 5.0F)
                        .texOffs(26, 15).addBox(0.0F, 1.5F, 0.0F, 0.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition rear = hind.addOrReplaceChild("rear", CubeListBuilder.create()
                        .texOffs(16, 8).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 5.0F)
                        .texOffs(36, 15).addBox(0.0F, 1.0F, 0.0F, 0.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        rear.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(32, 8).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 4.0F)
                        .texOffs(46, 15).addBox(0.0F, -1.0F, 0.0F, 0.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ElectricEel eel, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        // A wave runs down the body from head to tail, growing as it goes: slow and shallow at rest,
        // quicker and wider under way.
        float swim = Math.min(1.0F, limbSwingAmount * 2.0F);
        float phase = ageInTicks * (0.12F + 0.35F * swim) + eel.getId();
        float amplitude = 0.1F + 0.3F * swim;
        for (int i = 0; i < this.segments.length; ++i) {
            this.segments[i].yRot = Mth.sin(phase - i * 1.1F) * amplitude * (0.3F + 0.35F * i);
        }
        // The head holds its line against the wave, and follows where the eel is looking.
        this.head.yRot = -this.segments[0].yRot * 0.7F + netHeadYaw * Mth.DEG_TO_RAD * 0.3F;
        this.segments[0].xRot = headPitch * Mth.DEG_TO_RAD;
        int charge = eel.getCharge();
        if (charge > 0) {
            // Winding up: the whole length trembles, jaw agape.
            float build = Math.min(1.0F, charge / (float) ElectricEel.CHARGE_TICKS);
            for (int i = 0; i < this.segments.length; ++i) {
                this.segments[i].zRot = Mth.sin(ageInTicks * 2.7F + i * 1.9F) * 0.12F * build;
            }
            this.jaw.xRot = 0.35F + 0.2F * build;
        } else {
            this.jaw.xRot = Math.max(0.0F, Mth.sin(ageInTicks * 0.05F + eel.getId())) * 0.08F;
        }
    }
}
