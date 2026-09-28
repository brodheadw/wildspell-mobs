package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.flytrap.FlytrapBlock;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class FlytrapHead extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(FlytrapHead.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_SIZE = SynchedEntityData.defineId(FlytrapHead.class, EntityDataSerializers.BYTE);
    public static final byte ACTION_IDLE = 0;
    public static final byte ACTION_LUNGE = 1;
    public static final byte ACTION_HOLD = 2;

    public static final int SIZE_SMALL = 0;
    public static final int SIZE_MEDIUM = 1;
    public static final int SIZE_BIG = 2;
    private static final float[] SCALE = {0.45F, 1.0F, 1.5F};
    private static final double[] REACH = {1.5, 3.0, 4.0};
    private static final double[] HEALTH = {4.0, 20.0, 30.0};
    private static final double[] BITE = {2.0, 4.0, 6.0};

    public static final TagKey<EntityType<?>> HOSTILE_GROWTH = TagKey.create(Registries.ENTITY_TYPE, WildspellMobs.id("hostile_growth"));
    public static final TagKey<EntityType<?>> TINY_PREY = TagKey.create(Registries.ENTITY_TYPE, WildspellMobs.id("flytrap_sprout_prey"));
    private static final float TINY = 0.7F;

    public static final double SENSE_RADIUS = 8.0;
    private static final int SENSE_INTERVAL = 4;
    public static final int LUNGE_TICKS = 8;
    public static final int HOLD_TICKS = 24;
    private static final int HOLD_SLOWNESS = 4;
    private static final int MISS_COOLDOWN = 20;
    private static final int HOLD_COOLDOWN = 30;
    private static final int CUT_COOLDOWN = 60;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.flytrap_head.idle");
    private static final RawAnimation LUNGE = RawAnimation.begin().thenPlay("animation.flytrap_head.lunge");
    private static final RawAnimation HOLD = RawAnimation.begin().thenLoop("animation.flytrap_head.hold");
    private static final RawAnimation WITHER = RawAnimation.begin().thenPlayAndHold("animation.flytrap_head.wither");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private final MotionSense motion = new MotionSense();
    private int lungeTicks;
    private int holdTicks;
    private int cooldown;
    @Nullable
    private LivingEntity held;

    @Nullable
    private BlockPos anchor;
    private int stage;
    private int slot;
    private Vec3 offset = Vec3.ZERO;

    public FlytrapHead(EntityType<? extends FlytrapHead> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH[SIZE_MEDIUM])
                .add(Attributes.ATTACK_DAMAGE, BITE[SIZE_MEDIUM])
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, SENSE_RADIUS);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, ACTION_IDLE);
        builder.define(DATA_SIZE, (byte) SIZE_MEDIUM);
    }

    public byte getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void setAction(byte action) {
        this.entityData.set(DATA_ACTION, action);
    }

    public int getSize() {
        return this.entityData.get(DATA_SIZE);
    }

    private void setSize(int size) {
        this.entityData.set(DATA_SIZE, (byte) size);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH[size]);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BITE[size]);
        this.setHealth(this.getMaxHealth());
        this.xpReward = size == SIZE_SMALL ? 1 : 5;
        this.refreshDimensions();
    }

    public float getScale() {
        return SCALE[this.getSize()];
    }

    public double getReach() {
        return REACH[this.getSize()];
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_SIZE.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(this.getScale());
    }

    public void anchorTo(BlockPos anchor, int stage, FlytrapBlock.HeadSlot slot) {
        this.anchor = anchor.immutable();
        this.stage = stage;
        this.slot = slot.slot();
        this.offset = slot.offset();
        this.setSize(slot.size());
        Vec3 at = this.home();
        float yaw = slot.slot() == 1 ? 90.0F : slot.slot() == 2 ? -90.0F : this.random.nextFloat() * 360.0F;
        this.moveTo(at.x, at.y, at.z, yaw, 0.0F);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    @Nullable
    public BlockPos getAnchor() {
        return this.anchor;
    }

    public int getStage() {
        return this.stage;
    }

    public int getSlot() {
        return this.slot;
    }

    private Vec3 home() {
        return Vec3.atLowerCornerOf(this.anchor).add(this.offset);
    }

    private boolean plantStands() {
        if (this.anchor == null) {
            return false;
        }
        BlockState state = this.level().getBlockState(this.anchor);
        return FlytrapBlock.isFlytrap(state) && state.getValue(FlytrapBlock.AGE) == this.stage;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide && !this.isDeadOrDying()) {
            if (this.anchor == null || this.level().isLoaded(this.anchor) && !this.plantStands()) {
                this.discard();
                return;
            }
            Vec3 home = this.home();
            if (this.position().distanceToSqr(home) > 1.0E-6) {
                this.setPos(home);
            }
        }
        super.tick();
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide && this.plantStands()) {
            this.level().destroyBlock(this.anchor, true, source.getEntity());
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                FlytrapHead.this.yBodyRot = FlytrapHead.this.yHeadRot;
            }
        };
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % SENSE_INTERVAL == 0) {
            this.sense();
        }
        if (this.cooldown > 0) {
            --this.cooldown;
        }
        LivingEntity victim = this.held;
        if (victim != null) {
            this.tickHold(victim);
            return;
        }
        if (this.lungeTicks > 0) {
            this.tickLunge();
            return;
        }
        LivingEntity prey = this.cooldown <= 0 ? this.findPrey(this.getReach()) : null;
        if (prey != null) {
            this.lungeTicks = LUNGE_TICKS;
            this.setTarget(prey);
            this.setAction(ACTION_LUNGE);
            this.getLookControl().setLookAt(prey, 60.0F, 60.0F);
            this.playSound(SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 1.0F, 0.7F / this.getScale());
            return;
        }
        LivingEntity tracked = this.findPrey(SENSE_RADIUS);
        if (tracked != null) {
            this.getLookControl().setLookAt(tracked, 20.0F, 30.0F);
        }
    }

    private void sense() {
        this.motion.sense(this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SENSE_RADIUS),
                e -> e != this && e.isAlive()));
    }

    public boolean isMoving(LivingEntity other) {
        return this.motion.isMoving(other);
    }

    public boolean isPrey(LivingEntity other) {
        return this.isSensed(other) && this.reachSqr(other) <= this.getReach() * this.getReach();
    }

    private double reachSqr(LivingEntity other) {
        return other.getBoundingBox().distanceToSqr(this.getEyePosition());
    }

    private boolean isSensed(LivingEntity other) {
        return other != this && other.isAlive() && !other.getType().is(HOSTILE_GROWTH) && !other.isSpectator()
                && !(other instanceof Player player && player.isCreative()) && !other.isShiftKeyDown()
                && this.hunts(other) && this.isMoving(other) && this.getSensing().hasLineOfSight(other);
    }

    public boolean hunts(LivingEntity other) {
        return this.getSize() != SIZE_SMALL || other.getType().is(TINY_PREY) || other.getBbWidth() <= TINY && other.getBbHeight() <= TINY;
    }

    @Nullable
    private LivingEntity findPrey(double range) {
        LivingEntity nearest = null;
        double best = range * range;
        for (LivingEntity other : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), this::isSensed)) {
            double distance = this.reachSqr(other);
            if (distance <= best) {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    private void tickLunge() {
        LivingEntity target = this.getTarget();
        if (target != null) {
            this.getLookControl().setLookAt(target, 60.0F, 60.0F);
        }
        if (--this.lungeTicks > 0) {
            return;
        }
        this.playSound(SoundEvents.EVOKER_FANGS_ATTACK, this.getScale(), 1.3F / this.getScale());
        double reach = this.getReach() + 0.5;
        if (target != null && target.isAlive() && this.reachSqr(target) <= reach * reach && this.hasLineOfSight(target)
                && this.doHurtTarget(target) && target.isAlive()) {
            this.seize(target);
        } else {
            this.setAction(ACTION_IDLE);
            this.setTarget(null);
            this.cooldown = MISS_COOLDOWN;
        }
    }

    public void seize(LivingEntity victim) {
        this.lungeTicks = 0;
        this.held = victim;
        this.holdTicks = HOLD_TICKS;
        this.setTarget(victim);
        this.setAction(ACTION_HOLD);
        this.tickHold(victim);
    }

    private void tickHold(LivingEntity victim) {
        double letGo = this.getReach() + 2.0;
        if (!victim.isAlive() || victim.isRemoved() || this.reachSqr(victim) > letGo * letGo || --this.holdTicks <= 0) {
            this.release(false);
            return;
        }
        this.getLookControl().setLookAt(victim, 60.0F, 60.0F);
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, HOLD_SLOWNESS, false, false), this);
        Vec3 jaws = this.position().add(Vec3.directionFromRotation(0.0F, this.getYHeadRot()).scale(0.6 * this.getScale()));
        Vec3 toJaws = new Vec3(jaws.x - victim.getX(), 0.0, jaws.z - victim.getZ());
        double distance = toJaws.length();
        if (distance > 0.9 * this.getScale()) {
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.4, 1.0, 0.4).add(toJaws.scale(Math.min(0.25, distance * 0.15) / distance)));
            victim.hurtMarked = true;
        }
    }

    public void release(boolean cut) {
        LivingEntity victim = this.held;
        this.held = null;
        this.holdTicks = 0;
        this.setAction(ACTION_IDLE);
        this.setTarget(null);
        this.cooldown = cut ? CUT_COOLDOWN : HOLD_COOLDOWN;
        if (cut && victim != null) {
            MobEffectInstance slowness = victim.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if (slowness != null && slowness.getAmplifier() == HOLD_SLOWNESS && slowness.getDuration() <= 5) {
                victim.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            }
        }
    }

    @Nullable
    public LivingEntity getHeld() {
        return this.held;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.SHEARS) && this.held != null) {
            if (!this.level().isClientSide) {
                this.release(true);
                this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, 0.8F);
                stack.hurtAndBreak(1, player, getSlotForHand(hand));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean fire = source.is(DamageTypeTags.IS_FIRE);
        boolean wasBurning = this.getRemainingFireTicks() > 0;
        if (fire) {
            amount *= 2.0F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            if (fire && !wasBurning && !this.fireImmune()) {
                this.igniteForSeconds(8.0F);
            }
            if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker) {
                this.motion.markMoving(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.anchor != null) {
            tag.putLong("Anchor", this.anchor.asLong());
        }
        tag.putInt("Stage", this.stage);
        tag.putInt("Slot", this.slot);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.stage = tag.getInt("Stage");
        if (tag.contains("Anchor") && this.stage >= 0 && this.stage <= FlytrapBlock.MAX_AGE) {
            this.anchor = BlockPos.of(tag.getLong("Anchor"));
            float health = this.getHealth();
            for (FlytrapBlock.HeadSlot slot : FlytrapBlock.slots(this.stage)) {
                if (slot.slot() == tag.getInt("Slot")) {
                    this.slot = slot.slot();
                    this.offset = slot.offset();
                    this.setSize(slot.size());
                }
            }
            this.setHealth(health);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BIG_DRIPLEAF_TILT_UP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GRASS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BIG_DRIPLEAF_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 0.6F * this.getScale();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() / this.getScale();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 2, state -> state.setAndContinue(
                this.isDeadOrDying() ? WITHER : switch (this.getAction()) {
                    case ACTION_LUNGE -> LUNGE;
                    case ACTION_HOLD -> HOLD;
                    default -> IDLE;
                })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
