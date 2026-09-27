package com.wildspell.mobs;

import com.wildspell.mobs.crypt.PhylacteryBlock;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import com.wildspell.mobs.entity.Frost;
import com.wildspell.mobs.entity.FrostShard;
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.RimeSkull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** In-world checks, run with ./gradlew runGameTestServer. Only loaded when gametests are enabled. */
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class WildspellMobsTests {
    private static final String ARENA = "arena";
    private static final ResourceKey<Biome> FROSTED_CAVES = ResourceKey.create(Registries.BIOME,
            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frosted_caves"));

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void skullHoversWithoutFalling(GameTestHelper helper) {
        shade(helper);
        // AI off so nothing steers it: any drop would be gravity.
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
        pig.setInvulnerable(true);
        RimeSkull skull = helper.spawn(WildspellMobs.RIME_SKULL.get(), 0.5F, 4.0F, 0.5F);
        skull.setTarget(pig);
        int[] shards = {0};
        java.util.Set<Integer> seen = new java.util.HashSet<>();
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

    // Runs alone so the painted biome can't reach into a neighbouring test's arena.
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "freezing")
    public static void zombieFreezesInFrostedCaves(GameTestHelper helper) {
        shade(helper);
        paintFrostedCaves(helper);
        helper.setBlock(4, 0, 4, Blocks.ICE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, 4.5F, 1.0F, 4.5F);
        zombie.setNoAi(true);
        zombie.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        // Entities in a freshly placed arena can start ticking a few ticks late, so watch every tick
        // rather than checking at fixed times.
        boolean[] shivered = {false};
        helper.onEachTick(() -> shivered[0] |= zombie.isAlive() && zombie.isFullyFrozen());
        helper.succeedWhen(() -> {
            helper.assertTrue(!zombie.isAlive(), "zombie not converted; biome=" + helper.getLevel().getBiome(zombie.blockPosition()).getRegisteredName()
                    + " chill=" + zombie.getPersistentData() + " pos=" + helper.relativeVec(zombie.position()));
            helper.assertEntityNotPresent(EntityType.ZOMBIE);
            java.util.List<FrozenZombie> frozen = helper.getEntities(WildspellMobs.FROZEN_ZOMBIE.get());
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
        String mix = "off ice " + java.util.Arrays.toString(offIce) + ", on ice " + java.util.Arrays.toString(onIce);
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
        reloaded.load(zombie.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(reloaded.isIcebound() && reloaded.getY() == zombie.getY(), "ice-bound state not saved");
        helper.assertTrue(reloaded.hasWholeFace(), "whole face not saved");
        helper.assertTrue(reloaded.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.0,
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
        net.minecraft.world.phys.Vec3 stuckAt = zombie.position();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(zombie.isIcebound() && zombie.position().distanceTo(stuckAt) < 1.0E-3,
                    "moved while stuck in the ice: " + helper.relativeVec(zombie.position()));
            helper.setBlock(4, 0, 4, Blocks.AIR);
        });
        helper.runAfterDelay(100, () -> {
            helper.assertTrue(zombie.getVariant() == FrozenZombie.NORMAL, "still ice-bound after its ice broke");
            // Sunk 0.75 into a floor block; with that block gone it drops the last 0.25 onto the block below.
            helper.assertTrue(zombie.onGround() && zombie.getY() < stuckAt.y - 0.2, "didn't drop out of the broken ice: " + helper.relativeVec(zombie.position()));
            helper.succeed();
        });
    }

    /** Spawns a Frozen Zombie at {@code relative} and returns its variant, counting whole faces in {@code wholeFaces[0]}. */
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
        pig.setInvulnerable(true);
        helper.setBlock(1, 0, 1, Blocks.ICE);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 1.5F, 1.0F, 1.5F);
        zombie.pickVariant();
        zombie.setTarget(pig);
        boolean[] threwSnowball = {false};
        double[] closest = {Double.MAX_VALUE};
        java.util.Map<Integer, net.minecraft.world.phys.Vec3> flying = new java.util.HashMap<>();
        java.util.List<String> landed = new java.util.ArrayList<>();
        helper.onEachTick(() -> {
            closest[0] = Math.min(closest[0], zombie.distanceTo(pig));
            java.util.Set<Integer> now = new java.util.HashSet<>();
            for (var s : helper.getEntities(WildspellMobs.FROST_SHARD.get())) {
                threwSnowball[0] |= s.getItem().is(Items.SNOWBALL);
                now.add(s.getId());
                flying.put(s.getId(), helper.relativeVec(s.position()));
            }
            flying.keySet().removeIf(id -> {
                if (!now.contains(id)) {
                    net.minecraft.world.phys.Vec3 v = flying.get(id);
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
        EntityType<?> iceCubeType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "ice_cube"));
        net.minecraft.world.entity.Entity cube = helper.spawn(iceCubeType, 4.5F, 1.0F, 4.5F);
        var table = helper.getLevel().getServer().reloadableRegistries().getLootTable(((net.minecraft.world.entity.LivingEntity) cube).getLootTable());
        int ice = 0;
        for (int i = 0; i < 40; ++i) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, cube)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, cube.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE, helper.getLevel().damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
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
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        IceLich lich = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 7.5)), null);
        java.util.UUID first = lich.getUUID();
        lich.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
        helper.assertTrue(lich.isRemoved(), "a bound lich stayed after being struck down");
        helper.assertEntityPresent(WildspellMobs.LICH_WISP.get());
        helper.assertItemEntityNotPresent(WildspellMobs.FROSTBOUND_STAFF.get());
        helper.succeedWhen(() -> {
            java.util.List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
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
        placePhylactery(helper, new BlockPos(4, 1, 1), net.minecraft.core.Direction.SOUTH);
        net.minecraft.world.entity.player.Player intruder = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 6.5));
        long[] killedAt = {-1};
        long[] arrivedAt = {-1};
        boolean[] early = {false};
        java.util.UUID[] first = {null};
        helper.onEachTick(() -> {
            java.util.List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            boolean wisp = !helper.getEntities(WildspellMobs.LICH_WISP.get()).isEmpty();
            if (killedAt[0] < 0 && liches.size() == 1) {
                // Strike it down across the crypt, so its soul has a way to fly home.
                IceLich lich = liches.getFirst();
                first[0] = lich.getUUID();
                net.minecraft.world.phys.Vec3 far = helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 8.5));
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
            java.util.List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(arrivedAt[0] >= 0 && liches.size() == 1 && !liches.getFirst().getUUID().equals(first[0]), "not re-formed yet");
            helper.assertTrue(helper.getTick() - arrivedAt[0] >= PhylacteryBlockEntity.REFORM_TICKS_AWAKE - 1,
                    "re-formed " + (helper.getTick() - arrivedAt[0]) + " ticks after its soul arrived");
            intruder.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichStale")
    public static void aStaleCopyOfTheLichFadesAway(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        IceLich stale = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 3.0, 7.5)), null);
        IceLich current = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(6.5, 3.0, 7.5)), null);
        helper.succeedWhen(() -> {
            helper.assertTrue(stale.isRemoved(), "the replaced lich is still around");
            helper.assertTrue(current.isAlive() && current.getUUID().equals(phylactery.lichId()), "the phylactery's own lich went too");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichCrown")
    public static void strikingDownTheLichLeavesACrownFragment(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        IceLich lich = phylactery.soul(helper.getLevel()).raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 7.5)), null);
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        lich.setHealth(1.0F);
        lich.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
        helper.assertTrue(lich.isRemoved(), "the lich wasn't struck down");
        helper.assertItemEntityPresent(WildspellMobs.CROWN_FRAGMENT.get());
        helper.assertItemEntityNotPresent(WildspellMobs.FROSTBOUND_STAFF.get());
        ItemStack fragment = helper.getEntities(EntityType.ITEM).stream().map(net.minecraft.world.entity.item.ItemEntity::getItem)
                .filter(stack -> stack.is(WildspellMobs.CROWN_FRAGMENT.get())).findFirst().orElseThrow();
        helper.assertTrue(phylactery.soulId().equals(fragment.get(WildspellMobs.SOUL.get())), "the fragment isn't bound to the lich's soul");
        // Made into a Soulseeker, the fragment's soul goes with it.
        ItemStack seeker = new ItemStack(WildspellMobs.SOULSEEKER.get());
        net.minecraft.world.SimpleContainer grid = new net.minecraft.world.SimpleContainer(9);
        grid.setItem(4, fragment);
        com.wildspell.mobs.item.SoulseekerItem.onCrafted(new net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent(player, seeker, grid));
        helper.assertTrue(phylactery.soulId().equals(seeker.get(WildspellMobs.SOUL.get())), "the Soulseeker didn't take the fragment's soul");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichTaken")
    public static void takingThePhylacteryCarriesItsSoul(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        java.util.List<net.minecraft.world.entity.item.ItemEntity> items = helper.getEntities(EntityType.ITEM);
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
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        net.minecraft.world.entity.item.ItemEntity item = helper.getEntities(EntityType.ITEM).getFirst();
        item.hurt(helper.getLevel().damageSources().lava(), 10.0F);
        helper.assertTrue(soul.burned(), "the phylactery didn't burn");
        helper.assertTrue(lich.isRemoved(), "its old body should be torn away to the flames");
        java.util.List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
        helper.assertTrue(liches.size() == 1 && liches.getFirst().isLastForm() && !liches.getFirst().isBound() && liches.getFirst().isEnraged(),
                "expected one last form, mortal and enraged");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichFall")
    public static void theLastFormsFallCleansesItsCrypt(GameTestHelper helper) {
        shade(helper);
        BlockPos altar = new BlockPos(4, 1, 4);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, altar, net.minecraft.core.Direction.SOUTH);
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        BlockPos brazier = new BlockPos(4, 1, 8);
        helper.setBlock(brazier, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.SNOW);
        FrozenZombie zombie = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 7.5F, 1.0F, 7.5F);
        zombie.setNoAi(true);
        // A crypt is cleansed once the area around it is loaded, as it is with a player nearby.
        net.minecraft.world.level.ChunkPos center = new net.minecraft.world.level.ChunkPos(helper.absolutePos(altar));
        for (int dx = -3; dx <= 3; ++dx) {
            for (int dz = -3; dz <= 3; ++dz) {
                helper.getLevel().setChunkForced(center.x + dx, center.z + dz, true);
            }
        }
        helper.destroyBlock(altar);
        helper.getEntities(EntityType.ITEM).getFirst().hurt(helper.getLevel().damageSources().lava(), 10.0F);
        IceLich last = helper.getEntities(WildspellMobs.ICE_LICH.get()).getFirst();
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        last.setHealth(1.0F);
        last.hurt(helper.getLevel().damageSources().playerAttack(player), 100.0F);
        helper.succeedWhen(() -> {
            helper.assertTrue(last.isDeadOrDying(), "the last form survived");
            helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, last.getBoundingBox().inflate(16.0),
                    i -> i.getItem().is(WildspellMobs.FROSTBOUND_STAFF.get())).isEmpty(), "no staff; items near: " + helper.getLevel().getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, last.getBoundingBox().inflate(16.0)).stream().map(i -> i.getItem() + "@" + helper.relativeVec(i.position())).toList()
                    + " last form at " + helper.relativeVec(last.position()) + " lastHurtByPlayer=" + last.getLastHurtByMob());
            helper.assertTrue(!zombie.isAlive(), "the cold's undead nearby didn't crumble");
            helper.assertBlockPresent(Blocks.CHEST, altar);
            helper.assertTrue(helper.getBlockState(brazier).is(Blocks.CAMPFIRE) && helper.getBlockState(brazier).getValue(net.minecraft.world.level.block.CampfireBlock.LIT),
                    "the soul-fire brazier didn't turn to ordinary fire");
            helper.assertBlockNotPresent(Blocks.SNOW, new BlockPos(1, 1, 1));
            helper.assertTrue(com.wildspell.mobs.crypt.LichSouls.isCleansedZone(helper.getLevel(), helper.absolutePos(altar)), "the crypt isn't a safe zone");
            com.wildspell.mobs.crypt.LichSouls.get(helper.getLevel()).forgetCleansed(helper.getLevel(), helper.absolutePos(altar));
            for (int dx = -3; dx <= 3; ++dx) {
                for (int dz = -3; dz <= 3; ++dz) {
                    helper.getLevel().setChunkForced(center.x + dx, center.z + dz, false);
                }
            }
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichBearer")
    public static void theLichHuntsWhoeverBearsItsPhylactery(GameTestHelper helper) {
        shade(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich lich = soul.raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 3.0, 7.5)), null);
        helper.destroyBlock(new BlockPos(4, 1, 4));
        helper.getEntities(EntityType.ITEM).forEach(net.minecraft.world.entity.Entity::discard);
        net.minecraft.world.entity.player.Player bearer = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(1.5, 1.0, 1.5));
        bearer.getInventory().add(com.wildspell.mobs.crypt.PhylacteryItem.bound(soul.id));
        helper.succeedWhen(() -> {
            helper.assertTrue(bearer.getUUID().equals(soul.carrier()), "the soul doesn't know who carries its phylactery");
            helper.assertTrue(lich.getTarget() == bearer, "the lich isn't hunting the bearer");
            bearer.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichWards")
    public static void wardsKeepThePhylacteryWhole(GameTestHelper helper) {
        BlockPos at = new BlockPos(4, 1, 4);
        placePhylactery(helper, at, net.minecraft.core.Direction.SOUTH);
        helper.setBlock(new BlockPos(6, 1, 6), WildspellMobs.RIME_WARD.get());
        // Just behind the phylactery, outside its crypt: it only bothers checking its wards with someone near.
        net.minecraft.world.entity.player.Player miner = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 1.5));
        boolean[] wasWarded = {false};
        helper.onEachTick(() -> {
            net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(at);
            if (!wasWarded[0] && state.getValue(PhylacteryBlock.WARDED)) {
                wasWarded[0] = true;
                helper.assertTrue(state.getDestroyProgress(miner, helper.getLevel(), helper.absolutePos(at)) == 0.0F, "a warded phylactery can be mined");
                helper.setBlock(new BlockPos(6, 1, 6), Blocks.AIR);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(wasWarded[0], "phylactery never noticed its ward");
            net.minecraft.world.level.block.state.BlockState state = helper.getBlockState(at);
            helper.assertTrue(!state.getValue(PhylacteryBlock.WARDED), "still warded with its ward broken");
            helper.assertTrue(state.getDestroyProgress(miner, helper.getLevel(), helper.absolutePos(at)) > 0.0F, "unwarded phylactery can't be mined");
            miner.discard();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "cryptWakes")
    public static void cryptWakesForAnIntruder(GameTestHelper helper) {
        shade(helper);
        placePhylactery(helper, new BlockPos(4, 1, 1), net.minecraft.core.Direction.SOUTH);
        BlockPos[] braziers = {new BlockPos(1, 1, 7), new BlockPos(7, 1, 7)};
        for (BlockPos b : braziers) {
            helper.setBlock(b, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
        }
        BlockPos candle = new BlockPos(4, 1, 8);
        helper.setBlock(candle, Blocks.BLUE_CANDLE.defaultBlockState().setValue(net.minecraft.world.level.block.CandleBlock.CANDLES, 4));
        net.minecraft.world.entity.player.Player intruder = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 6.5));
        helper.succeedWhen(() -> {
            for (BlockPos b : braziers) {
                helper.assertTrue(helper.getBlockState(b).getValue(net.minecraft.world.level.block.CampfireBlock.LIT), "brazier at " + b + " not lit");
            }
            helper.assertTrue(helper.getBlockState(candle).getValue(net.minecraft.world.level.block.CandleBlock.LIT), "candle not lit");
            java.util.List<IceLich> liches = helper.getEntities(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(liches.size() == 1, "the lich didn't rise to meet the intruder");
            helper.assertTrue(liches.getFirst().isBound() && liches.getFirst().getTarget() == intruder, "risen lich isn't bound and after the intruder");
            intruder.discard();
        });
    }

    // Runs alone: the painted biome, the player and a 64-block ambush range would reach other tests.
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichAmbush")
    public static void lichAmbushesAPlayerNearItsCrypt(GameTestHelper helper) {
        paintFrostedCaves(helper);
        // Facing north, the crypt lies away from the arena, so the player stands outside it.
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 0), net.minecraft.core.Direction.NORTH);
        net.minecraft.world.entity.player.Player player = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 6.5));
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        // There's no teardown hook, so put the config back before the timeout whether or not it passes.
        helper.runAtTickTime(150, () -> SpawnBalance.LICH_AMBUSH_CHANCE.set(chance));
        // The roll runs from the soul's tick over level.players(), which a mock player is not in: hand it the player.
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), java.util.List.of(phylactery.soul(helper.getLevel())), java.util.List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            java.util.List<IceLich> liches = helper.getLevel().getEntitiesOfClass(IceLich.class, player.getBoundingBox().inflate(24.0));
            helper.assertTrue(liches.size() == 1, "no ambush yet");
            IceLich lich = liches.getFirst();
            helper.assertTrue(lich.isBound() && lich.getTarget() == player && lich.getUUID().equals(phylactery.lichId()), "ambusher isn't the phylactery's lich hunting the player");
            helper.assertTrue(lich.distanceTo(player) >= 9.0, "rose right on top of the player: " + lich.distanceTo(player));
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            player.discard();
        });
    }

    /** The ambush range is a config value and a hard edge: outside it, nothing rises however sure the roll. */
    @GameTest(template = ARENA, timeoutTicks = 160, batch = "lichAmbushRange")
    public static void lichRespectsItsAmbushRange(GameTestHelper helper) {
        paintFrostedCaves(helper);
        placePhylactery(helper, new BlockPos(4, 1, 0), net.minecraft.core.Direction.NORTH);
        net.minecraft.world.entity.player.Player player = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 6.5));
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
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), java.util.List.of(phylactery.soul(helper.getLevel())), java.util.List.of(player));
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

    /** Two crypts in reach: the nearer one's lich rises, the other stays dormant. One lich to a player. */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichNearestCrypt")
    public static void nearestCryptClaimsThePlayer(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity far = placePhylactery(helper, new BlockPos(1, 1, 0), net.minecraft.core.Direction.NORTH);
        PhylacteryBlockEntity near = placePhylactery(helper, new BlockPos(7, 1, 0), net.minecraft.core.Direction.NORTH);
        net.minecraft.world.entity.player.Player player = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(7.5, 1.0, 7.5));
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        helper.runAtTickTime(190, () -> SpawnBalance.LICH_AMBUSH_CHANCE.set(chance));
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), java.util.List.of(far.soul(helper.getLevel()), near.soul(helper.getLevel())), java.util.List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(near.lichId() != null, "the nearer crypt's lich has not risen");
            helper.assertTrue(far.lichId() == null, "the farther crypt's lich rose too");
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            player.discard();
        });
    }

    /**
     * A zombie that steps out of the cold (the cave biome's edge or roof, which a chase crosses all the
     * time) loses its chill only as fast as it gained it; only fire clears it outright. Babies are
     * zombies too: nothing here checks age.
     */
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

    /**
     * A lich left standing in its crypt, then the crypt unloaded, used to keep its soul "up" forever, so
     * it never stalked anyone again. Now the soul raises a fresh body behind the player, and the old one,
     * refused by its soul when next it checks in, vanishes as a stale copy.
     */
    @GameTest(template = ARENA, timeoutTicks = 200, batch = "lichBodyAway")
    public static void lichIdleInAnUnloadedCryptStillStalks(GameTestHelper helper) {
        paintFrostedCaves(helper);
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 0), net.minecraft.core.Direction.NORTH);
        net.minecraft.world.entity.player.Player player = addMockPlayer(helper, new net.minecraft.world.phys.Vec3(4.5, 1.0, 6.5));
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        IceLich old = soul.raise(helper.getLevel(), helper.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 1.0, 1.5)), null);
        helper.assertTrue(old != null && old.getUUID().equals(phylactery.lichId()), "the first body is on record");
        old.setNoAi(true);
        double chance = SpawnBalance.LICH_AMBUSH_CHANCE.get();
        SpawnBalance.LICH_AMBUSH_CHANCE.set(1.0);
        com.wildspell.mobs.crypt.LichSouls.ASSUME_BODY_AWAY_FOR_TEST = true;
        Runnable restore = () -> {
            SpawnBalance.LICH_AMBUSH_CHANCE.set(chance);
            com.wildspell.mobs.crypt.LichSouls.ASSUME_BODY_AWAY_FOR_TEST = false;
        };
        helper.runAtTickTime(190, restore);
        helper.onEachTick(() -> {
            if (helper.getTick() % 20 == 5) {
                PhylacteryBlockEntity.ambushFromAfar(helper.getLevel(), java.util.List.of(soul), java.util.List.of(player));
            }
        });
        helper.succeedWhen(() -> {
            java.util.UUID now = phylactery.lichId();
            helper.assertTrue(now != null && !now.equals(old.getUUID()), "no fresh body has risen");
            helper.assertTrue(helper.getLevel().getEntity(now) instanceof IceLich fresh && fresh.getTarget() == player, "the fresh body isn't hunting the player");
            helper.assertFalse(old.isAlive(), "the old body should have vanished as a stale copy");
            restore.run();
            player.discard();
            helper.killAllEntities();
        });
    }

    /** YUNG's icicles hanging over lava (or beside it) melt away; ones over stone are left alone. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iciclesMeltOverLava(GameTestHelper helper) {
        shade(helper);
        net.minecraft.world.level.block.Block icicle = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "icicle"));
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
        helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 4)).is(com.wildspell.mobs.IceMelting.MELTS_NEAR_HEAT), "the icicle is meltable");
        helper.assertTrue(com.wildspell.mobs.IceMelting.melt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 4))), "over lava it melts");
        helper.assertBlockPresent(Blocks.AIR, 2, 2, 4);
        helper.assertFalse(com.wildspell.mobs.IceMelting.melt(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 4))), "over stone it stays");
        helper.assertBlockPresent(icicle, 6, 2, 4);
        // The sampler finds them too: two thousand looks into a 9x5x9 volume all but certainly land on both
        // the icicle hanging one block over magma and the one two blocks over it.
        helper.setBlock(6, 0, 4, Blocks.MAGMA_BLOCK);
        helper.setBlock(6, 1, 4, icicle);
        int melted = com.wildspell.mobs.IceMelting.sampleAround(helper.getLevel(), helper.absolutePos(new BlockPos(4, 2, 4)), 2000, 4, 2);
        helper.assertTrue(melted == 2, "the sampler melted " + melted);
        helper.assertBlockPresent(Blocks.AIR, 6, 1, 4);
        helper.assertBlockPresent(Blocks.AIR, 6, 2, 4);
        helper.succeed();
    }

    // Built well off to the side of the test grid, so the crypts can't reach, or shade, another test's arena.
    @GameTest(template = ARENA, timeoutTicks = 100, batch = "cryptGen")
    public static void cryptBuildsTheSameWayRoundInEveryOrientation(GameTestHelper helper) {
        int i = 0;
        for (net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos origin = helper.absolutePos(new BlockPos(1000 + 60 * i++, 0, 1000));
            com.wildspell.mobs.crypt.LichCryptPiece piece = new com.wildspell.mobs.crypt.LichCryptPiece(origin, facing, 6);
            piece.postProcess(helper.getLevel(), helper.getLevel().structureManager(), helper.getLevel().getChunkSource().getGenerator(),
                    helper.getLevel().random, piece.getBoundingBox(), new net.minecraft.world.level.ChunkPos(origin), origin);
            BlockPos at = piece.phylacteryPos();
            helper.assertTrue(helper.getLevel().getBlockEntity(at) instanceof PhylacteryBlockEntity, facing + ": no phylactery at " + at);
            PhylacteryBlockEntity phylactery = (PhylacteryBlockEntity) helper.getLevel().getBlockEntity(at);
            net.minecraft.world.phys.AABB crypt = phylactery.cryptBounds();
            int wards = 0, braziers = 0, candles = 0, iceFloor = 0;
            for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(crypt.minX, crypt.minY, crypt.minZ), BlockPos.containing(crypt.maxX - 1, crypt.maxY - 1, crypt.maxZ - 1))) {
                net.minecraft.world.level.block.state.BlockState state = helper.getLevel().getBlockState(p);
                wards += state.is(WildspellMobs.RIME_WARD.get()) ? 1 : 0;
                braziers += state.is(Blocks.SOUL_CAMPFIRE) && !state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT) ? 1 : 0;
                candles += state.is(Blocks.BLUE_CANDLE) && !state.getValue(net.minecraft.world.level.block.CandleBlock.LIT) ? 1 : 0;
                iceFloor += p.getY() == at.getY() - PhylacteryBlockEntity.CRYPT_BELOW && state.is(net.minecraft.tags.BlockTags.ICE) ? 1 : 0;
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
            helper.assertTrue(net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(WildspellMobs.RIME_SKULL.get(), helper.getLevel(),
                    MobSpawnType.NATURAL, at, helper.getLevel().random), "a rime skull can't spawn over ice");
            helper.assertTrue(!net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(EntityType.ZOMBIE, helper.getLevel(),
                    MobSpawnType.NATURAL, at, helper.getLevel().random), "a zombie spawned on ice: the test isn't testing the ice");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void soulseekerPointsToItsLichsPhylactery(GameTestHelper helper) {
        PhylacteryBlockEntity phylactery = placePhylactery(helper, new BlockPos(4, 1, 4), net.minecraft.core.Direction.SOUTH);
        com.wildspell.mobs.crypt.LichSouls.Soul soul = phylactery.soul(helper.getLevel());
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack seeker = new ItemStack(WildspellMobs.SOULSEEKER.get());
        seeker.set(WildspellMobs.SOUL.get(), soul.id);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, seeker);
        seeker.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        var tracker = seeker.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
        helper.assertTrue(tracker != null && tracker.target().map(t -> t.pos().equals(helper.absolutePos(new BlockPos(4, 1, 4)))).orElse(false),
                "the needle doesn't point at the phylactery: " + tracker);
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(WildspellMobs.id("soulseeker")).isPresent(), "soulseeker recipe missing");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void anUnboundSoulseekerSpins(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        ItemStack seeker = new ItemStack(WildspellMobs.SOULSEEKER.get());
        seeker.set(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER, new net.minecraft.world.item.component.LodestoneTracker(
                java.util.Optional.of(net.minecraft.core.GlobalPos.of(helper.getLevel().dimension(), BlockPos.ZERO)), false));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, seeker);
        seeker.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!seeker.has(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER), "the needle kept pointing at an old target");
        helper.succeed();
    }

    // Each sun test runs alone, and puts the clock back when it's done: noon would thaw and burn the
    // Frozen Zombies and Rime Skulls in other tests.
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "sunThaw")
    public static void frozenZombiesThawInTheSunAndThenBurn(GameTestHelper helper) {
        long time = helper.getLevel().getDayTime();
        helper.getLevel().setDayTime(6000);
        openToTheSky(helper);
        FrozenZombie frozen = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 4.5F, 1.0F, 4.5F);
        frozen.setNoAi(true);
        helper.succeedWhen(() -> {
            java.util.List<Zombie> zombies = helper.getEntities(EntityType.ZOMBIE);
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
            java.util.List<Zombie> zombies = helper.getEntities(EntityType.ZOMBIE);
            helper.assertTrue(!frozen.isAlive() && zombies.size() == 1, "the burning frozen zombie hasn't melted");
            helper.assertTrue(helper.getTick() - start >= FrozenZombie.THAW_TICKS / 2 - 1, "it melted at once, not slowly");
            helper.assertTrue(zombies.getFirst().isOnFire(), "it stopped burning as it thawed");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200, batch = "fireFear")
    public static void frozenZombiesFleeFire(GameTestHelper helper) {
        shade(helper);
        // A fire in one corner, so it has the whole arena to back away across.
        BlockPos fire = new BlockPos(1, 1, 1);
        helper.setBlock(fire, Blocks.CAMPFIRE);
        FrozenZombie frozen = helper.spawn(WildspellMobs.FROZEN_ZOMBIE.get(), 2.5F, 1.0F, 2.5F);
        frozen.setVariant(0);
        net.minecraft.world.phys.Vec3 flame = helper.absoluteVec(net.minecraft.world.phys.Vec3.atCenterOf(fire));
        // It only backs off until it's out of the fire's reach (then it may drift back), so look for it
        // getting clear, not staying clear.
        double[] farthest = {0.0};
        helper.onEachTick(() -> farthest[0] = Math.max(farthest[0], frozen.position().distanceTo(flame)));
        helper.succeedWhen(() -> helper.assertTrue(farthest[0] > 4.0, "it hasn't backed away from the fire: " + farthest[0]));
    }

    // Held still under the open sky: it catches fire on a roll each sunlit tick, as skeletons do.
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
        // helper.destroyBlock drops nothing; break it the way a player's hand would.
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(4, 1, 4)), true);
        helper.assertItemEntityPresent(WildspellMobs.FROZEN_SOUL_ITEM.get());
        ItemStack soul = new ItemStack(WildspellMobs.FROZEN_SOUL_ITEM.get());
        var torch = net.minecraft.world.item.crafting.CraftingInput.of(1, 3, java.util.List.of(new ItemStack(Items.COAL), new ItemStack(Items.STICK), soul));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, torch, helper.getLevel())
                .map(r -> r.value().getResultItem(helper.getLevel().registryAccess()).is(Items.SOUL_TORCH)).orElse(false), "a frozen soul doesn't make a soul torch");
        ItemStack log = new ItemStack(Items.OAK_LOG);
        ItemStack stick = new ItemStack(Items.STICK);
        var campfire = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(ItemStack.EMPTY, stick, ItemStack.EMPTY, stick, soul, stick, log, log, log));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, campfire, helper.getLevel())
                .map(r -> r.value().getResultItem(helper.getLevel().registryAccess()).is(Items.SOUL_CAMPFIRE)).orElse(false), "a frozen soul doesn't make a soul campfire");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void eternalDamnationIsASecretChallenge(GameTestHelper helper) {
        var advancement = helper.getLevel().getServer().getAdvancements().get(WildspellMobs.id("eternal_damnation"));
        helper.assertTrue(advancement != null, "the eternal damnation advancement didn't load");
        var display = advancement.value().display().orElseThrow();
        helper.assertTrue(display.isHidden() && display.getType() == net.minecraft.advancements.AdvancementType.CHALLENGE, "it should be a hidden challenge");
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
        helper.assertTrue(WildspellMobs.FROST_ORB.get().is(net.minecraft.tags.EntityTypeTags.REDIRECTABLE_PROJECTILE), "a frost orb can't be struck back");
    }

    @GameTest(template = ARENA, timeoutTicks = 600, batch = "lichFight")
    public static void lichFightsWithVolleysMinionsAndBursts(GameTestHelper helper) {
        shade(helper);
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000.0);
        pig.setHealth(1000.0F);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 4.0F, 1.5F);
        lich.setTarget(pig);
        lich.setHealth(lich.getMaxHealth() * 0.4F);  // enraged, so the ice burst joins in
        boolean[] saw = new boolean[2];  // shard fired, minion raised
        helper.onEachTick(() -> {
            if (lich.getTarget() != pig) {
                lich.setTarget(pig);
            }
            saw[0] |= !helper.getEntities(WildspellMobs.FROST_SHARD.get()).isEmpty();
            saw[1] |= helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, lich.getBoundingBox().inflate(32),
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
            java.util.List<net.minecraft.world.entity.Mob> minions = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    lich.getBoundingBox().inflate(48), lich::isOwnMinion);
            helper.assertTrue(!minions.isEmpty(), "no minions yet");
            lich.hurt(helper.getLevel().damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(!lich.isAlive(), "lich survived");
            helper.assertTrue(minions.stream().noneMatch(net.minecraft.world.entity.Mob::isAlive), "minions outlived the lich");
        });
    }

    @GameTest(template = ARENA)
    public static void frostboundStaffFiresAShard(GameTestHelper helper) {
        net.minecraft.world.entity.player.Player player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        player.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack staff = new ItemStack(WildspellMobs.FROSTBOUND_STAFF.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, staff);
        staff.use(helper.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(com.wildspell.mobs.entity.FrostShard.class, player.getBoundingBox().inflate(4)).isEmpty(),
                "staff fired nothing");
        helper.assertTrue(staff.getDamageValue() == 1, "staff took no wear");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void rimeSkullsSpawnInEveryVariantAndKeepIt(GameTestHelper helper) {
        shade(helper);
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int i = 0; i < 60; ++i) {
            RimeSkull skull = WildspellMobs.RIME_SKULL.get().create(helper.getLevel());
            skull.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(helper.absolutePos(BlockPos.ZERO)), MobSpawnType.NATURAL, null);
            seen.add(skull.getVariant());
            RimeSkull reloaded = WildspellMobs.RIME_SKULL.get().create(helper.getLevel());
            reloaded.load(skull.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
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
        int[] ticks = new int[2];        // seized, moving
        double[] distance = new double[2]; // horizontal distance covered in each state
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
            // Momentum carries into the first ticks of each stall, so the seized average isn't near zero.
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
        java.util.List<net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData> monsters = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        java.util.List<String> creepers = monsters.stream()
                .filter(data -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(data.type).getPath().contains("creeper"))
                .map(data -> net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(data.type) + " " + data.getWeight().asInt() + "x" + data.maxCount)
                .toList();
        // With Creeper Overhaul installed its cave creepers give way to rare snowy ones; otherwise vanilla creepers are made rare.
        String expected = net.neoforged.fml.ModList.get().isLoaded("creeperoverhaul") ? "creeperoverhaul:snowy_creeper 3x1" : "minecraft:creeper 3x1";
        helper.assertTrue(creepers.equals(java.util.List.of(expected)), "frosted caves creepers: " + creepers + ", expected only " + expected);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void enchantedIceDropsACrystalUnlessSilkTouched(GameTestHelper helper) {
        net.minecraft.world.level.block.Block rareIce = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "rare_ice"));
        helper.assertTrue(rareIce != Blocks.AIR, "yungscavebiomes:rare_ice is not loaded");
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.getLevel().setBlockAndUpdate(pos, rareIce.defaultBlockState());
        ItemStack silkPick = new ItemStack(Items.DIAMOND_PICKAXE);
        silkPick.enchant(helper.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        for (ItemStack tool : java.util.List.of(ItemStack.EMPTY, new ItemStack(Items.DIAMOND_PICKAXE), silkPick)) {
            java.util.List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(helper.getLevel().getBlockState(pos), helper.getLevel(), pos,
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
            helper.spawn(EntityType.ZOMBIE, new BlockPos(3 + i % 3, 1, 3 + i / 3 % 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            double skullPass = passRate(helper, WildspellMobs.RIME_SKULL.get(), spot);
            helper.assertTrue(zombiePass == 0.0, "zombie pass rate with a full local cap " + zombiePass);
            helper.assertTrue(skullPass == 0.0, "skull pass rate with a full local cap " + skullPass);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "creeperCap")
    public static void creeperCapRefusesOnlyCreepers(GameTestHelper helper) {
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < SpawnBalance.UNDERGROUND_CREEPER_CAP.get(); ++i) {
            helper.spawn(EntityType.CREEPER, new BlockPos(3 + i, 1, 3)).setNoAi(true);
        }
        whenSealed(helper, spot, () -> {
            double creeperPass = passRate(helper, EntityType.CREEPER, spot);
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            helper.assertTrue(creeperPass == 0.0, "creeper pass rate with the creeper cap full " + creeperPass);
            helper.assertTrue(Math.abs(zombiePass - SpawnBalance.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.succeed();
        });
    }

    /**
     * A survival-mode mock player standing in the world. Not a ServerPlayer: those need a network
     * connection, and the pack's mods (YUNG's, FTB) crash sending payloads down the mock one.
     */
    private static net.minecraft.world.entity.player.Player addMockPlayer(GameTestHelper helper, net.minecraft.world.phys.Vec3 at) {
        net.minecraft.world.entity.player.Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 pos = helper.absoluteVec(at);
        player.moveTo(pos.x, pos.y, pos.z, 0.0F, 0.0F);
        player.setInvulnerable(true);
        helper.getLevel().addFreshEntity(player);
        return player;
    }

    /**
     * Roofs the arena over, well above its mobs' heads. The test world is at noon, and Frozen Zombies
     * thaw and Rime Skulls burn in sunlight; tests of other things keep them in the shade.
     */
    private static void shade(GameTestHelper helper) {
        for (int x = -1; x <= 9; ++x) {
            for (int z = -1; z <= 9; ++z) {
                helper.setBlock(x, 12, z, Blocks.STONE);
            }
        }
    }

    /** Clears anything above the arena's middle, so its centre sees the sky. */
    private static void openToTheSky(GameTestHelper helper) {
        for (int x = 2; x <= 6; ++x) {
            for (int z = 2; z <= 6; ++z) {
                for (int y = 6; y <= 9; ++y) {
                    helper.setBlock(x, y, z, Blocks.AIR);
                }
            }
        }
    }

    private static PhylacteryBlockEntity placePhylactery(GameTestHelper helper, BlockPos pos, net.minecraft.core.Direction facing) {
        helper.setBlock(pos, WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get().defaultBlockState().setValue(PhylacteryBlock.FACING, facing));
        return (PhylacteryBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }

    /**
     * Paints the arena as Frosted Caves. getBiome (like F3) samples a jittered position that can fall
     * in a neighbouring 4x4x4 biome cell, and the arena isn't aligned to that grid, so paint a margin of
     * cells around it. The command can also fail on the first ticks before the chunks are ready, so
     * keep applying it.
     */
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

    /**
     * Runs {@code checks} once, as soon as the light at {@code spot} has settled to no skylight after
     * {@link #sealCave}. How long that takes varies with how busy the light engine is.
     */
    private static void whenSealed(GameTestHelper helper, BlockPos spot, Runnable checks) {
        boolean[] done = {false};
        helper.onEachTick(() -> {
            if (!done[0] && helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, helper.absolutePos(spot)) == 0) {
                done[0] = true;
                checks.run();
            }
        });
    }

    /** Encloses (4, 1, 4) in a stone shell so it gets no skylight; light needs a few ticks to settle (see {@link #whenSealed}). */
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
        net.minecraft.world.phys.Vec3 from = new net.minecraft.world.phys.Vec3(skull.getX(), skull.getEyeY(), skull.getZ());
        net.minecraft.world.phys.Vec3 to = new net.minecraft.world.phys.Vec3(pig.getX(), pig.getEyeY(), pig.getZ());
        net.minecraft.world.phys.BlockHitResult hit = helper.getLevel().clip(new net.minecraft.world.level.ClipContext(from, to,
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, skull));
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
