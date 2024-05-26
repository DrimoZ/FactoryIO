package com.drimoz.factoryio.core.registery.custom;


import com.drimoz.factoryio.features.inserters.inserter.InserterScreen;
import net.minecraft.client.gui.screens.MenuScreens;

public class FactoryIOScreens {
    public static void registerScreens() {
        MenuScreens.register(FactoryIOContainers.INSERTER_MENU.get(), InserterScreen::new);
    }
}
