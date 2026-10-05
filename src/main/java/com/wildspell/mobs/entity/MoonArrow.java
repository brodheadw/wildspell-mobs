package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class MoonArrow extends AbstractArrow {
    public static final double SPEED = 3.0;
    private static final double BASE_DAMAGE = 2.5;
    private static final int LINGER = 60;

    public MoonArrow(EntityType<? extends MoonArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    public MoonArrow(Level level, LivingEntity shooter) {
        super(WildspellMobs.MOON_ARROW.get(), shooter, level, new ItemStack(Items.ARROW), null);
        this.setNoGravity(true);
        this.setBaseDamage(BASE_DAMAGE);
        this.pickup = Pickup.DISALLOWED;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && !this.inGround) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void tickDespawn() {
        if (this.inGroundTime > LINGER) {
            this.discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof Diana);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Items.ARROW);
    }
}
