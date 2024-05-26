package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.configs.FactoryIOCommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class InserterBlockEntity extends FactoryIOInserterBlockEntity {
    public static final int GRAB_DISTANCE = 1;
    public static final int MAX_COOLDOWN = FactoryIOCommonConfigs.INSERTER_ACTION_DURATION.get();
    public static final int MAX_ENERGY_LEVEL = FactoryIOCommonConfigs.INSERTER_ENERGY_CAPACITY.get();
    public static final int MAX_ENERGY_INPUT = FactoryIOCommonConfigs.INSERTER_ENERGY_TRANSFER.get();
    public static final int MAX_ITEM_PER_ACTION = FactoryIOCommonConfigs.INSERTER_ITEM_PER_ACTION.get();
    public static final int ENERGY_PER_ACTION = FactoryIOCommonConfigs.INSERTER_ENERGY_PER_ACTION.get();

    public InserterBlockEntity(BlockPos pPos, BlockState pState) {
        super(pPos, pState, null);

        energyStorage.overrideEnergyCapacity(MAX_ENERGY_LEVEL);
        energyStorage.overrideMaxTransfer(MAX_ENERGY_INPUT);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new InserterContainer(pContainerId, pPlayerInventory, pPlayer, this.level, this.worldPosition);
    }

    @Override
    public int getMaximumItemCountPerAction() {
        return MAX_ITEM_PER_ACTION;
    }

    @Override
    public int getGrabDistance() {
        return GRAB_DISTANCE;
    }

    @Override
    public int getDurationBetweenActions() {
        return MAX_COOLDOWN;
    }

    @Override
    public int getFuelCapacity() {
        return MAX_ENERGY_LEVEL;
    }

    @Override
    public int getFuelConsumptionPerAction() {
        return ENERGY_PER_ACTION;
    }

    @Override
    public int getPreferredFuelItemBufferCount() {
        return -1;
    }
}
