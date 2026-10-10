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
//? if <26.4 {
import com.wildspell.mobs.entity.FrozenZombie;
//?}
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import com.wildspell.mobs.entity.LuminousMoth;
import com.wildspell.mobs.entity.MoonArrow;
import com.wildspell.mobs.entity.Pegasus;
import com.wildspell.mobs.entity.RimeSkull;
import com.wildspell.mobs.entity.Stemwalker;
import com.wildspell.mobs.entity.SolarRay;
import com.wildspell.mobs.entity.Watcher;
import com.wildspell.mobs.flytrap.FlytrapBlock;
import com.wildspell.mobs.flytrap.FlytrapPatchFeature;
import com.wildspell.mobs.flytrap.FlytrapStemBlock;
import com.wildspell.mobs.gods.Heavens;
import com.wildspell.mobs.grove.SporeheartBlock;
import com.wildspell.mobs.grove.SporeheartBlockEntity;
import com.wildspell.mobs.grove.SporeheartFeature;
import com.wildspell.mobs.item.FrostboundStaffItem;
import com.wildspell.mobs.item.SoulseekerItem;
import com.wildspell.mobs.item.SoulseekerRecipe;
import com.wildspell.mobs.moth.LuminousMoss;
import com.wildspell.mobs.moth.MothBottleItem;
import com.wildspell.mobs.moth.MothGlowBlock;
import com.wildspell.mobs.watch.Face;
import com.wildspell.mobs.watch.OfficeEffect;
import com.wildspell.mobs.watch.Watchers;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SOUL = DATA_COMPONENTS.registerComponentType("soul",
            builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ReweighSpawnsBiomeModifier>> REWEIGH_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("reweigh_spawns", () -> ReweighSpawnsBiomeModifier.CODEC);

//? if <26.4 {
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_ZOMBIE_CRUNCH = sound("entity.frozen_zombie.crunch");
//?}
    public static final DeferredHolder<SoundEvent, SoundEvent> ICE_SHATTER = sound("ice.shatter");

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FROST_MOTE = PARTICLE_TYPES.register("frost_mote",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<EntityType<?>, EntityType<RimeSkull>> RIME_SKULL = entity("rime_skull", EntityType.Builder.of(RimeSkull::new, MobCategory.MONSTER)
            .sized(0.625F, 0.625F)
            .clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostShard>> FROST_SHARD = entity("frost_shard", EntityType.Builder.<FrostShard>of(FrostShard::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(4)
            .updateInterval(10));

//? if <26.4 {
    public static final DeferredHolder<EntityType<?>, EntityType<FrozenZombie>> FROZEN_ZOMBIE = entity("frozen_zombie", EntityType.Builder.of(FrozenZombie::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .eyeHeight(1.74F)
            .passengerAttachments(2.0125F)
            .ridingOffset(-0.7F)
            .clientTrackingRange(8));
//?}

    public static final DeferredHolder<EntityType<?>, EntityType<IceLich>> ICE_LICH = entity("ice_lich", EntityType.Builder.of(IceLich::new, MobCategory.MONSTER)
            .sized(0.8F, 2.6F)
            .eyeHeight(2.25F)
            .clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<FrostOrb>> FROST_ORB = entity("frost_orb", EntityType.Builder.<FrostOrb>of(FrostOrb::new, MobCategory.MISC)
            .sized(0.6F, 0.6F)
            .clientTrackingRange(6)
            .updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<LichWisp>> LICH_WISP = entity("lich_wisp", EntityType.Builder.<LichWisp>of(LichWisp::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .clientTrackingRange(10)
            .updateInterval(2)
            .fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<LuminousMoth>> LUMINOUS_MOTH = entity("luminous_moth", EntityType.Builder.of(LuminousMoth::new, MobCategory.AMBIENT)
            .sized(0.6F, 0.5F)
            .eyeHeight(0.25F)
            .clientTrackingRange(8));

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

    public static final DeferredHolder<EntityType<?>, EntityType<ElectricEel>> ELECTRIC_EEL = entity("electric_eel", EntityType.Builder.of(ElectricEel::new, MobCategory.UNDERGROUND_WATER_CREATURE)
            .sized(0.7F, 0.45F)
            .eyeHeight(0.25F)
            .clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<Pegasus>> PEGASUS = entity("pegasus", EntityType.Builder.of(Pegasus::new, MobCategory.CREATURE)
            .sized(1.587F, 1.818F)
            .eyeHeight(1.727F)
            .passengerAttachments(1.64F)
            .clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<FlytrapHead>> FLYTRAP_HEAD = entity("flytrap_head", EntityType.Builder.of(FlytrapHead::new, MobCategory.MONSTER)
            .sized(0.6F, 0.85F)
            .eyeHeight(0.62F)
            .clientTrackingRange(8));

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

    public static final DeferredHolder<Feature<?>, FlytrapPatchFeature> FLYTRAP_PATCH =
            FEATURES.register("flytrap_patch", FlytrapPatchFeature::new);

    public static final DeferredHolder<Feature<?>, SporeheartFeature> SPOREHEART_FEATURE =
            FEATURES.register("sporeheart", SporeheartFeature::new);

    public static final DeferredHolder<Block, SporeheartBlock> SPOREHEART = BLOCKS.register("sporeheart",
            () -> new SporeheartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .strength(2.5F, 6.0F)
                    .sound(SoundType.WART_BLOCK)
                    .lightLevel(state -> state.getValue(SporeheartBlock.ACTIVE) ? 5 : 0)));

    public static final DeferredItem<BlockItem> SPOREHEART_ITEM = ITEMS.registerSimpleBlockItem(SPOREHEART);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SporeheartBlockEntity>> SPOREHEART_ENTITY =
            BLOCK_ENTITY_TYPES.register("sporeheart", () -> BlockEntityType.Builder.of(SporeheartBlockEntity::new, SPOREHEART.get()).build(null));

    public static final DeferredHolder<EntityType<?>, EntityType<Apollo>> APOLLO = entity("apollo", EntityType.Builder.of(Apollo::new, MobCategory.MONSTER)
            .sized(1.0F, 3.4F)
            .eyeHeight(3.0F)
            .fireImmune()
            .clientTrackingRange(16));

    public static final DeferredHolder<EntityType<?>, EntityType<SolarRay>> SOLAR_RAY = entity("solar_ray", EntityType.Builder.<SolarRay>of(SolarRay::new, MobCategory.MISC)
            .sized(0.4F, 0.4F)
            .clientTrackingRange(8)
            .updateInterval(2)
            .fireImmune());

    public static final DeferredHolder<EntityType<?>, EntityType<Stemwalker>> STEMWALKER = entity("stemwalker", EntityType.Builder.of(Stemwalker::new, MobCategory.MONSTER)
            .sized(0.7F, 2.9F)
            .eyeHeight(2.5F)
            .clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<Diana>> DIANA = entity("diana", EntityType.Builder.of(Diana::new, MobCategory.MONSTER)
            .sized(0.7F, 2.4F)
            .eyeHeight(2.1F)
            .clientTrackingRange(16));

    public static final DeferredHolder<EntityType<?>, EntityType<MoonArrow>> MOON_ARROW = entity("moon_arrow", EntityType.Builder.<MoonArrow>of(MoonArrow::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(10)
            .updateInterval(20));

    public static final Map<Face, DeferredHolder<EntityType<?>, EntityType<Watcher>>> WATCHERS = watchers();

    public static final DeferredHolder<MobEffect, OfficeEffect> WEIGHED = MOB_EFFECTS.register("weighed", () -> (OfficeEffect) new OfficeEffect(0x4A4650)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("weighed_slow"), -0.6, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("weighed_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.GRAVITY, id("weighed_fall"), 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));

    public static final DeferredHolder<MobEffect, OfficeEffect> BOUND = MOB_EFFECTS.register("bound", () -> (OfficeEffect) new OfficeEffect(0x4C6A36)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, id("bound_feet"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.JUMP_STRENGTH, id("bound_jump"), -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final DeferredHolder<MobEffect, OfficeEffect> GLARE = MOB_EFFECTS.register("glare", () -> new OfficeEffect(0xFFF4D0));

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

    public static final DeferredItem<DeferredSpawnEggItem> STEMWALKER_SPAWN_EGG = spawnEgg("stemwalker_spawn_egg", STEMWALKER, 0xE8E2D4, 0x9E1F1F);

    public static final DeferredItem<DeferredSpawnEggItem> RIME_SKULL_SPAWN_EGG = spawnEgg("rime_skull_spawn_egg", RIME_SKULL, 0xD6F1FF, 0x4FA8D8);

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

    public static final DeferredItem<DeferredSpawnEggItem> ICE_LICH_SPAWN_EGG = spawnEgg("ice_lich_spawn_egg", ICE_LICH, 0xCFEFFF, 0x1E2B55);

//? if <26.4 {
    public static final DeferredItem<DeferredSpawnEggItem> FROZEN_ZOMBIE_SPAWN_EGG = spawnEgg("frozen_zombie_spawn_egg", FROZEN_ZOMBIE, 0x9FD4E8, 0x3F6B4A);
//?}

    public static final DeferredItem<MothBottleItem> LUMINOUS_MOTH_BOTTLE = ITEMS.register("luminous_moth_bottle",
            () -> new MothBottleItem(new Item.Properties().stacksTo(1)));

    public static final DeferredItem<DeferredSpawnEggItem> LUMINOUS_MOTH_SPAWN_EGG = spawnEgg("luminous_moth_spawn_egg", LUMINOUS_MOTH, 0xD8EFC4, 0x7FE0C8);

    public static final DeferredItem<DeferredSpawnEggItem> ELECTRIC_EEL_SPAWN_EGG = spawnEgg("electric_eel_spawn_egg", ELECTRIC_EEL, 0x3A4034, 0xE8A23A);

    public static final DeferredItem<DeferredSpawnEggItem> PEGASUS_SPAWN_EGG = spawnEgg("pegasus_spawn_egg", PEGASUS, 0xF4F1E8, 0xE8C766);

    public static final Map<Face, DeferredItem<DeferredSpawnEggItem>> WATCHER_SPAWN_EGGS = watcherEggs();

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
        MOB_EFFECTS.register(modBus);
        modBus.addListener(WildspellMobs::registerAttributes);
        modBus.addListener(WildspellMobs::registerSpawnPlacements);
        modBus.addListener(WildspellMobs::addToCreativeTabs);
        container.registerConfig(ModConfig.Type.COMMON, MobsConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(SpawnBalance::onPositionCheck);
//? if <26.4 {
        NeoForge.EVENT_BUS.addListener(ZombieFreezing::onEntityTick);
//?}
        NeoForge.EVENT_BUS.addListener(IceMelting::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(LichSouls::onServerTick);
        NeoForge.EVENT_BUS.addListener(Heavens::onServerTick);
        NeoForge.EVENT_BUS.addListener(Heavens::onLogin);
        NeoForge.EVENT_BUS.addListener(Heavens::onDeath);
        NeoForge.EVENT_BUS.addListener(Watchers::onServerTick);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> entity(String name, EntityType.Builder<T> builder) {
        return ENTITY_TYPES.register(name, () -> builder.build(name));
    }

    private static Map<Face, DeferredHolder<EntityType<?>, EntityType<Watcher>>> watchers() {
        Map<Face, DeferredHolder<EntityType<?>, EntityType<Watcher>>> out = new EnumMap<>(Face.class);
        for (Face face : Face.values()) {
            EntityType.Builder<Watcher> builder = EntityType.Builder.<Watcher>of((type, level) -> new Watcher(type, level, face), MobCategory.MONSTER)
                    .sized(0.9F, 2.6F)
                    .eyeHeight((float) Watcher.EYE)
                    .clientTrackingRange(10);
            out.put(face, entity("watcher_" + face.id, face == Face.SABBATAIOS ? builder.fireImmune() : builder));
        }
        return out;
    }

    private static Map<Face, DeferredItem<DeferredSpawnEggItem>> watcherEggs() {
        int[] colors = {0xD6CCB4, 0x60646E, 0x967E3A, 0x407C68, 0x625692, 0xC6A246, 0xC45428};
        Map<Face, DeferredItem<DeferredSpawnEggItem>> out = new EnumMap<>(Face.class);
        for (Face face : Face.values()) {
            out.put(face, spawnEgg("watcher_" + face.id + "_spawn_egg", WATCHERS.get(face), 0x4A4244, colors[face.ordinal()]));
        }
        return out;
    }

    private static DeferredItem<DeferredSpawnEggItem> spawnEgg(String name, Supplier<? extends EntityType<? extends Mob>> type, int background, int highlight) {
        return ITEMS.register(name, () -> new DeferredSpawnEggItem(type, background, highlight, new Item.Properties()));
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUND_EVENTS.register(path, () -> SoundEvent.createVariableRangeEvent(id(path)));
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(RIME_SKULL.get(), RimeSkull.createAttributes().build());
//? if <26.4 {
        event.put(FROZEN_ZOMBIE.get(), FrozenZombie.createAttributes().build());
//?}
        event.put(ICE_LICH.get(), IceLich.createAttributes().build());
        event.put(LUMINOUS_MOTH.get(), LuminousMoth.createAttributes().build());
        event.put(ELECTRIC_EEL.get(), ElectricEel.createAttributes().build());
        event.put(PEGASUS.get(), Pegasus.createAttributes().build());
        event.put(FLYTRAP_HEAD.get(), FlytrapHead.createAttributes().build());
        event.put(APOLLO.get(), Apollo.createAttributes().build());
        event.put(DIANA.get(), Diana.createAttributes().build());
        event.put(STEMWALKER.get(), Stemwalker.createAttributes().build());
        WATCHERS.values().forEach(type -> event.put(type.get(), Watcher.createAttributes().build()));
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(RIME_SKULL.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                RimeSkull::checkRimeSkullSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
//? if <26.4 {
        event.register(FROZEN_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
//?}
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
            event.accept(STEMWALKER_SPAWN_EGG);
            event.accept(SPOREHEART_ITEM);
//? if <26.4 {
            event.accept(FROZEN_ZOMBIE_SPAWN_EGG);
//?}
            event.accept(ICE_LICH_SPAWN_EGG);
            event.accept(LUMINOUS_MOTH_SPAWN_EGG);
            event.accept(ELECTRIC_EEL_SPAWN_EGG);
            event.accept(PEGASUS_SPAWN_EGG);
            WATCHER_SPAWN_EGGS.values().forEach(event::accept);
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
