package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.registery.custom.*;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class FactoryIORegistry {
    public static void register(IEventBus eventBus) {
        FactoryIOBlocks.registerBlocks(eventBus);
        FactoryIOBlockEntities.registerBlockEntities(eventBus);
        FactoryIOItems.registerItems(eventBus);
        FactoryIOContainers.registerContainers(eventBus);

        FactoryIONetworks.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io-common.toml");
    }

    public static void registerScreens() {
        FactoryIOScreens.registerScreens();
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        FactoryIORenderers.registerRenderers(event);
    }

    public static void registerNetwork(final FMLCommonSetupEvent event) {
        event.enqueueWork(FactoryIONetworks::register);
    }
}
