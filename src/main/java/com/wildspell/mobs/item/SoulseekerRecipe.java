package com.wildspell.mobs.item;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.WildspellMobs;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class SoulseekerRecipe extends ShapedRecipe {
    public static final MapCodec<SoulseekerRecipe> CODEC = ShapedRecipe.Serializer.CODEC.xmap(SoulseekerRecipe::new, recipe -> recipe);
    public static final StreamCodec<RegistryFriendlyByteBuf, SoulseekerRecipe> STREAM_CODEC =
            ShapedRecipe.Serializer.STREAM_CODEC.map(SoulseekerRecipe::new, recipe -> recipe);

    public SoulseekerRecipe(ShapedRecipe shaped) {
        super(shaped.getGroup(), shaped.category(), shaped.pattern, shaped.getResultItem(null), shaped.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (ItemStack ingredient : input.items()) {
            UUID soul = ingredient.is(WildspellMobs.CROWN_FRAGMENT.get()) ? ingredient.get(WildspellMobs.SOUL.get()) : null;
            if (soul != null) {
                result.set(WildspellMobs.SOUL.get(), soul);
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return WildspellMobs.SOULSEEKER_RECIPE.get();
    }

    public static class Serializer implements RecipeSerializer<SoulseekerRecipe> {
        @Override
        public MapCodec<SoulseekerRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SoulseekerRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
