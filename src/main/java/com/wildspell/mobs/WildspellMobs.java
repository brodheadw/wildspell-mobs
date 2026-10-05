package com.wildspell.mobs;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.crypt.FrozenSoulBlock;
import com.wildspell.mobs.crypt.LichCryptPiece;
import com.wildspell.mobs.crypt.LichCryptStructure;
import com.wildspell.mobs.crypt.LichSouls;
import com.wildspell.mobs.crypt.PhylacteryBlock;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import com.wildspell.mobs.crypt.PhylacteryItem;
import com.wildspell.mobs.entity.Apollo;
import com.wildspell.mobs.entity.Diana;
import com.wildspell.mobs.entity.ElectricEel;
import com.wildspell.mobs.entity.FlytrapHead;
import com.wildspell.mobs.entity.FrostOrb;
import com.wildspell.mobs.entity.FrostShard;
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import com.wildspell.mobs.entity.LuminousMoth;
import com.wildspell.mobs.entity.MoonArrow;
import com.wildspell.mobs.entity.Pegasus;
import com.wildspell.mobs.entity.RimeSkull;
import com.wildspell.mobs.entity.SolarRay;
import com.wildspell.mobs.flytrap.FlytrapBlock;
import com.wildspell.mobs.flytrap.FlytrapPatchFeature;
import com.wildspell.mobs.flytrap.FlytrapStemBlock;
import com.wildspell.mobs.gods.Heavens;
import com.wildspell.mobs.item.FrostboundStaffItem;
import com.wildspell.mobs.item.SoulseekerItem;
import com.wildspell.mobs.item.SoulseekerRecipe;
import com.wildspell.mobs.moth.LuminousMoss;
import com.wildspell.mobs.moth.MothBottleItem;
import com.wildspell.mobs.moth.MothGlowBlock;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(WildspellMobs.MODID)
public class WildspellMobs {
    public static final String MODID = "wildspellmobs";

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SOUL = DATA_COMPONENTS.registerComponentType("soul",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ReweighSpawnsBiomeModifier>> REWEIGH_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("reweigh_spawns", () -> ReweighSpawnsBiomeModifier.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_ZOMBIE_CRUNCH = sound("entity.frozen_zombie.crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_ZOMBIE_SHATTER = sound("entity.frozen_zombie.shatter");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_MOTE = PARTICLE_TYPES.register("frost_mote",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<RimeSkull>> RIME_SKULL = ENTITY_TYPES.register("rime_skull",
            () -> EntityType.Builder.of(RimeSkull::new, MobCategory.MONSTER)
                    .sized(0.625F, 0.625F)
                    .clientTrackingRange(8)
                    .build("rime_skull"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostShard>> FROST_SHARD = ENTITY_TYPES.register("frost_shard",
            () -> EntityType.Builder.<FrostShard>of(FrostShard::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("frost_shard"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrozenZombie>> FROZEN_ZOMBIE = ENTITY_TYPES.register("frozen_zombie",
            () -> EntityType.Builder.of(FrozenZombie::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.74F)
                    .passengerAttachments(2.0125F)
                    .ridingOffset(-0.7F)
                    .clientTrackingRange(8)
                    .build("frozen_zombie"));

    public static final DeferredHolder<EntityType<?>, EntityType<IceLich>> ICE_LICH = ENTITY_TYPES.register("ice_lich",
            () -> EntityType.Builder.of(IceLich::new, MobCategory.MONSTER)
                    .sized(0.8F, 2.6F)
                    .eyeHeight(2.25F)
                    .clientTrackingRange(10)
                    .build("ice_lich"));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostOrb>> FROST_ORB = ENTITY_TYPES.register("frost_orb",
            () -> EntityType.Builder.<FrostOrb>of(FrostOrb::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .clientTrackingRange(6)
                    .updateInterval(2)
                    .build("frost_orb"));

    public static final DeferredHolder<EntityType<?>, EntityType<LichWisp>> LICH_WISP = ENTITY_TYPES.register("lich_wisp",
            () -> EntityType.Builder.<LichWisp>of(LichWisp::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build("lich_wisp"));

    public static final DeferredHolder<EntityType<?>, EntityType<LuminousMoth>> LUMINOUS_MOTH = ENTITY_TYPES.register("luminous_moth",
            () -> EntityType.Builder.of(LuminousMoth::new, MobCategory.AMBIENT)
                    .sized(0.6F, 0.5F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(8)
                    .build("luminous_moth"));

    public static final DeferredHolder<Block, MothGlowBlock> MOTH_GLOW = BLOCKS.register("moth_glow",
            () -> new MothGlowBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .instabreak()
                    .noLootTable()
                    .noOcclusion()
                    .lightLevel(MothGlowBlock::lightLevel)
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, LuminousMoss.MossBlock> LUMINOUS_MOSS = BLOCKS.register("luminous_moss",
            () -> new LuminousMoss.MossBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK)
                    .lightLevel(state -> 9)
                    .randomTicks()
                    .dropsLike(Blocks.MOSS_BLOCK)));

    public static final DeferredHolder<Block, LuminousMoss.Carpet> LUMINOUS_MOSS_CARPET = BLOCKS.register("luminous_moss_carpet",
            () -> new LuminousMoss.Carpet(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)
                    .lightLevel(state -> 6)
                    .randomTicks()
                    .dropsLike(Blocks.MOSS_CARPET)));

    public static final DeferredHolder<EntityType<?>, EntityType<ElectricEel>> ELECTRIC_EEL = ENTITY_TYPES.register("electric_eel",
            () -> EntityType.Builder.of(ElectricEel::new, MobCategory.UNDERGROUND_WATER_CREATURE)
                    .sized(0.7F, 0.45F)
                    .eyeHeight(0.25F)
                    .clientTrackingRange(8)
                    .build("electric_eel"));

    public static final DeferredHolder<EntityType<?>, EntityType<Pegasus>> PEGASUS = ENTITY_TYPES.register("pegasus",
            () -> EntityType.Builder.of(Pegasus::new, MobCategory.CREATURE)
                    .sized(1.587F, 1.818F)
                    .eyeHeight(1.727F)
                    .passengerAttachments(1.64F)
                    .clientTrackingRange(10)
                    .build("pegasus"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlytrapHead>> FLYTRAP_HEAD = ENTITY_TYPES.register("flytrap_head",
            () -> EntityType.Builder.of(FlytrapHead::new, MobCategory.MONSTER)
                    .sized(0.6F, 0.85F)
                    .eyeHeight(0.62F)
                    .clientTrackingRange(8)
                    .build("flytrap_head"));

    public static final DeferredHolder<Block, FlytrapBlock> FLYTRAP = BLOCKS.register("flytrap",
            () -> new FlytrapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.5F)
                    .sound(SoundType.BIG_DRIPLEAF)
                    .noOcclusion()
                    .randomTicks()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, FlytrapStemBlock> FLYTRAP_STEM = BLOCKS.register("flytrap_stem",
            () -> new FlytrapStemBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.PLANT)
                    .strength(0.5F)
                    .sound(SoundType.BIG_DRIPLEAF)
                    .noOcclusion()
                    .noLootTable()
                    .ignitedByLava()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MODID);

    public static final DeferredHolder<Feature<?>, FlytrapPatchFeature> FLYTRAP_PATCH =
            FEATURES.register("flytrap_patch", FlytrapPatchFeature::new);

    public static final DeferredHolder<EntityType<?>, EntityType<Apollo>> APOLLO = ENTITY_TYPES.register("apollo",
            () -> EntityType.Builder.of(Apollo::new, MobCategory.MONSTER)
                    .sized(1.0F, 3.4F)
                    .eyeHeight(3.0F)
                    .fireImmune()
                    .clientTrackingRange(16)
                    .build("apollo"));

    public static final DeferredHolder<EntityType<?>, EntityType<SolarRay>> SOLAR_RAY = ENTITY_TYPES.register("solar_ray",
            () -> EntityType.Builder.<SolarRay>of(SolarRay::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(8)
                    .updateInterval(2)
                    .fireImmune()
                    .build("solar_ray"));

    public static final DeferredHolder<EntityType<?>, EntityType<Diana>> DIANA = ENTITY_TYPES.register("diana",
            () -> EntityType.Builder.of(Diana::new, MobCategory.MONSTER)
                    .sized(0.7F, 2.4F)
                    .eyeHeight(2.1F)
                    .clientTrackingRange(16)
                    .build("diana"));

    public static final DeferredHolder<EntityType<?>, EntityType<MoonArrow>> MOON_ARROW = ENTITY_TYPES.register("moon_arrow",
            () -> EntityType.Builder.<MoonArrow>of(MoonArrow::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .eyeHeight(0.13F)
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("moon_arrow"));

    public static final DeferredHolder<Block, PhylacteryBlock> FROZEN_PHYLACTERY_BLOCK = BLOCKS.register("frozen_phylactery",
            () -> new PhylacteryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F, 1200.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<Block, AmethystClusterBlock> RIME_WARD = BLOCKS.register("rime_ward",
            () -> new AmethystClusterBlock(7.0F, 3.0F, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 7)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<Block, FrozenSoulBlock> FROZEN_SOUL = BLOCKS.register("frozen_soul",
            () -> new FrozenSoulBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(0.5F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 13)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhylacteryBlockEntity>> PHYLACTERY = BLOCK_ENTITY_TYPES.register("phylactery",
            () -> BlockEntityType.Builder.of(PhylacteryBlockEntity::new, FROZEN_PHYLACTERY_BLOCK.get()).build(null));

    public static final DeferredHolder<StructureType<?>, StructureType<LichCryptStructure>> LICH_CRYPT = STRUCTURE_TYPES.register("lich_crypt",
            () -> () -> LichCryptStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> LICH_CRYPT_PIECE = STRUCTURE_PIECES.register("lich_crypt",
            () -> (StructurePieceType.ContextlessType) LichCryptPiece::new);

    public static final DeferredItem<Item> RIME_SHARD = ITEMS.registerSimpleItem("rime_shard");

    public static final DeferredItem<Item> ENCHANTED_ICE_CRYSTAL = ITEMS.registerSimpleItem("enchanted_ice_crystal",
            new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<DeferredSpawnEggItem> RIME_SKULL_SPAWN_EGG = ITEMS.register("rime_skull_spawn_egg",
            () -> new DeferredSpawnEggItem(RIME_SKULL, 0xD6F1FF, 0x4FA8D8, new Item.Properties()));

    public static final DeferredItem<PhylacteryItem> FROZEN_PHYLACTERY = ITEMS.register("frozen_phylactery",
            () -> new PhylacteryItem(FROZEN_PHYLACTERY_BLOCK.get(), new Item.Properties().rarity(Rarity.EPIC).stacksTo(1)));

    public static final DeferredItem<BlockItem> FROZEN_SOUL_ITEM = ITEMS.registerSimpleBlockItem(FROZEN_SOUL, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> RIME_WARD_ITEM = ITEMS.registerSimpleBlockItem(RIME_WARD, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredItem<Item> CROWN_FRAGMENT = ITEMS.registerSimpleItem("crown_fragment",
            new Item.Properties().rarity(Rarity.RARE).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<SoulseekerItem> SOULSEEKER = ITEMS.register("soulseeker",
            () -> new SoulseekerItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(1)));

    public static final DeferredHolder<RecipeSerializer<?>, SoulseekerRecipe.Serializer> SOULSEEKER_RECIPE =
            RECIPE_SERIALIZERS.register("soulseeker", SoulseekerRecipe.Serializer::new);

    public static final DeferredItem<FrostboundStaffItem> FROSTBOUND_STAFF = ITEMS.register("frostbound_staff",
            () -> new FrostboundStaffItem(new Item.Properties().rarity(Rarity.EPIC).durability(250)));

    public static final DeferredItem<DeferredSpawnEggItem> ICE_LICH_SPAWN_EGG = ITEMS.register("ice_lich_spawn_egg",
            () -> new DeferredSpawnEggItem(ICE_LICH, 0xCFEFFF, 0x1E2B55, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FROZEN_ZOMBIE_SPAWN_EGG = ITEMS.register("frozen_zombie_spawn_egg",
            () -> new DeferredSpawnEggItem(FROZEN_ZOMBIE, 0x9FD4E8, 0x3F6B4A, new Item.Properties()));

    public static final DeferredItem<MothBottleItem> LUMINOUS_MOTH_BOTTLE = ITEMS.register("luminous_moth_bottle",
            () -> new MothBottleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DeferredSpawnEggItem> LUMINOUS_MOTH_SPAWN_EGG = ITEMS.register("luminous_moth_spawn_egg",
            () -> new DeferredSpawnEggItem(LUMINOUS_MOTH, 0xD8EFC4, 0x7FE0C8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> ELECTRIC_EEL_SPAWN_EGG = ITEMS.register("electric_eel_spawn_egg",
            () -> new DeferredSpawnEggItem(ELECTRIC_EEL, 0x3A4034, 0xE8A23A, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> PEGASUS_SPAWN_EGG = ITEMS.register("pegasus_spawn_egg",
            () -> new DeferredSpawnEggItem(PEGASUS, 0xF4F1E8, 0xE8C766, new Item.Properties()));

    public static final DeferredItem<Item> TRAP_JAW = ITEMS.registerSimpleItem("trap_jaw");

    public static final DeferredItem<ItemNameBlockItem> FLYTRAP_SPROUT = ITEMS.register("flytrap_sprout",
            () -> new ItemNameBlockItem(FLYTRAP.get(), new Item.Properties()));

    public WildspellMobs(IEventBus modBus, ModContainer container) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        DATA_COMPONENTS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        SOUND_EVENTS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        BIOME_MODIFIER_SERIALIZERS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener(WildspellMobs::registerAttributes);
        modBus.addListener(WildspellMobs::registerSpawnPlacements);
        modBus.addListener(WildspellMobs::addToCreativeTabs);
        container.registerConfig(ModConfig.Type.COMMON, SpawnBalance.SPEC);
        NeoForge.EVENT_BUS.addListener(SpawnBalance::onPositionCheck);
        NeoForge.EVENT_BUS.addListener(ZombieFreezing::onEntityTick);
        NeoForge.EVENT_BUS.addListener(IceMelting::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LichSouls::onServerTick);
        NeoForge.EVENT_BUS.addListener(Heavens::onServerTick);
        NeoForge.EVENT_BUS.addListener(Heavens::onLogin);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id(path)));
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RIME_SKULL.get(), RimeSkull.createAttributes().build());
        event.put(FROZEN_ZOMBIE.get(), FrozenZombie.createAttributes().build());
        event.put(ICE_LICH.get(), IceLich.createAttributes().build());
        event.put(LUMINOUS_MOTH.get(), LuminousMoth.createAttributes().build());
        event.put(ELECTRIC_EEL.get(), ElectricEel.createAttributes().build());
        event.put(PEGASUS.get(), Pegasus.createAttributes().build());
        event.put(FLYTRAP_HEAD.get(), FlytrapHead.createAttributes().build());
        event.put(APOLLO.get(), Apollo.createAttributes().build());
        event.put(DIANA.get(), Diana.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(RIME_SKULL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RimeSkull::checkRimeSkullSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(FROZEN_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(LUMINOUS_MOTH.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LuminousMoth::checkMothSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ELECTRIC_EEL.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ElectricEel::checkEelSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PEGASUS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Pegasus::checkPegasusSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(RIME_SKULL_SPAWN_EGG);
            event.accept(FROZEN_ZOMBIE_SPAWN_EGG);
            event.accept(ICE_LICH_SPAWN_EGG);
            event.accept(LUMINOUS_MOTH_SPAWN_EGG);
            event.accept(ELECTRIC_EEL_SPAWN_EGG);
            event.accept(PEGASUS_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RIME_SHARD);
            event.accept(ENCHANTED_ICE_CRYSTAL);
            event.accept(CROWN_FRAGMENT);
            event.accept(TRAP_JAW);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FROZEN_PHYLACTERY);
            event.accept(FROZEN_SOUL_ITEM);
            event.accept(RIME_WARD_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(FLYTRAP_SPROUT);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(SOULSEEKER);
            event.accept(LUMINOUS_MOTH_BOTTLE);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FROSTBOUND_STAFF);
        }
    }
}
