package com.drimoz.factoryio.core.registery.custom;


import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.registery.loaders.FactoryIODataLoader;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class FactoryIOScreens {
    public static void registerScreens() {
        //MenuScreens.register(FactoryIOContainers.INSERTER_MENU.get(), InserterScreen::new);

        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            MenuScreens.register(
                    inserterData.registries().getMenu().get(),
                    FactoryIOInserterScreen<FactoryIOInserterContainer>::new
            );
        }
    }
}
