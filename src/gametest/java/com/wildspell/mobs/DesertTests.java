package com.wildspell.mobs;

import com.wildspell.mobs.entity.Scarab;
import com.wildspell.mobs.entity.Scorpion;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class DesertTests {
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "scorpionSting")
    public static void aBuriedScorpionStingsWhatWalksPast(GameTestHelper helper) {
        dunes(helper, true);
        Scorpion scorpion = buried(helper);
        Player walker = walker(helper);
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (scorpion.isBuried() || scorpion.getTarget() != null) {
                double along = 4.5 + 2.5 * Math.sin(tick[0]++ * 0.08);
                place(helper, walker, new Vec3(along, 1.0, 3.5));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(walker.hasEffect(MobEffects.POISON), "not poisoned; scorpion phase " + scorpion.getPhase());
            helper.assertTrue(walker.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "stung but not slowed");
            walker.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "scorpionSneak")
    public static void sneakingOrStandingStillLeavesAScorpionBuried(GameTestHelper helper) {
        dunes(helper, true);
        Scorpion scorpion = buried(helper);
        Player sneaker = walker(helper);
        sneaker.setShiftKeyDown(true);
        Player still = walker(helper);
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (scorpion.isBuried()) {
                place(helper, sneaker, new Vec3(4.5 + 1.5 * Math.sin(tick[0]++ * 0.08), 1.0, 3.5));
                place(helper, still, new Vec3(5.5, 1.0, 4.5));
            }
        });
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(scorpion.isBuried(), "a sneaking or still player woke the scorpion");
            sneaker.discard();
            still.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 500, batch = "scorpionRetreat")
    public static void aHurtScorpionRetreatsAndBurrowsAgain(GameTestHelper helper) {
        dunes(helper, true);
        Scorpion scorpion = helper.spawn(WildspellMobs.SCORPION.get(), 2.5F, 1.0F, 4.5F);
        Player attacker = walker(helper);
        place(helper, attacker, new Vec3(1.5, 1.0, 4.5));
        Vec3 from = scorpion.position();
        helper.runAfterDelay(5, () -> {
            scorpion.hurt(helper.getLevel().damageSources().playerAttack(attacker), 1.0F);
            helper.assertTrue(scorpion.isRetreating(), "a hurt scorpion isn't retreating");
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(scorpion.isAlive() && scorpion.isBuried(), "never burrowed again; phase " + scorpion.getPhase());
            helper.assertTrue(scorpion.position().distanceTo(from) > 1.5, "burrowed where it was hit");
            attacker.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 90, batch = "scorpionNoon")
    public static void theSunDrivesAScorpionUnder(GameTestHelper helper) {
        noon(helper);
        dunes(helper, false);
        Scorpion scorpion = helper.spawn(WildspellMobs.SCORPION.get(), 4.5F, 1.0F, 4.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(scorpion.inSun(), "the test scorpion isn't in the sun");
            helper.assertTrue(scorpion.isBuried(), "a scorpion in the sun stayed up; phase " + scorpion.getPhase());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1200, batch = "scarabNoon")
    public static void aSurfaceScarabGoesUnderOnceDawnIsOver(GameTestHelper helper) {
        noon(helper);
        dunes(helper, false);
        Scarab wild = WildspellMobs.SCARAB.get().create(helper.getLevel());
        Vec3 at = helper.absoluteVec(new Vec3(3.5, 1.0, 4.5));
        wild.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(wild);
        onFinish(helper, wild::discard);
        Scarab kept = helper.spawn(WildspellMobs.SCARAB.get(), 5.5F, 1.0F, 4.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(wild.isRemoved(), "a wild scarab is still rolling at noon");
            helper.assertTrue(kept.isAlive(), "a kept scarab went under");
            kept.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 900, batch = "scarabRoll")
    public static void aScarabRollsStraightAndTurnsAtAWall(GameTestHelper helper) {
        dunes(helper, true);
        Scarab scarab = helper.spawn(WildspellMobs.SCARAB.get(), 1.5F, 1.0F, 4.5F);
        scarab.setHeading(Scarab.EAST);
        Vec3 start = scarab.position();
        double[] drift = {0.0};
        helper.onEachTick(() -> {
            if (scarab.getTurns() == 0) {
                drift[0] = Math.max(drift[0], Math.abs(scarab.getZ() - start.z));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(scarab.getTurns() > 0, "never turned; at " + helper.relativeVec(scarab.position()));
            helper.assertTrue(scarab.getX() - start.x > 4.0, "turned before reaching the wall; rolled " + (scarab.getX() - start.x));
            helper.assertTrue(drift[0] < 0.5, "wandered " + drift[0] + " off its line");
            helper.assertTrue(Math.abs(Mth.wrapDegrees(scarab.getHeading() - Scarab.EAST)) >= 60.0F, "still heading into the wall: " + scarab.getHeading());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "scarabBottle")
    public static void aBottledScarabKeepsItsBallAndIsReleased(GameTestHelper helper) {
        dunes(helper, true);
        Scarab wild = helper.spawn(WildspellMobs.SCARAB.get(), 4.5F, 1.0F, 4.5F);
        wild.setDung(true);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.runAfterDelay(5, () -> {
            wild.interact(player, InteractionHand.MAIN_HAND);
            ItemStack held = player.getMainHandItem();
            helper.assertTrue(BuiltInRegistries.ITEM.getKey(held.getItem()).equals(WildspellMobs.id("bottled_scarab")), "not bottled: holding " + held);
            helper.assertTrue(wild.isRemoved(), "bottled scarab still in the world");
            BlockPos floor = helper.absolutePos(new BlockPos(2, 0, 2));
            held.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false)));
            helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "bottle not emptied: holding " + player.getMainHandItem());
            List<Scarab> scarabs = helper.getEntities(WildspellMobs.SCARAB.get());
            helper.assertTrue(scarabs.size() == 1, "expected the released scarab, found " + scarabs.size());
            helper.assertTrue(scarabs.get(0).isDung(), "the released scarab lost its dung ball");
            helper.assertTrue(scarabs.get(0).isPersistenceRequired(), "a released scarab can despawn");
            scarabs.get(0).discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "desertGround")
    public static void scorpionsAndScarabsKeepToSand(GameTestHelper helper) {
        dunes(helper, true);
        helper.setBlock(2, 0, 2, Blocks.STONE);
        helper.setBlock(6, 0, 6, Blocks.RED_SAND);
        Block ancient = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "ancient_sand"));
        helper.assertTrue(ancient != Blocks.AIR, "yungscavebiomes:ancient_sand is not loaded");
        helper.setBlock(6, 0, 2, ancient);
        whenSealed(helper, new BlockPos(4, 1, 4), () -> {
            for (BlockPos spot : List.of(new BlockPos(4, 1, 4), new BlockPos(6, 1, 6), new BlockPos(6, 1, 2))) {
                helper.assertTrue(Scorpion.canBurrowAt(helper.getLevel(), helper.absolutePos(spot)), "a scorpion can't burrow at " + spot);
                helper.assertTrue(scarabCanSpawn(helper, spot), "a scarab can't spawn in the dark at " + spot);
            }
            helper.assertFalse(Scorpion.canBurrowAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))), "a scorpion burrows into stone");
            helper.assertFalse(scarabCanSpawn(helper, new BlockPos(2, 1, 2)), "a scarab spawns on stone");
            helper.assertTrue(Scarab.isDawn(23500L) && Scarab.isDawn(24500L) && !Scarab.isDawn(6000L) && !Scarab.isDawn(18000L), "dawn is wrong");
            helper.succeed();
        });
    }

    private static boolean scarabCanSpawn(GameTestHelper helper, BlockPos spot) {
        return Scarab.checkScarabSpawnRules(WildspellMobs.SCARAB.get(), helper.getLevel(), MobSpawnType.NATURAL,
                helper.absolutePos(spot), helper.getLevel().random);
    }

    private static void dunes(GameTestHelper helper, boolean roofed) {
        for (int x = 0; x <= 8; ++x) {
            for (int z = 0; z <= 8; ++z) {
                helper.setBlock(x, 0, z, Blocks.SAND);
                boolean edge = x == 0 || x == 8 || z == 0 || z == 8;
                for (int y = 1; y <= 3; ++y) {
                    helper.setBlock(x, y, z, edge ? Blocks.SANDSTONE : Blocks.AIR);
                }
                for (int y = 4; y <= 15; ++y) {
                    helper.setBlock(x, y, z, roofed && y == 4 ? Blocks.SANDSTONE : Blocks.AIR);
                }
            }
        }
    }

    private static Scorpion buried(GameTestHelper helper) {
        Scorpion scorpion = helper.spawn(WildspellMobs.SCORPION.get(), 4.5F, 1.0F, 4.5F);
        scorpion.burrow();
        return scorpion;
    }

    private static Player walker(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        place(helper, player, new Vec3(1.5, 1.0, 1.5));
        helper.getLevel().addFreshEntity(player);
        onFinish(helper, player::discard);
        return player;
    }

    private static void place(GameTestHelper helper, Player player, Vec3 at) {
        Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setOnGround(true);
    }

    private static void noon(GameTestHelper helper) {
        long time = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(6000L);
        onFinish(helper, () -> helper.getLevel().setDayTime(time));
    }
}
