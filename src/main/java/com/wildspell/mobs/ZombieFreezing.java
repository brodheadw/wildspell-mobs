//? if <26.4 {
package com.wildspell.mobs;

import com.wildspell.mobs.crypt.LichSouls;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.FrozenZombie;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public final class ZombieFreezing {
    static final int SHIVER_AT = 60;
    static final int CONVERT_AT = 140;
    static final int CHECK_INTERVAL = 10;

    private static final String CHILL = WildspellMobs.MODID + ":chill";

    private ZombieFreezing() {
    }

    static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity().getType() != EntityType.ZOMBIE || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Zombie zombie = (Zombie) event.getEntity();
        if ((zombie.tickCount + zombie.getId()) % CHECK_INTERVAL != 0) {
            return;
        }
        CompoundTag data = zombie.getPersistentData();
        int chill = data.getInt(CHILL);
        if (zombie.isOnFire()) {
            if (chill > 0) {
                data.remove(CHILL);
            }
            return;
        }
        if (!level.getBiome(zombie.blockPosition()).is(ColdEffects.COLD_CAVES) || LichSouls.isCleansedZone(level, zombie.blockPosition())) {
            if (chill > 0) {
                chill -= CHECK_INTERVAL;
                if (chill <= 0) {
                    data.remove(CHILL);
                } else {
                    data.putInt(CHILL, chill);
                }
            }
            return;
        }
        chill += CHECK_INTERVAL;
        data.putInt(CHILL, chill);
        if (chill < SHIVER_AT) {
            return;
        }
        zombie.setTicksFrozen(zombie.getTicksRequiredToFreeze() + 2 * CHECK_INTERVAL);
        if (chill < SHIVER_AT + CHECK_INTERVAL) {
            zombie.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.8F, 0.8F);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, zombie.getX(), zombie.getY(0.6), zombie.getZ(), 8, 0.3, 0.5, 0.3, 0.01);
        if (chill >= CONVERT_AT && EventHooks.canLivingConvert(zombie, WildspellMobs.FROZEN_ZOMBIE.get(), ticks -> data.putInt(CHILL, CONVERT_AT - ticks))) {
            FrozenZombie frozen = zombie.convertTo(WildspellMobs.FROZEN_ZOMBIE.get(), true);
            if (frozen != null) {
                frozen.pickVariant();
                frozen.getPersistentData().remove(CHILL);
                frozen.playSound(WildspellMobs.ICE_SHATTER.get(), 0.8F, 1.2F);
                level.sendParticles(ColdEffects.ICE_CHIPS, frozen.getX(), frozen.getY(0.5), frozen.getZ(), 30, 0.3, 0.7, 0.3, 0.15);
                EventHooks.onLivingConvert(zombie, frozen);
            }
        }
    }
}
//?}
