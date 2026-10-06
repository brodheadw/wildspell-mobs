package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.gods.Gaze;
import com.wildspell.mobs.gods.Heavens;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Diana extends Monster implements GeoEntity {
    public static final int ACTION_IDLE = 0;
    public static final int ACTION_DRAW = 1;
    public static final int ACTION_LOOSE = 2;
    public static final int ACTION_SLASH = 3;
    public static final int ACTION_YIELD = 4;
    public static final int ACTION_DEPART = 5;
    public static final float YIELD_AT = 0.25F;
    public static final int DRAW_TICKS = 30;
    public static final int GRIEF_DRAW_TICKS = 22;
    public static final int QUICK_DRAW_TICKS = 12;
    public static final int HOLD_TICKS = 30;
    public static final double SNIPE_RANGE = 64.0;
    public static final int PHASES = 5;
    private static final double LOSE_RANGE = 112.0;
    private static final int LOSE_UNSEEN = 300;
    private static final int YIELD_TICKS = 60;
    private static final int DEPART_TICKS = 40;
    private static final EntityDataAccessor<Byte> DATA_ACTION = SynchedEntityData.defineId(Diana.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_GRIEVING = SynchedEntityData.defineId(Diana.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.diana.idle");
    private static final RawAnimation DRAW = RawAnimation.begin().thenPlay("animation.diana.draw").thenLoop("animation.diana.hold");
    private static final RawAnimation LOOSE = RawAnimation.begin().thenPlay("animation.diana.loose");
    private static final RawAnimation SLASH = RawAnimation.begin().thenPlay("animation.diana.slash");
    private static final RawAnimation YIELD = RawAnimation.begin().thenPlay("animation.diana.yield").thenLoop("animation.diana.kneel");

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(Component.translatable("bossbar.wildspellmobs.diana_hunt"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int actionTicks;
    private int drawn;
    private int volleyLeft;
    private int shotCooldown = 40;
    private int slashCooldown;
    private int unseenTicks;
    private int repickTicks;
    @Nullable
    private Vec3 lastSeen;
    private boolean restored;

    public Diana(EntityType<? extends Diana> type, Level level) {
        super(type, level);
        this.moveControl = new StalkMoveControl(this);
        this.setNoGravity(true);
        this.xpReward = 0;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(Attributes.FOLLOW_RANGE, LOSE_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
    }

    @Nullable
    public static Diana arrive(ServerLevel level, Player quarry, Heavens.Hunt hunt) {
        Diana diana = level.getDifficulty() == Difficulty.PEACEFUL ? null : WildspellMobs.DIANA.get().create(level);
        if (diana == null) {
            return null;
        }
        Vec3 spot = vantage(diana, quarry, 30.0, 10.0, 8.0);
        if (spot == null) {
            spot = vantage(diana, quarry, 10.0, 6.0, 3.0);
        }
        if (spot == null) {
            spot = quarry.position().add(0.0, 4.0, 0.0);
        }
        boolean returning = hunt.health() > 0.0F;
        diana.moveTo(spot.x, spot.y, spot.z, 0.0F, 0.0F);
        diana.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), MobSpawnType.EVENT, null);
        diana.entityData.set(DATA_GRIEVING, hunt.grieving);
        if (returning) {
            diana.setHealth(Math.max(diana.yieldHealth() + 1.0F, hunt.health()));
        }
        if (!level.addFreshEntity(diana)) {
            return null;
        }
        hunt.claim(diana);
        hunt.setHealth(diana.getHealth());
        diana.setTarget(quarry);
        diana.lastSeen = quarry.getEyePosition();
        level.sendParticles(ParticleTypes.END_ROD, spot.x, spot.y + 1.0, spot.z, 40, 0.4, 1.0, 0.4, 0.05);
        diana.horn(quarry);
        String key = hunt.grieving ? "message.wildspellmobs.diana_arrives_grieving" : returning ? "message.wildspellmobs.diana_returns" : "message.wildspellmobs.diana_arrives";
        quarry.displayClientMessage(Component.translatable(key).withStyle(hunt.grieving ? ChatFormatting.DARK_RED : ChatFormatting.AQUA), false);
        return diana;
    }

    public void greet(Player quarry) {
        this.horn(quarry);
        quarry.displayClientMessage(Component.translatable(this.isGrieving() ? "message.wildspellmobs.diana_arrives_grieving" : "message.wildspellmobs.diana_joins")
                .withStyle(this.isGrieving() ? ChatFormatting.DARK_RED : ChatFormatting.AQUA), false);
    }

    private void horn(Player quarry) {
        this.level().playSound(null, quarry.getX(), quarry.getY(), quarry.getZ(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(2).value(),
                SoundSource.HOSTILE, 4.0F, this.isGrieving() ? 0.7F : 1.0F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, (byte) ACTION_IDLE);
        builder.define(DATA_GRIEVING, false);
    }

    public int getAction() {
        return this.entityData.get(DATA_ACTION);
    }

    private void startAction(int action, int ticks) {
        this.entityData.set(DATA_ACTION, (byte) action);
        this.actionTicks = ticks;
    }

    public boolean isGrieving() {
        return this.entityData.get(DATA_GRIEVING);
    }

    public boolean isDrawing() {
        return this.getAction() == ACTION_DRAW;
    }

    public boolean hasYielded() {
        return this.getAction() == ACTION_YIELD || this.getAction() == ACTION_DEPART;
    }

    public float yieldHealth() {
        return this.getMaxHealth() * YIELD_AT;
    }

    public int moonPhase() {
        return moonPhase(this.getHealth(), this.getMaxHealth());
    }

    public static int moonPhase(float health, float maxHealth) {
        float floor = maxHealth * YIELD_AT;
        float left = Mth.clamp((health - floor) / (maxHealth - floor), 0.0F, 1.0F);
        return Math.min(PHASES - 1, (int) ((1.0F - left) * PHASES));
    }

    private int drawTicks() {
        if (this.volleyLeft > 0) {
            return QUICK_DRAW_TICKS;
        }
        return this.isGrieving() ? GRIEF_DRAW_TICKS : DRAW_TICKS;
    }

    @Nullable
    private Heavens.Hunt hunt() {
        if (!(this.level() instanceof ServerLevel level)) {
            return null;
        }
        Heavens.Hunt hunt = Heavens.get(level).hunt();
        return hunt != null && hunt.isDiana(this) ? hunt : null;
    }

    @Override
    public void tick() {
        if (this.restored && !this.level().isClientSide) {
            this.discard();
            return;
        }
        super.tick();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0.0, -0.01, 0.0);
            }
            if (this.isDrawing()) {
                Vec3 glint = this.bowHand();
                this.level().addParticle(ParticleTypes.END_ROD, glint.x, glint.y, glint.z, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        int action = this.getAction();
        if (action == ACTION_DEPART) {
            this.tickDepart(level);
            return;
        }
        if (action == ACTION_YIELD) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.7));
            if (--this.actionTicks <= 0) {
                this.startAction(ACTION_DEPART, DEPART_TICKS);
            }
            return;
        }
        Heavens.Hunt hunt = this.hunt();
        if (hunt == null) {
            this.startAction(ACTION_DEPART, DEPART_TICKS);
            return;
        }
        if (this.getHealth() <= this.yieldHealth()) {
            this.yieldTo(level);
            return;
        }
        if (this.tickCount % 20 == 0) {
            hunt.setHealth(this.getHealth());
            this.updateBar(level, hunt);
        }
        Player target = this.chooseQuarry(level, hunt);
        this.setTarget(target);
        if (target == null) {
            this.moonstep(level, hunt);
            return;
        }
        boolean sees = this.getSensing().hasLineOfSight(target);
        if (sees) {
            this.lastSeen = target.getEyePosition();
            this.unseenTicks = 0;
        } else if (++this.unseenTicks > LOSE_UNSEEN && this.distanceTo(target) > 48.0) {
            this.moonstep(level, hunt);
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.stalk(target, sees);
        this.shotCooldown--;
        this.slashCooldown--;
        if (action == ACTION_DRAW) {
            this.tickDraw(level, target, sees);
        } else if (this.actionTicks > 0) {
            if (--this.actionTicks <= 0) {
                this.startAction(ACTION_IDLE, 0);
            }
        } else if (sees && this.slashCooldown <= 0 && this.distanceTo(target) < 3.5) {
            this.slash(level, target);
        } else if (sees && (this.shotCooldown <= 0 || this.volleyLeft > 0) && this.distanceTo(target) < SNIPE_RANGE) {
            this.beginDraw();
        }
    }

    @Nullable
    private Player chooseQuarry(ServerLevel level, Heavens.Hunt hunt) {
        Player best = null;
        for (UUID id : hunt.quarry()) {
            Player player = level.getPlayerByUUID(id);
            if (player == null || !player.isAlive() || player.isSpectator() || player.distanceTo(this) > LOSE_RANGE) {
                continue;
            }
            if (best == null || player.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = player;
            }
        }
        return best;
    }

    private void updateBar(ServerLevel level, Heavens.Hunt hunt) {
        for (ServerPlayer player : this.bossEvent.getPlayers().toArray(ServerPlayer[]::new)) {
            if (!hunt.quarry().contains(player.getUUID()) || player.level() != level) {
                this.bossEvent.removePlayer(player);
            }
        }
        for (UUID id : hunt.quarry()) {
            if (level.getPlayerByUUID(id) instanceof ServerPlayer player) {
                this.bossEvent.addPlayer(player);
            }
        }
        ServerLevel origin = level.getServer().getLevel(hunt.origin);
        this.bossEvent.setColor(this.isGrieving() ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.BLUE);
        this.bossEvent.setProgress(origin == null ? 1.0F : nightLeft(origin.getTimeOfDay(1.0F)));
    }

    public static float nightLeft(float timeOfDay) {
        return Mth.clamp((0.75F - timeOfDay) / 0.5F, 0.0F, 1.0F);
    }

    private void stalk(Player target, boolean sees) {
        if (--this.repickTicks > 0 || this.isDrawing()) {
            return;
        }
        this.repickTicks = 40 + this.random.nextInt(30);
        Vec3 spot = vantage(this, target, 24.0, 16.0, 6.0);
        if (spot == null && this.lastSeen != null) {
            Vec3 seen = this.lastSeen;
            spot = ColdEffects.findSpot(12, () -> ColdEffects.ringPoint(this.random, seen, 3.0, 6.0, this.random.nextDouble() * 2.0),
                    at -> ColdEffects.isOpen(this.level(), BlockPos.containing(at), 3));
            this.repickTicks = 20;
        }
        if (spot != null) {
            this.getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, sees ? 1.0 : 1.6);
        }
    }

    @Nullable
    private static Vec3 vantage(Diana diana, Player target, double minRadius, double spread, double rise) {
        return ColdEffects.findSpot(16, () -> ColdEffects.ringPoint(diana.random, target.position(), minRadius, spread, rise + diana.random.nextDouble() * rise),
                at -> ColdEffects.isOpen(target.level(), BlockPos.containing(at), 3)
                        && ColdEffects.clearPath(target, at.add(0.0, 1.6, 0.0), target.getEyePosition()));
    }

    private Vec3 bowHand() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 right = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F);
        return this.position().add(0.0, this.getBbHeight() * 0.72, 0.0).add(forward.scale(0.9)).add(right.scale(0.35));
    }

    public void beginDraw() {
        this.startAction(ACTION_DRAW, 0);
        this.drawn = 0;
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 1.0);
        this.glint((ServerLevel) this.level(), true);
    }

    private void glint(ServerLevel level, boolean flash) {
        Vec3 at = this.bowHand();
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(at) < 160.0 * 160.0) {
                if (flash) {
                    level.sendParticles(player, ParticleTypes.FLASH, true, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
                }
                level.sendParticles(player, ParticleTypes.END_ROD, true, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
        }
        if (flash) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 4.0F, 1.9F);
        }
    }

    public void tickDraw(ServerLevel level, Player target, boolean sees) {
        this.drawn++;
        if (this.drawn % 3 == 0) {
            this.glint(level, false);
        }
        if (this.drawn < this.drawTicks()) {
            return;
        }
        if (sees) {
            this.loose(level, target);
        } else if (this.drawn >= this.drawTicks() + HOLD_TICKS) {
            this.startAction(ACTION_IDLE, 0);
            this.volleyLeft = 0;
            this.shotCooldown = 20;
        }
    }

    private void loose(ServerLevel level, Player target) {
        Vec3 from = this.bowHand();
        Vec3 dir = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0).subtract(from).normalize();
        MoonArrow arrow = new MoonArrow(level, this);
        arrow.setPos(from.x, from.y, from.z);
        arrow.shoot(dir.x, dir.y, dir.z, (float) MoonArrow.SPEED, 0.0F);
        level.addFreshEntity(arrow);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ARROW_SHOOT, SoundSource.HOSTILE, 3.0F, 0.7F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 1.6F);
        this.startAction(ACTION_LOOSE, 8);
        if (this.volleyLeft > 0) {
            this.volleyLeft--;
        } else if (this.moonPhase() >= 2) {
            this.volleyLeft = 2;
        }
        this.shotCooldown = this.isGrieving() ? 30 : 45;
    }

    private void slash(ServerLevel level, Player target) {
        this.startAction(ACTION_SLASH, 12);
        this.slashCooldown = 50;
        this.doHurtTarget(target);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.6), target.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        this.playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 1.4F);
        Vec3 away = this.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize();
        this.setDeltaMovement(away.scale(1.6).add(0.0, 0.8, 0.0));
        this.repickTicks = 0;
    }

    private void moonstep(ServerLevel level, Heavens.Hunt hunt) {
        hunt.setHealth(this.getHealth());
        hunt.release(this);
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(0.5), this.getZ(), 40, 0.4, 1.0, 0.4, 0.08);
        this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.5F, 1.2F);
        this.bossEvent.removeAllPlayers();
        this.discard();
    }

    private void yieldTo(ServerLevel level) {
        this.startAction(ACTION_YIELD, YIELD_TICKS);
        this.setHealth(Math.max(1.0F, Math.min(this.getHealth(), this.yieldHealth())));
        this.setTarget(null);
        this.bossEvent.removeAllPlayers();
        this.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.5F);
        Heavens.Hunt hunt = this.hunt();
        if (hunt != null) {
            for (UUID id : hunt.quarry()) {
                if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer player) {
                    player.displayClientMessage(Component.translatable(this.isGrieving() ? "message.wildspellmobs.diana_yields_grieving"
                            : "message.wildspellmobs.diana_yields").withStyle(this.isGrieving() ? ChatFormatting.DARK_RED : ChatFormatting.AQUA), false);
                }
            }
            Heavens.get(level).endHunt(level.getServer(), true, null);
        }
    }

    public void depart() {
        this.bossEvent.removeAllPlayers();
        if (!this.hasYielded()) {
            this.startAction(ACTION_DEPART, DEPART_TICKS);
        }
    }

    private void tickDepart(ServerLevel level) {
        Vec3 moon = Gaze.MOON.direction(level);
        Vec3 up = moon.y > 0.2 ? moon : new Vec3(0.0, 1.0, 0.0);
        this.setDeltaMovement(up.scale(0.1 + (DEPART_TICKS - this.actionTicks) * 0.03));
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(0.5), this.getZ(), 3, 0.3, 0.8, 0.3, 0.01);
        if (--this.actionTicks <= 0) {
            this.discard();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.hasYielded()) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && source.getEntity() instanceof Player player) {
            Heavens.Hunt hunt = this.hunt();
            if (hunt != null) {
                hunt.setHealth(this.getHealth());
                hunt.add(player);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        this.setHealth(this.yieldHealth());
        if (!this.hasYielded() && this.level() instanceof ServerLevel level) {
            this.yieldTo(level);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Moonstep", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.restored = tag.getBoolean("Moonstep");
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
        Heavens.Hunt hunt = this.hunt();
        if (hunt != null) {
            hunt.release(this);
        }
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
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.AMETHYST_BLOCK_RESONATE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 3, state -> state.setAndContinue(switch (this.getAction()) {
            case ACTION_DRAW -> DRAW;
            case ACTION_LOOSE -> LOOSE;
            case ACTION_SLASH -> SLASH;
            case ACTION_YIELD, ACTION_DEPART -> YIELD;
            default -> IDLE;
        })));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private static class StalkMoveControl extends ThrustMoveControl {
        StalkMoveControl(Diana diana) {
            super(diana, 0.045, 0.5);
        }

        @Override
        protected Vec3 drift(Vec3 motion) {
            return motion.scale(0.88);
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            this.faceTarget();
        }
    }
}
