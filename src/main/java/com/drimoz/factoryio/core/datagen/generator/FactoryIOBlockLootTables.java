package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.data.loot.BlockLoot;

public class FactoryIOBlockLootTables extends BlockLoot {
    @Override
    protected void addTables() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> dropSelf(inserter.getBlock().get()));
    }
}
