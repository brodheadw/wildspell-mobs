package com.wildspell.mobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.FrozenZombie;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.AbstractZombieRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Zombie;

public class FrozenZombieRenderer extends AbstractZombieRenderer<FrozenZombie, FrozenZombieModel> {
    private static final ResourceLocation SKIN = WildspellMobs.id("textures/entity/frozen_zombie.png");
    private static final ResourceLocation WHOLE_SKIN = WildspellMobs.id("textures/entity/frozen_zombie_whole.png");
    private static final RenderType EYES = RenderType.eyes(WildspellMobs.id("textures/entity/frozen_zombie_eyes.png"));
    private static final ResourceLocation CRUST = WildspellMobs.id("textures/entity/frozen_zombie_crust.png");

    public FrozenZombieRenderer(EntityRendererProvider.Context context) {
        super(context,
                new FrozenZombieModel(context.bakeLayer(ModelLayers.ZOMBIE)),
                new FrozenZombieModel(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new FrozenZombieModel(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)));
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return EYES;
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FrozenZombie zombie,
                    float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
                if (!zombie.hasWholeFace()) {
                    super.render(poseStack, buffer, packedLight, zombie, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
                }
            }
        });
        this.addLayer(new CrustLayer(this, new FrozenZombieModel(context.bakeLayer(FrozenZombieModel.CRUST_LAYER))));
    }

    @Override
    public ResourceLocation getTextureLocation(Zombie zombie) {
        return zombie instanceof FrozenZombie frozen && frozen.hasWholeFace() ? WHOLE_SKIN : SKIN;
    }

    private static class CrustLayer extends RenderLayer<FrozenZombie, FrozenZombieModel> {
        private final FrozenZombieModel crust;

        CrustLayer(RenderLayerParent<FrozenZombie, FrozenZombieModel> parent, FrozenZombieModel crust) {
            super(parent);
            this.crust = crust;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, FrozenZombie zombie,
                float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            coloredCutoutModelCopyLayerRender(this.getParentModel(), this.crust, CRUST, poseStack, buffer, packedLight, zombie,
                    limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTick, -1);
        }
    }
}
