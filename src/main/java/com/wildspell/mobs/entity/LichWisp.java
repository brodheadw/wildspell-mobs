package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.crypt.LichSouls;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class LichWisp extends Entity {
    public static final double SPEED = 0.3;
    public static final int MAX_AGE = 1200;

    @Nullable
    private UUID soul;
    @Nullable
    private UUID hunting;

    public LichWisp(EntityType<? extends LichWisp> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setGlowingTag(true);
    }

    public LichWisp(Level level, Vec3 at, UUID soul, @Nullable UUID hunting) {
        this(WildspellMobs.LICH_WISP.get(), level);
        this.setPos(at);
        this.soul = soul;
        this.hunting = hunting;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(), this.getZ(), 0.0, 0.01, 0.0);
            this.level().addParticle(WildspellMobs.FROST_MOTE.get(), this.getRandomX(0.4), this.getRandomY(), this.getRandomZ(0.4), 0.0, -0.02, 0.0);
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        LichSouls.Soul soul = LichSouls.get(level).soul(this.soul);
        if (soul == null || soul.burned()) {
            this.discard();
            return;
        }
        if (soul.dimension() != level.dimension() || this.tickCount > MAX_AGE) {
            this.arrive(level, soul);
            return;
        }
        Vec3 home = soul.inAltar() ? Vec3.atCenterOf(soul.anchor()) : Vec3.atCenterOf(soul.anchor()).add(0.0, 1.0, 0.0);
        Vec3 to = home.subtract(this.position());
        if (to.length() <= SPEED) {
            this.arrive(level, soul);
            return;
        }
        Vec3 weave = new Vec3(Math.sin(this.tickCount * 0.3) * 0.04, Math.cos(this.tickCount * 0.23) * 0.04, 0.0);
        this.setDeltaMovement(to.normalize().scale(SPEED).add(weave));
        this.setPos(this.position().add(this.getDeltaMovement()));
        if (this.tickCount % 20 == 0) {
            this.playSound(SoundEvents.SOUL_ESCAPE.value(), 0.6F, 0.8F);
        }
    }

    private void arrive(ServerLevel level, LichSouls.Soul soul) {
        soul.onWispArrived(level, this.hunting);
        level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(), this.getZ(), 20, 0.2, 0.3, 0.2, 0.03);
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 0.5F);
        this.discard();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.soul = tag.hasUUID("Soul") ? tag.getUUID("Soul") : null;
        this.hunting = tag.hasUUID("Hunting") ? tag.getUUID("Hunting") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.soul != null) {
            tag.putUUID("Soul", this.soul);
        }
        if (this.hunting != null) {
            tag.putUUID("Hunting", this.hunting);
        }
    }
}
