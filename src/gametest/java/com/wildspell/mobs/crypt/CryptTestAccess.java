package com.wildspell.mobs.crypt;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class CryptTestAccess {
    private CryptTestAccess() {
    }

    public static void assumeBodyAway(boolean away) {
        LichSouls.assumeBodyAway = away;
    }

    public static void forgetWithin(ServerLevel level, AABB area) {
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ), BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get())) {
                level.removeBlockEntity(pos);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        level.getEntitiesOfClass(ItemEntity.class, area, item -> item.getItem().is(WildspellMobs.FROZEN_PHYLACTERY.get())).forEach(ItemEntity::discard);
        LichSouls.get(level).forgetWithin(level, area);
    }
}
