package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Collections;

public class FactoryIOBlockLootTables extends BlockLootSubProvider {

    protected FactoryIOBlockLootTables() {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        FactoryIOInserterRegistry.getInstance().getInserters()
                .forEach((inserter) -> dropSelf(inserter.getBlock().get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return () -> FactoryIOInserterRegistry.getInstance().getInserters().stream()
                .map(inserter -> (Block) inserter.getBlock().get())
                .iterator();
    }
}
