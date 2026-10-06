package com.wildspell.mobs;

import com.wildspell.mobs.entity.ColdEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class IceMelting {
    public static final TagKey<Block> MELTS_NEAR_HEAT = TagKey.create(Registries.BLOCK, WildspellMobs.id("melts_near_heat"));
    public static final TagKey<Block> MELTS_ICE = TagKey.create(Registries.BLOCK, WildspellMobs.id("melts_ice"));
    static final int SAMPLES = 24;
    static final int REACH = 24;
    static final int REACH_Y = 12;

    private IceMelting() {
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        Player player = event.getEntity();
        if (!level.getBiome(player.blockPosition()).is(ColdEffects.COLD_CAVES)) {
            return;
        }
        sampleAround(level, player.blockPosition(), SAMPLES);
    }

    public static int sampleAround(ServerLevel level, BlockPos center, int samples) {
        return sampleAround(level, center, samples, REACH, REACH_Y);
    }

    public static int sampleAround(ServerLevel level, BlockPos center, int samples, int reach, int reachY) {
        int melted = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < samples; ++i) {
            pos.set(center.getX() + level.random.nextInt(2 * reach + 1) - reach,
                    center.getY() + level.random.nextInt(2 * reachY + 1) - reachY,
                    center.getZ() + level.random.nextInt(2 * reach + 1) - reach);
            if (level.isLoaded(pos) && melt(level, pos)) {
                ++melted;
            }
        }
        return melted;
    }

    public static boolean melt(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(MELTS_NEAR_HEAT) || !isHeated(level, pos)) {
            return false;
        }
        boolean waterlogged = state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
        level.setBlockAndUpdate(pos, waterlogged ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState());
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.25, 0.25, 0.25, 0.02);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1.6F + level.random.nextFloat() * 0.4F);
        return true;
    }

    static boolean isHeated(ServerLevel level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            if (level.getBlockState(pos.relative(side)).is(MELTS_ICE)) {
                return true;
            }
        }
        return level.getBlockState(pos.below(2)).is(MELTS_ICE);
    }
}
