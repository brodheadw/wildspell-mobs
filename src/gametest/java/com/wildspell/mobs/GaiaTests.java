package com.wildspell.mobs;

import com.wildspell.mobs.entity.GaiaManifestation;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class GaiaTests {
    private static final BlockPos CENTER = new BlockPos(21, 9, 21);

    @GameTest(template = "gaia_arena", timeoutTicks = 100, batch = "gaiaGeometry")
    public static void sanctuaryHasWalkableEntrancePoolCradleAndOneOccupant(GameTestHelper helper) {
        ground(helper);
        helper.assertTrue(place(helper), "configured sanctuary did not place");
        for (int z = 0; z <= 19; z++) {
            int x = z >= 11 && z <= 16 ? 2 : 0;
            for (int y = 1; y <= 2; y++) {
                helper.assertTrue(helper.getBlockState(CENTER.offset(x, y, z)).isAir(), "entrance is blocked at " + z);
            }
            helper.assertTrue(helper.getBlockState(CENTER.offset(x, 0, z)).isSolid(), "entrance lacks a walking floor");
        }
        helper.assertBlockPresent(Blocks.WATER, CENTER.offset(-4, 0, -1));
        helper.assertBlockPresent(Blocks.MOSS_BLOCK, CENTER.offset(0, 0, 0));
        helper.assertBlockPresent(Blocks.DARK_OAK_WOOD, CENTER.offset(0, 0, -5));
        helper.assertTrue(helper.getBlockState(CENTER.offset(0, 23, 0)).isSolid(), "upper living sphere is missing");
        helper.assertTrue(helper.getBlockState(CENTER.offset(4, 23, -2)).isAir(), "offset skylight is closed");
        helper.assertTrue(occupants(helper).size() == 1, "sanctuary must create exactly one occupant");
        helper.assertTrue(occupants(helper).getFirst().getYRot() == 0.0F, "manifestation does not face the entrance");
        helper.assertFalse(place(helper), "repeated placement overwrote the sanctuary");
        helper.assertTrue(occupants(helper).size() == 1, "repeated placement duplicated occupant");
        int leaves = 0;
        for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-19, -5, -19), CENTER.offset(19, 25, 19))) {
            BlockState state = helper.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock) {
                leaves++;
                helper.assertTrue(state.getValue(LeavesBlock.PERSISTENT), "sanctuary leaves can decay");
            }
        }
        helper.assertTrue(leaves > 20, "living shell has no foliage");
        assertLivingWalls(helper);
        Set<BlockPos> roots = new HashSet<>();
        Set<BlockPos> connected = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        for (int x = -19; x <= 19; x++) {
            for (int z = -19; z <= 19; z++) {
                BlockPos pos = new BlockPos(x, 0, z);
                if (helper.getBlockState(CENTER.offset(pos)).is(Blocks.DARK_OAK_WOOD)) {
                    roots.add(pos);
                    if (x * x + z * z <= 144) {
                        frontier.add(pos);
                        connected.add(pos);
                    }
                }
            }
        }
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.removeFirst();
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = pos.relative(direction);
                if (roots.contains(next) && connected.add(next)) {
                    frontier.add(next);
                }
            }
        }
        helper.assertTrue(connected.containsAll(roots), "outer root tendrils have gaps");
        helper.succeed();
    }

    @GameTest(template = "gaia_arena", timeoutTicks = 100, batch = "gaiaRefusal")
    public static void placementRefusesStructuresAndOccupantsWithoutChangingBlocks(GameTestHelper helper) {
        ground(helper);
        for (BlockState obstacle : new BlockState[] {Blocks.STONE_BRICKS.defaultBlockState(), Blocks.CHEST.defaultBlockState(), Blocks.STONE.defaultBlockState()}) {
            helper.setBlock(CENTER.offset(12, 5, 0), obstacle);
            Map<BlockPos, BlockState> before = snapshot(helper);
            helper.assertFalse(place(helper), "placement accepted an above-ground build");
            assertUnchanged(helper, before);
        }
        helper.setBlock(CENTER.offset(12, 5, 0), Blocks.AIR);
        helper.setBlock(CENTER.offset(1, -2, 1), Blocks.CHEST);
        Map<BlockPos, BlockState> buried = snapshot(helper);
        helper.assertFalse(place(helper), "placement accepted a buried block entity");
        assertUnchanged(helper, buried);
        helper.setBlock(CENTER.offset(1, -2, 1), Blocks.DIRT);
        Entity pig = helper.spawn(EntityType.PIG, CENTER.above());
        Map<BlockPos, BlockState> before = snapshot(helper);
        helper.assertFalse(place(helper), "placement accepted an occupied sanctuary");
        assertUnchanged(helper, before);
        pig.discard();
        helper.succeed();
    }

    @GameTest(template = "gaia_arena", timeoutTicks = 100, batch = "gaiaCanceled")
    public static void canceledManifestationInsertionLeavesGroundUnchanged(GameTestHelper helper) {
        ground(helper);
        Map<BlockPos, BlockState> before = snapshot(helper);
        Consumer<EntityJoinLevelEvent> cancel = event -> {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntity().getType()).equals(WildspellMobs.id("gaia_manifestation"))) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(cancel);
        try {
            helper.assertFalse(place(helper), "canceled insertion reported success");
            assertUnchanged(helper, before);
            helper.assertTrue(occupants(helper).isEmpty(), "canceled manifestation survived");
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.succeed();
    }

    @GameTest(template = "gaia_arena", timeoutTicks = 100, batch = "gaiaPersistence")
    public static void manifestationRemainsAnchoredPeacefulAndDropFreeAfterReload(GameTestHelper helper) {
        ground(helper);
        helper.assertTrue(place(helper), "sanctuary did not place");
        Mob original = occupants(helper).getFirst();
        ((GaiaManifestation) original).anchorTo(original.blockPosition(), 37.0F);
        original.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        original.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        Vec3 home = original.position();
        CompoundTag saved = new CompoundTag();
        original.save(saved);
        original.discard();
        Mob restored = (Mob) EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
        helper.assertTrue(restored != null && helper.getLevel().addFreshEntity(restored), "saved manifestation did not reload");
        Difficulty prior = helper.getLevel().getDifficulty();
        helper.getLevel().getServer().setDifficulty(Difficulty.PEACEFUL, true);
        GameTests.onFinish(helper, () -> helper.getLevel().getServer().setDifficulty(prior, true));
        Entity pig = helper.spawn(EntityType.PIG, CENTER.offset(5, 1, 0));
        helper.assertFalse(restored.startRiding(pig, true), "manifestation can ride a vehicle");
        helper.assertFalse(pig.startRiding(restored, true), "manifestation accepts a forced passenger");
        restored.setLeashedTo(pig, true);
        helper.assertFalse(restored.isLeashed(), "direct leash attachment succeeded");
        helper.assertFalse(restored.canUsePortal(false), "manifestation can enter a portal");
        helper.assertFalse(restored.randomTeleport(home.x + 5, home.y, home.z, false), "random teleport succeeded");
        helper.assertFalse(restored.teleportTo(helper.getLevel(), home.x + 5, home.y, home.z, Set.of(), 90, 0), "command teleport succeeded");
        restored.teleportTo(home.x + 5, home.y, home.z);
        helper.assertTrue(restored.position().distanceToSqr(home) < 0.0001, "direct teleport moved manifestation");
        // A direct position edit exercises the saved home, rather than merely saving the current position.
        restored.moveTo(home.x + 5, home.y + 2, home.z + 4, 90.0F, 0.0F);
        restored.move(MoverType.PISTON, new Vec3(5, 2, 4));
        restored.push(5, 1, 3);
        restored.setDeltaMovement(3, 2, 1);
        helper.assertFalse(restored.hurt(helper.getLevel().damageSources().generic(), 1000), "damage was accepted");
        helper.assertFalse(restored.canBeLeashed(), "manifestation can be leashed");
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(restored.isAlive(), "peaceful removed manifestation");
            helper.assertTrue(restored.position().distanceToSqr(home) < 0.0001, "saved home did not anchor manifestation");
            helper.assertTrue(restored.getYRot() == 37.0F, "saved rotation was lost");
            helper.assertTrue(restored.getTarget() == null, "manifestation acquired a target");
            helper.assertTrue(occupants(helper).size() == 1, "reload duplicated manifestation");
            helper.getLevel().getServer().getCommands().performPrefixedCommand(
                    helper.getLevel().getServer().createCommandSourceStack().withSuppressedOutput(), "kill " + restored.getUUID());
        });
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(restored.isRemoved(), "/kill could not remove manifestation");
            helper.assertTrue(helper.getEntities(EntityType.ITEM).isEmpty(), "manifestation produced item drops");
            helper.assertTrue(helper.getEntities(EntityType.EXPERIENCE_ORB).isEmpty(), "manifestation produced experience");
            helper.succeed();
        });
    }

    @GameTest(template = "gaia_arena", timeoutTicks = 100, batch = "gaiaBounds")
    public static void placementRefusesBuildLimitsBorderAndUnloadedChunks(GameTestHelper helper) {
        ground(helper);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(CENTER);
        Map<BlockPos, BlockState> before = snapshot(helper);
        helper.assertFalse(placeAt(helper, origin.atY(level.getMaxBuildHeight() - 20)), "placement crossed upper build limit");
        helper.assertFalse(placeAt(helper, origin.atY(level.getMinBuildHeight() + 4)), "placement crossed lower build limit");
        BlockPos border = new BlockPos((int) level.getWorldBorder().getMaxX() - 10, origin.getY(), origin.getZ());
        helper.assertFalse(placeAt(helper, border), "placement crossed world border");
        BlockPos unloaded = new BlockPos(1000000, origin.getY(), 1000000);
        helper.assertFalse(level.hasChunkAt(unloaded), "test expected an unloaded target");
        helper.assertFalse(placeAt(helper, unloaded), "placement accepted unloaded chunks");
        helper.assertFalse(level.hasChunkAt(unloaded), "refused placement loaded the target chunk");
        assertUnchanged(helper, before);
        double borderX = level.getWorldBorder().getCenterX();
        double borderZ = level.getWorldBorder().getCenterZ();
        double borderSize = level.getWorldBorder().getSize();
        try {
            level.getWorldBorder().setCenter(origin.getX() + 0.25, origin.getZ() + 0.25);
            level.getWorldBorder().setSize(38.5);
            helper.assertFalse(place(helper), "placement crossed a fractional border with the outer half of a block");
            assertUnchanged(helper, before);
        } finally {
            level.getWorldBorder().setCenter(borderX, borderZ);
            level.getWorldBorder().setSize(borderSize);
        }
        helper.succeed();
    }

    private static void assertLivingWalls(GameTestHelper helper) {
        Set<BlockPos> roots = new HashSet<>();
        Set<BlockPos> visibleRoots = new HashSet<>();
        Set<BlockPos> connected = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        int moss = 0;
        int leaves = 0;
        for (BlockPos mutable : BlockPos.betweenClosed(-19, 0, -19, 19, 25, 19)) {
            BlockPos pos = mutable.immutable();
            BlockState state = helper.getBlockState(CENTER.offset(pos));
            if (state.is(Blocks.DARK_OAK_WOOD)) {
                roots.add(pos);
                if (pos.getY() == 0) {
                    connected.add(pos);
                    frontier.add(pos);
                }
            }
            if (pos.getY() < 3 || pos.getY() > 17 || pos.getX() * pos.getX() + pos.getZ() * pos.getZ() > 110) {
                continue;
            }
            boolean exposed = false;
            for (Direction direction : Direction.values()) {
                exposed |= helper.getBlockState(CENTER.offset(pos.relative(direction))).isAir();
            }
            if (exposed) {
                if (state.is(Blocks.DARK_OAK_WOOD)) {
                    visibleRoots.add(pos);
                } else if (state.is(Blocks.MOSS_BLOCK)) {
                    moss++;
                } else if (state.getBlock() instanceof LeavesBlock) {
                    leaves++;
                }
            }
        }
        helper.assertTrue(visibleRoots.size() >= 80, "chamber lacks substantial exposed root ribs");
        helper.assertTrue(moss >= 40 && leaves >= 10, "inner walls lack living moss and leaf patches");
        while (!frontier.isEmpty()) {
            BlockPos pos = frontier.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (roots.contains(next) && connected.add(next)) {
                    frontier.add(next);
                }
            }
        }
        helper.assertTrue(connected.containsAll(visibleRoots), "interior roots are disconnected from the ground");
    }

    private static boolean place(GameTestHelper helper) {
        return placeAt(helper, helper.absolutePos(CENTER));
    }

    private static boolean placeAt(GameTestHelper helper, BlockPos origin) {
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(WildspellMobs.id("gaias_navel"));
        helper.assertTrue(feature != null, "gaias_navel configured feature is not registered");
        ServerLevel level = helper.getLevel();
        return feature.place(level, level.getChunkSource().getGenerator(), level.random, origin);
    }

    private static List<Mob> occupants(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(Mob.class,
                new AABB(Vec3.atLowerCornerOf(helper.absolutePos(CENTER.offset(-19, -5, -19))), Vec3.atLowerCornerOf(helper.absolutePos(CENTER.offset(20, 26, 20)))),
                entity -> BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(WildspellMobs.id("gaia_manifestation")));
    }

    private static void ground(GameTestHelper helper) {
        for (BlockPos pos : BlockPos.betweenClosed(1, 3, 1, 41, 9, 41)) {
            helper.setBlock(pos, pos.getY() == 9 ? Blocks.GRASS_BLOCK : Blocks.DIRT);
        }
    }

    private static Map<BlockPos, BlockState> snapshot(GameTestHelper helper) {
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-19, -5, -19), CENTER.offset(19, 25, 19))) {
            states.put(pos.immutable(), helper.getBlockState(pos));
        }
        return states;
    }

    private static void assertUnchanged(GameTestHelper helper, Map<BlockPos, BlockState> before) {
        before.forEach((pos, state) -> helper.assertTrue(helper.getBlockState(pos).equals(state), "refused placement changed " + pos));
        helper.assertTrue(occupants(helper).isEmpty(), "refused placement created an occupant");
    }
}
