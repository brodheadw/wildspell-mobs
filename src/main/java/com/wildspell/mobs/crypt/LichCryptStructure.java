package com.wildspell.mobs.crypt;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import java.util.Optional;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public class LichCryptStructure extends Structure {
    public static final MapCodec<LichCryptStructure> CODEC = simpleCodec(LichCryptStructure::new);
    private static final int PROBES = 24;
    private static final int MIN_Y = -48;
    private static final int MAX_Y = 32;
    private static final double MIN_BURIED = 0.8;
    private static final int[][] HALL_SAMPLES = {{12, 12}, {1, 1}, {23, 1}, {1, 23}, {23, 23}, {12, 1}, {12, 23}, {1, 12}, {23, 12}};

    public LichCryptStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        WorldgenRandom random = context.random();
        int baseX = context.chunkPos().getMiddleBlockX();
        int baseZ = context.chunkPos().getMiddleBlockZ();
        for (int probe = 0; probe < PROBES; ++probe) {
            int x = baseX + random.nextInt(65) - 32;
            int z = baseZ + random.nextInt(65) - 32;
            Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            LichCryptPiece layout = new LichCryptPiece(new BlockPos(x, 0, z), facing, LichCryptPiece.MAX_TUNNEL);
            BlockPos center = layout.localToWorld(12, 0, 12);
            int[] heights = this.biomeHeights(context, center);
            if (heights.length == 0) {
                continue;
            }
            NoiseColumn[] hall = new NoiseColumn[HALL_SAMPLES.length];
            for (int i = 0; i < hall.length; ++i) {
                BlockPos p = layout.localToWorld(HALL_SAMPLES[i][0], 0, HALL_SAMPLES[i][1]);
                hall[i] = context.chunkGenerator().getBaseColumn(p.getX(), p.getZ(), context.heightAccessor(), context.randomState());
            }
            NoiseColumn[] path = new NoiseColumn[LichCryptPiece.MAX_TUNNEL + LichCryptPiece.MOUTH_DEPTH];
            for (int t = 0; t < path.length; ++t) {
                BlockPos p = layout.localToWorld(12, 0, LichCryptPiece.WIDTH + t);
                path[t] = context.chunkGenerator().getBaseColumn(p.getX(), p.getZ(), context.heightAccessor(), context.randomState());
            }
            for (int y : heights) {
                if (!buried(hall, y)) {
                    continue;
                }
                int tunnel = tunnelToCave(path, y);
                if (tunnel < 0) {
                    continue;
                }
                BlockPos origin = new BlockPos(x, y, z);
                return Optional.of(new GenerationStub(center.atY(y + 2), pieces -> pieces.addPiece(new LichCryptPiece(origin, facing, tunnel))));
            }
        }
        return Optional.empty();
    }

    private int[] biomeHeights(GenerationContext context, BlockPos center) {
        return IntStream.rangeClosed(MIN_Y, MAX_Y)
                .filter(y -> context.validBiome().test(context.biomeSource().getNoiseBiome(QuartPos.fromBlock(center.getX()),
                        QuartPos.fromBlock(y + 2), QuartPos.fromBlock(center.getZ()), context.randomState().sampler())))
                .toArray();
    }

    private static boolean buried(NoiseColumn[] hall, int y) {
        int rock = 0;
        int total = 0;
        for (NoiseColumn column : hall) {
            for (int dy = 1; dy < LichCryptPiece.HEIGHT; ++dy) {
                BlockState state = column.getBlock(y + dy);
                if (!state.getFluidState().isEmpty()) {
                    return false;
                }
                rock += state.isAir() ? 0 : 1;
                ++total;
            }
        }
        return rock >= total * MIN_BURIED;
    }

    private static int tunnelToCave(NoiseColumn[] path, int y) {
        for (int t = LichCryptPiece.MIN_TUNNEL; t <= LichCryptPiece.MAX_TUNNEL; ++t) {
            boolean open = true;
            for (int d = 0; d < LichCryptPiece.MOUTH_DEPTH && open; ++d) {
                NoiseColumn column = path[t + d];
                open = isAir(column, y + 2) && isAir(column, y + 3) && isAir(column, y + 4);
            }
            if (open && (isSolid(path[t], y + 1) || isSolid(path[t], y))) {
                return t;
            }
        }
        return -1;
    }

    private static boolean isAir(NoiseColumn column, int y) {
        return column.getBlock(y).isAir();
    }

    private static boolean isSolid(NoiseColumn column, int y) {
        BlockState state = column.getBlock(y);
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    @Override
    public StructureType<?> type() {
        return WildspellMobs.LICH_CRYPT.get();
    }
}
