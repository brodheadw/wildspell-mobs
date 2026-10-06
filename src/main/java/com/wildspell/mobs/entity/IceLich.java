package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.crypt.LichSouls;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class IceLich extends Monster implements RangedAttackMob, GeoEntity {
    public static final String MINION_TAG = WildspellMobs.MODID + ".lich_minion";
    public static final int ACTION_IDLE = 0;
    public static final int ACTION_CAST = 1;
    public static final int ACTION_SUMMON = 2;
    public static final int ACTION_BEAM = 3;
    public static final int ACTION_BURST = 4;
    public static final int ACTION_BEAM_CHARGE = 5;
    public static final int ACTION_TOSS = 6;
    public static final int ACTION_SPIN = 7;
    public static final int TOSS_TICKS = 30;
    public static final int SPIN_TICKS = 30;
    public static final int SPIN_RELEASE = 25;
    private static final int TOSS_CHANCE = 300;
    public static final int MAX_MINIONS = 3;
    public static final int SUMMON_CHANNEL = 30;
    public static final int BEAM_WINDUP = 20;
    public static final int BEAM_ACTIVE = 40;
    public static final double BEAM_RANGE = 20.0;
    private static final double BEAM_TURN = Math.toRadians(2.5);
    private static final int BURST_WINDUP = 30;
    private static final double BURST_RADIUS = 2.5;
    private static final double BLINK_DISTANCE = 18.0;
    private static final int BLINK_AFTER_UNSEEN = 60;
    private static final int IDLE_LIMIT = 60;
    private static final int FULL_CAP_RECHECK = 40;
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(IceLich.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Vector3f> DATA_BEAM_DIR = SynchedEntityData.defineId(IceLich.class, EntityDataSerializers.VECTOR3);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ice_lich.idle");
    private static final RawAnimation CAST = RawAnimation.begin().thenPlay("animation.ice_lich.cast");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenLoop("animation.ice_lich.summon");
    private static final RawAnimation BEAM = RawAnimation.begin().thenLoop("animation.ice_lich.beam");
    private static final RawAnimation BURST = RawAnimation.begin().thenPlay("animation.ice_lich.burst");
    private static final RawAnimation TOSS = RawAnimation.begin().thenPlay("animation.ice_lich.toss");
    private static final RawAnimation SPIN = RawAnimation.begin().thenPlay("animation.ice_lich.spin");

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int boltCooldown = 40;
    private int summonCooldown = 100;
    private int beamCooldown = 160;
    private int burstCooldown = 100;
    private int spinCooldown = 120;
    private int blinkCooldown;
    private int actionTicks;
    private int burstTicks;
    private int unseenTicks;
    private int idleSeconds;
    private Vec3 burstAt = Vec3.ZERO;
    private Vec3 beamDir = Vec3.ZERO;
    @Nullable
    private UUID soulId;
    @Nullable
    private BlockPos legacyHome;
    private boolean lastForm;
    @Nullable
    private UUID hunted;

    public IceLich(EntityType<? extends IceLich> type, Level level) {
        super(type, level);
        this.moveControl = new HoverMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 150;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Nullable
    public static IceLich summon(ServerLevel level, Vec3 at, @Nullable UUID soul) {
        IceLich lich = level.getDifficulty() == Difficulty.PEACEFUL ? null : WildspellMobs.ICE_LICH.get().create(level);
        if (lich == null) {
            return null;
        }
        lich.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        lich.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        lich.soulId = soul;
        if (!level.addFreshEntity(lich)) {
            return null;
        }
        ColdEffects.soulBurst(level, at.add(0.0, 1.0, 0.0), 0.8, 1.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.6F);
        level.playSound(null, at.x, at.y, at.z, WildspellMobs.ICE_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 0.7F);
        return lich;
    }

    @Nullable
    public static IceLich summonLastForm(ServerLevel level, Vec3 at, UUID soul) {
        IceLich lich = summon(level, at, soul);
        if (lich != null) {
            lich.lastForm = true;
            level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0, at.z, 60, 0.6, 1.0, 0.6, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        return lich;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, (byte) ACTION_IDLE);
        builder.define(DATA_BEAM_DIR, new Vector3f());
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void startAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, (byte) action);
        this.actionTicks = ticks;
    }

    public boolean isEnraged() {
        return this.lastForm || this.getHealth() < this.getMaxHealth() / 2.0F;
    }

    public boolean isBound() {
        LichSouls.Soul soul = this.soul();
        return soul != null && !soul.burned();
    }

    public boolean isLastForm() {
        return this.lastForm;
    }

    @Nullable
    public UUID soulId() {
        return this.soulId;
    }

    public void hunt(Player player) {
        this.hunted = player.getUUID();
        this.setTarget(player);
    }

    public static boolean isMinion(Entity entity) {
        return entity.getTags().contains(MINION_TAG);
    }

    public boolean isOwnMinion(Entity entity) {
        return entity.getTags().contains(this.ownerTag());
    }

    private String ownerTag() {
        return MINION_TAG + "." + this.getStringUUID();
    }

    @Nullable
    private LichSouls.Soul soul() {
        return this.soulId != null && this.level() instanceof ServerLevel level ? LichSouls.get(level).soul(this.soulId) : null;
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
            int action = this.getAction();
            if (action == ACTION_BEAM || action == ACTION_BEAM_CHARGE && this.tickCount % 2 == 0) {
                this.drawBeam(action == ACTION_BEAM);
            }
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (this.tickCount % 20 == 0) {
            this.checkAnchor();
            this.updateHunt();
            if (this.isRemoved()) {
                return;
            }
        }
        this.boltCooldown--;
        this.summonCooldown--;
        this.beamCooldown--;
        this.burstCooldown--;
        this.spinCooldown--;
        this.blinkCooldown--;
        LivingEntity target = this.getTarget();
        boolean hasTarget = target != null && target.isAlive();
        this.setAggressive(hasTarget);
        boolean canSee = hasTarget && this.getSensing().hasLineOfSight(target);
        this.unseenTicks = canSee ? 0 : this.unseenTicks + 1;
        if (this.actionTicks > 0) {
            this.tickAction(hasTarget ? target : null);
        } else if (hasTarget) {
            this.chooseAction(target, canSee);
        } else if (this.random.nextInt(TOSS_CHANCE * 2) == 0) {
            this.startAction(ACTION_TOSS, TOSS_TICKS);
        }
        if (this.burstTicks > 0) {
            this.tickBurst((ServerLevel) this.level());
        }
    }

    private void chooseAction(LivingEntity target, boolean canSee) {
        boolean enraged = this.isEnraged();
        double distance = this.distanceTo(target);
        if (this.blinkCooldown <= 0 && (distance > BLINK_DISTANCE || this.unseenTicks > BLINK_AFTER_UNSEEN)) {
            this.blinkNear(target);
        } else if (this.summonCooldown <= 0 && this.minions().size() < MAX_MINIONS) {
            this.holdStill();
            this.startAction(ACTION_SUMMON, SUMMON_CHANNEL);
            this.playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1.5F, 0.7F);
        } else if (this.summonCooldown <= 0) {
            this.summonCooldown = FULL_CAP_RECHECK;
        } else if (this.spinCooldown <= 0 && canSee && distance > 5.0 && distance < 28.0) {
            this.holdStill();
            this.startAction(ACTION_SPIN, SPIN_TICKS);
            this.spinCooldown = enraged ? 180 : 260;
            this.playSound(SoundEvents.TRIDENT_RIPTIDE_1.value(), 1.2F, 0.7F);
        } else if (this.beamCooldown <= 0 && canSee && distance < BEAM_RANGE - 2.0) {
            this.holdStill();
            this.setBeamDir(this.aimAt(target));
            this.startAction(ACTION_BEAM_CHARGE, BEAM_WINDUP + BEAM_ACTIVE);
            this.playSound(SoundEvents.BEACON_ACTIVATE, 1.5F, 1.8F);
        } else if (enraged && this.burstCooldown <= 0 && this.burstTicks == 0) {
            this.burstAt = target.position();
            this.burstTicks = BURST_WINDUP;
            this.burstCooldown = 160;
            this.startAction(ACTION_BURST, 20);
            this.playSound(SoundEvents.EVOKER_PREPARE_ATTACK, 1.5F, 0.6F);
        } else if (this.boltCooldown <= 0 && canSee) {
            this.performRangedAttack(target, 1.0F);
            this.boltCooldown = enraged ? 30 : 50;
            this.startAction(ACTION_CAST, 15);
        } else if (this.random.nextInt(TOSS_CHANCE) == 0) {
            this.startAction(ACTION_TOSS, TOSS_TICKS);
        }
    }

    private void tickAction(@Nullable LivingEntity target) {
        int action = this.getAction();
        --this.actionTicks;
        if (action == ACTION_SUMMON) {
            ServerLevel level = (ServerLevel) this.level();
            double angle = this.tickCount * 0.5;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX() + Math.cos(angle) * 1.2, this.getY() + 0.2, this.getZ() + Math.sin(angle) * 1.2,
                    1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(1.1), this.getZ(), 3, 0.4, 0.2, 0.4, 0.02);
            if (this.actionTicks == 0 && target != null) {
                this.summonMinions(target);
            }
        } else if (action == ACTION_BEAM_CHARGE || action == ACTION_BEAM) {
            this.tickBeam(target);
        } else if (action == ACTION_SPIN) {
            this.tickSpin(target);
        }
        if (this.actionTicks <= 0) {
            if (action == ACTION_SUMMON) {
                this.summonCooldown = this.isEnraged() ? 240 : 320;
            } else if (action == ACTION_BEAM_CHARGE || action == ACTION_BEAM) {
                this.beamCooldown = 200;
            }
            this.startAction(ACTION_IDLE, 0);
        }
    }

    private void holdStill() {
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 1.0);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.swing(InteractionHand.MAIN_HAND);
        for (int i = -1; i <= 1; ++i) {
            FrostShard shard = new FrostShard(this.level(), this);
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            double dy = shard.lobTo(target, dx, dz, 1.6F);
            double angle = Math.toRadians(8.0 * i);
            double sx = dx * Math.cos(angle) - dz * Math.sin(angle);
            double sz = dx * Math.sin(angle) + dz * Math.cos(angle);
            shard.shoot(sx, dy, sz, 1.6F, 2.0F);
            this.level().addFreshEntity(shard);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.2F, 1.3F);
    }

    private List<Mob> minions() {
        String owner = this.ownerTag();
        return this.level().getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(32.0), m -> m.getTags().contains(owner) && m.isAlive());
    }

    // The frozen dead it raises: our Frozen Zombie until Minecraft has its own, the Frostbite, from 26.4.
    private static EntityType<? extends Mob> frozenDead() {
//? if <26.4 {
        return WildspellMobs.FROZEN_ZOMBIE.get();
//?} else {
/*        return EntityType.FROSTBITE;*/
//?}
    }

    private void summonMinions(LivingEntity target) {
        ServerLevel level = (ServerLevel) this.level();
        int room = MAX_MINIONS - this.minions().size();
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
            Mob minion = i < zombies ? frozenDead().create(level) : WildspellMobs.RIME_SKULL.get().create(level);
            if (minion == null) {
                continue;
            }
            minion.moveTo(spot.getX() + 0.5, spot.getY() + (i < zombies ? 0.0 : 1.5), spot.getZ() + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.MOB_SUMMONED, null);
            minion.addTag(MINION_TAG);
            minion.addTag(this.ownerTag());
            minion.setTarget(target);
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.SNOWFLAKE, minion.getX(), minion.getY(0.5), minion.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.7F);
    }

    @Nullable
    private BlockPos findSummonSpot(ServerLevel level) {
        for (int attempt = 0; attempt < 12; ++attempt) {
            BlockPos column = BlockPos.containing(ColdEffects.ringPoint(this.random, this.position(), 3.0, 3.0, 0.0));
            for (int dy = 2; dy >= -8; --dy) {
                BlockPos pos = column.above(dy);
                if (level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                        && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above())) {
                    return pos;
                }
            }
        }
        return null;
    }

    private Vec3 beamOrigin() {
        return this.position().add(0.0, this.getBbHeight() * 0.7, 0.0);
    }

    private Vec3 aimAt(LivingEntity target) {
        return target.getEyePosition().subtract(this.beamOrigin()).normalize();
    }

    private void setBeamDir(Vec3 dir) {
        this.beamDir = dir;
        this.entityData.set(DATA_BEAM_DIR, dir.toVector3f());
    }

    private Vec3 beamEnd(Vec3 origin, Vec3 dir) {
        return this.level().clip(new ClipContext(origin, origin.add(dir.scale(BEAM_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getLocation();
    }

    private void tickBeam(@Nullable LivingEntity target) {
        ServerLevel level = (ServerLevel) this.level();
        int elapsed = BEAM_WINDUP + BEAM_ACTIVE - this.actionTicks;
        Vec3 origin = this.beamOrigin();
        if (target != null) {
            Vec3 want = this.aimAt(target);
            this.setBeamDir(elapsed < BEAM_WINDUP ? want : turnToward(this.beamDir, want, BEAM_TURN));
            this.getLookControl().setLookAt(origin.add(this.beamDir.scale(10.0)));
        }
        if (elapsed == BEAM_WINDUP) {
            this.startAction(ACTION_BEAM, this.actionTicks);
        }
        if (elapsed < BEAM_WINDUP || elapsed % 10 != 0) {
            return;
        }
        Vec3 end = this.beamEnd(origin, this.beamDir);
        level.sendParticles(ColdEffects.ICE_CHIPS, end.x, end.y, end.z, 8, 0.15, 0.15, 0.15, 0.05);
        this.playSound(SoundEvents.POWDER_SNOW_STEP, 1.5F, 0.6F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(origin, end).inflate(1.0),
                e -> e != this && !isMinion(e) && e.getBoundingBox().inflate(0.3).clip(origin, end).isPresent())) {
            if (victim.hurt(this.damageSources().indirectMagic(this, this), 4.0F)) {
                Frost.add(victim, 30, 20);
            }
        }
    }

    private void drawBeam(boolean firing) {
        Vec3 dir = new Vec3(this.entityData.get(DATA_BEAM_DIR));
        if (dir.lengthSqr() < 0.5) {
            return;
        }
        Vec3 origin = this.beamOrigin();
        double length = this.beamEnd(origin, dir).distanceTo(origin);
        for (double d = firing ? 0.5 : 1.0; d < length; d += firing ? 0.4 : 1.5) {
            Vec3 p = origin.add(dir.scale(d));
            this.level().addParticle(firing ? WildspellMobs.FROST_MOTE.get() : ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 0.0, 0.0, 0.0);
        }
    }

    private void tickSpin(@Nullable LivingEntity target) {
        ServerLevel level = (ServerLevel) this.level();
        int elapsed = SPIN_TICKS - this.actionTicks;
        if (elapsed < SPIN_RELEASE) {
            double angle = elapsed * 0.9;
            Vec3 tip = this.position().add(Math.cos(angle) * 0.9, this.getBbHeight() + 0.6, Math.sin(angle) * 0.9);
            level.sendParticles(WildspellMobs.FROST_MOTE.get(), tip.x, tip.y, tip.z, 2, 0.05, 0.05, 0.05, 0.0);
            if (elapsed % 6 == 0) {
                this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
            }
        } else if (elapsed == SPIN_RELEASE && target != null) {
            Vec3 from = this.staffTip();
            Vec3 to = target.getEyePosition().subtract(from);
            FrostOrb orb = new FrostOrb(level, this, to.normalize());
            orb.setPos(from);
            level.addFreshEntity(orb);
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.5F, 0.5F);
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 0.6F);
        }
    }

    private Vec3 staffTip() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 right = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F);
        return this.position().add(forward.scale(2.0)).add(right.scale(0.45)).add(0.0, 1.7, 0.0);
    }

    static Vec3 turnToward(Vec3 from, Vec3 to, double maxAngle) {
        double angle = Math.acos(Mth.clamp(from.dot(to), -1.0, 1.0));
        if (angle <= maxAngle) {
            return to;
        }
        return from.lerp(to, maxAngle / angle).normalize();
    }

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
        level.sendParticles(ColdEffects.ICE_CHIPS, this.burstAt.x, this.burstAt.y + 0.5, this.burstAt.z, 60, BURST_RADIUS / 2, 0.6, BURST_RADIUS / 2, 0.2);
        level.playSound(null, this.burstAt.x, this.burstAt.y, this.burstAt.z, WildspellMobs.ICE_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        AABB area = new AABB(this.burstAt, this.burstAt).inflate(BURST_RADIUS, 1.5, BURST_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != this && !isMinion(e))) {
            if (victim.hurt(this.damageSources().indirectMagic(this, this), 8.0F)) {
                Frost.add(victim, 100, 20);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), this);
                victim.push(0.0, 0.5, 0.0);
            }
        }
    }

    private void blinkNear(LivingEntity target) {
        this.blinkCooldown = 100;
        Vec3 spot = ColdEffects.findSpot(16, () -> ColdEffects.ringPoint(this.random, target.position(), 7.0, 5.0, 1.0 + this.random.nextInt(3)),
                at -> ColdEffects.isOpen(this.level(), BlockPos.containing(at), 3)
                        && ColdEffects.clearPath(this, at.add(0.0, this.getEyeHeight(), 0.0), target.getEyePosition()));
        if (spot != null) {
            this.teleportWithFrost(spot);
        }
    }

    private void teleportWithFrost(Vec3 to) {
        ServerLevel level = (ServerLevel) this.level();
        ColdEffects.soulBurst(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), 0.4, 1.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.5F);
        this.teleportTo(to.x, to.y, to.z);
        this.setDeltaMovement(Vec3.ZERO);
        this.holdStill();
        ColdEffects.soulBurst(level, to.add(0.0, 1.2, 0.0), 0.4, 1.0);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.HOSTILE, 1.2F, 0.6F);
        this.unseenTicks = 0;
    }

    public void recall(Vec3 altar, Player intruder) {
        this.teleportWithFrost(altar);
        this.hunt(intruder);
    }

    private void checkAnchor() {
        ServerLevel level = (ServerLevel) this.level();
        if (this.legacyHome != null && level.isLoaded(this.legacyHome)) {
            this.soulId = level.getBlockEntity(this.legacyHome) instanceof PhylacteryBlockEntity phylactery ? phylactery.soul(level).id : null;
            this.legacyHome = null;
        }
        LichSouls.Soul soul = this.soul();
        if (this.soulId == null || this.lastForm) {
            return;
        }
        if (soul == null) {
            this.loseAnchor();
        } else if (soul.burned() || !soul.claim(this)) {
            this.vanish();
        }
    }

    public void loseAnchor() {
        if (this.soulId == null) {
            return;
        }
        this.soulId = null;
        this.playSound(SoundEvents.WITHER_HURT, 2.0F, 0.5F);
        ColdEffects.soulBurst((ServerLevel) this.level(), this.position().add(0.0, this.getBbHeight() * 0.6, 0.0), 0.4, 0.8);
    }

    private void updateHunt() {
        Player prey = this.hunted != null && ((ServerLevel) this.level()).getEntity(this.hunted) instanceof Player player ? player : null;
        if (prey != null && !PhylacteryBlockEntity.isPrey(prey)) {
            prey = null;
        }
        if (prey == null) {
            this.hunted = null;
        }
        LichSouls.Soul soul = this.lastForm ? null : this.soul();
        if (soul != null && !soul.inAltar() && soul.carrier() != null
                && ((ServerLevel) this.level()).getEntity(soul.carrier()) instanceof Player bearer && PhylacteryBlockEntity.isPrey(bearer)) {
            prey = bearer;
            this.hunted = bearer.getUUID();
        }
        if (soul != null && soul.inAltar()) {
            if (prey != null && prey.distanceToSqr(Vec3.atCenterOf(soul.anchor())) > PhylacteryBlockEntity.leash() * PhylacteryBlockEntity.leash()) {
                this.retreat();
                return;
            }
            if (this.getTarget() == null && prey == null) {
                if (++this.idleSeconds >= IDLE_LIMIT) {
                    this.retreat();
                    return;
                }
            } else {
                this.idleSeconds = 0;
            }
        }
        if (prey != null && this.getTarget() == null) {
            this.setTarget(prey);
        }
    }

    private void retreat() {
        LichSouls.Soul soul = this.soul();
        if (soul != null) {
            soul.onLichRetreated(this);
        }
        this.vanish();
    }

    public void vanish() {
        ServerLevel level = (ServerLevel) this.level();
        ColdEffects.soulBurst(level, this.position().add(0.0, this.getBbHeight() * 0.5, 0.0), 0.4, 1.0);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
        this.shatterMinions(level);
        this.discard();
    }

    @Override
    public void die(DamageSource source) {
        LichSouls.Soul soul = this.soul();
        if (this.level() instanceof ServerLevel level && soul != null && !soul.burned() && !this.lastForm) {
            this.discorporate(level, soul, source);
            return;
        }
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.shatterMinions(level);
            if (soul != null) {
                soul.fall(level, this.position());
            }
        }
    }

    private void discorporate(ServerLevel level, LichSouls.Soul soul, DamageSource source) {
        UUID next = source.getEntity() instanceof Player player ? player.getUUID() : this.hunted;
        if (source.getEntity() instanceof Player) {
            ItemStack fragment = new ItemStack(WildspellMobs.CROWN_FRAGMENT.get());
            fragment.set(WildspellMobs.SOUL.get(), soul.id);
            this.spawnAtLocation(fragment);
        }
        level.sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.getY(0.5), this.getZ(), 60, 0.4, 1.0, 0.4, 0.2);
        ColdEffects.tellNearby(level, this.getBoundingBox().inflate(32.0), Component.translatable("message.wildspellmobs.lich_soul_flees"));
        level.addFreshEntity(new LichWisp(level, this.position().add(0.0, this.getBbHeight() * 0.6, 0.0), soul.id, next));
        soul.onLichDiscorporated(this);
        this.vanish();
    }

    private void shatterMinions(ServerLevel level) {
        String owner = this.ownerTag();
        for (Mob minion : level.getEntitiesOfClass(Mob.class, this.getBoundingBox().inflate(48.0), m -> m.getTags().contains(owner))) {
            ColdEffects.shatter(level, minion, 1.2F);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FIRE)) {
            amount *= 1.5F;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.getAction() == ACTION_SUMMON && source.getEntity() != null && this.isAlive()) {
            this.startAction(ACTION_IDLE, 0);
            this.summonCooldown = 160;
            this.playSound(SoundEvents.SHIELD_BREAK, 1.5F, 0.6F);
            ((ServerLevel) this.level()).sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.getY(0.6), this.getZ(), 30, 0.4, 0.6, 0.4, 0.15);
        }
        return hurt;
    }

    @Override
    public void checkDespawn() {
        if (this.lastForm && this.level().getDifficulty() == Difficulty.PEACEFUL && this.level() instanceof ServerLevel level) {
            LichSouls.Soul soul = this.soul();
            this.shatterMinions(level);
            if (soul != null) {
                soul.fall(level, this.position());
            }
            this.discard();
            return;
        }
        super.checkDespawn();
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.soulId != null) {
            tag.putUUID("Soul", this.soulId);
        }
        if (this.legacyHome != null) {
            tag.putLong("Phylactery", this.legacyHome.asLong());
        }
        tag.putBoolean("LastForm", this.lastForm);
        if (this.hunted != null) {
            tag.putUUID("Hunting", this.hunted);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.soulId = tag.hasUUID("Soul") ? tag.getUUID("Soul") : null;
        this.legacyHome = tag.contains("Phylactery", CompoundTag.TAG_LONG) ? BlockPos.of(tag.getLong("Phylactery")) : null;
        this.lastForm = tag.getBoolean("LastForm");
        this.hunted = tag.hasUUID("Hunting") ? tag.getUUID("Hunting") : null;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> state.setAndContinue(switch (this.getAction()) {
            case ACTION_CAST -> CAST;
            case ACTION_SUMMON -> SUMMON;
            case ACTION_BEAM, ACTION_BEAM_CHARGE -> BEAM;
            case ACTION_BURST -> BURST;
            case ACTION_TOSS -> TOSS;
            case ACTION_SPIN -> SPIN;
            default -> IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private static class HoverMoveControl extends ThrustMoveControl {
        HoverMoveControl(IceLich lich) {
            super(lich, 0.02, 1.0);
        }

        @Override
        protected Vec3 drift(Vec3 motion) {
            return motion.multiply(1.0, 0.9, 1.0).add(0.0, Mth.sin(this.mob.tickCount * 0.08F) * 0.004, 0.0);
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            this.faceTarget();
        }
    }

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
            int action = IceLich.this.getAction();
            if (--this.repickTicks > 0 || action == ACTION_SUMMON || action == ACTION_BEAM_CHARGE || action == ACTION_BEAM || action == ACTION_SPIN) {
                return;
            }
            this.repickTicks = 40 + IceLich.this.random.nextInt(40);
            IceLich lich = IceLich.this;
            Vec3 spot = ColdEffects.findSpot(10, () -> ColdEffects.ringPoint(lich.random, target.position(), 6.0, 3.0, 2.0 + lich.random.nextDouble() * 2.0),
                    at -> ColdEffects.isOpen(lich.level(), BlockPos.containing(at), 3)
                            && ColdEffects.clearPath(lich, lich.getEyePosition(), at)
                            && ColdEffects.clearPath(lich, at.add(0.0, lich.getEyeHeight(), 0.0), target.getEyePosition()));
            if (spot != null) {
                lich.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 1.0);
            }
        }
    }
}
