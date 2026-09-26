package com.wildspell.mobs.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * An electric eel of flooded caves. It keeps a den, a crevice in the rock near where it spawned, and
 * lies in it with its head out. It ignores anyone on dry land, but anything that swims into its
 * territory gets hunted.
 *
 * <p>Its weapon is the discharge. It stops, winds up for {@link #CHARGE_TICKS} (crackling and
 * glowing brighter, fair warning to get out of the water), then lets go: everything in the water
 * within {@link #SHOCK_RADIUS} is hurt and seized (slowed hard) as its muscles clench, whoever the
 * eel was after or not. Other eels are unharmed. Between discharges it bites. Like real electric
 * eels, it also leaps: someone standing at the water's edge within reach gets a leap and a stronger
 * contact shock. Stranded, it flops toward the nearest water.
 *
 * <p>It senses by its own electric field rather than sight, so it gives off a faint flicker now
 * and then even at rest.
 */
public class ElectricEel extends WaterAnimal {
    private static final EntityDataAccessor<Integer> DATA_CHARGE = SynchedEntityData.defineId(ElectricEel.class, EntityDataSerializers.INT);

    public static final ResourceKey<DamageType> SHOCK = ResourceKey.create(Registries.DAMAGE_TYPE, WildspellMobs.id("eel_shock"));

    /** How long a discharge takes to wind up, and how long after one before the next can start. */
    public static final int CHARGE_TICKS = 30;
    public static final int RECHARGE_TICKS = 100;
    /** How far a discharge carries through the water, and what it does to anything caught in it. */
    public static final double SHOCK_RADIUS = 5.0;
    public static final float SHOCK_DAMAGE = 4.0F;
    public static final int SEIZE_TICKS = 40;
    /** A leap's contact shock: stronger, and only for the one it hits. */
    public static final float LEAP_DAMAGE = 6.0F;
    /** How far out of the water (horizontally) the eel leaps at someone at the edge. */
    public static final double LEAP_RANGE = 4.0;
    /** How near its den a swimmer must come to be hunted, and how far the eel roams from it. */
    public static final double TERRITORY = 6.0;
    public static final int DEN_RANGE = 10;
    /** How far it looks for a crevice to make its den. */
    private static final int DEN_SEARCH = 6;
    /** How long a target can stay out of reach (out of the water, beyond a leap) before it's let go. */
    private static final int GIVE_UP_TICKS = 60;
    private static final byte DISCHARGE_EVENT = 71;

    @Nullable
    private BlockPos den;
    private int denSearchTicks;
    private int rechargeTicks;
    private int strandedTicks;
    private int lurkCooldown = 100;
    private boolean lurking;
    // Client: ticks left of the discharge's flash.
    private int flashTicks;

    public ElectricEel(EntityType<? extends ElectricEel> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, false);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 1.2)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** Natural spawns: as the glow squid's, in deep water that never sees light. */
    public static boolean checkEelSpawnRules(EntityType<ElectricEel> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return pos.getY() <= level.getSeaLevel() - 33 && level.getRawBrightness(pos, 0) == 0 && level.getBlockState(pos).is(Blocks.WATER);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGE, 0);
    }

    /** Ticks into winding up a discharge; 0 when it isn't. */
    public int getCharge() {
        return this.entityData.get(DATA_CHARGE);
    }

    private void setCharge(int charge) {
        this.entityData.set(DATA_CHARGE, charge);
    }

    @Nullable
    public BlockPos getDen() {
        return this.den;
    }

    /** Keeps the eel to {@code den}; null lets it roam until it finds one. */
    public void setDen(@Nullable BlockPos den) {
        this.den = den;
        if (den == null) {
            this.clearRestriction();
        } else {
            this.restrictTo(den, DEN_RANGE);
        }
    }

    /** True while the eel lies still in its den. */
    public boolean isLurking() {
        return this.lurking;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.den != null) {
            tag.putLong("Den", this.den.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setDen(tag.contains("Den") ? BlockPos.of(tag.getLong("Den")) : null);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DischargeGoal());
        this.goalSelector.addGoal(1, new LeapGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(4, new LurkGoal());
        this.goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.8, 40));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // It senses a swimmer's field rather than seeing it, so it needn't see them.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false,
                this::isIntruder));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractFish.class, 40, false, false, this::inTerritory));
    }

    /** True for someone swimming in the eel's territory, which it will go for. */
    public boolean isIntruder(LivingEntity entity) {
        return entity.isInWater() && this.inTerritory(entity);
    }

    private boolean inTerritory(LivingEntity entity) {
        Vec3 origin = this.den != null ? Vec3.atCenterOf(this.den) : this.position();
        return entity.position().closerThan(origin, TERRITORY);
    }

    /** Dolphin-style swimming in water; out of it, ordinary movement (and gravity). */
    @Override
    public void travel(Vec3 travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9));
            this.calculateEntityAnimation(true);
        } else {
            super.travel(travelVector);
        }
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && !this.isInWater() && this.onGround() && this.verticalCollision) {
            this.flopTowardWater();
        }
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.flashTicks > 0) {
                --this.flashTicks;
            }
            if (this.getCharge() > 0 && this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, this.getRandomX(1.2), this.getRandomY(), this.getRandomZ(1.2),
                        0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.rechargeTicks > 0) {
            --this.rechargeTicks;
        }
        if (this.lurkCooldown > 0) {
            --this.lurkCooldown;
        }
        if (this.den == null && this.isInWater() && --this.denSearchTicks <= 0) {
            this.denSearchTicks = 40;
            this.setDen(this.findDen());
        }
        LivingEntity target = this.getTarget();
        if (target != null) {
            boolean reachable = target.isInWater() || this.distanceTo(target) <= LEAP_RANGE + 1.0;
            this.strandedTicks = reachable ? 0 : this.strandedTicks + 1;
            boolean strayed = this.den != null && !target.position().closerThan(Vec3.atCenterOf(this.den), DEN_RANGE + TERRITORY);
            if (this.strandedTicks > GIVE_UP_TICKS || strayed) {
                this.setTarget(null);
                this.strandedTicks = 0;
            }
        }
    }

    /**
     * A crevice nearby to lie in: a water block walled in on at least four of its six sides. Failing
     * one, a spot it has lingered at long enough will do.
     */
    @Nullable
    private BlockPos findDen() {
        Level level = this.level();
        for (BlockPos pos : BlockPos.withinManhattan(this.blockPosition(), DEN_SEARCH, DEN_SEARCH / 2, DEN_SEARCH)) {
            if (level.isLoaded(pos) && isCrevice(level, pos)) {
                return pos.immutable();
            }
        }
        return this.tickCount > 400 ? this.blockPosition() : null;
    }

    static boolean isCrevice(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.WATER)) {
            return false;
        }
        int walls = 0;
        for (Direction direction : Direction.values()) {
            BlockPos side = pos.relative(direction);
            walls += level.getBlockState(side).isFaceSturdy(level, side, direction.getOpposite()) ? 1 : 0;
        }
        return walls >= 4;
    }

    /** Stranded: a fish's flop, aimed at water if there's any close by. */
    private void flopTowardWater() {
        Vec3 toward = Vec3.ZERO;
        for (BlockPos pos : BlockPos.withinManhattan(this.blockPosition(), 3, 1, 3)) {
            if (this.level().getFluidState(pos).is(FluidTags.WATER)) {
                toward = Vec3.atCenterOf(pos).subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.15);
                break;
            }
        }
        this.setDeltaMovement(this.getDeltaMovement().add(toward.x + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F, 0.4,
                toward.z + (this.random.nextFloat() * 2.0F - 1.0F) * 0.05F));
        this.setOnGround(false);
        this.hasImpulse = true;
        this.playSound(SoundEvents.COD_FLOP, this.getSoundVolume(), this.getVoicePitch());
    }

    private DamageSource shock() {
        return this.damageSources().source(SHOCK, this);
    }

    /** A shocked victim's muscles clench: it can barely move for a moment. */
    private static void seize(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SEIZE_TICKS, 2));
    }

    /** Lets the charge go: hurts and seizes everything in the water nearby except other eels. */
    public void discharge() {
        this.setCharge(0);
        this.rechargeTicks = RECHARGE_TICKS + this.random.nextInt(40);
        this.level().broadcastEntityEvent(this, DISCHARGE_EVENT);
        this.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.8F + this.random.nextFloat() * 0.2F);
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SHOCK_RADIUS),
                e -> e.isAlive() && !(e instanceof ElectricEel) && e.isInWater() && this.distanceTo(e) <= SHOCK_RADIUS)) {
            if (victim.hurt(this.shock(), SHOCK_DAMAGE)) {
                seize(victim);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != DISCHARGE_EVENT) {
            super.handleEntityEvent(id);
            return;
        }
        this.flashTicks = 6;
        for (int i = 0; i < 40; ++i) {
            Vec3 at = this.position().add(new Vec3(this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5, this.random.nextDouble() - 0.5)
                    .normalize().scale(this.random.nextDouble() * SHOCK_RADIUS));
            this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    /**
     * How brightly the eel's electric organ shows, 0 to 1: rising through a wind-up, full in the
     * flash of a discharge, and otherwise just a faint, occasional sensing flicker.
     */
    public float getGlow(float partialTick) {
        if (this.flashTicks > 0) {
            return 1.0F;
        }
        int charge = this.getCharge();
        if (charge > 0) {
            float rise = Math.min(1.0F, (charge + partialTick) / CHARGE_TICKS);
            return (0.25F + 0.6F * rise) * (0.8F + 0.2F * this.random.nextFloat());
        }
        return (this.tickCount + this.getId() * 7) % 50 < 2 ? 0.15F : 0.0F;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt) {
            this.lurking = false;
        }
        return hurt;
    }

    @Override
    protected int getBaseExperienceReward() {
        return 5;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.7F;
    }

    /** Stop, build up a charge with the target in the water nearby, then let it go on everything around. */
    private class DischargeGoal extends Goal {
        DischargeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = ElectricEel.this.getTarget();
            return ElectricEel.this.rechargeTicks <= 0 && ElectricEel.this.isInWater() && target != null && target.isAlive()
                    && target.isInWater() && ElectricEel.this.distanceTo(target) <= SHOCK_RADIUS - 1.0;
        }

        /** Once it starts winding up it's committed: the discharge comes whoever is still around. */
        @Override
        public boolean canContinueToUse() {
            return ElectricEel.this.getCharge() > 0 && ElectricEel.this.isInWater();
        }

        @Override
        public void start() {
            ElectricEel.this.lurking = false;
            ElectricEel.this.getNavigation().stop();
            ElectricEel.this.setCharge(1);
            ElectricEel.this.playSound(SoundEvents.GUARDIAN_ATTACK, 0.5F, 1.8F);
        }

        @Override
        public void stop() {
            // Hauled out of the water mid wind-up: the charge fizzles, and it's a moment before the next.
            if (ElectricEel.this.getCharge() > 0) {
                ElectricEel.this.setCharge(0);
                ElectricEel.this.rechargeTicks = Math.max(ElectricEel.this.rechargeTicks, 20);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ElectricEel eel = ElectricEel.this;
            LivingEntity target = eel.getTarget();
            if (target != null) {
                eel.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            eel.setDeltaMovement(eel.getDeltaMovement().scale(0.8));
            int charge = eel.getCharge() + 1;
            if (charge >= CHARGE_TICKS) {
                eel.discharge();
            } else {
                eel.setCharge(charge);
            }
        }
    }

    /** At someone standing at the water's edge: leap out at them and shock them on contact. */
    private class LeapGoal extends Goal {
        private int ticks;
        private boolean struck;

        LeapGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            ElectricEel eel = ElectricEel.this;
            LivingEntity target = eel.getTarget();
            if (eel.rechargeTicks > 0 || !eel.isInWater() || target == null || !target.isAlive() || target.isInWater()) {
                return false;
            }
            double rise = target.getY() - eel.getY();
            Vec3 across = target.position().subtract(eel.position()).multiply(1.0, 0.0, 1.0);
            // Near the surface: it can only leap through a couple of blocks of water.
            boolean nearSurface = !eel.level().getFluidState(eel.blockPosition().above(2)).is(FluidTags.WATER);
            return across.length() <= LEAP_RANGE && rise > -1.0 && rise < 3.0 && nearSurface && eel.hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.struck && this.ticks < 30 && !(this.ticks > 5 && ElectricEel.this.isInWater());
        }

        @Override
        public void start() {
            ElectricEel eel = ElectricEel.this;
            LivingEntity target = eel.getTarget();
            this.ticks = 0;
            this.struck = false;
            eel.lurking = false;
            eel.getNavigation().stop();
            Vec3 across = target.position().subtract(eel.position()).multiply(1.0, 0.0, 1.0);
            double rise = Math.max(0.0, target.getY() - eel.getY());
            Vec3 launch = across.scale(0.12).add(0.0, 0.7 + rise * 0.12, 0.0);
            eel.setDeltaMovement(launch);
            eel.hasImpulse = true;
            eel.setCharge(CHARGE_TICKS - 1);
            eel.playSound(SoundEvents.GENERIC_SPLASH, 0.6F, 1.2F);
        }

        @Override
        public void stop() {
            ElectricEel eel = ElectricEel.this;
            eel.setCharge(0);
            eel.rechargeTicks = Math.max(eel.rechargeTicks, this.struck ? RECHARGE_TICKS : 40);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.ticks;
            ElectricEel eel = ElectricEel.this;
            LivingEntity target = eel.getTarget();
            if (target == null || !eel.getBoundingBox().inflate(0.4).intersects(target.getBoundingBox())) {
                return;
            }
            this.struck = true;
            eel.level().broadcastEntityEvent(eel, DISCHARGE_EVENT);
            eel.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.8F);
            if (target.hurt(eel.shock(), LEAP_DAMAGE)) {
                seize(target);
            }
        }
    }

    /** With nothing to hunt, go back to the den and lie in it for a good while. */
    private class LurkGoal extends Goal {
        private int ticks;
        private int stayTicks;

        LurkGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            ElectricEel eel = ElectricEel.this;
            return eel.den != null && eel.getTarget() == null && eel.lurkCooldown <= 0 && eel.isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            return ElectricEel.this.getTarget() == null && this.ticks < this.stayTicks && ElectricEel.this.den != null;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.stayTicks = 400;
            BlockPos den = ElectricEel.this.den;
            ElectricEel.this.getNavigation().moveTo(den.getX() + 0.5, den.getY() + 0.3, den.getZ() + 0.5, 0.8);
        }

        @Override
        public void stop() {
            ElectricEel eel = ElectricEel.this;
            eel.lurking = false;
            eel.lurkCooldown = 200 + eel.random.nextInt(400);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ++this.ticks;
            ElectricEel eel = ElectricEel.this;
            Vec3 den = Vec3.atCenterOf(eel.den);
            if (eel.lurking) {
                // Lie still in it.
                eel.setDeltaMovement(eel.getDeltaMovement().scale(0.5));
                return;
            }
            if (eel.position().distanceToSqr(den) < 1.0) {
                eel.getNavigation().stop();
                eel.lurking = true;
                this.stayTicks = this.ticks + 600 + eel.random.nextInt(1200);
            } else if (eel.getNavigation().isDone()) {
                if (eel.position().distanceToSqr(den) < 9.0) {
                    eel.getMoveControl().setWantedPosition(den.x, den.y, den.z, 0.6);
                } else if (this.ticks > 100) {
                    // Can't get back: give up on the den and find another.
                    eel.setDen(null);
                }
            }
        }
    }
}
