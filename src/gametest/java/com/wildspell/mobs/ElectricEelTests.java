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
import net.minecraft.world.entity.animal.Cod;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class ElectricEelTests {
    private static final ResourceKey<Biome> DRIPSTONE_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("dripstone_caves"));

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void dischargeShocksEverythingInTheWaterButNothingAshore(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 1.2F, 4.5F);
        Pig target = helper.spawn(EntityType.PIG, 5.5F, 2.0F, 4.5F);
        Pig bystander = helper.spawn(EntityType.PIG, 3.5F, 2.0F, 2.5F);
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

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "eelHungry")
    public static void hungryEelCatchesAndSwallowsAFish(GameTestHelper helper) {
        pool(helper);
        ElectricEel eel = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 1.2F, 4.5F);
        eel.setFedTicks(0);
        Cod cod = helper.spawn(EntityType.COD, 3.5F, 2.0F, 4.5F);
        helper.onEachTick(() -> helper.assertTrue(eel.getCharge() == 0, "eel wound up a discharge at a fish"));
        helper.succeedWhen(() -> {
            helper.assertTrue(!cod.isAlive(), "hungry eel hasn't caught the cod; target=" + eel.getTarget());
            helper.assertTrue(!eel.isHungry(), "eel caught the cod but is still hungry");
            helper.assertTrue(eel.getTarget() == null, "fed eel still after " + eel.getTarget());
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "the cod was left behind as a drop, not eaten");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "eelDen")
    public static void eelSurvivesLosingItsDen(GameTestHelper helper) {
        pool(helper);
        ElectricEel[] eels = new ElectricEel[2];
        eels[0] = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 2.5F, 2.0F, 3.5F);
        eels[1] = helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 4.5F, 2.0F, 5.5F);
        helper.assertTrue((eels[0].getId() + eels[1].getId()) % 2 == 1, "the two eels tick on opposite parities");
        helper.runAfterDelay(120, () -> {
            eels[0].setDen(helper.absolutePos(new BlockPos(1, 1, 1)));
            eels[1].setDen(helper.absolutePos(new BlockPos(5, 1, 7)));
        });
        helper.runAfterDelay(123, () -> {
            helper.assertTrue(eels[0].getNavigation().isInProgress() || eels[1].getNavigation().isInProgress(), "the lurk goal set off for the den");
            eels[0].setDen(null);
            eels[1].setDen(null);
        });
        helper.runAfterDelay(160, () -> {
            helper.assertTrue(eels[0].isAlive() && eels[1].isAlive(), "both eels are still with us");
            helper.succeed();
        });
    }

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

    @GameTest(template = ARENA, timeoutTicks = 300, batch = "eelParity")
    public static void aDischargeEndsTheWindUpWhateverTheTickParity(GameTestHelper helper) {
        pool(helper);
        ElectricEel[] eels = {helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 3.5F, 1.2F, 4.5F), helper.spawn(WildspellMobs.ELECTRIC_EEL.get(), 5.5F, 1.2F, 4.5F)};
        Pig target = helper.spawn(EntityType.PIG, 4.5F, 2.0F, 4.5F);
        target.setNoAi(true);
        target.setInvulnerable(true);
        int[] last = new int[2];
        boolean[] discharged = new boolean[2];
        for (ElectricEel eel : eels) {
            eel.setTarget(target);
        }
        helper.onEachTick(() -> {
            for (int i = 0; i < 2; ++i) {
                int charge = eels[i].getCharge();
                if (discharged[i]) {
                    helper.assertTrue(charge == 0, "eel " + i + " wound up again with nothing to shock");
                } else if (last[i] >= ElectricEel.CHARGE_TICKS - 2 && charge <= 1) {
                    discharged[i] = true;
                    eels[i].setTarget(null);
                    helper.assertTrue(charge == 0, "eel " + i + " rewound straight after its discharge");
                }
                last[i] = charge;
            }
            if (discharged[0] && discharged[1] && !target.isRemoved()) {
                target.discard();
            }
        });
        helper.runAtTickTime(290, () -> {
            helper.assertTrue(discharged[0] && discharged[1], "not every eel discharged");
            helper.succeed();
        });
    }
}
