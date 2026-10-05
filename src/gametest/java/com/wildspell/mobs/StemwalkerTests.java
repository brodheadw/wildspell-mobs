package com.wildspell.mobs;

import com.wildspell.mobs.entity.Stemwalker;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class StemwalkerTests {
    private static final String SKY = "sky_arena";

    private static void grove(GameTestHelper helper) {
        for (int x = 0; x < 31; ++x) {
            for (int z = 0; z < 31; ++z) {
                helper.setBlock(x, 0, z, Blocks.MYCELIUM);
            }
        }
    }

    private static Stemwalker stem(GameTestHelper helper, double x, double z) {
        Stemwalker stem = helper.spawn(WildspellMobs.STEMWALKER.get(), new Vec3(x, 1.0, z));
        stem.setNoAi(true);
        return stem;
    }

    private static Player player(GameTestHelper helper, double x, double z, Vec3 lookAt) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = helper.absoluteVec(new Vec3(x, 1.0, z));
        player.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, helper.absoluteVec(lookAt));
        return player;
    }

    @GameTest(template = SKY, batch = "stemwalker")
    public static void itSeesBeingSeenUpAndDownItsLength(GameTestHelper helper) {
        grove(helper);
        Stemwalker stem = stem(helper, 15.5, 15.5);
        helper.assertTrue(Stemwalker.sees(player(helper, 15.5, 9.5, new Vec3(15.5, 2.0, 15.5)), stem), "looking at its foot didn't count");
        helper.assertTrue(Stemwalker.sees(player(helper, 15.5, 9.5, new Vec3(15.5, 12.0, 15.5)), stem), "looking up at its crown didn't count");
        helper.assertFalse(Stemwalker.sees(player(helper, 15.5, 9.5, new Vec3(15.5, 2.0, 2.5)), stem), "looking away counted");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "stemwalker")
    public static void strikingOneWakesTheGrove(GameTestHelper helper) {
        grove(helper);
        Stemwalker struck = stem(helper, 5.5, 5.5);
        Stemwalker near = stem(helper, 25.5, 25.5);
        Player player = player(helper, 8.5, 8.5, new Vec3(5.5, 2.0, 5.5));
        helper.assertTrue(near.getAction() == Stemwalker.DORMANT, "a grove stem started awake");
        struck.hurt(helper.getLevel().damageSources().playerAttack(player), 2.0F);
        helper.assertTrue(near.getAction() == Stemwalker.AWAKE && near.getQuarry() == player, "the rest of the grove didn't wake to the blow");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "stemwalker")
    public static void watchedItHoldsUnwatchedItLeansAndSlams(GameTestHelper helper) {
        grove(helper);
        ServerLevel level = helper.getLevel();
        Stemwalker stem = stem(helper, 15.5, 8.5);
        Player player = player(helper, 15.5, 15.0, new Vec3(15.5, 2.0, 8.5));
        stem.wake(player);
        for (int i = 0; i < 40; ++i) {
            stem.step(level, true);
        }
        helper.assertTrue(stem.getAction() == Stemwalker.AWAKE, "it moved while watched");
        stem.step(level, false);
        helper.assertTrue(stem.getAction() == Stemwalker.LEANING, "unwatched and in reach, it didn't lean");
        for (int i = 0; i < 5; ++i) {
            stem.step(level, false);
        }
        int leaned = stem.actionTicks();
        for (int i = 0; i < 40; ++i) {
            stem.step(level, true);
        }
        helper.assertTrue(stem.getAction() == Stemwalker.LEANING && stem.actionTicks() == leaned, "the lean went on while watched");
        Vec3 impact = stem.impactPoint();
        helper.assertTrue(impact.subtract(player.position()).horizontalDistance() < Stemwalker.SLAM_RADIUS, "the cap won't come down on the player");
        Sheep sheep = EntityType.SHEEP.create(level);
        sheep.moveTo(impact.x, impact.y, impact.z);
        level.addFreshEntity(sheep);
        float before = sheep.getHealth();
        for (int i = 0; i < Stemwalker.LEAN_TICKS + Stemwalker.SLAM_TICKS + 2; ++i) {
            stem.step(level, false);
        }
        helper.assertTrue(stem.getAction() == Stemwalker.RECOVERING, "it never slammed");
        helper.assertTrue(sheep.getHealth() < before, "the slam missed what stood under the cap");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "stemwalker")
    public static void outOfReachItSinksAndRisesNearTheQuarry(GameTestHelper helper) {
        grove(helper);
        ServerLevel level = helper.getLevel();
        Stemwalker stem = stem(helper, 3.5, 3.5);
        Player player = player(helper, 20.5, 20.5, new Vec3(27.5, 2.0, 27.5));
        stem.wake(player);
        stem.step(level, false);
        helper.assertTrue(stem.getAction() == Stemwalker.SINKING, "far from its quarry, it didn't sink");
        int sunk = stem.actionTicks();
        for (int i = 0; i < 20; ++i) {
            stem.step(level, true);
        }
        helper.assertTrue(stem.getAction() == Stemwalker.SINKING && stem.actionTicks() == sunk, "it kept sinking while watched");
        for (int i = 0; i < Stemwalker.SINK_TICKS; ++i) {
            stem.step(level, false);
        }
        helper.assertTrue(stem.getAction() == Stemwalker.RISING, "it never rose");
        double away = stem.position().subtract(player.position()).horizontalDistance();
        helper.assertTrue(away > Stemwalker.SLAM_NEAREST - 1.0 && away < Stemwalker.SLAM_FARTHEST, "it rose " + away + " from its quarry");
        helper.assertFalse(Stemwalker.sees(player, stem), "it rose where its quarry was looking");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "stemwalker")
    public static void itTakesRootOnlyInSoil(GameTestHelper helper) {
        grove(helper);
        helper.setBlock(4, 0, 4, Blocks.STONE);
        ServerLevel level = helper.getLevel();
        BlockPos soil = helper.absolutePos(new BlockPos(10, 1, 10));
        BlockPos stone = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(Stemwalker.checkStemwalkerSpawnRules(WildspellMobs.STEMWALKER.get(), level, MobSpawnType.TRIAL_SPAWNER, soil, level.random), "no root in mycelium");
        helper.assertFalse(Stemwalker.checkStemwalkerSpawnRules(WildspellMobs.STEMWALKER.get(), level, MobSpawnType.TRIAL_SPAWNER, stone, level.random), "took root in stone");
        helper.succeed();
    }
}
