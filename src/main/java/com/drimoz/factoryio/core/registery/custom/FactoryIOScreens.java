package com.drimoz.factoryio.core.registery.custom;


import com.drimoz.factoryio.core.containers.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.registery.loaders.FactoryIODataLoader;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.core.screens.inserters.FactoryIOInserterScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FactoryIOScreens {
    public static void registerScreens() {
        for (InserterData inserterData : FactoryIODataLoader.INSERTER_DATA_LIST) {
            MenuScreens.register(
                    inserterData.registries().getMenu().get(),
                    new MenuScreens.ScreenConstructor<FactoryIOInserterContainer, FactoryIOInserterScreen>() {
                        @Override
                        public FactoryIOInserterScreen create(FactoryIOInserterContainer pMenu, Inventory pInventory, Component pTitle) {
                            return new FactoryIOInserterScreen<>(pMenu, pInventory, pTitle);
                        }
                    }
            );
        }
    }
}
