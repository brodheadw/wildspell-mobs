package com.wildspell.mobs;

import com.wildspell.mobs.entity.ColdEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestListener;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

final class GameTests {
    static final String ARENA = "arena";
    static final String SKY = "sky_arena";
    static final ResourceKey<Biome> FROSTED_CAVES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frosted_caves"));

    private GameTests() {
    }

    static Player addMockPlayer(GameTestHelper helper, Vec3 at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setInvulnerable(true);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    static void onFinish(GameTestHelper helper, Runnable cleanup) {
        helper.testInfo.addListener(new GameTestListener() {
            @Override
            public void testStructureLoaded(GameTestInfo info) {
            }

            @Override
            public void testPassed(GameTestInfo info, GameTestRunner runner) {
                cleanup.run();
            }

            @Override
            public void testFailed(GameTestInfo info, GameTestRunner runner) {
                cleanup.run();
            }

            @Override
            public void testAddedForRerun(GameTestInfo oldInfo, GameTestInfo newInfo, GameTestRunner runner) {
            }
        });
    }

    static void shade(GameTestHelper helper) {
        for (int x = -1; x <= 9; ++x) {
            for (int z = -1; z <= 9; ++z) {
                helper.setBlock(x, 12, z, Blocks.STONE);
            }
        }
    }

    static void openToTheSky(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int z = 2; z <= 6; ++z) {
                for (int y = 6; y <= 9; ++y) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    // getBiome jitters into neighbouring cells and fillbiome can fail before chunks load: paint a margin, every tick.
    static void paintFrostedCaves(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(new BlockPos(-2, -2, -2));
        BlockPos to = helper.absolutePos(new BlockPos(10, 6, 10));
        String fill = "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + FROSTED_CAVES.location();
        helper.onEachTick(() -> {
            if (!helper.getLevel().getBiome(helper.absolutePos(new BlockPos(4, 1, 4))).is(ColdEffects.COLD_CAVES)) {
                helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), fill);
            }
        });
    }

    static void whenSealed(GameTestHelper helper, BlockPos spot, Runnable checks) {
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (!done[0] && helper.getLevel().getBrightness(LightLayer.SKY, helper.absolutePos(spot)) == 0) {
                done[0] = true;
                checks.run();
            }
        });
    }

    static void sealCave(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int y = 0; y <= 4; ++y) {
                for (int z = 2; z <= 6; ++z) {
                    boolean shell = x == 2 || x == 6 || y == 0 || y == 4 || z == 2 || z == 6;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }
}
