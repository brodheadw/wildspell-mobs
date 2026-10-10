package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.item.CreatureBottleItem;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scarab extends PathfinderMob implements GeoEntity {
    public static final int ROLLING = 0;
    public static final int DANCING = 1;
    public static final int DIGGING = 2;
    public static final int DANCE_TICKS = 50;
    public static final int DIG_TICKS = 40;
    public static final int STUCK_TICKS = 20;
    public static final double LEG = 12.0;
    public static final long DAWN_START = 22500L;
    public static final long DAWN_END = 1500L;
    public static final float EAST = -90.0F;
    public static final float WEST = 90.0F;

    public static final TagKey<Block> SPAWNABLE_ON = TagKey.create(Registries.BLOCK, WildspellMobs.id("scarab_spawnable_on"));

    private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(Scarab.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DUNG = SynchedEntityData.defineId(Scarab.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.scarab.idle");
    private static final RawAnimation ROLL = RawAnimation.begin().thenLoop("animation.scarab.roll");
    private static final RawAnimation DANCE = RawAnimation.begin().thenPlay("animation.scarab.dance");
    private static final RawAnimation DIG = RawAnimation.begin().thenPlayAndHold("animation.scarab.dig");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private float heading;
    private int phaseTicks;
    private int stuckTicks;
    private double rolled;
    private boolean blocked;
    private int turns;

    public Scarab(EntityType<? extends Scarab> type, Level level) {
        super(type, level);
        this.heading = this.random.nextFloat() * 360.0F;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 2.0)
                .add(Attributes.MOVEMENT_SPEED, 0.08)
                .add(Attributes.STEP_HEIGHT, 0.25);
    }

    public static boolean checkScarabSpawnRules(EntityType<Scarab> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.below()).is(SPAWNABLE_ON) || !level.getBlockState(pos).isAir() || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        return !level.canSeeSkyFromBelowWater(pos) || isDawn(level.dayTime());
    }

    public static boolean isDawn(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        return time >= DAWN_START || time < DAWN_END;
    }

    public static float bearing(Level level, BlockPos pos, RandomSource random, float current, boolean blocked) {
        if (blocked) {
            return Mth.wrapDegrees(current + (random.nextBoolean() ? 1 : -1) * (70 + random.nextInt(80)));
        }
        long time = Math.floorMod(level.getDayTime(), 24000L);
        if (level.canSeeSky(pos) && (isDawn(time) || time < 12000L)) {
            float sun = isDawn(time) || time < 6000L ? EAST : WEST;
            return Mth.wrapDegrees(sun + (random.nextFloat() - 0.5F) * 30.0F);
        }
        return Mth.wrapDegrees(current + (random.nextFloat() - 0.5F) * 50.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, ROLLING);
        builder.define(DATA_DUNG, false);
    }

    public int getPhase() {
        return this.entityData.get(DATA_PHASE);
    }

    private void setPhase(int phase) {
        this.entityData.set(DATA_PHASE, phase);
        this.phaseTicks = 0;
    }

    public boolean isDung() {
        return this.entityData.get(DATA_DUNG);
    }

    public void setDung(boolean dung) {
        this.entityData.set(DATA_DUNG, dung);
    }

    public float getHeading() {
        return this.heading;
    }

    public void setHeading(float heading) {
        this.heading = heading;
    }

    public int getTurns() {
        return this.turns;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        this.setDung(!level.getBlockState(this.blockPosition().below()).is(SPAWNABLE_ON));
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ++this.phaseTicks;
        switch (this.getPhase()) {
            case DANCING -> {
                this.holdStill();
                if (this.phaseTicks >= DANCE_TICKS) {
                    this.heading = bearing(this.level(), this.blockPosition(), this.random, this.heading, this.blocked);
                    this.setYRot(this.heading);
                    this.yBodyRot = this.heading;
                    this.rolled = 0.0;
                    this.stuckTicks = 0;
                    ++this.turns;
                    this.setPhase(ROLLING);
                }
            }
            case DIGGING -> {
                this.holdStill();
                if (this.phaseTicks % 10 == 0) {
                    this.playSound(SoundEvents.SAND_HIT, 0.3F, 1.6F);
                }
                if (this.phaseTicks >= DIG_TICKS) {
                    this.discard();
                }
            }
            default -> this.roll();
        }
    }

    private void roll() {
        if (this.tickCount % 20 == 0 && this.morningOver()) {
            this.setPhase(DIGGING);
            return;
        }
        Vec3 dir = Vec3.directionFromRotation(0.0F, this.heading);
        if (this.blockedAhead(dir) || this.stuckTicks >= STUCK_TICKS) {
            this.dance(true);
            return;
        }
        if (this.rolled >= LEG) {
            this.dance(false);
            return;
        }
        this.getMoveControl().setWantedPosition(this.getX() + dir.x * 2.0, this.getY(), this.getZ() + dir.z * 2.0, 1.0);
        double moved = this.getDeltaMovement().horizontalDistance();
        this.rolled += moved;
        this.stuckTicks = this.tickCount > 5 && moved < 0.004 ? this.stuckTicks + 1 : 0;
    }

    private boolean morningOver() {
        BlockPos pos = this.blockPosition();
        return !this.isPersistenceRequired() && this.random.nextInt(10) == 0 && !isDawn(this.level().getDayTime())
                && this.level().canSeeSky(pos) && this.level().getBlockState(pos.below()).is(SPAWNABLE_ON);
    }

    private void dance(boolean blocked) {
        this.blocked = blocked;
        this.holdStill();
        this.setPhase(DANCING);
    }

    private void holdStill() {
        this.getNavigation().stop();
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
        this.setZza(0.0F);
        this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
    }

    private boolean blockedAhead(Vec3 dir) {
        BlockPos next = BlockPos.containing(this.getX() + dir.x * 0.6, this.getY() + 0.1, this.getZ() + dir.z * 0.6);
        if (next.equals(this.blockPosition())) {
            return false;
        }
        Level level = this.level();
        if (!level.getFluidState(next).isEmpty() || !level.getFluidState(next.below()).isEmpty()
                || !level.getBlockState(next).getCollisionShape(level, next).isEmpty()) {
            return true;
        }
        return level.getBlockState(next.below()).getCollisionShape(level, next.below()).isEmpty()
                && level.getBlockState(next.below(2)).getCollisionShape(level, next.below(2)).isEmpty();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(Items.GLASS_BOTTLE) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        ItemStack bottled = new ItemStack(WildspellMobs.BOTTLED_SCARAB.get());
        Bucketable.saveDefaultDataToBucketTag(this, bottled);
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bottled, tag -> tag.putBoolean("Dung", this.isDung()));
        return CreatureBottleItem.catchIn(this, player, hand, bottled);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
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
    public float getVoicePitch() {
        return 1.8F + this.random.nextFloat() * 0.2F;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dung", this.isDung());
        tag.putFloat("Heading", this.heading);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setDung(tag.getBoolean("Dung"));
        if (tag.contains("Heading")) {
            this.heading = tag.getFloat("Heading");
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(switch (this.getPhase()) {
            case DANCING -> DANCE;
            case DIGGING -> DIG;
            default -> state.isMoving() ? ROLL : IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
