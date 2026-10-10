package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.item.CompassItemPropertyFunction;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.LodestoneTracker;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WildspellMobs.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = WildspellMobs.MODID, value = Dist.CLIENT)
public class WildspellMobsClient {
    public WildspellMobsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(WildspellMobs.SOULSEEKER.get(), ResourceLocation.withDefaultNamespace("angle"),
                new CompassItemPropertyFunction((level, stack, entity) -> {
                    LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
                    return tracker != null ? tracker.target().orElse(null) : null;
                })));
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RimeSkullModel.LAYER, RimeSkullModel::createBodyLayer);
//? if <26.4 {
        event.registerLayerDefinition(FrozenZombieModel.CRUST_LAYER, FrozenZombieModel::createCrustLayer);
//?}
        event.registerLayerDefinition(LuminousMothModel.LAYER, LuminousMothModel::createBodyLayer);
        event.registerLayerDefinition(ElectricEelModel.LAYER, ElectricEelModel::createBodyLayer);
        event.registerLayerDefinition(PegasusModel.LAYER, PegasusModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WildspellMobs.RIME_SKULL.get(), RimeSkullRenderer::new);
        event.registerEntityRenderer(WildspellMobs.FROST_SHARD.get(), ThrownItemRenderer::new);
//? if <26.4 {
        event.registerEntityRenderer(WildspellMobs.FROZEN_ZOMBIE.get(), FrozenZombieRenderer::new);
//?}
        event.registerEntityRenderer(WildspellMobs.ICE_LICH.get(), IceLichRenderer::new);
        event.registerEntityRenderer(WildspellMobs.LICH_WISP.get(),
                context -> new GlowSpriteRenderer<>(context, WildspellMobs.id("textures/entity/lich_wisp.png"), 0.6F));
        event.registerEntityRenderer(WildspellMobs.FROST_ORB.get(),
                context -> new GlowSpriteRenderer<>(context, WildspellMobs.id("textures/entity/frost_orb.png"), 1.1F));
        event.registerEntityRenderer(WildspellMobs.LUMINOUS_MOTH.get(), LuminousMothRenderer::new);
        event.registerEntityRenderer(WildspellMobs.ELECTRIC_EEL.get(), ElectricEelRenderer::new);
        event.registerEntityRenderer(WildspellMobs.PEGASUS.get(), PegasusRenderer::new);
        event.registerEntityRenderer(WildspellMobs.FLYTRAP_HEAD.get(), FlytrapHeadRenderer::new);
        event.registerEntityRenderer(WildspellMobs.APOLLO.get(), ApolloRenderer::new);
        event.registerEntityRenderer(WildspellMobs.SOLAR_RAY.get(), SolarRayRenderer::new);
        event.registerEntityRenderer(WildspellMobs.DIANA.get(), DianaRenderer::new);
        event.registerEntityRenderer(WildspellMobs.STEMWALKER.get(), StemwalkerRenderer::new);
        event.registerEntityRenderer(WildspellMobs.MOON_ARROW.get(), MoonArrowRenderer::new);
        WildspellMobs.WATCHERS.forEach((face, type) -> event.registerEntityRenderer(type.get(), context -> new WatcherRenderer(context, face.id)));
        event.registerEntityRenderer(WildspellMobs.SCORPION.get(), ScorpionRenderer::new);
        event.registerEntityRenderer(WildspellMobs.SCARAB.get(), ScarabRenderer::new);
    }

    @SubscribeEvent
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, WildspellMobs.id("glare"), new GlareOverlay());
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WildspellMobs.FROST_MOTE.get(), FrostMoteParticle.Provider::new);
    }
}
