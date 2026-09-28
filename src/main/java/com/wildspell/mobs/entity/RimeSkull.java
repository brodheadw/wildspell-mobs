package com.wildspell.mobs.entity;

import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.crypt.LichSouls;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A floating, frost-rimed skull. It jitters around its target, lunges with its jaw open, and
 * spits ice shards from range. Flies freely (no gravity) but still collides with terrain.
 */
public class RimeSkull extends Monster {
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(RimeSkull.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(RimeSkull.class, EntityDataSerializers.INT);
    /** Subtle looks a skull can spawn with: frost tint, crack pattern, eye glow and crown layout. */
    public static final int VARIANTS = 3;

    private int chargeCooldown = 20;
    private int spitCooldown = 60;
    private int gnashCooldown = 40;

    private static final byte EVENT_GNASH = 100;
    private static final int CHOMP_TICKS = 5;
    /** A natural spawn hovers at most this far above whatever is under it. */
    private static final int HOVER_SPAWN_HEIGHT = 3;
    // Client-side gnash animation: ticks left and how many chomps this gnash has.
    private int gnashTicks;
    private int gnashLength;

    public RimeSkull(EntityType<? extends RimeSkull> type, Level level) {
        super(type, level);
        this.moveControl = new SkullMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_VARIANT, 0);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Math.floorMod(variant, VARIANTS));
    }

    /**
     * Natural spawns: a dark open spot, hovering within a few blocks of something below it. Unlike
     * ground mobs it doesn't care what that something is, so it spawns over ice too (vanilla lets
     * nothing but polar bears spawn on ice), and in the cramped ice-floored caverns as well as the open ones.
     */
    public static boolean checkRimeSkullSpawnRules(EntityType<RimeSkull> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()
                || LichSouls.isCleansedZone(level.getLevel(), pos)) {
            return false;
        }
        if (!MobSpawnType.ignoresLightRequirements(spawnType) && !Monster.isDarkEnoughToSpawn(level, pos, random)) {
            return false;
        }
        for (int dy = 1; dy <= HOVER_SPAWN_HEIGHT; ++dy) {
            if (!level.getBlockState(pos.below(dy)).getCollisionShape(level, pos.below(dy)).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        this.setVariant(this.random.nextInt(VARIANTS));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING);
    }

    private void setCharging(boolean charging) {
        this.entityData.set(DATA_CHARGING, charging);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ChargeGoal());
        this.goalSelector.addGoal(3, new SpitGoal());
        this.goalSelector.addGoal(4, new CircleTargetGoal());
        this.goalSelector.addGoal(8, new DriftGoal());
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            this.clientEffects();
            return;
        }
        // Undead ice: daylight sets it burning, like a skeleton.
        if (this.isSunBurnTick()) {
            this.igniteForSeconds(8.0F);
        }
        if (this.chargeCooldown > 0) {
            --this.chargeCooldown;
        }
        if (this.spitCooldown > 0) {
            --this.spitCooldown;
        }
        if (this.gnashCooldown > 0) {
            --this.gnashCooldown;
        }
        LivingEntity target = this.getTarget();
        if (target != null && !this.isCharging() && this.gnashCooldown <= 0 && this.distanceToSqr(target) < 144.0 && this.random.nextInt(25) == 0) {
            this.gnash();
        }
    }

    /** Snap the jaw shut two or three times; the animation and chomp sounds play client-side. */
    private void gnash() {
        this.level().broadcastEntityEvent(this, EVENT_GNASH);
        this.gnashCooldown = 60 + this.random.nextInt(80);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_GNASH) {
            this.gnashLength = (2 + this.random.nextInt(2)) * CHOMP_TICKS;
            this.gnashTicks = this.gnashLength;
        } else {
            super.handleEntityEvent(id);
        }
    }

    private void clientEffects() {
        // Ice motes spilling off the skull and falling away, plus the odd snowflake.
        for (int i = 0; i < 2; ++i) {
            this.level().addParticle(WildspellMobs.FROST_MOTE.get(),
                    this.getX() + (this.random.nextDouble() - 0.5) * 0.7, this.getY() + 0.1 + this.random.nextDouble() * 0.5,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.7,
                    (this.random.nextDouble() - 0.5) * 0.03, -0.01 - this.random.nextDouble() * 0.02, (this.random.nextDouble() - 0.5) * 0.03);
        }
        if (this.random.nextInt(5) == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, -0.02, 0.0);
        }
        if (this.gnashTicks > 0) {
            --this.gnashTicks;
            // The jaw snaps shut at the end of each chomp.
            if ((this.gnashLength - this.gnashTicks) % CHOMP_TICKS == 0) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE,
                        0.7F, 1.5F + this.random.nextFloat() * 0.2F, false);
            }
        }
    }

    /** Jaw openness 0..1 during a gnash (one open-and-snap per chomp), or -1 when not gnashing. */
    public float gnashOpenness(float partialTick) {
        if (this.gnashTicks <= 0) {
            return -1.0F;
        }
        float progress = (this.gnashLength - this.gnashTicks + partialTick) / CHOMP_TICKS;
        return Math.abs(Mth.sin(progress * Mth.PI));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            Frost.freezeSolid(living, 60);
            this.playSound(SoundEvents.PLAYER_HURT_FREEZE, 1.0F, 1.2F);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 2.0F;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.35F;
    }

    private static class SkullMoveControl extends ThrustMoveControl {
        SkullMoveControl(RimeSkull skull) {
            super(skull, 0.05, 0.5);
        }

        @Override
        protected double arrival() {
            return this.mob.getBoundingBox().getSize();
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            if (heading == null) {
                return;
            }
            LivingEntity target = this.mob.getTarget();
            double faceX = target == null ? this.mob.getDeltaMovement().x : target.getX() - this.mob.getX();
            double faceZ = target == null ? this.mob.getDeltaMovement().z : target.getZ() - this.mob.getZ();
            this.mob.setYRot(-((float) Mth.atan2(faceX, faceZ)) * Mth.RAD_TO_DEG);
            this.mob.yBodyRot = this.mob.getYRot();
        }
    }

    /** Lunge at the target's face. The aim locks after a few ticks so the lunge can be dodged. */
    private class ChargeGoal extends Goal {
        private static final int AIM_TICKS = 8;
        private static final int MAX_TICKS = 40;
        private int chargeTicks;

        ChargeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = RimeSkull.this.getTarget();
            return target != null && target.isAlive() && RimeSkull.this.chargeCooldown <= 0
                    && RimeSkull.this.random.nextInt(reducedTickDelay(10)) == 0
                    && RimeSkull.this.distanceToSqr(target) < 144.0 && RimeSkull.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = RimeSkull.this.getTarget();
            return target != null && target.isAlive() && RimeSkull.this.isCharging() && this.chargeTicks < MAX_TICKS;
        }

        @Override
        public void start() {
            this.chargeTicks = 0;
            RimeSkull.this.setCharging(true);
            this.aimAt(RimeSkull.this.getTarget());
            RimeSkull.this.playSound(SoundEvents.PHANTOM_SWOOP, 0.8F, 1.6F);
        }

        @Override
        public void stop() {
            RimeSkull.this.setCharging(false);
            RimeSkull.this.chargeCooldown = 30 + RimeSkull.this.random.nextInt(40);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = RimeSkull.this.getTarget();
            if (target == null) {
                return;
            }
            ++this.chargeTicks;
            RimeSkull.this.getLookControl().setLookAt(target);
            if (RimeSkull.this.getBoundingBox().inflate(0.2).intersects(target.getBoundingBox())) {
                RimeSkull.this.doHurtTarget(target);
                RimeSkull.this.gnash();
                RimeSkull.this.setCharging(false);
            } else if (this.chargeTicks < AIM_TICKS) {
                this.aimAt(target);
            } else if (!RimeSkull.this.getMoveControl().hasWanted()) {
                RimeSkull.this.setCharging(false);
            }
        }

        private void aimAt(LivingEntity target) {
            Vec3 eye = target.getEyePosition();
            RimeSkull.this.getMoveControl().setWantedPosition(eye.x, eye.y - 0.4, eye.z, 1.0);
        }
    }

    /** Spit an ice shard from mid range. Flagless, so it fires while the skull keeps circling. */
    private class SpitGoal extends Goal {
        @Override
        public boolean canUse() {
            LivingEntity target = RimeSkull.this.getTarget();
            if (target == null || !target.isAlive() || RimeSkull.this.spitCooldown > 0 || RimeSkull.this.isCharging()) {
                return false;
            }
            double distance = RimeSkull.this.distanceToSqr(target);
            return distance > 16.0 && distance < 400.0 && RimeSkull.this.getSensing().hasLineOfSight(target);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity target = RimeSkull.this.getTarget();
            FrostShard shard = new FrostShard(RimeSkull.this.level(), RimeSkull.this);
            double dx = target.getX() - RimeSkull.this.getX();
            double dy = target.getY(0.5) - shard.getY();
            double dz = target.getZ() - RimeSkull.this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            shard.shoot(dx, dy + horizontal * 0.12, dz, 1.3F, 4.0F);
            RimeSkull.this.level().addFreshEntity(shard);
            RimeSkull.this.playSound(SoundEvents.SNOW_GOLEM_SHOOT, 1.0F, 0.7F);
            RimeSkull.this.spitCooldown = 60 + RimeSkull.this.random.nextInt(60);
        }
    }

    /** Hover around the target, darting to a new nearby point every second or two. */
    private class CircleTargetGoal extends Goal {
        private int repickTicks;

        CircleTargetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = RimeSkull.this.getTarget();
            return target != null && target.isAlive();
        }

        @Override
        public void start() {
            this.repickTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = RimeSkull.this.getTarget();
            if (target == null) {
                return;
            }
            RimeSkull.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.repickTicks > 0 && RimeSkull.this.getMoveControl().hasWanted()) {
                return;
            }
            this.repickTicks = 15 + RimeSkull.this.random.nextInt(25);
            RimeSkull skull = RimeSkull.this;
            Vec3 targetEye = target.getEyePosition();
            Vec3 spot = ColdEffects.findSpot(12, () -> ColdEffects.ringPoint(skull.random, target.position(), 3.5, 3.0, 1.2 + skull.random.nextDouble() * 2.0),
                    at -> ColdEffects.isOpen(skull.level(), BlockPos.containing(at), 1)
                            && ColdEffects.clearPath(skull, skull.getEyePosition(), at)
                            && ColdEffects.clearPath(skull, at, targetEye));
            if (spot != null) {
                skull.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 0.55);
                return;
            }
            // Boxed in (usually tucked under a ledge): rise to get a new view, else close in.
            BlockPos above = RimeSkull.this.blockPosition().above(2);
            if (ColdEffects.isOpen(RimeSkull.this.level(), above.below(), 2)) {
                RimeSkull.this.getMoveControl().setWantedPosition(RimeSkull.this.getX(), RimeSkull.this.getY() + 2.0, RimeSkull.this.getZ(), 0.55);
            } else {
                RimeSkull.this.getMoveControl().setWantedPosition(targetEye.x, targetEye.y, targetEye.z, 0.55);
            }
        }
    }

    /** Idle drift to a nearby open spot when nothing is being hunted. */
    private class DriftGoal extends Goal {
        DriftGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return RimeSkull.this.getTarget() == null && !RimeSkull.this.getMoveControl().hasWanted()
                    && RimeSkull.this.random.nextInt(reducedTickDelay(7)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            BlockPos origin = RimeSkull.this.blockPosition();
            for (int attempt = 0; attempt < 3; ++attempt) {
                BlockPos pos = origin.offset(RimeSkull.this.random.nextInt(9) - 4, RimeSkull.this.random.nextInt(5) - 2, RimeSkull.this.random.nextInt(9) - 4);
                if (ColdEffects.isOpen(RimeSkull.this.level(), pos, 1)) {
                    RimeSkull.this.getMoveControl().setWantedPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0.25);
                    return;
                }
            }
        }
    }
}
