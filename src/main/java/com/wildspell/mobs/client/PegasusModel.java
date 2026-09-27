package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Pegasus;
import java.util.EnumSet;
import java.util.Set;
import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

/**
 * The vanilla horse with a pair of great feathered wings rising from the withers. Each wing is built like a
 * bird's: humerus, forearm and hand, with five tertials on the humerus, eight secondaries on the forearm,
 * nine long primaries fanned from the hand and three rows of coverts (marginal, median, greater) over each
 * bone, the marginals forming the leading edge. Every feather is a single flat face with its own patch of
 * the texture, so overlapping planes never fight and each can be coloured on its own.
 *
 * <p>Spread, the humerus climbs steeply from the shoulder, the forearm levels out and the hand sweeps back,
 * the primaries splayed; it beats from the shoulder, the outer wing lagging, deep when it climbs and shallow
 * when it glides. Landed, it folds like a bird's wing: humerus back, forearm forward, hand back again (the
 * forearm and hand turned over as they fold), the upper side facing out, the coverts forming the shoulder
 * and every flight feather laid back and drooping over the flank.
 *
 * <p>Planes are built in the wing's frame: +x along the bone (out from the body when spread), +z toward the
 * trailing edge, -y the upper side. The texture is 128x128: the horse's 64x64 sheet in the top left, the
 * coverts to its right and the flight feathers below; paint_pegasus.py uses the same layout.
 */
public class PegasusModel extends HorseModel<Pegasus> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(WildspellMobs.id("pegasus"), "main");
    private static final Set<Direction> TOP_ONLY = EnumSet.of(Direction.DOWN); // the -y face, at (u + depth, v)

    private static final float HUMERUS = 10.0F;
    private static final float FOREARM = 12.0F;
    private static final float HAND = 8.0F;
    private static final float FEATHER_WIDTH = 3.0F;

    // Flight feathers along each bone: where each sits along it and how long it is, inner first.
    private static final float[] TERTIAL_AT = {0.5F, 2.5F, 4.5F, 6.5F, 8.0F};
    private static final float[] TERTIAL_LENGTH = {11.0F, 12.0F, 13.0F, 14.0F, 15.0F};
    private static final float[] SECONDARY_AT = {0.0F, 1.5F, 3.0F, 4.5F, 6.0F, 7.5F, 9.0F, 10.5F};
    private static final float[] SECONDARY_LENGTH = {18.0F, 18.0F, 19.0F, 19.0F, 20.0F, 20.0F, 21.0F, 21.0F};
    private static final float[] PRIMARY_AT = {0.0F, 0.9F, 1.8F, 2.7F, 3.6F, 4.5F, 5.4F, 6.3F, 7.2F};
    private static final float[] PRIMARY_LENGTH = {20.0F, 21.5F, 23.0F, 24.5F, 26.0F, 27.0F, 28.0F, 29.0F, 30.0F};
    /** Covert rows over every bone, outermost (the leading edge) first: marginal, median, greater. */
    private static final float[] COVERT_DEPTH = {4.0F, 6.0F, 9.0F};
    private static final float[] COVERT_LIFT = {-0.45F, -0.35F, -0.25F};
    private static final float[] COVERT_BACK = {-1.0F, 0.0F, 0.4F};
    /** Where each flat face sits on the sheet (u, v of its top-left corner); paint_pegasus.py matches these. */
    private static final int[] COVERT_FACE_U = {64, 76, 90}; // per bone
    private static final int[] COVERT_FACE_V = {0, 6, 14}; // per row
    /** Flight feathers' faces run 3 wide from here along one row: tertials, secondaries, then primaries. */
    private static final int FEATHER_FACE_U = 30;
    private static final int FEATHER_FACE_V = 64;

    /** Each feather sits this much below the one before it, so overlaps never z-fight. */
    private static final float LAYER_STEP = 0.03F;

    // Folded (left wing; the right mirrors it). See the class comment: back, forward, back, upper side out.
    private static final float FOLDED_HUMERUS_YAW = -Mth.HALF_PI;
    private static final float FOLDED_ROLL = Mth.HALF_PI;
    /** How far folded flight feathers droop below the line of the back, in radians: most at the inner end. */
    private static final float TERTIAL_DROOP = 0.5F;
    private static final float SECONDARY_DROOP = 0.45F;
    private static final float PRIMARY_DROOP = 0.14F;
    /** Folded flight feathers are drawn shorter (along their length) so they end over the rump, not past it. */
    private static final float FOLDED_TERTIAL_LENGTH = 0.75F;
    private static final float FOLDED_FLIGHT_LENGTH = 0.6F;

    // Spread.
    private static final float SPREAD_HUMERUS_YAW = -0.1F;
    private static final float SPREAD_HUMERUS_RAISE = -1.0F;
    private static final float SPREAD_FOREARM_YAW = 0.1F;
    private static final float SPREAD_FOREARM_BEND = 0.28F;
    private static final float SPREAD_HAND_YAW = -0.08F;
    private static final float SPREAD_HAND_BEND = 0.1F;
    private static final float PRIMARY_SPLAY = 0.11F;
    private static final float BEAT = 0.6F;
    private static final float OUTER_BEAT = 0.45F;

    private final Wing left;
    private final Wing right;

    public PegasusModel(ModelPart root) {
        super(root);
        this.left = new Wing(this.body.getChild("left_wing"), 1.0F);
        this.right = new Wing(this.body.getChild("right_wing"), -1.0F);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HorseModel.createBodyMesh(CubeDeformation.NONE);
        PartDefinition body = mesh.getRoot().getChild("body");
        addWing(body, "left_wing", 1.0F);
        addWing(body, "right_wing", -1.0F);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Builds one wing, running out along +x for the left ({@code side} 1) and -x for the right (-1). */
    private static void addWing(PartDefinition body, String name, float side) {
        // The bones carry no geometry of their own: the marginal coverts make the leading edge.
        PartDefinition humerus = body.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.offset(side * 5.2F, -7.0F, -9.0F));
        PartDefinition forearm = humerus.addOrReplaceChild("forearm", CubeListBuilder.create(), PartPose.offset(side * HUMERUS, 0.0F, 0.0F));
        PartDefinition hand = forearm.addOrReplaceChild("hand", CubeListBuilder.create(), PartPose.offset(side * FOREARM, 0.0F, 0.0F));
        PartDefinition[] bones = {humerus, forearm, hand};
        float[] lengths = {HUMERUS, FOREARM, HAND};
        for (int b = 0; b < 3; b++) {
            for (int row = 0; row < COVERT_DEPTH.length; row++) {
                int[] face = {COVERT_FACE_U[b], COVERT_FACE_V[row]};
                bones[b].addOrReplaceChild("covert_" + row, plane(side, face, lengths[b], COVERT_DEPTH[row]),
                        PartPose.offset(0.0F, COVERT_LIFT[row], COVERT_BACK[row]));
            }
        }
        int u = FEATHER_FACE_U;
        u = feathers(humerus, side, TERTIAL_AT, TERTIAL_LENGTH, 0.0F, u);
        u = feathers(forearm, side, SECONDARY_AT, SECONDARY_LENGTH, 0.0F, u);
        feathers(hand, side, PRIMARY_AT, PRIMARY_LENGTH, 0.2F, u);
    }

    /** Lays flight feathers along a bone, each with its own face from {@code u} on; returns the next free u. */
    private static int feathers(PartDefinition bone, float side, float[] at, float[] length, float layer, int u) {
        for (int i = 0; i < at.length; i++) {
            int[] face = {u, FEATHER_FACE_V};
            bone.addOrReplaceChild("feather_" + i, plane(side, face, FEATHER_WIDTH, length[i]),
                    PartPose.offset(side * at[i], layer + i * LAYER_STEP, 0.5F));
            u += (int) FEATHER_WIDTH;
        }
        return u;
    }

    /** A flat face {@code width} along the bone and {@code depth} back from it; the face's texture sits at {@code face}. */
    private static CubeListBuilder plane(float side, int[] face, float width, float depth) {
        return CubeListBuilder.create().texOffs(face[0] - (int) depth, face[1]).mirror(side < 0.0F)
                .addBox(side < 0.0F ? -width : 0.0F, 0.0F, 0.0F, width, 0.0F, depth, TOP_ONLY);
    }

    @Override
    public void prepareMobModel(Pegasus pegasus, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(pegasus, limbSwing, limbSwingAmount, partialTick);
        float spread = pegasus.getWingSpread(partialTick);
        float flap = pegasus.getFlap(partialTick);
        float strength = pegasus.getFlapStrength(partialTick);
        float beat = -Mth.sin(flap) * BEAT * strength;
        float outerBeat = -Mth.sin(flap - 0.9F) * OUTER_BEAT * strength;
        this.left.pose(spread, beat, outerBeat);
        this.right.pose(spread, beat, outerBeat);
    }

    private static final class Wing {
        private final float side;
        private final ModelPart humerus;
        private final ModelPart forearm;
        private final ModelPart hand;
        private final ModelPart[] coverts;
        private final ModelPart[] tertials;
        private final ModelPart[] secondaries;
        private final ModelPart[] primaries;

        Wing(ModelPart humerus, float side) {
            this.side = side;
            this.humerus = humerus;
            this.forearm = humerus.getChild("forearm");
            this.hand = this.forearm.getChild("hand");
            ModelPart[] bones = {this.humerus, this.forearm, this.hand};
            this.coverts = new ModelPart[bones.length * COVERT_DEPTH.length];
            for (int b = 0; b < bones.length; b++) {
                for (int row = 0; row < COVERT_DEPTH.length; row++) {
                    this.coverts[b * COVERT_DEPTH.length + row] = bones[b].getChild("covert_" + row);
                }
            }
            this.tertials = feathers(this.humerus, TERTIAL_AT.length);
            this.secondaries = feathers(this.forearm, SECONDARY_AT.length);
            this.primaries = feathers(this.hand, PRIMARY_AT.length);
        }

        /** The inner feather droops by {@code most}, the outer about a third as much. */
        private static float droop(float most, int i, int count) {
            return most * (1.0F - 0.65F * i / (count - 1));
        }

        private static ModelPart[] feathers(ModelPart bone, int count) {
            ModelPart[] feathers = new ModelPart[count];
            for (int i = 0; i < count; i++) {
                feathers[i] = bone.getChild("feather_" + i);
            }
            return feathers;
        }

        /**
         * Poses it as the left wing would be, mirrored by {@code side} (mirroring across x flips yaw and roll,
         * not the turn about the bone's own axis).
         *
         * <p>Folded, the forearm and hand are each turned half about the vertical and half about their own
         * length, so the forearm frame is the humerus frame turned half about z and the hand frame is the
         * humerus frame again. The humerus frame's +x is back along the body and -z is down, so a feather
         * drooping {@code d} below the back points at (cos d, 0, -sin d) there, which is a yaw of
         * pi/2 + d from the humerus or hand and -(pi/2 + d) from the forearm.
         */
        void pose(float spread, float beat, float outerBeat) {
            float s = this.side;
            this.humerus.yRot = s * Mth.lerp(spread, FOLDED_HUMERUS_YAW, SPREAD_HUMERUS_YAW);
            this.humerus.zRot = s * Mth.lerp(spread, FOLDED_ROLL, SPREAD_HUMERUS_RAISE + beat);
            this.forearm.xRot = Mth.lerp(spread, Mth.PI, 0.0F);
            this.forearm.yRot = s * Mth.lerp(spread, Mth.PI, SPREAD_FOREARM_YAW);
            this.forearm.zRot = s * Mth.lerp(spread, 0.0F, SPREAD_FOREARM_BEND + outerBeat * 0.5F);
            this.hand.xRot = Mth.lerp(spread, Mth.PI, 0.0F);
            this.hand.yRot = s * Mth.lerp(spread, Mth.PI, SPREAD_HAND_YAW);
            this.hand.zRot = s * Mth.lerp(spread, 0.0F, SPREAD_HAND_BEND + outerBeat);
            // Folded, the coverts turn over to hang down the flank from the bones instead of standing up.
            for (ModelPart covert : this.coverts) {
                covert.xRot = Mth.lerp(spread, Mth.PI, 0.0F);
            }
            float tertialLength = Mth.lerp(spread, FOLDED_TERTIAL_LENGTH, 1.0F);
            float flightLength = Mth.lerp(spread, FOLDED_FLIGHT_LENGTH, 1.0F);
            for (int i = 0; i < this.tertials.length; i++) {
                this.tertials[i].yRot = s * Mth.lerp(spread, Mth.HALF_PI + droop(TERTIAL_DROOP, i, this.tertials.length), 0.0F);
                this.tertials[i].zScale = tertialLength;
            }
            for (int i = 0; i < this.secondaries.length; i++) {
                this.secondaries[i].yRot = s * Mth.lerp(spread, -(Mth.HALF_PI + droop(SECONDARY_DROOP, i, this.secondaries.length)), 0.0F);
                this.secondaries[i].zScale = flightLength;
            }
            for (int i = 0; i < this.primaries.length; i++) {
                this.primaries[i].yRot = s * Mth.lerp(spread, Mth.HALF_PI + droop(PRIMARY_DROOP, i, this.primaries.length),
                        PRIMARY_SPLAY * 6.0F / this.primaries.length * (i + 0.5F));
                this.primaries[i].zScale = flightLength;
            }
        }
    }
}
