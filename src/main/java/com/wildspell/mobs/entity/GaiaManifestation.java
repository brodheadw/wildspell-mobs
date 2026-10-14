package com.wildspell.mobs.entity;

import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GaiaManifestation extends Mob implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.gaia_manifestation.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos home;
    private float homeYaw;

    public GaiaManifestation(EntityType<? extends GaiaManifestation> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.setNoGravity(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    public void anchorTo(BlockPos home, float yaw) {
        this.home = home.immutable();
        this.homeYaw = Float.isFinite(yaw) ? yaw : 0.0F;
        this.returnHome();
    }

    private void returnHome() {
        if (this.home != null) {
            this.moveTo(this.home.getX() + 0.5, this.home.getY(), this.home.getZ() + 0.5, this.homeYaw, 0.0F);
            this.setYHeadRot(this.homeYaw);
            this.setYBodyRot(this.homeYaw);
        }
        this.setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (this.home == null) {
                this.anchorTo(this.blockPosition(), this.getYRot());
            }
            this.returnHome();
        }
        super.tick();
        if (!this.level().isClientSide) {
            this.returnHome();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) {
            tag.putLong("GaiaHome", this.home.asLong());
            tag.putFloat("GaiaYaw", this.homeYaw);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPersistenceRequired();
        this.setNoAi(true);
        this.setNoGravity(true);
        this.setLeashData(null);
        if (tag.contains("GaiaHome")) {
            this.anchorTo(BlockPos.of(tag.getLong("GaiaHome")), tag.getFloat("GaiaYaw"));
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return source.is(DamageTypes.GENERIC_KILL) && super.hurt(source, amount);
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
    }

    @Override
    public void travel(Vec3 movement) {
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
    public boolean startRiding(Entity vehicle, boolean force) {
        return false;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void setLeashedTo(Entity holder, boolean broadcastPacket) {
    }

    @Override
    public void teleportTo(double x, double y, double z) {
    }

    @Override
    public boolean teleportTo(ServerLevel level, double x, double y, double z,
            Set<RelativeMovement> relativeMovements, float yaw, float pitch) {
        return false;
    }

    @Override
    public boolean randomTeleport(double x, double y, double z, boolean broadcast) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level oldLevel, Level newLevel) {
        return false;
    }

    @Override
    @Nullable
    public Entity changeDimension(DimensionTransition transition) {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 10, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
