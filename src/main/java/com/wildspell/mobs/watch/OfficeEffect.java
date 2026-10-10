package com.wildspell.mobs.watch;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class OfficeEffect extends MobEffect {
    public OfficeEffect(int color) {
        super(MobEffectCategory.HARMFUL, color);
    }
}
