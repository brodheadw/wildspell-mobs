package com.wildspell.mobs.item;

import com.wildspell.mobs.WildspellMobs;
import com.wildspell.mobs.crypt.LichSouls;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A scrying focus of rime and enchanted ice bound around a Crown Fragment, a shard of one lich's crown
 * still bound to its soul ({@link WildspellMobs#SOUL}). By sympathy the shard pulls toward where that
 * soul is kept: its needle points to that lich's phylactery, on its altar or wherever it's been carried,
 * like a lodestone compass's. Using it says how far, and whether above or below. Once that lich is
 * destroyed, the pull is gone.
 */
public class SoulseekerItem extends Item {
    /** Height differences beyond this read as "above" or "below". */
    private static final int LEVEL_BAND = 8;
    private static final int UPDATE_INTERVAL = 20;

    public SoulseekerItem(Properties properties) {
        super(properties);
    }

    /** Where the needle points: the phylactery of the soul this Soulseeker is bound to, or null once there's none. */
    @Nullable
    private static GlobalPos target(ServerLevel level, ItemStack stack) {
        LichSouls.Soul soul = LichSouls.get(level).soul(stack.get(WildspellMobs.SOUL.get()));
        return soul == null || soul.burned() ? null : GlobalPos.of(soul.dimension(), soul.anchor());
    }

    /** Keeps the needle on the phylactery as it moves; spins once it's gone. */
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
            String why = target == null ? "none" : "elsewhere";
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

    /** The Soulseeker takes on the soul of the Crown Fragment it was made from. */
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack result = event.getCrafting();
        if (!result.is(WildspellMobs.SOULSEEKER.get())) {
            return;
        }
        for (int i = 0; i < event.getInventory().getContainerSize(); ++i) {
            ItemStack ingredient = event.getInventory().getItem(i);
            UUID soul = ingredient.is(WildspellMobs.CROWN_FRAGMENT.get()) ? ingredient.get(WildspellMobs.SOUL.get()) : null;
            if (soul != null) {
                result.set(WildspellMobs.SOUL.get(), soul);
                return;
            }
        }
    }
}
