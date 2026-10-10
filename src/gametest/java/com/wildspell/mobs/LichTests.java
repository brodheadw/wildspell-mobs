package com.wildspell.mobs;

import com.wildspell.mobs.crypt.CryptTestAccess;
import com.wildspell.mobs.crypt.LichCryptPiece;
import com.wildspell.mobs.crypt.LichSouls;
import com.wildspell.mobs.crypt.PhylacteryBlock;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import com.wildspell.mobs.entity.IceLich;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class LichTests {
    @GameTest(template = ARENA, timeoutTicks = 700, batch = "lichReform")
    public static void boundLichReformsAtItsPhylactery(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        IceLich lich = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), null);
        UUID first = lich.getUUID();
        lich.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(lich.isRemoved(), "a bound lich stayed after being struck down");
        helper.assertEntityPresent(WildspellMobs.LICH_WISP.get());
        helper.assertItemEntityNotPresent(WildspellMobs.FROSTBOUND_STAFF.get());
        helper.succeedWhen(() -> {
            List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(liches.size() == 1, "expected the lich to re-form, found " + liches.size());
            helper.assertTrue(helper.getTick() >= PhylacteryBlockEntity.REFORM_TICKS, "re-formed too soon, at tick " + helper.getTick());
            IceLich reformed = liches.getFirst();
            helper.assertTrue(!reformed.getUUID().equals(first) && reformed.isBound(), "re-formed lich isn't a fresh, bound one");
            helper.assertTrue(reformed.getUUID().equals(phylactery.lichId()), "phylactery lost track of its lich");
            helper.assertEntityNotPresent(WildspellMobs.LICH_WISP.get());
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichCrown")
    public static void strikingDownTheLichLeavesACrownFragment(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        IceLich lich = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), null);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        lich.setHealth(1.0F);
        lich.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
        helper.assertTrue(lich.isRemoved(), "the lich wasn't struck down");
        helper.assertItemEntityPresent(WildspellMobs.CROWN_FRAGMENT.get());
        helper.assertItemEntityNotPresent(WildspellMobs.FROSTBOUND_STAFF.get());
        ItemStack fragment = helper.getEntities(EntityType.ITEM).stream().map(ItemEntity::getItem)
                .filter(stack -> stack.is(WildspellMobs.CROWN_FRAGMENT.get())).findFirst().orElseThrow();
        helper.assertTrue(phylactery.soulId().equals(fragment.get(WildspellMobs.SOUL.get())), "the fragment isn't bound to the lich's soul");
        CraftingRecipe recipe = (CraftingRecipe) helper.getLevel().getRecipeManager().byKey(WildspellMobs.id("soulseeker")).orElseThrow().value();
        ItemStack rime = new ItemStack(WildspellMobs.RIME_SHARD.get());
        ItemStack lily = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frost_lily")));
        ItemStack crystal = new ItemStack(WildspellMobs.ENCHANTED_ICE_CRYSTAL.get());
        CraftingInput grid = CraftingInput.of(3, 3, List.of(rime, fragment, rime, lily, crystal, lily, rime, lily, rime));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "the Soulseeker recipe doesn't match its own pattern");
        ItemStack seeker = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(seeker.is(WildspellMobs.SOULSEEKER.get()), "the recipe didn't make a Soulseeker");
        helper.assertTrue(phylactery.soulId().equals(seeker.get(WildspellMobs.SOUL.get())), "the Soulseeker didn't take the fragment's soul");
        player.setItemInHand(InteractionHand.MAIN_HAND, seeker);
        seeker.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var tracker = seeker.get(DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().map(t -> t.pos().equals(helper.absolutePos(new BlockPos(4, 1, 4)))).orElse(false),
                "the needle doesn't point at the phylactery: " + tracker);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichFall")
    public static void theLastFormsFallCleansesItsCrypt(GameTestHelper helper) {
        shade(helper);
        BlockPos altar = new BlockPos(4, 1, 4);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, altar, Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        BlockPos brazier = new BlockPos(4, 1, 8);
        helper.setBlock(brazier, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SNOW);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 7.5F, 1.0F, 7.5F);
        zombie.setNoAi(true);
        zombie.addTag(IceLich.MINION_TAG);
        ChunkPos center = new ChunkPos(helper.absolutePos(altar));
        forceChunks(helper, center);
        helper.destroyBlock(altar);
        helper.getEntities(EntityType.ITEM).getFirst().hurt(helper.getLevel().damageSources().lava(), 10.0F);
        helper.assertTrue(soul.burned(), "the phylactery didn't burn");
        List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
        helper.assertTrue(liches.size() == 1 && liches.getFirst().isLastForm() && !liches.getFirst().isBound() && liches.getFirst().isEnraged(),
                "burning the phylactery should raise one last form, mortal and enraged");
        IceLich last = liches.getFirst();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        last.setHealth(1.0F);
        last.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(last.isDeadOrDying(), "the last form survived");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, last.getBoundingBox().inflate(16.0),
                    i -> i.getItem().is(WildspellMobs.FROSTBOUND_STAFF.get())).isEmpty(), "no staff; items near: " + helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class, last.getBoundingBox().inflate(16.0)).stream().map(i -> i.getItem() + "@" + helper.relativeVec(i.position())).toList()
                    + " last form at " + helper.relativeVec(last.position()) + " lastHurtByPlayer=" + last.getLastHurtByMob());
            helper.assertTrue(!zombie.isAlive(), "the dead it raised didn't crumble");
            helper.assertBlockPresent(Blocks.CHEST, altar);
            helper.assertTrue(helper.getBlockState(brazier).is(Blocks.CAMPFIRE) && helper.getBlockState(brazier).getValue(CampfireBlock.LIT),
                    "the soul-fire brazier didn't turn to ordinary fire");
            helper.assertBlockNotPresent(Blocks.SNOW, new BlockPos(1, 1, 1));
            helper.assertTrue(LichSouls.isCleansedZone(helper.getLevel(), helper.absolutePos(altar)), "the crypt isn't a safe zone");
            helper.assertTrue(LichSouls.get(helper.getLevel()).soul(soul.id) == null, "a cleansed soul is still on record");
        });
    }

    private static void forceChunks(GameTestHelper helper, ChunkPos center) {
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                helper.getLevel().setChunkForced(center.x + dx, center.z + dz, true);
            }
        }
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichVoid")
    public static void aPhylacteryLostToTheVoidFindsRoomAboveItsAltar(GameTestHelper helper) {
        shade(helper);
        BlockPos altar = new BlockPos(4, 1, 4);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, altar, Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        helper.destroyBlock(altar);
        helper.setBlock(altar, Blocks.STONE);
        ItemEntity item = helper.getEntities(EntityType.ITEM).getFirst();
        item.setNoGravity(true);
        item.setPos(item.getX(), helper.getLevel().getMinBuildHeight() - 2, item.getZ());
        helper.succeedWhen(() -> {
            helper.assertTrue(item.isRemoved(), "the phylactery is still in the void");
            helper.assertBlockPresent(WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get(), altar.above());
            helper.assertTrue(soul.inAltar(), "its soul doesn't know it's home");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "cryptWakes")
    public static void cryptWakesForAnIntruder(GameTestHelper helper) {
        shade(helper);
        placePhylactery(helper, new BlockPos(4, 1, 1), Direction.SOUTH);
        BlockPos[] braziers = {new BlockPos(1, 1, 7), new BlockPos(7, 1, 7)};
        for (BlockPos b : braziers) {
            helper.setBlock(b, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        }
        BlockPos candle = new BlockPos(4, 1, 8);
        helper.setBlock(candle, Blocks.BLUE_CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 4));
        Player intruder = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        helper.succeedWhen(() -> {
            for (BlockPos b : braziers) {
                helper.assertTrue(helper.getBlockState(b).getValue(CampfireBlock.LIT), "brazier at " + b + " not lit");
            }
            helper.assertTrue(helper.getBlockState(candle).getValue(CandleBlock.LIT), "candle not lit");
            List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(liches.size() == 1, "the lich didn't rise to meet the intruder");
            helper.assertTrue(liches.getFirst().isBound() && liches.getFirst().getTarget() == intruder, "risen lich isn't bound and after the intruder");
            intruder.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 1000, batch = "cryptSleeps")
    public static void anAwakeCryptSleepsOnceEveryoneIsGone(GameTestHelper helper) {
        shade(helper);
        placePhylactery(helper, new BlockPos(4, 1, 1), Direction.SOUTH);
        Player intruder = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        PhylacteryBlockEntity crypt = helper.getBlockEntity(new BlockPos(4, 1, 1));
        boolean[] woke = {false};
        helper.succeedWhen(() -> {
            if (crypt.isAwake() && !woke[0]) {
                woke[0] = true;
                intruder.discard();
            }
            helper.assertTrue(woke[0] && !crypt.isAwake(), "the crypt is still awake");
            helper.killAllEntities();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichAmbush")
    public static void lichAmbushesAPlayerNearItsCrypt(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 0), Direction.NORTH);
        Player player = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        double chance = MobsConfig.LICH_AMBUSH_CHANCE.get();
        MobsConfig.LICH_AMBUSH_CHANCE.set(1.0);
        onFinish(helper, () -> {
            MobsConfig.LICH_AMBUSH_CHANCE.set(chance);
            player.discard();
        });
        // The roll runs over level.players(), which a mock player is not in: hand it the player.
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), List.of(phylactery.soul(helper.getLevel())), List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            List<IceLich> liches = helper.getLevel().getEntitiesOfClass(IceLich.class, player.getBoundingBox().inflate(24.0));
            helper.assertTrue(liches.size() == 1, "no ambush yet");
            IceLich lich = liches.getFirst();
            helper.assertTrue(lich.isBound() && lich.getTarget() == player && lich.getUUID().equals(phylactery.lichId()), "ambusher isn't the phylactery's lich hunting the player");
            helper.assertTrue(lich.distanceTo(player) >= 9.0, "rose right on top of the player: " + lich.distanceTo(player));
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichBodyAway")
    public static void lichIdleInAnUnloadedCryptStillStalks(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 0), Direction.NORTH);
        Player player = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich old = soul.raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 1.0, 1.5)), null);
        helper.assertTrue(old != null && old.getUUID().equals(phylactery.lichId()), "the first body is on record");
        old.setNoAi(true);
        double chance = MobsConfig.LICH_AMBUSH_CHANCE.get();
        MobsConfig.LICH_AMBUSH_CHANCE.set(1.0);
        CryptTestAccess.assumeBodyAway(true);
        onFinish(helper, () -> {
            MobsConfig.LICH_AMBUSH_CHANCE.set(chance);
            CryptTestAccess.assumeBodyAway(false);
            player.discard();
        });
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), List.of(soul), List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            UUID now = phylactery.lichId();
            helper.assertTrue(now != null && !now.equals(old.getUUID()), "no fresh body has risen");
            helper.assertTrue(helper.getLevel().getEntity(now) instanceof IceLich fresh && fresh.getTarget() == player, "the fresh body isn't hunting the player");
            helper.assertFalse(old.isAlive(), "the old body should have vanished as a stale copy");
            helper.killAllEntities();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "cryptGen")
    public static void cryptBuildsTheSameWayRoundInEveryOrientation(GameTestHelper helper) {
        int i = 0;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos origin = helper.absolutePos(new BlockPos(1000 + 60 * i++, 0, 1000));
            LichCryptPiece piece = new LichCryptPiece(origin, facing, 6);
            piece.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                    helper.getLevel().random, piece.getBoundingBox(), new ChunkPos(origin), origin);
            BlockPos at = piece.phylacteryPos();
            helper.assertTrue(helper.getLevel().getBlockEntity(at) instanceof PhylacteryBlockEntity, facing + ": no phylactery at " + at);
            PhylacteryBlockEntity phylactery = (PhylacteryBlockEntity) helper.getLevel().getBlockEntity(at);
            AABB crypt = phylactery.cryptBounds();
            onFinish(helper, () -> CryptTestAccess.forgetWithin(helper.getLevel(), crypt.inflate(2.0)));
            int wards = 0, braziers = 0, candles = 0, iceFloor = 0;
            for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(crypt.minX, crypt.minY, crypt.minZ), BlockPos.containing(crypt.maxX - 1, crypt.maxY - 1, crypt.maxZ - 1))) {
                BlockState state = helper.getLevel().getBlockState(p);
                wards += state.is(WildspellMobs.RIME_WARD.get()) ? 1 : 0;
                braziers += state.is(Blocks.SOUL_CAMPFIRE) && !state.getValue(CampfireBlock.LIT) ? 1 : 0;
                candles += state.is(Blocks.BLUE_CANDLE) && !state.getValue(CandleBlock.LIT) ? 1 : 0;
                iceFloor += p.getY() == at.getY() - PhylacteryBlockEntity.CRYPT_BELOW && state.is(BlockTags.ICE) ? 1 : 0;
            }
            helper.assertTrue(wards == 4 && braziers == 8 && candles == 12,
                    facing + ": crypt bounds hold " + wards + " wards, " + braziers + " unlit braziers and " + candles + " unlit candles");
            helper.assertTrue(iceFloor == 0, facing + ": " + iceFloor + " ice blocks in the floor");
            helper.assertTrue(helper.getLevel().isEmptyBlock(at.above()) && helper.getLevel().isEmptyBlock(at.above(3)), facing + ": no room for the lich over the altar");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600, batch = "lichFight")
    public static void lichFightsWithVolleysMinionsAndBursts(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
        pig.setHealth(1000.0F);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 4.0F, 1.5F);
        lich.setTarget(pig);
        lich.setHealth(lich.getMaxHealth() * 0.4F);
        boolean[] saw = new boolean[2];
        helper.onEachTick(() -> {
            if (lich.getTarget() != pig) {
                lich.setTarget(pig);
            }
            saw[0] |= !helper.getEntities(WildspellMobs.FROST_SHARD.get()).isEmpty();
            saw[1] |= helper.getLevel().getEntitiesOfClass(Mob.class, lich.getBoundingBox().inflate(32),
                    lich::isOwnMinion).size() > 0;
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(saw[0], "lich never fired a volley");
            helper.assertTrue(saw[1], "lich never raised minions");
            helper.assertTrue(pig.getTicksFrozen() > 0 && pig.getHealth() < 1000.0F, "nothing hurt and froze the target");
            helper.assertTrue(lich.isEnraged(), "lich not enraged below half health");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "lichDeath")
    public static void lichMinionsShatterWhenItDies(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 4.0F, 1.5F);
        lich.setTarget(pig);
        helper.succeedWhen(() -> {
            List<Mob> minions = helper.getLevel().getEntitiesOfClass(Mob.class,
                    lich.getBoundingBox().inflate(48), lich::isOwnMinion);
            helper.assertTrue(!minions.isEmpty(), "no minions yet");
            lich.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(!lich.isAlive(), "lich survived");
            helper.assertTrue(minions.stream().noneMatch(Mob::isAlive), "minions outlived the lich");
        });
    }

    private static PhylacteryBlockEntity placePhylactery(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get().defaultBlockState().setValue(PhylacteryBlock.FACING, facing));
        AABB area = helper.getBounds().inflate(2.0);
        onFinish(helper, () -> CryptTestAccess.forgetWithin(helper.getLevel(), area));
        return (PhylacteryBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
}
