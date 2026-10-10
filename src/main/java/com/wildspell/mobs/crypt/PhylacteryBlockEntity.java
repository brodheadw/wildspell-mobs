package com.wildspell.mobs.crypt;

import com.wildspell.mobs.MobsConfig;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.IceLich;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PhylacteryBlockEntity extends BlockEntity {
    public static final int CRYPT_CENTER = 9;
    public static final int CRYPT_HALF = 11;
    public static final int CRYPT_BELOW = 3;
    public static final int CRYPT_ABOVE = 6;
    public static final double NEAR = 64.0;

    public static double ambushRange() {
        return MobsConfig.LICH_AMBUSH_RANGE.get();
    }

    public static double leash() {
        return ambushRange();
    }
    public static final int REFORM_TICKS = 400;
    public static final int REFORM_TICKS_AWAKE = 100;
    public static final int RISE_TICKS = 40;
    public static final int RETREAT_COOLDOWN = 300;
    private static final int EMPTY_LIMIT = 30;
    private static final int MIN_FLAMES = 4;
    private static final int BEAM_INTERVAL = 4;
    private static final int WARD_RESCAN_INTERVAL = 40;
    private static final double BEAM_VIEW_RANGE = 32.0;

    @Nullable
    private UUID soulId;
    @Nullable
    private UUID legacyLich;
    private boolean awake;
    private int emptySeconds;
    private final Deque<BlockPos> toIgnite = new ArrayDeque<>();
    private boolean resumeIgnite;
    private boolean dirty;
    private List<BlockPos> wardCache = List.of();

    public PhylacteryBlockEntity(BlockPos pos, BlockState state) {
        super(WildspellMobs.PHYLACTERY.get(), pos, state);
    }

    public static boolean isPrey(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.getAbilities().instabuild;
    }

    public AABB cryptBounds() {
        return cryptBounds(this.worldPosition, this.getBlockState().getValue(PhylacteryBlock.FACING));
    }

    public static AABB cryptBounds(BlockPos altar, Direction facing) {
        BlockPos center = altar.relative(facing, CRYPT_CENTER);
        return new AABB(center.getX() - CRYPT_HALF, altar.getY() - CRYPT_BELOW, center.getZ() - CRYPT_HALF,
                center.getX() + CRYPT_HALF + 1, altar.getY() + CRYPT_ABOVE, center.getZ() + CRYPT_HALF + 1);
    }

    public static Iterable<BlockPos> cryptBlocks(BlockPos altar, Direction facing) {
        AABB crypt = cryptBounds(altar, facing);
        return BlockPos.betweenClosed(cryptMin(crypt), cryptMax(crypt));
    }

    private static BlockPos cryptMin(AABB crypt) {
        return BlockPos.containing(crypt.minX, crypt.minY, crypt.minZ);
    }

    private static BlockPos cryptMax(AABB crypt) {
        return BlockPos.containing(crypt.maxX - 1, crypt.maxY - 1, crypt.maxZ - 1);
    }

    private Iterable<BlockPos> cryptBlocks() {
        return cryptBlocks(this.worldPosition, this.getBlockState().getValue(PhylacteryBlock.FACING));
    }

    private boolean cryptLoaded(Level level) {
        AABB crypt = this.cryptBounds();
        return level.hasChunksAt(cryptMin(crypt), cryptMax(crypt));
    }

    public boolean isAwake() {
        return this.awake;
    }

    public LichSouls.Soul soul(ServerLevel level) {
        LichSouls souls = LichSouls.get(level);
        LichSouls.Soul soul = souls.soul(this.soulId);
        if (soul == null) {
            soul = souls.create(level, this.worldPosition);
            this.soulId = soul.id;
            if (this.legacyLich != null && level.getEntity(this.legacyLich) instanceof IceLich lich) {
                soul.claim(lich);
            }
            this.changed();
        }
        return soul;
    }

    @Nullable
    public UUID soulId() {
        return this.soulId;
    }

    void bindSoul(ServerLevel level, UUID soulId) {
        LichSouls.Soul soul = LichSouls.get(level).soul(soulId);
        if (soul != null) {
            this.soulId = soulId;
            soul.placeOnAltar(level, this.worldPosition);
            this.changed();
        }
    }

    @Nullable
    public UUID lichId() {
        return this.soulId == null || !(this.level instanceof ServerLevel level) ? null : this.soul(level).lichId();
    }

    private void changed() {
        this.dirty = true;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, PhylacteryBlockEntity phylactery) {
        ServerLevel server = (ServerLevel) level;
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
        LichSouls.Soul soul = this.soul(level);
        IceLich lich = soul.findLich(level);
        List<Player> near = level.getEntitiesOfClass(Player.class, new AABB(this.worldPosition).inflate(NEAR), PhylacteryBlockEntity::isPrey);
        if (near.isEmpty() && !this.awake) {
            return;
        }
        AABB crypt = this.cryptBounds();
        if (this.cryptLoaded(level)) {
            this.tendCrypt(level, soul, lich, near, crypt);
        }
    }

    private void tendCrypt(ServerLevel level, LichSouls.Soul soul, @Nullable IceLich lich, List<Player> near, AABB crypt) {
        boolean warded = false;
        List<BlockPos> braziers = new ArrayList<>();
        for (BlockPos p : this.cryptBlocks()) {
            BlockState state = level.getBlockState(p);
            if (state.is(WildspellMobs.RIME_WARD.get())) {
                warded = true;
            } else if (isFlame(state)) {
                braziers.add(p.immutable());
            }
        }
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
                this.awaken(level, soul, intruder, lich, braziers);
            } else if (soul.dormant()) {
                soul.scheduleRise(intruder, RISE_TICKS);
            }
        } else if (this.awake && ++this.emptySeconds >= EMPTY_LIMIT) {
            this.sleep(level, braziers);
        }
        if (this.awake && this.toIgnite.isEmpty() && lich != null && crypt.contains(lich.position())) {
            this.burnDown(level, braziers, lich.getHealth() / lich.getMaxHealth());
        }
    }

    private void awaken(ServerLevel level, LichSouls.Soul soul, Player intruder, @Nullable IceLich lich, List<BlockPos> braziers) {
        this.awake = true;
        this.changed();
        this.queueIgnition(level, braziers, intruder.blockPosition());
        level.playSound(null, this.worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, this.worldPosition, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.HOSTILE, 2.0F, 0.7F);
        if (lich != null) {
            lich.recall(this.altarSpot(), intruder);
        } else if (soul.dormant()) {
            soul.scheduleRise(intruder, RISE_TICKS);
        } else {
            soul.hurryRise(RISE_TICKS);
        }
    }

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

    private void sleep(ServerLevel level, List<BlockPos> braziers) {
        this.awake = false;
        this.emptySeconds = 0;
        this.toIgnite.clear();
        this.changed();
        for (BlockPos brazier : braziers) {
            setLit(level, brazier, false);
        }
    }

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

    public static boolean isFlame(BlockState state) {
        return (state.is(Blocks.SOUL_CAMPFIRE) || state.is(BlockTags.CANDLES)) && state.hasProperty(BlockStateProperties.LIT);
    }

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

    static void ambushFromAfar(ServerLevel level, List<LichSouls.Soul> souls) {
        ambushFromAfar(level, souls, level.players());
    }

    public static void ambushFromAfar(ServerLevel level, List<LichSouls.Soul> souls, List<? extends Player> candidates) {
        List<LichSouls.Soul> waiting = new ArrayList<>();
        for (LichSouls.Soul soul : souls) {
            if (soul.dimension() != level.dimension() || !soul.canStalk(level)) {
                continue;
            }
            if (level.shouldTickBlocksAt(soul.anchor()) && level.getBlockEntity(soul.anchor()) instanceof PhylacteryBlockEntity crypt && crypt.isAwake()) {
                continue;
            }
            waiting.add(soul);
        }
        if (waiting.isEmpty()) {
            return;
        }
        double range = ambushRange();
        Map<LichSouls.Soul, List<Player>> prey = new HashMap<>();
        for (Player player : candidates) {
            if (!isPrey(player) || !level.getBiome(player.blockPosition()).is(ColdEffects.COLD_CAVES)) {
                continue;
            }
            LichSouls.Soul nearest = null;
            double best = range * range;
            for (LichSouls.Soul soul : waiting) {
                double d2 = player.distanceToSqr(Vec3.atCenterOf(soul.anchor()));
                if (d2 < best && !cryptBounds(soul.crypt(), soul.cryptFacing()).contains(player.position())) {
                    best = d2;
                    nearest = soul;
                }
            }
            if (nearest != null) {
                prey.computeIfAbsent(nearest, k -> new ArrayList<>()).add(player);
            }
        }
        double chance = MobsConfig.LICH_AMBUSH_CHANCE.get();
        for (Map.Entry<LichSouls.Soul, List<Player>> entry : prey.entrySet()) {
            LichSouls.Soul soul = entry.getKey();
            if (soul.ambushCooldown() > 0) {
                soul.coolAmbush();
                continue;
            }
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                continue;
            }
            for (Player player : entry.getValue()) {
                if (level.random.nextDouble() < chance) {
                    Vec3 spot = findAmbushSpot(level, player);
                    if (spot != null) {
                        soul.raise(level, spot, player);
                        break;
                    }
                } else if (level.random.nextDouble() < PRESENCE_CHANCE) {
                    haunt(level, player);
                }
            }
        }
    }

    public static final double PRESENCE_CHANCE = 0.05;

    static void haunt(ServerLevel level, Player player) {
        float yaw = player.getYRot() + 180.0F + (level.random.nextFloat() - 0.5F) * 90.0F;
        double distance = 4.0 + level.random.nextDouble() * 4.0;
        double x = player.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * distance;
        double z = player.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * distance;
        double y = player.getEyeY();
        level.sendParticles(WildspellMobs.FROST_MOTE.get(), x, y, z, 10, 0.5, 0.6, 0.5, 0.01);
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.2F, 0.4F + level.random.nextFloat() * 0.2F);
    }

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

    public Vec3 altarSpot() {
        return Vec3.atBottomCenterOf(this.worldPosition.above());
    }

    void release(ServerLevel level) {
        LichSouls.Soul soul = this.soul(level);
        Vec3 at = Vec3.atCenterOf(this.worldPosition);
        soul.moved(level, this.worldPosition, null);
        ItemStack stack = PhylacteryItem.bound(soul.id);
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, stack);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y, at.z, 30, 0.3, 0.5, 0.3, 0.05);
        level.playSound(null, this.worldPosition, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 2.0F, 0.5F);
        level.playSound(null, this.worldPosition, SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.5F);
        ColdEffects.tellNearby(level, new AABB(this.worldPosition).inflate(48.0), Component.translatable("message.wildspellmobs.phylactery_taken"));
    }

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
        if (this.soulId != null) {
            tag.putUUID("Soul", this.soulId);
        }
        tag.putBoolean("Awake", this.awake);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.soulId = tag.hasUUID("Soul") ? tag.getUUID("Soul") : null;
        this.legacyLich = tag.hasUUID("Lich") ? tag.getUUID("Lich") : null;
        this.awake = tag.getBoolean("Awake");
        this.resumeIgnite = this.awake;
    }
}
