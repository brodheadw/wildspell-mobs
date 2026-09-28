package com.wildspell.mobs;

import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * The crypt feeds Fundamental Magic's field, by data alone: this mod doesn't depend on the engine, so
 * the test finds its tag and data map by name and skips when the engine isn't loaded.
 */
@GameTestHolder(WildspellMobs.MODID)
@PrefixGameTestTemplate(false)
public class FieldTests {
    private static final String ARENA = "arena";

    @GameTest(template = ARENA, timeoutTicks = 20)
    public static void cryptBlocksFeedTheField(GameTestHelper helper) {
        if (!ModList.get().isLoaded("fundamentalmagic")) {
            helper.succeed();
            return;
        }
        TagKey<Block> sources = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("fundamentalmagic", "essence_sources"));
        DataMapType<Block, ?> strength = RegistryManager.getDataMap(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("fundamentalmagic", "essence_strength"));
        helper.assertTrue(strength != null, "the engine's essence_strength data map is registered");
        for (Block block : new Block[] {WildspellMobs.FROZEN_PHYLACTERY_BLOCK.get(), WildspellMobs.RIME_WARD.get(), WildspellMobs.FROZEN_SOUL.get()}) {
            helper.assertTrue(block.defaultBlockState().is(sources), block + " is an essence source");
            Object value = block.builtInRegistryHolder().getData(strength);
            helper.assertTrue(value != null && value.toString().contains("STILL"), block + " leans still: " + value);
        }
        helper.succeed();
    }
}
