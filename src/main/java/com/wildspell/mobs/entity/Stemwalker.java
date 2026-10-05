package com.wildspell.mobs.entity;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Stemwalker extends Monster implements GeoEntity {
    public static final int DORMANT = 0;
    public static final int AWAKE = 1;
    public static final int SINKING = 2;
    public static final int RISING = 3;
    public static final int LEANING = 4;
    public static final int SLAMMING = 5;
    public static final int RECOVERING = 6;

    public static final double NETWORK = 32.0;
    public static final double NOTICE = 10.0;
    public static final double HUNT = 48.0;
    public static final double SLAM_REACH = 6.5;
    public static final double SLAM_RADIUS = 2.5;
    public static final double SLAM_NEAREST = 3.5;
    public static final double SLAM_FARTHEST = 9.5;
    public static final double WATCH_DOT = 0.8;
    public static final float SLAM_DAMAGE = 9.0F;
    public static final int SINK_TICKS = 30;
    public static final int RISE_TICKS = 30;
    public static final int LEAN_TICKS = 20;
    public static final int SLAM_TICKS = 5;
    public static final int RECOVER_TICKS = 20;
    public static final int COOLDOWN_TICKS = 40;
    public static final int FORGET_TICKS = 300;

    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(Stemwalker.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_WATCHED = SynchedEntityData.defineId(Stemwalker.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.stemwalker.idle");
    private static final RawAnimation AWAKE_LOOP = RawAnimation.begin().thenPlay("animation.stemwalker.wake").thenLoop("animation.stemwalker.awake");
    private static final RawAnimation SINK = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.sink");
    private static final RawAnimation RISE = RawAnimation.begin().thenPlay("animation.stemwalker.rise").thenLoop("animation.stemwalker.awake");
    private static final RawAnimation LEAN = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.lean");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.slam");
    private static final RawAnimation RECOVER = RawAnimation.begin().thenPlay("animation.stemwalker.recover").thenLoop("animation.stemwalker.awake");
    private static final RawAnimation BREATHE = RawAnimation.begin().thenLoop("animation.stemwalker.breathe");
    private static final RawAnimation HURT = RawAnimation.begin().thenPlay("animation.stemwalker.hurt");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlayAndHold("animation.stemwalker.die");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int actionTicks;
    private int cooldown;
    private int lostTicks;
    @Nullable
    private Player quarry;
    @Nullable
    private BlockPos risingAt;

    public Stemwalker(EntityType<? extends Stemwalker> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, HUNT);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, DORMANT);
        builder.define(DATA_WATCHED, false);
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void setAction(int action) {
        this.entityData.set(DATA_ACTION, action);
        this.actionTicks = 0;
    }

    public boolean isWatched() {
        return this.entityData.get(DATA_WATCHED);
    }

    public int actionTicks() {
        return this.actionTicks;
    }

    @Nullable
    public Player getQuarry() {
        return this.quarry;
    }

    public static boolean sees(Player player, Stemwalker stem) {
        if (!player.isAlive() || player.isSpectator() || player.distanceTo(stem) > HUNT) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        for (double y = 0.5; y < stem.getBbHeight(); y += 1.0) {
            Vec3 toward = new Vec3(stem.getX(), stem.getY() + y, stem.getZ()).subtract(eye);
            if (toward.lengthSqr() < 1.0E-4 || view.dot(toward.normalize()) > WATCH_DOT) {
                return player.hasLineOfSight(stem);
            }
        }
        return false;
    }

    public boolean watchedByAnyone() {
        for (Player player : this.level().players()) {
            if (sees(player, this)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hunts(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative();
    }

    public void wake(Player quarry) {
        this.quarry = quarry;
        this.lostTicks = 0;
        if (this.getAction() == DORMANT) {
            this.setAction(AWAKE);
            this.playSound(SoundEvents.WOODEN_DOOR_OPEN, 1.2F, 0.5F + this.random.nextFloat() * 0.1F);
        }
    }

    public static int wakeGrove(ServerLevel level, Vec3 at, Player quarry) {
        int woke = 0;
        for (Stemwalker stem : level.getEntitiesOfClass(Stemwalker.class, new AABB(at, at).inflate(NETWORK))) {
            if (stem.isAlive() && stem.position().distanceTo(at) <= NETWORK) {
                stem.wake(quarry);
                ++woke;
            }
        }
        return woke;
    }

    @Override
    public void tick() {
        super.tick();
        this.setYBodyRot(this.getYRot());
        this.setYHeadRot(this.getYRot());
        if (this.level() instanceof ServerLevel level && this.isAlive() && !this.isNoAi()) {
            this.step(level, this.watchedByAnyone());
        }
    }

    public void step(ServerLevel level, boolean watched) {
        this.entityData.set(DATA_WATCHED, watched);
        if (this.cooldown > 0) {
            --this.cooldown;
        }
        if (this.getAction() == DORMANT) {
            if (this.tickCount % 10 == 0) {
                Player near = level.getNearestPlayer(this.getX(), this.getY(), this.getZ(), NOTICE, p -> p instanceof Player pl && hunts(pl));
                if (near != null) {
                    wakeGrove(level, this.position(), near);
                }
            }
            return;
        }
        if (this.quarry == null || !hunts(this.quarry) || this.quarry.level() != level || this.quarry.distanceTo(this) > HUNT) {
            if (++this.lostTicks >= FORGET_TICKS && this.getAction() == AWAKE) {
                this.quarry = null;
                this.setAction(DORMANT);
            }
            if (this.getAction() == AWAKE || this.quarry == null) {
                return;
            }
        } else {
            this.lostTicks = 0;
        }
        if (watched && this.getAction() != SLAMMING) {
            return;
        }
        ++this.actionTicks;
        switch (this.getAction()) {
            case AWAKE -> this.choose(level);
            case SINKING -> {
                if (this.actionTicks % 6 == 0) {
                    this.burrowEffects(level, this.blockPosition());
                }
                if (this.actionTicks >= SINK_TICKS) {
                    this.surface(level);
                }
            }
            case RISING -> {
                if (this.actionTicks % 6 == 0) {
                    this.burrowEffects(level, this.blockPosition());
                }
                if (this.actionTicks >= RISE_TICKS) {
                    this.setAction(AWAKE);
                }
            }
            case LEANING -> {
                if (this.actionTicks >= LEAN_TICKS) {
                    this.setAction(SLAMMING);
                }
            }
            case SLAMMING -> {
                if (this.actionTicks >= SLAM_TICKS) {
                    this.slam(level);
                    this.setAction(RECOVERING);
                }
            }
            case RECOVERING -> {
                if (this.actionTicks >= RECOVER_TICKS) {
                    this.cooldown = COOLDOWN_TICKS;
                    this.setAction(AWAKE);
                }
            }
            default -> {
            }
        }
    }

    private void face(Vec3 target) {
        float yaw = (float) (Mth.atan2(target.z - this.getZ(), target.x - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
    }

    public Vec3 impactPoint() {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.getYRot());
        return this.position().add(ahead.scale(SLAM_REACH));
    }

    private void choose(ServerLevel level) {
        if (this.cooldown > 0 || this.quarry == null) {
            return;
        }
        double away = this.quarry.position().subtract(this.position()).horizontalDistance();
        if (away >= SLAM_NEAREST && away <= SLAM_FARTHEST) {
            this.face(this.quarry.position());
            this.setAction(LEANING);
            this.playSound(SoundEvents.WOODEN_DOOR_OPEN, 1.5F, 0.4F);
            this.playSound(SoundEvents.BAMBOO_WOOD_BREAK, 1.0F, 0.5F);
            return;
        }
        BlockPos spot = this.findRisingSpot(level, this.quarry);
        if (spot != null) {
            this.risingAt = spot;
            this.setAction(SINKING);
            this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.5F);
        }
    }

    @Nullable
    public BlockPos findRisingSpot(ServerLevel level, Player quarry) {
        Vec3 look = quarry.getViewVector(1.0F);
        double behind = Math.atan2(-look.z, -look.x);
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 24; ++attempt) {
            double angle = behind + (this.random.nextDouble() - 0.5) * (attempt < 16 ? Math.PI * 0.7 : Math.PI * 2.0);
            double distance = SLAM_REACH + (this.random.nextDouble() - 0.5) * 2.0;
            int x = Mth.floor(quarry.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(quarry.getZ() + Math.sin(angle) * distance);
            BlockPos ground = this.groundNear(level, x, Mth.floor(quarry.getY()), z);
            if (ground == null) {
                continue;
            }
            BlockPos stand = ground.above();
            Vec3 at = Vec3.atBottomCenterOf(stand);
            if (!level.noCollision(this, this.getType().getDimensions().makeBoundingBox(at))) {
                continue;
            }
            Vec3 toward = at.add(0.0, 1.0, 0.0).subtract(quarry.getEyePosition()).normalize();
            if (quarry.getViewVector(1.0F).dot(toward) <= WATCH_DOT) {
                return stand;
            }
            if (fallback == null) {
                fallback = stand;
            }
        }
        return fallback;
    }

    @Nullable
    private BlockPos groundNear(ServerLevel level, int x, int y, int z) {
        for (int dy = 4; dy >= -8; --dy) {
            BlockPos pos = new BlockPos(x, y + dy, z);
            if (rootable(level.getBlockState(pos)) && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) {
                return pos;
            }
        }
        return null;
    }

    public static boolean rootable(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.NYLIUM) || state.is(Blocks.MOSS_BLOCK);
    }

    private void surface(ServerLevel level) {
        BlockPos spot = this.risingAt;
        this.risingAt = null;
        if (spot == null) {
            this.setAction(AWAKE);
            return;
        }
        this.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, this.getYRot(), 0.0F);
        if (this.quarry != null) {
            this.face(this.quarry.position());
        }
        this.setAction(RISING);
        this.playSound(SoundEvents.ROOTED_DIRT_BREAK, 1.5F, 0.6F);
        this.burrowEffects(level, spot);
    }

    private void burrowEffects(ServerLevel level, BlockPos at) {
        BlockState ground = level.getBlockState(at.below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5,
                12, 0.4, 0.05, 0.4, 0.1);
    }

    public int slam(ServerLevel level) {
        Vec3 impact = this.impactPoint();
        int struck = 0;
        AABB area = new AABB(impact, impact).inflate(SLAM_RADIUS, 3.0, SLAM_RADIUS);
        List<LivingEntity> under = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && !(e instanceof Stemwalker) && e.position().subtract(impact).horizontalDistance() <= SLAM_RADIUS);
        for (LivingEntity victim : under) {
            if (victim.hurt(this.damageSources().mobAttack(this), SLAM_DAMAGE)) {
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60), this);
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120), this);
                ++struck;
            }
        }
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, impact.x, impact.y + 0.5, impact.z, 80, SLAM_RADIUS * 0.6, 0.8, SLAM_RADIUS * 0.6, 0.02);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()),
                impact.x, impact.y + 0.3, impact.z, 30, 0.8, 0.2, 0.8, 0.15);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.MUD_BREAK, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.SPORE_BLOSSOM_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        return struck;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && this.level() instanceof ServerLevel level) {
            this.triggerAnim("hit", "hurt");
            if (source.getEntity() instanceof Player player && hunts(player)) {
                wakeGrove(level, this.position(), player);
            }
        }
        return hurt;
    }

    public static boolean checkStemwalkerSpawnRules(EntityType<Stemwalker> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return rootable(level.getBlockState(pos.below()))
                && (MobSpawnType.ignoresLightRequirements(spawnType) || Monster.isDarkEnoughToSpawn(level, pos, random));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FUNGUS_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected float getSoundVolume() {
        return 1.4F;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F + this.random.nextFloat() * 0.1F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Cooldown", this.cooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.cooldown = tag.getInt("Cooldown");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> {
            state.getController().setAnimationSpeed(this.isWatched() && this.getAction() != SLAMMING ? 0.0 : 1.0);
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DIE);
            }
            return state.setAndContinue(switch (this.getAction()) {
                case AWAKE -> AWAKE_LOOP;
                case SINKING -> SINK;
                case RISING -> RISE;
                case LEANING -> LEAN;
                case SLAMMING -> SLAM;
                case RECOVERING -> RECOVER;
                default -> IDLE;
            });
        }));
        controllers.add(new AnimationController<>(this, "breath", 0, state -> {
            state.getController().setAnimationSpeed(this.isWatched() ? 0.0 : 1.0);
            return state.setAndContinue(BREATHE);
        }));
        controllers.add(new AnimationController<>(this, "hit", 0, state -> PlayState.STOP).triggerableAnim("hurt", HURT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
