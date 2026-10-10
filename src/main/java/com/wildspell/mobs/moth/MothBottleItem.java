package com.wildspell.mobs.moth;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import com.wildspell.mobs.item.CreatureBottleItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

public class MothBottleItem extends CreatureBottleItem<LuminousMoth> {
    public MothBottleItem(Item.Properties properties) {
        super(WildspellMobs.LUMINOUS_MOTH, 0.3, properties);
    }

    @Override
    protected void released(LuminousMoth moth, ServerLevel level, BlockPos pos, CompoundTag data) {
        if (level.getMaxLocalRawBrightness(pos) < LuminousMoth.DARK_BELOW) {
            moth.setHome(pos);
        }
    }
}
