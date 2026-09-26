package com.wildspell.mobs.crypt;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * The Ice Lich's crypt, sunk somewhere in the Frosted Caves. The Frosted Caves are a cave biome, so
 * the structure probes a handful of points around and below the chunk for one inside the biome
 * (the structure's biome tag) and builds the crypt there.
 */
public class LichCryptStructure extends Structure {
    public static final MapCodec<LichCryptStructure> CODEC = simpleCodec(LichCryptStructure::new);
    private static final int PROBES = 32;
    private static final int MIN_Y = -48;
    private static final int MAX_Y = 32;

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
            int y = MIN_Y + random.nextInt(MAX_Y - MIN_Y + 1);
            var biome = context.biomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), context.randomState().sampler());
            if (context.validBiome().test(biome)) {
                Direction facing = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos at = new BlockPos(x, y, z);
                return Optional.of(new GenerationStub(at, pieces -> pieces.addPiece(new LichCryptPiece(at, facing))));
            }
        }
        return Optional.empty();
    }

    @Override
    public StructureType<?> type() {
        return WildspellMobs.LICH_CRYPT.get();
    }
}
