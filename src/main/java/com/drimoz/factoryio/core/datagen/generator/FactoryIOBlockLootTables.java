package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.data.loot.BlockLoot;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FactoryIOBlockLootTables extends BlockLoot {
    @Override
    protected void addTables() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> dropSelf(inserter.getBlock().get()));
    }

    //@Override
    //protected Iterable<Block> getKnownBlocks() {
    //    List<Block> list = new ArrayList<>();
    //    FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> list.add(inserter.getBlock().get()));
//
    //    return list.stream()::iterator;
    //}

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return () -> FactoryIOInserterRegistry.getInstance().getInserters().stream()
                .map(inserter -> (Block) inserter.getBlock().get())
                .iterator();
    }
}
