package com.wildspell.mobs.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

public final class WatcherTestAccess {
    private WatcherTestAccess() {
    }

    public static void work(Watcher watcher, Player quarry) {
        watcher.beginWork((ServerLevel) watcher.level(), quarry);
    }
}
