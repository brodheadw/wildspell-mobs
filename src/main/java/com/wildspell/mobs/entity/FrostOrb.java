package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FrostOrb extends AbstractHurtingProjectile {
    private static final float DAMAGE = 6.0F;
    private static final double BURST_RADIUS = 2.5;
    private static final int MAX_AGE = 200;

    public FrostOrb(EntityType<? extends FrostOrb> type, Level level) {
        super(type, level);
    }

    public FrostOrb(Level level, LivingEntity owner, Vec3 direction) {
        super(WildspellMobs.FROST_ORB.get(), owner, direction, level);
        this.accelerationPower = 0.06;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return WildspellMobs.FROST_MOTE.get();
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, 0.0, 0.0);
        } else if (this.tickCount > MAX_AGE) {
            this.burst((ServerLevel) this.level());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level() instanceof ServerLevel level) {
            this.burst(level);
        }
    }

    private void burst(ServerLevel level) {
        Entity owner = this.getOwner();
        level.sendParticles(ColdEffects.ICE_CHIPS, this.getX(), this.getY(), this.getZ(), 50, 0.6, 0.6, 0.6, 0.2);
        level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(), 40, 0.8, 0.8, 0.8, 0.1);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 0.9F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(this.position(), this.position()).inflate(BURST_RADIUS),
                e -> e != owner && !(owner instanceof IceLich && IceLich.isMinion(e)))) {
            if (victim.hurt(this.damageSources().indirectMagic(this, owner), DAMAGE)) {
                Frost.add(victim, 60, 20);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1), owner);
            }
        }
        this.discard();
    }
}
