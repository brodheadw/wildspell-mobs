package com.wildspell.mobs;

import com.wildspell.mobs.entity.ElectricEel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-world checks for the Electric Eel, run with the rest by ./gradlew runGameTestServer. */
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class ElectricEelTests {
    private static final String ARENA = "arena";
    private static final ResourceKey<Biome> DRIPSTONE_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("dripstone_caves"));

    @GameTest(template = ARENA)
    public static void caveWatersSpawnEels(GameTestHelper helper) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(DRIPSTONE_CAVES);
        boolean listed = biome.getMobSettings().getMobs(MobCategory.UNDERGROUND_WATER_CREATURE).unwrap().stream()
                .anyMatch(data -> data.type == WildspellMobs.ELECTRIC_EEL.get());
        helper.assertTrue(listed, "electric eel missing from dripstone caves underground water spawns");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dischargeShocksEverythingInTheWaterButNothingAshore(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 1.2F, 4.5F);
        Pig target = helper.spawn(EntityType.PIG, 5.5F, 2.0F, 4.5F);
        Pig bystander = helper.spawn(EntityType.PIG, 3.5F, 2.0F, 2.5F);
        // Within the discharge's reach, but on the bank.
        Pig ashore = helper.spawn(EntityType.PIG, 7.5F, 4.0F, 4.5F);
        for (Pig pig : new Pig[] {target, bystander, ashore}) {
            pig.setNoAi(true);
        }
        eel.setTarget(target);
        helper.succeedWhen(() -> {
            helper.assertTrue(target.getHealth() < target.getMaxHealth(), "target not shocked; eel charge=" + eel.getCharge());
            helper.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "shock didn't seize the target");
            helper.assertTrue(bystander.getHealth() < bystander.getMaxHealth(), "bystander in the water was spared");
            helper.assertTrue(ashore.getHealth() == ashore.getMaxHealth(), "pig on the bank was shocked");
            helper.assertTrue(eel.getHealth() == eel.getMaxHealth(), "eel shocked itself");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "eelFed")
    public static void fedEelLeavesFishAlone(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 1.2F, 4.5F);
        eel.setFedTicks(ElectricEel.FED_TICKS);
        Cod cod = helper.spawn(EntityType.COD, 3.5F, 2.0F, 4.5F);
        helper.onEachTick(() -> {
            helper.assertTrue(eel.getCharge() == 0, "a fed eel wound up a discharge; target=" + eel.getTarget());
            helper.assertTrue(eel.getTarget() == null, "a fed eel went for " + eel.getTarget());
        });
        helper.runAtTickTime(280, () -> {
            helper.assertTrue(cod.getHealth() == cod.getMaxHealth(), "a fed eel hurt the cod");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "eelHungry")
    public static void hungryEelCatchesAndSwallowsAFish(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 1.2F, 4.5F);
        eel.setFedTicks(0);
        Cod cod = helper.spawn(EntityType.COD, 3.5F, 2.0F, 4.5F);
        // It catches fish and swallows them; the discharge is for threats.
        helper.onEachTick(() -> helper.assertTrue(eel.getCharge() == 0, "eel wound up a discharge at a fish"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!cod.isAlive(), "hungry eel hasn't caught the cod; target=" + eel.getTarget());
            helper.assertTrue(!eel.isHungry(), "eel caught the cod but is still hungry");
            helper.assertTrue(eel.getTarget() == null, "fed eel still after " + eel.getTarget());
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "the cod was left behind as a drop, not eaten");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void onlySwimmersInItsTerritoryAreHunted(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 2.5F, 1.2F, 4.5F);
        eel.setNoAi(true);
        Player swimmer = addMockPlayer(helper, new Vec3(4.5, 1.5, 4.5));
        Player ashore = addMockPlayer(helper, new Vec3(7.5, 4.0, 4.5));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(swimmer.isInWater(), "swimmer isn't in the water");
            helper.assertTrue(eel.isIntruder(swimmer), "eel ignores a swimmer two blocks off");
            helper.assertTrue(!eel.isIntruder(ashore), "eel goes for someone on the bank");
            swimmer.discard();
            ashore.discard();
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "eelLeap")
    public static void eelLeapsAtSomeoneOnTheBank(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 2.2F, 4.5F);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 bank = helper.absoluteVec(new Vec3(7.5, 4.0, 4.5));
        player.moveTo(bank.x, bank.y, bank.z, 90.0F, 0.0F);
        helper.getLevel().addFreshEntity(player);
        eel.setTarget(player);
        helper.onEachTick(() -> {
            // Keep the player planted on the bank, out of reach of a discharge.
            player.moveTo(bank.x, bank.y, bank.z, 90.0F, 0.0F);
            helper.assertTrue(!player.isInWater(), "player on the bank is in the water");
            if (player.getLastDamageSource() != null) {
                helper.assertTrue(player.getLastDamageSource().is(ElectricEel.SHOCK), "hurt by " + player.getLastDamageSource().getMsgId() + ", not a shock");
                player.discard();
                helper.succeed();
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "eelDen")
    public static void eelTakesACreviceForItsDen(GameTestHelper helper) {
        pool(helper);
        // A nook cut into the far wall at water level, walled on four sides and open to the pool.
        BlockPos nook = new BlockPos(1, 1, 4);
        helper.setBlock(nook.north(), Blocks.STONE);
        helper.setBlock(nook.south(), Blocks.STONE);
        helper.setBlock(nook.above(), Blocks.STONE);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 2.2F, 4.5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.absolutePos(nook).equals(eel.getDen()), "eel's den is " + eel.getDen() + ", not the nook");
            helper.assertTrue(eel.isLurking(), "eel hasn't settled into its den; at " + helper.relativeVec(eel.position()));
        });
    }

    /**
     * A stone basin (x 1-6, z 1-7) three blocks deep in water, with a dry stone bank from x = 7
     * whose top is at y = 4.
     */
    private static void pool(GameTestHelper helper) {
        for (int x = 0; x <= 8; ++x) {
            for (int z = 0; z <= 8; ++z) {
                for (int y = 0; y <= 3; ++y) {
                    boolean wall = y == 0 || x == 0 || x >= 7 || z == 0 || z == 8;
                    helper.setBlock(x, y, z, wall ? Blocks.STONE : Blocks.WATER);
                }
            }
        }
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
