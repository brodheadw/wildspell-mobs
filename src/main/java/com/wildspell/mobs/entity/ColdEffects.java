package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Effects and world checks shared by the cold mobs, the lich and its phylactery. */
public final class ColdEffects {
    /** Shards of ice flying off something that shatters or cracks. */
    public static final BlockParticleOption ICE_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

    private ColdEffects() {
    }

    /**
     * True if the {@code height} blocks from {@code pos} upward are loaded and empty. Checking
     * loadedness first means a spot search never forces a chunk to load on the server thread.
     */
    public static boolean isOpen(Level level, BlockPos pos, int height) {
        for (int dy = 0; dy < height; ++dy) {
            BlockPos p = pos.above(dy);
            if (!level.isLoaded(p) || !level.isEmptyBlock(p)) {
                return false;
            }
        }
        return true;
    }

    /** True if nothing solid lies on the straight line between two points (flyers steer in straight lines). */
    public static boolean clearPath(Entity mover, Vec3 from, Vec3 to) {
        return mover.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mover)).getType() == HitResult.Type.MISS;
    }

    public static Vec3 ringPoint(RandomSource random, Vec3 center, double minRadius, double spread, double rise) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = minRadius + random.nextDouble() * spread;
        return new Vec3(center.x + Math.cos(angle) * radius, center.y + rise, center.z + Math.sin(angle) * radius);
    }

    @Nullable
    public static Vec3 findSpot(int attempts, Supplier<Vec3> candidate, Predicate<Vec3> accept) {
        for (int attempt = 0; attempt < attempts; ++attempt) {
            Vec3 spot = candidate.get();
            if (accept.test(spot)) {
                return spot;
            }
        }
        return null;
    }

    public static void shatter(ServerLevel level, Mob mob, float pitch) {
        level.sendParticles(ICE_CHIPS, mob.getX(), mob.getY(0.5), mob.getZ(), 25, 0.3, 0.6, 0.3, 0.15);
        mob.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 1.0F, pitch);
        mob.discard();
    }

    /** Souls and frost billowing up: the lich rising, fleeing or sinking away. */
    public static void soulBurst(ServerLevel level, Vec3 at, double width, double height) {
        level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 40, width, height, width, 0.05);
        level.sendParticles(WildspellMobs.FROST_MOTE.get(), at.x, at.y, at.z, 30, width, height, width, 0.05);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 20, width, height, width, 0.03);
    }

    /** Shows {@code message} above the hotbar of every player in {@code area}. */
    public static void tellNearby(ServerLevel level, AABB area, Component message) {
        for (Player player : level.getEntitiesOfClass(Player.class, area)) {
            player.displayClientMessage(message, true);
        }
    }
}
