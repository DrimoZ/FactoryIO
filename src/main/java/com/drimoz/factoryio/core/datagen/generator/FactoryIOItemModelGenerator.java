package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class FactoryIOItemModelGenerator extends ItemModelProvider {

    public FactoryIOItemModelGenerator(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
    }

    @Override
    protected void registerModels() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) ->
                withExistingParent("item/" + inserter.getName(), modLoc("block/" + inserter.getName())));

        FactoryIOItems.ENTRIES.forEach((registryObject) -> {
            String itemName = registryObject.getId().getPath();
            singleTexture(itemName, mcLoc("item/generated"), "layer0", modLoc("item/" + itemName));
        });
    }
}
