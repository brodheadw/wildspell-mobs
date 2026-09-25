package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Ice Lich, a floating skeletal boss summoned from Enchanted Ice. It keeps its distance, fires
 * volleys of frost shards, and raises Frozen Zombies and Rime Skulls around itself. Below half health
 * it also calls up telegraphed ice bursts under its target. Its minions shatter when it dies.
 */
public class IceLich extends Monster implements RangedAttackMob {
    /** Entity tag marking mobs the lich raised; they shatter with it and its shards pass through them. */
    public static final String MINION_TAG = WildspellMobs.MODID + ".lich_minion";
    private static final int MAX_MINIONS = 6;
    private static final int BURST_WINDUP = 30;
    private static final double BURST_RADIUS = 2.5;
    private static final BlockParticleOption ICE_CHIPS = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState());

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true);
    private int boltCooldown = 40;
    private int summonCooldown = 60;
    private int burstCooldown = 100;
    private int burstTicks;
    private Vec3 burstAt = Vec3.ZERO;

    public IceLich(EntityType<? extends IceLich> type, Level level) {
        super(type, level);
        this.moveControl = new HoverMoveControl();
        this.setNoGravity(true);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    /** Below half health the lich fights harder: faster volleys and summons, plus ice bursts. */
    public boolean isEnraged() {
        return this.getHealth() < this.getMaxHealth() / 2.0F;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new KeepDistanceGoal());
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(WildspellMobs.FROST_MOTE.get(), this.getRandomX(0.7), this.getRandomY(), this.getRandomZ(0.7),
                        (this.random.nextDouble() - 0.5) * 0.03, -0.02, (this.random.nextDouble() - 0.5) * 0.03);
            }
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.boltCooldown--;
        this.summonCooldown--;
        this.burstCooldown--;
        LivingEntity target = this.getTarget();
        this.setAggressive(target != null);
        if (target != null && target.isAlive()) {
            boolean enraged = this.isEnraged();
            if (this.boltCooldown <= 0 && this.getSensing().hasLineOfSight(target)) {
                this.performRangedAttack(target, 1.0F);
                this.boltCooldown = enraged ? 30 : 50;
            }
            if (this.summonCooldown <= 0) {
                this.summonMinions(target);
                this.summonCooldown = enraged ? 240 : 320;
            }
            if (enraged && this.burstCooldown <= 0 && this.burstTicks == 0) {
                this.burstAt = target.position();
                this.burstTicks = BURST_WINDUP;
                this.burstCooldown = 160;
                this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.6F);
            }
        }
        if (this.burstTicks > 0) {
            this.tickBurst((ServerLevel) this.level());
        }
    }

    /** A volley of three frost shards, fanned slightly. */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        for (int i = -1; i <= 1; ++i) {
            FrostShard shard = new FrostShard(this.level(), this);
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double flightTicks = horizontal / 1.6;
            double dy = target.getY(0.5) - shard.getY() + 0.5 * FrostShard.GRAVITY * flightTicks * flightTicks;
            double angle = Math.toRadians(8.0 * i);
            double sx = dx * Math.cos(angle) - dz * Math.sin(angle);
            double sz = dx * Math.sin(angle) + dz * Math.cos(angle);
            shard.shoot(sx, dy, sz, 1.6F, 2.0F);
            this.level().addFreshEntity(shard);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 1.3F);
    }

    /** Raise Frozen Zombies and a Rime Skull around itself, up to a cap. */
    private void summonMinions(LivingEntity target) {
        ServerLevel level = (ServerLevel) this.level();
        List<Mob> minions = level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(32.0), m -> m.getTags().contains(MINION_TAG) && m.isAlive());
        int room = MAX_MINIONS - minions.size();
        if (room <= 0) {
            return;
        }
        int zombies = Math.min(room, 2);
        int skulls = Math.min(room - zombies, 1);
        for (int i = 0; i < zombies + skulls; ++i) {
            BlockPos spot = this.findSummonSpot(level);
            if (spot == null) {
                continue;
            }
            Mob minion = i < zombies ? WildspellMobs.FROZEN_ZOMBIE.get().create(level) : WildspellMobs.RIME_SKULL.get().create(level);
            if (minion == null) {
                continue;
            }
            minion.moveTo(spot.getX() + 0.5, spot.getY() + (i < zombies ? 0.0 : 1.5), spot.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.MOB_SUMMONED, null);
            minion.addTag(MINION_TAG);
            minion.setTarget(target);
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.SNOWFLAKE, minion.getX(), minion.getY(0.5), minion.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        }
        this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.7F);
    }

    /** An open spot with floor, 3-6 blocks from the lich. */
    private BlockPos findSummonSpot(ServerLevel level) {
        for (int attempt = 0; attempt < 12; ++attempt) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            double radius = 3.0 + this.random.nextDouble() * 3.0;
            BlockPos column = BlockPos.containing(this.getX() + Math.cos(angle) * radius, this.getY(), this.getZ() + Math.sin(angle) * radius);
            for (int dy = 2; dy >= -8; --dy) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)
                        && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** Frost gathers under the target, then erupts. */
    private void tickBurst(ServerLevel level) {
        --this.burstTicks;
        if (this.burstTicks % 2 == 0) {
            for (int i = 0; i < 12; ++i) {
                double angle = i / 12.0 * Math.PI * 2.0;
                level.sendParticles(ParticleTypes.SNOWFLAKE, this.burstAt.x + Math.cos(angle) * BURST_RADIUS, this.burstAt.y + 0.1,
                        this.burstAt.z + Math.sin(angle) * BURST_RADIUS, 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
        if (this.burstTicks > 0) {
            return;
        }
        level.sendParticles(ICE_CHIPS, this.burstAt.x, this.burstAt.y + 0.5, this.burstAt.z, 60, BURST_RADIUS / 2, 0.6, BURST_RADIUS / 2, 0.2);
        level.playSound(null, this.burstAt.x, this.burstAt.y, this.burstAt.z, WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        AABB area = new AABB(this.burstAt, this.burstAt).inflate(BURST_RADIUS, 1.5, BURST_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && !e.getTags().contains(MINION_TAG))) {
            if (victim.hurt(this.damageSources().indirectMagic(this, this), 8.0F)) {
                victim.setTicksFrozen(Math.max(victim.getTicksFrozen(), victim.getTicksRequiredToFreeze() + 100));
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
                victim.push(0.0, 0.5, 0.0);
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            for (Mob minion : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48.0), m -> m.getTags().contains(MINION_TAG))) {
                level.sendParticles(ICE_CHIPS, minion.getX(), minion.getY(0.5), minion.getZ(), 25, 0.3, 0.6, 0.3, 0.15);
                minion.playSound(WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), 1.0F, 1.2F);
                minion.discard();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 1.5F;
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
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.6F;
    }

    /** Hover-steering: accelerate toward the wanted point, drifting gently up and down. */
    private class HoverMoveControl extends MoveControl {
        HoverMoveControl() {
            super(IceLich.this);
        }

        @Override
        public void tick() {
            IceLich lich = IceLich.this;
            Vec3 motion = lich.getDeltaMovement().multiply(1.0, 0.9, 1.0).add(0.0, Mth.sin(lich.tickCount * 0.08F) * 0.004, 0.0);
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                Vec3 toWanted = new Vec3(this.wantedX - lich.getX(), this.wantedY - lich.getY(), this.wantedZ - lich.getZ());
                double distance = toWanted.length();
                if (distance < 0.5) {
                    this.operation = MoveControl.Operation.WAIT;
                } else {
                    motion = motion.add(toWanted.scale(this.speedModifier * 0.02 / distance));
                }
            }
            lich.setDeltaMovement(motion);
            LivingEntity target = lich.getTarget();
            if (target != null) {
                lich.setYRot(-((float) Mth.atan2(target.getX() - lich.getX(), target.getZ() - lich.getZ())) * Mth.RAD_TO_DEG);
                lich.yBodyRot = lich.getYRot();
            }
        }
    }

    /** Hang back 6-9 blocks from the target and a few blocks above it, shifting position now and then. */
    private class KeepDistanceGoal extends Goal {
        private int repickTicks;

        KeepDistanceGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return IceLich.this.getTarget() != null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = IceLich.this.getTarget();
            if (target == null) {
                return;
            }
            IceLich.this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (--this.repickTicks > 0) {
                return;
            }
            this.repickTicks = 40 + IceLich.this.random.nextInt(40);
            for (int attempt = 0; attempt < 10; ++attempt) {
                double angle = IceLich.this.random.nextDouble() * Math.PI * 2.0;
                double radius = 6.0 + IceLich.this.random.nextDouble() * 3.0;
                Vec3 spot = new Vec3(target.getX() + Math.cos(angle) * radius, target.getY() + 2.0 + IceLich.this.random.nextDouble() * 2.0,
                        target.getZ() + Math.sin(angle) * radius);
                BlockPos pos = BlockPos.containing(spot);
                if (IceLich.this.level().isEmptyBlock(pos) && IceLich.this.level().isEmptyBlock(pos.above()) && IceLich.this.level().isEmptyBlock(pos.above(2))) {
                    IceLich.this.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 1.0);
                    return;
                }
            }
        }
    }
}
