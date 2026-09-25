package com.wildspell.mobs;

import com.wildspell.mobs.entity.IceLich;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * How an Ice Lich comes into the world: a Frozen Phylactery thrown into icy water (water in a
 * freezing biome, or touching ice), or rarely, out of Enchanted Ice mined without Silk Touch.
 */
final class LichSummoning {
    private static final ResourceLocation ENCHANTED_ICE = ResourceLocation.fromNamespaceAndPath("yungscavebiomes", "rare_ice");

    private LichSummoning() {
    }

    /** Phylacteries lying in icy water summon the lich, consuming one. */
    static void onEntityTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel level)
                || item.tickCount % 10 != 0 || !item.getItem().is(WildspellMobs.FROZEN_PHYLACTERY.get()) || !item.isInWater()) {
            return;
        }
        BlockPos pos = item.blockPosition();
        if (!isIcyWater(level, pos)) {
            return;
        }
        ItemStack stack = item.getItem();
        stack.shrink(1);
        if (stack.isEmpty()) {
            item.discard();
        }
        // The water freezes over and the lich rises out of it.
        BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2)).forEach(p -> {
            if (level.getFluidState(p).isSource() && level.getBlockState(p).is(Blocks.WATER) && level.isEmptyBlock(p.above())) {
                level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
            }
        });
        summon(level, Vec3.atBottomCenterOf(pos).add(0.0, 1.5, 0.0));
    }

    static boolean isIcyWater(ServerLevel level, BlockPos pos) {
        if (level.getBiome(pos).is(ZombieFreezing.FREEZES_ZOMBIES)) {
            return true;
        }
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (level.getBlockState(near).is(BlockTags.ICE)) {
                return true;
            }
        }
        return false;
    }

    /** Mining Enchanted Ice without Silk Touch occasionally wakes a lich sleeping inside it. */
    static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getPlayer().isCreative()
                || !BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).equals(ENCHANTED_ICE)) {
            return;
        }
        ItemStack tool = event.getPlayer().getMainHandItem();
        boolean silkTouch = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().holderOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        if (!silkTouch && level.random.nextDouble() < SpawnBalance.ENCHANTED_ICE_LICH_CHANCE.get()) {
            summon(level, Vec3.atBottomCenterOf(event.getPos()));
        }
    }

    static IceLich summon(ServerLevel level, Vec3 at) {
        IceLich lich = WildspellMobs.ICE_LICH.get().create(level);
        if (lich == null) {
            return null;
        }
        lich.moveTo(at.x, at.y, at.z, level.random.nextFloat() * 360.0F, 0.0F);
        lich.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        level.addFreshEntity(lich);
        level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1.0, at.z, 80, 0.8, 1.2, 0.8, 0.08);
        level.sendParticles(WildspellMobs.FROST_MOTE.get(), at.x, at.y + 1.0, at.z, 60, 0.8, 1.2, 0.8, 0.05);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.6F);
        level.playSound(null, at.x, at.y, at.z, WildspellMobs.FROZEN_ZOMBIE_SHATTER.get(), SoundSource.HOSTILE, 1.5F, 0.7F);
        return lich;
    }
}
