package com.drimoz.factoryio.core.registery;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import com.drimoz.factoryio.core.registery.custom.*;
import com.drimoz.factoryio.core.registery.loaders.FactoryIODataLoader;
import com.drimoz.factoryio.core.registery.models.InserterData;
import io.netty.util.concurrent.Promise;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class FactoryIORegistry {
    public static void register(IEventBus eventBus) {
        FactoryIOBlocks.registerBlocks(eventBus);

        FactoryIOBlocks.registerBlockEntities(eventBus);

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

    public static void createEntitiesFromConfigs() {
        FactoryIODataLoader.setup();

        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {

            // Block & Entities

            FactoryIOBlocks.registerInserterBlockFromData(inserterData);

            // Items

            FactoryIOItems.registerInserterItemFromData(inserterData);

            // Containers

            FactoryIOContainers.registerMenu(inserterData);

            FactoryIO.LOGGER.error("Inserter correctly created : " + inserterData.identifier);
            FactoryIO.LOGGER.error("Inserter Block : " + inserterData.registries().getBlock());
            FactoryIO.LOGGER.error("Inserter Entity : " + inserterData.registries().getBlockEntity());
        }
    }
}


// CONTAINERS

// RegistryObject<MenuType<FactoryIOInserterContainer>> inserterContainer = FactoryIOContainers.registerMenuType(
//         (windowId, inv, data) -> {
//             BlockPos pos = data.readBlockPos();
//             Level world = inv.player.getCommandSenderWorld();
//             return FactoryIOInserterContainer.create(inserterData, windowId,  world, pos, inv, inv.player);
//         },
//         inserterData.identifier + "_container"
// );

// Screen Menus

// MenuScreens.register(inserterContainer.get(), InserterScreen::new);



// Items

// FactoryIOItems.ITEMS.register(
//         inserterData.identifier,
//         () -> FactoryIOInserterItem.create(
//                 inserterEntityBlockRegistryObject.get(),
//                 new Item.Properties().tab(ModCreativeModeTab.MOD_TAB),
//                 inserterData
//         )
// );