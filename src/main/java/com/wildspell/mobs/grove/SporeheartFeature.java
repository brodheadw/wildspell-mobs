package com.wildspell.mobs.grove;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class SporeheartFeature extends Feature<NoneFeatureConfiguration> {
    public static final int TRIES = 48;
    public static final int SPREAD = 7;
    public static final int MIN_STEM = 6;

    public SporeheartFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static boolean soil(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.NYLIUM);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return plant(context.level(), context.origin(), context.random()) != null;
    }

    @Nullable
    public static BlockPos plant(WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (int i = 0; i < TRIES; ++i) {
            int x = origin.getX() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            int z = origin.getZ() + random.nextInt(SPREAD * 2 + 1) - SPREAD;
            BlockPos base = stemBase(level, x, z);
            if (base != null) {
                BlockPos heart = base.above();
                level.setBlock(heart, WildspellMobs.SPOREHEART.get().defaultBlockState(), 2);
                return heart;
            }
        }
        return null;
    }

    @Nullable
    public static BlockPos stemBase(WorldGenLevel level, int x, int z) {
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        int run = 0;
        for (int y = top; y > level.getMinBuildHeight() && y > top - 40; --y) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.MUSHROOM_STEM)) {
                ++run;
                if (soil(level.getBlockState(pos.below()))) {
                    return run >= MIN_STEM ? pos : null;
                }
            } else if (run > 0) {
                return null;
            }
        }
        return null;
    }
}
