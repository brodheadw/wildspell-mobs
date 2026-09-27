package com.wildspell.mobs.flytrap;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.FlytrapHead;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rooted part of a flytrap: a rosette of leaves and a stalk, with its jaws as {@link FlytrapHead}
 * entities anchored above it. It grows like a sapling (random ticks in light 9 or more, or bone meal)
 * through three stages ({@link #AGE}):
 * <ul>
 * <li>0, a sprout: one small head at its tip that snaps only at tiny creatures (moths, bees...);
 * <li>1, about a block tall: one head at about a player's height, which snaps at anything moving;
 * <li>2, a stalk three blocks tall (this block and two {@link FlytrapStemBlock}s above it) with a big
 * head on top and two smaller ones on branches either side.
 * </ul>
 * It roots only in {@link #ROOTS_IN} (grass, moss, mud and other dirt) and breaks as one plant: taking
 * any part of it, or killing any of its heads, breaks the whole thing, which drops a sprout and a Trap
 * Jaw per grown stage. Heads keep to their plant: they appear a tick after the plant is placed or
 * changes stage, and vanish when it goes (however it goes, set to air included) or changes stage.
 *
 * <p>Wildspell Magic places it by id ({@code wildspellmobs:flytrap}, any {@code age}); placing the
 * grown stage builds its stem, room permitting (else it settles for stage 1).
 */
public class FlytrapBlock extends Block implements BonemealableBlock {
    public static final MapCodec<FlytrapBlock> CODEC = simpleCodec(FlytrapBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    public static final int MAX_AGE = 2;
    /** Ground a flytrap roots in: grass, moss, mud and other dirt. */
    public static final TagKey<Block> ROOTS_IN = TagKey.create(Registries.BLOCK, WildspellMobs.id("flytrap_roots_in"));
    public static final TagKey<Block> HOSTILE_GROWTH = TagKey.create(Registries.BLOCK, WildspellMobs.id("hostile_growth"));
    /** Chance per random tick, in enough light, of growing a stage: about ten minutes a stage. */
    private static final int GROW_ONE_IN = 10;
    /** Stems above a grown plant's base. */
    public static final int STEM_HEIGHT = 2;

    private static final VoxelShape SPROUT = Block.box(4.0, 0.0, 4.0, 12.0, 6.0, 12.0);
    private static final VoxelShape YOUNG = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape STALK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    /**
     * Where a stage's heads sit, relative to the block's corner: which slot, how big, and where its
     * neck meets the plant. The grown plant's side heads sit at the ends of its stem's branches.
     */
    public record HeadSlot(int slot, int size, Vec3 offset) {
    }

    private static final List<List<HeadSlot>> SLOTS = List.of(
            List.of(new HeadSlot(0, FlytrapHead.SIZE_SMALL, new Vec3(0.5, 0.375, 0.5))),
            List.of(new HeadSlot(0, FlytrapHead.SIZE_MEDIUM, new Vec3(0.5, 1.0, 0.5))),
            List.of(new HeadSlot(0, FlytrapHead.SIZE_BIG, new Vec3(0.5, 1.0 + STEM_HEIGHT, 0.5)),
                    new HeadSlot(1, FlytrapHead.SIZE_MEDIUM, new Vec3(0.5 - FlytrapStemBlock.BRANCH_REACH, 1.0 + FlytrapStemBlock.BRANCH_HEIGHT, 0.5)),
                    new HeadSlot(2, FlytrapHead.SIZE_MEDIUM, new Vec3(0.5 + FlytrapStemBlock.BRANCH_REACH, 1.0 + FlytrapStemBlock.BRANCH_HEIGHT, 0.5))));

    public FlytrapBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    public static List<HeadSlot> slots(int age) {
        return SLOTS.get(age);
    }

    public static boolean isFlytrap(BlockState state) {
        return state.is(WildspellMobs.FLYTRAP.get());
    }

    // --- Shape and survival --------------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AGE)) {
            case 0 -> SPROUT;
            case 1 -> YOUNG;
            default -> STALK;
        };
    }

    /** The young plant is soft enough to walk through; the grown one's stalk is not. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AGE) == MAX_AGE ? STALK : Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ROOTS_IN);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    // --- Growth --------------------------------------------------------------------------------------

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < MAX_AGE && level.getRawBrightness(pos.above(), 0) >= 9 && random.nextInt(GROW_ONE_IN) == 0) {
            this.grow(level, pos, state);
        } else {
            // A safety net: a head lost some other way than dying (which breaks the plant) comes back.
            syncHeads(level, pos, state);
        }
    }

    /** Whether it has room for its next stage: the grown plant's stem and top head need three blocks of air. */
    public static boolean canGrow(LevelReader level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        return age < MAX_AGE && (age + 1 < MAX_AGE || hasRoomForStem(level, pos));
    }

    private static boolean hasRoomForStem(LevelReader level, BlockPos pos) {
        for (int dy = 1; dy <= STEM_HEIGHT + 1; ++dy) {
            if (!level.getBlockState(pos.above(dy)).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    /** Grows it a stage, if it has the room. */
    public void grow(Level level, BlockPos pos, BlockState state) {
        if (canGrow(level, pos, state)) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return canGrow(level, pos, state);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        this.grow(level, pos, state);
    }

    /** Puts up the grown plant's stem: the branched segment, then plain ones. */
    public static void buildStem(LevelAccessor level, BlockPos pos, int flags) {
        for (int dy = 1; dy <= STEM_HEIGHT; ++dy) {
            level.setBlock(pos.above(dy), WildspellMobs.FLYTRAP_STEM.get().defaultBlockState()
                    .setValue(FlytrapStemBlock.BRANCHES, dy == 1), flags);
        }
    }

    // --- Heads ---------------------------------------------------------------------------------------

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(state.getBlock()) && oldState.getValue(AGE).equals(state.getValue(AGE))) {
            return;
        }
        if (state.getValue(AGE) == MAX_AGE && !level.getBlockState(pos.above()).is(WildspellMobs.FLYTRAP_STEM.get())) {
            if (hasRoomForStem(level, pos)) {
                buildStem(level, pos, Block.UPDATE_ALL);
            } else {
                level.setBlock(pos, state.setValue(AGE, MAX_AGE - 1), Block.UPDATE_ALL);
                return;
            }
        }
        if (level instanceof ServerLevel server) {
            clearHeads(server, pos, head -> head.getStage() != state.getValue(AGE));
        }
        // The heads come a tick later, once the block (and any stem) has settled.
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        syncHeads(level, pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(state.getBlock()) && level instanceof ServerLevel server) {
            // A head that is dying (and so broke the plant) withers where it is.
            clearHeads(server, pos, head -> !head.isDeadOrDying());
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The heads anchored to the plant at {@code pos}. */
    public static List<FlytrapHead> headsOf(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(FlytrapHead.class, new AABB(pos).inflate(3.0, 5.0, 3.0), head -> pos.equals(head.getAnchor()));
    }

    private static void clearHeads(ServerLevel level, BlockPos pos, java.util.function.Predicate<FlytrapHead> which) {
        for (FlytrapHead head : headsOf(level, pos)) {
            if (which.test(head)) {
                head.discard();
            }
        }
    }

    /** Clears heads from another stage and puts any missing ones of this stage in their places. */
    public static void syncHeads(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        List<FlytrapHead> heads = headsOf(level, pos);
        for (HeadSlot slot : slots(age)) {
            boolean present = false;
            for (FlytrapHead head : heads) {
                if (head.getStage() != age) {
                    head.discard();
                } else if (head.getSlot() == slot.slot() && head.isAlive()) {
                    present = true;
                }
            }
            if (!present) {
                FlytrapHead head = WildspellMobs.FLYTRAP_HEAD.get().create(level);
                if (head != null) {
                    head.anchorTo(pos, age, slot);
                    level.addFreshEntity(head);
                }
            }
        }
    }

    // --- Fire ----------------------------------------------------------------------------------------

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }
}
