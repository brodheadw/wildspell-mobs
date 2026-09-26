package com.wildspell.mobs.entity;

import net.minecraft.world.entity.LivingEntity;

/**
 * Frost from the mod's cold attacks, built on vanilla's powder-snow freezing. As with powder snow,
 * anything wearing a freeze-immune piece (any leather armour) shrugs it off: without this check the
 * frost overlay and its slow would land on leather wearers too, since only the freeze damage tick
 * checks {@code canFreeze()}.
 */
public final class Frost {
    private Frost() {
    }

    /** Adds frost that builds toward fully frozen, capped {@code slack} ticks past it. Frost thaws by 2 a tick. */
    public static void add(LivingEntity target, int ticks, int slack) {
        if (!target.canFreeze()) {
            return;
        }
        int cap = target.getTicksRequiredToFreeze() + slack;
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), Math.min(cap, target.getTicksFrozen() + ticks)));
    }

    /** Freezes the target solid, held {@code slack} ticks past fully frozen. */
    public static void freezeSolid(LivingEntity target, int slack) {
        if (target.canFreeze()) {
            target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + slack));
        }
    }
}
