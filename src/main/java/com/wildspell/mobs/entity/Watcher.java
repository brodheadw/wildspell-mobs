package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.watch.Face;
import com.wildspell.mobs.watch.Watchers;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Watcher extends Monster implements GeoEntity {
    public static final int IDLE = 0;
    public static final int ARRIVING = 1;
    public static final int WATCHING = 2;
    public static final int WINDUP = 3;
    public static final int WORKING = 4;
    public static final int LOST = 5;
    public static final int WITHDRAWING = 6;

    public static final double EYE = 2.1;
    public static final int ARRIVE_TICKS = 32;
    public static final int WITHDRAW_TICKS = 32;
    public static final int LOST_TICKS = 50;
    public static final int WINDUP_TICKS = 40;
    public static final int RECALL_TICKS = 80;
    public static final int MAX_BREAKS = 3;
    public static final int LIFETIME = 2400;
    public static final int UNSEEN_LIMIT = 200;
    public static final double REACH = 24.0;
    public static final double LOSE_RANGE = 48.0;
    public static final double HEAT_RADIUS = 2.5;
    public static final double HEAT_CREEP = 0.12;
    public static final double GLARE_CONE = 60.0;
    public static final float KNOWING_FOR_TWO = 0.85F;

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(Watcher.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_GAZE = SynchedEntityData.defineId(Watcher.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.watcher.idle");
    private static final RawAnimation ARRIVE_ANIM = RawAnimation.begin().thenPlay("animation.watcher.arrive").thenLoop("animation.watcher.watch");
    private static final RawAnimation WATCH_ANIM = RawAnimation.begin().thenLoop("animation.watcher.watch");
    private static final RawAnimation WINDUP_ANIM = RawAnimation.begin().thenPlayAndHold("animation.watcher.windup");
    private static final RawAnimation WORK_ANIM = RawAnimation.begin().thenLoop("animation.watcher.work");
    private static final RawAnimation LOST_ANIM = RawAnimation.begin().thenLoop("animation.watcher.lost");
    private static final RawAnimation WITHDRAW_ANIM = RawAnimation.begin().thenPlayAndHold("animation.watcher.withdraw");
    private static final RawAnimation DEATH_ANIM = RawAnimation.begin().thenPlayAndHold("animation.watcher.death");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final Face face;
    private final ArrayDeque<Vec3> trail = new ArrayDeque<>();
    @Nullable
    private UUID quarry;
    private float drawnBy;
    private int stateTicks;
    private int watchTicks;
    private int workingsLeft = 1;
    private int breaks;
    private int unseenTicks;
    private int buffetCooldown;
    private boolean restored;
    @Nullable
    private Vec3 mark;
    private List<Vec3> recall = List.of();

    public Watcher(EntityType<? extends Watcher> type, Level level, Face face) {
        super(type, level);
        this.face = face;
        this.moveControl = new HoverControl(this);
        this.setNoGravity(true);
        this.xpReward = 12;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 70.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FLYING_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, LOSE_RANGE);
    }

    public Face face() {
        return this.face;
    }

    @Nullable
    public UUID quarryId() {
        return this.quarry;
    }

    public int state() {
        return this.entityData.get(DATA_STATE);
    }

    @Nullable
    public Entity gaze() {
        int id = this.entityData.get(DATA_GAZE);
        return id < 0 ? null : this.level().getEntity(id);
    }

    @Nullable
    public Vec3 mark() {
        return this.mark;
    }

    public List<Vec3> trail() {
        return List.copyOf(this.trail);
    }

    public void comeFor(Player player, float knowing) {
        this.quarry = player.getUUID();
        this.drawnBy = knowing;
        this.workingsLeft = knowing >= KNOWING_FOR_TWO ? 2 : 1;
        this.watchTicks = (int) Mth.lerp(knowing, 200.0F, 100.0F);
        this.entityData.set(DATA_GAZE, player.getId());
        this.setState(ARRIVING);
    }

    private void setState(int state) {
        this.entityData.set(DATA_STATE, (byte) state);
        this.stateTicks = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) IDLE);
        builder.define(DATA_GAZE, -1);
    }

    @Override
    public void tick() {
        if (this.restored && !this.level().isClientSide) {
            this.discard();
            return;
        }
        super.tick();
    }

    @Nullable
    private Player quarry(ServerLevel level) {
        if (this.quarry == null) {
            return null;
        }
        return level.getEntity(this.quarry) instanceof Player player && player.isAlive() && this.distanceTo(player) <= LOSE_RANGE ? player : null;
    }

    public boolean sees(Player player) {
        return this.distanceTo(player) <= REACH && ColdEffects.clearPath(this, this.getEyePosition(), player.getEyePosition());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        ++this.stateTicks;
        --this.buffetCooldown;
        int state = this.state();
        if (state == WITHDRAWING) {
            this.hold();
            if (this.stateTicks >= WITHDRAW_TICKS) {
                this.discard();
            }
            return;
        }
        if (state == LOST) {
            this.hold();
            if (this.stateTicks >= LOST_TICKS) {
                this.withdraw();
            }
            return;
        }
        if (state == IDLE && this.quarry == null) {
            Player near = level.getNearestPlayer(this, 16.0);
            this.entityData.set(DATA_GAZE, near == null ? -1 : near.getId());
            this.hold();
            return;
        }
        Player quarry = this.quarry(level);
        if (quarry == null || this.tickCount > LIFETIME) {
            this.withdraw();
            return;
        }
        if (!Watchers.stillKnows(quarry, this.drawnBy)) {
            this.lose();
            return;
        }
        this.entityData.set(DATA_GAZE, quarry.getId());
        this.trail.addLast(quarry.position());
        while (this.trail.size() > RECALL_TICKS) {
            this.trail.removeFirst();
        }
        boolean sees = this.sees(quarry);
        if (sees) {
            this.unseenTicks = 0;
        } else if (++this.unseenTicks > UNSEEN_LIMIT) {
            this.lose();
            return;
        }
        this.hover(level, quarry, sees);
        if (this.buffetCooldown <= 0 && this.distanceTo(quarry) < 3.0) {
            this.buffet(quarry);
        }
        switch (state) {
            case ARRIVING -> {
                if (this.stateTicks >= ARRIVE_TICKS) {
                    this.setState(WATCHING);
                }
            }
            case WATCHING, IDLE -> {
                if (this.stateTicks >= this.watchTicks && sees) {
                    this.beginWindup();
                }
            }
            case WINDUP -> {
                if (!sees) {
                    this.broken();
                } else if (this.stateTicks >= WINDUP_TICKS) {
                    this.beginWork(level, quarry);
                }
            }
            case WORKING -> {
                if (!this.work(level, quarry, sees)) {
                    this.workDone();
                }
            }
            default -> {
            }
        }
    }

    private void hold() {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.6));
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
    }

    private void hover(ServerLevel level, Player quarry, boolean sees) {
        if (this.tickCount % 30 != 0 && sees) {
            return;
        }
        if (!sees && this.tickCount % 10 != 0) {
            return;
        }
        Vec3 spot = Watchers.vantage(level, quarry);
        if (spot != null && (!sees || spot.distanceTo(this.position()) < 10.0)) {
            this.getMoveControl().setWantedPosition(spot.x, spot.y + 0.5, spot.z, 1.0);
        }
    }

    private void buffet(Player quarry) {
        this.buffetCooldown = 40;
        Vec3 push = quarry.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize().scale(1.4);
        quarry.hurt(this.damageSources().mobAttack(this), 4.0F);
        quarry.push(push.x, 0.45, push.z);
        quarry.hurtMarked = true;
        this.playSound(SoundEvents.PHANTOM_FLAP, 1.6F, 0.5F);
    }

    private void beginWindup() {
        this.setState(WINDUP);
        this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), this.windupSound(), SoundSource.HOSTILE, 2.0F, this.windupPitch());
    }

    private void broken() {
        this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (++this.breaks >= MAX_BREAKS) {
            this.withdraw();
        } else {
            this.watchTicks = 40;
            this.setState(WATCHING);
        }
    }

    private void lose() {
        if (this.state() != LOST) {
            this.mark = null;
            this.setState(LOST);
            this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), SoundEvents.ENDER_EYE_DEATH, SoundSource.HOSTILE, 1.5F, 0.4F);
        }
    }

    public void withdraw() {
        if (this.state() != WITHDRAWING) {
            this.mark = null;
            this.setState(WITHDRAWING);
            this.playSound(SoundEvents.PHANTOM_FLAP, 1.4F, 0.4F);
        }
    }

    private void workDone() {
        this.mark = null;
        if (--this.workingsLeft > 0) {
            this.watchTicks = 60;
            this.setState(WATCHING);
        } else {
            this.withdraw();
        }
    }

    private SoundEvent windupSound() {
        return switch (this.face) {
            case ATHOTH -> SoundEvents.BEACON_DEACTIVATE;
            case ELOAIOU -> SoundEvents.ANVIL_LAND;
            case ASTAPHAIOS -> SoundEvents.FOX_SCREECH;
            case YAO -> SoundEvents.CHAIN_BREAK;
            case SABAOTH -> SoundEvents.TRIDENT_THUNDER.value();
            case ADONIN -> SoundEvents.BEACON_POWER_SELECT;
            case SABBATAIOS -> SoundEvents.BLAZE_SHOOT;
        };
    }

    private float windupPitch() {
        return switch (this.face) {
            case ADONIN -> 1.6F;
            case SABAOTH -> 1.0F;
            default -> 0.45F;
        };
    }

    void beginWork(ServerLevel level, Player quarry) {
        this.setState(WORKING);
        this.mark = quarry.position();
        if (this.face == Face.ATHOTH) {
            List<Vec3> back = new ArrayList<>(this.trail);
            Collections.reverse(back);
            this.recall = back;
        } else if (this.face == Face.ADONIN && Apollo.isFacing(quarry, this.getEyePosition(), GLARE_CONE)) {
            quarry.addEffect(new MobEffectInstance(WildspellMobs.GLARE, 100, 0, false, false, true));
            level.playSound(null, quarry.getX(), quarry.getEyeY(), quarry.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.5F, 2.0F);
        }
    }

    private boolean work(ServerLevel level, Player quarry, boolean sees) {
        int t = this.stateTicks;
        return switch (this.face) {
            case ATHOTH -> this.turnBack(quarry, t, sees);
            case ELOAIOU -> {
                if (sees && t % 10 == 1) {
                    this.press(quarry, WildspellMobs.WEIGHED, 30);
                }
                yield t < 160;
            }
            case ASTAPHAIOS -> {
                if (sees && t % 20 == 1) {
                    this.rot(level, quarry, 2);
                }
                yield t < 120;
            }
            case YAO -> {
                if (sees && t % 70 == 1) {
                    this.press(quarry, WildspellMobs.BOUND, 50);
                    level.playSound(null, quarry.getX(), quarry.getY(), quarry.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.HOSTILE, 1.5F, 0.5F);
                }
                yield t < 160;
            }
            case SABAOTH -> this.storm(level, quarry, t, sees);
            case ADONIN -> t < 20;
            case SABBATAIOS -> this.burn(level, quarry, t, sees);
        };
    }

    private void press(Player quarry, Holder<MobEffect> effect, int ticks) {
        quarry.addEffect(new MobEffectInstance(effect, ticks, 0, false, false, true));
    }

    private boolean turnBack(Player quarry, int t, boolean sees) {
        int step = t * 2;
        if (!sees || step >= this.recall.size()) {
            return false;
        }
        Vec3 to = this.recall.get(step);
        if (!this.level().noCollision(quarry, quarry.getDimensions(quarry.getPose()).makeBoundingBox(to))) {
            return false;
        }
        if (quarry instanceof ServerPlayer player) {
            player.connection.teleport(to.x, to.y, to.z, player.getYRot(), player.getXRot());
        } else {
            quarry.teleportTo(to.x, to.y, to.z);
        }
        quarry.setDeltaMovement(Vec3.ZERO);
        quarry.resetFallDistance();
        if (t == 1) {
            this.level().playSound(null, quarry.getX(), quarry.getY(), quarry.getZ(), SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.2F, 0.5F);
        }
        return true;
    }

    public void rot(ServerLevel level, Player quarry, int count) {
        List<ItemStack> carried = new ArrayList<>();
        for (List<ItemStack> part : List.of(quarry.getInventory().items, quarry.getInventory().armor, quarry.getInventory().offhand)) {
            for (ItemStack stack : part) {
                if (rots(stack)) {
                    carried.add(stack);
                }
            }
        }
        for (int i = 0; i < count && !carried.isEmpty(); ++i) {
            ItemStack stack = carried.remove(level.random.nextInt(carried.size()));
            if (stack.has(DataComponents.FOOD)) {
                stack.shrink(1);
                ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
                if (!quarry.getInventory().add(flesh)) {
                    quarry.drop(flesh, false);
                }
            } else {
                int wear = Math.max(3, stack.getMaxDamage() / 25);
                stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + wear));
            }
        }
        level.playSound(null, quarry.getX(), quarry.getY(), quarry.getZ(), SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.HOSTILE, 1.0F, 0.5F);
    }

    private static boolean rots(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(Items.ROTTEN_FLESH) && (stack.has(DataComponents.FOOD) || stack.isDamageableItem());
    }

    private boolean storm(ServerLevel level, Player quarry, int t, boolean sees) {
        int beat = t % 50;
        if (beat == 1) {
            this.mark = sees ? quarry.position() : null;
            if (this.mark != null) {
                level.playSound(null, this.mark.x, this.mark.y, this.mark.z, SoundEvents.BEEHIVE_WORK, SoundSource.HOSTILE, 2.0F, 2.0F);
            }
        }
        if (this.mark != null && beat < 25 && beat % 5 == 1) {
            this.ring(level, this.mark, 1.2, ParticleTypes.ELECTRIC_SPARK);
        }
        if (this.mark != null && beat == 25) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
            if (bolt != null) {
                bolt.moveTo(this.mark);
                level.addFreshEntity(bolt);
            }
            this.mark = null;
        }
        return t < 150;
    }

    private boolean burn(ServerLevel level, Player quarry, int t, boolean sees) {
        if (this.mark == null) {
            return false;
        }
        if (sees) {
            Vec3 toward = quarry.position().subtract(this.mark);
            double length = toward.length();
            this.mark = length <= HEAT_CREEP ? quarry.position() : this.mark.add(toward.scale(HEAT_CREEP / length));
        }
        if (t % 10 == 1) {
            this.ring(level, this.mark, HEAT_RADIUS, ParticleTypes.SMALL_FLAME);
        }
        if (t % 20 == 1 && inHeat(quarry, this.mark)) {
            quarry.igniteForSeconds(4.0F);
            quarry.hurt(this.damageSources().onFire(), 2.0F);
        }
        if (t == 1) {
            level.playSound(null, this.mark.x, this.mark.y, this.mark.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.5F, 0.4F);
        }
        return t < 120;
    }

    public static boolean inHeat(Player quarry, Vec3 heat) {
        Vec3 off = quarry.position().subtract(heat);
        return off.horizontalDistance() <= HEAT_RADIUS && Math.abs(off.y) <= HEAT_RADIUS && !quarry.fireImmune();
    }

    private void ring(ServerLevel level, Vec3 at, double radius, ParticleOptions particle) {
        for (int i = 0; i < 16; ++i) {
            double a = i * Math.PI / 8.0;
            level.sendParticles(particle, at.x + Math.cos(a) * radius, at.y + 0.1, at.z + Math.sin(a) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && source.getEntity() instanceof Player player && !player.isCreative()) {
            if (this.quarry == null) {
                this.comeFor(player, Watchers.knowing(player));
                this.setState(WATCHING);
            }
            if (player.getUUID().equals(this.quarry) && this.state() == WATCHING) {
                this.beginWindup();
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof Player player) {
            Watchers.felled(player, this.face);
        }
        super.die(source);
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt bolt) {
        if (this.face != Face.SABAOTH) {
            super.thunderHit(level, bolt);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return this.quarry == null && super.shouldBeSaved();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
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
    public boolean canFreeze() {
        return false;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PHANTOM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PHANTOM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.4F + this.random.nextFloat() * 0.1F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.quarry != null) {
            tag.putBoolean("Withdrawn", true);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.restored = tag.getBoolean("Withdrawn");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, state -> {
            if (this.isDeadOrDying()) {
                return state.setAndContinue(DEATH_ANIM);
            }
            return state.setAndContinue(switch (this.state()) {
                case ARRIVING -> ARRIVE_ANIM;
                case WATCHING -> WATCH_ANIM;
                case WINDUP -> WINDUP_ANIM;
                case WORKING -> WORK_ANIM;
                case LOST -> LOST_ANIM;
                case WITHDRAWING -> WITHDRAW_ANIM;
                default -> IDLE_ANIM;
            });
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private static class HoverControl extends ThrustMoveControl {
        HoverControl(Watcher watcher) {
            super(watcher, 0.03, 0.5);
        }

        @Override
        protected Vec3 drift(Vec3 motion) {
            return motion.scale(0.85);
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            Entity gaze = ((Watcher) this.mob).gaze();
            if (gaze == null) {
                return;
            }
            float want = -((float) Mth.atan2(gaze.getX() - this.mob.getX(), gaze.getZ() - this.mob.getZ())) * Mth.RAD_TO_DEG;
            this.mob.setYRot(Mth.approachDegrees(this.mob.getYRot(), want, 4.0F));
            this.mob.yBodyRot = this.mob.getYRot();
            this.mob.yHeadRot = this.mob.getYRot();
        }
    }
}
