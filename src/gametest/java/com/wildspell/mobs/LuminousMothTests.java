package com.wildspell.mobs;

import com.wildspell.mobs.entity.LuminousMoth;
import java.util.List;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
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

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class LuminousMothTests {
    private static final ResourceKey<Biome> LUSH_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves"));

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothPerch")
    public static void mothLandsOnPlantsAndBrightensMoss(GameTestHelper helper) {
        for (int x = -2; x <= 10; ++x) {
            for (int z = -2; z <= 10; ++z) {
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

    @GameTest(template = ARENA, timeoutTicks = 1600, batch = "mothSneak")
    public static void sneakingPastLeavesAPerchedMothBe(GameTestHelper helper) {
        watchAPerchedMoth(helper, true, (moth, walkedTicks) -> {
            helper.assertTrue(moth.isPerched(), "a sneaking player flushed the moth after " + walkedTicks + " ticks");
            return walkedTicks >= 80;
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

    private static void watchAPerchedMoth(GameTestHelper helper, boolean sneaking, BiPredicate<LuminousMoth, Integer> check) {
        for (int x = 0; x < 9; ++x) {
            for (int z = 0; z < 9; ++z) {
                helper.setBlock(x, 0, z, Blocks.MOSS_BLOCK);
                helper.setBlock(x, 1, z, Blocks.MOSS_CARPET);
            }
        }
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 4.5F, 2.5F, 4.5F);
        Player player = addMockPlayer(helper, new Vec3(0.5, 1.1, 0.5));
        player.setShiftKeyDown(true);
        int[] walked = {-1};
        helper.runAtTickTime(1590, () -> helper.fail("moth never settled; at " + helper.relativeVec(moth.position()) + " alive=" + moth.isAlive()
                + " nearby=" + helper.getLevel().getEntitiesOfClass(LivingEntity.class, moth.getBoundingBox().inflate(4)).stream()
                .map(e -> e.getType().toShortString() + "@" + helper.relativeVec(e.position())).toList()));
        helper.onEachTick(() -> {
            if (walked[0] < 0) {
                if (!moth.isPerched()) {
                    return;
                }
                walked[0] = 0;
                player.setShiftKeyDown(sneaking);
            }
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
        for (BlockPos pos : BlockPos.betweenClosed(-2, 0, -2, 10, 2, 10)) {
            if (helper.getBlockState(pos).is(WildspellMobs.LUMINOUS_MOSS.get()) || helper.getBlockState(pos).is(WildspellMobs.LUMINOUS_MOSS_CARPET.get())) {
                ++count;
            }
        }
        return count;
    }

    private static void succeedAndLeave(GameTestHelper helper, Player player) {
        player.discard();
        helper.succeed();
    }
}
