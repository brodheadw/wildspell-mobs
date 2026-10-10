package com.wildspell.mobs;

import com.wildspell.mobs.entity.FlytrapHead;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

// Heads bite anything moving nearby, so every test that grows one gets a batch of its own.
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class FlytrapTests {
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
        Player sneaker = addMockPlayer(helper, new Vec3(2.5, 1.0, 4.5));
        sneaker.setShiftKeyDown(true);
        Player still = addMockPlayer(helper, new Vec3(4.5, 1.0, 2.5));
        Player walker = addMockPlayer(helper, new Vec3(6.5, 1.0, 4.5));
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

    private static void clearPlants(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(-12, -2, -12)), helper.absolutePos(new BlockPos(20, 8, 20)))) {
            BlockState state = helper.getLevel().getBlockState(pos);
            if (state.is(WildspellMobs.FLYTRAP.get()) || state.is(WildspellMobs.FLYTRAP_STEM.get())) {
                helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
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
