package com.wildspell.mobs.entity;

import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.item.CreatureBottleItem;
import com.wildspell.mobs.moth.LuminousMoss;
import com.wildspell.mobs.moth.MothGlowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LuminousMoth extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> DATA_PERCHED = SynchedEntityData.defineId(LuminousMoth.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Direction> DATA_PERCH_FACE = SynchedEntityData.defineId(LuminousMoth.class, EntityDataSerializers.DIRECTION);
    public static final double WALL_GAP = 0.28;

    public static final TagKey<Item> LURES = TagKey.create(Registries.ITEM, WildspellMobs.id("luminous_moth_lures"));
    public static final TagKey<Block> PERCHES = TagKey.create(Registries.BLOCK, WildspellMobs.id("luminous_moth_perches"));

    public static final int TRAIL_LIGHT = 12;
    public static final int HOME_LIGHT = 15;
    public static final int RING_LIGHT = 11;
    public static final int HOME_RADIUS = 5;
    public static final int DARK_BELOW = 8;
    public static final int MOSS_RADIUS = 2;
    public static final int KEEPS_MOSS_LIT = 4;
    private static final int MOSS_INTERVAL = 200;

    public static final double DISTURB_RADIUS = 5.0;
    public static final double STARTLE_RADIUS = 1.5;
    public static final double SNEAK_STARTLE_RADIUS = 1.0;
    public static final double FLEE_RADIUS = 6.0;
    public static final double SNEAK_FLEE_RADIUS = 3.0;
    private static final double FLEE_SPEED = 2.0;

    @Nullable
    private BlockPos home;
    private int perchCooldown = 20 + this.random.nextInt(40);
    private int flushTicks;
    @Nullable
    private Vec3 flushFrom;
    private final MotionSense motion = new MotionSense();

    public LuminousMoth(EntityType<? extends LuminousMoth> type, Level level) {
        super(type, level);
        this.moveControl = new MothMoveControl(this);
        this.setNoGravity(true);
        this.setPathfindingMalus(PathType.WATER, -1.0F);
        this.setPathfindingMalus(PathType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathType.LAVA, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0)
                .add(Attributes.FLYING_SPEED, 0.08)
                .add(Attributes.MOVEMENT_SPEED, 0.1)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    public static boolean checkMothSpawnRules(EntityType<LuminousMoth> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty() && !level.canSeeSkyFromBelowWater(pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PERCHED, false);
        builder.define(DATA_PERCH_FACE, Direction.UP);
    }

    public boolean isPerched() {
        return this.entityData.get(DATA_PERCHED);
    }

    public Direction getPerchFace() {
        return this.entityData.get(DATA_PERCH_FACE);
    }

    private void setPerched(boolean perched) {
        this.entityData.set(DATA_PERCHED, perched);
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    public void setHome(@Nullable BlockPos home) {
        this.home = home;
        if (home == null) {
            this.clearRestriction();
        } else {
            this.restrictTo(home, HOME_RADIUS);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) {
            tag.putLong("Home", this.home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHome(tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(false);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowLureGoal());
        this.goalSelector.addGoal(2, new FlushGoal());
        this.goalSelector.addGoal(3, new GoHomeGoal());
        this.goalSelector.addGoal(4, new PerchGoal());
        this.goalSelector.addGoal(5, new FlutterGoal());
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(this.isInWater() || this.isInLava() ? 0.8 : 0.91));
        }
        this.calculateEntityAnimation(false);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if (this.perchCooldown > 0) {
            --this.perchCooldown;
        }
        if (this.flushTicks > 0) {
            --this.flushTicks;
        }
        if (this.isInWater()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.06, 0.0));
        }
        if (this.isPerched()) {
            if (this.tickCount % 4 == 0) {
                Entity disturber = this.findDisturbance();
                if (disturber != null) {
                    this.flush(disturber.position());
                }
            }
        } else {
            if (this.tickCount % 4 == 0) {
                Player threat = this.findThreat();
                if (threat != null) {
                    this.flee(threat.position());
                }
            }
            if (this.random.nextInt(3) == 0) {
                this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextDouble() - 0.5) * 0.12,
                        (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.12));
            }
        }
        if (this.tickCount % 5 == 0) {
            this.glowAt(this.blockPosition(), TRAIL_LIGHT, false);
        }
        if (this.home != null && this.tickCount % 40 == 0) {
            this.lightHome();
        }
    }

    @Nullable
    private Entity findDisturbance() {
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(DISTURB_RADIUS),
                e -> e != this && e.isAlive() && !(e instanceof LuminousMoth) && !e.isSpectator());
        this.motion.sense(nearby);
        for (LivingEntity other : nearby) {
            if (other instanceof Player player && holdsLure(player)) {
                continue;
            }
            boolean sneaking = other instanceof Player && other.isShiftKeyDown();
            if (this.distanceTo(other) < (sneaking ? SNEAK_STARTLE_RADIUS : STARTLE_RADIUS) || this.motion.isMoving(other) && !sneaking) {
                return other;
            }
        }
        return null;
    }

    @Nullable
    private Player findThreat() {
        Player threat = null;
        double best = Double.MAX_VALUE;
        for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(FLEE_RADIUS),
                p -> p.isAlive() && !p.isSpectator() && !holdsLure(p))) {
            double distance = this.distanceTo(player);
            if (distance < (player.isShiftKeyDown() ? SNEAK_FLEE_RADIUS : FLEE_RADIUS) && distance < best) {
                best = distance;
                threat = player;
            }
        }
        return threat;
    }

    private void flee(Vec3 from) {
        if (this.flushTicks <= 0) {
            this.flush(from);
        } else {
            this.flushFrom = from;
            this.flushTicks = Math.max(this.flushTicks, 30);
        }
    }

    public void flush(Vec3 from) {
        this.setPerched(false);
        this.flushFrom = from;
        this.flushTicks = 40 + this.random.nextInt(40);
        this.perchCooldown = this.flushTicks + 40 + this.random.nextInt(80);
        this.playSound(SoundEvents.BAT_TAKEOFF, 0.15F, 2.0F);
    }

    private void glowAt(BlockPos pos, int light, boolean anchor) {
        Level level = this.level();
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(WildspellMobs.MOTH_GLOW.get())) {
            if (!anchor && (state.getValue(MothGlowBlock.ANCHOR) || state.getValue(MothGlowBlock.LEVEL) >= light)) {
                return;
            }
        } else if (!state.isAir() || !level.getFluidState(pos).isEmpty()) {
            return;
        }
        MothGlowBlock.place(level, pos, light, anchor);
    }

    private void lightHome() {
        BlockPos center = openNear(this.level(), this.home);
        if (center == null) {
            return;
        }
        this.glowAt(center, HOME_LIGHT, true);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos reach = center;
            for (int step = 1; step <= HOME_RADIUS; ++step) {
                BlockPos next = center.relative(direction, step);
                if (!isOpen(this.level(), next)) {
                    break;
                }
                reach = next;
            }
            if (reach != center && reach.distManhattan(center) >= 3) {
                this.glowAt(reach, RING_LIGHT, true);
            }
        }
    }

    public boolean keepsLit(BlockPos pos) {
        return this.home != null && this.isAlive() && this.home.closerThan(pos, HOME_RADIUS + 3);
    }

    public void brightenMoss() {
        Level level = this.level();
        BlockPos origin = this.blockPosition();
        for (int down = 0; down <= 2; ++down) {
            LuminousMoss.brighten(level, origin.below(down));
        }
        BlockPos pos = origin.offset(this.random.nextInt(2 * MOSS_RADIUS + 1) - MOSS_RADIUS,
                this.random.nextInt(2 * MOSS_RADIUS + 1) - MOSS_RADIUS - 1, this.random.nextInt(2 * MOSS_RADIUS + 1) - MOSS_RADIUS);
        if (level.isLoaded(pos)) {
            LuminousMoss.brighten(level, pos);
        }
    }

    public static boolean mothNear(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(LuminousMoth.class, new AABB(pos).inflate(KEEPS_MOSS_LIT), LuminousMoth::isAlive).isEmpty();
    }

    static boolean isOpen(Level level, BlockPos pos) {
        if (!level.isLoaded(pos) || !level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(WildspellMobs.MOTH_GLOW.get());
    }

    @Nullable
    private static BlockPos openNear(Level level, BlockPos pos) {
        for (BlockPos candidate : BlockPos.withinManhattan(pos, 2, 2, 2)) {
            if (isOpen(level, candidate)) {
                return candidate.immutable();
            }
        }
        return null;
    }

    public static boolean holdsLure(Player player) {
        return player.getMainHandItem().is(LURES) || player.getOffhandItem().is(LURES);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!player.getItemInHand(hand).is(Items.GLASS_BOTTLE) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        ItemStack bottled = new ItemStack(WildspellMobs.LUMINOUS_MOTH_BOTTLE.get());
        Bucketable.saveDefaultDataToBucketTag(this, bottled);
        return CreatureBottleItem.catchIn(this, player, hand, bottled);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return this.home == null && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.flush(source.getSourcePosition() != null ? source.getSourcePosition() : this.position());
        }
        return hurt;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BAT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 1.6F;
    }

    private class FollowLureGoal extends Goal {
        @Nullable
        private Player player;
        private int repickTicks;

        FollowLureGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            List<Player> players = LuminousMoth.this.level().getEntitiesOfClass(Player.class, LuminousMoth.this.getBoundingBox().inflate(10.0),
                    p -> p.isAlive() && !p.isSpectator() && holdsLure(p));
            this.player = null;
            double best = Double.MAX_VALUE;
            for (Player candidate : players) {
                double distance = LuminousMoth.this.distanceToSqr(candidate);
                if (distance < best) {
                    best = distance;
                    this.player = candidate;
                }
            }
            return this.player != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.player != null && this.player.isAlive() && holdsLure(this.player) && LuminousMoth.this.distanceToSqr(this.player) < 144.0;
        }

        @Override
        public void start() {
            this.repickTicks = 0;
            LuminousMoth.this.setPerched(false);
        }

        @Override
        public void stop() {
            LuminousMoth.this.perchCooldown = 20;
            this.player = null;
            LuminousMoth.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LuminousMoth.this.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
            if (--this.repickTicks > 0) {
                return;
            }
            this.repickTicks = 10 + LuminousMoth.this.random.nextInt(15);
            Vec3 flower = this.player.getEyePosition().add(this.player.getLookAngle().scale(0.8)).subtract(0.0, 0.5, 0.0);
            if (LuminousMoth.this.position().distanceToSqr(flower) < 2.0) {
                LuminousMoth.this.getNavigation().stop();
                return;
            }
            double angle = LuminousMoth.this.random.nextDouble() * Math.PI * 2.0;
            LuminousMoth.this.getNavigation().moveTo(flower.x + Math.cos(angle) * 0.8, flower.y, flower.z + Math.sin(angle) * 0.8, 1.2);
        }
    }

    private class GoHomeGoal extends Goal {
        GoHomeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return LuminousMoth.this.home != null && !LuminousMoth.this.home.closerToCenterThan(LuminousMoth.this.position(), HOME_RADIUS + 2);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && !LuminousMoth.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            LuminousMoth.this.setPerched(false);
            BlockPos home = LuminousMoth.this.home;
            LuminousMoth.this.getNavigation().moveTo(home.getX() + 0.5, home.getY() + 0.5, home.getZ() + 0.5, 1.0);
        }
    }

    private class PerchGoal extends Goal {
        private static final int SEARCH = 6;
        @Nullable
        private Vec3 spot;
        private Direction face = Direction.UP;
        private int ticks;
        private int stayTicks;

        PerchGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (LuminousMoth.this.perchCooldown > 0 || LuminousMoth.this.random.nextInt(reducedTickDelay(4)) != 0) {
                return false;
            }
            this.spot = this.findPerch();
            return this.spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null && this.ticks < this.stayTicks && LuminousMoth.this.flushTicks <= 0;
        }

        @Override
        public void start() {
            this.ticks = 0;
            this.stayTicks = 300;
            LuminousMoth.this.getNavigation().moveTo(this.spot.x, this.spot.y + 0.2, this.spot.z, 0.8);
        }

        @Override
        public void stop() {
            LuminousMoth.this.setPerched(false);
            LuminousMoth.this.perchCooldown = Math.max(LuminousMoth.this.perchCooldown, 60 + LuminousMoth.this.random.nextInt(120));
            this.spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.spot == null) {
                return;
            }
            ++this.ticks;
            LuminousMoth moth = LuminousMoth.this;
            if (moth.isPerched()) {
                moth.setDeltaMovement(Vec3.ZERO);
                moth.setPos(this.spot.x, this.spot.y, this.spot.z);
                if (this.face != Direction.UP) {
                    float yaw = this.face.getOpposite().toYRot();
                    moth.setYRot(yaw);
                    moth.yBodyRot = yaw;
                    moth.yHeadRot = yaw;
                }
                if (this.ticks % MOSS_INTERVAL == 0) {
                    moth.brightenMoss();
                }
                return;
            }
            if (moth.position().distanceToSqr(this.spot) < 0.36) {
                moth.getNavigation().stop();
                moth.entityData.set(DATA_PERCH_FACE, this.face);
                moth.setPerched(true);
                moth.motion.forget();
                moth.brightenMoss();
                this.stayTicks = this.ticks + 600 + moth.random.nextInt(1800);
            } else if (moth.getNavigation().isDone()) {
                if (moth.position().distanceToSqr(this.spot) < 4.0) {
                    moth.getMoveControl().setWantedPosition(this.spot.x, this.spot.y, this.spot.z, 0.6);
                } else if (this.ticks > 40) {
                    this.spot = null;
                }
            }
            if (this.ticks > 200 && !moth.isPerched()) {
                this.spot = null;
            }
        }

        @Nullable
        private Vec3 findPerch() {
            this.face = Direction.UP;
            Vec3 plant = this.search(SEARCH, 16, true);
            if (plant != null) {
                return plant;
            }
            boolean wallFirst = LuminousMoth.this.random.nextBoolean();
            Vec3 first = wallFirst ? this.searchWalls(4, 12) : this.search(4, 12, false);
            if (first != null) {
                return first;
            }
            return wallFirst ? this.search(4, 12, false) : this.searchWalls(4, 12);
        }

        @Nullable
        private Vec3 searchWalls(int radius, int attempts) {
            Level level = LuminousMoth.this.level();
            BlockPos origin = LuminousMoth.this.blockPosition();
            for (int attempt = 0; attempt < attempts; ++attempt) {
                BlockPos pos = origin.offset(LuminousMoth.this.random.nextInt(2 * radius + 1) - radius,
                        LuminousMoth.this.random.nextInt(8) - 5, LuminousMoth.this.random.nextInt(2 * radius + 1) - radius);
                if (!LuminousMoth.this.isWithinRestriction(pos) || !isOpen(level, pos)) {
                    continue;
                }
                Direction toWall = Direction.Plane.HORIZONTAL.getRandomDirection(LuminousMoth.this.random);
                BlockPos wall = pos.relative(toWall);
                if (!level.isLoaded(wall) || !level.getBlockState(wall).isFaceSturdy(level, wall, toWall.getOpposite())) {
                    continue;
                }
                double offset = 0.5 - WALL_GAP;
                Vec3 spot = new Vec3(pos.getX() + 0.5 + toWall.getStepX() * offset, pos.getY() + 0.25, pos.getZ() + 0.5 + toWall.getStepZ() * offset);
                if (this.inSight(spot)) {
                    this.face = toWall.getOpposite();
                    return spot;
                }
            }
            return null;
        }

        private boolean inSight(Vec3 spot) {
            return ColdEffects.clearPath(LuminousMoth.this, LuminousMoth.this.getEyePosition(), spot);
        }

        @Nullable
        private Vec3 search(int radius, int attempts, boolean plants) {
            Level level = LuminousMoth.this.level();
            BlockPos origin = LuminousMoth.this.blockPosition();
            for (int attempt = 0; attempt < attempts; ++attempt) {
                BlockPos pos = origin.offset(LuminousMoth.this.random.nextInt(2 * radius + 1) - radius,
                        LuminousMoth.this.random.nextInt(9) - 6, LuminousMoth.this.random.nextInt(2 * radius + 1) - radius);
                if (!level.isLoaded(pos) || !LuminousMoth.this.isWithinRestriction(pos)) {
                    continue;
                }
                BlockState state = level.getBlockState(pos);
                if (plants ? !state.is(PERCHES) : !state.isFaceSturdy(level, pos, Direction.UP) || !level.getFluidState(pos).isEmpty()) {
                    continue;
                }
                double top = state.getShape(level, pos).isEmpty() ? 0.0 : state.getShape(level, pos).max(Direction.Axis.Y);
                double y = pos.getY() + top;
                boolean room = true;
                for (int by = (int) Math.floor(y); by <= (int) Math.floor(y + LuminousMoth.this.getBbHeight()); ++by) {
                    BlockPos body = new BlockPos(pos.getX(), by, pos.getZ());
                    room &= body.equals(pos) || isOpen(level, body);
                }
                Vec3 spot = new Vec3(pos.getX() + 0.5, y, pos.getZ() + 0.5);
                if (room && this.inSight(spot.add(0.0, 0.2, 0.0))) {
                    return spot;
                }
            }
            return null;
        }
    }

    private class FlushGoal extends Goal {
        private int repickTicks;
        @Nullable
        private Vec3 target;

        FlushGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return LuminousMoth.this.flushTicks > 0 && LuminousMoth.this.flushFrom != null;
        }

        @Override
        public void start() {
            this.repickTicks = 0;
            this.target = null;
            LuminousMoth.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LuminousMoth moth = LuminousMoth.this;
            if (--this.repickTicks <= 0) {
                this.repickTicks = 5 + moth.random.nextInt(6);
                this.target = this.pick();
            }
            // The move control only pushes on ticks it's asked to, so ask every tick.
            if (this.target != null) {
                moth.getMoveControl().setWantedPosition(this.target.x, this.target.y, this.target.z, FLEE_SPEED);
            }
        }

        @Nullable
        private Vec3 pick() {
            LuminousMoth moth = LuminousMoth.this;
            Vec3 away = moth.position().subtract(moth.flushFrom).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(moth.random.nextDouble() - 0.5, 0.0, moth.random.nextDouble() - 0.5) : away;
            Vec3 escape = away.normalize();
            return ColdEffects.findSpot(6, () -> {
                Vec3 heading = escape.yRot((float) ((moth.random.nextDouble() - 0.5) * 2.4));
                double distance = 3.0 + moth.random.nextDouble() * 2.0;
                double lift = moth.flushTicks > 30 ? 0.6 + moth.random.nextDouble() : (moth.random.nextDouble() - 0.5) * 1.2;
                return moth.position().add(heading.scale(distance)).add(0.0, lift, 0.0);
            }, spot -> isOpen(moth.level(), BlockPos.containing(spot)) && ColdEffects.clearPath(moth, moth.position(), spot));
        }
    }

    private class FlutterGoal extends Goal {
        FlutterGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return LuminousMoth.this.getNavigation().isDone();
        }

        @Override
        public boolean canContinueToUse() {
            return LuminousMoth.this.getNavigation().isInProgress();
        }

        @Override
        public void start() {
            LuminousMoth moth = LuminousMoth.this;
            Vec3 spot = ColdEffects.findSpot(8,
                    () -> Vec3.atCenterOf(moth.blockPosition().offset(moth.random.nextInt(7) - 3, moth.random.nextInt(4) - 2, moth.random.nextInt(7) - 3)),
                    at -> moth.isWithinRestriction(BlockPos.containing(at)) && isOpen(moth.level(), BlockPos.containing(at))
                            && ColdEffects.clearPath(moth, moth.position(), at));
            if (spot != null) {
                moth.getNavigation().moveTo(spot.x, spot.y, spot.z, 0.8);
            }
        }
    }

    private static class MothMoveControl extends ThrustMoveControl {
        private static final double THRUST = 0.21;

        MothMoveControl(Mob mob) {
            super(mob, THRUST, 1.0);
        }

        @Override
        protected double arrival() {
            return 0.05;
        }

        @Override
        protected double thrust(double distance) {
            return Math.min(this.speedModifier * this.mob.getAttributeValue(Attributes.FLYING_SPEED) * THRUST, distance * 0.02);
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            if (heading != null && heading.horizontalDistanceSqr() > 1.0E-4) {
                float yaw = (float) (Mth.atan2(heading.z, heading.x) * Mth.RAD_TO_DEG) - 90.0F;
                this.mob.setYRot(this.rotlerp(this.mob.getYRot(), yaw, 40.0F));
                this.mob.yBodyRot = this.mob.getYRot();
            }
        }

        @Override
        public void tick() {
            this.mob.setSpeed(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setZza(0.0F);
            super.tick();
            this.operation = Operation.WAIT;
        }
    }
}
