package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Mossback;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Prototype tortoise silhouette, tiered shell and little plants growing from its back. */
public class MossbackModel extends HierarchicalModel<Mossback> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("mossback"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[4];

    public MossbackModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < legs.length; i++) legs[i] = root.getChild("leg" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-13, -9, -18, 26, 11, 36), PartPose.offset(0, 16, 0));
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17, -14, -22, 34, 12, 44)
                .texOffs(0, 0).addBox(-14, -17, -19, 28, 3, 38), PartPose.offset(0, 12, 0));
        float[][] plants = {{-9, -10, 4}, {8, -8, 5}, {-3, 8, 5}, {11, 9, 3}, {3, -17, 4}};
        for (int i = 0; i < plants.length; i++) {
            float[] p = plants[i];
            shell.addOrReplaceChild("plant" + i, CubeListBuilder.create()
                    .texOffs(210, 12).addBox(-1, -p[2], -1, 2, p[2], 2),
                    PartPose.offset(p[0], -17, p[1]));
        }
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(158, 0).addBox(-6, -6, -11, 12, 9, 13)
                .texOffs(215, 0).addBox(-6.1F, -2, -9, 1, 2, 2)
                .texOffs(215, 0).addBox(5.1F, -2, -9, 1, 2, 2),
                PartPose.offset(0, 14, -20));
        float[][] positions = {{-13, -16}, {13, -16}, {-13, 16}, {13, 16}};
        for (int i = 0; i < positions.length; i++) {
            root.addOrReplaceChild("leg" + i, CubeListBuilder.create()
                    .texOffs(160, 30).addBox(-5, -2, -5, 10, 12, 10),
                    PartPose.offset(positions[i][0], 14, positions[i][1]));
        }
        return LayerDefinition.create(mesh, 256, 128);
    }

    @Override
    public ModelPart root() { return root; }

    @Override
    public void setupAnim(Mossback mossback, float limbSwing, float limbSwingAmount,
            float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = Mth.clamp(netHeadYaw, -25, 25) * Mth.DEG_TO_RAD;
        head.xRot = Mth.clamp(headPitch, -20, 20) * Mth.DEG_TO_RAD;
        for (int i = 0; i < legs.length; i++) {
            legs[i].xRot = Mth.cos(limbSwing * 0.48F + (i == 0 || i == 3 ? 0 : Mth.PI))
                    * 0.32F * limbSwingAmount;
        }
    }
}
