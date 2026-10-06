package com.wildspell.mobs.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class MotionSense {
    private static final double MOVED = 0.15;

    private Map<Integer, Vec3> lastSeen = new HashMap<>();
    private final Set<Integer> moving = new HashSet<>();

    public void sense(List<? extends Entity> nearby) {
        Map<Integer, Vec3> seen = new HashMap<>();
        this.moving.clear();
        for (Entity other : nearby) {
            seen.put(other.getId(), other.position());
            Vec3 before = this.lastSeen.get(other.getId());
            if (before != null && before.distanceToSqr(other.position()) > MOVED * MOVED) {
                this.moving.add(other.getId());
            }
        }
        this.lastSeen = seen;
    }

    public boolean isMoving(Entity other) {
        return this.moving.contains(other.getId());
    }

    public void markMoving(Entity other) {
        this.moving.add(other.getId());
    }

    public void forget() {
        this.lastSeen.clear();
        this.moving.clear();
    }
}
