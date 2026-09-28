package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import java.util.EnumSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class FrozenZombie extends Zombie implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> DATA_SEIZED = SynchedEntityData.defineId(FrozenZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(FrozenZombie.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WHOLE_FACE = SynchedEntityData.defineId(FrozenZombie.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation SEIZED_SLOWDOWN = WildspellMobs.id("seized");
    private static final ResourceLocation ICEBOUND_STUCK = WildspellMobs.id("icebound");
    private static final double SUBMERGED = 0.75;
    private static final double FEAR_RANGE = 4.0;
    public static final int THAW_TICKS = 120;
    public static final TagKey<Item> SCARY_FIRE = TagKey.create(Registries.ITEM, WildspellMobs.id("frightens_the_cold"));

    private static final float SNOWBALL_SPEED = 1.5F;

    public static final int NORMAL = 0;
    public static final int ONE_ARMED = 1;
    public static final int ICEBOUND = 2;

    private int phaseTicks;
    private int thaw;
    @Nullable
    private BlockPos iceBlock;
    @Nullable
    private Vec3 heldAt;

    public float heldLimbSwing;
    public float heldLimbSwingAmount;
    public float heldAgeInTicks;
    public float heldHeadYaw;
    public float heldHeadPitch;

    public FrozenZombie(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.19);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SEIZED, false);
        builder.define(DATA_VARIANT, NORMAL);
        builder.define(DATA_WHOLE_FACE, false);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public boolean hasWholeFace() {
        return this.entityData.get(DATA_WHOLE_FACE);
    }

    public void setWholeFace(boolean whole) {
        this.entityData.set(DATA_WHOLE_FACE, whole);
    }

    public boolean isIcebound() {
        return this.getVariant() == ICEBOUND;
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Math.floorMod(variant, 3));
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (variant == ICEBOUND) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(ICEBOUND_STUCK, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(ICEBOUND_STUCK);
            this.iceBlock = null;
            this.heldAt = null;
        }
        this.setNoGravity(variant == ICEBOUND);
    }

    public void pickVariant() {
        this.pickVariant(true);
    }

    private void pickVariant(boolean canBeIcebound) {
        this.setWholeFace(this.random.nextInt(3) == 0);
        BlockPos below = this.blockPosition().below();
        if (canBeIcebound && this.level().getBlockState(below).is(BlockTags.ICE)) {
            this.setVariant(ICEBOUND);
            this.iceBlock = below;
            this.heldAt = new Vec3(this.getX(), below.getY() + 1.0 - SUBMERGED, this.getZ());
            this.setPos(this.heldAt);
        } else {
            this.setVariant(this.random.nextInt(10) < 6 ? NORMAL : ONE_ARMED);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.pickVariant(spawnType != MobSpawnType.MOB_SUMMONED);
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
        tag.putBoolean("WholeFace", this.hasWholeFace());
        tag.putInt("Thaw", this.thaw);
        if (this.iceBlock != null && this.heldAt != null) {
            tag.putLong("IceBlock", this.iceBlock.asLong());
            tag.putDouble("HeldX", this.heldAt.x);
            tag.putDouble("HeldY", this.heldAt.y);
            tag.putDouble("HeldZ", this.heldAt.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
        this.setWholeFace(tag.getBoolean("WholeFace"));
        this.thaw = tag.getInt("Thaw");
        if (tag.contains("IceBlock")) {
            this.iceBlock = BlockPos.of(tag.getLong("IceBlock"));
            this.heldAt = new Vec3(tag.getDouble("HeldX"), tag.getDouble("HeldY"), tag.getDouble("HeldZ"));
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new SnowballGoal());
        this.goalSelector.addGoal(1, new FleeFireGoal());
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        FrostShard snowball = new FrostShard(this.level(), this);
        snowball.setItem(new ItemStack(Items.SNOWBALL));
        double dx = target.getX() - this.getX();
        double dz = target.getZ() - this.getZ();
        snowball.shoot(dx, snowball.lobTo(target, dx, dz, SNOWBALL_SPEED), dz, SNOWBALL_SPEED, 5.0F);
        this.swing(InteractionHand.MAIN_HAND);
        this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.6F + this.random.nextFloat() * 0.2F);
        this.level().addFreshEntity(snowball);
    }

    public boolean isSeized() {
        return this.entityData.get(DATA_SEIZED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.random.nextInt(10) == 0) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, -0.03, 0.0);
            }
            return;
        }
        if (this.thawInSunOrFire()) {
            return;
        }
        if (this.isIcebound()) {
            this.holdInIce();
            return;
        }
        if (--this.phaseTicks > 0) {
            return;
        }
        boolean walking = !this.getNavigation().isDone();
        AttributeInstance speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (this.isSeized()) {
            this.entityData.set(DATA_SEIZED, false);
            this.phaseTicks = 12 + this.random.nextInt(10);
            speed.removeModifier(SEIZED_SLOWDOWN);
            if (walking) {
                this.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.7F, 0.85F + this.random.nextFloat() * 0.25F);
                ((ServerLevel) this.level()).sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.getY(0.4), this.getZ(), 8, 0.25, 0.4, 0.25, 0.08);
            }
        } else {
            this.entityData.set(DATA_SEIZED, true);
            this.phaseTicks = 5 + this.random.nextInt(8);
            speed.addOrUpdateTransientModifier(new AttributeModifier(SEIZED_SLOWDOWN, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void holdInIce() {
        if (this.iceBlock == null || this.heldAt == null || !this.level().getBlockState(this.iceBlock).is(BlockTags.ICE)) {
            this.setVariant(NORMAL);
            this.entityData.set(DATA_SEIZED, false);
            this.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 0.8F, 1.3F);
            ((ServerLevel) this.level()).sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.getY(0.2), this.getZ(), 20, 0.3, 0.3, 0.3, 0.12);
            return;
        }
        this.entityData.set(DATA_SEIZED, true);
        this.setDeltaMovement(Vec3.ZERO);
        if (this.position().distanceToSqr(this.heldAt) > 1.0E-4) {
            this.setPos(this.heldAt);
        }
        if (--this.phaseTicks <= 0) {
            this.phaseTicks = 40 + this.random.nextInt(60);
            this.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.6F, 0.7F + this.random.nextFloat() * 0.2F);
            ((ServerLevel) this.level()).sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.heldAt.y + SUBMERGED, this.getZ(), 6, 0.3, 0.05, 0.3, 0.06);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Frost.freezeSolid(living, 60);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0), this);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            this.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.9F, 1.05F + this.random.nextFloat() * 0.2F);
        }
        return hurt;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    private boolean thawInSunOrFire() {
        boolean burning = this.isOnFire();
        if (!burning && !this.inSunlight()) {
            if (this.thaw > 0) {
                --this.thaw;
            }
            return false;
        }
        this.thaw += burning ? 2 : 1;
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.DRIPPING_WATER, this.getRandomX(0.5), this.getY(0.3 + this.random.nextDouble() * 0.6), this.getRandomZ(0.5), 2, 0.1, 0.1, 0.1, 0.0);
        }
        if (this.tickCount % 20 == 0) {
            this.playSound(burning ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.4F, 1.2F);
        }
        if (this.thaw < THAW_TICKS || !EventHooks.canLivingConvert(this, EntityType.ZOMBIE, ticks -> this.thaw = THAW_TICKS - ticks)) {
            return false;
        }
        Vec3 standAt = this.isIcebound() && this.heldAt != null ? new Vec3(this.getX(), this.heldAt.y + SUBMERGED, this.getZ()) : null;
        int fire = this.getRemainingFireTicks();
        Zombie zombie = this.convertTo(EntityType.ZOMBIE, true);
        if (zombie == null) {
            return false;
        }
        if (standAt != null) {
            zombie.setPos(standAt);
        }
        zombie.setRemainingFireTicks(fire);
        level.sendParticles(ColdEffects.ICE_CHIPS, zombie.getX(), zombie.getY(0.5), zombie.getZ(), 25, 0.3, 0.6, 0.3, 0.12);
        level.sendParticles(ParticleTypes.SPLASH, zombie.getX(), zombie.getY(0.5), zombie.getZ(), 30, 0.3, 0.6, 0.3, 0.1);
        zombie.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 0.8F, 1.3F);
        EventHooks.onLivingConvert(this, zombie);
        return true;
    }

    private boolean inSunlight() {
        return this.level().isDay() && !this.isInWaterRainOrBubble() && this.getLightLevelDependentMagicValue() > 0.5F
                && this.level().canSeeSky(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()));
    }

    public static boolean isOpenFlame(BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(BlockTags.CAMPFIRES) && state.getOptionalValue(BlockStateProperties.LIT).orElse(false)
                || state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.getFluidState().is(FluidTags.LAVA);
    }

    @Nullable
    private Vec3 nearestFire() {
        for (LivingEntity other : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(FEAR_RANGE),
                e -> e != this && (e.getMainHandItem().is(SCARY_FIRE) || e.getOffhandItem().is(SCARY_FIRE)))) {
            return other.position();
        }
        return BlockPos.findClosestMatch(this.blockPosition(), (int) FEAR_RANGE, 2, pos -> this.level().isLoaded(pos) && isOpenFlame(this.level().getBlockState(pos)))
                .map(Vec3::atCenterOf).orElse(null);
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WildspellMobs.FROZEN_ZOMBIE_SHATTER.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.8F;
    }

    private class SnowballGoal extends RangedAttackGoal {
        SnowballGoal() {
            super(FrozenZombie.this, 1.0, 40, 60, 12.0F);
        }

        @Override
        public boolean canUse() {
            return FrozenZombie.this.isIcebound() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return FrozenZombie.this.isIcebound() && super.canContinueToUse();
        }
    }

    private class FleeFireGoal extends Goal {
        @Nullable
        private Path path;
        private int checkTicks;

        FleeFireGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (FrozenZombie.this.isIcebound() || --this.checkTicks > 0) {
                return false;
            }
            this.checkTicks = 10 + FrozenZombie.this.random.nextInt(10);
            Vec3 fire = FrozenZombie.this.nearestFire();
            if (fire == null) {
                return false;
            }
            for (int attempt = 0; attempt < 6; ++attempt) {
                Vec3 away = DefaultRandomPos.getPosAway(FrozenZombie.this, 8, 4, fire);
                if (away == null) {
                    continue;
                }
                Path path = FrozenZombie.this.getNavigation().createPath(away.x, away.y, away.z, 0);
                if (path != null && path.canReach()) {
                    this.path = path;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            FrozenZombie.this.getNavigation().moveTo(this.path, 1.3);
        }

        @Override
        public boolean canContinueToUse() {
            return !FrozenZombie.this.getNavigation().isDone();
        }
    }
}
