package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * A zombie cased in ice, what a zombie becomes after lingering in the Frosted Caves. It moves slowly
 * and with effort: it drags itself forward, joints crunching and ice chipping off, then seizes up
 * mid-stride for a moment before breaking free again.
 *
 * <p>Variants: some have lost an arm to the cold, and one that froze over standing on an ice block has
 * sunk into it up to the hips. That one is stuck fast, straining against the ice and throwing snowballs;
 * break the ice around it and it pulls free as an ordinary Frozen Zombie.
 */
public class FrozenZombie extends Zombie implements RangedAttackMob {
    private static final EntityDataAccessor<Boolean> DATA_SEIZED = SynchedEntityData.defineId(FrozenZombie.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(FrozenZombie.class, EntityDataSerializers.INT);
    private static final ResourceLocation SEIZED_SLOWDOWN = WildspellMobs.id("seized");
    private static final ResourceLocation ICEBOUND_STUCK = WildspellMobs.id("icebound");
    /** How deep an ice-bound zombie sits in its block: its legs are 12 of its 32 pixels, 0.75 blocks. */
    private static final double SUBMERGED = 0.75;

    private static final float SNOWBALL_SPEED = 1.5F;

    public static final int NORMAL = 0;
    public static final int ONE_ARMED = 1;
    public static final int ICEBOUND = 2;
    private static final BlockParticleOption ICE_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

    private int phaseTicks;
    // Ice-bound only: the ice block it's frozen into, and the spot it's held at.
    @Nullable
    private BlockPos iceBlock;
    @Nullable
    private Vec3 heldAt;

    // Client-side pose snapshot the model holds while seized, so the body freezes mid-stride.
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
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
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

    /**
     * Frozen standing on ice, its legs are locked into it. Otherwise six in ten come through whole and
     * the rest lose an arm.
     */
    public void pickVariant() {
        BlockPos below = this.blockPosition().below();
        if (this.level().getBlockState(below).is(BlockTags.ICE)) {
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
        this.pickVariant();
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
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
        if (tag.contains("IceBlock")) {
            this.iceBlock = BlockPos.of(tag.getLong("IceBlock"));
            this.heldAt = new Vec3(tag.getDouble("HeldX"), tag.getDouble("HeldY"), tag.getDouble("HeldZ"));
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Outranks the zombie's melee goal and shares its flags, so an ice-bound one only ever throws.
        this.goalSelector.addGoal(1, new SnowballGoal());
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        FrostShard snowball = new FrostShard(this.level(), this);
        snowball.setItem(new ItemStack(Items.SNOWBALL));
        double dx = target.getX() - this.getX();
        // Aim at the middle of the target, whatever its height, lofted by how far the snowball will drop
        // on the way: gravity * flightTime^2 / 2, with flightTime = distance / speed.
        double dy = target.getY(0.5) - snowball.getY();
        double dz = target.getZ() - this.getZ();
        double flightTicks = Math.sqrt(dx * dx + dz * dz) / SNOWBALL_SPEED;
        double drop = 0.5 * FrostShard.GRAVITY * flightTicks * flightTicks;
        snowball.shoot(dx, dy + drop, dz, SNOWBALL_SPEED, 5.0F);
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
            // Break free and drag forward, joints crunching.
            this.entityData.set(DATA_SEIZED, false);
            this.phaseTicks = 12 + this.random.nextInt(10);
            speed.removeModifier(SEIZED_SLOWDOWN);
            if (walking) {
                this.playSound(WildspellMobs.FROZEN_ZOMBIE_CRUNCH.get(), 0.7F, 0.85F + this.random.nextFloat() * 0.25F);
                ((ServerLevel) this.level()).sendParticles(ICE_CHIPS, this.getX(), this.getY(0.4), this.getZ(), 8, 0.25, 0.4, 0.25, 0.08);
            }
        } else {
            // Seize up mid-stride.
            this.entityData.set(DATA_SEIZED, true);
            this.phaseTicks = 5 + this.random.nextInt(8);
            speed.addOrUpdateTransientModifier(new AttributeModifier(SEIZED_SLOWDOWN, -0.9, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** Keep an ice-bound zombie pinned in its block, straining now and then; free it if the ice is gone. */
    private void holdInIce() {
        if (this.iceBlock == null || this.heldAt == null || !this.level().getBlockState(this.iceBlock).is(BlockTags.ICE)) {
            this.setVariant(NORMAL);
            this.entityData.set(DATA_SEIZED, false);
            this.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 0.8F, 1.3F);
            ((ServerLevel) this.level()).sendParticles(ICE_CHIPS, this.getX(), this.getY(0.2), this.getZ(), 20, 0.3, 0.3, 0.3, 0.12);
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
            ((ServerLevel) this.level()).sendParticles(ICE_CHIPS, this.getX(), this.heldAt.y + SUBMERGED, this.getZ(), 6, 0.3, 0.05, 0.3, 0.06);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.setTicksFrozen(Math.max(living.getTicksFrozen(), living.getTicksRequiredToFreeze() + 60));
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
}
