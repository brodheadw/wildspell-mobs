package com.wildspell.mobs;

import com.mojang.serialization.MapCodec;
import com.wildspell.mobs.crypt.LichCryptPiece;
import com.wildspell.mobs.crypt.LichCryptStructure;
import com.wildspell.mobs.crypt.PhylacteryBlock;
import com.wildspell.mobs.crypt.PhylacteryBlockEntity;
import com.wildspell.mobs.entity.FrostShard;
import com.wildspell.mobs.entity.FrozenZombie;
import com.wildspell.mobs.entity.IceLich;
import com.wildspell.mobs.entity.LichWisp;
import com.wildspell.mobs.entity.Mossback;
import com.wildspell.mobs.entity.RimeSkull;
import com.wildspell.mobs.item.FrostboundStaffItem;
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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.NeoForge;
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
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, MODID);
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<ReweighSpawnsBiomeModifier>> REWEIGH_SPAWNS =
            BIOME_MODIFIER_SERIALIZERS.register("reweigh_spawns", () -> ReweighSpawnsBiomeModifier.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_ZOMBIE_CRUNCH = sound("entity.frozen_zombie.crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> FROZEN_ZOMBIE_SHATTER = sound("entity.frozen_zombie.shatter");

    /** Light-blue ice mote: pours off the Rime Skull and the lich, and traces the lich's beam and wards. */
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

    /** A struck-down lich's soul, flying home to its phylactery. */
    public static final DeferredHolder<EntityType<?>, EntityType<LichWisp>> LICH_WISP = ENTITY_TYPES.register("lich_wisp",
            () -> EntityType.Builder.<LichWisp>of(LichWisp::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .fireImmune()
                    .build("lich_wisp"));

    /** A rare peaceful wanderer of jungles and leafy Overworld biomes. */
    public static final DeferredHolder<EntityType<?>, EntityType<Mossback>> MOSSBACK = ENTITY_TYPES.register("mossback",
            () -> EntityType.Builder.of(Mossback::new, MobCategory.CREATURE)
                    .sized(2.7F, 1.95F)
                    .passengerAttachments(1.85F)
                    .clientTrackingRange(10)
                    .build("mossback"));

    /** The lich's phylactery, on the altar of its crypt (see PhylacteryBlockEntity). */
    public static final DeferredHolder<net.minecraft.world.level.block.Block, PhylacteryBlock> FROZEN_PHYLACTERY_BLOCK = BLOCKS.register("frozen_phylactery",
            () -> new PhylacteryBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(2.0F, 1200.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 10)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    /** Wards the phylactery while it stands; four stand in each crypt. */
    public static final DeferredHolder<net.minecraft.world.level.block.Block, AmethystClusterBlock> RIME_WARD = BLOCKS.register("rime_ward",
            () -> new AmethystClusterBlock(7.0F, 3.0F, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .strength(3.0F, 1200.0F)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .lightLevel(state -> 7)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhylacteryBlockEntity>> PHYLACTERY = BLOCK_ENTITY_TYPES.register("phylactery",
            () -> BlockEntityType.Builder.of(PhylacteryBlockEntity::new, FROZEN_PHYLACTERY_BLOCK.get()).build(null));

    public static final DeferredHolder<StructureType<?>, StructureType<LichCryptStructure>> LICH_CRYPT = STRUCTURE_TYPES.register("lich_crypt",
            () -> () -> LichCryptStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> LICH_CRYPT_PIECE = STRUCTURE_PIECES.register("lich_crypt",
            () -> (StructurePieceType.ContextlessType) LichCryptPiece::new);

    public static final DeferredItem<Item> RIME_SHARD = ITEMS.registerSimpleItem("rime_shard");

    /** Drops from YUNG's Enchanted Ice when mined without Silk Touch (see loot_modifiers/). */
    public static final DeferredItem<Item> ENCHANTED_ICE_CRYSTAL = ITEMS.registerSimpleItem("enchanted_ice_crystal",
            new Item.Properties().rarity(Rarity.UNCOMMON).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));

    public static final DeferredItem<DeferredSpawnEggItem> RIME_SKULL_SPAWN_EGG = ITEMS.register("rime_skull_spawn_egg",
            () -> new DeferredSpawnEggItem(RIME_SKULL, 0xD6F1FF, 0x4FA8D8, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> MOSSBACK_SPAWN_EGG = ITEMS.register("mossback_spawn_egg",
            () -> new DeferredSpawnEggItem(MOSSBACK, 0x576B3B, 0x92A96C, new Item.Properties()));

    /** The phylactery's item; creative-only, for building crypts or testing. */
    public static final DeferredItem<BlockItem> FROZEN_PHYLACTERY = ITEMS.registerSimpleBlockItem(FROZEN_PHYLACTERY_BLOCK,
            new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<BlockItem> RIME_WARD_ITEM = ITEMS.registerSimpleBlockItem(RIME_WARD, new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredItem<FrostboundStaffItem> FROSTBOUND_STAFF = ITEMS.register("frostbound_staff",
            () -> new FrostboundStaffItem(new Item.Properties().rarity(Rarity.EPIC).durability(250)));

    public static final DeferredItem<DeferredSpawnEggItem> ICE_LICH_SPAWN_EGG = ITEMS.register("ice_lich_spawn_egg",
            () -> new DeferredSpawnEggItem(ICE_LICH, 0xCFEFFF, 0x1E2B55, new Item.Properties()));

    public static final DeferredItem<DeferredSpawnEggItem> FROZEN_ZOMBIE_SPAWN_EGG = ITEMS.register("frozen_zombie_spawn_egg",
            () -> new DeferredSpawnEggItem(FROZEN_ZOMBIE, 0x9FD4E8, 0x3F6B4A, new Item.Properties()));

    public WildspellMobs(IEventBus modBus, ModContainer container) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
        SOUND_EVENTS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        BIOME_MODIFIER_SERIALIZERS.register(modBus);
        modBus.addListener(WildspellMobs::registerAttributes);
        modBus.addListener(WildspellMobs::registerSpawnPlacements);
        modBus.addListener(WildspellMobs::addToCreativeTabs);
        container.registerConfig(ModConfig.Type.COMMON, SpawnBalance.SPEC);
        NeoForge.EVENT_BUS.addListener(SpawnBalance::onPositionCheck);
        NeoForge.EVENT_BUS.addListener(ZombieFreezing::onEntityTick);
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
        event.put(MOSSBACK.get(), Mossback.createAttributes().build());
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(MOSSBACK.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Mossback::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(RIME_SKULL.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(FROZEN_ZOMBIE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(RIME_SKULL_SPAWN_EGG);
            event.accept(FROZEN_ZOMBIE_SPAWN_EGG);
            event.accept(ICE_LICH_SPAWN_EGG);
            event.accept(MOSSBACK_SPAWN_EGG);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(RIME_SHARD);
            event.accept(ENCHANTED_ICE_CRYSTAL);
        } else if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FROZEN_PHYLACTERY);
            event.accept(RIME_WARD_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FROSTBOUND_STAFF);
        }
    }
}
