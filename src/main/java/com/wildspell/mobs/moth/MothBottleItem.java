package com.wildspell.mobs.moth;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.entity.LuminousMoth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * A Luminous Moth caught in a glass bottle. Used on a block, it lets the moth out there; if that spot
 * is dark, the moth takes it as its home and lights it up.
 */
public class MothBottleItem extends Item {
    public MothBottleItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(context.getClickedFace());
        }
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() || !level.getFluidState(pos).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            release(server, pos, context.getItemInHand(), context.getPlayer());
        }
        Player player = context.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            ItemStack empty = new ItemStack(Items.GLASS_BOTTLE);
            context.getItemInHand().shrink(1);
            if (context.getItemInHand().isEmpty()) {
                player.setItemInHand(context.getHand(), empty);
            } else if (!player.getInventory().add(empty)) {
                player.drop(empty, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Lets a moth out at {@code pos}, homed there if it's dark. */
    public static LuminousMoth release(ServerLevel level, BlockPos pos, ItemStack bottle, Player player) {
        // Measure before the moth is out, so its own light doesn't count.
        boolean dark = level.getMaxLocalRawBrightness(pos) < LuminousMoth.DARK_BELOW;
        LuminousMoth moth = WildspellMobs.LUMINOUS_MOTH.get().create(level);
        Bucketable.loadDefaultDataFromBucketTag(moth, bottle.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
        moth.moveTo(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        moth.setCustomName(bottle.get(DataComponents.CUSTOM_NAME));
        moth.setPersistenceRequired();
        if (dark) {
            moth.setHome(pos);
        }
        level.addFreshEntity(moth);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.4F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return moth;
    }
}
