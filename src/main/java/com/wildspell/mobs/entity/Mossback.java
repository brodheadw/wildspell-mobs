package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** One species with four visible life stages. Ancient size is earned, not an ordinary adult spawn. */
public class Mossback extends Animal {
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(Mossback.class, EntityDataSerializers.INT);
    private static final TagKey<Biome> SHORE = TagKey.create(Registries.BIOME, WildspellMobs.id("mossback_shore_spawns"));
    private long birthGameTick = Long.MIN_VALUE;

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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, MossbackGrowth.HATCHLING);
    }

    public float growthScale() {
        return (float) this.getAttributeValue(Attributes.SCALE);
    }

    public int lifeStage() {
        return this.entityData.get(DATA_STAGE);
    }

    public boolean isAncient() {
        return this.lifeStage() == MossbackGrowth.ANCIENT;
    }

    public long ageDays() {
        return this.birthGameTick == Long.MIN_VALUE ? 0L
                : MossbackGrowth.days(MossbackGrowth.elapsed(this.birthGameTick, this.level().getGameTime()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.55, 150));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 9.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (this.birthGameTick == Long.MIN_VALUE) this.birthGameTick = this.level().getGameTime();
            if (this.tickCount % 200 == 0) this.syncGrowth();
        }
    }

    private void syncGrowth() {
        long age = MossbackGrowth.elapsed(this.birthGameTick, this.level().getGameTime());
        float newScale = MossbackGrowth.scale(age);
        int stage = MossbackGrowth.stage(age);
        if (Math.abs(newScale - growthScale()) >= 0.001F) this.getAttribute(Attributes.SCALE).setBaseValue(newScale);
        if (stage != lifeStage()) this.entityData.set(DATA_STAGE, stage);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.birthGameTick != Long.MIN_VALUE) tag.putLong("MossbackBirthGameTick", this.birthGameTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.birthGameTick = tag.contains("MossbackBirthGameTick")
                ? tag.getLong("MossbackBirthGameTick")
                : this.level().getGameTime() - MossbackGrowth.JUVENILE_DAYS * MossbackGrowth.DAY_TICKS;
        this.syncGrowth();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
            MobSpawnType spawnType, @Nullable SpawnGroupData group) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, group);
        long ageDays;
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            boolean shore = level.getBiome(this.blockPosition()).is(SHORE);
            int roll = this.random.nextInt(1000);
            if (shore) {
                ageDays = roll < 850 ? this.random.nextInt(30)
                        : roll < 990 ? 30 + this.random.nextInt(150)
                        : 180 + this.random.nextInt(550);
            } else if (roll < 350) {
                ageDays = this.random.nextInt(30);
            } else if (roll < 750) {
                ageDays = 30 + this.random.nextInt(150);
            } else if (roll < 999) {
                ageDays = 180 + this.random.nextInt(550);
            } else {
                // Roughly 1 in 1,000 inland Mossbacks is an ancient, and only in open terrain
                // with no other loaded ancient within 512 blocks.
                boolean neighbor = level instanceof ServerLevel sl
                        && !sl.getEntitiesOfClass(Mossback.class,
                                new AABB(this.blockPosition()).inflate(512.0),
                                Mossback::isAncient).isEmpty();
                ageDays = neighbor || !this.hasAncientClearance(level, this.blockPosition())
                        ? 180 + this.random.nextInt(550)
                        : MossbackGrowth.FULL_SIZE_DAYS + this.random.nextInt(730);
            }
        } else {
            ageDays = 30; // Spawn eggs are conveniently one-block juveniles for testing.
        }
        this.birthGameTick = this.level().getGameTime() - ageDays * MossbackGrowth.DAY_TICKS
                - this.random.nextInt((int) MossbackGrowth.DAY_TICKS);
        this.syncGrowth();
        return result;
    }

    private boolean hasAncientClearance(ServerLevelAccessor level, BlockPos pos) {
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            for (int y = 0; y <= 5; y++) {
                BlockPos check = pos.offset(x, y, z);
                if (!level.getBlockState(check).getCollisionShape(level, check).isEmpty()) return false;
            }
        }
        return true;
    }

    /** Vanilla beach IDs also cover WWOO reworked beaches. Tiny babies only where land meets water. */
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
        if (level.getBiome(pos).is(SHORE) && !hasNearbyWater(level, pos)) return false;
        if (level instanceof ServerLevel sl) {
            List<Mossback> nearby = sl.getEntitiesOfClass(Mossback.class, new AABB(pos).inflate(64, 32, 64));
            if (nearby.size() >= 3) return false;
        }
        return true;
    }

    private static boolean hasNearbyWater(ServerLevelAccessor level, BlockPos pos) {
        for (int d = 2; d <= 8; d += 2) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos sample = pos.relative(dir, d);
                if (level.getFluidState(sample).is(FluidTags.WATER)
                        || level.getFluidState(sample.below()).is(FluidTags.WATER)) return true;
            }
        }
        return false;
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
        return passenger instanceof Player
                && this.lifeStage() >= MossbackGrowth.MATURE && this.getPassengers().isEmpty();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()
                && !player.isSecondaryUseActive() && this.getPassengers().isEmpty()
                && this.lifeStage() >= MossbackGrowth.MATURE) {
            if (!this.level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }
}
