package com.drimoz.factoryio.core.registery;


import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.model.InserterData;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class FactoryIORegistryScreens {

    // Interface ( Generic )

    public static void init() {

        /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/
        /**                                                          INSERTERS                                                          **/
        /** +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ **/

        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            MenuScreens.register(
                    inserterData.registries().getMenu().get(),
                    FactoryIOInserterScreen<FactoryIOInserterContainer>::new
            );
        }
    }
}
