package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.RimeSkull;
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
 * A 9x8x8 skull with a hinged jaw and a crown of short icicles; bobs, chatters and gnashes while it
 * floats. Each skull variant shows its own subset of the crown.
 */
public class RimeSkullModel extends HierarchicalModel<RimeSkull> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("rime_skull"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] spikes = new ModelPart[SPIKES.length];

    // Crown icicles as {x, z, height}, sitting on top of the skull.
    private static final float[][] SPIKES = {{-3.0F, -1.0F, 2.0F}, {-0.5F, -2.5F, 3.0F}, {2.0F, 0.5F, 1.0F}, {0.5F, 2.0F, 2.0F}, {-4.0F, 2.0F, 1.0F}, {3.0F, -2.5F, 2.0F}};
    // Which icicles each variant wears.
    private static final boolean[][] CROWNS = {
            {true, true, true, true, false, false},
            {false, true, false, true, true, true},
            {true, false, true, false, true, true}};

    public RimeSkullModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < SPIKES.length; ++i) {
            this.spikes[i] = this.head.getChild("spike" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();
        PartDefinition head = parts.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -8.0F, -4.0F, 9.0F, 8.0F, 8.0F),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        for (int i = 0; i < SPIKES.length; ++i) {
            float[] spike = SPIKES[i];
            head.addOrReplaceChild("spike" + i, CubeListBuilder.create()
                    .texOffs(40, 0).addBox(spike[0], -8.0F - spike[2], spike[1], 1.0F, spike[2], 1.0F), PartPose.ZERO);
        }
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.0F, 0.0F, -7.0F, 8.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(RimeSkull skull, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        boolean[] crown = CROWNS[skull.getVariant()];
        for (int i = 0; i < this.spikes.length; ++i) {
            this.spikes[i].visible = crown[i];
        }
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.y += Mth.sin(ageInTicks * 0.15F) * 1.2F;
        this.head.zRot = Mth.sin(ageInTicks * 0.09F) * 0.08F;
        float gnash = skull.gnashOpenness(ageInTicks - skull.tickCount);
        if (gnash >= 0.0F) {
            this.jaw.xRot = gnash * 0.8F;
        } else if (skull.isCharging()) {
            this.jaw.xRot = 0.75F;
        } else {
            this.jaw.xRot = 0.08F + Mth.sin(ageInTicks * 0.6F) * 0.06F;
        }
    }
}
