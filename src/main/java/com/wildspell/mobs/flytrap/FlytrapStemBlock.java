package com.wildspell.mobs.flytrap;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The stalk of a grown flytrap, above its {@link FlytrapBlock}, the way a big dripleaf's stem stands
 * under its leaf. The lowest segment carries the two branches its side heads sit on. It stands only
 * on the grown plant (or more stem), drops nothing itself, and taking it breaks the whole plant.
 */
public class FlytrapStemBlock extends Block {
    public static final MapCodec<FlytrapStemBlock> CODEC = simpleCodec(FlytrapStemBlock::new);
    public static final BooleanProperty BRANCHES = BooleanProperty.create("branches");
    /** How far out along X the branches carry the side heads, and how high up the segment. */
    public static final double BRANCH_REACH = 0.875;
    public static final double BRANCH_HEIGHT = 0.5625;

    private static final VoxelShape STALK = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);

    public FlytrapStemBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BRANCHES, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BRANCHES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return STALK;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || FlytrapBlock.isFlytrap(below) && below.getValue(FlytrapBlock.AGE) == FlytrapBlock.MAX_AGE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                     BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** The plant's base under a stem segment, or null if the stem stands on nothing of the plant. */
    @Nullable
    public static BlockPos baseOf(BlockGetter level, BlockPos pos) {
        BlockPos below = pos.below();
        for (int i = 0; i < FlytrapBlock.STEM_HEIGHT + 1; ++i, below = below.below()) {
            BlockState state = level.getBlockState(below);
            if (FlytrapBlock.isFlytrap(state)) {
                return below;
            }
            if (!state.is(WildspellMobs.FLYTRAP_STEM.get())) {
                return null;
            }
        }
        return null;
    }

    /** In creative, the plant goes without dropping anything, as a double plant does. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos base = baseOf(level, pos);
        if (!level.isClientSide && player.isCreative() && base != null) {
            level.setBlock(base, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** Taking any piece of the stem breaks the whole plant, which drops from its base. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !newState.is(this)) {
            BlockPos base = baseOf(level, pos);
            if (base != null) {
                level.destroyBlock(base, true);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(WildspellMobs.FLYTRAP_SPROUT.get());
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 100;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 60;
    }
}
