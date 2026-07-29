package com.drimoz.factoryio.core.datagen.generator;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

public class FactoryIOLootGenerator extends LootTableProvider {

    public FactoryIOLootGenerator(PackOutput output) {
        super(
                output,
                Set.of(),
                List.of(new SubProviderEntry(FactoryIOBlockLootTables::new, LootContextParamSets.BLOCK)));
    }
}
