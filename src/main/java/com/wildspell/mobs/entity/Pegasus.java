package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

/**
 * A winged horse of the Aether's meadows. Tamed, saddled and ridden like a horse, but the jump key takes it
 * into the air: hold jump to climb, look where you want to go and push forward, let go to glide down. It
 * never takes fall damage. Wild ones now and then take wing and spiral up over their meadow before coming
 * back down near where they rose, and take wing when struck.
 */
public class Pegasus extends AbstractHorse {
    /** What a wild pegasus spawns on: Aether grass, or ordinary animal ground (see the block tag). */
    public static final TagKey<Block> SPAWNS_ON = TagKey.create(Registries.BLOCK, WildspellMobs.id("pegasus_spawnable_on"));
    /** Treats eaten like apples besides the horse foods: the Aether's berries (see the item tag). */
    public static final TagKey<Item> TREATS = TagKey.create(Registries.ITEM, WildspellMobs.id("pegasus_treats"));

    /** Ridden: top forward speed in blocks per tick (a galloping horse is about 0.35). */
    public static final double CRUISE = 0.7;
    /** Ridden: extra speed when diving steeply. */
    public static final double DIVE_BONUS = 0.5;
    /** Ridden: climb rate while jump is held. */
    public static final double CLIMB = 0.45;
    /** How fast it sinks with its wings held out: ridden with no climb, or wild. */
    public static final double GLIDE_SINK = 0.12;
    /** Upward kick of a ridden take-off. */
    public static final double TAKEOFF = 0.75;
    /** How quickly ridden flight answers the rider: the share of the gap to the wanted velocity closed per tick. */
    private static final double RESPONSE = 0.12;

    /** One wild soar: a turn up, a turn down, back over the ground it left. */
    public static final int SOAR_TICKS = 240;
    private static final int SOAR_CLIMB_TICKS = SOAR_TICKS / 2;
    private static final double SOAR_SPEED = 0.3;
    private static final double SOAR_LIFT = 0.08;
    private static final float SOAR_TURN = 360.0F / SOAR_CLIMB_TICKS;
    /** Chance per tick an idle wild pegasus takes wing (about once every two minutes). */
    private static final int SOAR_CHANCE = 2400;

    private int soarTicks;
    private float soarTurn;

    // Client-side wing animation (see PegasusModel).
    private float wingSpread;
    private float wingSpreadO;
    private float flap;
    private float flapO;

    public Pegasus(EntityType<? extends Pegasus> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseHorseAttributes();
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

    /** Takes wing on its own, if it's wild, grown, free and has the headroom. */
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
        if (!this.level().isClientSide && !this.isSoaring() && this.random.nextInt(SOAR_CHANCE) == 0) {
            this.startSoaring();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            // Flying without gravity keeps a rider from being kicked for "floating a vehicle" on servers that
            // don't allow flight (ServerGamePacketListenerImpl skips no-gravity vehicles); flight supplies its own sink.
            this.setNoGravity(this.isVehicle() && this.isAloft());
            return;
        }
        this.wingSpreadO = this.wingSpread;
        this.flapO = this.flap;
        boolean aloft = this.isAloft();
        this.wingSpread = Mth.approach(this.wingSpread, aloft ? 1.0F : 0.0F, 0.15F);
        boolean rising = this.getY() - this.yo > 0.02;
        float before = this.flap;
        this.flap += aloft ? (rising ? 0.7F : 0.3F) : 0.0F;
        // One wingbeat per 2 pi: a soft beat sound on each downstroke while it climbs.
        if (aloft && rising && Mth.floor(before / Mth.TWO_PI) != Mth.floor(this.flap / Mth.TWO_PI)) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_FLAP, this.getSoundSource(),
                    0.25F, 1.5F + this.random.nextFloat() * 0.2F, false);
        }
    }

    /** 0 folded against its sides, 1 spread. */
    public float getWingSpread(float partialTick) {
        return Mth.lerp(partialTick, this.wingSpreadO, this.wingSpread);
    }

    public float getFlap(float partialTick) {
        return Mth.lerp(partialTick, this.flapO, this.flap);
    }

    @Override
    public void travel(Vec3 input) {
        if (this.isAlive() && this.getControllingPassenger() instanceof Player rider && this.isSaddled()
                && !this.isInWater() && !this.isInLava() && (!this.onGround() || rider.jumping)) {
            this.flyRidden(rider, input);
            return;
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

    /**
     * Runs on the rider's client, which drives a ridden mount. {@code input} is the horse's scaled rider input.
     * Public so gametests can drive it: their mock riders aren't local players, so nothing else would.
     */
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

    /** A wild soar: spiral up one full turn, then down another, landing about where it rose. */
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
            this.soarTicks = SOAR_CLIMB_TICKS; // blocked: turn it into the descent
        }
        if (this.onGround() && !climbing) {
            this.soarTicks = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && !this.isSoaring()) {
            this.startSoaring();
        }
        return hurt;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** No charged horse jump: the jump key is the pegasus's climb (see flyRidden). */
    @Override
    public boolean canJump() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(TREATS) || super.isFood(stack);
    }

    /** An Aether berry is as good as an apple (fedFood still spends the berry itself). */
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
        }
        return foal;
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
