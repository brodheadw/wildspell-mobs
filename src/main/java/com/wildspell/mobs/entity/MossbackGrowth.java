package com.wildspell.mobs.entity;

/**
 * Mossbacks measure age using elapsed world game time, not entity tick count.
 * Thus a saved Mossback continues maturing while its chunk is unloaded, as long
 * as the world is running. A "year" is intentionally 365 Minecraft days.
 */
public final class MossbackGrowth {
    private MossbackGrowth() {}

    public static final long DAY_TICKS = 24000L;
    public static final long YEAR_DAYS = 365L;
    public static final long HATCHLING_DAYS = 30L;
    public static final long JUVENILE_DAYS = 180L;
    public static final long ANCIENT_DAYS = 2L * YEAR_DAYS; // 730 Minecraft days
    public static final long FULL_SIZE_DAYS = 3L * YEAR_DAYS; // 1095 Minecraft days

    public static final int HATCHLING = 0;
    public static final int JUVENILE = 1;
    public static final int MATURE = 2;
    public static final int ANCIENT = 3;

    public static long elapsed(long birthGameTick, long currentGameTick) {
        return Math.max(0L, currentGameTick - birthGameTick);
    }

    public static long days(long ageTicks) {
        return Math.max(0L, ageTicks) / DAY_TICKS;
    }

    public static int stage(long ageTicks) {
        long d = days(ageTicks);
        if (d < HATCHLING_DAYS) return HATCHLING;
        if (d < JUVENILE_DAYS) return JUVENILE;
        if (d < ANCIENT_DAYS) return MATURE;
        return ANCIENT;
    }

    public static float scale(long ageTicks) {
        double d = Math.max(0.0, ageTicks / (double) DAY_TICKS);
        if (d < HATCHLING_DAYS) return interpolate(d, 0, HATCHLING_DAYS, 0.13F, 0.20F);
        if (d < JUVENILE_DAYS) return interpolate(d, HATCHLING_DAYS, JUVENILE_DAYS, 0.20F, 0.40F);
        if (d < ANCIENT_DAYS) return interpolate(d, JUVENILE_DAYS, ANCIENT_DAYS, 0.40F, 1.10F);
        if (d < FULL_SIZE_DAYS) return interpolate(d, ANCIENT_DAYS, FULL_SIZE_DAYS, 1.10F, 2.46F);
        return 2.46F;
    }

    private static float interpolate(double day, double start, double end, float min, float max) {
        return (float) (min + (max - min) * ((day - start) / (end - start)));
    }
}
