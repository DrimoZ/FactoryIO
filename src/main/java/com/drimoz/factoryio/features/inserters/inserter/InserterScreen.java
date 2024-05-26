package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.screens.inserters.FactoryIOInserterScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class InserterScreen extends FactoryIOInserterScreen<InserterContainer> {
    public InserterScreen(InserterContainer pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }
}
