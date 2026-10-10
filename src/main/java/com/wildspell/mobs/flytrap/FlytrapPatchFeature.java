package com.wildspell.mobs.flytrap;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class FlytrapPatchFeature extends Feature<NoneFeatureConfiguration> {
    private static final int TRIES = 8;
    private static final int SPREAD = 3;
    private static final int MAX_PLANTS = 4;
    public static final TagKey<Block> THICKET = TagKey.create(Registries.BLOCK, WildspellMobs.id("flytrap_thicket"));
    private static final int THICKET_RADIUS = 3;
    private static final int THICKET_HEIGHT = 3;
    private static final int THICKET_DENSITY = 8;

    public FlytrapPatchFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        FlytrapBlock block = WildspellMobs.FLYTRAP.get();
        int placed = 0;
        for (int i = 0; i < TRIES && placed < MAX_PLANTS; ++i) {
            BlockState state = block.defaultBlockState();
            BlockPos pos = this.findGround(level, state, origin.offset(random.nextInt(SPREAD * 2 + 1) - SPREAD, 0, random.nextInt(SPREAD * 2 + 1) - SPREAD));
            if (pos == null) {
                continue;
            }
            int roll = random.nextInt(10);
            int age = roll < 4 ? 0 : roll < 8 ? 1 : 2;
            state = state.setValue(FlytrapBlock.AGE, age);
            if (age == FlytrapBlock.MAX_AGE && !FlytrapBlock.canGrow(level, pos, state.setValue(FlytrapBlock.AGE, age - 1))) {
                state = state.setValue(FlytrapBlock.AGE, age - 1);
            }
            level.setBlock(pos, state, Block.UPDATE_CLIENTS);
            if (state.getValue(FlytrapBlock.AGE) == FlytrapBlock.MAX_AGE) {
                FlytrapBlock.buildStem(level, pos, Block.UPDATE_CLIENTS);
            }
            level.scheduleTick(pos, block, 1);
            ++placed;
        }
        return placed > 0;
    }

    @Nullable
    private BlockPos findGround(WorldGenLevel level, BlockState state, BlockPos start) {
        for (int dy = 2; dy >= -2; --dy) {
            BlockPos pos = start.above(dy);
            if (level.getBlockState(pos).isAir() && state.canSurvive(level, pos) && isShadyThicket(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    public static boolean isShadyThicket(LevelReader level, BlockPos pos) {
        if (pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ())) {
            return false;
        }
        int foliage = 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-THICKET_RADIUS, -1, -THICKET_RADIUS),
                pos.offset(THICKET_RADIUS, THICKET_HEIGHT, THICKET_RADIUS))) {
            if (level.getBlockState(near).is(THICKET) && ++foliage >= THICKET_DENSITY) {
                return true;
            }
        }
        return false;
    }
}
