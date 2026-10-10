package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Watcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class WatcherRenderer extends GeoEntityRenderer<Watcher> {
    public WatcherRenderer(EntityRendererProvider.Context context, String face) {
        super(context, new Model(face));
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(Watcher watcher) {
        return 0.0F;
    }

    private static class Model extends DefaultedEntityGeoModel<Watcher> {
        private static final float SHIFT = 0.55F;

        Model(String face) {
            super(WildspellMobs.id("watcher_" + face), false);
        }

        @Override
        public void setCustomAnimations(Watcher watcher, long instanceId, AnimationState<Watcher> state) {
            super.setCustomAnimations(watcher, instanceId, state);
            Entity gaze = watcher.gaze();
            float across = 0.0F;
            float up = 0.0F;
            boolean lost = watcher.state() == Watcher.LOST || gaze == null;
            if (!lost) {
                float partial = state.getPartialTick();
                Vec3 to = gaze.getEyePosition(partial).subtract(watcher.getEyePosition(partial)).normalize();
                float yaw = Mth.rotLerp(partial, watcher.yBodyRotO, watcher.yBodyRot) * Mth.DEG_TO_RAD;
                across = (float) (to.x * -Mth.cos(yaw) + to.z * -Mth.sin(yaw));
                up = (float) to.y;
            }
            float time = (watcher.tickCount + state.getPartialTick()) * 0.15F;
            int index = 0;
            for (GeoBone bone : this.getAnimationProcessor().getRegisteredBones()) {
                String name = bone.getName();
                if (!name.startsWith("pupil_")) {
                    continue;
                }
                float a = across;
                float b = up;
                if (lost) {
                    a = Mth.sin(time + index * 1.7F);
                    b = Mth.cos(time * 0.8F + index * 2.3F) * 0.6F;
                }
                ++index;
                boolean flat = name.contains("_arm_") || name.contains("_hand_");
                float sideways = Mth.clamp(a, -1.0F, 1.0F) * SHIFT;
                float lift = Mth.clamp(b, -1.0F, 1.0F) * SHIFT;
                if (flat) {
                    bone.setPosX(-sideways);
                } else {
                    bone.setPosZ(name.contains("_right_") ? sideways : -sideways);
                }
                bone.setPosY(lift);
            }
        }
    }
}
