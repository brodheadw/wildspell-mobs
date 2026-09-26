package com.wildspell.mobs.crypt;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * The crypt, built block by block. In local coordinates: a 25x25 deepslate hall (x, z 0-24) with its
 * floor at y 1 and roof at y 11, four pillars to hide behind, the altar and phylactery against the
 * back wall (z 0-5), a Rime Ward on a plinth in each corner, eight unlit soul-fire braziers and twelve
 * clusters of unlit blue candles (on the dais and along the walls) around the
 * walls, and a tunnel out of the front wall (from z 25), long enough to break into the cave the crypt
 * was dug beside (see {@link LichCryptStructure}); its mouth is framed in chiseled deepslate.
 *
 * <p>No floor block is ice: Frozen Zombies raised on ice would freeze into it. The phylactery faces
 * local +z, into the hall. In a piece's local frame +z is NORTH (with a NORTH orientation, world z
 * runs backwards from local z, and the piece rotates and mirrors block states to match), so it's
 * placed facing NORTH and comes out facing the hall whichever way the crypt is turned; then
 * {@link PhylacteryBlockEntity} finds the room {@value PhylacteryBlockEntity#CRYPT_CENTER} blocks in front of it.
 */
public class LichCryptPiece extends StructurePiece {
    public static final int WIDTH = 25;
    public static final int HEIGHT = 12;
    /** The tunnel can run this far; the bounding box always allows for the longest. */
    public static final int MIN_TUNNEL = 2;
    public static final int MAX_TUNNEL = 12;
    /** How far past the tunnel the cave must open, and how far the mouth is dug out through cave ice. */
    public static final int MOUTH_DEPTH = 3;
    public static final int DEPTH = WIDTH + MAX_TUNNEL + MOUTH_DEPTH;
    /** Pieces saved before tunnels varied had this one. */
    private static final int LEGACY_TUNNEL = 6;
    /** The phylactery's local position; the hall's centre is (12, _, 12). */
    public static final int PHYLACTERY_X = 12;
    public static final int PHYLACTERY_Y = 4;
    public static final int PHYLACTERY_Z = 3;
    private static final int[][] WARDS = {{3, 3}, {21, 3}, {3, 21}, {21, 21}};
    private static final int[][] BRAZIERS = {{1, 9}, {1, 15}, {23, 9}, {23, 15}, {5, 1}, {19, 1}, {5, 23}, {19, 23}};
    /** Candle clusters: {x, y, z}; the first four stand on the dais. */
    private static final int[][] CANDLES = {{9, 3, 1}, {15, 3, 1}, {9, 3, 5}, {15, 3, 5},
            {1, 2, 4}, {1, 2, 12}, {1, 2, 20}, {23, 2, 4}, {23, 2, 12}, {23, 2, 20}, {9, 2, 23}, {15, 2, 23}};
    /** Single-block columns, symmetric about the hall's centre (12, 12). */
    private static final int[][] PILLARS = {{6, 6}, {18, 6}, {6, 18}, {18, 18}};
    private static final int FOUNDATION_DEPTH = 12;

    /** What the tunnel's mouth is dug out through. */
    private static final TagKey<Block> MOUTH_CLEARS = TagKey.create(Registries.BLOCK, WildspellMobs.id("crypt_mouth_clears"));
    private static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    private static final BlockState BRICKS = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState POLISHED = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    private static final BlockState CHISELED = Blocks.CHISELED_DEEPSLATE.defaultBlockState();

    private final int tunnel;

    public LichCryptPiece(BlockPos at, Direction facing, int tunnel) {
        super(WildspellMobs.LICH_CRYPT_PIECE.get(), 0, makeBoundingBox(at.getX(), at.getY(), at.getZ(), facing, WIDTH, HEIGHT, DEPTH));
        this.setOrientation(facing);
        this.tunnel = Mth.clamp(tunnel, MIN_TUNNEL, MAX_TUNNEL);
    }

    public LichCryptPiece(CompoundTag tag) {
        super(WildspellMobs.LICH_CRYPT_PIECE.get(), tag);
        this.tunnel = tag.contains("Tunnel", CompoundTag.TAG_INT) ? Mth.clamp(tag.getInt("Tunnel"), MIN_TUNNEL, MAX_TUNNEL) : LEGACY_TUNNEL;
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Tunnel", this.tunnel);
    }

    /** A local position in the world, e.g. to survey the ground before building. */
    public BlockPos localToWorld(int x, int y, int z) {
        return this.getWorldPos(x, y, z).immutable();
    }

    /** The phylactery's position in the world. */
    public BlockPos phylacteryPos() {
        return this.getWorldPos(PHYLACTERY_X, PHYLACTERY_Y, PHYLACTERY_Z).immutable();
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox box,
                            net.minecraft.world.level.ChunkPos chunkPos, BlockPos pivot) {
        // Shell: foundation, walls and roof, hollowed out; weathered bricks here and there.
        for (int x = 0; x < WIDTH; ++x) {
            for (int z = 0; z < WIDTH; ++z) {
                for (int y = 0; y < HEIGHT; ++y) {
                    boolean wall = x == 0 || x == WIDTH - 1 || z == 0 || z == WIDTH - 1;
                    BlockState state;
                    if (y == 0 || y == HEIGHT - 1) {
                        state = y == 0 ? BRICKS : TILES;
                    } else if (wall) {
                        state = y == 6 && (x + z) % 4 == 0 ? Blocks.BLUE_ICE.defaultBlockState()
                                : y == 5 ? POLISHED : this.weathered(x, y, z) ? CRACKED : BRICKS;
                    } else if (y == 1) {
                        state = this.floor(x, z);
                    } else {
                        state = AIR;
                    }
                    this.placeBlock(level, state, x, y, z, box);
                }
                if (x == 0 || x == WIDTH - 1 || z == 0 || z == WIDTH - 1 || (x % 6 == 0 && z % 6 == 0)) {
                    this.foundation(level, x, z, box);
                }
            }
        }
        for (int[] p : PILLARS) {
            this.generateBox(level, box, p[0], 3, p[1], p[0], HEIGHT - 3, p[1], POLISHED, POLISHED, false);
            this.placeBlock(level, CHISELED, p[0], 2, p[1], box);
            this.placeBlock(level, CHISELED, p[0], HEIGHT - 2, p[1], box);
        }
        // The altar: a raised dais with a step, the phylactery on a plinth, skulls either side.
        this.generateBox(level, box, 9, 2, 1, 15, 2, 5, TILES, TILES, false);
        BlockState step = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        this.generateBox(level, box, 10, 2, 6, 14, 2, 6, step, step, false);
        this.placeBlock(level, CHISELED, PHYLACTERY_X, PHYLACTERY_Y - 1, PHYLACTERY_Z, box);
        this.placeBlock(level, WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get().defaultBlockState()
                .setValue(PhylacteryBlock.FACING, Direction.NORTH).setValue(PhylacteryBlock.WARDED, true), PHYLACTERY_X, PHYLACTERY_Y, PHYLACTERY_Z, box);
        this.placeBlock(level, Blocks.SKELETON_SKULL.defaultBlockState(), 10, 3, 2, box);
        this.placeBlock(level, Blocks.SKELETON_SKULL.defaultBlockState(), 14, 3, 2, box);
        for (int[] w : WARDS) {
            this.placeBlock(level, CHISELED, w[0], 2, w[1], box);
            this.placeBlock(level, WildspellMobs.RIME_WARD.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), w[0], 3, w[1], box);
        }
        BlockState brazier = Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false);
        for (int[] b : BRAZIERS) {
            this.placeBlock(level, brazier, b[0], 2, b[1], box);
        }
        BlockState candles = Blocks.BLUE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4).setValue(CandleBlock.LIT, false);
        for (int[] c : CANDLES) {
            this.placeBlock(level, candles, c[0], c[1], c[2], box);
        }
        // Chains hanging from the roof.
        for (int[] c : new int[][] {{12, 9}, {9, 15}, {15, 15}, {12, 20}}) {
            this.generateBox(level, box, c[0], HEIGHT - 3, c[1], c[0], HEIGHT - 2, c[1], Blocks.CHAIN.defaultBlockState(), AIR, false);
        }
        // The entrance: a doorway in the front wall, a tunnel out through the rock, and a chiseled arch at its mouth.
        int end = WIDTH + this.tunnel - 1;
        this.generateBox(level, box, 11, 2, WIDTH - 1, 13, 5, WIDTH - 1, AIR, AIR, false);
        this.generateBox(level, box, 10, 1, WIDTH, 10, 6, end, BRICKS, BRICKS, false);
        this.generateBox(level, box, 14, 1, WIDTH, 14, 6, end, BRICKS, BRICKS, false);
        this.generateBox(level, box, 11, 6, WIDTH, 13, 6, end, BRICKS, BRICKS, false);
        this.generateBox(level, box, 11, 1, WIDTH, 13, 1, end, TILES, TILES, false);
        this.generateBox(level, box, 11, 2, WIDTH, 13, 5, end, AIR, AIR, false);
        this.generateBox(level, box, 10, 1, end, 10, 6, end, POLISHED, POLISHED, false);
        this.generateBox(level, box, 14, 1, end, 14, 6, end, POLISHED, POLISHED, false);
        this.generateBox(level, box, 10, 6, end, 14, 6, end, CHISELED, CHISELED, false);
        for (int z = WIDTH; z <= end; z += 2) {
            this.foundation(level, 10, z, box);
            this.foundation(level, 14, z, box);
        }
        // The cave's own ice (icicles, an ice patch, frost) can grow across the mouth after the site was
        // chosen: dig the way out through it (see the crypt_mouth_clears tag), never through rock.
        for (int z = end + 1; z <= end + MOUTH_DEPTH; ++z) {
            for (int x = 11; x <= 13; ++x) {
                for (int y = 2; y <= 4; ++y) {
                    BlockPos p = this.getWorldPos(x, y, z);
                    BlockState state = box.isInside(p) ? level.getBlockState(p) : AIR;
                    if (state.is(MOUTH_CLEARS)) {
                        level.setBlock(p, AIR, 2);
                    }
                }
            }
        }
    }

    /** Polished deepslate, with a tiled aisle from the door to the altar and a chiseled ring at the centre. */
    private BlockState floor(int x, int z) {
        int dx = Math.abs(x - 12);
        int dz = Math.abs(z - 12);
        if (dx <= 1) {
            return TILES;
        }
        if (Math.max(dx, dz) == 3) {
            return CHISELED;
        }
        return POLISHED;
    }

    /** About one wall brick in six is cracked; hashed from position so chunk seams agree. */
    private boolean weathered(int x, int y, int z) {
        BlockPos p = this.getWorldPos(x, y, z);
        return Math.floorMod(Mth.getSeed(p.getX(), p.getY(), p.getZ()), 6) == 0;
    }

    /** Bricks down to the rock below, so the crypt never floats if it's built over a cavern. */
    private void foundation(WorldGenLevel level, int x, int z, BoundingBox box) {
        for (int y = -1; y >= -FOUNDATION_DEPTH; --y) {
            BlockPos p = this.getWorldPos(x, y, z);
            if (!box.isInside(p)) {
                return;
            }
            BlockState state = level.getBlockState(p);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                return;
            }
            level.setBlock(p, BRICKS, 2);
        }
    }
}
