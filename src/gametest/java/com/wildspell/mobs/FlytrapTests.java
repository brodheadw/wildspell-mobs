package com.wildspell.mobs;

import com.wildspell.mobs.entity.FlytrapHead;
import com.wildspell.mobs.entity.LuminousMoth;
import com.wildspell.mobs.flytrap.FlytrapBlock;
import com.wildspell.mobs.flytrap.FlytrapStemBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

// Heads bite anything moving nearby, so every test that grows one gets a batch of its own.
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class FlytrapTests {
    private static final String ARENA = "arena";
    private static final ResourceKey<Biome> LUSH_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("lush_caves"));
    private static final ResourceKey<Biome> JUNGLE = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("jungle"));
    private static final BlockPos PLANT = new BlockPos(4, 1, 4);

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapGrow")
    public static void flytrapGrowsThroughItsStagesAndHeads(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 0);
        BlockPos roofed = new BlockPos(1, 1, 1);
        plant(helper, roofed, 1);
        helper.setBlock(roofed.above(2), Blocks.STONE);
        helper.runAfterDelay(2, () -> {
            assertHeads(helper, PLANT, FlytrapHead.SIZE_SMALL);
            boneMealUntil(helper, PLANT, 1);
        });
        helper.runAfterDelay(4, () -> {
            assertHeads(helper, PLANT, FlytrapHead.SIZE_MEDIUM);
            boneMealUntil(helper, PLANT, 2);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertBlockProperty(PLANT.above(), FlytrapStemBlock.BRANCHES, true);
            helper.assertBlockProperty(PLANT.above(2), FlytrapStemBlock.BRANCHES, false);
            assertHeads(helper, PLANT, FlytrapHead.SIZE_BIG, FlytrapHead.SIZE_MEDIUM, FlytrapHead.SIZE_MEDIUM);
            FlytrapHead top = heads(helper, PLANT).stream().filter(h -> h.getSlot() == 0).findFirst().orElseThrow();
            helper.assertTrue(helper.absolutePos(PLANT).above(3).equals(top.blockPosition()), "top head isn't on top of the stem: " + top.blockPosition());
            BlockState grown = helper.getBlockState(PLANT);
            helper.assertFalse(FlytrapBlock.canGrow(helper.getLevel(), helper.absolutePos(PLANT), grown), "a grown flytrap can grow further");
            helper.assertFalse(FlytrapBlock.canGrow(helper.getLevel(), helper.absolutePos(roofed), helper.getBlockState(roofed)),
                    "a flytrap grew tall under a roof");
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapMagic")
    public static void placedAtAnyStageAndWitheredToAir(GameTestHelper helper) {
        lawn(helper);
        Block block = BuiltInRegistries.BLOCK.get(WildspellMobs.id("flytrap"));
        IntegerProperty age = (IntegerProperty) block.getStateDefinition().getProperty("age");
        helper.assertTrue(block.defaultBlockState().is(FlytrapBlock.HOSTILE_GROWTH), "flytrap isn't tagged wildspellmobs:hostile_growth");
        helper.assertTrue(WildspellMobs.FLYTRAP_STEM.get().defaultBlockState().is(FlytrapBlock.HOSTILE_GROWTH), "flytrap stem isn't tagged");
        helper.assertTrue(BuiltInRegistries.ENTITY_TYPE.get(WildspellMobs.id("flytrap_head")).is(FlytrapHead.HOSTILE_GROWTH),
                "flytrap_head isn't tagged wildspellmobs:hostile_growth");
        BlockPos[] spots = {new BlockPos(1, 1, 4), new BlockPos(4, 1, 4), new BlockPos(7, 1, 4)};
        for (int stage = 0; stage <= 2; ++stage) {
            helper.setBlock(spots[stage], block.defaultBlockState().setValue(age, stage));
        }
        helper.runAfterDelay(3, () -> {
            assertHeads(helper, spots[0], FlytrapHead.SIZE_SMALL);
            assertHeads(helper, spots[1], FlytrapHead.SIZE_MEDIUM);
            assertHeads(helper, spots[2], FlytrapHead.SIZE_BIG, FlytrapHead.SIZE_MEDIUM, FlytrapHead.SIZE_MEDIUM);
            helper.assertBlockPresent(WildspellMobs.FLYTRAP_STEM.get(), spots[2].above(2));
            for (BlockPos spot : spots) {
                helper.setBlock(spot, Blocks.AIR);
            }
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(helper.getEntities(WildspellMobs.FLYTRAP_HEAD.get()).isEmpty(), "heads outlived their withered plants");
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP_STEM.get(), spots[2].above());
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP_STEM.get(), spots[2].above(2));
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "withering a flytrap dropped something");
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "flytrapSprout")
    public static void sproutSnapsAtAMothNotAPlayer(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 0);
        LuminousMoth moth = helper.spawn(WildspellMobs.LUMINOUS_MOTH.get(), 5.3F, 1.2F, 4.5F);
        moth.setNoAi(true);
        Player player = WildspellMobsTests.addMockPlayer(helper, new Vec3(4.5, 1.0, 5.4));
        int[] tick = {0};
        helper.onEachTick(() -> {
            double sway = (tick[0]++ / 4) % 2 == 0 ? 0.0 : 0.3;
            place(helper, player, new Vec3(4.5 + sway, 1.0, 5.4));
            if (moth.isAlive() && moth.getHealth() == moth.getMaxHealth()) {
                Vec3 at = helper.absoluteVec(new Vec3(5.3, 1.2 + sway, 4.5));
                moth.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            }
            for (FlytrapHead head : heads(helper, PLANT)) {
                helper.assertFalse(head.getTarget() == player || head.isPrey(player), "a sprout went for a player");
            }
        });
        helper.succeedWhen(() -> {
            FlytrapHead head = head(helper, PLANT);
            helper.assertTrue(head.isMoving(player), "the player isn't seen moving: the test isn't testing the player");
            helper.assertTrue(!moth.isAlive() || moth.getHealth() < moth.getMaxHealth(), "the sprout never bit the moth");
            player.discard();
            clearPlants(helper);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "flytrapBite")
    public static void headBitesAndHoldsAMovingMobInReach(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 1);
        Pig pig = helper.spawn(EntityType.PIG, 2.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        Vec3 home = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        int[] tick = {0};
        helper.onEachTick(() -> {
            if (pig.getHealth() == pig.getMaxHealth()) {
                Vec3 at = helper.absoluteVec(new Vec3((tick[0]++ / 4) % 2 == 0 ? 2.5 : 2.9, 1.0, 4.5));
                pig.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
            }
            heads(helper, PLANT).forEach(head -> assertAt(helper, head, home));
        });
        helper.succeedWhen(() -> {
            FlytrapHead head = head(helper, PLANT);
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "the flytrap never bit the pig; action=" + head.getAction());
            helper.assertTrue(head.getHeld() == pig, "the flytrap bit the pig but isn't holding it");
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the held pig isn't slowed");
            helper.assertTrue(head.getAction() == FlytrapHead.ACTION_HOLD, "not in the hold pose");
            clearPlants(helper);
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapSneak")
    public static void sneakingOrStandingStillSlipsPast(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 1);
        plant(helper, new BlockPos(1, 1, 1), 1);
        Player sneaker = WildspellMobsTests.addMockPlayer(helper, new Vec3(2.5, 1.0, 4.5));
        sneaker.setShiftKeyDown(true);
        Player still = WildspellMobsTests.addMockPlayer(helper, new Vec3(4.5, 1.0, 2.5));
        Player walker = WildspellMobsTests.addMockPlayer(helper, new Vec3(6.5, 1.0, 4.5));
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 6.5F);
        pig.setNoAi(true);
        int[] tick = {0};
        helper.onEachTick(() -> {
            double sway = (tick[0]++ / 4) % 2 == 0 ? 0.0 : 0.4;
            place(helper, sneaker, new Vec3(2.5, 1.0, 4.5 + sway));
            place(helper, walker, new Vec3(6.5, 1.0, 4.5 + sway));
            place(helper, still, new Vec3(4.5, 1.0, 2.5));
        });
        helper.runAfterDelay(30, () -> {
            FlytrapHead head = head(helper, PLANT);
            helper.assertTrue(head.isMoving(sneaker), "the sneaking player isn't seen moving: the test isn't testing the sneak");
            helper.assertFalse(head.isPrey(sneaker), "a sneaking player is prey");
            helper.assertFalse(head.isMoving(still) || head.isPrey(still), "a player standing still is prey");
            helper.assertFalse(head.isPrey(pig), "a still pig is prey");
            helper.assertTrue(head.isPrey(walker), "a player walking two blocks off isn't prey");
            helper.assertFalse(head.isPrey(head(helper, new BlockPos(1, 1, 1))), "a flytrap is prey to a flytrap");
            sneaker.discard();
            still.discard();
            walker.discard();
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapShears")
    public static void shearsCutAHeldVictimFree(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 1);
        Pig pig = helper.spawn(EntityType.PIG, 3.0F, 1.0F, 4.5F);
        pig.setNoAi(true);
        helper.runAfterDelay(2, () -> head(helper, PLANT).seize(pig));
        helper.runAfterDelay(6, () -> {
            FlytrapHead head = head(helper, PLANT);
            helper.assertTrue(head.getHeld() == pig && pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the flytrap isn't holding the pig");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
            InteractionResult result = head.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.consumesAction(), "shears did nothing: " + result);
            helper.assertTrue(head.getHeld() == null, "the pig is still held");
            helper.assertFalse(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the freed pig is still slowed");
            helper.assertTrue(player.getMainHandItem().getDamageValue() == 1, "cutting didn't wear the shears");
        });
        helper.runAfterDelay(34, () -> {
            FlytrapHead head = head(helper, PLANT);
            helper.assertTrue(head.getHeld() == null && head.getAction() == FlytrapHead.ACTION_IDLE, "the flytrap took the pig back");
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapFire")
    public static void fireBurnsAHeadHarder(GameTestHelper helper) {
        lawn(helper);
        BlockPos burntAt = new BlockPos(2, 1, 4);
        BlockPos cutAt = new BlockPos(6, 1, 4);
        plant(helper, burntAt, 1);
        plant(helper, cutAt, 1);
        helper.runAfterDelay(2, () -> {
            FlytrapHead burnt = head(helper, burntAt);
            FlytrapHead cut = head(helper, cutAt);
            burnt.setNoAi(true);
            cut.setNoAi(true);
            burnt.hurt(helper.getLevel().damageSources().inFire(), 3.0F);
            cut.hurt(helper.getLevel().damageSources().generic(), 3.0F);
            float burntLoss = burnt.getMaxHealth() - burnt.getHealth();
            float cutLoss = cut.getMaxHealth() - cut.getHealth();
            helper.assertTrue(cutLoss == 3.0F, "a plain hit did " + cutLoss);
            helper.assertTrue(burntLoss == 6.0F, "fire did " + burntLoss + ", not double");
            helper.assertTrue(burnt.isOnFire(), "a touch of fire didn't set it alight");
            helper.assertTrue(cut.getMaxHealth() == 20.0F, "a young plant's head has " + cut.getMaxHealth() + " health");
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapRooted")
    public static void headsKeepTheirPlaceOnThePlant(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 1);
        Vec3 home = helper.absoluteVec(new Vec3(4.5, 2.0, 4.5));
        helper.runAfterDelay(3, () -> {
            FlytrapHead head = head(helper, PLANT);
            head.push(1.0, 0.5, 1.0);
            head.knockback(2.0, 1.0, 1.0);
            head.setDeltaMovement(0.8, 0.6, -0.8);
            helper.spawn(EntityType.PIG, 4.5F, 2.0F, 4.5F);
        });
        helper.runAfterDelay(40, () -> {
            List<FlytrapHead> heads = heads(helper, PLANT);
            helper.assertTrue(heads.size() == 1, "the plant has " + heads.size() + " heads");
            assertAt(helper, heads.getFirst(), home);
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapKill")
    public static void killingAHeadBreaksItsWholePlant(GameTestHelper helper) {
        lawn(helper);
        plant(helper, PLANT, 2);
        BlockPos young = new BlockPos(1, 1, 7);
        plant(helper, young, 1);
        helper.runAfterDelay(3, () -> {
            FlytrapHead side = heads(helper, PLANT).stream().filter(h -> h.getSlot() == 1).findFirst().orElseThrow();
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            side.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
            helper.assertTrue(side.isDeadOrDying(), "the side head didn't die");
            head(helper, young).kill();
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP.get(), PLANT);
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP.get(), young);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP_STEM.get(), PLANT.above());
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP_STEM.get(), PLANT.above(2));
            helper.assertTrue(helper.getEntities(WildspellMobs.FLYTRAP_HEAD.get()).stream().allMatch(FlytrapHead::isDeadOrDying),
                    "a living head outlived its plant");
            helper.assertTrue(count(helper, WildspellMobs.FLYTRAP_SPROUT.get()) == 2, "sprouts: " + count(helper, WildspellMobs.FLYTRAP_SPROUT.get()));
            helper.assertTrue(count(helper, WildspellMobs.TRAP_JAW.get()) == 3, "trap jaws: " + count(helper, WildspellMobs.TRAP_JAW.get()));
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapBreak")
    public static void breakingAFlytrapDropsItsSproutAndJaws(GameTestHelper helper) {
        lawn(helper);
        BlockPos sprout = new BlockPos(1, 1, 4);
        BlockPos young = new BlockPos(4, 1, 1);
        BlockPos grown = new BlockPos(6, 1, 6);
        plant(helper, sprout, 0);
        plant(helper, young, 1);
        plant(helper, grown, 2);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(helper.getEntities(WildspellMobs.FLYTRAP_HEAD.get()).size() == 5, "expected 5 heads");
            breakBlock(helper, sprout);
            breakBlock(helper, young);
            breakBlock(helper, grown.above(2));
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP.get(), grown);
            helper.assertBlockNotPresent(WildspellMobs.FLYTRAP_STEM.get(), grown.above());
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(helper.getEntities(WildspellMobs.FLYTRAP_HEAD.get()).isEmpty(), "heads outlived their broken plants");
            helper.assertTrue(count(helper, WildspellMobs.FLYTRAP_SPROUT.get()) == 3, "sprouts: " + count(helper, WildspellMobs.FLYTRAP_SPROUT.get()));
            helper.assertTrue(count(helper, WildspellMobs.TRAP_JAW.get()) == 3, "trap jaws: " + count(helper, WildspellMobs.TRAP_JAW.get()));
            clearPlants(helper);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void flytrapsRootInGrassMossAndMudOnly(GameTestHelper helper) {
        BlockState flytrap = WildspellMobs.FLYTRAP.get().defaultBlockState();
        BlockPos spot = new BlockPos(4, 2, 4);
        for (Block ground : new Block[] {Blocks.GRASS_BLOCK, Blocks.MOSS_BLOCK, Blocks.MUD, Blocks.STONE, Blocks.AIR}) {
            helper.setBlock(spot.below(), ground);
            boolean roots = ground != Blocks.STONE && ground != Blocks.AIR;
            helper.assertTrue(flytrap.canSurvive(helper.getLevel(), helper.absolutePos(spot)) == roots, "flytrap rooting in " + ground + ": " + !roots);
        }
        clearPlants(helper);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void junglesAndLushCavesGrowFlytraps(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        assertGrows(helper, biomes.get(JUNGLE), "flytrap_patch_jungle");
        assertGrows(helper, biomes.get(LUSH_CAVES), "flytrap_patch_lush_caves");
        clearPlants(helper);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "flytrapPatch")
    public static void patchRootsFlytrapsThatGrowTheirHeads(GameTestHelper helper) {
        lawn(helper);
        ConfiguredFeature<?, ?> patch = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(WildspellMobs.id("flytrap_patch"));
        helper.assertTrue(patch != null && patch.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(),
                RandomSource.create(7), helper.absolutePos(PLANT)), "the patch placed nothing");
        helper.runAfterDelay(3, () -> {
            int plants = 0;
            for (int x = 0; x < 9; ++x) {
                for (int z = 0; z < 9; ++z) {
                    BlockPos pos = new BlockPos(x, 1, z);
                    BlockState state = helper.getBlockState(pos);
                    if (!state.is(WildspellMobs.FLYTRAP.get())) {
                        continue;
                    }
                    ++plants;
                    int age = state.getValue(FlytrapBlock.AGE);
                    helper.assertTrue(heads(helper, pos).size() == FlytrapBlock.slots(age).size(), "a stage " + age + " flytrap at " + pos + " lacks heads");
                    if (age == FlytrapBlock.MAX_AGE) {
                        helper.assertBlockPresent(WildspellMobs.FLYTRAP_STEM.get(), pos.above(2));
                    }
                }
            }
            helper.assertTrue(plants >= 1, "no flytraps in the patch");
                        clearPlants(helper);
                        helper.succeed();
        });
    }

    private static void clearPlants(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(-12, -2, -12)), helper.absolutePos(new BlockPos(20, 8, 20)))) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(WildspellMobs.FLYTRAP.get()) || state.is(WildspellMobs.FLYTRAP_STEM.get())) {
                helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void assertGrows(GameTestHelper helper, Biome biome, String feature) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, WildspellMobs.id(feature));
        boolean listed = biome.getGenerationSettings().features().stream().anyMatch(step -> step.stream().anyMatch(holder -> holder.is(key)));
        helper.assertTrue(listed, feature + " missing from its biome's features");
    }

    private static void plant(GameTestHelper helper, BlockPos pos, int age) {
        helper.setBlock(pos, WildspellMobs.FLYTRAP.get().defaultBlockState().setValue(FlytrapBlock.AGE, age));
    }

    private static List<FlytrapHead> heads(GameTestHelper helper, BlockPos plant) {
        return FlytrapBlock.headsOf(helper.getLevel(), helper.absolutePos(plant));
    }

    private static FlytrapHead head(GameTestHelper helper, BlockPos plant) {
        List<FlytrapHead> heads = heads(helper, plant);
        helper.assertFalse(heads.isEmpty(), "the plant at " + plant + " has no head");
        return heads.getFirst();
    }

    private static void assertHeads(GameTestHelper helper, BlockPos plant, int... sizes) {
        List<FlytrapHead> heads = heads(helper, plant);
        helper.assertTrue(heads.size() == sizes.length, "a stage " + helper.getBlockState(plant).getValue(FlytrapBlock.AGE) + " plant has "
                + heads.size() + " heads, not " + sizes.length);
        double[] reach = {1.5, 3.0, 4.0};
        for (FlytrapHead head : heads) {
            int size = sizes[head.getSlot()];
            helper.assertTrue(head.getSize() == size, "head " + head.getSlot() + " is size " + head.getSize() + ", not " + size);
            helper.assertTrue(head.getReach() == reach[size], "head " + head.getSlot() + " reaches " + head.getReach());
        }
    }

    private static void assertAt(GameTestHelper helper, FlytrapHead head, Vec3 home) {
        double drift = head.position().distanceTo(home);
        helper.assertTrue(drift < 1.0E-6, "the head moved " + drift + " blocks off its plant");
    }

    private static void boneMealUntil(GameTestHelper helper, BlockPos plant, int age) {
        BlockPos pos = helper.absolutePos(plant);
        for (int i = 0; i < 100 && helper.getBlockState(plant).getValue(FlytrapBlock.AGE) < age; ++i) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), helper.getLevel(), pos);
        }
        helper.assertBlockProperty(plant, FlytrapBlock.AGE, age);
    }

    private static void breakBlock(GameTestHelper helper, BlockPos pos) {
        helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
    }

    private static int count(GameTestHelper helper, Item item) {
        return helper.getEntities(EntityType.ITEM).stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static void lawn(GameTestHelper helper) {
        for (int x = 0; x < 9; ++x) {
            for (int z = 0; z < 9; ++z) {
                helper.setBlock(x, 0, z, Blocks.GRASS_BLOCK);
            }
        }
    }

    private static void place(GameTestHelper helper, Player player, Vec3 at) {
        Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
    }
}
