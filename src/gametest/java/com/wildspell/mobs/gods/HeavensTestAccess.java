package com.wildspell.mobs.gods;

import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;

public final class HeavensTestAccess {
    private HeavensTestAccess() {
    }

    public static void restoreTheSun(ServerLevel level) {
        Heavens.get(level).restoreTheSun();
    }

    public static void forgetHunt(ServerLevel level) {
        Heavens.get(level).forgetHunt();
    }

    public static Set<String> pending(ServerLevel level, UUID player) {
        return Heavens.get(level).pending(player);
    }

    public static void forgetPending(ServerLevel level, UUID player) {
        Heavens.get(level).forgetPending(player);
    }

    public static void addSlayer(ServerLevel level, UUID player) {
        Heavens.get(level).addSlayer(player);
    }

    public static void dawn(ServerLevel level, Heavens.Hunt hunt) {
        Heavens.get(level).dawn(level.getServer(), hunt);
    }
}
