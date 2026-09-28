package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class FrostShard extends ThrowableItemProjectile {
    private static final byte EVENT_SHATTER = 3;
    public static final double GRAVITY = 0.03;
    public static final int SHARD_FROST = 45;

    public FrostShard(EntityType<? extends FrostShard> type, Level level) {
        super(type, level);
    }

    public FrostShard(Level level, LivingEntity shooter) {
        super(WildspellMobs.FROST_SHARD.get(), shooter, level);
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    protected Item getDefaultItem() {
        return WildspellMobs.RIME_SHARD.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SHATTER) {
            ItemParticleOption shards = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
            for (int i = 0; i < 8; ++i) {
                this.level().addParticle(i % 2 == 0 ? shards : ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                        (this.random.nextDouble() - 0.5) * 0.15, this.random.nextDouble() * 0.1, (this.random.nextDouble() - 0.5) * 0.15);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(this.getOwner() instanceof IceLich && (target instanceof IceLich || IceLich.isMinion(target)));
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target.hurt(this.damageSources().thrown(this, this.getOwner()), 3.0F)) {
            Frost.add(target, SHARD_FROST, 20);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0), this);
        }
    }

    public double lobTo(LivingEntity target, double dx, double dz, float speed) {
        double flightTicks = Math.sqrt(dx * dx + dz * dz) / speed;
        return target.getY(0.5) - this.getY() + 0.5 * GRAVITY * flightTicks * flightTicks;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, EVENT_SHATTER);
            this.playSound(SoundEvents.GLASS_BREAK, 0.4F, 1.8F);
            this.discard();
        }
    }
}
