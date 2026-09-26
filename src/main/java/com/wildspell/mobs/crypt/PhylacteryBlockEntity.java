package com.wildspell.mobs.crypt;

import com.wildspell.mobs.SpawnBalance;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.ZombieFreezing;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The phylactery's side of the lich's unlife. It
 * <ul>
 *   <li>raises the lich, very rarely, behind a player wandering the Frosted Caves near the crypt, and
 *       sends it hunting them;</li>
 *   <li>re-forms the lich on the altar when its soul (a {@link LichWisp}) flies home after it's struck down;</li>
 *   <li>wakes the crypt when a player walks in: its flames (soul-fire braziers and candles) ignite one
 *       by one and the lich is called home to face them. While it fights there, they burn down with its health;</li>
 *   <li>keeps itself warded, and so unbreakable, while any Rime Ward stands in the crypt.</li>
 * </ul>
 *
 * <p>The lich has at most one form at a time: a body ({@link #lichId}), a soul in flight
 * ({@link #soulTicks}), or a re-forming countdown ({@link #reformTicks}). Nothing raises a new body
 * while it has any of them.
 *
 * <p>All the work happens once a second, and only with a player within {@link #AMBUSH_RANGE}; the
 * crypt's blocks are only touched while all its chunks are loaded, so the phylactery never loads
 * chunks itself.
 */
public class PhylacteryBlockEntity extends BlockEntity {
    /** The crypt room is centred this many blocks in front of the phylactery (see {@link LichCryptPiece}). */
    public static final int CRYPT_CENTER = 9;
    /** Horizontal half-size of the crypt's interior. */
    public static final int CRYPT_HALF = 11;
    /** The crypt floor is this far below the phylactery; its ceiling this far above. */
    public static final int CRYPT_BELOW = 3;
    public static final int CRYPT_ABOVE = 6;
    /** A hunting lich gives up and goes home once its prey is this far from the phylactery. */
    public static final double LEASH = 80.0;
    /** Players within this range of the phylactery can be ambushed; beyond it, the phylactery idles. */
    public static final double AMBUSH_RANGE = 64.0;
    /** Ticks for the lich to re-form after its soul gets home; much quicker while the crypt is awake. */
    public static final int REFORM_TICKS = 400;
    public static final int REFORM_TICKS_AWAKE = 100;
    /** Ticks for the lich to rise when a player first walks into its crypt. */
    public static final int RISE_TICKS = 40;
    /** Seconds (spent with a player near) before the lich can ambush again after giving up a hunt. */
    public static final int RETREAT_COOLDOWN = 300;
    /** Seconds a lich can go unfound before the phylactery stops waiting for it. */
    private static final int MISSING_LIMIT = 60;
    /** Seconds the crypt stays awake with nobody in it. */
    private static final int EMPTY_LIMIT = 30;
    /** Flames left burning however hurt the lich is. */
    private static final int MIN_FLAMES = 4;
    /** Client: how often to redraw the ward beams, and to look for wards again. */
    private static final int BEAM_INTERVAL = 4;
    private static final int WARD_RESCAN_INTERVAL = 40;
    private static final double BEAM_VIEW_RANGE = 32.0;

    @Nullable
    private UUID lichId;
    @Nullable
    private UUID reformHunting;
    private int reformTicks = -1;
    /** Ticks left before a soul in flight is given up for lost. */
    private int soulTicks;
    private int ambushCooldown;
    private int missingSeconds;
    private boolean awake;
    private int emptySeconds;
    /** Braziers still to light as the crypt wakes, nearest the intruder first. */
    private final Deque<BlockPos> toIgnite = new ArrayDeque<>();
    /** Loaded awake: finish lighting any braziers the save caught unlit. */
    private boolean resumeIgnite;
    private boolean dirty;
    /** Client only: the wards the beams are drawn from. */
    private List<BlockPos> wardCache = List.of();

    public PhylacteryBlockEntity(BlockPos pos, BlockState state) {
        super(WildspellMobs.PHYLACTERY.get(), pos, state);
    }

    /** Players the lich hunts and the crypt wakes for: not spectators or creative-mode players. */
    public static boolean isPrey(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    /** The crypt's interior, from its floor to its ceiling. */
    public AABB cryptBounds() {
        BlockPos center = this.worldPosition.relative(this.getBlockState().getValue(PhylacteryBlock.FACING), CRYPT_CENTER);
        return new AABB(center.getX() - CRYPT_HALF, this.worldPosition.getY() - CRYPT_BELOW, center.getZ() - CRYPT_HALF,
                center.getX() + CRYPT_HALF + 1, this.worldPosition.getY() + CRYPT_ABOVE, center.getZ() + CRYPT_HALF + 1);
    }

    /** Every block position in the crypt. */
    public Iterable<BlockPos> cryptBlocks() {
        AABB crypt = this.cryptBounds();
        return BlockPos.betweenClosed(BlockPos.containing(crypt.minX, crypt.minY, crypt.minZ), BlockPos.containing(crypt.maxX - 1, crypt.maxY - 1, crypt.maxZ - 1));
    }

    private boolean cryptLoaded(Level level) {
        AABB crypt = this.cryptBounds();
        return level.hasChunksAt(BlockPos.containing(crypt.minX, crypt.minY, crypt.minZ), BlockPos.containing(crypt.maxX - 1, crypt.maxY - 1, crypt.maxZ - 1));
    }

    @Nullable
    public UUID lichId() {
        return this.lichId;
    }

    /** No body, no soul in flight and no re-forming under way: the lich is waiting to rise. */
    private boolean dormant() {
        return this.lichId == null && this.soulTicks <= 0 && this.reformTicks < 0;
    }

    private static boolean canRaise(Level level) {
        return level.getDifficulty() != Difficulty.PEACEFUL;
    }

    private void changed() {
        this.dirty = true;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, PhylacteryBlockEntity phylactery) {
        ServerLevel server = (ServerLevel) level;
        if (phylactery.reformTicks > 0 && --phylactery.reformTicks == 0) {
            phylactery.reformTicks = -1;
            phylactery.reform(server);
            phylactery.changed();
        }
        if (!phylactery.toIgnite.isEmpty() && level.getGameTime() % 3 == 0) {
            phylactery.igniteNext(server);
        }
        if ((level.getGameTime() + pos.asLong()) % 20 == 0) {
            phylactery.secondTick(server);
        }
        if (phylactery.dirty) {
            phylactery.dirty = false;
            phylactery.setChanged();
        }
    }

    private void secondTick(ServerLevel level) {
        IceLich lich = this.findLich(level);
        if (this.soulTicks > 0) {
            this.soulTicks = Math.max(0, this.soulTicks - 20);
            this.changed();
        }
        List<Player> near = level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(AMBUSH_RANGE), PhylacteryBlockEntity::isPrey);
        if (near.isEmpty()) {
            return;
        }
        AABB crypt = this.cryptBounds();
        if (this.cryptLoaded(level)) {
            this.tendCrypt(level, lich, near, crypt);
        }
        if (!this.awake && this.dormant()) {
            this.tryAmbush(level, near, crypt);
        }
    }

    /** Wards, braziers and intruders: everything that needs the crypt's blocks, so only while they're all loaded. */
    private void tendCrypt(ServerLevel level, @Nullable IceLich lich, List<Player> near, AABB crypt) {
        List<BlockPos> wards = new ArrayList<>();
        List<BlockPos> braziers = new ArrayList<>();
        for (BlockPos p : this.cryptBlocks()) {
            BlockState state = level.getBlockState(p);
            if (state.is(WildspellMobs.RIME_WARD.get())) {
                wards.add(p.immutable());
            } else if (isFlame(state)) {
                braziers.add(p.immutable());
            }
        }
        boolean warded = !wards.isEmpty();
        if (this.getBlockState().getValue(PhylacteryBlock.WARDED) != warded) {
            level.setBlock(this.worldPosition, this.getBlockState().setValue(PhylacteryBlock.WARDED, warded), 3);
        }
        if (this.resumeIgnite) {
            this.resumeIgnite = false;
            this.queueIgnition(level, braziers, this.worldPosition);
        }
        Player intruder = near.stream().filter(p -> crypt.contains(p.position())).findFirst().orElse(null);
        if (intruder != null) {
            this.emptySeconds = 0;
            if (!this.awake) {
                this.awaken(level, intruder, lich, braziers);
            } else if (this.dormant()) {
                // Its old body was lost (unloaded far off, then forgotten): rise afresh for whoever's here.
                this.scheduleRise(intruder);
            }
        } else if (this.awake && ++this.emptySeconds >= EMPTY_LIMIT) {
            this.sleep(level, braziers);
        }
        if (this.awake && this.toIgnite.isEmpty() && lich != null && crypt.contains(lich.position())) {
            this.burnDown(level, braziers, lich.getHealth() / lich.getMaxHealth());
        }
    }

    /** The phylactery's lich, if it's loaded; forgets a lich that has stayed unfound too long. */
    @Nullable
    private IceLich findLich(ServerLevel level) {
        if (this.lichId == null) {
            return null;
        }
        Entity entity = level.getEntity(this.lichId);
        if (entity instanceof IceLich lich && lich.isAlive()) {
            this.missingSeconds = 0;
            return lich;
        }
        if (++this.missingSeconds >= MISSING_LIMIT) {
            this.lichId = null;
            this.missingSeconds = 0;
            this.changed();
        }
        return null;
    }

    /**
     * A bound lich checking in. It's this phylactery's lich if it's the one on record, or if the
     * phylactery has lost track of its lich and this body turns up; any other body is a stale copy.
     */
    public boolean claim(IceLich lich) {
        if (lich.getUUID().equals(this.lichId)) {
            return true;
        }
        if (this.lichId == null && this.reformTicks < 0) {
            this.lichId = lich.getUUID();
            this.soulTicks = 0;
            this.missingSeconds = 0;
            this.changed();
            return true;
        }
        return false;
    }

    /** A player has walked into the crypt: light the braziers and call the lich home. */
    private void awaken(ServerLevel level, Player intruder, @Nullable IceLich lich, List<BlockPos> braziers) {
        this.awake = true;
        this.changed();
        this.queueIgnition(level, braziers, intruder.blockPosition());
        level.playSound(null, this.worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, this.worldPosition, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
        if (lich != null) {
            lich.recall(this.altarSpot(), intruder);
        } else if (this.dormant()) {
            this.scheduleRise(intruder);
        } else if (this.reformTicks >= 0) {
            this.reformTicks = Math.min(this.reformTicks, RISE_TICKS);
        }
        // Otherwise its soul is still on the way home, or its body is out of reach: it comes when it can.
    }

    private void scheduleRise(Player prey) {
        this.reformHunting = prey.getUUID();
        this.reformTicks = RISE_TICKS;
        this.changed();
    }

    /** Queues the unlit braziers for lighting, nearest {@code from} first. */
    private void queueIgnition(ServerLevel level, List<BlockPos> braziers, BlockPos from) {
        this.toIgnite.clear();
        braziers.stream()
                .filter(p -> !level.getBlockState(p).getValue(BlockStateProperties.LIT))
                .sorted(Comparator.comparingDouble(p -> p.distSqr(from)))
                .forEach(this.toIgnite::add);
    }

    private void igniteNext(ServerLevel level) {
        BlockPos brazier = this.toIgnite.poll();
        if (brazier != null && setLit(level, brazier, true)) {
            level.playSound(null, brazier, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.8F, 0.6F + level.random.nextFloat() * 0.2F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, brazier.getX() + 0.5, brazier.getY() + 0.6, brazier.getZ() + 0.5, 12, 0.2, 0.3, 0.2, 0.02);
        }
    }

    /** Nobody's left in the crypt: it goes dark again, ready to wake for the next intruder. */
    private void sleep(ServerLevel level, List<BlockPos> braziers) {
        this.awake = false;
        this.emptySeconds = 0;
        this.toIgnite.clear();
        this.changed();
        for (BlockPos brazier : braziers) {
            setLit(level, brazier, false);
        }
    }

    /**
     * The braziers burn in step with the lich's health, those nearest the altar lasting longest: they
     * gutter out as it weakens and flare back up when it re-forms whole.
     */
    private void burnDown(ServerLevel level, List<BlockPos> braziers, float health) {
        braziers.sort(Comparator.comparingDouble(p -> p.distSqr(this.worldPosition)));
        int burning = Math.max(Math.min(MIN_FLAMES, braziers.size()), Mth.ceil(braziers.size() * health));
        for (int i = 0; i < braziers.size(); ++i) {
            BlockPos brazier = braziers.get(i);
            if (setLit(level, brazier, i < burning)) {
                if (i < burning) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, brazier.getX() + 0.5, brazier.getY() + 0.6, brazier.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.02);
                } else {
                    level.playSound(null, brazier, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 0.8F);
                    level.sendParticles(ParticleTypes.LARGE_SMOKE, brazier.getX() + 0.5, brazier.getY() + 0.6, brazier.getZ() + 0.5, 6, 0.15, 0.2, 0.15, 0.01);
                }
            }
        }
    }

    /** The crypt's flames: soul-fire braziers and candles (the "braziers" throughout). */
    public static boolean isFlame(BlockState state) {
        return (state.is(Blocks.SOUL_CAMPFIRE) || state.is(BlockTags.CANDLES)) && state.hasProperty(BlockStateProperties.LIT);
    }

    /** Lights or snuffs a loaded brazier or candle; true if it changed. */
    private static boolean setLit(ServerLevel level, BlockPos pos, boolean lit) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!isFlame(state) || state.getValue(BlockStateProperties.LIT) == lit) {
            return false;
        }
        level.setBlock(pos, state.setValue(BlockStateProperties.LIT, lit), 3);
        return true;
    }

    /** Very rarely, rise behind a player roaming the Frosted Caves nearby. */
    private void tryAmbush(ServerLevel level, List<Player> near, AABB crypt) {
        if (this.ambushCooldown > 0) {
            --this.ambushCooldown;
            this.changed();
            return;
        }
        if (!canRaise(level)) {
            return;
        }
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        Vec3 center = Vec3.atCenterOf(this.worldPosition);
        for (Player player : near) {
            if (player.distanceToSqr(center) < AMBUSH_RANGE * AMBUSH_RANGE && !crypt.contains(player.position())
                    && level.getBiome(player.blockPosition()).is(ZombieFreezing.FREEZES_ZOMBIES) && level.random.nextDouble() < chance) {
                Vec3 spot = findAmbushSpot(level, player);
                if (spot != null) {
                    this.raise(level, spot, player);
                    return;
                }
            }
        }
    }

    /** An open spot 10-16 blocks behind the player, where they won't see the lich rise. */
    @Nullable
    private static Vec3 findAmbushSpot(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 24; ++attempt) {
            float yaw = player.getYRot() + 180.0F + (level.random.nextFloat() - 0.5F) * 140.0F;
            double distance = 10.0 + level.random.nextDouble() * 6.0;
            double x = player.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * distance;
            double z = player.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * distance;
            for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
                BlockPos pos = BlockPos.containing(x, player.getY() + dy, z);
                if (ColdEffects.isOpen(level, pos, 3)) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
        }
        return null;
    }

    /** Where the lich re-forms: just above the phylactery. */
    public Vec3 altarSpot() {
        return Vec3.atBottomCenterOf(this.worldPosition.above());
    }

    /** Raises this phylactery's lich at {@code at}, hunting {@code prey} if given. Nothing rises on Peaceful. */
    @Nullable
    public IceLich raise(ServerLevel level, Vec3 at, @Nullable Player prey) {
        IceLich lich = canRaise(level) ? IceLich.summon(level, at, this.worldPosition) : null;
        if (lich != null) {
            if (prey != null) {
                lich.hunt(prey);
            }
            this.lichId = lich.getUUID();
            this.missingSeconds = 0;
            this.soulTicks = 0;
            this.changed();
        }
        return lich;
    }

    private void reform(ServerLevel level) {
        Player prey = this.reformHunting != null && level.getEntity(this.reformHunting) instanceof Player player ? player : null;
        this.reformHunting = null;
        if (prey != null && (!isPrey(prey) || prey.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) > LEASH * LEASH)) {
            prey = null;
        }
        if (prey == null) {
            prey = level.getEntitiesOfClass(Player.class, this.cryptBounds(), PhylacteryBlockEntity::isPrey).stream().findFirst().orElse(null);
        }
        if (this.lichId == null) {
            this.raise(level, this.altarSpot(), prey);
        }
    }

    /** The lich's soul has flown home; it re-forms after a while. */
    public void onWispArrived(@Nullable UUID hunting) {
        this.soulTicks = 0;
        this.changed();
        if (this.lichId != null || this.reformTicks >= 0) {
            return;
        }
        this.reformHunting = hunting;
        this.reformTicks = this.awake ? REFORM_TICKS_AWAKE : REFORM_TICKS;
    }

    /** The lich was struck down; its soul is on the way home. */
    public void onLichDiscorporated(IceLich lich) {
        if (lich.getUUID().equals(this.lichId)) {
            this.lichId = null;
            this.soulTicks = LichWisp.MAX_AGE;
            this.changed();
        }
    }

    /** The lich gave up its hunt and sank back into the phylactery. */
    public void onLichRetreated(IceLich lich) {
        if (lich.getUUID().equals(this.lichId)) {
            this.lichId = null;
            this.ambushCooldown = RETREAT_COOLDOWN;
            this.changed();
        }
    }

    /**
     * The phylactery is broken: its lich is mortal. If the lich has no body right now (its soul is in
     * flight, or waiting to re-form), it takes one last form here, unbound. A body that's out of reach
     * finds out it's mortal when it next checks on its phylactery.
     */
    void shatter(ServerLevel level) {
        Vec3 at = Vec3.atCenterOf(this.worldPosition);
        level.sendParticles(ColdEffects.ICE_CHIPS, at.x, at.y, at.z, 60, 0.3, 0.4, 0.3, 0.2);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 30, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, this.worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.playSound(null, this.worldPosition, SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1.5F, 0.5F);
        ColdEffects.tellNearby(level, new AABB(this.worldPosition).inflate(48.0), Component.translatable("message.wildspellmobs.phylactery_shattered"));
        IceLich lich = this.findLich(level);
        if (lich != null) {
            lich.loseAnchor();
        } else if (this.lichId == null && canRaise(level)) {
            Player prey = level.getNearestPlayer(at.x, at.y, at.z, 48.0, p -> p instanceof Player player && isPrey(player));
            IceLich last = IceLich.summon(level, this.altarSpot(), null);
            if (last != null && prey != null) {
                last.setTarget(prey);
            }
        }
        this.lichId = null;
        this.reformTicks = -1;
    }

    /** Client: soul-light streams from each standing ward into the phylactery while someone's near. */
    static void clientTick(Level level, BlockPos pos, BlockState state, PhylacteryBlockEntity phylactery) {
        long time = level.getGameTime();
        if (!state.getValue(PhylacteryBlock.WARDED) || time % BEAM_INTERVAL != 0
                || level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, BEAM_VIEW_RANGE, false) == null) {
            return;
        }
        if (time % WARD_RESCAN_INTERVAL == 0 || phylactery.wardCache.isEmpty()) {
            phylactery.wardCache = phylactery.cryptLoaded(level) ? phylactery.findWards(level) : List.of();
        }
        Vec3 to = Vec3.atCenterOf(pos);
        for (BlockPos ward : phylactery.wardCache) {
            Vec3 from = Vec3.atCenterOf(ward);
            Vec3 step = to.subtract(from);
            int points = Mth.ceil(step.length() / 0.8);
            for (int i = 0; i <= points; ++i) {
                Vec3 p = from.add(step.scale((i + level.random.nextDouble() * 0.5) / points));
                level.addParticle(WildspellMobs.FROST_MOTE.get(), p.x, p.y, p.z, 0.0, 0.0, 0.0);
            }
        }
    }

    private List<BlockPos> findWards(Level level) {
        List<BlockPos> wards = new ArrayList<>();
        for (BlockPos p : this.cryptBlocks()) {
            if (level.getBlockState(p).is(WildspellMobs.RIME_WARD.get())) {
                wards.add(p.immutable());
            }
        }
        return wards;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.lichId != null) {
            tag.putUUID("Lich", this.lichId);
        }
        if (this.reformHunting != null) {
            tag.putUUID("ReformHunting", this.reformHunting);
        }
        tag.putInt("ReformTicks", this.reformTicks);
        tag.putInt("SoulTicks", this.soulTicks);
        tag.putInt("AmbushCooldown", this.ambushCooldown);
        tag.putBoolean("Awake", this.awake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lichId = tag.hasUUID("Lich") ? tag.getUUID("Lich") : null;
        this.reformHunting = tag.hasUUID("ReformHunting") ? tag.getUUID("ReformHunting") : null;
        this.reformTicks = tag.contains("ReformTicks", CompoundTag.TAG_INT) ? tag.getInt("ReformTicks") : -1;
        this.soulTicks = tag.getInt("SoulTicks");
        this.ambushCooldown = tag.getInt("AmbushCooldown");
        this.awake = tag.getBoolean("Awake");
        this.resumeIgnite = this.awake;
    }
}
