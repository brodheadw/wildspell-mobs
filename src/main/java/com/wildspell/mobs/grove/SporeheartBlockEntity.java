package com.wildspell.mobs.grove;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.Stemwalker;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SporeheartBlockEntity extends BlockEntity {
    public static final int MAX_BOUND = 2;
    public static final double SENSE = 32.0;
    public static final int SUMMON_COOLDOWN = 200;
    public static final int NEAREST = 5;
    public static final int FARTHEST = 12;

    private final List<UUID> bound = new ArrayList<>();
    private int cooldown;

    public SporeheartBlockEntity(BlockPos pos, BlockState state) {
        super(WildspellMobs.SPOREHEART_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SporeheartBlockEntity heart) {
        if (heart.cooldown > 0) {
            --heart.cooldown;
        }
        if (level.getGameTime() % 20L == 0L && level instanceof ServerLevel server) {
            heart.pulse(server, state);
        }
    }

    public boolean alive() {
        return this.getBlockState().getValue(SporeheartBlock.ACTIVE);
    }

    public void pulse(ServerLevel level, BlockState state) {
        boolean active = SporeheartBlock.night(level) && SporeheartBlock.enshrined(level, this.worldPosition);
        if (active != state.getValue(SporeheartBlock.ACTIVE)) {
            level.setBlock(this.worldPosition, state.setValue(SporeheartBlock.ACTIVE, active), 3);
        }
        this.bound.removeIf(id -> !(level.getEntity(id) instanceof Stemwalker walker) || !walker.isAlive());
        if (!active || this.cooldown > 0 || this.bound.size() >= MAX_BOUND) {
            return;
        }
        Player near = level.getNearestPlayer(this.worldPosition.getX() + 0.5, this.worldPosition.getY(), this.worldPosition.getZ() + 0.5, SENSE,
                entity -> entity instanceof Player player && Stemwalker.hunts(player));
        if (near != null) {
            this.summon(level, near);
        }
    }

    @Nullable
    public Stemwalker summon(ServerLevel level, Player quarry) {
        BlockPos spot = this.findSoil(level, quarry);
        if (spot == null) {
            return null;
        }
        Stemwalker walker = WildspellMobs.STEMWALKER.get().create(level);
        if (walker == null) {
            return null;
        }
        float yaw = (float) (Mth.atan2(quarry.getZ() - spot.getZ(), quarry.getX() - spot.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        walker.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, yaw, 0.0F);
        walker.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.SPAWNER, null);
        walker.bindTo(this.worldPosition);
        walker.emerge();
        level.addFreshEntity(walker);
        this.bound.add(walker.getUUID());
        this.cooldown = SUMMON_COOLDOWN;
        this.setChanged();
        level.playSound(null, this.worldPosition, SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.5F, 0.5F);
        Stemwalker.trail(level, Vec3.atCenterOf(this.worldPosition), walker.position());
        return walker;
    }

    @Nullable
    private BlockPos findSoil(ServerLevel level, Player quarry) {
        BlockPos fallback = null;
        for (int attempt = 0; attempt < 24; ++attempt) {
            BlockPos column = BlockPos.containing(ColdEffects.ringPoint(level.random, Vec3.atBottomCenterOf(this.worldPosition), NEAREST, FARTHEST - NEAREST, 0.0));
            for (int dy = 4; dy >= -6; --dy) {
                BlockPos ground = column.above(dy);
                BlockPos stand = ground.above();
                if (!SporeheartFeature.soil(level.getBlockState(ground))
                        || !level.noCollision(WildspellMobs.STEMWALKER.get().getSpawnAABB(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5))) {
                    continue;
                }
                Vec3 toward = Vec3.atBottomCenterOf(stand).add(0.0, 1.5, 0.0).subtract(quarry.getEyePosition()).normalize();
                if (quarry.getViewVector(1.0F).dot(toward) < 0.5) {
                    return stand;
                }
                if (fallback == null) {
                    fallback = stand;
                }
                break;
            }
        }
        return fallback;
    }

    public void die(ServerLevel level) {
        for (UUID id : this.bound) {
            if (level.getEntity(id) instanceof Stemwalker walker && walker.isAlive()) {
                walker.crumble();
            }
        }
        for (Stemwalker walker : level.getEntitiesOfClass(Stemwalker.class, new AABB(this.worldPosition).inflate(Stemwalker.TETHER),
                w -> w.isAlive() && this.worldPosition.equals(w.getHeart()))) {
            walker.crumble();
        }
        this.bound.clear();
        Vec3 at = Vec3.atCenterOf(this.worldPosition);
        level.sendParticles(ParticleTypes.CRIMSON_SPORE, at.x, at.y, at.z, 60, 0.6, 0.8, 0.6, 0.02);
        level.playSound(null, this.worldPosition, SoundEvents.WART_BLOCK_BREAK, SoundSource.BLOCKS, 1.5F, 0.4F);
    }

    public List<UUID> bound() {
        return this.bound;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        this.bound.forEach(id -> list.add(NbtUtils.createUUID(id)));
        tag.put("Bound", list);
        tag.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.bound.clear();
        for (Tag id : tag.getList("Bound", Tag.TAG_INT_ARRAY)) {
            this.bound.add(NbtUtils.loadUUID(id));
        }
        this.cooldown = tag.getInt("Cooldown");
    }
}
