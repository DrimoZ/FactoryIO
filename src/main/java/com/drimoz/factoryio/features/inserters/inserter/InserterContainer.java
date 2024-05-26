package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.containers.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.registery.custom.FactoryIOContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

public class InserterContainer extends FactoryIOInserterContainer {
    public InserterContainer(int pContainerId, Inventory pPlayerInv, Player pPlayer, Level pLevel, BlockPos pPos) {
        super((MenuType) FactoryIOContainers.INSERTER_MENU.get(), pContainerId, pLevel, pPos, pPlayerInv, pPlayer);
    }
}
