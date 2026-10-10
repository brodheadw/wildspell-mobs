package com.wildspell.mobs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class HoveringBoss extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(HoveringBoss.class, EntityDataSerializers.BYTE);

    protected final ServerBossEvent bossEvent;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    protected int actionTicks;

    protected HoveringBoss(EntityType<? extends HoveringBoss> type, Level level, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(this.getDisplayName(), color, overlay);
        this.setNoGravity(true);
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, (byte) 0);
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    protected void startAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, (byte) action);
        this.actionTicks = ticks;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
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
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
