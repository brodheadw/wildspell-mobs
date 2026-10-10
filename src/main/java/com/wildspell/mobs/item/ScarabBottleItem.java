package com.wildspell.mobs.item;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.Scarab;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

public class ScarabBottleItem extends CreatureBottleItem<Scarab> {
    public ScarabBottleItem(Item.Properties properties) {
        super(WildspellMobs.SCARAB, 0.0, properties);
    }

    @Override
    protected void released(Scarab scarab, ServerLevel level, BlockPos pos, CompoundTag data) {
        scarab.setDung(data.getBoolean("Dung"));
    }
}
