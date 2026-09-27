package com.wildspell.mobs;

import com.wildspell.mobs.entity.LuminousMoth;
import com.wildspell.mobs.moth.MothBottleItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-world checks for the Luminous Moth, run with the rest by ./gradlew runGameTestServer. */
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class LuminousMothTests {
    private static final String ARENA = "arena";
    private static final ResourceKey<Biome> LUSH_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves"));

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void mothHoversWithoutFalling(GameTestHelper helper) {
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 4.0F, 4.5F);
        moth.setNoAi(true);
        helper.runAfterDelay(60, () -> {
            double height = helper.relativeVec(moth.position()).y;
            helper.assertTrue(Math.abs(height - 4.0) < 0.01, "moth fell to relative y=" + height);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void lushCavesSpawnMoths(GameTestHelper helper) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(LUSH_CAVES);
        boolean listed = biome.getMobSettings().getMobs(MobCategory.AMBIENT).unwrap().stream()
                .anyMatch(data -> data.type == WildspellMobs.LUMINOUS_MOTH.get());
        helper.assertTrue(listed, "luminous moth missing from lush caves ambient spawns");
        helper.succeed();
    }

    // Own batch: light from one test's moth mustn't reach into another's sealed cave.
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "mothTrail")
    public static void mothsLightGoesWithIt(GameTestHelper helper) {
        sealCave(helper);
        BlockPos spot = new BlockPos(4, 2, 4);
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 2.2F, 4.5F);
        moth.setNoAi(true);
        helper.runAfterDelay(30, () -> {
            helper.assertBlockPresent(WildspellMobs.MOTH_GLOW.get(), spot);
            helper.assertTrue(blockLight(helper, spot) >= LuminousMoth.TRAIL_LIGHT, "moth's spot lit only " + blockLight(helper, spot));
            moth.discard();
        });
        helper.runAfterDelay(80, () -> {
            helper.assertBlockNotPresent(WildspellMobs.MOTH_GLOW.get(), spot);
            helper.assertTrue(blockLight(helper, spot) == 0, "light lingered after the moth left: " + blockLight(helper, spot));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothPerch")
    public static void mothLandsOnPlantsAndBrightensMoss(GameTestHelper helper) {
        for (int x = 0; x < 9; ++x) {
            for (int z = 0; z < 9; ++z) {
                helper.setBlock(x, 0, z, Blocks.MOSS_BLOCK);
                if ((x + z) % 2 == 0) {
                    helper.setBlock(x, 1, z, Blocks.MOSS_CARPET);
                }
            }
        }
        helper.setBlock(4, 1, 5, Blocks.AZALEA);
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 3.0F, 4.5F);
        boolean[] perched = {false};
        helper.onEachTick(() -> perched[0] |= moth.isPerched());
        helper.succeedWhen(() -> {
            helper.assertTrue(perched[0], "moth never landed; at " + helper.relativeVec(moth.position()));
            helper.assertTrue(countLuminousMoss(helper) > 0, "moth landed but brightened no moss");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothFlush")
    public static void movingNearbyFlushesAPerchedMoth(GameTestHelper helper) {
        watchAPerchedMoth(helper, false, (moth, walkedTicks) -> {
            helper.assertTrue(walkedTicks < 40 || !moth.isPerched(), "still perched after 40 ticks of a player walking past");
            return !moth.isPerched();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "mothFlee")
    public static void aMothInFlightFleesAPlayer(GameTestHelper helper) {
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 4.0F, 4.5F);
        Player player = addMockPlayer(helper, new Vec3(2.0, 1.0, 4.5));
        double start = moth.distanceTo(player);
        helper.runAfterDelay(40, () -> {
            double now = moth.distanceTo(player);
            helper.assertTrue(now > LuminousMoth.FLEE_RADIUS, "in two seconds the moth only got from " + start + " to " + now + " blocks off");
            succeedAndLeave(helper, player);
        });
    }

    // Own batch: a lure next door would draw the fleeing moth to it.
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "mothLureStay")
    public static void aMothStaysNearAPlayerWithASporeBlossom(GameTestHelper helper) {
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 3.0F, 4.5F);
        Player player = addMockPlayer(helper, new Vec3(2.0, 1.0, 4.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPORE_BLOSSOM));
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(moth.distanceTo(player) < 4.0, "fled a player holding a lure: " + moth.distanceTo(player));
            succeedAndLeave(helper, player);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothSneak")
    public static void sneakingPastLeavesAPerchedMothBe(GameTestHelper helper) {
        watchAPerchedMoth(helper, true, (moth, walkedTicks) -> {
            helper.assertTrue(moth.isPerched(), "a sneaking player flushed the moth after " + walkedTicks + " ticks");
            return walkedTicks >= 80;
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothWall")
    public static void mothSettlesOnAWall(GameTestHelper helper) {
        // A closed stone room over water: its walls are the only place left to settle.
        for (int x = 1; x <= 7; ++x) {
            for (int y = 0; y <= 6; ++y) {
                for (int z = 1; z <= 7; ++z) {
                    boolean shell = x == 1 || x == 7 || z == 1 || z == 7 || y == 6;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : y == 0 ? Blocks.WATER : Blocks.AIR);
                }
            }
        }
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 3.0F, 4.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(moth.isPerched(), "moth hasn't settled; at " + helper.relativeVec(moth.position()));
            helper.assertTrue(moth.getPerchFace().getAxis().isHorizontal(), "settled facing " + moth.getPerchFace() + ", not on a wall");
            helper.assertTrue(moth.getHealth() == moth.getMaxHealth(), "hurt itself settling on the wall: " + moth.getHealth());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "mothMossFade")
    public static void luminousMossFadesOnlyWithNoMothNear(GameTestHelper helper) {
        // Keeping moss lit looks in a box six blocks out, so the lonely moss sits outside the moth's.
        BlockPos lonely = new BlockPos(0, 1, 0);
        BlockPos kept = new BlockPos(7, 1, 7);
        helper.setBlock(lonely, WildspellMobs.LUMINOUS_MOSS.get());
        helper.setBlock(kept, WildspellMobs.LUMINOUS_MOSS_CARPET.get());
        helper.setBlock(kept.below(), Blocks.STONE);
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 8.5F, 3.0F, 8.5F);
        moth.setNoAi(true);
        helper.runAfterDelay(2, () -> {
            for (int i = 0; i < 200; ++i) {
                randomTick(helper, lonely);
                randomTick(helper, kept);
            }
            helper.assertBlockPresent(Blocks.MOSS_BLOCK, lonely);
            helper.assertBlockPresent(WildspellMobs.LUMINOUS_MOSS_CARPET.get(), kept);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "mothLure")
    public static void mothFollowsASporeBlossom(GameTestHelper helper) {
        Player player = addMockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SPORE_BLOSSOM));
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 7.5F, 3.0F, 7.5F);
        helper.onEachTick(() -> {
            if (moth.distanceTo(player) < 3.0F) {
                succeedAndLeave(helper, player);
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "mothLantern")
    public static void bottledMothReleasedInTheDarkLightsIt(GameTestHelper helper) {
        sealCave(helper);
        BlockPos floor = new BlockPos(4, 0, 4);
        BlockPos home = floor.above();
        LuminousMoth wild = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 2.0F, 4.5F);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));
        helper.runAfterDelay(5, () -> {
            wild.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(player.getMainHandItem().is(WildspellMobs.LUMINOUS_MOTH_BOTTLE.get()), "not bottled: holding " + player.getMainHandItem());
            helper.assertTrue(wild.isRemoved(), "bottled moth still in the world");
        });
        // Its trail light gone, the cave is dark again: release it there.
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(blockLight(helper, home) == 0, "cave not dark before release: " + blockLight(helper, home));
            BlockPos abs = helper.absolutePos(floor);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(abs).add(0.0, 0.5, 0.0), Direction.UP, abs, false)));
            helper.assertTrue(player.getMainHandItem().is(Items.GLASS_BOTTLE), "bottle not emptied: holding " + player.getMainHandItem());
        });
        helper.runAfterDelay(160, () -> {
            List<LuminousMoth> moths = helper.getEntities(WildspellMobs.LUMINOUS_MOTH.get());
            helper.assertTrue(moths.size() == 1, "expected the released moth, found " + moths.size());
            LuminousMoth moth = moths.get(0);
            helper.assertTrue(helper.absolutePos(home).equals(moth.getHome()), "released in the dark but homed at " + moth.getHome());
            helper.assertTrue(moth.isPersistenceRequired(), "released moth can despawn");
            helper.assertTrue(blockLight(helper, home) >= LuminousMoth.HOME_LIGHT - 1, "home lit only " + blockLight(helper, home));
            moth.discard();
        });
        helper.runAfterDelay(300, () -> {
            helper.assertTrue(blockLight(helper, home) == 0, "home still lit after its moth left: " + blockLight(helper, home));
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void mothReleasedInTheLightHasNoHome(GameTestHelper helper) {
        helper.setBlock(4, 1, 3, Blocks.GLOWSTONE);
        helper.runAfterDelay(10, () -> {
            LuminousMoth moth = MothBottleItem.release(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 4)),
                    new ItemStack(WildspellMobs.LUMINOUS_MOTH_BOTTLE.get()), null);
            helper.assertTrue(moth.getHome() == null, "released by glowstone but took a home");
            helper.succeed();
        });
    }

    /**
     * Lays a mossy floor, waits for a moth to settle on it, then has a player pace back and forth
     * a couple of blocks off it (sneaking or not), calling {@code check} each tick of that until it
     * says the test is done.
     */
    private static void watchAPerchedMoth(GameTestHelper helper, boolean sneaking, java.util.function.BiPredicate<LuminousMoth, Integer> check) {
        for (int x = 0; x < 9; ++x) {
            for (int z = 0; z < 9; ++z) {
                helper.setBlock(x, 0, z, Blocks.MOSS_BLOCK);
                helper.setBlock(x, 1, z, Blocks.MOSS_CARPET);
            }
        }
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 2.5F, 4.5F);
        Player player = addMockPlayer(helper, new Vec3(0.5, 1.1, 0.5));
        // Sneaking until it settles: a moth in flight flees anyone walking this close.
        player.setShiftKeyDown(true);
        int[] walked = {-1};
        helper.runAtTickTime(1590, () -> helper.fail("moth never settled; at " + helper.relativeVec(moth.position()) + " alive=" + moth.isAlive()
                + " nearby=" + helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, moth.getBoundingBox().inflate(4)).stream()
                .map(e -> e.getType().toShortString() + "@" + helper.relativeVec(e.position())).toList()));
        helper.onEachTick(() -> {
            if (walked[0] < 0) {
                if (!moth.isPerched()) {
                    return;
                }
                walked[0] = 0;
                player.setShiftKeyDown(sneaking);
            }
            // Pace along a line about two and a half blocks from where the moth sat down.
            Vec3 perch = moth.position();
            double along = Math.sin(walked[0] * 0.1) * 1.5;
            player.moveTo(perch.x + 2.5, perch.y, perch.z + along, 0.0F, 0.0F);
            if (check.test(moth, walked[0]++)) {
                succeedAndLeave(helper, player);
            }
        });
    }

    private static int blockLight(GameTestHelper helper, BlockPos relative) {
        return helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(relative));
    }

    private static int countLuminousMoss(GameTestHelper helper) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(0, 0, 0, 8, 2, 8)) {
            if (helper.getBlockState(pos).is(WildspellMobs.LUMINOUS_MOSS.get()) || helper.getBlockState(pos).is(WildspellMobs.LUMINOUS_MOSS_CARPET.get())) {
                ++count;
            }
        }
        return count;
    }

    private static void randomTick(GameTestHelper helper, BlockPos relative) {
        BlockPos pos = helper.absolutePos(relative);
        helper.getLevel().getBlockState(pos).randomTick(helper.getLevel(), pos, helper.getLevel().random);
    }

    /** Encloses (4, 1, 4) in a stone shell so no light reaches it; light needs a few ticks to settle. */
    private static void sealCave(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int y = 0; y <= 4; ++y) {
                for (int z = 2; z <= 6; ++z) {
                    boolean shell = x == 2 || x == 6 || y == 0 || y == 4 || z == 2 || z == 6;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    /**
     * Passes the test and takes its mock player out of the world. Left behind, a mock player
     * outlasts its test and draws the attention of other tests' mobs (the lich hunts players).
     */
    private static void succeedAndLeave(GameTestHelper helper, Player player) {
        player.discard();
        helper.succeed();
    }

    /** A survival-mode mock player standing in the world (not a ServerPlayer, which needs a connection). */
    private static Player addMockPlayer(GameTestHelper helper, Vec3 at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setInvulnerable(true);
        helper.getLevel().addFreshEntity(player);
        return player;
    }
}
