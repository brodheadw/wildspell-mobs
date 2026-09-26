package com.wildspell.mobs.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/** A rare peaceful living garden: slow, persistent, rideable but never steerable. */
public class Mossback extends Animal {
    public Mossback(EntityType<? extends Mossback> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.95)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.55, 150));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 9.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    /** Allow shaded WWOO jungles and solid volcanic ground, without spawning inside blocks or water. */
    public static boolean checkSpawnRules(EntityType<Mossback> type, ServerLevelAccessor level,
            MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (pos.getY() < level.getSeaLevel() - 8) return false;
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;
        for (int i = 0; i < 3; i++) {
            BlockPos space = pos.above(i);
            if (!level.getFluidState(space).isEmpty()
                    || !level.getBlockState(space).getCollisionShape(level, space).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public int getMaxSpawnClusterSize() { return 1; }

    @Override
    public boolean isFood(ItemStack stack) { return false; }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) { return null; }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player && this.getPassengers().isEmpty();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()
                && !player.isSecondaryUseActive() && this.getPassengers().isEmpty()) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
}
