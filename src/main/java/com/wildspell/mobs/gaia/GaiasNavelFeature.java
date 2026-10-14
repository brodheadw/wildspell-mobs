package com.wildspell.mobs.gaia;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.GaiaManifestation;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GaiasNavelFeature extends Feature<NoneFeatureConfiguration> {
    private static final int RADIUS = 19;
    private static final int MIN_Y = -5;
    private static final int MAX_Y = 25;
    private static final int QUIET = Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public GaiasNavelFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        // Live insertion is required: worldgen can defer an entity's cancellation until chunk load.
        if (!(context.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos origin = context.origin();
        BlockPos low = origin.offset(-RADIUS, MIN_Y, -RADIUS);
        BlockPos high = origin.offset(RADIUS, MAX_Y, RADIUS);
        AABB bounds = new AABB(Vec3.atLowerCornerOf(low), Vec3.atLowerCornerOf(high.offset(1, 1, 1)));
        if (level.isOutsideBuildHeight(low) || level.isOutsideBuildHeight(high)
                || !level.getWorldBorder().isWithinBounds(bounds)) {
            return false;
        }
        for (int x = low.getX() >> 4; x <= high.getX() >> 4; x++) {
            for (int z = low.getZ() >> 4; z <= high.getZ() >> 4; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null || !level.areEntitiesLoaded(ChunkPos.asLong(x, z))) {
                    return false;
                }
            }
        }
        if (!level.getEntities(null, bounds).isEmpty() || !terrain(level.getBlockState(origin))) {
            return false;
        }
        for (BlockPos pos : BlockPos.betweenClosed(low, high)) {
            BlockState state = level.getBlockState(pos);
            if (state.hasBlockEntity() || level.getBlockEntity(pos) != null
                    || !(state.isAir() || vegetation(state) || pos.getY() <= origin.getY() && terrain(state))) {
                return false;
            }
        }
        Map<BlockPos, BlockState> changes = new LinkedHashMap<>();
        Map<BlockPos, BlockState> before = new LinkedHashMap<>();
        plan().forEach((relative, state) -> {
            BlockPos pos = origin.offset(relative);
            BlockState old = level.getBlockState(pos);
            if (!old.equals(state)) {
                changes.put(pos, state);
                before.put(pos, old);
            }
        });
        GaiaManifestation manifestation = WildspellMobs.GAIA_MANIFESTATION.get().create(level);
        if (manifestation == null) {
            return false;
        }
        manifestation.anchorTo(origin.offset(0, 1, -5), 0.0F);
        if (!level.addFreshEntity(manifestation)) {
            return false;
        }
        boolean complete = false;
        try {
            for (var entry : changes.entrySet()) {
                level.setBlock(entry.getKey(), entry.getValue(), QUIET);
                if (!level.getBlockState(entry.getKey()).equals(entry.getValue())) {
                    return false;
                }
            }
            complete = true;
        } finally {
            if (!complete) {
                before.forEach((pos, state) -> level.setBlock(pos, state, QUIET));
                manifestation.discard();
            }
            before.forEach((pos, state) -> level.sendBlockUpdated(pos, state, level.getBlockState(pos), Block.UPDATE_CLIENTS));
        }
        return true;
    }

    private static boolean vegetation(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS)
                || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH)
                || state.is(Blocks.SNOW);
    }

    private static boolean terrain(BlockState state) {
        // Vanilla stores no provenance for terrain blocks, including player-placed dirt or stone.
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL) || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.STONE) || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF)
                || state.is(Blocks.CLAY);
    }

    /** Relative block plan, with the walking floor at y=0 and the entrance facing south. */
    public static Map<BlockPos, BlockState> plan() {
        Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
        BlockState root = Blocks.DARK_OAK_WOOD.defaultBlockState();
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int y = MIN_Y; y <= MAX_Y; y++) {
            for (int x = -15; x <= 15; x++) {
                for (int z = -15; z <= 15; z++) {
                    double radius = 15.0 + 0.4 * Math.sin(x * 0.7 + z * 0.5) * Math.sin(y * 0.6);
                    double distance = Math.sqrt(x * x + (y - 10) * (y - 10) + z * z);
                    if (distance > radius) {
                        continue;
                    }
                    int grain = Math.floorMod(x * 7349 + y * 9151 + z * 3911, 23);
                    BlockState state = (grain < 6 ? Blocks.ROOTED_DIRT : Blocks.DIRT).defaultBlockState();
                    if (distance > radius - 1.2) {
                        state = (y > 12 ? Blocks.GRASS_BLOCK : Blocks.MOSS_BLOCK).defaultBlockState();
                        if (grain < 5 && y > 2) {
                            state = leaves;
                        }
                    }
                    double crown = Math.max(0, y - 7) / 10.0;
                    double chamber = (x * x + z * z) / (8.5 * 8.5) + crown * crown;
                    if (y >= 1 && y <= 17 && chamber > 1.0 && chamber < 1.35) {
                        double patch = Math.sin(x * 0.58 + y * 0.3) + Math.cos(z * 0.66 - y * 0.31);
                        if (patch > 0.5) {
                            state = Blocks.MOSS_BLOCK.defaultBlockState();
                        } else if (patch < -1.0) {
                            state = leaves;
                        }
                    }
                    blocks.put(new BlockPos(x, y, z), state);
                }
            }
        }
        for (int rib = 0; rib < 9; rib++) {
            double bearing = rib * Math.PI * 2.0 / 9.0;
            for (int y = 0; y <= 24; y++) {
                double angle = bearing + 0.11 * Math.sin(y * 0.27 + rib);
                double radius = Math.sqrt(225 - (y - 10) * (y - 10));
                int x = (int) Math.round(Math.sin(angle) * radius);
                int z = (int) Math.round(Math.cos(angle) * radius);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) <= 1) {
                            blocks.put(new BlockPos(x + dx, y, z + dz), root);
                        }
                    }
                }
            }
            int previousX = 0;
            int previousZ = 0;
            for (int step = 8; step <= 18; step++) {
                double angle = bearing + 0.1 * Math.sin(step * 0.5 + rib);
                int x = (int) Math.round(Math.sin(angle) * step);
                int z = (int) Math.round(Math.cos(angle) * step);
                if (step == 8) {
                    previousX = x;
                    previousZ = z;
                }
                while (true) {
                    blocks.put(new BlockPos(previousX, 0, previousZ), root);
                    blocks.put(new BlockPos(previousX, -1, previousZ), Blocks.ROOTED_DIRT.defaultBlockState());
                    if (previousX != x) {
                        previousX += Integer.signum(x - previousX);
                    } else if (previousZ != z) {
                        previousZ += Integer.signum(z - previousZ);
                    } else {
                        break;
                    }
                }
                if (step < 14) {
                    blocks.put(new BlockPos(x + 1, 0, z), root);
                    blocks.put(new BlockPos(x, 0, z + 1), root);
                }
            }
        }
        double[] innerBearings = {0.9, 1.8, Math.PI, 4.4, 5.3};
        for (int rib = 0; rib < innerBearings.length; rib++) {
            for (int step = 0; step <= 68; step++) {
                double height = step / 4.0;
                double crown = Math.max(0, height - 7) / 10.0;
                double radius = 8.5 * Math.sqrt(1.0 - crown * crown) + 0.7;
                double angle = innerBearings[rib] + 0.075 * Math.sin(height * 0.4 + rib);
                int x = (int) Math.round(Math.sin(angle) * radius);
                int z = (int) Math.round(Math.cos(angle) * radius);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        blocks.put(new BlockPos(x + dx, (int) height, z + dz), root);
                        blocks.put(new BlockPos(x + dx, (int) height + 1, z + dz), root);
                    }
                }
            }
        }
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                double radial = (x * x + z * z) / (8.5 * 8.5);
                if (radial > 1) {
                    continue;
                }
                blocks.put(new BlockPos(x, 0, z), Blocks.MOSS_BLOCK.defaultBlockState());
                for (int y = 1; y <= 17; y++) {
                    double crown = Math.max(0, y - 7) / 10.0;
                    if (radial + crown * crown <= 1) {
                        blocks.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // A low dogleg conceals the chamber while keeping two blocks of headroom throughout.
        for (int z = 6; z <= 19; z++) {
            int center = z >= 11 && z <= 16 ? 2 : 0;
            for (int x = center - 1; x <= center + 1; x++) {
                blocks.put(new BlockPos(x, 0, z), Blocks.MOSS_BLOCK.defaultBlockState());
                for (int y = 1; y <= 3; y++) {
                    blocks.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = 3; x <= 5; x++) {
            for (int z = -3; z <= -1; z++) {
                for (int y = 12; y <= 25; y++) {
                    blocks.put(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int x = -6; x <= -2; x++) {
            for (int z = -3; z <= 1; z++) {
                if ((x + 4) * (x + 4) + (z + 1) * (z + 1) <= 4) {
                    blocks.put(new BlockPos(x, -1, z), Blocks.CLAY.defaultBlockState());
                    blocks.put(new BlockPos(x, 0, z), Blocks.WATER.defaultBlockState());
                }
            }
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -7; z <= -4; z++) {
                blocks.put(new BlockPos(x, 0, z), root);
                if (z == -7 || Math.abs(x) == 2) {
                    blocks.put(new BlockPos(x, 1, z), root);
                    if (z == -7) {
                        blocks.put(new BlockPos(x, 2, z), leaves);
                    }
                }
            }
        }
        blocks.put(new BlockPos(0, 0, -8), root);
        blocks.put(new BlockPos(0, 0, -9), root);
        blocks.put(new BlockPos(4, 1, 3), Blocks.FERN.defaultBlockState());
        blocks.put(new BlockPos(-6, 1, 3), Blocks.BROWN_MUSHROOM.defaultBlockState());
        return blocks;
    }
}
