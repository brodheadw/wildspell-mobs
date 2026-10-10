package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scorpion extends Monster implements GeoEntity {
    public static final int SURFACE = 0;
    public static final int DIGGING = 1;
    public static final int BURIED = 2;
    public static final int EMERGING = 3;
    public static final int DIG_TICKS = 20;
    public static final int EMERGE_TICKS = 8;
    public static final int SENSE_INTERVAL = 4;
    public static final double SENSE_RADIUS = 3.0;
    public static final double SUN_SENSE_RADIUS = 1.25;
    public static final double CHASE_RADIUS = 6.0;
    public static final double SUN_CHASE_RADIUS = 2.0;
    public static final int RETREAT_TICKS = 50;
    public static final int RESTLESS_TICKS = 100;
    public static final int BURROW_SEARCH = 8;
    public static final int SLOW_TICKS = 20;

    public static final TagKey<Block> BURROWS_IN = TagKey.create(Registries.BLOCK, WildspellMobs.id("scorpion_burrows_in"));

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Scorpion.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.scorpion.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.scorpion.walk");
    private static final RawAnimation STING = RawAnimation.begin().thenPlay("animation.scorpion.sting");
    private static final RawAnimation DIG = RawAnimation.begin().thenPlayAndHold("animation.scorpion.dig");
    private static final RawAnimation UNDER = RawAnimation.begin().thenLoop("animation.scorpion.buried");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("animation.scorpion.emerge");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final MotionSense motion = new MotionSense();
    private int phaseTicks;
    private int idleTicks;
    private int retreatTicks;
    @Nullable
    private Vec3 threat;
    @Nullable
    private BlockPos lair;

    public Scorpion(EntityType<? extends Scorpion> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.5)
                .add(Attributes.FOLLOW_RANGE, 8.0);
    }

    public static boolean checkScorpionSpawnRules(EntityType<Scorpion> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(BURROWS_IN) && Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random);
    }

    public static boolean canBurrowAt(LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BURROWS_IN) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty();
    }

    public static boolean stings(LivingEntity other) {
        return other.isAlive() && !(other instanceof Enemy) && !(other instanceof Scarab) && !(other instanceof ArmorStand)
                && !other.isSpectator() && !(other instanceof Player player && player.isCreative());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RetreatGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, false) {
            @Override
            public boolean canUse() {
                return Scorpion.this.fighting() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Scorpion.this.fighting() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new BurrowGoal());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return Scorpion.this.getPhase() == SURFACE && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, SURFACE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_PHASE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        EntityDimensions full = super.getDefaultDimensions(pose);
        return this.getPhase() == BURIED ? full.scale(1.0F, 0.35F) : full;
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        this.phaseTicks = 0;
    }

    public boolean isBuried() {
        return this.getPhase() == BURIED;
    }

    public boolean isRetreating() {
        return this.retreatTicks > 0;
    }

    private boolean fighting() {
        return this.getPhase() == SURFACE && this.retreatTicks == 0;
    }

    public boolean inSun() {
        return this.level().isDay() && this.level().canSeeSky(this.blockPosition());
    }

    public void burrow() {
        this.setTarget(null);
        this.idleTicks = 0;
        this.getNavigation().stop();
        this.setPhase(DIGGING);
        this.playSound(SoundEvents.SAND_HIT, 0.6F, 1.3F);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        if ((spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) && canBurrowAt(level, this.blockPosition())) {
            this.setPhase(BURIED);
        }
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
            return;
        }
        ++this.phaseTicks;
        if (this.retreatTicks > 0) {
            --this.retreatTicks;
        }
        switch (this.getPhase()) {
            case DIGGING -> {
                this.holdStill();
                if (this.phaseTicks % 7 == 0) {
                    this.playSound(SoundEvents.SAND_HIT, 0.4F, 1.4F);
                }
                if (this.phaseTicks >= DIG_TICKS) {
                    this.setPhase(BURIED);
                }
            }
            case BURIED -> {
                this.holdStill();
                if (!canBurrowAt(level, this.blockPosition())) {
                    this.emerge();
                } else if (this.tickCount % SENSE_INTERVAL == 0) {
                    LivingEntity prey = this.sensePrey();
                    if (prey != null) {
                        this.setTarget(prey);
                        this.emerge();
                    }
                }
            }
            case EMERGING -> {
                this.holdStill();
                if (this.phaseTicks >= EMERGE_TICKS) {
                    this.lair = this.blockPosition();
                    this.setPhase(SURFACE);
                }
            }
            default -> this.tickSurface();
        }
    }

    private void tickSurface() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            ++this.idleTicks;
            if (this.retreatTicks == 0 && this.tickCount % SENSE_INTERVAL == 0) {
                LivingEntity prey = this.sensePrey();
                if (prey != null) {
                    this.lair = this.blockPosition();
                    this.setTarget(prey);
                }
            }
            return;
        }
        this.idleTicks = 0;
        double chase = this.inSun() ? SUN_CHASE_RADIUS : CHASE_RADIUS;
        Vec3 from = this.lair != null ? Vec3.atBottomCenterOf(this.lair) : this.position();
        if (!stings(target) || target.distanceToSqr(from) > chase * chase) {
            this.setTarget(null);
        }
    }

    private void emerge() {
        this.setPhase(EMERGING);
        this.playSound(SoundEvents.SAND_BREAK, 0.5F, 1.4F);
    }

    private void holdStill() {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
    }

    @Nullable
    private LivingEntity sensePrey() {
        double radius = this.inSun() ? SUN_SENSE_RADIUS : SENSE_RADIUS;
        var nearby = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SENSE_RADIUS, 1.0, SENSE_RADIUS),
                other -> other != this && other.isAlive());
        this.motion.sense(nearby);
        LivingEntity best = null;
        double bestDistance = radius * radius;
        for (LivingEntity other : nearby) {
            double distance = this.distanceToSqr(other);
            if (distance <= bestDistance && this.feels(other) && stings(other)) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    public boolean feels(LivingEntity other) {
        return other.onGround() && !other.isShiftKeyDown() && this.motion.isMoving(other);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive()) {
            Vec3 from = source.getEntity() != null ? source.getEntity().position() : source.getSourcePosition();
            if (from != null) {
                this.threat = from;
                this.retreatTicks = RETREAT_TICKS;
                this.setTarget(null);
            }
            if (this.getPhase() == BURIED || this.getPhase() == DIGGING) {
                this.emerge();
            }
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            int poison = switch (this.level().getDifficulty()) {
                case HARD -> 140;
                case NORMAL -> 80;
                default -> 40;
            };
            living.addEffect(new MobEffectInstance(MobEffects.POISON, poison), this);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SLOW_TICKS, 1), this);
            this.triggerAnim("action", "sting");
            if (this.inSun()) {
                this.setTarget(null);
            }
        }
        return hit;
    }

    @Override
    public boolean isPushable() {
        return this.getPhase() == SURFACE && super.isPushable();
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SILVERFISH_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SILVERFISH_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SILVERFISH_STEP, 0.1F, 1.4F);
    }

    @Override
    public float getVoicePitch() {
        return 1.3F + this.random.nextFloat() * 0.2F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", this.getPhase() == DIGGING ? BURIED : this.getPhase() == EMERGING ? SURFACE : this.getPhase());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPhase(tag.getInt("Phase"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(switch (this.getPhase()) {
            case DIGGING -> DIG;
            case BURIED -> UNDER;
            case EMERGING -> EMERGE;
            default -> state.isMoving() ? WALK : IDLE;
        })));
        controllers.add(new AnimationController<>(this, "action", 1, state -> PlayState.STOP)
                .triggerableAnim("sting", STING));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private class RetreatGoal extends Goal {
        RetreatGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Scorpion.this.getPhase() == SURFACE && Scorpion.this.retreatTicks > 0 && Scorpion.this.threat != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            this.flee();
        }

        @Override
        public void tick() {
            if (Scorpion.this.getNavigation().isDone()) {
                this.flee();
            }
        }

        private void flee() {
            Vec3 away = DefaultRandomPos.getPosAway(Scorpion.this, 8, 3, Scorpion.this.threat);
            if (away != null) {
                Scorpion.this.getNavigation().moveTo(away.x, away.y, away.z, 1.5);
            }
        }

        @Override
        public void stop() {
            Scorpion.this.getNavigation().stop();
        }
    }

    private class BurrowGoal extends Goal {
        @Nullable
        private BlockPos spot;

        BurrowGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean wanted() {
            return Scorpion.this.getPhase() == SURFACE && Scorpion.this.getTarget() == null && Scorpion.this.retreatTicks == 0
                    && (Scorpion.this.inSun() || Scorpion.this.idleTicks >= RESTLESS_TICKS);
        }

        @Override
        public boolean canUse() {
            if (!this.wanted() || Scorpion.this.random.nextInt(5) != 0) {
                return false;
            }
            BlockPos here = Scorpion.this.blockPosition();
            this.spot = canBurrowAt(Scorpion.this.level(), here) ? here
                    : BlockPos.findClosestMatch(here, BURROW_SEARCH, 3, pos -> canBurrowAt(Scorpion.this.level(), pos)).map(BlockPos::immutable).orElse(null);
            return this.spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.wanted() && this.spot != null;
        }

        @Override
        public void start() {
            Scorpion.this.getNavigation().moveTo(this.spot.getX() + 0.5, this.spot.getY(), this.spot.getZ() + 0.5, 1.0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos here = Scorpion.this.blockPosition();
            if (here.equals(this.spot) || Scorpion.this.getNavigation().isDone() && canBurrowAt(Scorpion.this.level(), here)) {
                Scorpion.this.burrow();
            } else if (Scorpion.this.getNavigation().isDone()) {
                this.spot = null;
            }
        }
    }
}
