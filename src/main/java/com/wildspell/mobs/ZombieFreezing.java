package com.wildspell.mobs;

import com.wildspell.mobs.entity.FrozenZombie;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Zombies that linger in a freezing biome (the Frosted Caves) turn into Frozen Zombies, the way
 * skeletons in powder snow turn into strays: after a few seconds they start to shiver and frost
 * over, then crack into their frozen form. Leaving the biome thaws the progress away.
 *
 * <p>Zombies are checked every {@link #CHECK_INTERVAL} ticks (staggered by entity id) rather than
 * every tick, so a crowd of zombies costs a tenth of the biome lookups and save-data writes.
 */
final class ZombieFreezing {
    static final TagKey<Biome> FREEZES_ZOMBIES = TagKey.create(Registries.BIOME, WildspellMobs.id("freezes_zombies"));
    static final int SHIVER_AT = 60;
    static final int CONVERT_AT = 140;
    static final int CHECK_INTERVAL = 10;

    private static final String CHILL = WildspellMobs.MODID + ":chill";
    private static final BlockParticleOption ICE_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

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
        if (!level.getBiome(zombie.blockPosition()).is(FREEZES_ZOMBIES)) {
            if (chill > 0) {
                data.remove(CHILL);
            }
            return;
        }
        chill += CHECK_INTERVAL;
        data.putInt(CHILL, chill);
        if (chill < SHIVER_AT) {
            return;
        }
        // Held fully frozen, the vanilla renderer shivers it and frost creeps over it. Frost thaws
        // by 2 a tick, so top it up with enough slack to last until the next check.
        zombie.setTicksFrozen(zombie.getTicksRequiredToFreeze() + 2 * CHECK_INTERVAL);
        if (chill < SHIVER_AT + CHECK_INTERVAL) {
            zombie.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.8F, 0.8F);
        }
        level.sendParticles(ParticleTypes.SNOWFLAKE, zombie.getX(), zombie.getY(0.6), zombie.getZ(), 8, 0.3, 0.5, 0.3, 0.01);
        if (chill >= CONVERT_AT && EventHooks.canLivingConvert(zombie, WildspellMobs.FROZEN_ZOMBIE.get(), ticks -> data.putInt(CHILL, CONVERT_AT - ticks))) {
            FrozenZombie frozen = zombie.convertTo(WildspellMobs.FROZEN_ZOMBIE.get(), true);
            if (frozen != null) {
                // Sometimes an arm shatters off as it freezes; on ice, its legs freeze into the ice.
                frozen.pickVariant();
                frozen.getPersistentData().remove(CHILL);
                frozen.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 0.8F, 1.2F);
                level.sendParticles(ICE_CHIPS, frozen.getX(), frozen.getY(0.5), frozen.getZ(), 30, 0.3, 0.7, 0.3, 0.15);
                EventHooks.onLivingConvert(zombie, frozen);
            }
        }
    }
}
