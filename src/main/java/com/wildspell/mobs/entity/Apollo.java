package com.wildspell.mobs.entity;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.gods.Gaze;
import com.wildspell.mobs.gods.Heavens;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

public class Apollo extends HoveringBoss {
    public static final int ACTION_IDLE = 0;
    public static final int ACTION_THROW = 1;
    public static final int ACTION_FOCUS = 2;
    public static final int ACTION_GLARE = 3;
    public static final int ACTION_CONCEDE = 4;
    public static final int ACTION_WARNED = 5;
    public static final int ACTION_DESCEND = 6;
    public static final int ACTION_LEAVE = 7;
    public static final int MAX_RAYS = 12;
    public static final float CONCEDE_AT = 0.15F;
    public static final float WARY_CONCEDE_AT = 0.3F;
    public static final int THROW_TICKS = 20;
    public static final int FOCUS_TICKS = 120;
    public static final int GLARE_WINDUP = 36;
    public static final double GLARE_CONE = 70.0;
    public static final float GLARE_DAMAGE = 6.0F;
    public static final int SPARE_HOLD = 60;
    public static final int CONCEDE_PATIENCE = 1200;
    public static final int WARNING_TICKS = 600;
    public static final double FOCUS_SPEED = 0.32;
    public static final double FOCUS_REACH = 1.3;
    private static final double JOIN_RANGE = 96.0;
    private static final double LEAVE_RANGE = 160.0;
    private static final int ALONE_LIMIT = 5;
    private static final int DESCEND_TICKS = 60;
    private static final int LEAVE_TICKS = 40;
    private static final double ARRIVE_DISTANCE = 40.0;
    private static final EntityDataAccessor<Byte> DATA_RAYS = SynchedEntityData.defineId(Apollo.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.apollo.idle");
    private static final RawAnimation THROW = RawAnimation.begin().thenPlay("animation.apollo.throw");
    private static final RawAnimation FOCUS = RawAnimation.begin().thenLoop("animation.apollo.focus");
    private static final RawAnimation GLARE = RawAnimation.begin().thenPlay("animation.apollo.glare");
    private static final RawAnimation CONCEDE = RawAnimation.begin().thenLoop("animation.apollo.concede");
    private static final RawAnimation WARNED = RawAnimation.begin().thenLoop("animation.apollo.warned");

    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> strikers = new HashSet<>();
    private int throwCooldown = 30;
    private int focusCooldown = 140;
    private int glareCooldown = 200;
    private int regrowTicks;
    private int pulseCooldown;
    private int aloneSeconds;
    private int openHandTicks;
    private int focusTicks;
    private Vec3 focus = Vec3.ZERO;
    private Vec3 orbit = Vec3.ZERO;
    private int repickTicks;
    private boolean wary;
    private boolean restored;
    private boolean slain;

    public Apollo(EntityType<? extends Apollo> type, Level level) {
        super(type, level, BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_12);
        this.moveControl = new GlideMoveControl(this);
        this.xpReward = 500;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.FOLLOW_RANGE, JOIN_RANGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Nullable
    public static Apollo descend(ServerLevel level, Player player, boolean wary) {
        Apollo apollo = level.getDifficulty() == Difficulty.PEACEFUL ? null : WildspellMobs.APOLLO.get().create(level);
        if (apollo == null) {
            return null;
        }
        Vec3 from = player.getEyePosition().add(Gaze.SUN.direction(level).scale(ARRIVE_DISTANCE));
        apollo.moveTo(from.x, from.y, from.z, player.getYRot() + 180.0F, 0.0F);
        apollo.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(from)), MobSpawnType.EVENT, null);
        apollo.wary = wary;
        apollo.participants.add(player.getUUID());
        if (!level.addFreshEntity(apollo)) {
            return null;
        }
        Heavens.get(level).apolloArrived(apollo);
        apollo.startAction(ACTION_DESCEND, DESCEND_TICKS);
        apollo.getMoveControl().setWantedPosition(player.getX(), player.getY() + 6.0, player.getZ(), 1.0);
        level.sendParticles(ParticleTypes.FLASH, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3.0F, 0.5F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.HOSTILE, 1.5F, 1.6F);
        player.displayClientMessage(Component.translatable(wary ? "message.wildspellmobs.apollo_arrives_wary" : "message.wildspellmobs.apollo_arrives",
                apollo.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        return apollo;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_RAYS, (byte) MAX_RAYS);
    }

    public int rays() {
        return this.entityData.get(DATA_RAYS);
    }

    private void setRays(int rays) {
        this.entityData.set(DATA_RAYS, (byte) Mth.clamp(rays, 0, MAX_RAYS));
    }

    public boolean isEnraged() {
        return this.getHealth() < this.getMaxHealth() / 2.0F;
    }

    public boolean hasConceded() {
        int action = this.getAction();
        return action == ACTION_CONCEDE || action == ACTION_WARNED;
    }

    public boolean isWarned() {
        return this.getAction() == ACTION_WARNED;
    }

    public boolean isLeaving() {
        return this.getAction() == ACTION_LEAVE;
    }

    public float concedeHealth() {
        return this.getMaxHealth() * (this.wary ? WARY_CONCEDE_AT : CONCEDE_AT);
    }

    public void join(Player player) {
        if (this.participants.add(player.getUUID()) && player instanceof ServerPlayer server) {
            this.bossEvent.addPlayer(server);
        }
    }

    // His team runs five blocks ahead of his hitbox; keep it drawn while he is at the edge of the screen.
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(5.0, 1.0, 5.0);
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
            this.clientEffects();
            return;
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        if (this.tickCount % 20 == 0) {
            this.refreshParticipants(level);
            if (this.isRemoved()) {
                return;
            }
        }
        int action = this.getAction();
        if (action == ACTION_LEAVE) {
            this.tickLeave(level);
            return;
        }
        if (action == ACTION_DESCEND) {
            if (--this.actionTicks <= 0) {
                this.startAction(ACTION_IDLE, 0);
            }
            this.emberTrail(level);
            return;
        }
        if (!this.hasConceded() && this.getHealth() <= this.concedeHealth()) {
            this.concede();
            return;
        }
        if (this.hasConceded()) {
            this.tickConceded(level);
            return;
        }
        this.tickFight(level);
    }

    private void refreshParticipants(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (Gaze.isAloft(player) && player.distanceTo(this) < JOIN_RANGE) {
                this.join(player);
            }
        }
        this.participants.removeIf(id -> {
            Player player = level.getPlayerByUUID(id);
            boolean gone = player == null || !Gaze.isAloft(player) || player.distanceTo(this) > LEAVE_RANGE;
            if (gone && player instanceof ServerPlayer server) {
                this.bossEvent.removePlayer(server);
            }
            return gone;
        });
        if (!this.participants.isEmpty()) {
            this.aloneSeconds = 0;
        } else if (++this.aloneSeconds >= ALONE_LIMIT) {
            this.withdraw();
        }
    }

    private List<Player> present() {
        List<Player> out = new ArrayList<>();
        for (UUID id : this.participants) {
            Player player = this.level().getPlayerByUUID(id);
            if (player != null) {
                out.add(player);
            }
        }
        return out;
    }

    @Nullable
    private Player nearestParticipant() {
        Player best = null;
        for (Player player : this.present()) {
            if (best == null || player.distanceToSqr(this) < best.distanceToSqr(this)) {
                best = player;
            }
        }
        return best;
    }

    private void tickFight(ServerLevel level) {
        Player target = this.nearestParticipant();
        this.setTarget(target);
        this.throwCooldown--;
        this.focusCooldown--;
        this.glareCooldown--;
        this.pulseCooldown--;
        if (this.getAction() != ACTION_THROW && this.rays() < MAX_RAYS && ++this.regrowTicks >= 30) {
            this.regrowTicks = 0;
            this.setRays(this.rays() + 1);
        }
        if (target == null) {
            return;
        }
        this.getLookControl().setLookAt(target, 30.0F, 30.0F);
        this.keepToTheSun(target);
        this.searingPulse(level);
        if (this.focusTicks > 0) {
            this.tickFocus(level, target);
        }
        if (this.actionTicks > 0) {
            this.tickAction(level, target);
            return;
        }
        boolean enraged = this.isEnraged();
        int volley = enraged ? 3 : 2;
        if (this.glareCooldown <= 0) {
            this.startAction(ACTION_GLARE, enraged ? GLARE_WINDUP - 8 : GLARE_WINDUP);
            this.glareCooldown = enraged ? 200 : 280;
            this.playSound(SoundEvents.BEACON_POWER_SELECT, 3.0F, 0.6F);
            this.tell(Component.translatable("message.wildspellmobs.apollo_glare").withStyle(ChatFormatting.YELLOW), true);
        } else if (this.focusCooldown <= 0 && this.focusTicks <= 0) {
            this.startAction(ACTION_FOCUS, 30);
            this.focus = this.palm();
            this.focusTicks = enraged ? FOCUS_TICKS + 40 : FOCUS_TICKS;
            this.focusCooldown = enraged ? 220 : 300;
            this.playSound(SoundEvents.FIRECHARGE_USE, 2.0F, 0.5F);
        } else if (this.throwCooldown <= 0 && this.rays() >= volley) {
            this.startAction(ACTION_THROW, THROW_TICKS);
            this.throwCooldown = enraged ? 36 : 50;
        }
    }

    private void tickAction(ServerLevel level, Player target) {
        int action = this.getAction();
        --this.actionTicks;
        if (action == ACTION_THROW) {
            int sinceWindup = THROW_TICKS - this.actionTicks - 8;
            int volley = this.isEnraged() ? 3 : 2;
            if (sinceWindup >= 0 && sinceWindup % 4 == 0 && sinceWindup / 4 < volley && this.rays() > 0) {
                this.throwRay(level, target, sinceWindup / 4 - (volley - 1) / 2.0);
            }
        } else if (action == ACTION_GLARE) {
            this.tickGlare(level);
        }
        if (this.actionTicks <= 0) {
            this.startAction(ACTION_IDLE, 0);
        }
    }

    private Vec3 crown() {
        return this.position().add(0.0, this.getBbHeight() * 0.85, 0.0).add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-0.5));
    }

    private Vec3 palm() {
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 right = Vec3.directionFromRotation(0.0F, this.yBodyRot + 90.0F);
        return this.position().add(0.0, this.getBbHeight() * 0.62, 0.0).add(forward.scale(1.1)).add(right.scale(-0.6));
    }

    private void throwRay(ServerLevel level, Player target, double spread) {
        Vec3 from = this.crown();
        Vec3 to = target.getEyePosition().subtract(from).normalize();
        Vec3 side = to.cross(new Vec3(0.0, 1.0, 0.0));
        Vec3 dir = side.lengthSqr() < 1.0E-4 ? to : to.add(side.normalize().scale(spread * 0.08)).normalize();
        SolarRay ray = new SolarRay(level, this, dir);
        ray.setPos(from);
        level.addFreshEntity(ray);
        this.setRays(this.rays() - 1);
        this.playSound(SoundEvents.TRIDENT_THROW.value(), 2.0F, 1.4F);
        this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 1.8F);
    }

    private void tickFocus(ServerLevel level, Player target) {
        --this.focusTicks;
        Vec3 aim = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 step = aim.subtract(this.focus);
        double distance = step.length();
        this.focus = distance <= FOCUS_SPEED ? aim : this.focus.add(step.scale(FOCUS_SPEED / distance));
        Vec3 palm = this.palm();
        Vec3 beam = this.focus.subtract(palm);
        for (int i = 1; i <= 6; ++i) {
            Vec3 p = palm.add(beam.scale(i / 7.0));
            level.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(ParticleTypes.END_ROD, this.focus.x, this.focus.y, this.focus.z, 2, 0.05, 0.05, 0.05, 0.0);
        level.sendParticles(ParticleTypes.SMALL_FLAME, this.focus.x, this.focus.y, this.focus.z, 2, 0.08, 0.08, 0.08, 0.01);
        if (this.focusTicks % 10 != 0) {
            return;
        }
        level.playSound(null, this.focus.x, this.focus.y, this.focus.z, SoundEvents.FIRE_AMBIENT, SoundSource.HOSTILE, 1.5F, 1.4F);
        for (Player player : this.present()) {
            if (focusTouches(this.focus, player)) {
                if (player.hurt(this.damageSources().indirectMagic(this, this), 3.0F)) {
                    player.igniteForTicks(60);
                }
                level.sendParticles(ParticleTypes.LAVA, this.focus.x, this.focus.y, this.focus.z, 3, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    public static boolean focusTouches(Vec3 focus, LivingEntity entity) {
        return entity.getBoundingBox().inflate(FOCUS_REACH - 0.5).contains(focus);
    }

    private void tickGlare(ServerLevel level) {
        Vec3 at = this.crown();
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 1.0, 1.0, 1.0, 0.02);
        if (this.actionTicks % 6 == 0) {
            this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 3.0F, 0.5F + 1.5F * (1.0F - this.actionTicks / (float) GLARE_WINDUP));
        }
        if (this.actionTicks != 0) {
            return;
        }
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 80, 0.3, 0.3, 0.3, 0.6);
        this.playSound(SoundEvents.FIREWORK_ROCKET_BLAST_FAR, 4.0F, 0.5F);
        for (Player player : this.present()) {
            if (isFacing(player, at, GLARE_CONE)) {
                this.blind(player);
            }
        }
    }

    public static boolean isFacing(LivingEntity viewer, Vec3 at, double cone) {
        return Gaze.degreesBetween(viewer.getViewVector(1.0F), at.subtract(viewer.getEyePosition())) <= cone;
    }

    private void blind(Player player) {
        if (player.hurt(this.damageSources().indirectMagic(this, this), GLARE_DAMAGE)) {
            player.igniteForTicks(60);
        }
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0), this);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), this);
    }

    private void searingPulse(ServerLevel level) {
        if (this.pulseCooldown > 0) {
            return;
        }
        boolean pushed = false;
        for (Player player : this.present()) {
            if (player.distanceTo(this) < 3.5) {
                Vec3 away = player.position().subtract(this.position()).normalize().scale(1.6);
                player.push(away.x, away.y + 0.3, away.z);
                player.hurtMarked = true;
                if (player.hurt(this.damageSources().mobAttack(this), 4.0F)) {
                    player.igniteForTicks(60);
                }
                pushed = true;
            }
        }
        if (pushed) {
            this.pulseCooldown = 40;
            level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5), this.getZ(), 40, 1.0, 1.4, 1.0, 0.15);
            this.playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
        }
    }

    private void keepToTheSun(Player target) {
        int action = this.getAction();
        if (--this.repickTicks > 0 && action != ACTION_FOCUS) {
            return;
        }
        this.repickTicks = 30;
        if (this.orbit == Vec3.ZERO || this.random.nextInt(3) == 0) {
            double angle = this.random.nextDouble() * Math.PI * 2.0;
            this.orbit = new Vec3(Math.cos(angle) * 5.0, (this.random.nextDouble() - 0.5) * 4.0, Math.sin(angle) * 5.0);
        }
        Vec3 spot = sunwardSpot(target.getEyePosition(), Gaze.SUN.direction(this.level()), this.wary ? 20.0 : 13.0).add(this.orbit);
        this.getMoveControl().setWantedPosition(spot.x, spot.y - this.getBbHeight() * 0.5, spot.z, 1.0);
    }

    public static Vec3 sunwardSpot(Vec3 from, Vec3 sun, double distance) {
        Vec3 dir = sun.y < 0.25 ? new Vec3(sun.x, 0.25, sun.z).normalize() : sun;
        return from.add(dir.scale(distance));
    }

    public void concede() {
        this.startAction(ACTION_CONCEDE, CONCEDE_PATIENCE);
        this.setHealth(Math.max(1.0F, Math.min(this.getHealth(), this.concedeHealth())));
        this.focusTicks = 0;
        this.openHandTicks = 0;
        this.setTarget(null);
        this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 1.0);
        this.bossEvent.setColor(BossEvent.BossBarColor.WHITE);
        this.playSound(SoundEvents.BEACON_DEACTIVATE, 3.0F, 0.6F);
        this.tell(Component.translatable(this.wary ? "message.wildspellmobs.apollo_concedes_wary" : "message.wildspellmobs.apollo_concedes",
                this.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
    }

    private void tickConceded(ServerLevel level) {
        this.setDeltaMovement(this.getDeltaMovement().scale(0.8));
        Player nearest = this.nearestParticipant();
        if (nearest != null) {
            this.getLookControl().setLookAt(nearest, 10.0F, 10.0F);
        }
        List<Player> present = this.present();
        if (this.isWarned() && this.actionTicks % 10 == 0) {
            for (Player player : present) {
                eclipseSign(level, player);
            }
        }
        this.openHandTicks = openHanded(present) ? this.openHandTicks + 1 : 0;
        if (this.openHandTicks >= SPARE_HOLD) {
            this.spare(present);
            return;
        }
        if (--this.actionTicks > 0) {
            return;
        }
        if (this.isWarned()) {
            this.startAction(ACTION_CONCEDE, CONCEDE_PATIENCE);
            this.tell(Component.translatable("message.wildspellmobs.apollo_steadies").withStyle(ChatFormatting.GOLD), false);
        } else {
            this.tell(Component.translatable("message.wildspellmobs.apollo_takes_silence").withStyle(ChatFormatting.GOLD), false);
            this.spare(present);
        }
    }

    public static boolean openHanded(Collection<? extends Player> present) {
        return !present.isEmpty() && present.stream().allMatch(player -> player.getMainHandItem().isEmpty());
    }

    public static void eclipseSign(ServerLevel level, Player player) {
        if (!(player instanceof ServerPlayer viewer)) {
            return;
        }
        Vec3 sun = Gaze.SUN.direction(level);
        Vec3 center = player.getEyePosition().add(sun.scale(48.0));
        Vec3 u = sun.cross(new Vec3(0.0, 0.0, 1.0)).normalize();
        Vec3 v = sun.cross(u).normalize();
        for (int i = 0; i < 32; ++i) {
            double a = i / 32.0 * Math.PI * 2.0;
            Vec3 p = center.add(u.scale(Math.cos(a) * 3.2)).add(v.scale(Math.sin(a) * 3.2));
            level.sendParticles(viewer, ParticleTypes.SQUID_INK, true, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.sendParticles(viewer, ParticleTypes.LARGE_SMOKE, true, center.x, center.y, center.z, 12, 1.2, 1.2, 1.2, 0.0);
    }

    public void spare(Collection<? extends Player> present) {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Heavens heavens = Heavens.get(level);
        MinecraftServer server = level.getServer();
        Set<UUID> marked = new HashSet<>();
        for (Player player : present) {
            Heavens.markSpared(player, Heavens.APOLLO);
            marked.add(player.getUUID());
        }
        for (UUID id : this.participants) {
            if (!marked.contains(id)) {
                heavens.spare(server, id, Heavens.APOLLO);
            }
        }
        heavens.apolloSpared();
        for (Player player : present) {
            player.displayClientMessage(Component.translatable("message.wildspellmobs.apollo_spared", this.getDisplayName()).withStyle(ChatFormatting.GOLD), false);
        }
        this.playSound(SoundEvents.PLAYER_LEVELUP, 2.0F, 0.6F);
        this.withdraw();
    }

    public void withdraw() {
        if (this.isLeaving()) {
            return;
        }
        this.startAction(ACTION_LEAVE, LEAVE_TICKS);
        this.focusTicks = 0;
        this.bossEvent.removeAllPlayers();
        if (this.level() instanceof ServerLevel level) {
            Heavens.get(level).apolloGone(this);
        }
    }

    private void tickLeave(ServerLevel level) {
        Vec3 sun = Gaze.SUN.direction(level);
        this.setDeltaMovement(sun.scale(0.15 + (LEAVE_TICKS - this.actionTicks) * 0.04));
        this.emberTrail(level);
        if (--this.actionTicks <= 0) {
            level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(0.6), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            this.discard();
        }
    }

    private void emberTrail(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(0.5), this.getZ(), 3, 0.5, 1.0, 0.5, 0.02);
    }

    private void tell(Component message, boolean actionBar) {
        for (Player player : this.present()) {
            player.displayClientMessage(message, actionBar);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide || this.isLeaving() || this.getAction() == ACTION_DESCEND && !(source.getEntity() instanceof Player)) {
            return false;
        }
        if (!this.hasConceded()) {
            return super.hurt(source, amount);
        }
        if (!(source.getEntity() instanceof Player striker)) {
            return false;
        }
        this.strikers.add(striker.getUUID());
        if (!this.isWarned()) {
            this.warn(striker);
            return false;
        }
        this.slain = true;
        this.setHealth(1.0F);
        this.invulnerableTime = 0;
        return super.hurt(source, Float.MAX_VALUE);
    }

    private void warn(Player striker) {
        this.startAction(ACTION_WARNED, WARNING_TICKS);
        this.openHandTicks = 0;
        this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        this.playSound(SoundEvents.BEACON_DEACTIVATE, 4.0F, 0.4F);
        this.playSound(SoundEvents.WARDEN_HEARTBEAT, 4.0F, 0.5F);
        ServerLevel level = (ServerLevel) this.level();
        List<Player> present = this.present();
        if (!present.contains(striker)) {
            present.add(striker);
        }
        for (Player player : present) {
            player.displayClientMessage(Component.translatable("message.wildspellmobs.apollo_warning").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), false);
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0), this);
            eclipseSign(level, player);
            if (player instanceof ServerPlayer server) {
                server.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("title.wildspellmobs.apollo_warning").withStyle(ChatFormatting.RED)));
                server.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("subtitle.wildspellmobs.apollo_warning")));
            }
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.slain) {
            this.setHealth(this.concedeHealth());
            if (!this.hasConceded()) {
                this.concede();
            }
            return;
        }
        super.die(source);
        if (this.level() instanceof ServerLevel level) {
            this.putOutTheSun(level, source.getEntity() instanceof Player killer ? killer : null);
        }
    }

    private void putOutTheSun(ServerLevel level, @Nullable Player killer) {
        Heavens heavens = Heavens.get(level);
        Set<UUID> slayers = new HashSet<>(this.strikers);
        if (killer != null) {
            slayers.add(killer.getUUID());
        }
        heavens.apolloGone(this);
        heavens.slayTheSun(level.getServer(), slayers);
        this.bossEvent.removeAllPlayers();
        level.sendParticles(ParticleTypes.FLASH, this.getX(), this.getY(0.6), this.getZ(), 3, 0.5, 0.5, 0.5, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.6), this.getZ(), 120, 1.5, 2.0, 1.5, 0.08);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            player.displayClientMessage(Component.translatable("message.wildspellmobs.sun_slain").withStyle(ChatFormatting.DARK_RED), false);
            player.playNotifySound(SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.6F, 0.4F);
        }
    }

    private void clientEffects() {
        int action = this.getAction();
        Vec3 crown = this.position().add(0.0, this.getBbHeight() * 0.85, 0.0);
        if (this.random.nextInt(this.hasConceded() ? 6 : 2) == 0) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            this.level().addParticle(ParticleTypes.END_ROD, crown.x + Math.cos(a) * 1.4, crown.y + Math.sin(a) * 1.4, crown.z,
                    Math.cos(a) * 0.02, Math.sin(a) * 0.02, 0.0);
        }
        if (action == ACTION_GLARE) {
            this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(1.2), this.getRandomY(), this.getRandomZ(1.2), 0.0, 0.02, 0.0);
        }
        if (action == ACTION_WARNED && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.8), this.getY(0.8), this.getRandomZ(0.8), 0.0, 0.03, 0.0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Withdraw", true);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.restored = tag.getBoolean("Withdraw");
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        if (this.level() instanceof ServerLevel level) {
            Heavens.get(level).apolloGone(this);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BEACON_DEACTIVATE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 5, state -> state.setAndContinue(switch (this.getAction()) {
            case ACTION_THROW -> THROW;
            case ACTION_FOCUS -> FOCUS;
            case ACTION_GLARE -> GLARE;
            case ACTION_CONCEDE -> CONCEDE;
            case ACTION_WARNED -> WARNED;
            default -> IDLE;
        })));
    }

    private static class GlideMoveControl extends ThrustMoveControl {
        GlideMoveControl(Apollo apollo) {
            super(apollo, 0.025, 0.6);
        }

        @Override
        protected Vec3 drift(Vec3 motion) {
            return motion.scale(0.9).add(0.0, Mth.sin(this.mob.tickCount * 0.05F) * 0.003, 0.0);
        }

        @Override
        protected double arrival() {
            return 1.0;
        }

        @Override
        protected void face(@Nullable Vec3 heading) {
            this.faceTarget();
        }
    }
}
