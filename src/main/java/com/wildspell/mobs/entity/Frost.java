package com.wildspell.mobs.entity;

import net.minecraft.world.entity.LivingEntity;

public final class Frost {
    private Frost() {
    }

    public static void add(LivingEntity target, int ticks, int slack) {
        if (!target.canFreeze()) {
            return;
        }
        int cap = target.getTicksRequiredToFreeze() + slack;
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), Math.min(cap, target.getTicksFrozen() + ticks)));
    }

    public static void freezeSolid(LivingEntity target, int slack) {
        if (target.canFreeze()) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + slack));
        }
    }
}
