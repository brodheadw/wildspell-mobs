package com.wildspell.mobs.moth;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light a Luminous Moth sheds: invisible, intangible and replaceable, like vanilla's light block.
 * It checks on a timer that a moth is still keeping it and removes itself if not, so the light
 * follows the moth and never outlives it. A trail glow needs a moth right beside it; an anchored one
 * (around a released moth's home) needs that moth nearby.
 */
public class MothGlowBlock extends Block {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    public static final BooleanProperty ANCHOR = BooleanProperty.create("anchor");
    private static final int TRAIL_CHECK = 10;
    private static final int ANCHOR_CHECK = 60;

    public MothGlowBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, LuminousMoth.TRAIL_LIGHT).setValue(ANCHOR, false));
    }

    /** Sets a glow of {@code light} at {@code pos} and starts its keep-alive checks. */
    public static void place(Level level, BlockPos pos, int light, boolean anchor) {
        BlockState state = WildspellMobs.MOTH_GLOW.get().defaultBlockState().setValue(LEVEL, light).setValue(ANCHOR, anchor);
        if (level.getBlockState(pos) != state) {
            // No neighbour or shape updates: nothing around cares that the air got brighter.
            level.setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
            level.scheduleTick(pos, state.getBlock(), anchor ? ANCHOR_CHECK : TRAIL_CHECK);
        }
    }

    public static int lightLevel(BlockState state) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, ANCHOR);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean anchor = state.getValue(ANCHOR);
        boolean kept = anchor
                ? !level.getEntitiesOfClass(LuminousMoth.class, new AABB(pos).inflate(LuminousMoth.HOME_RADIUS + 8), moth -> moth.keepsLit(pos)).isEmpty()
                : !level.getEntitiesOfClass(LuminousMoth.class, new AABB(pos).inflate(1.5), LuminousMoth::isAlive).isEmpty();
        if (kept) {
            level.scheduleTick(pos, this, anchor ? ANCHOR_CHECK : TRAIL_CHECK);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
