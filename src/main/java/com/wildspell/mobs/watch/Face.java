package com.wildspell.mobs.watch;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;

public enum Face {
    ATHOTH("athoth"),
    ELOAIOU("eloaiou"),
    ASTAPHAIOS("astaphaios"),
    YAO("yao"),
    SABAOTH("sabaoth"),
    ADONIN("adonin"),
    SABBATAIOS("sabbataios");

    public static final int LAVA_REACH = 6;
    public static final int GLARE_LIGHT = 12;

    public final String id;
    public final TagKey<Biome> haunts;

    Face(String id) {
        this.id = id;
        this.haunts = TagKey.create(Registries.BIOME, WildspellMobs.id("watcher_haunts/" + id));
    }

    public boolean haunts(ServerLevel level, Player player) {
        BlockPos at = player.blockPosition();
        boolean overworld = level.dimension() == Level.OVERWORLD;
        boolean sky = level.canSeeSky(BlockPos.containing(player.getEyePosition()));
        boolean place = level.getBiome(at).is(this.haunts);
        long time = Math.floorMod(level.getDayTime(), 24000L);
        return switch (this) {
            case ATHOTH -> overworld && sky && place && (time >= 22800L || time < 1200L || (time >= 11800L && time <= 13400L));
            case ELOAIOU -> overworld && !sky && (player.getY() < 0.0 || place);
            case ASTAPHAIOS, YAO -> place;
            case SABAOTH -> place && sky && level.isThundering();
            case ADONIN -> place && (sky ? noon(time) && !level.isRaining() : noon(time) || level.getBrightness(LightLayer.BLOCK, at) >= GLARE_LIGHT);
            case SABBATAIOS -> place || nearLava(level, at);
        };
    }

    private static boolean noon(long time) {
        return time >= 4500L && time <= 7500L;
    }

    private static boolean nearLava(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-LAVA_REACH, -3, -LAVA_REACH), at.offset(LAVA_REACH, 3, LAVA_REACH))) {
            if (level.getFluidState(p).is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }
}
