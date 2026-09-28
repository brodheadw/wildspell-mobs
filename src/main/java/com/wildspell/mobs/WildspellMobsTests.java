package com.wildspell.mobs;

import com.wildspell.mobs.crypt.LichCryptPiece;
import com.wildspell.mobs.crypt.LichSouls;
import com.wildspell.mobs.crypt.PhylacteryBlock;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import com.wildspell.mobs.crypt.PhylacteryItem;
import com.wildspell.mobs.entity.Frost;
import com.wildspell.mobs.entity.FrostShard;
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.RimeSkull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class WildspellMobsTests {
    private static final String ARENA = "arena";
    private static final ResourceKey<Biome> FROSTED_CAVES = ResourceKey.create(Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frosted_caves"));

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void skullHoversWithoutFalling(GameTestHelper helper) {
        shade(helper);
        RimeSkull skull = helper.spawn(WildspellMobs.RIME_SKULL.get(), 4.5F, 4.0F, 4.5F);
        skull.setNoAi(true);
        helper.runAfterDelay(80, () -> {
            double height = helper.relativeVec(skull.position()).y;
            helper.assertTrue(Math.abs(height - 4.0) < 0.01, "skull fell to relative y=" + height);
            helper.succeed();
        });
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
        int[] shards = {0};
        Set<Integer> seen = new HashSet<>();
        helper.onEachTick(() -> helper.getEntities(WildspellMobs.FROST_SHARD.get()).forEach(s -> { if (seen.add(s.getId())) shards[0]++; }));
        helper.succeedWhen(() -> helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN),
                "no frost shard has landed; shards fired=" + shards[0] + " target=" + skull.getTarget() + " skull=" + helper.relativeVec(skull.position())
                        + " dist=" + Math.sqrt(skull.distanceToSqr(pig)) + " los=" + skull.hasLineOfSight(pig) + " blockedBy=" + blocker(helper, skull, pig)));
    }

    @GameTest(template = ARENA)
    public static void frostedCavesSpawnSkulls(GameTestHelper helper) {
        shade(helper);
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(FROSTED_CAVES);
        helper.assertTrue(biome != null, "yungscavebiomes:frosted_caves is not loaded");
        boolean listed = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .anyMatch(data -> data.type == WildspellMobs.RIME_SKULL.get());
        helper.assertTrue(listed, "rime skull missing from frosted caves monster spawns");
        helper.succeed();
    }

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

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void zombieStaysAZombieOutsideTheCold(GameTestHelper helper) {
        shade(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4.5F, 1.0F, 4.5F);
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        helper.runAfterDelay(ZombieFreezing.CONVERT_AT + 40, () -> {
            helper.assertTrue(zombie.isAlive() && !zombie.isFullyFrozen(), "zombie froze outside a freezing biome");
            helper.assertEntityNotPresent(WildspellMobs.FROZEN_ZOMBIE.get());
            helper.succeed();
        });
    }

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

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void iceboundZombieIsStuckUntilTheIceBreaks(GameTestHelper helper) {
        shade(helper);
        helper.setBlock(4, 0, 4, Blocks.PACKED_ICE);
        Pig pig = helper.spawn(EntityType.PIG, 8.5F, 1.0F, 8.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 4.5F, 1.0F, 4.5F);
        zombie.pickVariant();
        zombie.setTarget(pig);
        Vec3 stuckAt = zombie.position();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(zombie.isIcebound() && zombie.position().distanceTo(stuckAt) < 1.0E-3,
                    "moved while stuck in the ice: " + helper.relativeVec(zombie.position()));
            helper.setBlock(4, 0, 4, Blocks.AIR);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(zombie.getVariant() == FrozenZombie.NORMAL, "still ice-bound after its ice broke");
            helper.assertTrue(zombie.onGround() && zombie.getY() < stuckAt.y - 0.2, "didn't drop out of the broken ice: " + helper.relativeVec(zombie.position()));
            helper.succeed();
        });
    }

    private static int pickVariantAt(GameTestHelper helper, BlockPos relative, int[] wholeFaces) {
        FrozenZombie zombie = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos pos = helper.absolutePos(relative);
        zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        wholeFaces[0] += zombie.hasWholeFace() ? 1 : 0;
        return zombie.getVariant();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void iceboundFrozenZombieThrowsSnowballsFromRange(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 8.5F, 1.0F, 8.5F);
        pig.setNoAi(true);
        helper.setBlock(1, 0, 1, Blocks.ICE);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 1.5F, 1.0F, 1.5F);
        zombie.pickVariant();
        zombie.setTarget(pig);
        boolean[] threwSnowball = {false};
        double[] closest = {Double.MAX_VALUE};
        Map<Integer, Vec3> flying = new HashMap<>();
        List<String> landed = new ArrayList<>();
        helper.onEachTick(() -> {
            closest[0] = Math.min(closest[0], zombie.distanceTo(pig));
            Set<Integer> now = new HashSet<>();
            for (var s : helper.getEntities(WildspellMobs.FROST_SHARD.get())) {
                threwSnowball[0] |= s.getItem().is(Items.SNOWBALL);
                now.add(s.getId());
                flying.put(s.getId(), helper.relativeVec(s.position()));
            }
            flying.keySet().removeIf(id -> {
                if (!now.contains(id)) {
                    Vec3 v = flying.get(id);
                    landed.add(String.format("(%.1f,%.1f,%.1f)", v.x, v.y, v.z));
                    return true;
                }
                return false;
            });
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(threwSnowball[0], "no snowball thrown");
            helper.assertTrue(pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "no snowball has landed; zombie at " + helper.relativeVec(zombie.position())
                    + " pig at " + helper.relativeVec(pig.position()) + " ended at " + landed);
            helper.assertTrue(closest[0] > 2.0, "closed to melee range: " + closest[0]);
        });
    }

    @GameTest(template = ARENA)
    public static void iceCubesDropIce(GameTestHelper helper) {
        EntityType<?> iceCubeType = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "ice_cube"));
        Entity cube = helper.spawn(iceCubeType, 4.5F, 1.0F, 4.5F);
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(((LivingEntity) cube).getLootTable());
        int ice = 0;
        for (int i = 0; i < 40; ++i) {
            var params = new LootParams.Builder(helper.getLevel())
                    .withParameter(LootContextParams.THIS_ENTITY, cube)
                    .withParameter(LootContextParams.ORIGIN, cube.position())
                    .withParameter(LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().generic())
                    .create(LootContextParamSets.ENTITY);
            ice += table.getRandomItems(params).stream().filter(s -> s.is(Items.ICE)).mapToInt(ItemStack::getCount).sum();
        }
        helper.assertTrue(ice > 10, "ice cubes dropped only " + ice + " ice over 40 kills");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void leatherShrugsOffFrostAndShardsBuildIt(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, 2.5F, 1.0F, 2.5F);
        pig.setNoAi(true);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 6.5F, 1.0F, 6.5F);
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        Frost.add(pig, FrostShard.SHARD_FROST, 20);
        Frost.add(zombie, FrostShard.SHARD_FROST, 20);
        helper.assertTrue(pig.getTicksFrozen() == FrostShard.SHARD_FROST, "one shard should add frost, not freeze solid: " + pig.getTicksFrozen());
        helper.assertTrue(zombie.getTicksFrozen() == 0, "leather boots should keep frost off");
        for (int i = 0; i < 5; ++i) {
            Frost.add(pig, FrostShard.SHARD_FROST, 20);
        }
        helper.assertTrue(pig.getTicksFrozen() == pig.getTicksRequiredToFreeze() + 20, "frost not capped: " + pig.getTicksFrozen());
        Frost.freezeSolid(zombie, 60);
        helper.assertTrue(zombie.getTicksFrozen() == 0, "leather boots should keep a freezing hit off too");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void summonedFrozenZombieIsNeverIcebound(GameTestHelper helper) {
        shade(helper);
        helper.setBlock(4, 0, 4, Blocks.ICE);
        FrozenZombie raised = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos on = helper.absolutePos(new BlockPos(4, 1, 4));
        raised.moveTo(on.getX() + 0.5, on.getY(), on.getZ() + 0.5, 0.0F, 0.0F);
        raised.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(on), MobSpawnType.MOB_SUMMONED, null);
        helper.assertTrue(!raised.isIcebound(), "a zombie the lich raised on ice froze into it");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "lichChannel")
    public static void hittingTheLichBreaksItsSummon(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 4.0F, 1.5F);
        lich.setTarget(pig);
        long[] hitAt = {-1};
        boolean[] minions = {false};
        helper.onEachTick(() -> {
            if (hitAt[0] < 0 && lich.getAction() == IceLich.ACTION_SUMMON) {
                lich.hurt(helper.getLevel().damageSources().mobAttack(pig), 1.0F);
                hitAt[0] = helper.getTick();
            }
            minions[0] |= !helper.getLevel().getEntitiesOfClass(Mob.class, lich.getBoundingBox().inflate(32), lich::isOwnMinion).isEmpty();
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(hitAt[0] >= 0, "lich never began a summon");
            helper.assertTrue(helper.getTick() > hitAt[0] + IceLich.SUMMON_CHANNEL + 5, "waiting out the channel");
            helper.assertTrue(!minions[0], "the summon went through despite the hit");
            helper.assertTrue(lich.getAction() != IceLich.ACTION_SUMMON, "still channelling");
        });
    }

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

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "lichSoul")
    public static void inAnAwakeCryptTheLichWaitsForItsSoul(GameTestHelper helper) {
        shade(helper);
        placePhylactery(helper, new BlockPos(4, 1, 1), Direction.SOUTH);
        Player intruder = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        long[] killedAt = {-1};
        long[] arrivedAt = {-1};
        boolean[] early = {false};
        UUID[] first = {null};
        helper.onEachTick(() -> {
            List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            boolean wisp = !helper.getEntities(WildspellMobs.LICH_WISP.get()).isEmpty();
            if (killedAt[0] < 0 && liches.size() == 1) {
                IceLich lich = liches.getFirst();
                first[0] = lich.getUUID();
                Vec3 far = helper.absoluteVec(new Vec3(4.5, 3.0, 8.5));
                lich.teleportTo(far.x, far.y, far.z);
                lich.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
                killedAt[0] = helper.getTick();
            } else if (killedAt[0] >= 0) {
                early[0] |= wisp && !liches.isEmpty();
                if (arrivedAt[0] < 0 && !wisp) {
                    arrivedAt[0] = helper.getTick();
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!early[0], "the lich re-formed while its soul was still flying home");
            List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(arrivedAt[0] >= 0 && liches.size() == 1 && !liches.getFirst().getUUID().equals(first[0]), "not re-formed yet");
            helper.assertTrue(helper.getTick() - arrivedAt[0] >= PhylacteryBlockEntity.REFORM_TICKS_AWAKE - 1,
                    "re-formed " + (helper.getTick() - arrivedAt[0]) + " ticks after its soul arrived");
            intruder.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichStale")
    public static void aStaleCopyOfTheLichFadesAway(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        IceLich stale = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new Vec3(2.5, 3.0, 7.5)), null);
        IceLich current = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new Vec3(6.5, 3.0, 7.5)), null);
        helper.succeedWhen(() -> {
            helper.assertTrue(stale.isRemoved(), "the replaced lich is still around");
            helper.assertTrue(current.isAlive() && current.getUUID().equals(phylactery.lichId()), "the phylactery's own lich went too");
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
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichTaken")
    public static void takingThePhylacteryCarriesItsSoul(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        List<ItemEntity> items = helper.getEntities(EntityType.ITEM);
        helper.assertTrue(items.size() == 1 && soul.id.equals(items.getFirst().getItem().get(WildspellMobs.SOUL.get())),
                "taking the phylactery should leave it as an item holding its soul");
        helper.assertTrue(lich.isBound() && !soul.inAltar(), "its lich should still be bound, to a phylactery off its altar");
        helper.assertTrue(!items.getFirst().getItem().getItem().canBeHurtBy(items.getFirst().getItem(), helper.getLevel().damageSources().cactus()),
                "only fire should harm a phylactery");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichBurn")
    public static void burningThePhylacteryRaisesItsLastForm(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        ItemEntity item = helper.getEntities(EntityType.ITEM).getFirst();
        item.hurt(helper.getLevel().damageSources().lava(), 10.0F);
        helper.assertTrue(soul.burned(), "the phylactery didn't burn");
        helper.assertTrue(lich.isRemoved(), "its old body should be torn away to the flames");
        List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
        helper.assertTrue(liches.size() == 1 && liches.getFirst().isLastForm() && !liches.getFirst().isBound() && liches.getFirst().isEnraged(),
                "expected one last form, mortal and enraged");
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
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 7.5F, 1.0F, 7.5F);
        zombie.setNoAi(true);
        ChunkPos center = new ChunkPos(helper.absolutePos(altar));
        forceChunks(helper, center, true);
        Runnable cleanup = () -> {
            LichSouls.get(helper.getLevel()).forgetCleansed(helper.getLevel(), helper.absolutePos(altar));
            forceChunks(helper, center, false);
        };
        helper.runAtTickTime(199, cleanup);
        helper.destroyBlock(altar);
        helper.getEntities(EntityType.ITEM).getFirst().hurt(helper.getLevel().damageSources().lava(), 10.0F);
        IceLich last = helper.getEntities(WildspellMobs.ICE_LICH.get()).getFirst();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        last.setHealth(1.0F);
        last.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(last.isDeadOrDying(), "the last form survived");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(ItemEntity.class, last.getBoundingBox().inflate(16.0),
                    i -> i.getItem().is(WildspellMobs.FROSTBOUND_STAFF.get())).isEmpty(), "no staff; items near: " + helper.getLevel().getEntitiesOfClass(
                    ItemEntity.class, last.getBoundingBox().inflate(16.0)).stream().map(i -> i.getItem() + "@" + helper.relativeVec(i.position())).toList()
                    + " last form at " + helper.relativeVec(last.position()) + " lastHurtByPlayer=" + last.getLastHurtByMob());
            helper.assertTrue(!zombie.isAlive(), "the cold's undead nearby didn't crumble");
            helper.assertBlockPresent(Blocks.CHEST, altar);
            helper.assertTrue(helper.getBlockState(brazier).is(Blocks.CAMPFIRE) && helper.getBlockState(brazier).getValue(CampfireBlock.LIT),
                    "the soul-fire brazier didn't turn to ordinary fire");
            helper.assertBlockNotPresent(Blocks.SNOW, new BlockPos(1, 1, 1));
            helper.assertTrue(LichSouls.isCleansedZone(helper.getLevel(), helper.absolutePos(altar)), "the crypt isn't a safe zone");
            helper.assertTrue(LichSouls.get(helper.getLevel()).soul(soul.id) == null, "a cleansed soul is still on record");
            cleanup.run();
        });
    }

    private static void forceChunks(GameTestHelper helper, ChunkPos center, boolean forced) {
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                helper.getLevel().setChunkForced(center.x + dx, center.z + dz, forced);
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

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichBearer")
    public static void theLichHuntsWhoeverBearsItsPhylactery(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        helper.getEntities(EntityType.ITEM).forEach(Entity::discard);
        Player bearer = addMockPlayer(helper, new Vec3(1.5, 1.0, 1.5));
        bearer.getInventory().add(PhylacteryItem.bound(soul.id));
        helper.succeedWhen(() -> {
            helper.assertTrue(bearer.getUUID().equals(soul.carrier()), "the soul doesn't know who carries its phylactery");
            helper.assertTrue(lich.getTarget() == bearer, "the lich isn't hunting the bearer");
            bearer.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichWards")
    public static void wardsKeepThePhylacteryWhole(GameTestHelper helper) {
        BlockPos at = new BlockPos(4, 1, 4);
        placePhylactery(helper, at, Direction.SOUTH);
        helper.setBlock(new BlockPos(6, 1, 6), WildspellMobs.RIME_WARD.get());
        Player miner = addMockPlayer(helper, new Vec3(4.5, 1.0, 1.5));
        boolean[] wasWarded = {false};
        helper.onEachTick(() -> {
            BlockState state = helper.getBlockState(at);
            if (!wasWarded[0] && state.getValue(PhylacteryBlock.WARDED)) {
                wasWarded[0] = true;
                helper.assertTrue(state.getDestroyProgress(miner, helper.getLevel(), helper.absolutePos(at)) == 0.0F, "a warded phylactery can be mined");
                helper.setBlock(new BlockPos(6, 1, 6), Blocks.AIR);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(wasWarded[0], "phylactery never noticed its ward");
            BlockState state = helper.getBlockState(at);
            helper.assertTrue(!state.getValue(PhylacteryBlock.WARDED), "still warded with its ward broken");
            helper.assertTrue(state.getDestroyProgress(miner, helper.getLevel(), helper.absolutePos(at)) > 0.0F, "unwarded phylactery can't be mined");
            miner.discard();
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

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichAmbush")
    public static void lichAmbushesAPlayerNearItsCrypt(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 0), Direction.NORTH);
        Player player = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        helper.runAtTickTime(150, () -> SpawnBalance.LICH_AMBUSH_CHANCE.set(chance));
        // The roll runs from the soul's tick over level.players(), which a mock player is not in: hand it the player.
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
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            player.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 160, batch = "lichAmbushRange")
    public static void lichRespectsItsAmbushRange(GameTestHelper helper) {
        paintFrostedCaves(helper);
        placePhylactery(helper, new BlockPos(4, 1, 0), Direction.NORTH);
        Player player = addMockPlayer(helper, new Vec3(4.5, 1.0, 6.5));
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        int range = SpawnBalance.LICH_AMBUSH_RANGE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        SpawnBalance.LICH_AMBUSH_RANGE.set(4);
        helper.assertTrue(PhylacteryBlockEntity.leash() == 4.0, "the chase ends at the same range");
        Runnable restore = () -> {
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            SpawnBalance.LICH_AMBUSH_RANGE.set(range);
        };
        helper.runAtTickTime(150, restore);
        PhylacteryBlockEntity phylactery = (PhylacteryBlockEntity) helper.getBlockEntity(new BlockPos(4, 1, 0));
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), List.of(phylactery.soul(helper.getLevel())), List.of(player));
            }
        });
        helper.runAfterDelay(100, () -> {
            try {
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(IceLich.class, player.getBoundingBox().inflate(24.0)).isEmpty(),
                        "six blocks out with a range of four, and something rose");
            } finally {
                restore.run();
                player.discard();
            }
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichNearestCrypt")
    public static void nearestCryptClaimsThePlayer(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity far = placePhylactery(helper, new BlockPos(1, 1, 0), Direction.NORTH);
        PhylacteryBlockEntity near = placePhylactery(helper, new BlockPos(7, 1, 0), Direction.NORTH);
        Player player = addMockPlayer(helper, new Vec3(7.5, 1.0, 7.5));
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        helper.runAtTickTime(190, () -> SpawnBalance.LICH_AMBUSH_CHANCE.set(chance));
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), List.of(far.soul(helper.getLevel()), near.soul(helper.getLevel())), List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(near.lichId() != null, "the nearer crypt's lich has not risen");
            helper.assertTrue(far.lichId() == null, "the farther crypt's lich rose too");
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            player.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 120)
    public static void chillFadesOutsideTheColdButFireClearsIt(GameTestHelper helper) {
        shade(helper);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 2.5F, 1.0F, 4.5F);
        zombie.setNoAi(true);
        zombie.setBaby(true);
        zombie.getPersistentData().putInt("wildspellmobs:chill", 100);
        Zombie burning = helper.spawn(EntityType.ZOMBIE, 6.5F, 1.0F, 4.5F);
        burning.setNoAi(true);
        burning.getPersistentData().putInt("wildspellmobs:chill", 100);
        burning.setRemainingFireTicks(400);
        helper.runAfterDelay(45, () -> {
            int chill = zombie.getPersistentData().getInt("wildspellmobs:chill");
            helper.assertTrue(chill > 40 && chill < 100, "out of the cold the chill fades, not vanishes: " + chill);
            helper.assertTrue(zombie.isBaby(), "still a baby");
            helper.assertFalse(burning.getPersistentData().contains("wildspellmobs:chill"), "fire clears it outright");
            helper.killAllEntities();
            helper.succeed();
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
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        LichSouls.ASSUME_BODY_AWAY_FOR_TEST = true;
        Runnable restore = () -> {
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            LichSouls.ASSUME_BODY_AWAY_FOR_TEST = false;
        };
        helper.runAtTickTime(190, restore);
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
            restore.run();
            player.discard();
            helper.killAllEntities();
        });
    }

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

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void cryptStructureIsRegistered(GameTestHelper helper) {
        var structures = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        helper.assertTrue(structures.containsKey(WildspellMobs.id("lich_crypt")), "lich_crypt structure missing");
        var sets = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET);
        helper.assertTrue(sets.containsKey(WildspellMobs.id("lich_crypt")), "lich_crypt structure set missing");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void rimeSkullsSpawnOverIceWhereGroundMobsCannot(GameTestHelper helper) {
        shade(helper);
        sealCave(helper);
        helper.setBlock(4, 0, 4, Blocks.ICE);
        BlockPos spot = new BlockPos(4, 1, 4);
        whenSealed(helper, spot, () -> {
            BlockPos at = helper.absolutePos(spot);
            helper.assertTrue(SpawnPlacements.checkSpawnRules(WildspellMobs.RIME_SKULL.get(), helper.getLevel(),
                    MobSpawnType.NATURAL, at, helper.getLevel().random), "a rime skull can't spawn over ice");
            helper.assertTrue(!SpawnPlacements.checkSpawnRules(EntityType.ZOMBIE, helper.getLevel(),
                    MobSpawnType.NATURAL, at, helper.getLevel().random), "a zombie spawned on ice: the test isn't testing the ice");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void soulseekerPointsToItsLichsPhylactery(GameTestHelper helper) {
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), Direction.SOUTH);
        LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack seeker = new ItemStack(WildspellMobs.SOULSEEKER.get());
        seeker.set(WildspellMobs.SOUL.get(), soul.id);
        player.setItemInHand(InteractionHand.MAIN_HAND, seeker);
        seeker.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        var tracker = seeker.get(DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().map(t -> t.pos().equals(helper.absolutePos(new BlockPos(4, 1, 4)))).orElse(false),
                "the needle doesn't point at the phylactery: " + tracker);
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(WildspellMobs.id("soulseeker")).isPresent(), "soulseeker recipe missing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anUnboundSoulseekerSpins(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack seeker = new ItemStack(WildspellMobs.SOULSEEKER.get());
        seeker.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(
                Optional.of(GlobalPos.of(helper.getLevel().dimension(), BlockPos.ZERO)), false));
        player.setItemInHand(InteractionHand.MAIN_HAND, seeker);
        seeker.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!seeker.has(DataComponents.LODESTONE_TRACKER), "the needle kept pointing at an old target");
        helper.succeed();
    }

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

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "fireThaw")
    public static void aBurningFrozenZombieMeltsSlowly(GameTestHelper helper) {
        shade(helper);
        FrozenZombie frozen = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 4.5F, 1.0F, 4.5F);
        frozen.setNoAi(true);
        frozen.igniteForSeconds(15.0F);
        long start = helper.getTick();
        helper.succeedWhen(() -> {
            List<Zombie> zombies = helper.getEntities(EntityType.ZOMBIE);
            helper.assertTrue(!frozen.isAlive() && zombies.size() == 1, "the burning frozen zombie hasn't melted");
            helper.assertTrue(helper.getTick() - start >= FrozenZombie.THAW_TICKS / 2 - 1, "it melted at once, not slowly");
            helper.assertTrue(zombies.getFirst().isOnFire(), "it stopped burning as it thawed");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "fireFear")
    public static void frozenZombiesFleeFire(GameTestHelper helper) {
        shade(helper);
        BlockPos fire = new BlockPos(1, 1, 1);
        helper.setBlock(fire, Blocks.CAMPFIRE);
        FrozenZombie frozen = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 2.5F, 1.0F, 2.5F);
        frozen.setVariant(0);
        Vec3 flame = helper.absoluteVec(Vec3.atCenterOf(fire));
        double[] farthest = {0.0};
        helper.onEachTick(() -> farthest[0] = Math.max(farthest[0], frozen.position().distanceTo(flame)));
        helper.succeedWhen(() -> helper.assertTrue(farthest[0] > 4.0, "it hasn't backed away from the fire: " + farthest[0]));
    }

    @GameTest(template = ARENA, timeoutTicks = 400, batch = "sunSkull")
    public static void rimeSkullsBurnInTheSun(GameTestHelper helper) {
        long time = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(6000);
        openToTheSky(helper);
        RimeSkull skull = helper.spawn(WildspellMobs.RIME_SKULL.get(), 4.5F, 2.0F, 4.5F);
        skull.setNoAi(true);
        helper.succeedWhen(() -> {
            helper.assertTrue(skull.isOnFire(), "the rime skull isn't burning in the sun");
            helper.getLevel().setDayTime(time);
        });
    }

    @GameTest(template = ARENA)
    public static void theLichHasTwoThirdsItsOldHealth(GameTestHelper helper) {
        shade(helper);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 2.0F, 4.5F);
        helper.assertTrue(lich.getMaxHealth() == 120.0F, "lich max health is " + lich.getMaxHealth());
        lich.discard();
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void frozenSoulsFeedSoulFire(GameTestHelper helper) {
        helper.setBlock(new BlockPos(4, 1, 4), WildspellMobs.FROZEN_SOUL.get());
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(4, 1, 4)), true);
        helper.assertItemEntityPresent(WildspellMobs.FROZEN_SOUL_ITEM.get());
        ItemStack soul = new ItemStack(WildspellMobs.FROZEN_SOUL_ITEM.get());
        var torch = CraftingInput.of(1, 3, List.of(new ItemStack(Items.COAL), new ItemStack(Items.STICK), soul));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, torch, helper.getLevel())
                .map(r -> r.value().getResultItem(helper.getLevel().registryAccess()).is(Items.SOUL_TORCH)).orElse(false), "a frozen soul doesn't make a soul torch");
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack stick = new ItemStack(Items.STICK);
        var campfire = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, stick, ItemStack.EMPTY, stick, soul, stick, log, log, log));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, campfire, helper.getLevel())
                .map(r -> r.value().getResultItem(helper.getLevel().registryAccess()).is(Items.SOUL_CAMPFIRE)).orElse(false), "a frozen soul doesn't make a soul campfire");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void eternalDamnationIsASecretChallenge(GameTestHelper helper) {
        var advancement = helper.getLevel().getServer().getAdvancements().get(WildspellMobs.id("eternal_damnation"));
        helper.assertTrue(advancement != null, "the eternal damnation advancement didn't load");
        var display = advancement.value().display().orElseThrow();
        helper.assertTrue(display.isHidden() && display.getType() == AdvancementType.CHALLENGE, "it should be a hidden challenge");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 500, batch = "lichSpin")
    public static void theLichSpinsUpAFrostOrb(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 1.5F, 1.0F, 1.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 7.5F, 3.0F, 7.5F);
        lich.setTarget(pig);
        boolean[] spun = {false};
        helper.onEachTick(() -> spun[0] |= lich.getAction() == IceLich.ACTION_SPIN);
        helper.succeedWhen(() -> {
            helper.assertTrue(spun[0], "the lich never spun its staff");
            helper.assertEntityPresent(WildspellMobs.FROST_ORB.get());
        });
        helper.assertTrue(WildspellMobs.FROST_ORB.get().is(EntityTypeTags.REDIRECTABLE_PROJECTILE), "a frost orb can't be struck back");
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

    @GameTest(template = ARENA)
    public static void frostboundStaffFiresAShard(GameTestHelper helper) {
        Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack staff = new ItemStack(WildspellMobs.FROSTBOUND_STAFF.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        staff.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(FrostShard.class, player.getBoundingBox().inflate(4)).isEmpty(),
                "staff fired nothing");
        helper.assertTrue(staff.getDamageValue() == 1, "staff took no wear");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rimeSkullsSpawnInEveryVariantAndKeepIt(GameTestHelper helper) {
        shade(helper);
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 60; ++i) {
            RimeSkull skull = WildspellMobs.RIME_SKULL.get().create(helper.getLevel());
            skull.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), MobSpawnType.NATURAL, null);
            seen.add(skull.getVariant());
            RimeSkull reloaded = WildspellMobs.RIME_SKULL.get().create(helper.getLevel());
            reloaded.load(skull.saveWithoutId(new CompoundTag()));
            helper.assertTrue(reloaded.getVariant() == skull.getVariant(), "variant not saved: " + skull.getVariant() + " -> " + reloaded.getVariant());
        }
        helper.assertTrue(seen.size() == RimeSkull.VARIANTS, "only saw variants " + seen);
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void frozenZombieLurchesAndSeizes(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 8.5F, 1.0F, 8.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 0.5F, 1.0F, 0.5F);
        zombie.setTarget(pig);
        double startDistance = zombie.distanceTo(pig);
        double[] last = {zombie.getX(), zombie.getZ()};
        int[] ticks = new int[2];
        double[] distance = new double[2];
        helper.onEachTick(() -> {
            double moved = Math.hypot(zombie.getX() - last[0], zombie.getZ() - last[1]);
            last[0] = zombie.getX();
            last[1] = zombie.getZ();
            int state = zombie.isSeized() ? 0 : 1;
            ++ticks[state];
            distance[state] += moved;
        });
        helper.runAfterDelay(200, () -> {
            double seizedPace = distance[0] / Math.max(1, ticks[0]);
            double movingPace = distance[1] / Math.max(1, ticks[1]);
            String stats = String.format("seized=%d ticks @ %.3f/t, moving=%d ticks @ %.3f/t", ticks[0], seizedPace, ticks[1], movingPace);
            helper.assertTrue(ticks[0] > 30 && ticks[1] > 60, "gait never alternated: " + stats);
            helper.assertTrue(movingPace > 2 * seizedPace, "seizing doesn't stall it: " + stats);
            helper.assertTrue(movingPace < 0.12, "moving too fast for a frozen body: " + stats);
            helper.assertTrue(zombie.distanceTo(pig) < startDistance - 2.0, "made no progress toward its target: " + stats);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void frozenZombieHitFreezes(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 2.5F, 1.0F, 4.5F);
        zombie.setTarget(pig);
        helper.succeedWhen(() -> {
            helper.assertTrue(pig.getHealth() < pig.getMaxHealth(), "pig not hit yet");
            helper.assertTrue(pig.getTicksFrozen() > 0 && pig.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "hit did not freeze and slow");
        });
    }

    @GameTest(template = ARENA)
    public static void creepersAreRareInFrostedCaves(GameTestHelper helper) {
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(FROSTED_CAVES);
        helper.assertTrue(biome != null, "yungscavebiomes:frosted_caves is not loaded");
        List<MobSpawnSettings.SpawnerData> monsters = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        List<String> creepers = monsters.stream()
                .filter(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getPath().contains("creeper"))
                .map(data -> BuiltInRegistries.ENTITY_TYPE.getKey(data.type) + " " + data.getWeight().asInt() + "x" + data.maxCount)
                .toList();
        String expected = ModList.get().isLoaded("creeperoverhaul") ? "creeperoverhaul:snowy_creeper 3x1" : "minecraft:creeper 3x1";
        helper.assertTrue(creepers.equals(List.of(expected)), "frosted caves creepers: " + creepers + ", expected only " + expected);
        helper.succeed();
    }

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
            helper.assertTrue(Math.abs(creeperPass - SpawnBalance.UNDERGROUND_CREEPER_CHANCE.get()) < 0.05, "creeper pass rate " + creeperPass);
            helper.assertTrue(Math.abs(zombiePass - SpawnBalance.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.assertTrue(skullPass == 1.0, "rime skull pass rate " + skullPass);
            helper.succeed();
        });
    }

    // Tests in a batch run side by side ~14 blocks apart, inside each other's cap radius, so each
    // test that crowds its arena gets a batch of its own.
    @GameTest(template = ARENA, timeoutTicks = 100, batch = "localCap")
    public static void crowdedCaveRefusesSpawns(GameTestHelper helper) {
        shade(helper);
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < SpawnBalance.UNDERGROUND_LOCAL_CAP.get(); ++i) {
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
        for (int i = 0; i < SpawnBalance.UNDERGROUND_LOCAL_CAP.get(); ++i) {
            helper.spawn(EntityType.ZOMBIE, new BlockPos(3 + i % 3, 1, 3 + i / 3 % 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            helper.assertTrue(Math.abs(zombiePass - SpawnBalance.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.succeed();
        });
    }

    private static <T extends Mob> T spawnWild(GameTestHelper helper, EntityType<T> type, BlockPos relative) {
        T mob = type.create(helper.getLevel());
        Vec3 at = helper.absoluteVec(Vec3.atBottomCenterOf(relative));
        mob.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
        helper.getLevel().addFreshEntity(mob);
        return mob;
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "creeperCap")
    public static void creeperCapRefusesOnlyCreepers(GameTestHelper helper) {
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < SpawnBalance.UNDERGROUND_CREEPER_CAP.get(); ++i) {
            spawnWild(helper, EntityType.CREEPER, new BlockPos(3 + i, 1, 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double creeperPass = passRate(helper, EntityType.CREEPER, spot);
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            helper.assertTrue(creeperPass == 0.0, "creeper pass rate with the creeper cap full " + creeperPass);
            helper.assertTrue(Math.abs(zombiePass - SpawnBalance.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.succeed();
        });
    }

    private static Player addMockPlayer(GameTestHelper helper, Vec3 at) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setInvulnerable(true);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    private static void shade(GameTestHelper helper) {
        for (int x = -1; x <= 9; ++x) {
            for (int z = -1; z <= 9; ++z) {
                helper.setBlock(x, 12, z, Blocks.STONE);
            }
        }
    }

    private static void openToTheSky(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int z = 2; z <= 6; ++z) {
                for (int y = 6; y <= 9; ++y) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    private static PhylacteryBlockEntity placePhylactery(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.setBlock(pos, WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get().defaultBlockState().setValue(PhylacteryBlock.FACING, facing));
        return (PhylacteryBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    // getBiome samples a jittered position that can fall in a neighbouring 4x4x4 cell, so paint a margin;
    // fillbiome can fail before the chunks are ready, so keep applying it.
    private static void paintFrostedCaves(GameTestHelper helper) {
        BlockPos from = helper.absolutePos(new BlockPos(-2, -2, -2));
        BlockPos to = helper.absolutePos(new BlockPos(10, 6, 10));
        String fill = "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " " + FROSTED_CAVES.location();
        helper.onEachTick(() -> {
            if (!helper.getLevel().getBiome(helper.absolutePos(new BlockPos(4, 1, 4))).is(ZombieFreezing.FREEZES_ZOMBIES)) {
                helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), fill);
            }
        });
    }

    private static void whenSealed(GameTestHelper helper, BlockPos spot, Runnable checks) {
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (!done[0] && helper.getLevel().getBrightness(LightLayer.SKY, helper.absolutePos(spot)) == 0) {
                done[0] = true;
                checks.run();
            }
        });
    }

    private static void sealCave(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int y = 0; y <= 4; ++y) {
                for (int z = 2; z <= 6; ++z) {
                    boolean shell = x == 2 || x == 6 || y == 0 || y == 4 || z == 2 || z == 6;
                    helper.setBlock(x, y, z, shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static String blocker(GameTestHelper helper, RimeSkull skull, Pig pig) {
        Vec3 from = new Vec3(skull.getX(), skull.getEyeY(), skull.getZ());
        Vec3 to = new Vec3(pig.getX(), pig.getEyeY(), pig.getZ());
        BlockHitResult hit = helper.getLevel().clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, skull));
        return hit.getType() + "@" + helper.relativePos(hit.getBlockPos()) + "=" + helper.getLevel().getBlockState(hit.getBlockPos());
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
