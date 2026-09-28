package com.wildspell.mobs.item;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.crypt.LichSouls;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

public class SoulseekerItem extends Item {
    private static final int LEVEL_BAND = 8;
    private static final int UPDATE_INTERVAL = 20;

    public SoulseekerItem(Properties properties) {
        super(properties);
    }

    @Nullable
    private static GlobalPos target(ServerLevel level, ItemStack stack) {
        LichSouls.Soul soul = LichSouls.get(level).soul(stack.get(WildspellMobs.SOUL.get()));
        return soul == null || soul.burned() ? null : GlobalPos.of(soul.dimension(), soul.anchor());
    }

    private static void aim(ServerLevel level, ItemStack stack) {
        GlobalPos target = target(level, stack);
        LodestoneTracker current = stack.get(DataComponents.LODESTONE_TRACKER);
        Optional<GlobalPos> now = current == null ? Optional.empty() : current.target();
        if (target == null) {
            if (current != null) {
                stack.remove(DataComponents.LODESTONE_TRACKER);
            }
        } else if (!now.equals(Optional.of(target))) {
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(target), false));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level instanceof ServerLevel server && holder.tickCount % UPDATE_INTERVAL == 0) {
            aim(server, stack);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }
        aim(server, stack);
        GlobalPos target = target(server, stack);
        if (target == null || target.dimension() != level.dimension()) {
            String why = !stack.has(WildspellMobs.SOUL.get()) ? "unbound" : target == null ? "none" : "elsewhere";
            player.displayClientMessage(Component.translatable("item.wildspellmobs.soulseeker." + why), true);
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            return InteractionResultHolder.consume(stack);
        }
        int distance = (int) Math.round(Math.sqrt(player.blockPosition().distSqr(target.pos())) / 10.0) * 10;
        int dy = target.pos().getY() - player.getBlockY();
        String where = dy < -LEVEL_BAND ? "below" : dy > LEVEL_BAND ? "above" : "level";
        player.displayClientMessage(Component.translatable("item.wildspellmobs.soulseeker.found." + where, distance), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.7F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!stack.has(WildspellMobs.SOUL.get())) {
            tooltip.add(Component.translatable("item.wildspellmobs.soulseeker.unbound").withStyle(ChatFormatting.GRAY));
        }
    }
}
