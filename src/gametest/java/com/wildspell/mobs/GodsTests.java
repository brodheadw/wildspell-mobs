package com.wildspell.mobs;

import com.wildspell.mobs.entity.Apollo;
import com.wildspell.mobs.entity.Diana;
import com.wildspell.mobs.gods.Gaze;
import com.wildspell.mobs.gods.Heavens;
import com.wildspell.mobs.gods.HeavensTestAccess;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
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

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class GodsTests {

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
        Player player = aloftPlayer(helper, new Vec3(4.5, 1.0, 4.5));
        helper.assertFalse(Heavens.sunWillAnswer(player), "a player who slew no warden shouldn't be heard");
        int needed = MobsConfig.APOLLO_WARDENS.get();
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
    public static void onlyAGazeFromTheGodSkyHeightsCounts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        String dimension = MobsConfig.GOD_SKY_DIMENSION.get();
        int height = MobsConfig.GOD_ARRIVAL_HEIGHT.get();
        double zenith = MobsConfig.GOD_ZENITH_DEGREES.get();
        onFinish(helper, () -> {
            MobsConfig.GOD_SKY_DIMENSION.set(dimension);
            MobsConfig.GOD_ARRIVAL_HEIGHT.set(height);
            MobsConfig.GOD_ZENITH_DEGREES.set(zenith);
        });
        Gaze body = Gaze.SUN.isUp(level) ? Gaze.SUN : Gaze.MOON;
        Player player = aloftPlayer(helper, new Vec3(15.5, 20.0, 15.5));
        Vec3 look = body.direction(level);
        player.setXRot((float) -Math.toDegrees(Math.asin(look.y)));
        player.setYRot((float) Math.toDegrees(Math.atan2(-look.x, look.z)));
        player.setYHeadRot(player.getYRot());
        helper.assertTrue(Gaze.degreesBetween(player.getViewVector(1.0F), look) < 1.0, "the test couldn't aim the player at the " + body);
        MobsConfig.GOD_ZENITH_DEGREES.set(90.0);
        MobsConfig.GOD_ARRIVAL_HEIGHT.set((int) player.getY() - 5);
        helper.assertFalse(body.holds(player), "a gaze from the overworld counted while the god sky is " + dimension);
        MobsConfig.GOD_SKY_DIMENSION.set(level.dimension().location().toString());
        helper.assertTrue(body.holds(player), "a gaze from the god sky's heights didn't count");
        MobsConfig.GOD_ARRIVAL_HEIGHT.set((int) player.getY() + 5);
        helper.assertFalse(body.holds(player), "a gaze from below the arrival height counted");
        helper.succeed();
    }

    @GameTest(template = SKY, batch = "godsApollo")
    public static void apolloConcedesInsteadOfDying(GameTestHelper helper) {
        onFinish(helper, () -> HeavensTestAccess.restoreTheSun(helper.getLevel()));
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

    @GameTest(template = SKY, batch = "godsSunSlain")
    public static void killingApolloPutsOutTheSunForGood(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        long time = overworld.getDayTime();
        boolean daylight = overworld.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT);
        onFinish(helper, () -> {
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
        onFinish(helper, () -> {
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
        onFinish(helper, () -> {
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
}
