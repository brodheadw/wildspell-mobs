package com.wildspell.mobs;

//? if <26.4
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.RimeSkull;
import java.util.Arrays;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.wildspell.mobs.GameTests.*;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class WildspellMobsTests {
    @GameTest(template = ARENA)
    public static void biomesCarryOurSpawnsAndPatches(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Biome frosted = biomes.get(FROSTED_CAVES);
        helper.assertTrue(frosted != null, "yungscavebiomes:frosted_caves is not loaded");
        assertSpawns(helper, frosted, MobCategory.MONSTER, WildspellMobs.RIME_SKULL.get());
        assertSpawns(helper, biomes.get(Biomes.DRIPSTONE_CAVES), MobCategory.UNDERGROUND_WATER_CREATURE, WildspellMobs.ELECTRIC_EEL.get());
        assertSpawns(helper, biomes.get(Biomes.LUSH_CAVES), MobCategory.AMBIENT, WildspellMobs.LUMINOUS_MOTH.get());
        List<String> creepers = frosted.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .filter(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getPath().contains("creeper"))
                .map(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type) + " " + data.getWeight().asInt() + "x" + data.maxCount)
                .toList();
        String expected = ModList.get().isLoaded("creeperoverhaul") ? "creeperoverhaul:snowy_creeper 3x1" : "minecraft:creeper 3x1";
        helper.assertTrue(creepers.equals(List.of(expected)), "frosted caves creepers: " + creepers + ", expected only " + expected);
        assertGrows(helper, biomes.get(Biomes.JUNGLE), "flytrap_patch_jungle");
        assertGrows(helper, biomes.get(Biomes.LUSH_CAVES), "flytrap_patch_lush_caves");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void skullLungesAndFreezes(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        RimeSkull skull = helper.spawn(WildspellMobs.RIME_SKULL.get(), 1.5F, 3.0F, 1.5F);
        skull.setTarget(pig);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not hit yet");
            helper.assertTrue(pig.getTicksFrozen() > 0, "hit did not freeze");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 500)
    public static void skullSpitsFromRange(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 8.5F, 1.0F, 8.5F);
        pig.setNoAi(true);
        RimeSkull skull = helper.spawn(WildspellMobs.RIME_SKULL.get(), 0.5F, 4.0F, 0.5F);
        skull.setTarget(pig);
        helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "no frost shard has landed; skull at " + helper.relativeVec(skull.position()) + ", line of sight " + skull.hasLineOfSight(pig)));
    }

    //? if <26.4 {
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "freezing")
    public static void zombieFreezesInFrostedCaves(GameTestHelper helper) {
        shade(helper);
        paintFrostedCaves(helper);
        helper.setBlock(4, 0, 4, Blocks.ICE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4.5F, 1.0F, 4.5F);
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        boolean[] shivered = {false};
        helper.onEachTick(() -> shivered[0] |= zombie.isAlive() && zombie.isFullyFrozen());
        helper.succeedWhen(() -> {
            helper.assertTrue(!zombie.isAlive(), "zombie not converted; biome=" + helper.getLevel().getBiome(zombie.blockPosition()).getRegisteredName()
                    + " chill=" + zombie.getPersistentData() + " pos=" + helper.relativeVec(zombie.position()));
            helper.assertEntityNotPresent(EntityType.ZOMBIE);
            List<FrozenZombie> frozen = helper.getEntities(WildspellMobs.FROZEN_ZOMBIE.get());
            helper.assertTrue(frozen.size() == 1, "expected one frozen zombie, found " + frozen.size());
            helper.assertTrue(shivered[0], "zombie converted without shivering first");
            helper.assertTrue(frozen.get(0).getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET), "equipment lost in conversion");
            helper.assertTrue(frozen.get(0).isIcebound(), "froze standing on ice but its legs aren't locked in it");
            helper.assertTrue(helper.relativeVec(frozen.get(0).position()).y < 0.5, "not sunk into the ice: " + helper.relativeVec(frozen.get(0).position()));
        });
    }
    //?}

    //? if <26.4 {
    @GameTest(template = ARENA)
    public static void frozenZombieVariantFollowsTheGroundAndIsKept(GameTestHelper helper) {
        shade(helper);
        helper.setBlock(2, 0, 2, Blocks.STONE);
        helper.setBlock(6, 0, 6, Blocks.PACKED_ICE);
        int[] offIce = new int[3];
        int[] onIce = new int[3];
        int[] wholeFaces = {0};
        for (int i = 0; i < 200; ++i) {
            ++offIce[pickVariantAt(helper, new BlockPos(2, 1, 2), wholeFaces)];
            ++onIce[pickVariantAt(helper, new BlockPos(6, 1, 6), wholeFaces)];
        }
        helper.assertTrue(wholeFaces[0] > 80 && wholeFaces[0] < 190, "whole faces out of about one in three: " + wholeFaces[0] + " of 400");
        String mix = "off ice " + Arrays.toString(offIce) + ", on ice " + Arrays.toString(onIce);
        helper.assertTrue(onIce[FrozenZombie.ICEBOUND] == 200, "not always ice-bound on ice: " + mix);
        helper.assertTrue(offIce[FrozenZombie.ICEBOUND] == 0 && offIce[FrozenZombie.NORMAL] > offIce[FrozenZombie.ONE_ARMED]
                && offIce[FrozenZombie.ONE_ARMED] > 40, "unexpected mix: " + mix);
        FrozenZombie zombie = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos iceTop = helper.absolutePos(new BlockPos(6, 1, 6));
        zombie.moveTo(iceTop.getX() + 0.5, iceTop.getY(), iceTop.getZ() + 0.5);
        zombie.pickVariant();
        zombie.setWholeFace(true);
        helper.assertTrue(Math.abs(zombie.getY() - (iceTop.getY() - 0.75)) < 1.0E-6, "not sunk into the ice: y=" + zombie.getY());
        FrozenZombie reloaded = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        reloaded.load(zombie.saveWithoutId(new CompoundTag()));
        helper.assertTrue(reloaded.isIcebound() && reloaded.getY() == zombie.getY(), "ice-bound state not saved");
        helper.assertTrue(reloaded.hasWholeFace(), "whole face not saved");
        helper.assertTrue(reloaded.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0.0,
                "reloaded ice-bound zombie can still walk");
        helper.succeed();
    }
    //?}

    //? if <26.4 {
    private static int pickVariantAt(GameTestHelper helper, BlockPos relative, int[] wholeFaces) {
        FrozenZombie zombie = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos pos = helper.absolutePos(relative);
        zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        wholeFaces[0] += zombie.hasWholeFace() ? 1 : 0;
        return zombie.getVariant();
    }
    //?}

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iciclesMeltOverLava(GameTestHelper helper) {
        shade(helper);
        Block icicle = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "icicle"));
        if (icicle == Blocks.AIR) {
            helper.succeed();
            return;
        }
        helper.setBlock(2, 3, 4, Blocks.STONE);
        helper.setBlock(2, 2, 4, icicle);
        helper.setBlock(2, 0, 4, Blocks.LAVA);
        helper.setBlock(6, 3, 4, Blocks.STONE);
        helper.setBlock(6, 2, 4, icicle);
        helper.setBlock(6, 0, 4, Blocks.STONE);
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 4)).is(IceMelting.MELTS_NEAR_HEAT), "the icicle is meltable");
        helper.assertTrue(IceMelting.melt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 4))), "over lava it melts");
        helper.assertBlockPresent(Blocks.AIR, 2, 2, 4);
        helper.assertFalse(IceMelting.melt(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 4))), "over stone it stays");
        helper.assertBlockPresent(icicle, 6, 2, 4);
        helper.setBlock(6, 0, 4, Blocks.MAGMA_BLOCK);
        helper.setBlock(6, 1, 4, icicle);
        int melted = IceMelting.sampleAround(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4)), 2000, 4, 2);
        helper.assertTrue(melted == 2, "the sampler melted " + melted);
        helper.assertBlockPresent(Blocks.AIR, 6, 1, 4);
        helper.assertBlockPresent(Blocks.AIR, 6, 2, 4);
        helper.succeed();
    }

    //? if <26.4 {
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "sunThaw")
    public static void frozenZombiesThawInTheSunAndThenBurn(GameTestHelper helper) {
        long time = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(6000);
        openToTheSky(helper);
        FrozenZombie frozen = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 4.5F, 1.0F, 4.5F);
        frozen.setNoAi(true);
        helper.succeedWhen(() -> {
            List<Zombie> zombies = helper.getEntities(EntityType.ZOMBIE);
            helper.assertTrue(!frozen.isAlive() && zombies.size() == 1, "the frozen zombie hasn't thawed in the sun");
            helper.assertTrue(zombies.getFirst().isOnFire(), "the thawed zombie isn't burning in the sun");
            helper.getLevel().setDayTime(time);
        });
    }
    //?}

    @GameTest(template = ARENA)
    public static void enchantedIceDropsACrystalUnlessSilkTouched(GameTestHelper helper) {
        Block rareIce = BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "rare_ice"));
        helper.assertTrue(rareIce != Blocks.AIR, "yungscavebiomes:rare_ice is not loaded");
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.getLevel().setBlockAndUpdate(pos, rareIce.defaultBlockState());
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(Enchantments.SILK_TOUCH), 1);
        for (ItemStack tool : List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE), silkPick)) {
            List<ItemStack> drops = Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
                    helper.getLevel().getBlockEntity(pos), null, tool);
            boolean crystal = drops.stream().anyMatch(stack -> stack.is(WildspellMobs.ENCHANTED_ICE_CRYSTAL.get()));
            boolean block = drops.stream().anyMatch(stack -> stack.is(rareIce.asItem()));
            boolean silk = tool == silkPick;
            helper.assertTrue(crystal != silk && block == silk, (silk ? "silk touch" : "plain " + tool) + " dropped " + drops);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void undergroundCreepersAreThinned(GameTestHelper helper) {
        shade(helper);
        BlockPos spot = new BlockPos(4, 1, 4);
        helper.assertTrue(passRate(helper, EntityType.CREEPER, spot) == 1.0, "creepers thinned under open sky");
        sealCave(helper);
        whenSealed(helper, spot, () -> {
            double creeperPass = passRate(helper, EntityType.CREEPER, spot);
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            double skullPass = passRate(helper, WildspellMobs.RIME_SKULL.get(), spot);
            helper.assertTrue(Math.abs(creeperPass - MobsConfig.UNDERGROUND_CREEPER_CHANCE.get()) < 0.05, "creeper pass rate " + creeperPass);
            helper.assertTrue(Math.abs(zombiePass - MobsConfig.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.assertTrue(skullPass == 1.0, "rime skull pass rate " + skullPass);
            double scorpionPass = passRate(helper, WildspellMobs.SCORPION.get(), spot);
            helper.assertTrue(scorpionPass == 1.0, "scorpion pass rate " + scorpionPass);
            helper.succeed();
        });
    }

    // Batched arenas sit inside each other's cap radius, so each crowded arena gets its own batch.
    @GameTest(template = ARENA, timeoutTicks = 100, batch = "localCap")
    public static void crowdedCaveRefusesSpawns(GameTestHelper helper) {
        shade(helper);
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < MobsConfig.UNDERGROUND_LOCAL_CAP.get(); ++i) {
            spawnWild(helper, EntityType.ZOMBIE, new BlockPos(3 + i % 3, 1, 3 + i / 3 % 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            double skullPass = passRate(helper, WildspellMobs.RIME_SKULL.get(), spot);
            helper.assertTrue(zombiePass == 0.0, "zombie pass rate with a full local cap " + zombiePass);
            helper.assertTrue(skullPass == 0.0, "skull pass rate with a full local cap " + skullPass);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "persistentCap")
    public static void persistentMobsLeaveTheLocalCapAlone(GameTestHelper helper) {
        shade(helper);
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < MobsConfig.UNDERGROUND_LOCAL_CAP.get(); ++i) {
            helper.spawn(EntityType.ZOMBIE, new BlockPos(3 + i % 3, 1, 3 + i / 3 % 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            helper.assertTrue(Math.abs(zombiePass - MobsConfig.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.succeed();
        });
    }

    private static void assertSpawns(GameTestHelper helper, Biome biome, MobCategory category, EntityType<?> type) {
        helper.assertTrue(biome.getMobSettings().getMobs(category).unwrap().stream().anyMatch(data -> data.type == type),
                type.toShortString() + " missing from its biome's " + category.getName() + " spawns");
    }

    private static void assertGrows(GameTestHelper helper, Biome biome, String feature) {
        ResourceKey<PlacedFeature> key = ResourceKey.create(Registries.PLACED_FEATURE, WildspellMobs.id(feature));
        helper.assertTrue(biome.getGenerationSettings().features().stream().anyMatch(step -> step.stream().anyMatch(holder -> holder.is(key))),
                feature + " missing from its biome's features");
    }

    private static <T extends Mob> T spawnWild(GameTestHelper helper, EntityType<T> type, BlockPos relative) {
        T mob = type.create(helper.getLevel());
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        mob.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    private static double passRate(GameTestHelper helper, EntityType<? extends Mob> type, BlockPos relative) {
        SpawnBalance.clearCache();
        Mob mob = type.create(helper.getLevel());
        BlockPos pos = helper.absolutePos(relative);
        mob.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        int passed = 0;
        int trials = 4000;
        for (int i = 0; i < trials; ++i) {
            MobSpawnEvent.PositionCheck event = new MobSpawnEvent.PositionCheck(mob, helper.getLevel(), MobSpawnType.NATURAL, null);
            NeoForge.EVENT_BUS.post(event);
            if (event.getResult() != MobSpawnEvent.PositionCheck.Result.FAIL) {
                ++passed;
            }
        }
        mob.discard();
        return passed / (double) trials;
    }
}
