package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.FrozenZombie;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Zombie model for a body half frozen solid: short, stiff, stop-motion strides with a laboured side
 * to side lean while it moves; while seized the whole pose (head included) freezes mid-stride and
 * only trembles. The one-armed variant hides its arm; the ice-bound one, sunk in ice to the hips,
 * keeps its legs straight and hunches forward, straining.
 */
public class FrozenZombieModel extends ZombieModel<FrozenZombie> {
    public static final ModelLayerLocation CRUST_LAYER = new ModelLayerLocation(WildspellMobs.id("frozen_zombie"), "crust");
    private static final float GAIT_STEP = 0.7F;
    private static final float STRIDE = 0.55F;

    public FrozenZombieModel(ModelPart root) {
        super(root);
    }

    /** The ice crust: the zombie mesh inflated slightly, like the drowned's outer layer. */
    public static LayerDefinition createCrustLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(0.3F), 0.0F), 64, 64);
    }

    @Override
    public void setupAnim(FrozenZombie zombie, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!zombie.isSeized()) {
            zombie.heldLimbSwing = Mth.floor(limbSwing / GAIT_STEP) * GAIT_STEP;
            zombie.heldLimbSwingAmount = limbSwingAmount * STRIDE;
            zombie.heldAgeInTicks = Mth.floor(ageInTicks / 4.0F) * 4.0F;
            zombie.heldHeadYaw = netHeadYaw;
            zombie.heldHeadPitch = headPitch;
        }
        super.setupAnim(zombie, zombie.heldLimbSwing, zombie.heldLimbSwingAmount, zombie.heldAgeInTicks, zombie.heldHeadYaw, zombie.heldHeadPitch);
        // Vanilla never resets roll on the head or body, so always assign these rather than add to them.
        float lean = Mth.sin(zombie.heldLimbSwing * 0.6662F) * 0.09F * Math.min(1.0F, zombie.heldLimbSwingAmount * 2.5F);
        this.body.zRot = lean;
        this.head.zRot = lean * 0.5F;
        if (zombie.isSeized()) {
            float tremble = ageInTicks * 2.9F;
            this.head.zRot += Mth.sin(tremble) * 0.03F;
            this.head.xRot += Mth.cos(tremble * 1.3F) * 0.02F;
            this.rightArm.zRot += Mth.sin(tremble * 1.7F) * 0.025F;
            this.leftArm.zRot -= Mth.sin(tremble * 1.5F) * 0.025F;
        }
        // Parts are shared by every zombie drawn with this model, so set visibility every frame.
        int variant = zombie.getVariant();
        this.leftArm.visible = variant != FrozenZombie.ONE_ARMED;
        if (variant == FrozenZombie.ICEBOUND) {
            this.rightLeg.xRot = 0.0F;
            this.leftLeg.xRot = 0.0F;
            this.rightLeg.yRot = 0.0F;
            this.leftLeg.yRot = 0.0F;
            this.body.xRot = 0.18F + 0.1F * Math.min(1.0F, zombie.heldLimbSwingAmount * 3.0F);
        }
        this.hat.copyFrom(this.head);
    }
}
