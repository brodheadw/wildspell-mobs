package com.wildspell.mobs.entity;

import com.wildspell.mobs.grove.SporeheartBlock;
import com.wildspell.mobs.grove.SporeheartBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Stemwalker extends Monster implements GeoEntity {
    public static final int WALKING = 0;
    public static final int EMERGING = 1;
    public static final int SINKING = 2;
    public static final int GROUND_TICKS = 30;
    public static final int LEASH = 24;
    public static final double TETHER = 40.0;

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Stemwalker.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stemwalker.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.stemwalker.walk");
    private static final RawAnimation EMERGE = RawAnimation.begin().thenPlay("animation.stemwalker.emerge");
    private static final RawAnimation SINK = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.sink");
    private static final RawAnimation CRUMBLE = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.crumble");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.stemwalker.attack");
    private static final RawAnimation SHRUG = RawAnimation.begin().thenPlay("animation.stemwalker.shrug");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos heart;
    private int phaseTicks;

    public Stemwalker(EntityType<? extends Stemwalker> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            public boolean canUse() {
                return !Stemwalker.this.grounded() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Stemwalker.this.grounded() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.8));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, entity -> entity instanceof Player p && hunts(p)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, WALKING);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        this.phaseTicks = 0;
    }

    public boolean grounded() {
        return this.getPhase() != WALKING;
    }

    public static boolean hunts(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    @Nullable
    public BlockPos getHeart() {
        return this.heart;
    }

    public void bindTo(BlockPos pos) {
        this.heart = pos.immutable();
        this.restrictTo(this.heart, LEASH);
    }

    public void emerge() {
        this.setPhase(EMERGING);
    }

    @Nullable
    public SporeheartBlockEntity heartEntity() {
        if (this.heart == null || !this.level().isLoaded(this.heart)) {
            return null;
        }
        return this.level().getBlockEntity(this.heart) instanceof SporeheartBlockEntity found ? found : null;
    }

    public boolean shielded() {
        SporeheartBlockEntity found = this.heartEntity();
        return found != null && found.alive();
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level) || !this.isAlive()) {
            return;
        }
        ++this.phaseTicks;
        if (this.grounded()) {
            this.getNavigation().stop();
            this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
            if (this.phaseTicks % 6 == 0) {
                level.sendParticles(ParticleTypes.MYCELIUM, this.getX(), this.getY() + 0.1, this.getZ(), 10, 0.4, 0.05, 0.4, 0.0);
            }
            if (this.getPhase() == EMERGING && this.phaseTicks >= GROUND_TICKS) {
                this.setPhase(WALKING);
            } else if (this.getPhase() == SINKING && this.phaseTicks >= GROUND_TICKS) {
                this.discard();
            }
            return;
        }
        if (this.heart != null && this.tickCount % 20 == 0 && level.isLoaded(this.heart)) {
            BlockState state = level.getBlockState(this.heart);
            if (!(state.getBlock() instanceof SporeheartBlock)) {
                this.crumble();
            } else if (!state.getValue(SporeheartBlock.ACTIVE) || this.position().distanceTo(Vec3.atCenterOf(this.heart)) > TETHER) {
                this.sink();
            }
        }
    }

    public void sink() {
        if (this.getPhase() != SINKING) {
            this.setPhase(SINKING);
            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.5F);
        }
    }

    public void crumble() {
        this.heart = null;
        this.clearRestriction();
        this.kill();
    }

    public static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 span = to.subtract(from);
        int steps = Math.max(1, (int) (span.length() / 0.6));
        for (int i = 0; i <= steps; ++i) {
            Vec3 at = from.add(span.scale(i / (double) steps));
            level.sendParticles(ParticleTypes.MYCELIUM, at.x, at.y + 0.1, at.z, 3, 0.15, 0.02, 0.15, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, at.x, at.y + 0.3, at.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && this.shielded() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.level() instanceof ServerLevel level && this.heart != null && this.invulnerableTime <= 10) {
                this.invulnerableTime = 20;
                this.triggerAnim("action", "shrug");
                this.playSound(SoundEvents.FUNGUS_BREAK, 1.2F, 0.5F);
                level.playSound(null, this.heart, SoundEvents.WART_BLOCK_HIT, this.getSoundSource(), 1.5F, 0.4F);
                trail(level, this.position(), Vec3.atBottomCenterOf(this.heart));
                if (source.getEntity() instanceof Player player && hunts(player)) {
                    this.setTarget(player);
                }
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            this.triggerAnim("action", "attack");
        }
        return hit;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.heart != null || super.removeWhenFarAway(distance);
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? SoundEvents.FUNGUS_PLACE : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FUNGUS_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WART_BLOCK_BREAK;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.FUNGUS_STEP, 0.6F, 0.6F);
    }

    @Override
    public float getVoicePitch() {
        return 0.5F + this.random.nextFloat() * 0.15F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.heart != null) {
            tag.put("Heart", NbtUtils.writeBlockPos(this.heart));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Heart").ifPresent(this::bindTo);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(CRUMBLE);
            }
            return state.setAndContinue(switch (this.getPhase()) {
                case EMERGING -> EMERGE;
                case SINKING -> SINK;
                default -> state.isMoving() ? WALK : IDLE;
            });
        }));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK)
                .triggerableAnim("shrug", SHRUG));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
