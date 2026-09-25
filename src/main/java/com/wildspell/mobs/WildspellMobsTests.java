package com.wildspell.mobs;

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

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void skullHoversWithoutFalling(GameTestHelper helper) {
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
        ResourceKey<Biome> frosted = ResourceKey.create(Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frosted_caves"));
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(frosted);
        helper.assertTrue(biome != null, "yungscavebiomes:frosted_caves is not loaded");
        boolean listed = biome.getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .anyMatch(data -> data.type == WildspellMobs.RIME_SKULL.get());
        helper.assertTrue(listed, "rime skull missing from frosted caves monster spawns");
        helper.succeed();
    }

    // Runs alone so the painted biome can't reach into a neighbouring test's arena.
    @GameTest(template = ARENA, timeoutTicks = 300, batch = "freezing")
    public static void zombieFreezesInFrostedCaves(GameTestHelper helper) {
        // getBiome (like F3) samples a jittered position that can fall in a neighbouring 4x4x4 biome cell,
        // and the arena isn't aligned to that grid, so paint a margin of cells around the zombie. The
        // command can also fail on the first ticks before the chunks are ready, so keep applying it.
        BlockPos from = helper.absolutePos(new BlockPos(-2, -2, -2));
        BlockPos to = helper.absolutePos(new BlockPos(10, 6, 10));
        String fill = "fillbiome " + from.getX() + " " + from.getY() + " " + from.getZ() + " " + to.getX() + " " + to.getY() + " " + to.getZ() + " yungscavebiomes:frosted_caves";
        helper.onEachTick(() -> {
            if (!helper.getLevel().getBiome(helper.absolutePos(new BlockPos(4, 1, 4))).is(ZombieFreezing.FREEZES_ZOMBIES)) {
                helper.getLevel().getServer().getCommands().performPrefixedCommand(helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), fill);
            }
        });
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
        helper.setBlock(2, 0, 2, Blocks.STONE);
        helper.setBlock(6, 0, 6, Blocks.PACKED_ICE);
        int[] offIce = new int[3];
        int[] onIce = new int[3];
        for (int i = 0; i < 200; ++i) {
            ++offIce[pickVariantAt(helper, new BlockPos(2, 1, 2))];
            ++onIce[pickVariantAt(helper, new BlockPos(6, 1, 6))];
        }
        String mix = "off ice " + java.util.Arrays.toString(offIce) + ", on ice " + java.util.Arrays.toString(onIce);
        helper.assertTrue(onIce[FrozenZombie.ICEBOUND] == 200, "not always ice-bound on ice: " + mix);
        helper.assertTrue(offIce[FrozenZombie.ICEBOUND] == 0 && offIce[FrozenZombie.NORMAL] > offIce[FrozenZombie.ONE_ARMED]
                && offIce[FrozenZombie.ONE_ARMED] > 40, "unexpected mix: " + mix);
        FrozenZombie zombie = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos iceTop = helper.absolutePos(new BlockPos(6, 1, 6));
        zombie.moveTo(iceTop.getX() + 0.5, iceTop.getY(), iceTop.getZ() + 0.5);
        zombie.pickVariant();
        helper.assertTrue(Math.abs(zombie.getY() - (iceTop.getY() - 0.75)) < 1.0E-6, "not sunk into the ice: y=" + zombie.getY());
        FrozenZombie reloaded = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        reloaded.load(zombie.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        helper.assertTrue(reloaded.isIcebound() && reloaded.getY() == zombie.getY(), "ice-bound state not saved");
        helper.assertTrue(reloaded.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED) == 0.0,
                "reloaded ice-bound zombie can still walk");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void iceboundZombieIsStuckUntilTheIceBreaks(GameTestHelper helper) {
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

    private static int pickVariantAt(GameTestHelper helper, BlockPos relative) {
        FrozenZombie zombie = WildspellMobs.FROZEN_ZOMBIE.get().create(helper.getLevel());
        BlockPos pos = helper.absolutePos(relative);
        zombie.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        zombie.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        return zombie.getVariant();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void iceboundFrozenZombieThrowsSnowballsFromRange(GameTestHelper helper) {
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
    public static void phylacteryRecipeIsRegistered(GameTestHelper helper) {
        var recipe = helper.getLevel().getRecipeManager().byKey(WildspellMobs.id("frozen_phylactery"));
        helper.assertTrue(recipe.isPresent(), "frozen phylactery recipe missing");
        helper.assertTrue(recipe.get().value().getResultItem(helper.getLevel().registryAccess()).is(WildspellMobs.FROZEN_PHYLACTERY.get()), "wrong result");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100, batch = "lichRitual")
    public static void phylacteryInIcyWaterSummonsTheLich(GameTestHelper helper) {
        helper.setBlock(4, 0, 4, Blocks.WATER);
        helper.setBlock(5, 0, 4, Blocks.PACKED_ICE);
        net.minecraft.world.entity.item.ItemEntity phylactery = helper.spawnItem(WildspellMobs.FROZEN_PHYLACTERY.get(), 4.5F, 1.0F, 4.5F);
        helper.succeedWhen(() -> {
            helper.assertEntityPresent(WildspellMobs.ICE_LICH.get());
            helper.assertTrue(!phylactery.isAlive(), "phylactery not consumed");
            helper.assertBlockPresent(Blocks.ICE, new BlockPos(4, 0, 4));
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void phylacteryInPlainWaterDoesNothing(GameTestHelper helper) {
        helper.setBlock(4, 0, 4, Blocks.WATER);
        helper.spawnItem(WildspellMobs.FROZEN_PHYLACTERY.get(), 4.5F, 1.0F, 4.5F);
        helper.runAfterDelay(80, () -> {
            helper.assertEntityNotPresent(WildspellMobs.ICE_LICH.get());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void miningEnchantedIceCanWakeALich(GameTestHelper helper) {
        net.minecraft.world.level.block.Block rareIce = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "rare_ice"));
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.getLevel().setBlockAndUpdate(pos, rareIce.defaultBlockState());
        net.minecraft.world.entity.player.Player miner = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
        double chance = SpawnBalance.ENCHANTED_ICE_LICH_CHANCE.get();
        SpawnBalance.ENCHANTED_ICE_LICH_CHANCE.set(1.0);
        try {
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(helper.getLevel(), pos, rareIce.defaultBlockState(), miner));
        } finally {
            SpawnBalance.ENCHANTED_ICE_LICH_CHANCE.set(chance);
        }
        helper.assertEntityPresent(WildspellMobs.ICE_LICH.get());
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 600, batch = "lichFight")
    public static void lichFightsWithVolleysMinionsAndBursts(GameTestHelper helper) {
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
                    m -> m.getTags().contains(IceLich.MINION_TAG)).size() > 0;
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
        Pig pig = helper.spawn(EntityType.PIG, 4.5F, 1.0F, 4.5F);
        pig.setNoAi(true);
        pig.setInvulnerable(true);
        IceLich lich = helper.spawn(WildspellMobs.ICE_LICH.get(), 4.5F, 4.0F, 1.5F);
        lich.setTarget(pig);
        helper.succeedWhen(() -> {
            java.util.List<net.minecraft.world.entity.Mob> minions = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    lich.getBoundingBox().inflate(48), m -> m.getTags().contains(IceLich.MINION_TAG));
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
        ResourceKey<Biome> key = ResourceKey.create(Registries.BIOME, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "frosted_caves"));
        Biome biome = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).get(key);
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
        BlockPos spot = new BlockPos(4, 1, 4);
        helper.assertTrue(passRate(helper, EntityType.CREEPER, spot) == 1.0, "creepers thinned under open sky");
        sealCave(helper);
        helper.runAfterDelay(40, () -> {
            assertNoSkylight(helper, spot);
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
        BlockPos spot = new BlockPos(4, 1, 4);
        sealCave(helper);
        for (int i = 0; i < SpawnBalance.UNDERGROUND_LOCAL_CAP.get(); ++i) {
            helper.spawn(EntityType.ZOMBIE, new BlockPos(3 + i % 3, 1, 3 + i / 3 % 3)).setNoAi(true);
        }
        helper.runAfterDelay(40, () -> {
            assertNoSkylight(helper, spot);
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
        helper.runAfterDelay(40, () -> {
            assertNoSkylight(helper, spot);
            double creeperPass = passRate(helper, EntityType.CREEPER, spot);
            double zombiePass = passRate(helper, EntityType.ZOMBIE, spot);
            helper.assertTrue(creeperPass == 0.0, "creeper pass rate with the creeper cap full " + creeperPass);
            helper.assertTrue(Math.abs(zombiePass - SpawnBalance.UNDERGROUND_MONSTER_CHANCE.get()) < 0.05, "zombie pass rate " + zombiePass);
            helper.succeed();
        });
    }

    /** Encloses (4, 1, 4) in a stone shell so it gets no skylight; light needs a few ticks to settle. */
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

    private static void assertNoSkylight(GameTestHelper helper, BlockPos spot) {
        int sky = helper.getLevel().getBrightness(net.minecraft.world.level.LightLayer.SKY, helper.absolutePos(spot));
        helper.assertTrue(sky == 0, "sealed spot still has skylight " + sky);
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
