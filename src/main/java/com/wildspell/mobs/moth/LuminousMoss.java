package com.wildspell.mobs.moth;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public final class LuminousMoss {
    private static final int FADE_CHANCE = 4;

    private LuminousMoss() {
    }

    public static void brighten(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.MOSS_BLOCK)) {
            level.setBlockAndUpdate(pos, WildspellMobs.LUMINOUS_MOSS.get().defaultBlockState());
        } else if (state.is(Blocks.MOSS_CARPET)) {
            level.setBlockAndUpdate(pos, WildspellMobs.LUMINOUS_MOSS_CARPET.get().defaultBlockState());
        }
    }

    static void fade(ServerLevel level, BlockPos pos, RandomSource random, Block plain) {
        if (random.nextInt(FADE_CHANCE) == 0 && !LuminousMoth.mothNear(level, pos)) {
            level.setBlockAndUpdate(pos, plain.defaultBlockState());
        }
    }

    public static class MossBlock extends Block {
        public MossBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            fade(level, pos, random, Blocks.MOSS_BLOCK);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return new ItemStack(Blocks.MOSS_BLOCK);
        }
    }

    public static class Carpet extends CarpetBlock {
        public Carpet(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            fade(level, pos, random, Blocks.MOSS_CARPET);
        }

        @Override
        public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
            return new ItemStack(Blocks.MOSS_CARPET);
        }
    }
}
