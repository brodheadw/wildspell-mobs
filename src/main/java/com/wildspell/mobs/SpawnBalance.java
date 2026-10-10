package com.wildspell.mobs;

import com.wildspell.mobs.entity.RimeSkull;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

public final class SpawnBalance {
    private static final Long2ObjectOpenHashMap<int[]> NEARBY = new Long2ObjectOpenHashMap<>();
    private static long nearbyTick = Long.MIN_VALUE;

    private SpawnBalance() {
    }

    static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL) {
            return;
        }
        Mob mob = event.getEntity();
        if (!(mob instanceof Enemy) || mob.level().dimension() != Level.OVERWORLD || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = mob.blockPosition();
        if (event.getLevel().getBrightness(LightLayer.SKY, pos) > 0) {
            return;
        }
        boolean creeper = mob instanceof Creeper;
        if (!(mob instanceof RimeSkull)) {
            double chance = creeper ? MobsConfig.UNDERGROUND_CREEPER_CHANCE.get() : MobsConfig.UNDERGROUND_MONSTER_CHANCE.get();
            if (mob.getRandom().nextDouble() >= chance) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
        int localCap = MobsConfig.UNDERGROUND_LOCAL_CAP.get();
        int creeperCap = MobsConfig.UNDERGROUND_CREEPER_CAP.get();
        if (localCap == 0 && (creeperCap == 0 || !creeper)) {
            return;
        }
        int[] nearby = nearbyHostiles(level, pos);
        if (localCap > 0 && nearby[0] >= localCap || creeper && creeperCap > 0 && nearby[1] >= creeperCap) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    private static int[] nearbyHostiles(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if (now != nearbyTick) {
            NEARBY.clear();
            nearbyTick = now;
        }
        return NEARBY.computeIfAbsent(SectionPos.asLong(pos), key -> {
            int[] counts = new int[2];
            AABB area = new AABB(SectionPos.of(pos).center()).inflate(MobsConfig.UNDERGROUND_CAP_RADIUS.get());
            for (Mob other : level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && m.isAlive() && !m.isPersistenceRequired() && !m.requiresCustomPersistence())) {
                ++counts[0];
                if (other instanceof Creeper) {
                    ++counts[1];
                }
            }
            return counts;
        });
    }

    static void clearCache() {
        NEARBY.clear();
    }
}
