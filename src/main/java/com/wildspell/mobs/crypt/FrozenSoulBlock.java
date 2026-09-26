package com.wildspell.mobs.crypt;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One of the souls a lich held, freed when it fell and lingering in the caves around its crypt: a
 * small, cold light hanging in the air, shedding wisps. Nothing holds it up and nothing can touch it;
 * only Silk Touch can catch one, to hang elsewhere.
 */
public class FrozenSoulBlock extends Block {
    public static final MapCodec<FrozenSoulBlock> CODEC = simpleCodec(FrozenSoulBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);

    public FrozenSoulBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    /** Wisps of the soul drift up off it, with a little frost. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SOUL, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(WildspellMobs.FROST_MOTE.get(), x + (random.nextDouble() - 0.5) * 0.5, y + (random.nextDouble() - 0.5) * 0.5,
                    z + (random.nextDouble() - 0.5) * 0.5, 0.0, -0.01, 0.0);
        }
    }
}
