package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Mossback;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MossbackRenderer extends MobRenderer<Mossback, MossbackModel> {
    private static final ResourceLocation TEXTURE = WildspellMobs.id("textures/entity/mossback.png");

    public MossbackRenderer(EntityRendererProvider.Context context) {
        super(context, new MossbackModel(context.bakeLayer(MossbackModel.LAYER)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(Mossback mossback) { return TEXTURE; }
}
