package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class Pegasus extends AbstractHorse {
    public static final TagKey<Block> SPAWNS_ON = TagKey.create(Registries.BLOCK, WildspellMobs.id("pegasus_spawnable_on"));
    public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, WildspellMobs.id("pegasus_treats"));

    public static final double CRUISE = 0.7;
    public static final double DIVE_BONUS = 0.5;
    public static final double CLIMB = 0.45;
    public static final double GLIDE_SINK = 0.12;
    public static final double TAKEOFF = 0.75;
    private static final double RESPONSE = 0.12;

    public static final int SOAR_TICKS = 240;
    private static final int SOAR_CLIMB_TICKS = SOAR_TICKS / 2;
    private static final double SOAR_SPEED = 0.3;
    private static final double SOAR_LIFT = 0.08;
    private static final float SOAR_TURN = 360.0F / SOAR_CLIMB_TICKS;
    private static final int SOAR_CHANCE = 2400;

    public static final int HERD_FLIGHT_TICKS = 600;
    public static final int HERD_DESCENT_TICKS = 160;
    public static final int HERD_CHANCE = 9600;
    public static final double HERD_REACH = 24.0;
    public static final int HERD_MAX = 7;
    public static final double HERD_ALTITUDE = 24.0;
    public static final double HERD_SPEED = 0.45;
    public static final double HERD_SPACING = 3.0;
    private static final double HERD_CLIMB = 0.25;
    private static final double HERD_KEEP = 0.15;

    private static final EntityDataAccessor<Boolean> DATA_ALOFT = SynchedEntityData.defineId(Pegasus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Pegasus.class, EntityDataSerializers.INT);
    public static final int PINK_ONE_IN = 200;

    public enum Variant {
        WHITE("white", 0xFFF6DC, 0xFFFFFF, 0xF2DC9A),
        BLACK("black", 0xC8CCE0, 0x5A6CB4, 0x9AA0BC),
        PINK("pink", 0x4A40AA, 0x7C54C4, 0xB096E4, 0x849CE8, 0xAAD0F6, 0xF8BAD6, 0xE28CBA),
        PURE("pure", 0xFFFFFF, 0xF4F6FF);

        public final String name;
        private final int[] sparkles;

        Variant(String name, int... sparkles) {
            this.name = name;
            this.sparkles = sparkles;
        }

        static Variant byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : WHITE;
        }
    }

    private int soarTicks;
    private float soarTurn;
    private int herdTicks;
    private float herdHeading;
    private double herdCruise;
    @Nullable
    private Pegasus herdLeader;
    private int herdSlot;
    private boolean flightNoGravity;

    private float wingSpread;
    private float wingSpreadO;
    private float flap;
    private float flapO;
    private float flapStrength;
    private float flapStrengthO;

    public Pegasus(EntityType<? extends Pegasus> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ALOFT, false);
        builder.define(DATA_VARIANT, Variant.WHITE.ordinal());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes();
    }

    public Variant getVariant() {
        return Variant.byId(this.entityData.get(DATA_VARIANT));
    }

    public void setVariant(Variant variant) {
        this.entityData.set(DATA_VARIANT, variant.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Variant", this.getVariant().name);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (Variant variant : Variant.values()) {
            if (variant.name.equals(tag.getString("Variant"))) {
                this.setVariant(variant);
            }
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData groupData) {
        Variant herd;
        if (groupData instanceof Herd existing) {
            herd = existing.variant;
        } else {
            herd = HERD_COATS[this.random.nextInt(HERD_COATS.length)];
            groupData = new Herd(herd);
        }
        this.setVariant(this.random.nextInt(PINK_ONE_IN) == 0 ? Variant.PINK : herd);
        return super.finalizeSpawn(level, difficulty, spawnType, groupData);
    }

    private static final Variant[] HERD_COATS = {Variant.WHITE, Variant.PURE, Variant.BLACK};

    public static class Herd extends AgeableMob.AgeableMobGroupData {
        final Variant variant;

        Herd(Variant variant) {
            super(0.2F);
            this.variant = variant;
        }
    }

    @Override
    protected void randomizeAttributes(RandomSource random) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(random::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(random::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(random::nextDouble));
    }

    public static boolean checkPegasusSpawnRules(EntityType<Pegasus> type, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SPAWNS_ON) && isBrightEnoughToSpawn(level, pos);
    }

    public boolean isAloft() {
        return !this.onGround() && !this.isInWater() && !this.isInLava() && !this.isPassenger();
    }

    public boolean isSoaring() {
        return this.soarTicks > 0;
    }

    public boolean isHerdFlying() {
        return this.herdTicks > 0 || this.herdLeader != null;
    }

    @Nullable
    public Pegasus getHerdLeader() {
        return this.herdLeader;
    }

    private boolean canTakeWing() {
        return !this.isTamed() && !this.isBaby() && !this.isVehicle() && !this.isLeashed() && !this.isInWater()
                && this.level().noCollision(this, this.getBoundingBox().move(0.0, 3.0, 0.0));
    }

    public int startHerdFlight() {
        if (!this.canTakeWing() || !this.onGround() || this.isHerdFlying()) {
            return 0;
        }
        java.util.List<Pegasus> herd = this.level().getEntitiesOfClass(Pegasus.class, this.getBoundingBox().inflate(HERD_REACH),
                other -> other != this && other.canTakeWing() && !other.isHerdFlying());
        if (herd.isEmpty()) {
            return 0;
        }
        this.soarTicks = 0;
        this.herdTicks = HERD_FLIGHT_TICKS;
        this.herdHeading = this.random.nextFloat() * 360.0F;
        this.herdCruise = this.getY() + HERD_ALTITUDE;
        this.setEating(false);
        this.setStanding(false);
        this.getNavigation().stop();
        int slot = 0;
        for (Pegasus follower : herd) {
            if (slot >= HERD_MAX) {
                break;
            }
            follower.soarTicks = 0;
            follower.herdLeader = this;
            follower.herdSlot = slot++;
            follower.setEating(false);
            follower.setStanding(false);
            follower.getNavigation().stop();
        }
        return slot;
    }

    public Vec3 herdPlace(int slot) {
        int row = slot / 2 + 1;
        double side = (slot % 2 == 0 ? 1.0 : -1.0) * HERD_SPACING * row;
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.herdHeading);
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return this.position().subtract(forward.scale(HERD_SPACING * row)).add(right.scale(side)).add(0.0, 0.6 * row, 0.0);
    }

    private void leadHerd() {
        int elapsed = HERD_FLIGHT_TICKS - this.herdTicks;
        boolean descending = this.herdTicks <= HERD_DESCENT_TICKS;
        this.herdHeading += 0.35F * Mth.sin(elapsed * 0.02F);
        this.setYRot(this.herdHeading);
        this.yBodyRot = this.yHeadRot = this.herdHeading;
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.herdHeading).scale(HERD_SPEED);
        double lift = descending ? -SOAR_LIFT : Mth.clamp((this.herdCruise - this.getY()) * 0.05, -HERD_CLIMB, HERD_CLIMB);
        Vec3 motion = new Vec3(ahead.x, lift, ahead.z);
        this.getNavigation().stop();
        this.move(MoverType.SELF, motion);
        this.setDeltaMovement(motion);
        this.resetFallDistance();
        this.herdTicks--;
        if (this.horizontalCollision && !descending) {
            this.herdCruise += 4.0;
        }
        if (this.onGround() && (descending || elapsed > 40)) {
            this.herdTicks = 0;
        }
    }

    private void followHerd() {
        Pegasus leader = this.herdLeader;
        if (leader == null || !leader.isAlive() || leader.herdTicks <= 0 || leader.level() != this.level()) {
            this.herdLeader = null;
            return;
        }
        Vec3 place = leader.herdPlace(this.herdSlot);
        Vec3 toward = place.subtract(this.position()).scale(HERD_KEEP);
        double max = HERD_SPEED * 1.4;
        if (toward.length() > max) {
            toward = toward.normalize().scale(max);
        }
        Vec3 motion = toward.add(leader.getDeltaMovement().scale(0.5));
        this.setYRot(leader.herdHeading);
        this.yBodyRot = this.yHeadRot = leader.herdHeading;
        this.getNavigation().stop();
        this.move(MoverType.SELF, motion);
        this.setDeltaMovement(motion);
        this.resetFallDistance();
    }

    public boolean startSoaring() {
        if (this.isTamed() || this.isBaby() || this.isVehicle() || this.isLeashed() || !this.onGround() || this.isInWater()
                || !this.level().noCollision(this, this.getBoundingBox().move(0.0, 3.0, 0.0))) {
            return false;
        }
        this.soarTicks = SOAR_TICKS;
        this.soarTurn = this.random.nextBoolean() ? SOAR_TURN : -SOAR_TURN;
        this.setEating(false);
        this.setStanding(false);
        this.getNavigation().stop();
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        if ((this.isSoaring() || this.isHerdFlying()) && (this.isVehicle() || this.isLeashed() || this.isTamed())) {
            this.soarTicks = 0;
            this.herdTicks = 0;
            this.herdLeader = null;
        } else if (!this.isSoaring() && !this.isHerdFlying() && this.random.nextInt(HERD_CHANCE) == 0) {
            this.startHerdFlight();
        } else if (!this.isSoaring() && !this.isHerdFlying() && this.random.nextInt(SOAR_CHANCE) == 0) {
            this.startSoaring();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Flying without gravity keeps a rider from being kicked for "floating a vehicle" on servers that
            // don't allow flight (ServerGamePacketListenerImpl skips no-gravity vehicles); flight supplies its own sink.
            boolean flying = this.isAloft() && this.isSaddled() && this.getControllingPassenger() instanceof Player;
            if (flying != this.flightNoGravity) {
                this.flightNoGravity = flying;
                this.setNoGravity(flying);
            }
            // Checked against the ground itself: a mob that never moves (NoAI) never updates onGround.
            this.entityData.set(DATA_ALOFT, this.isAloft() && this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.1, 0.0)));
            return;
        }
        this.wingSpreadO = this.wingSpread;
        this.flapO = this.flap;
        this.flapStrengthO = this.flapStrength;
        boolean aloft = this.isControlledByLocalInstance() ? this.isAloft() : this.entityData.get(DATA_ALOFT);
        this.wingSpread = Mth.approach(this.wingSpread, aloft ? 1.0F : 0.0F, 0.15F);
        boolean rising = this.getY() - this.yo > 0.02;
        this.flapStrength = Mth.approach(this.flapStrength, rising ? 1.0F : 0.3F, 0.08F);
        if (aloft && this.wingSpread > 0.8F) {
            this.sparkle();
            this.sparkle();
        }
        float before = this.flap;
        this.flap += aloft ? (rising ? 0.7F : 0.3F) : 0.0F;
        if (aloft && rising && Mth.floor(before / Mth.TWO_PI) != Mth.floor(this.flap / Mth.TWO_PI)) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(),
                    0.25F, 1.5F + this.random.nextFloat() * 0.2F, false);
        }
    }

    private static final float MODEL_SCALE = 1.25F / 16.0F;
    private static final float[] WING_BONES = {10.0F, 12.0F, 8.0F};
    private static final float[] WING_RISE = {1.0F, 0.72F, 0.62F};

    private void sparkle() {
        int[] colours = this.getVariant().sparkles;
        int rgb = colours[this.random.nextInt(colours.length)];
        float flap = this.getFlap(1.0F);
        float strength = this.getFlapStrength(1.0F);
        float beat = Mth.sin(flap) * 0.6F * strength;
        float outerBeat = Mth.sin(flap - 0.9F) * 0.45F * strength;
        float along = this.random.nextFloat() * (WING_BONES[0] + WING_BONES[1] + WING_BONES[2]);
        double out = 5.2;
        double up = 0.0;
        for (int b = 0; b < WING_BONES.length && along > 0.0F; b++) {
            float length = Math.min(along, WING_BONES[b]);
            double rise = WING_RISE[b] + beat + (b > 0 ? outerBeat * (b == 1 ? 0.5 : 1.0) : 0.0);
            out += Math.cos(rise) * length;
            up += Math.sin(rise) * length;
            along -= length;
        }
        double back = 4.0 + this.random.nextFloat() * 18.0;
        Vec3 side = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F).scale(this.random.nextBoolean() ? 1.0 : -1.0);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 at = this.position()
                .add(side.scale(out * MODEL_SCALE))
                .add(forward.scale((4.0 - back) * MODEL_SCALE))
                .add(0.0, (20.0 + up) * MODEL_SCALE, 0.0);
        this.level().addParticle(new DustParticleOptions(Vec3.fromRGB24(rgb).toVector3f(), 0.4F), at.x, at.y, at.z, 0.0, -0.03, 0.0);
    }

    public float getWingSpread(float partialTick) {
        return Mth.lerp(partialTick, this.wingSpreadO, this.wingSpread);
    }

    public float getFlap(float partialTick) {
        return Mth.lerp(partialTick, this.flapO, this.flap);
    }

    public float getFlapStrength(float partialTick) {
        return Mth.lerp(partialTick, this.flapStrengthO, this.flapStrength);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player rider && this.isSaddled()
                && !this.isInWater() && !this.isInLava() && (!this.onGround() || rider.jumping)) {
            this.flyRidden(rider, input);
            return;
        }
        if (this.isAlive() && this.herdTicks > 0 && !this.level().isClientSide) {
            this.leadHerd();
            return;
        }
        if (this.isAlive() && this.herdLeader != null && !this.level().isClientSide) {
            this.followHerd();
            if (this.herdLeader != null) {
                return;
            }
        }
        if (this.isAlive() && this.isSoaring() && !this.level().isClientSide) {
            this.soar();
            return;
        }
        super.travel(input);
        if (this.isAloft() && this.getDeltaMovement().y < -GLIDE_SINK) {
            Vec3 motion = this.getDeltaMovement();
            this.setDeltaMovement(motion.x, -GLIDE_SINK, motion.z);
        }
    }

    public void flyRidden(Player rider, Vec3 input) {
        Vec3 motion = this.getDeltaMovement();
        if (this.onGround()) {
            motion = new Vec3(motion.x, TAKEOFF, motion.z);
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(), 0.6F, 1.3F, false);
        } else {
            Vec3 look = Vec3.directionFromRotation(rider.getXRot(), this.getYRot());
            double forward = input.z;
            double speed = CRUISE + (look.y < 0.0 ? -look.y * DIVE_BONUS : 0.0);
            Vec3 side = new Vec3(look.z, 0.0, -look.x).normalize().scale(input.x * 0.5);
            Vec3 wanted = look.scale(forward * speed).add(side).add(0.0, rider.jumping ? CLIMB : -GLIDE_SINK, 0.0);
            motion = motion.add(wanted.subtract(motion).scale(RESPONSE));
        }
        this.move(MoverType.SELF, motion);
        this.setDeltaMovement(this.horizontalCollision ? motion.multiply(0.5, 1.0, 0.5) : motion);
        this.resetFallDistance();
        this.walkAnimation.update(0.0F, 0.4F);
    }

    private void soar() {
        int elapsed = SOAR_TICKS - this.soarTicks;
        boolean climbing = elapsed < SOAR_CLIMB_TICKS;
        float heading = this.getYRot() + this.soarTurn;
        this.setYRot(heading);
        this.yBodyRot = this.yHeadRot = heading;
        Vec3 ahead = Vec3.directionFromRotation(0.0F, heading).scale(SOAR_SPEED);
        Vec3 motion = new Vec3(ahead.x, climbing ? SOAR_LIFT : -SOAR_LIFT, ahead.z);
        this.getNavigation().stop();
        this.move(MoverType.SELF, motion);
        this.setDeltaMovement(motion);
        this.resetFallDistance();
        this.soarTicks--;
        if (this.horizontalCollision && climbing) {
            this.soarTicks = SOAR_CLIMB_TICKS;
        }
        if (this.onGround() && !climbing) {
            this.soarTicks = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isSoaring() && !this.isHerdFlying()) {
            this.startSoaring();
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(TREATS) || super.isFood(stack);
    }

    @Override
    protected boolean handleEating(Player player, ItemStack stack) {
        return super.handleEating(player, stack.is(TREATS) ? new ItemStack(Items.APPLE) : stack);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean openInventory = !this.isBaby() && this.isTamed() && player.isSecondaryUseActive();
        if (!this.isVehicle() && !openInventory) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty()) {
                if (this.isFood(stack)) {
                    return this.fedFood(player, stack);
                }
                if (!this.isTamed()) {
                    this.makeMad();
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public boolean canMate(Animal other) {
        return other != this && other instanceof Pegasus pegasus && this.canParent() && pegasus.canParent();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Pegasus foal = WildspellMobs.PEGASUS.get().create(level);
        if (foal != null) {
            this.setOffspringAttributes(partner, foal);
            foal.setVariant(foalVariant(this.getVariant(), ((Pegasus) partner).getVariant(), this.random));
        }
        return foal;
    }

    public static Variant foalVariant(Variant a, Variant b, RandomSource random) {
        if (random.nextInt(PINK_ONE_IN) == 0 || ((a == Variant.PINK || b == Variant.PINK) && random.nextInt(4) == 0)) {
            return Variant.PINK;
        }
        Variant pick = random.nextBoolean() ? a : b;
        return pick != Variant.PINK ? pick : (a != Variant.PINK ? a : b != Variant.PINK ? b : Variant.WHITE);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.HORSE_EAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.HORSE_ANGRY;
    }
}
