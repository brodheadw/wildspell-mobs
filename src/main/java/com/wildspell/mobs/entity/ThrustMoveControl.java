package com.wildspell.mobs.entity;

import javax.annotation.Nullable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;

public class ThrustMoveControl extends MoveControl {
    private final double accel;
    private final double brake;

    public ThrustMoveControl(Mob mob, double accel, double brake) {
        super(mob);
        this.accel = accel;
        this.brake = brake;
    }

    protected Vec3 drift(Vec3 motion) {
        return motion;
    }

    protected double arrival() {
        return 0.5;
    }

    protected double thrust(double distance) {
        return this.speedModifier * this.accel;
    }

    protected void face(@Nullable Vec3 heading) {
    }

    @Override
    public void tick() {
        Vec3 motion = this.drift(this.mob.getDeltaMovement());
        Vec3 heading = null;
        if (this.operation == Operation.MOVE_TO) {
            Vec3 to = new Vec3(this.wantedX - this.mob.getX(), this.wantedY - this.mob.getY(), this.wantedZ - this.mob.getZ());
            double distance = to.length();
            if (distance < this.arrival()) {
                this.operation = Operation.WAIT;
                motion = motion.scale(this.brake);
            } else {
                motion = motion.add(to.scale(this.thrust(distance) / distance));
                heading = to;
            }
        }
        this.mob.setDeltaMovement(motion);
        this.face(heading);
    }
}
