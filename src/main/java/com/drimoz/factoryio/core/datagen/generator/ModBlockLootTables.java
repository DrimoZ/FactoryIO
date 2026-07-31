package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.stream.Stream;

public class ModBlockLootTables extends BlockLootSubProvider {

    protected ModBlockLootTables() {
        super(Collections.emptySet(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        InserterRegistry.getInstance().getInserters()
                .forEach((inserter) -> dropSelf(inserter.getBlock().get()));

        ModBlocks.ENTRIES.forEach(block -> dropSelf(block.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return () -> Stream.concat(
                        InserterRegistry.getInstance().getInserters().stream()
                                .map(inserter -> (Block) inserter.getBlock().get()),
                        ModBlocks.ENTRIES.stream().map(RegistryObject::get))
                .iterator();
    }
}
