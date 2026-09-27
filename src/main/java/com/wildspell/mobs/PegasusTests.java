package com.wildspell.mobs;

import com.wildspell.mobs.entity.Pegasus;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-world checks for the Pegasus, run with the rest by ./gradlew runGameTestServer. */
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class PegasusTests {
    private static final String ARENA = "arena";
    /** 31x24x31 of open air: room to climb and circle (the arena's barrier roof is 7 up). */
    private static final String SKY = "sky_arena";

    /** Tamed, so it won't take wing on its own mid-test. */
    private static Pegasus tamedPegasus(GameTestHelper helper, float x, float y, float z) {
        Pegasus pegasus = helper.spawn(WildspellMobs.PEGASUS.get(), x, y, z);
        pegasus.setTamed(true);
        return pegasus;
    }

    @GameTest(template = SKY, timeoutTicks = 300)
    public static void pegasusGlidesDownUnhurt(GameTestHelper helper) {
        Pegasus pegasus = tamedPegasus(helper, 15.5F, 20.0F, 15.5F);
        double[] fastest = {0.0};
        double[] lastY = {pegasus.getY()};
        helper.onEachTick(() -> {
            fastest[0] = Math.max(fastest[0], lastY[0] - pegasus.getY());
            lastY[0] = pegasus.getY();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(pegasus.onGround(), "still in the air at relative y=" + helper.relativeVec(pegasus.position()).y);
            helper.assertTrue(fastest[0] <= Pegasus.GLIDE_SINK + 1.0E-6, "fell " + fastest[0] + " in a tick, faster than a glide");
            helper.assertTrue(pegasus.getHealth() == pegasus.getMaxHealth(), "hurt by the landing: " + pegasus.getHealth());
        });
    }

    @GameTest(template = SKY, timeoutTicks = 200)
    public static void riderClimbsThenGlides(GameTestHelper helper) {
        Pegasus pegasus = tamedPegasus(helper, 15.5F, 0.0F, 15.5F);
        pegasus.setNoAi(true);
        pegasus.equipSaddle(new ItemStack(Items.SADDLE), null);
        Player rider = helper.makeMockPlayer(GameType.SURVIVAL);
        rider.moveTo(pegasus.position());
        helper.assertTrue(rider.startRiding(pegasus, true), "rider could not mount");
        helper.assertTrue(pegasus.getControllingPassenger() == rider, "saddled pegasus isn't steered by its rider");
        double startY = pegasus.getY();
        rider.setXRot(0.0F);
        rider.jumping = true;
        int[] tick = {0};
        double[] topY = {startY};
        double[] sinkPerTick = {0.0};
        helper.onEachTick(() -> {
            tick[0]++;
            if (tick[0] == 40) {
                rider.jumping = false; // let go: it should glide, not drop
            }
            double before = pegasus.getY();
            pegasus.flyRidden(rider, Vec3.ZERO); // straight up: the sky arena is 31 wide, not 30 blocks of flight
            topY[0] = Math.max(topY[0], pegasus.getY());
            if (tick[0] > 60) {
                sinkPerTick[0] = Math.max(sinkPerTick[0], before - pegasus.getY());
            }
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(topY[0] - startY > 8.0, "climbed only " + (topY[0] - startY) + " blocks in 40 ticks");
            helper.assertTrue(pegasus.isNoGravity(), "a ridden pegasus aloft must be no-gravity so servers don't kick its rider");
            helper.assertTrue(sinkPerTick[0] > 0.0 && sinkPerTick[0] <= Pegasus.GLIDE_SINK + 0.02,
                    "glide sank " + sinkPerTick[0] + " a tick");
            rider.stopRiding();
            rider.discard();
            helper.succeed();
        });
    }

    // Own batch: the soar circles half the sky arena.
    @GameTest(template = SKY, timeoutTicks = 500, batch = "pegasusSoar")
    public static void wildPegasusSoarsAndComesBack(GameTestHelper helper) {
        Pegasus pegasus = helper.spawn(WildspellMobs.PEGASUS.get(), 15.5F, 0.0F, 15.5F);
        Vec3[] home = {null};
        double[] topY = {Double.NEGATIVE_INFINITY};
        helper.onEachTick(() -> {
            if (home[0] == null && pegasus.onGround() && pegasus.startSoaring()) {
                home[0] = pegasus.position();
            }
            topY[0] = Math.max(topY[0], pegasus.getY());
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(home[0] != null, "a wild pegasus on open ground never took wing");
            helper.assertTrue(!pegasus.isSoaring() && pegasus.onGround(), "still soaring");
            helper.assertTrue(topY[0] - home[0].y > 6.0, "rose only " + (topY[0] - home[0].y));
            double away = pegasus.position().subtract(home[0]).horizontalDistance();
            helper.assertTrue(away < 4.0, "came down " + away + " blocks from where it rose");
        });
    }

    @GameTest(template = ARENA)
    public static void tamedPegasusStaysGrounded(GameTestHelper helper) {
        Pegasus pegasus = tamedPegasus(helper, 4.5F, 0.0F, 4.5F);
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(pegasus.startSoaring(), "a tamed pegasus flew off on its own");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void pegasusSpawnsOnGrassNotStone(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 0, 2), Blocks.GRASS_BLOCK);
        helper.setBlock(new BlockPos(6, 0, 6), Blocks.STONE);
        EntityType<Pegasus> type = WildspellMobs.PEGASUS.get();
        helper.assertTrue(Pegasus.checkPegasusSpawnRules(type, helper.getLevel(), MobSpawnType.NATURAL,
                helper.absolutePos(new BlockPos(2, 1, 2)), helper.getLevel().random), "no spawn on grass in daylight");
        helper.assertFalse(Pegasus.checkPegasusSpawnRules(type, helper.getLevel(), MobSpawnType.NATURAL,
                helper.absolutePos(new BlockPos(6, 1, 6)), helper.getLevel().random), "spawned on stone");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void pegasiBreedPegasi(GameTestHelper helper) {
        Pegasus mare = tamedPegasus(helper, 2.5F, 1.0F, 2.5F);
        Pegasus stallion = tamedPegasus(helper, 5.5F, 1.0F, 5.5F);
        mare.setInLove(null);
        stallion.setInLove(null);
        helper.assertTrue(mare.canMate(stallion), "two tamed pegasi in love can't mate");
        helper.assertFalse(mare.canMate(helper.spawn(EntityType.HORSE, 4.5F, 1.0F, 4.5F)), "pegasus mated with a horse");
        helper.assertTrue(mare.getBreedOffspring(helper.getLevel(), stallion) instanceof Pegasus, "foal isn't a pegasus");
        helper.succeed();
    }
}
