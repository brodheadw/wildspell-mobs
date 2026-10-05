package com.wildspell.mobs;

import com.wildspell.mobs.entity.Apollo;
import com.wildspell.mobs.entity.Diana;
import com.wildspell.mobs.entity.MoonArrow;
import com.wildspell.mobs.gods.Gaze;
import com.wildspell.mobs.gods.Heavens;
import com.wildspell.mobs.gods.HeavensTestAccess;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class GodsTests {
    private static final String SKY = "sky_arena";

    private static Player aloftPlayer(GameTestHelper helper, Vec3 at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(at));
        return player;
    }

    private static Apollo stillApollo(GameTestHelper helper, Player player) {
        Apollo apollo = helper.spawn(WildspellMobs.APOLLO.get(), 15.5F, 10.0F, 15.5F);
        apollo.setNoAi(true);
        apollo.join(player);
        return apollo;
    }

    private static Diana huntingDiana(GameTestHelper helper, Heavens.Hunt hunt) {
        Diana diana = helper.spawn(WildspellMobs.DIANA.get(), 15.5F, 10.0F, 15.5F);
        diana.setNoAi(true);
        hunt.claim(diana);
        return diana;
    }

    @GameTest(template = SKY, batch = "godsWardens")
    public static void theSunAnswersOnlyAfterItsWardensFall(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.moveTo(helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 1.0, 4.5)));
        helper.assertFalse(Heavens.sunWillAnswer(player), "a player who slew no warden shouldn't be heard");
        int needed = com.wildspell.mobs.SpawnBalance.APOLLO_WARDENS.get();
        CompoundTag persisted = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        persisted.putInt(Heavens.WARDENS, needed - 1);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        helper.assertFalse(Heavens.sunWillAnswer(player), "one warden short shouldn't be heard");
        persisted.putInt(Heavens.WARDENS, needed);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, persisted);
        helper.assertTrue(Heavens.sunWillAnswer(player), "enough wardens slain should open the sun");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsSky")
    public static void sunAndMoonStandWhereTheSkyDrawsThem(GameTestHelper helper) {
        Vec3 noon = Gaze.SUN.direction(0.0F);
        helper.assertTrue(noon.y > 0.999, "the sun isn't overhead at noon: " + noon);
        helper.assertTrue(Gaze.SUN.direction(0.75F).x > 0.999, "the sun doesn't rise in the east: " + Gaze.SUN.direction(0.75F));
        helper.assertTrue(Gaze.SUN.direction(0.25F).x < -0.999, "the sun doesn't set in the west: " + Gaze.SUN.direction(0.25F));
        helper.assertTrue(Gaze.MOON.direction(0.5F).y > 0.999, "the moon isn't overhead at midnight: " + Gaze.MOON.direction(0.5F));
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 tilted = new Vec3(Math.sin(Math.toRadians(8.0)), Math.cos(Math.toRadians(8.0)), 0.0);
        helper.assertTrue(Gaze.SUN.holds(up, 0.0F, 30.0, 5.0), "staring straight at the noon sun doesn't count");
        helper.assertFalse(Gaze.SUN.holds(tilted, 0.0F, 30.0, 5.0), "a gaze 8 degrees off the sun counted");
        helper.assertFalse(Gaze.SUN.holds(Gaze.SUN.direction(0.85F), 0.85F, 30.0, 5.0), "a morning sun far from noon counted");
        helper.assertFalse(Gaze.SUN.holds(Gaze.SUN.direction(0.5F), 0.5F, 30.0, 5.0), "the midnight sun under the world counted");
        helper.assertTrue(Gaze.MOON.holds(up, 0.5F, 30.0, 5.0), "staring at the midnight moon doesn't count");
        helper.assertFalse(Gaze.MOON.holds(up, 0.0F, 30.0, 5.0), "the moon counted at noon");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsSky")
    public static void onlyAGazeFromTheGodSkyHeightsCounts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String dimension = SpawnBalance.GOD_SKY_DIMENSION.get();
        int height = SpawnBalance.GOD_ARRIVAL_HEIGHT.get();
        double zenith = SpawnBalance.GOD_ZENITH_DEGREES.get();
        WildspellMobsTests.onFinish(helper, () -> {
            SpawnBalance.GOD_SKY_DIMENSION.set(dimension);
            SpawnBalance.GOD_ARRIVAL_HEIGHT.set(height);
            SpawnBalance.GOD_ZENITH_DEGREES.set(zenith);
        });
        Gaze body = Gaze.SUN.isUp(level) ? Gaze.SUN : Gaze.MOON;
        Player player = aloftPlayer(helper, new Vec3(15.5, 20.0, 15.5));
        Vec3 look = body.direction(level);
        player.setXRot((float) -Math.toDegrees(Math.asin(look.y)));
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setYHeadRot(player.getYRot());
        helper.assertTrue(Gaze.degreesBetween(player.getViewVector(1.0F), look) < 1.0, "the test couldn't aim the player at the " + body);
        SpawnBalance.GOD_ZENITH_DEGREES.set(90.0);
        SpawnBalance.GOD_ARRIVAL_HEIGHT.set((int) player.getY() - 5);
        helper.assertFalse(body.holds(player), "a gaze from the overworld counted while the god sky is " + dimension);
        SpawnBalance.GOD_SKY_DIMENSION.set(level.dimension().location().toString());
        helper.assertTrue(body.holds(player), "a gaze from the god sky's heights didn't count");
        SpawnBalance.GOD_ARRIVAL_HEIGHT.set((int) player.getY() + 5);
        helper.assertFalse(body.holds(player), "a gaze from below the arrival height counted");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsApollo")
    public static void apolloConcedesInsteadOfDying(GameTestHelper helper) {
        WildspellMobsTests.onFinish(helper, () -> HeavensTestAccess.restoreTheSun(helper.getLevel()));
        Player player = aloftPlayer(helper, new Vec3(15.5, 6.0, 15.5));
        Apollo apollo = stillApollo(helper, player);
        apollo.hurt(helper.getLevel().damageSources().playerAttack(player), 10000.0F);
        helper.assertTrue(apollo.isAlive(), "Apollo died to a single blow instead of conceding");
        helper.assertTrue(apollo.hasConceded(), "Apollo didn't concede at low health");
        helper.assertTrue(apollo.getHealth() == apollo.concedeHealth(), "conceded at " + apollo.getHealth() + ", not " + apollo.concedeHealth());
        float health = apollo.getHealth();
        helper.assertFalse(apollo.hurt(helper.getLevel().damageSources().magic(), 20.0F), "a conceded Apollo took damage from no one");
        helper.assertTrue(apollo.getHealth() == health && !apollo.isWarned(), "a nameless hurt counted as a strike");
        helper.assertTrue(Apollo.openHanded(List.of(player)), "an empty main hand isn't an open hand");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        helper.assertFalse(Apollo.openHanded(List.of(player)), "a drawn sword passed for an open hand");
        helper.assertFalse(Apollo.openHanded(List.of()), "nobody present passed for an open hand");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsApollo")
    public static void sparingApolloMarksThePlayerAndHeLeaves(GameTestHelper helper) {
        WildspellMobsTests.onFinish(helper, () -> HeavensTestAccess.restoreTheSun(helper.getLevel()));
        Heavens heavens = Heavens.get(helper.getLevel());
        Player player = aloftPlayer(helper, new Vec3(15.5, 6.0, 15.5));
        Apollo apollo = stillApollo(helper, player);
        helper.assertFalse(heavens.apolloIsWary(), "Apollo is wary before he was ever spared");
        apollo.concede();
        apollo.spare(List.of(player));
        apollo.spare(List.of(player));
        helper.assertTrue(Heavens.hasSpared(player, Heavens.APOLLO), "sparing Apollo left no mark on the player");
        ListTag spared = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getList(Heavens.SPARED, Tag.TAG_STRING);
        helper.assertTrue(spared.size() == 1 && spared.getString(0).equals(Heavens.APOLLO), "the persisted spared list reads " + spared);
        helper.assertTrue(apollo.isLeaving(), "a spared Apollo didn't withdraw");
        helper.assertTrue(heavens.apolloIsWary(), "a spared Apollo isn't wary next time");
        helper.assertFalse(heavens.sunSlain(), "sparing Apollo put out the sun");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsSunSlain")
    public static void killingApolloPutsOutTheSunForGood(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        long time = overworld.getDayTime();
        boolean daylight = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        WildspellMobsTests.onFinish(helper, () -> {
            HeavensTestAccess.restoreTheSun(helper.getLevel());
            overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(daylight, overworld.getServer());
            overworld.setDayTime(time);
        });
        Heavens heavens = Heavens.get(helper.getLevel());
        Player player = aloftPlayer(helper, new Vec3(15.5, 6.0, 15.5));
        Apollo apollo = stillApollo(helper, player);
        helper.assertTrue(heavens.apolloCanCome(overworld.getServer()), "Apollo can't come before anything has happened");
        apollo.concede();
        float health = apollo.getHealth();
        apollo.hurt(helper.getLevel().damageSources().playerAttack(player), 50.0F);
        helper.assertTrue(apollo.isAlive() && apollo.isWarned(), "the first blow on a conceded Apollo didn't warn");
        helper.assertTrue(apollo.getHealth() == health, "the warning blow hurt him");
        helper.assertFalse(heavens.sunSlain(), "the warning blow already put out the sun");
        apollo.hurt(helper.getLevel().damageSources().playerAttack(player), 1.0F);
        helper.assertTrue(apollo.isDeadOrDying(), "the second blow didn't kill him");
        helper.assertTrue(heavens.sunSlain(), "killing Apollo didn't put out the sun");
        helper.assertTrue(heavens.isSunSlayer(player.getUUID()), "the killer isn't remembered");
        helper.assertFalse(Heavens.hasSpared(player, Heavens.APOLLO), "the killer was marked as having spared him");
        helper.assertTrue(Math.floorMod(overworld.getDayTime(), 24000L) == Heavens.NIGHT_TIME, "the overworld isn't night: " + overworld.getDayTime());
        helper.assertFalse(overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), "the daylight cycle still runs");
        helper.assertFalse(heavens.apolloCanCome(overworld.getServer()), "a dead Apollo can still be called");
        overworld.setDayTime(overworld.getDayTime() + 24000L - Heavens.NIGHT_TIME + 1000L);
        overworld.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, overworld.getServer());
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(Math.floorMod(overworld.getDayTime(), 24000L) == Heavens.NIGHT_TIME, "the day came back: " + overworld.getDayTime());
            helper.assertFalse(overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT), "the daylight cycle came back");
            helper.succeed();
        });
    }

    @GameTest(template = SKY, batch = "godsDiana")
    public static void dianaYieldsAndTheHuntIsTheQuarrys(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = aloftPlayer(helper, new Vec3(15.5, 6.0, 15.5));
        WildspellMobsTests.onFinish(helper, () -> {
            HeavensTestAccess.forgetHunt(level);
            HeavensTestAccess.forgetPending(level, player.getUUID());
        });
        Heavens.Hunt hunt = Heavens.get(level).beginHunt(level, false);
        hunt.add(player);
        Diana diana = huntingDiana(helper, hunt);
        helper.assertTrue(diana.moonPhase() == 0, "an unhurt Diana's face isn't full: " + diana.moonPhase());
        diana.hurt(level.damageSources().playerAttack(player), 10000.0F);
        helper.assertTrue(diana.isAlive(), "Diana died; she should only yield");
        helper.assertTrue(diana.hasYielded(), "brought low, Diana didn't yield");
        helper.assertTrue(diana.moonPhase() == Diana.PHASES - 1, "a yielding Diana's face isn't dark: " + diana.moonPhase());
        helper.assertTrue(Heavens.get(level).hunt() == null, "the hunt goes on after she yielded");
        helper.assertTrue(HeavensTestAccess.pending(level, player.getUUID()).contains(Heavens.DIANA), "turning the hunt wasn't recorded");
        helper.assertFalse(diana.hurt(level.damageSources().playerAttack(player), 50.0F), "a yielded Diana can still be hurt");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsDiana")
    public static void dawnReleasesTheQuarryButNotTheSunsKillers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player innocent = aloftPlayer(helper, new Vec3(12.5, 6.0, 15.5));
        Player slayer = aloftPlayer(helper, new Vec3(18.5, 6.0, 15.5));
        WildspellMobsTests.onFinish(helper, () -> {
            HeavensTestAccess.forgetHunt(level);
            HeavensTestAccess.restoreTheSun(level);
            HeavensTestAccess.forgetPending(level, innocent.getUUID());
            HeavensTestAccess.forgetPending(level, slayer.getUUID());
        });
        Heavens heavens = Heavens.get(level);
        HeavensTestAccess.addSlayer(level, slayer.getUUID());
        Heavens.Hunt grief = heavens.beginHunt(level, true);
        grief.add(innocent);
        grief.add(slayer);
        HeavensTestAccess.dawn(level, grief);
        helper.assertTrue(heavens.hunt() == grief, "a grieving Diana let the sun's killer go at dawn");
        helper.assertTrue(grief.quarry().size() == 1 && grief.quarry().contains(slayer.getUUID()), "dawn left " + grief.quarry() + " hunted");
        helper.assertTrue(HeavensTestAccess.pending(level, innocent.getUUID()).contains(Heavens.DIANA), "the innocent's dawn wasn't recorded");
        helper.assertFalse(HeavensTestAccess.pending(level, slayer.getUUID()).contains(Heavens.DIANA), "the killer was recorded as surviving");
        HeavensTestAccess.forgetHunt(level);
        Heavens.Hunt hunt = heavens.beginHunt(level, false);
        hunt.add(slayer);
        HeavensTestAccess.dawn(level, hunt);
        helper.assertTrue(heavens.hunt() == null, "dawn didn't end an ordinary hunt");
        helper.assertTrue(HeavensTestAccess.pending(level, slayer.getUUID()).contains(Heavens.DIANA), "surviving to dawn wasn't recorded");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsDiana")
    public static void dianaGlintsBeforeSheLoosesAndCoverHoldsTheShot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = aloftPlayer(helper, new Vec3(15.5, 2.0, 4.5));
        WildspellMobsTests.onFinish(helper, () -> {
            HeavensTestAccess.forgetHunt(level);
            HeavensTestAccess.forgetPending(level, player.getUUID());
        });
        Heavens.Hunt hunt = Heavens.get(level).beginHunt(level, false);
        hunt.add(player);
        Diana diana = huntingDiana(helper, hunt);
        diana.beginDraw();
        for (int i = 0; i < Diana.DRAW_TICKS + Diana.HOLD_TICKS + 2; ++i) {
            diana.tickDraw(level, player, false);
        }
        helper.assertFalse(diana.isDrawing(), "with her quarry behind cover she held the draw forever");
        helper.assertTrue(arrows(helper).isEmpty(), "she loosed at a quarry she couldn't see");
        diana.beginDraw();
        for (int i = 0; i < Diana.DRAW_TICKS - 1; ++i) {
            diana.tickDraw(level, player, true);
        }
        helper.assertTrue(arrows(helper).isEmpty() && diana.isDrawing(), "she loosed before the glint had shown for its full draw");
        diana.tickDraw(level, player, true);
        helper.assertTrue(arrows(helper).size() == 1, "she didn't loose once the draw was done: " + arrows(helper).size());
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsDiana")
    public static void dianaGrievesInASunlessWorld(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = aloftPlayer(helper, new Vec3(15.5, 2.0, 15.5));
        WildspellMobsTests.onFinish(helper, () -> {
            Heavens.Hunt hunt = Heavens.get(level).hunt();
            Diana diana = hunt == null ? null : hunt.diana(level.getServer());
            if (diana != null) {
                diana.discard();
            }
            HeavensTestAccess.forgetHunt(level);
            HeavensTestAccess.forgetPending(level, player.getUUID());
        });
        Heavens.Hunt hunt = Heavens.get(level).beginHunt(level, true);
        hunt.add(player);
        Diana diana = Diana.arrive(level, player, hunt);
        helper.assertTrue(diana != null, "Diana didn't come");
        helper.assertTrue(diana.isGrieving(), "Diana doesn't grieve in a world whose sun is dead");
        helper.assertTrue(hunt.isDiana(diana), "the hunt doesn't know its Diana");
        helper.assertTrue(Diana.nightLeft(0.25F) == 1.0F && Diana.nightLeft(0.75F) == 0.0F && Math.abs(Diana.nightLeft(0.5F) - 0.5F) < 1.0E-6,
                "the night bar doesn't run from dusk to dawn");
        helper.succeed();
    }

    private static List<MoonArrow> arrows(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(MoonArrow.class, helper.getBounds().inflate(8.0));
    }
}
