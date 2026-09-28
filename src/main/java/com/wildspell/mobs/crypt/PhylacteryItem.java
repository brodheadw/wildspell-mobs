package com.wildspell.mobs.crypt;

import com.wildspell.mobs.WildspellMobs;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class PhylacteryItem extends BlockItem {
    public PhylacteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack bound(UUID soul) {
        ItemStack stack = new ItemStack(WildspellMobs.FROZEN_PHYLACTERY.get());
        stack.set(WildspellMobs.SOUL.get(), soul);
        return stack;
    }

    @Nullable
    private static LichSouls.Soul soul(ServerLevel level, ItemStack stack) {
        return LichSouls.get(level).soul(stack.get(WildspellMobs.SOUL.get()));
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return source.is(DamageTypeTags.IS_FIRE);
    }

    @Override
    public void onDestroyed(ItemEntity item, DamageSource source) {
        if (item.level() instanceof ServerLevel level) {
            LichSouls.Soul soul = soul(level, item.getItem());
            if (soul != null) {
                soul.burn(level, item.position());
            }
        }
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity item) {
        if (!(item.level() instanceof ServerLevel level) || item.tickCount % 10 != 0) {
            return false;
        }
        LichSouls.Soul soul = soul(level, stack);
        if (soul == null) {
            return false;
        }
        if (item.getY() < level.getMinBuildHeight()) {
            this.returnToAltar(level, soul, item);
            return true;
        }
        soul.moved(level, item.blockPosition(), null);
        return false;
    }

    private void returnToAltar(ServerLevel level, LichSouls.Soul soul, ItemEntity item) {
        ServerLevel home = level.getServer().getLevel(soul.cryptDimension());
        if (home == null) {
            home = level.getServer().overworld();
        }
        BlockPos altar = this.freeSpotAbove(home, soul.crypt());
        if (altar != null && home.setBlock(altar, this.getBlock().defaultBlockState().setValue(PhylacteryBlock.FACING, soul.cryptFacing()), 3)
                && home.getBlockEntity(altar) instanceof PhylacteryBlockEntity phylactery) {
            item.discard();
            phylactery.bindSoul(home, soul.id);
            return;
        }
        BlockPos drop = soul.crypt().atY(Mth.clamp(soul.crypt().getY(), home.getMinBuildHeight(), home.getMaxBuildHeight() - 1));
        item.teleportTo(home, drop.getX() + 0.5, drop.getY() + 0.5, drop.getZ() + 0.5, Set.of(), item.getYRot(), item.getXRot());
        item.setDeltaMovement(Vec3.ZERO);
    }

    @Nullable
    private BlockPos freeSpotAbove(ServerLevel level, BlockPos altar) {
        for (int dy = 0; dy < 16; ++dy) {
            BlockPos pos = altar.above(dy);
            if (level.isInWorldBounds(pos) && level.getBlockState(pos).canBeReplaced()) {
                return pos;
            }
        }
        return null;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level instanceof ServerLevel server && holder.tickCount % 10 == 0) {
            LichSouls.Soul soul = soul(server, stack);
            if (soul != null) {
                soul.moved(level, holder.blockPosition(), holder);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (stack.has(WildspellMobs.SOUL.get())) {
            tooltip.add(Component.translatable("item.wildspellmobs.frozen_phylactery.bound").withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable("item.wildspellmobs.frozen_phylactery.fire").withStyle(ChatFormatting.GRAY));
        }
    }
}
