package com.drimoz.factoryio.core.registery.models;

import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.a_core.inserters.FactoryIOInserterContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class InserterRegistries {
    private Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityType;
    private Supplier<FactoryIOInserterEntityBlock> blockSupplier;
    private Supplier<MenuType<FactoryIOInserterContainer>> menuSupplier;

    public InserterRegistries() {
    }

    public Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> getBlockEntity() {
        return this.blockEntityType;
    }

    public void setBlockEntityType(Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityType) {
        this.blockEntityType = blockEntityType;
    }

    public Supplier<FactoryIOInserterEntityBlock> getBlock() {
        return this.blockSupplier;
    }

    public void setBlockSupplier(Supplier<FactoryIOInserterEntityBlock> blockSupplier) {
        this.blockSupplier = blockSupplier;
    }

    public Supplier<MenuType<FactoryIOInserterContainer>> getMenu() {
        return this.menuSupplier;
    }

    public void setMenuSupplier(Supplier<MenuType<FactoryIOInserterContainer>> menuSupplier) {
        this.menuSupplier = menuSupplier;
    }
}