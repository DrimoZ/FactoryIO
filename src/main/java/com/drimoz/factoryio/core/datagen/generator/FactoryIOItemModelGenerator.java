package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Direction;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

import java.util.Objects;


public class FactoryIOItemModelGenerator extends ItemModelProvider {
    public FactoryIOItemModelGenerator(DataGenerator gen, String modid, ExistingFileHelper exFileHelper) {
        super(gen, modid, exFileHelper);
    }

    @Override
    protected void registerModels() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            FactoryIOInserterEntityBlock block = inserter.getBlock().get();
            String blockName = Objects.requireNonNull(block.getRegistryName()).getPath();

            withExistingParent("item/" + blockName, modLoc("block/" + blockName));
        });

        FactoryIOItems.ENTRIES.keySet().forEach((registry) -> {
            String itemName = registry.getId().getPath();
            singleTexture(itemName, mcLoc("item/generated"), "layer0", modLoc("item/" + itemName));
        });
    }
}
