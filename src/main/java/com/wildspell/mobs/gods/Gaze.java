package com.wildspell.mobs.gods;

import com.wildspell.mobs.SpawnBalance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public enum Gaze {
    SUN(1.0),
    MOON(-1.0);

    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);
    private final double sign;

    Gaze(double sign) {
        this.sign = sign;
    }

    public Vec3 direction(float timeOfDay) {
        double angle = timeOfDay * Math.PI * 2.0;
        return new Vec3(-Math.sin(angle) * this.sign, Math.cos(angle) * this.sign, 0.0);
    }

    public Vec3 direction(Level level) {
        return this.direction(level.getTimeOfDay(1.0F));
    }

    public boolean isUp(Level level) {
        return this.direction(level).y > 0.0;
    }

    public boolean nearZenith(Vec3 body, double zenithDegrees) {
        return degreesBetween(body, UP) <= zenithDegrees;
    }

    public boolean holds(Vec3 look, float timeOfDay, double zenithDegrees, double gazeDegrees) {
        Vec3 body = this.direction(timeOfDay);
        return this.nearZenith(body, zenithDegrees) && degreesBetween(look, body) <= gazeDegrees;
    }

    public boolean holds(Player player) {
        return isAloft(player) && this.holds(player.getViewVector(1.0F), player.level().getTimeOfDay(1.0F),
                SpawnBalance.GOD_ZENITH_DEGREES.get(), SpawnBalance.GOD_GAZE_DEGREES.get());
    }

    public static double degreesBetween(Vec3 a, Vec3 b) {
        return Math.toDegrees(Math.acos(Mth.clamp(a.normalize().dot(b.normalize()), -1.0, 1.0)));
    }

    public static boolean isGodSky(Level level) {
        return level.dimension().location().equals(ResourceLocation.tryParse(SpawnBalance.GOD_SKY_DIMENSION.get()));
    }

    public static boolean isAloft(Player player) {
        return player.isAlive() && !player.isSpectator() && isGodSky(player.level()) && player.getY() > SpawnBalance.GOD_ARRIVAL_HEIGHT.get();
    }
}
