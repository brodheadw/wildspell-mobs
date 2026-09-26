package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Mossback;
import com.wildspell.mobs.entity.MossbackGrowth;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Progressive vegetation; the oldest shell becomes a miniature, block-sized garden. */
public class MossbackModel extends HierarchicalModel<Mossback> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("mossback"), "main");
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] plants = new ModelPart[5];
    private final ModelPart[] gardenBlocks = new ModelPart[3];
    private final ModelPart[] whiskers = new ModelPart[2];

    public MossbackModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < whiskers.length; i++) whiskers[i] = head.getChild("whisker" + i);
        ModelPart shell = root.getChild("shell");
        for (int i = 0; i < legs.length; i++) legs[i] = root.getChild("leg" + i);
        for (int i = 0; i < plants.length; i++) plants[i] = shell.getChild("plant" + i);
        for (int i = 0; i < gardenBlocks.length; i++) gardenBlocks[i] = shell.getChild("garden" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 60).addBox(-13, -9, -18, 26, 11, 36), PartPose.offset(0, 16, 0));
        PartDefinition shell = root.addOrReplaceChild("shell", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-17, -14, -22, 34, 12, 44)
                .texOffs(0, 0).addBox(-14, -17, -19, 28, 3, 38), PartPose.offset(0, 12, 0));
        float[][] p = {{-9, -10, 4}, {8, -8, 5}, {-3, 8, 5}, {11, 9, 3}, {3, -17, 4}};
        for (int i = 0; i < p.length; i++) {
            shell.addOrReplaceChild("plant" + i, CubeListBuilder.create()
                    .texOffs(210, 12).addBox(-1, -p[i][2], -1, 2, p[i][2], 2),
                    PartPose.offset(p[i][0], -17, p[i][1]));
        }
        // 6.5 model pixels scale into approximately one full Minecraft block at final size (2.46x).
        float[][] garden = {{-6, -9}, {6, 1}, {-4, 10}};
        for (int i = 0; i < garden.length; i++) {
            shell.addOrReplaceChild("garden" + i, CubeListBuilder.create()
                    .texOffs(20, 82).addBox(-3.25F, -6.5F, -3.25F, 6.5F, 6.5F, 6.5F),
                    PartPose.offset(garden[i][0], -17, garden[i][1]));
        }
        // A long, angular sea-stone face rather than a vanilla turtle's square beak.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(144, 0).addBox(-7.5F, -7.0F, -12.0F, 15.0F, 10.0F, 15.0F)
                .texOffs(144, 27).addBox(-6.0F, -4.0F, -16.0F, 12.0F, 5.0F, 7.0F)
                .texOffs(144, 42).addBox(-5.0F, 0.0F, -17.0F, 10.0F, 2.0F, 5.0F),
                PartPose.offset(0, 14, -20));
        // Deep brow plates frame two vertical crystal slits. No round pupils or googly eyes.
        head.addOrReplaceChild("brow_left", CubeListBuilder.create()
                .texOffs(191, 0).addBox(-7.8F, -6.5F, -13.0F, 5.0F, 2.5F, 5.0F), PartPose.ZERO);
        head.addOrReplaceChild("brow_right", CubeListBuilder.create()
                .texOffs(191, 0).addBox(2.8F, -6.5F, -13.0F, 5.0F, 2.5F, 5.0F), PartPose.ZERO);
        head.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(220, 0).addBox(-5.6F, -4.5F, -12.15F, 1.7F, 3.0F, 0.4F), PartPose.ZERO);
        head.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(220, 0).addBox(3.9F, -4.5F, -12.15F, 1.7F, 3.0F, 0.4F), PartPose.ZERO);
        head.addOrReplaceChild("whisker0", CubeListBuilder.create()
                .texOffs(192, 12).addBox(-8.0F, -1.0F, -13.0F, 3.0F, 1.0F, 9.0F), PartPose.ZERO);
        head.addOrReplaceChild("whisker1", CubeListBuilder.create()
                .texOffs(192, 12).addBox(5.0F, -1.0F, -13.0F, 3.0F, 1.0F, 9.0F), PartPose.ZERO);
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
        for (ModelPart whisker : whiskers) whisker.visible = mossback.lifeStage() >= MossbackGrowth.MATURE;
        for (int i = 0; i < legs.length; i++) {
            legs[i].xRot = Mth.cos(limbSwing * 0.48F + (i == 0 || i == 3 ? 0 : Mth.PI))
                    * 0.32F * limbSwingAmount;
        }
        int stage = mossback.lifeStage();
        for (int i = 0; i < plants.length; i++) plants[i].visible =
                stage >= MossbackGrowth.MATURE || (stage == MossbackGrowth.JUVENILE && i < 2);
        for (ModelPart part : gardenBlocks) part.visible = stage == MossbackGrowth.ANCIENT;
    }
}
