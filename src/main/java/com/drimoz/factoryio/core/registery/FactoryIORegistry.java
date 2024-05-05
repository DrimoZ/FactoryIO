package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.registery.custom.FactoryIOBlockEntities;
import com.drimoz.factoryio.core.registery.custom.FactoryIOBlocks;
import com.drimoz.factoryio.core.registery.custom.FactoryIOItems;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class FactoryIORegistry {
    public static void register(IEventBus eventBus) {
        FactoryIOBlocks.registerBlocks(eventBus);
        FactoryIOBlockEntities.registerBlockEntities(eventBus);
        FactoryIOItems.registerItems(eventBus);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io-common.toml");
    }

    public static void registerScreens() {

    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {

    }
}
