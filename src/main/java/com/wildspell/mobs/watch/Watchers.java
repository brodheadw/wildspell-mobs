package com.wildspell.mobs.watch;

import com.wildspell.mobs.MobsConfig;
import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.Watcher;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class Watchers {
    public static final String KNOWING = "wildspell_knowing";
    public static final String UNSEEN_UNTIL = "wildspell_unseen_until";
    public static final String RESTING_UNTIL = WildspellMobs.MODID + ":watched_until";
    public static final String FELLED = WildspellMobs.MODID + ":watchers_felled";
    public static final float FORGETTING = 0.2F;
    public static final double NEAREST = 10.0;
    public static final double FARTHEST = 16.0;
    public static final double SEARCH = 96.0;

    private Watchers() {
    }

    public static float knowing(Player player) {
        return Mth.clamp(player.getPersistentData().getFloat(KNOWING), 0.0F, 1.0F);
    }

    public static boolean unseen(Player player) {
        return player.level().getGameTime() < player.getPersistentData().getLong(UNSEEN_UNTIL);
    }

    public static boolean noticeable(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && !unseen(player) && knowing(player) > 0.0F;
    }

    public static boolean stillKnows(Player player, float drawnBy) {
        return player.isAlive() && !player.isSpectator() && !unseen(player) && knowing(player) >= drawnBy - FORGETTING;
    }

    public static List<Face> haunting(ServerLevel level, Player player) {
        List<Face> faces = new ArrayList<>();
        for (Face face : Face.values()) {
            if (face.haunts(level, player)) {
                faces.add(face);
            }
        }
        return faces;
    }

    public static long restingUntil(Player player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(RESTING_UNTIL);
    }

    private static void rest(Player player, float knowing) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        long seconds = (long) (MobsConfig.WATCHER_REST_SECONDS.get() * (1.5F - knowing));
        persisted.putLong(RESTING_UNTIL, player.level().getGameTime() + seconds * 20L);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void felled(Player player, Face face) {
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        ListTag felled = persisted.getList(FELLED, Tag.TAG_STRING);
        if (!felled.contains(StringTag.valueOf(face.id))) {
            felled.add(StringTag.valueOf(face.id));
        }
        persisted.put(FELLED, felled);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().overworld().getGameTime() % 20 != 13) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            consider(player.serverLevel(), player);
        }
    }

    static void consider(ServerLevel level, ServerPlayer player) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !noticeable(player) || level.getGameTime() < restingUntil(player)) {
            return;
        }
        float knowing = knowing(player);
        if (level.random.nextDouble() >= MobsConfig.WATCHER_CHANCE.get() * knowing * knowing || watching(level, player) != null) {
            return;
        }
        List<Face> faces = haunting(level, player);
        if (!faces.isEmpty()) {
            arrive(level, player, faces.get(level.random.nextInt(faces.size())));
        }
    }

    @Nullable
    public static Watcher watching(ServerLevel level, Player player) {
        List<Watcher> found = level.getEntitiesOfClass(Watcher.class, player.getBoundingBox().inflate(SEARCH),
                watcher -> player.getUUID().equals(watcher.quarryId()));
        return found.isEmpty() ? null : found.get(0);
    }

    @Nullable
    public static Watcher arrive(ServerLevel level, Player player, Face face) {
        Vec3 spot = vantage(level, player);
        if (spot == null) {
            return null;
        }
        Watcher watcher = WildspellMobs.WATCHERS.get(face).get().create(level);
        if (watcher == null) {
            return null;
        }
        watcher.moveTo(spot.x, spot.y, spot.z, level.random.nextFloat() * 360.0F, 0.0F);
        watcher.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(spot)), MobSpawnType.EVENT, null);
        watcher.comeFor(player, knowing(player));
        if (!level.addFreshEntity(watcher)) {
            return null;
        }
        rest(player, knowing(player));
        watcher.playSound(SoundEvents.ENDER_EYE_DEATH, 2.0F, 0.5F);
        return watcher;
    }

    @Nullable
    public static Vec3 vantage(ServerLevel level, Player player) {
        for (int attempt = 0; attempt < 32; ++attempt) {
            float yaw = level.random.nextFloat() * 360.0F;
            double distance = NEAREST + level.random.nextDouble() * (FARTHEST - NEAREST);
            double x = player.getX() - Mth.sin(yaw * Mth.DEG_TO_RAD) * distance;
            double z = player.getZ() + Mth.cos(yaw * Mth.DEG_TO_RAD) * distance;
            for (int dy : new int[] {1, 0, 2, -1, 3, -2}) {
                BlockPos pos = BlockPos.containing(x, player.getY() + dy, z);
                Vec3 eye = Vec3.atBottomCenterOf(pos).add(0.0, Watcher.EYE, 0.0);
                if (ColdEffects.isOpen(level, pos, 3) && ColdEffects.clearPath(player, eye, player.getEyePosition())) {
                    return Vec3.atBottomCenterOf(pos);
                }
            }
        }
        return null;
    }
}
