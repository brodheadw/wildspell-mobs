package com.wildspell.mobs.crypt;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.ColdEffects;
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.RimeSkull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * What happens when a lich's last form falls: its hold on the caves breaks. The cold of the Frosted
 * Caves is natural (the biome's own), so this undoes only the lich's work:
 * <ul>
 *   <li>the undead it emboldened nearby (Frozen Zombies and Rime Skulls) crumble ({@link #purge}, where
 *       it falls and around its crypt);</li>
 *   <li>the thin frost it spread (snow, powder snow, YUNG's ice sheets) melts back from its crypt;
 *       solid ice stays, so nothing floods;</li>
 *   <li>its crypt goes warm: the soul-fire braziers burn as ordinary fire and its candles are lit, and
 *       its hoard is left on the empty altar;</li>
 *   <li>the souls it bound go free: they linger through the surrounding caves as Frozen Souls, cold
 *       lights hanging in the air;</li>
 *   <li>and the crypt's surroundings become a lasting safe zone ({@link LichSouls#isCleansed}).</li>
 * </ul>
 * The crypt's part waits until the area around it is loaded (see {@link LichSouls.Soul}).
 */
public final class Cleansing {
    /** How far around the crypt must be loaded to cleanse it (the frost thaw's reach, and some). */
    public static final int LOADED_RADIUS = 40;
    /** How far the freed souls' light and the news of it reach. */
    public static final double REACH = 64.0;
    private static final double PURGE_RADIUS = 48.0;
    private static final int FROST_RADIUS = 32;
    private static final int FROST_HEIGHT = 16;
    private static final int SOULS = 40;
    private static final double SOUL_RADIUS = 48.0;
    /** The lich's frost, which melts when it falls. */
    private static final TagKey<Block> LICH_FROST = TagKey.create(Registries.BLOCK, WildspellMobs.id("lich_frost"));
    private static final ResourceKey<LootTable> HOARD = ResourceKey.create(Registries.LOOT_TABLE, WildspellMobs.id("chests/lich_hoard"));

    private Cleansing() {
    }

    /** The cold's undead within reach crumble, and a pulse of warmth rolls out. */
    public static void purge(ServerLevel level, Vec3 at) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, new AABB(at, at).inflate(PURGE_RADIUS),
                m -> (m instanceof FrozenZombie || m instanceof RimeSkull) && m.isAlive())) {
            ColdEffects.shatter(level, mob, 1.1F);
        }
        for (int i = 0; i < 48; ++i) {
            double angle = i / 48.0 * Math.PI * 2.0;
            level.sendParticles(ParticleTypes.FLAME, at.x + Math.cos(angle) * 2.0, at.y + 0.5, at.z + Math.sin(angle) * 2.0, 1,
                    Math.cos(angle) * 0.4, 0.02, Math.sin(angle) * 0.4, 0.3);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.4F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.5F);
    }

    /** The crypt at {@code altar}'s share of the cleansing. */
    public static void cleanse(ServerLevel level, BlockPos altar, Direction facing) {
        Vec3 center = Vec3.atCenterOf(altar);
        purge(level, center);
        thawFrost(level, altar);
        warmCrypt(level, altar, facing);
        leaveHoard(level, altar, facing);
        freeSouls(level, altar);
        ColdEffects.tellNearby(level, new AABB(altar).inflate(REACH), Component.translatable("message.wildspellmobs.crypt_cleansed"));
    }

    private static void thawFrost(ServerLevel level, BlockPos altar) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -FROST_RADIUS; dx <= FROST_RADIUS; ++dx) {
            for (int dz = -FROST_RADIUS; dz <= FROST_RADIUS; ++dz) {
                if (dx * dx + dz * dz > FROST_RADIUS * FROST_RADIUS) {
                    continue;
                }
                for (int dy = -FROST_HEIGHT; dy <= FROST_HEIGHT; ++dy) {
                    p.set(altar.getX() + dx, altar.getY() + dy, altar.getZ() + dz);
                    if (level.getBlockState(p).is(LICH_FROST)) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    /** Soul-fire gives way to ordinary fire, and every candle is lit. */
    private static void warmCrypt(ServerLevel level, BlockPos altar, Direction facing) {
        for (BlockPos p : PhylacteryBlockEntity.cryptBlocks(altar, facing)) {
            BlockState state = level.getBlockState(p);
            if (state.is(Blocks.SOUL_CAMPFIRE)) {
                level.setBlock(p, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.FACING, state.getValue(CampfireBlock.FACING))
                        .setValue(CampfireBlock.LIT, true), 3);
                level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 10, 0.2, 0.3, 0.2, 0.02);
            } else if (state.is(BlockTags.CANDLES) && state.hasProperty(BlockStateProperties.LIT)) {
                level.setBlock(p, state.setValue(BlockStateProperties.LIT, true), 3);
            }
        }
    }

    /** The lich's hoard, left in a chest on the empty altar. */
    private static void leaveHoard(ServerLevel level, BlockPos altar, Direction facing) {
        if (!level.getBlockState(altar).canBeReplaced()) {
            return;
        }
        level.setBlock(altar, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing), 3);
        RandomizableContainer.setBlockEntityLootTable(level, level.random, altar, HOARD);
    }

    /**
     * The bound souls stream out and linger in the surrounding caves: hanging in open air, a few blocks
     * under a cave roof, clear of the walls.
     */
    private static void freeSouls(ServerLevel level, BlockPos altar) {
        Vec3 at = Vec3.atCenterOf(altar);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + 1.0, at.z, 120, 1.5, 2.0, 1.5, 0.08);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, at.y + 1.0, at.z, 60, 1.0, 1.5, 1.0, 0.05);
        level.playSound(null, altar, SoundEvents.SOUL_ESCAPE.value(), SoundSource.AMBIENT, 3.0F, 1.2F);
        level.playSound(null, altar, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.AMBIENT, 3.0F, 0.8F);
        BlockState soul = WildspellMobs.FROZEN_SOUL.get().defaultBlockState();
        int placed = 0;
        for (int attempt = 0; attempt < SOULS * 8 && placed < SOULS; ++attempt) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = 4.0 + level.random.nextDouble() * (SOUL_RADIUS - 4.0);
            BlockPos spot = BlockPos.containing(altar.getX() + Math.cos(angle) * radius, altar.getY() - 12 + level.random.nextInt(25),
                    altar.getZ() + Math.sin(angle) * radius);
            if (!ColdEffects.isOpen(level, spot, 1)) {
                continue;
            }
            // Up to the roof, then back down a little, to hang in the air.
            int climbed = 0;
            while (climbed < 12 && ColdEffects.isOpen(level, spot.above(), 1)) {
                spot = spot.above();
                ++climbed;
            }
            spot = spot.below(Math.min(climbed, 1 + level.random.nextInt(3)));
            if (clearAround(level, spot)) {
                level.setBlock(spot, soul, 3);
                ++placed;
            }
        }
    }

    /** Open air on every side, so a soul doesn't sit against a wall. */
    private static boolean clearAround(ServerLevel level, BlockPos pos) {
        if (!ColdEffects.isOpen(level, pos, 1)) {
            return false;
        }
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!ColdEffects.isOpen(level, pos.relative(side), 1)) {
                return false;
            }
        }
        return true;
    }
}
