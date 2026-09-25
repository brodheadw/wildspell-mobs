package com.wildspell.mobs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Changes the weight and group size of existing spawn entries in the given biomes, e.g. making
 * creepers rare in the Frosted Caves. NeoForge's built-in modifiers always apply adds before
 * removes, so "remove, then add back at a lower weight" can't be expressed with them; this edits
 * the entries in place during the MODIFY phase instead.
 */
public record ReweighSpawnsBiomeModifier(HolderSet<Biome> biomes, HolderSet<EntityType<?>> entityTypes, int weight, int maxCount)
        implements BiomeModifier {
    public static final MapCodec<ReweighSpawnsBiomeModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(ReweighSpawnsBiomeModifier::biomes),
            RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE).fieldOf("entity_types").forGetter(ReweighSpawnsBiomeModifier::entityTypes),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("weight").forGetter(ReweighSpawnsBiomeModifier::weight),
            Codec.intRange(1, 64).fieldOf("max_count").forGetter(ReweighSpawnsBiomeModifier::maxCount))
            .apply(instance, ReweighSpawnsBiomeModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.MODIFY || !this.biomes.contains(biome)) {
            return;
        }
        for (MobCategory category : MobCategory.values()) {
            List<MobSpawnSettings.SpawnerData> spawners = builder.getMobSpawnSettings().getSpawner(category);
            for (int i = 0; i < spawners.size(); ++i) {
                MobSpawnSettings.SpawnerData spawner = spawners.get(i);
                if (this.entityTypes.contains(spawner.type.builtInRegistryHolder())) {
                    spawners.set(i, new MobSpawnSettings.SpawnerData(spawner.type, this.weight, Math.min(spawner.minCount, this.maxCount), this.maxCount));
                }
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return WildspellMobs.REWEIGH_SPAWNS.get();
    }
}
