package com.wildspell.mobs;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MobsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue UNDERGROUND_CREEPER_CHANCE = BUILDER
            .comment("Chance (0-1) that a natural creeper spawn is allowed underground in the overworld. 1.0 = vanilla.")
            .defineInRange("undergroundCreeperChance", 0.3, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue UNDERGROUND_MONSTER_CHANCE = BUILDER
            .comment("Chance (0-1) that any other natural hostile spawn is allowed underground in the overworld.",
                    "Rime Skulls are exempt. 1.0 = vanilla.")
            .defineInRange("undergroundMonsterChance", 0.75, 0.0, 1.0);

    public static final ModConfigSpec.IntValue UNDERGROUND_LOCAL_CAP = BUILDER
            .comment("Underground spawns are refused when this many hostile mobs (Rime Skulls included; persistent ones like",
                    "flytrap heads and liches not) are already within 'undergroundCapRadius' blocks of the spot. 0 = no cap.")
            .defineInRange("undergroundLocalCap", 8, 0, 256);

    public static final ModConfigSpec.IntValue UNDERGROUND_CREEPER_CAP = BUILDER
            .comment("Underground creeper spawns are refused when this many creepers are already within",
                    "'undergroundCapRadius' blocks of the spot. 0 = no cap.")
            .defineInRange("undergroundCreeperCap", 2, 0, 256);

    public static final ModConfigSpec.IntValue UNDERGROUND_CAP_RADIUS = BUILDER
            .comment("Radius in blocks used by the two underground caps.")
            .defineInRange("undergroundCapRadius", 32, 8, 128);

    public static final ModConfigSpec.DoubleValue LICH_AMBUSH_CHANCE = BUILDER
            .comment("Chance (0-1), each second, that an Ice Lich rises behind a player in the Frosted Caves within",
                    "'lichAmbushRange' blocks of its crypt. The default averages about 4 minutes spent near a crypt; while it",
                    "waits, the caves give signs (a whisper behind you, a drift of ice motes). 0 = never.")
            .defineInRange("lichAmbushChance", 0.004, 0.0, 1.0);

    public static final ModConfigSpec.IntValue LICH_AMBUSH_RANGE = BUILDER
            .comment("The Ice Lich's hunting ground: how far (blocks) from its phylactery it can sense a player in the Frosted",
                    "Caves and rise behind them, and how far it will chase before it goes home. The crypt need not be loaded,",
                    "and only the nearest crypt to a player stalks them.")
            .defineInRange("lichAmbushRange", 200, 16, 4096);

    public static final ModConfigSpec.ConfigValue<String> GOD_SKY_DIMENSION = BUILDER
            .comment("The dimension whose heights the sun and moon gods are met in, by id. The Aether's by default; it need not be",
                    "installed (without it nobody can get there and neither god comes).")
            .define("godSkyDimension", "aether:the_aether");

    public static final ModConfigSpec.IntValue GOD_ARRIVAL_HEIGHT = BUILDER
            .comment("How high (y) a player must be in that dimension to meet a god. The Aether's build limit is 256, so 400 can",
                    "only be reached by flying. Dropping below it leaves Apollo's fight.")
            .defineInRange("godArrivalHeight", 400, -64, 4096);

    public static final ModConfigSpec.DoubleValue GOD_ZENITH_DEGREES = BUILDER
            .comment("How near the top of the sky (degrees from straight up) the sun must stand for Apollo, or the moon for Diana.",
                    "30 is roughly the two hours either side of noon or midnight.")
            .defineInRange("godZenithDegrees", 30.0, 1.0, 90.0);

    public static final ModConfigSpec.DoubleValue GOD_GAZE_DEGREES = BUILDER
            .comment("How close (degrees) a player's gaze must hold to the sun or moon to call its god.")
            .defineInRange("godGazeDegrees", 5.0, 0.5, 45.0);

    public static final ModConfigSpec.IntValue GOD_GAZE_SECONDS = BUILDER
            .comment("How many seconds in a row that gaze must hold (it is checked once a second).")
            .defineInRange("godGazeSeconds", 3, 1, 60);

    public static final ModConfigSpec.IntValue APOLLO_WARDENS = BUILDER
            .comment("How many Sun Spirits (the Aether's gold-dungeon boss, the sun's wardens) a player must have helped slay",
                    "before the sun answers their gaze and Apollo comes.")
            .defineInRange("apolloWardens", 4, 0, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    private MobsConfig() {
    }
}
