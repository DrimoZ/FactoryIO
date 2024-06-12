package com.drimoz.factoryio.a_core.registery;

import com.drimoz.factoryio.a_core.models.InserterData;
import com.drimoz.factoryio.a_core.configs.FactoryIOCommonConfigs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class FactoryIORegistry {
    public static void init(IEventBus eventBus) {
        FactoryIORegistryBlocks.init(eventBus);
        FactoryIORegistryItems.init(eventBus);
        FactoryIORegistryContainers.init(eventBus);

        FactoryIORegistryNetworks.init();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FactoryIOCommonConfigs.SPEC, "factory_io-common.toml");
    }

    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
    /**                                                          INSERTERS                                                          **/
    /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

    public static void createEntitiesFromConfigs() {
        FactoryIODataLoader.setup();

        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            FactoryIORegistryBlocks.registerInserterBlockFromData(inserterData);
            FactoryIORegistryItems.registerInserterItemFromData(inserterData);
            FactoryIORegistryContainers.registerMenu(inserterData);
        }
    }
}