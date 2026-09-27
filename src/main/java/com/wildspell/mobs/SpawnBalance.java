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
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Thins natural hostile spawns in overworld caves. "Underground" means the spawn spot gets no
 * skylight at all, i.e. a cave.
 *
 * <p>The per-spawn chances only change which mobs fill the vanilla monster cap, not how many there
 * are, so the local caps do the real thinning: a cave spot that already has enough hostiles (or
 * creepers) nearby refuses further spawns, pushing the cap's mobs out away from the player.
 */
public final class SpawnBalance {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static final ModConfigSpec.DoubleValue UNDERGROUND_CREEPER_CHANCE = BUILDER
            .comment("Chance (0-1) that a natural creeper spawn is allowed underground in the overworld. 1.0 = vanilla.")
            .defineInRange("undergroundCreeperChance", 0.3, 0.0, 1.0);

    static final ModConfigSpec.DoubleValue UNDERGROUND_MONSTER_CHANCE = BUILDER
            .comment("Chance (0-1) that any other natural hostile spawn is allowed underground in the overworld.",
                    "Rime Skulls are exempt. 1.0 = vanilla.")
            .defineInRange("undergroundMonsterChance", 0.75, 0.0, 1.0);

    static final ModConfigSpec.IntValue UNDERGROUND_LOCAL_CAP = BUILDER
            .comment("Underground spawns are refused when this many hostile mobs (Rime Skulls included) are already",
                    "within 'undergroundCapRadius' blocks of the spot. 0 = no cap.")
            .defineInRange("undergroundLocalCap", 8, 0, 256);

    static final ModConfigSpec.IntValue UNDERGROUND_CREEPER_CAP = BUILDER
            .comment("Underground creeper spawns are refused when this many creepers are already within",
                    "'undergroundCapRadius' blocks of the spot. 0 = no cap.")
            .defineInRange("undergroundCreeperCap", 2, 0, 256);

    static final ModConfigSpec.IntValue UNDERGROUND_CAP_RADIUS = BUILDER
            .comment("Radius in blocks used by the two underground caps.")
            .defineInRange("undergroundCapRadius", 32, 8, 128);

    public static final ModConfigSpec.DoubleValue LICH_AMBUSH_CHANCE = BUILDER
            .comment("Chance (0-1), each second, that an Ice Lich rises behind a player in the Frosted Caves within 64",
                    "blocks of its crypt. The default averages about 4 minutes spent near a crypt; while it waits, the",
                    "caves give signs (a whisper behind you, a drift of ice motes). 0 = never.")
            .defineInRange("lichAmbushChance", 0.004, 0.0, 1.0);

    public static final ModConfigSpec.IntValue LICH_AMBUSH_RANGE = BUILDER
            .comment("The Ice Lich's hunting ground: how far (blocks) from its phylactery it can sense a player in the Frosted",
                    "Caves and rise behind them, and how far it will chase before it goes home. The crypt need not be loaded,",
                    "and only the nearest crypt to a player stalks them.")
            .defineInRange("lichAmbushRange", 200, 16, 4096);

    static final ModConfigSpec SPEC = BUILDER.build();

    // Spawning probes thousands of spots per tick; count neighbours once per chunk section per tick.
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
            double chance = creeper ? UNDERGROUND_CREEPER_CHANCE.get() : UNDERGROUND_MONSTER_CHANCE.get();
            if (mob.getRandom().nextDouble() >= chance) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                return;
            }
        }
        int localCap = UNDERGROUND_LOCAL_CAP.get();
        int creeperCap = UNDERGROUND_CREEPER_CAP.get();
        if (localCap == 0 && (creeperCap == 0 || !creeper)) {
            return;
        }
        int[] nearby = nearbyHostiles(level, pos);
        if (localCap > 0 && nearby[0] >= localCap || creeper && creeperCap > 0 && nearby[1] >= creeperCap) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    /** {hostiles, creepers} within the cap radius of the section containing {@code pos}. */
    private static int[] nearbyHostiles(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        if (now != nearbyTick) {
            NEARBY.clear();
            nearbyTick = now;
        }
        return NEARBY.computeIfAbsent(SectionPos.asLong(pos), key -> {
            int[] counts = new int[2];
            AABB area = new AABB(SectionPos.of(pos).center()).inflate(UNDERGROUND_CAP_RADIUS.get());
            for (Mob other : level.getEntitiesOfClass(Mob.class, area, m -> m instanceof Enemy && m.isAlive())) {
                ++counts[0];
                if (other instanceof Creeper) {
                    ++counts[1];
                }
            }
            return counts;
        });
    }

    /** Drops cached neighbour counts; gametests call this after spawning mobs mid-tick. */
    static void clearCache() {
        NEARBY.clear();
    }
}
