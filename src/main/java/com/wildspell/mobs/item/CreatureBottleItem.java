package com.wildspell.mobs.item;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class CreatureBottleItem<T extends Mob> extends Item {
    private final Supplier<? extends EntityType<T>> type;
    private final double lift;

    public CreatureBottleItem(Supplier<? extends EntityType<T>> type, double lift, Item.Properties properties) {
        super(properties);
        this.type = type;
        this.lift = lift;
    }

    public static InteractionResult catchIn(Mob mob, Player player, InteractionHand hand, ItemStack bottled) {
        mob.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.4F);
        player.setItemInHand(hand, ItemUtils.createFilledResult(player.getItemInHand(hand), player, bottled, false));
        if (!mob.level().isClientSide) {
            mob.discard();
        }
        return InteractionResult.sidedSuccess(mob.level().isClientSide);
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
            this.release(server, pos, context.getItemInHand(), context.getPlayer());
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

    public T release(ServerLevel level, BlockPos pos, ItemStack bottle, @Nullable Player player) {
        T mob = this.type.get().create(level);
        CompoundTag data = bottle.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
        Bucketable.loadDefaultDataFromBucketTag(mob, data);
        mob.moveTo(pos.getX() + 0.5, pos.getY() + this.lift, pos.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        mob.setCustomName(bottle.get(DataComponents.CUSTOM_NAME));
        mob.setPersistenceRequired();
        this.released(mob, level, pos, data);
        level.addFreshEntity(mob);
        level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.NEUTRAL, 1.0F, 1.4F);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
        return mob;
    }

    protected void released(T mob, ServerLevel level, BlockPos pos, CompoundTag data) {
    }
}
