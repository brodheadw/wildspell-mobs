package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public final class GlareOverlay implements LayeredDraw.Layer {
    public static final int FADE = 60;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker delta) {
        Player player = Minecraft.getInstance().player;
        MobEffectInstance glare = player == null ? null : player.getEffect(WildspellMobs.GLARE);
        if (glare == null) {
            return;
        }
        float strength = Mth.clamp(glare.getDuration() / (float) FADE, 0.0F, 1.0F);
        int alpha = (int) (235 * strength);
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24 | 0xFFF8E6);
    }
}
