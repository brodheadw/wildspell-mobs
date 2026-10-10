package com.wildspell.mobs;

import com.wildspell.mobs.entity.Stemwalker;
import com.wildspell.mobs.grove.SporeheartBlock;
import com.wildspell.mobs.grove.SporeheartBlockEntity;
import com.wildspell.mobs.grove.SporeheartFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class StemwalkerTests {
    private static final BlockPos HEART = new BlockPos(15, 2, 15);

    private static SporeheartBlockEntity grove(GameTestHelper helper, int stemAbove) {
        for (int x = 0; x < 31; ++x) {
            for (int z = 0; z < 31; ++z) {
                helper.setBlock(x, 0, z, Blocks.MYCELIUM);
            }
        }
        helper.setBlock(HEART.below(), Blocks.MUSHROOM_STEM);
        helper.setBlock(HEART, WildspellMobs.SPOREHEART.get());
        for (int y = 1; y <= stemAbove; ++y) {
            helper.setBlock(HEART.above(y), Blocks.MUSHROOM_STEM);
        }
        return (SporeheartBlockEntity) helper.getBlockEntity(HEART);
    }

    private static void at(GameTestHelper helper, long time) {
        helper.getLevel().setDayTime(time);
        onFinish(helper, () -> helper.getLevel().setDayTime(6000L));
    }

    private static void pulse(GameTestHelper helper, SporeheartBlockEntity heart) {
        heart.pulse(helper.getLevel(), helper.getBlockState(HEART));
    }

    private static Stemwalker bound(GameTestHelper helper, SporeheartBlockEntity heart) {
        Stemwalker walker = helper.spawn(WildspellMobs.STEMWALKER.get(), new Vec3(20.5, 1.0, 15.5));
        walker.setNoAi(true);
        walker.bindTo(heart.getBlockPos());
        return walker;
    }

    @GameTest(template = SKY, batch = "sporeheartNight")
    public static void theHeartWakesOnlyAtNightInsideAStem(GameTestHelper helper) {
        at(helper, 18000L);
        SporeheartBlockEntity heart = grove(helper, 6);
        pulse(helper, heart);
        helper.assertTrue(helper.getBlockState(HEART).getValue(SporeheartBlock.ACTIVE), "a heart in its stem at night stayed dormant");
        helper.setBlock(HEART.above(), Blocks.AIR);
        pulse(helper, heart);
        helper.assertFalse(helper.getBlockState(HEART).getValue(SporeheartBlock.ACTIVE), "a heart cut out of its stem stayed awake");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "sporeheartNight")
    public static void aBoundWalkerShrugsOffBlowsUntilItsHeartBreaks(GameTestHelper helper) {
        at(helper, 18000L);
        SporeheartBlockEntity heart = grove(helper, 6);
        pulse(helper, heart);
        Stemwalker walker = bound(helper, heart);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        walker.hurt(helper.getLevel().damageSources().playerAttack(player), 12.0F);
        helper.assertTrue(walker.getHealth() == walker.getMaxHealth(), "a walker with a living heart took the blow");
        helper.destroyBlock(HEART);
        helper.assertTrue(walker.isDeadOrDying(), "breaking the heart didn't fell its walker");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "sporeheartNight")
    public static void theHeartRaisesAWalkerNearItsQuarry(GameTestHelper helper) {
        at(helper, 18000L);
        SporeheartBlockEntity heart = grove(helper, 6);
        pulse(helper, heart);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new Vec3(15.5, 1.0, 26.5)));
        Stemwalker walker = heart.summon(helper.getLevel(), player);
        helper.assertTrue(walker != null, "the heart raised nothing");
        helper.assertTrue(heart.getBlockPos().equals(walker.getHeart()) && heart.bound().contains(walker.getUUID()), "the walker isn't bound to its heart");
        helper.assertTrue(walker.getPhase() == Stemwalker.EMERGING, "it didn't rise out of the ground");
        double away = walker.position().subtract(Vec3.atBottomCenterOf(heart.getBlockPos())).horizontalDistance();
        helper.assertTrue(away >= SporeheartBlockEntity.NEAREST - 1 && away <= SporeheartBlockEntity.FARTHEST + 1, "it rose " + away + " from its heart");
        helper.assertTrue(SporeheartFeature.soil(helper.getLevel().getBlockState(walker.blockPosition().below())), "it rose out of something that isn't soil");
        helper.killAllEntities();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "sporeheartDay", timeoutTicks = 200)
    public static void atDawnTheBoundSinkAway(GameTestHelper helper) {
        at(helper, 6000L);
        SporeheartBlockEntity heart = grove(helper, 6);
        Stemwalker walker = bound(helper, heart);
        helper.succeedWhen(() -> helper.assertTrue(walker.isRemoved() && !walker.isDeadOrDying(), "the walker lingered past dawn"));
    }
}
