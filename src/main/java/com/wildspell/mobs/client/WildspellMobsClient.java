package com.wildspell.mobs.client;

import com.wildspell.mobs.WildspellMobs;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = WildspellMobs.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = WildspellMobs.MODID, value = Dist.CLIENT)
public class WildspellMobsClient {
    public WildspellMobsClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(RimeSkullModel.LAYER, RimeSkullModel::createBodyLayer);
        event.registerLayerDefinition(FrozenZombieModel.CRUST_LAYER, FrozenZombieModel::createCrustLayer);
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(WildspellMobs.RIME_SKULL.get(), RimeSkullRenderer::new);
        event.registerEntityRenderer(WildspellMobs.FROST_SHARD.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(WildspellMobs.FROZEN_ZOMBIE.get(), FrozenZombieRenderer::new);
        event.registerEntityRenderer(WildspellMobs.ICE_LICH.get(), IceLichRenderer::new);
        event.registerEntityRenderer(WildspellMobs.LICH_WISP.get(), LichWispRenderer::new);
    }

    @SubscribeEvent
    static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(WildspellMobs.FROST_MOTE.get(), FrostMoteParticle.Provider::new);
    }
}
