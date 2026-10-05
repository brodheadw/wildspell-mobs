package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SolarRay extends AbstractHurtingProjectile {
    public static final float DAMAGE = 5.0F;
    private static final int BURN_TICKS = 80;
    private static final int MAX_AGE = 120;

    public SolarRay(EntityType<? extends SolarRay> type, Level level) {
        super(type, level);
    }

    public SolarRay(Level level, LivingEntity owner, Vec3 direction) {
        super(WildspellMobs.SOLAR_RAY.get(), owner, direction, level);
        this.accelerationPower = 0.12;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.END_ROD;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.6F;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount > MAX_AGE) {
            this.fade((ServerLevel) this.level());
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof Apollo);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (result.getEntity() instanceof LivingEntity target && target.hurt(this.damageSources().indirectMagic(this, this.getOwner()), DAMAGE)) {
            target.igniteForTicks(BURN_TICKS);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            this.fade(level);
        }
    }

    private void fade(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 12, 0.2, 0.2, 0.2, 0.08);
        level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 8, 0.2, 0.2, 0.2, 0.04);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 0.6F, 1.6F);
        this.discard();
    }
}
