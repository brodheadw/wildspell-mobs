package com.wildspell.mobs;

import com.wildspell.mobs.entity.Watcher;
import com.wildspell.mobs.entity.WatcherTestAccess;
import com.wildspell.mobs.watch.Face;
import com.wildspell.mobs.watch.Watchers;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class WatcherTests {
    private static final Vec3 QUARRY = new Vec3(15.5, 1.0, 15.5);
    private static final Vec3 ACROSS = new Vec3(15.5, 1.5, 22.5);

    private static Player knower(GameTestHelper helper, float knowing) {
        for (int x = 0; x < 31; ++x) {
            for (int z = 0; z < 31; ++z) {
                helper.setBlock(x, 0, z, Blocks.STONE);
            }
        }
        Player player = addMockPlayer(helper, QUARRY);
        player.getPersistentData().putFloat(Watchers.KNOWING, knowing);
        onFinish(helper, player::discard);
        return player;
    }

    private static Watcher watcher(GameTestHelper helper, Face face, Player quarry) {
        Watcher watcher = helper.spawn(WildspellMobs.WATCHERS.get(face).get(), ACROSS);
        watcher.comeFor(quarry, Watchers.knowing(quarry));
        return watcher;
    }

    private static void face(Player player, Vec3 at) {
        Vec3 to = at.subtract(player.getEyePosition());
        player.setYRot((float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI));
        player.setXRot(0.0F);
        player.yHeadRot = player.getYRot();
    }

    @GameTest(template = SKY, batch = "watchers")
    public static void onlyAKnowingMindIsNoticed(GameTestHelper helper) {
        Player player = knower(helper, 0.0F);
        helper.assertFalse(Watchers.noticeable(player), "a player who knows nothing was noticed");
        player.getPersistentData().putFloat(Watchers.KNOWING, 0.7F);
        helper.assertTrue(Watchers.noticeable(player), "a knowing player went unnoticed");
        player.getPersistentData().putLong(Watchers.UNSEEN_UNTIL, helper.getLevel().getGameTime() + 100L);
        helper.assertFalse(Watchers.noticeable(player), "a player Magic hid was still noticed");
        player.getPersistentData().putLong(Watchers.UNSEEN_UNTIL, 0L);
        helper.assertTrue(Watchers.stillKnows(player, 0.7F), "the mind that drew it was lost at once");
        player.getPersistentData().putFloat(Watchers.KNOWING, 0.3F);
        helper.assertFalse(Watchers.stillKnows(player, 0.7F), "dropping what it knew didn't lose the watcher");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "watchers")
    public static void theBurningHauntsLavaAndTheWeightTheDeep(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        helper.assertFalse(Face.SABBATAIOS.haunts(helper.getLevel(), player), "the flame-faced haunts a place with no fire");
        helper.setBlock(new BlockPos(19, 1, 15), Blocks.LAVA);
        helper.assertTrue(Face.SABBATAIOS.haunts(helper.getLevel(), player), "the flame-faced doesn't haunt the lava's edge");
        for (int x = 10; x <= 20; ++x) {
            for (int z = 10; z <= 20; ++z) {
                helper.setBlock(x, 4, z, Blocks.STONE);
            }
        }
        boolean deep = player.getY() < 0.0;
        helper.succeedWhen(() -> helper.assertTrue(Face.ELOAIOU.haunts(helper.getLevel(), player) == deep,
                "the ass-faced haunting doesn't follow depth under a roof (y " + player.getY() + ")"));
    }

    @GameTest(template = SKY, batch = "watchers")
    public static void aWatcherComesInSightAndIsNeverSaved(GameTestHelper helper) {
        Player player = knower(helper, 0.9F);
        Watcher watcher = Watchers.arrive(helper.getLevel(), player, Face.YAO);
        helper.assertTrue(watcher != null, "no watcher came");
        double away = watcher.position().distanceTo(player.position());
        helper.assertTrue(away >= Watchers.NEAREST - 3 && away <= Watchers.FARTHEST + 3, "it came " + away + " away");
        helper.assertTrue(watcher.sees(player), "it came where it can't see its quarry");
        helper.assertTrue(watcher.state() == Watcher.ARRIVING, "it didn't arrive");
        helper.assertFalse(watcher.shouldBeSaved(), "a watcher that came for someone would be saved");
        helper.assertTrue(Watchers.restingUntil(player) > helper.getLevel().getGameTime(), "nothing stops the next coming at once");
        helper.assertTrue(Watchers.watching(helper.getLevel(), player) == watcher, "it isn't counted as watching its quarry");
        watcher.discard();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 60)
    public static void theWeightPressesOnlyWhatItSees(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        Player hidden = addMockPlayer(helper, new Vec3(5.5, 1.0, 5.5));
        hidden.getPersistentData().putFloat(Watchers.KNOWING, 0.7F);
        onFinish(helper, hidden::discard);
        for (int y = 1; y <= 10; ++y) {
            for (int x = 0; x <= 12; ++x) {
                helper.setBlock(x, y, 8, Blocks.STONE);
            }
        }
        Watcher seen = watcher(helper, Face.ELOAIOU, player);
        WatcherTestAccess.work(seen, player);
        Watcher blind = helper.spawn(WildspellMobs.WATCHERS.get(Face.ELOAIOU).get(), new Vec3(5.5, 1.5, 12.5));
        blind.comeFor(hidden, 0.7F);
        WatcherTestAccess.work(blind, hidden);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(player.hasEffect(WildspellMobs.WEIGHED), "the quarry in its sight wasn't pressed down");
            helper.assertTrue(player.getAttributeValue(Attributes.JUMP_STRENGTH) == 0.0, "the weighed can still jump");
            helper.assertFalse(hidden.hasEffect(WildspellMobs.WEIGHED), "the weight went through a wall");
            seen.discard();
            blind.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = "watchers")
    public static void theRotSpoilsFoodAndWearsTools(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        ItemStack bread = new ItemStack(Items.BREAD, 3);
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        player.getInventory().setItem(0, bread);
        player.getInventory().setItem(1, pick);
        Watcher watcher = watcher(helper, Face.ASTAPHAIOS, player);
        watcher.rot(helper.getLevel(), player, 2);
        helper.assertTrue(bread.getCount() == 2, "the food didn't spoil");
        helper.assertTrue(player.getInventory().countItem(Items.ROTTEN_FLESH) == 1, "the spoiled food didn't turn to rot");
        helper.assertTrue(pick.getDamageValue() > 0, "the tool didn't wear");
        watcher.discard();
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 120)
    public static void theTurningDragsTheQuarryBackAlongItsPath(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        Watcher watcher = watcher(helper, Face.ATHOTH, player);
        Vec3 start = player.position();
        for (int i = 1; i <= 40; ++i) {
            int step = i;
            helper.runAfterDelay(i, () -> player.moveTo(start.x + step * 0.1, start.y, start.z));
        }
        helper.runAfterDelay(42, () -> WatcherTestAccess.work(watcher, player));
        helper.runAfterDelay(62, () -> {
            helper.assertTrue(player.getX() < start.x + 2.5, "the quarry wasn't turned back (x " + (player.getX() - start.x) + ")");
            watcher.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 40)
    public static void theGlareBlindsOnlyThoseWhoLook(GameTestHelper helper) {
        Player looking = knower(helper, 0.7F);
        Player away = addMockPlayer(helper, new Vec3(17.5, 1.0, 15.5));
        away.getPersistentData().putFloat(Watchers.KNOWING, 0.7F);
        onFinish(helper, away::discard);
        Watcher one = watcher(helper, Face.ADONIN, looking);
        Watcher two = helper.spawn(WildspellMobs.WATCHERS.get(Face.ADONIN).get(), new Vec3(17.5, 1.5, 22.5));
        two.comeFor(away, 0.7F);
        helper.runAfterDelay(2, () -> {
            face(looking, one.getEyePosition());
            face(away, away.getEyePosition().scale(2.0).subtract(two.getEyePosition()));
            WatcherTestAccess.work(one, looking);
            WatcherTestAccess.work(two, away);
            helper.assertTrue(looking.hasEffect(WildspellMobs.GLARE), "looking at it didn't blind");
            helper.assertFalse(away.hasEffect(WildspellMobs.GLARE), "looking away still blinded");
            one.discard();
            two.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 60)
    public static void theBurningSetsTheQuarryAlight(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        player.setInvulnerable(false);
        Watcher watcher = watcher(helper, Face.SABBATAIOS, player);
        WatcherTestAccess.work(watcher, player);
        helper.assertTrue(Watcher.inHeat(player, watcher.mark()), "the heat isn't round the quarry");
        helper.assertFalse(Watcher.inHeat(player, watcher.mark().add(Watcher.HEAT_RADIUS + 1.0, 0.0, 0.0)), "the heat reaches past its edge");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(player.getRemainingFireTicks() > 0, "the quarry standing in the heat didn't burn");
            watcher.discard();
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 140)
    public static void forgettingLosesTheWatcher(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        Watcher watcher = watcher(helper, Face.YAO, player);
        helper.runAfterDelay(5, () -> player.getPersistentData().putFloat(Watchers.KNOWING, 0.1F));
        helper.runAfterDelay(10, () -> helper.assertTrue(watcher.state() == Watcher.LOST, "it kept its quarry after they forgot"));
        helper.succeedWhen(() -> helper.assertTrue(watcher.isRemoved() && !watcher.isDeadOrDying(), "it didn't withdraw"));
    }

    @GameTest(template = SKY, batch = "watchers", timeoutTicks = 40)
    public static void aFelledWatcherLeavesNothingButTheDeed(GameTestHelper helper) {
        Player player = knower(helper, 0.7F);
        Watcher watcher = watcher(helper, Face.SABAOTH, player);
        watcher.hurt(helper.getLevel().damageSources().playerAttack(player), 1000.0F);
        helper.assertTrue(watcher.isDeadOrDying(), "it didn't die");
        helper.assertTrue(player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(Watchers.FELLED, Tag.TAG_STRING).getString(0)
                .equals(Face.SABAOTH.id), "felling it wasn't remembered");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, watcher.getBoundingBox().inflate(4.0)).isEmpty(), "it dropped something");
            helper.succeed();
        });
    }
}
