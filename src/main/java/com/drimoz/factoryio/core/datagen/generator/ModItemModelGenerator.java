package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelGenerator extends ItemModelProvider {

    public ModItemModelGenerator(PackOutput output, String modid, ExistingFileHelper exFileHelper) {
        super(output, modid, exFileHelper);
    }

    @Override
    protected void registerModels() {
        InserterRegistry.getInstance().getInserters().forEach((inserter) ->
                withExistingParent("item/" + inserter.getName(), modLoc("block/" + inserter.getName())));

        ModItems.ENTRIES.forEach((registryObject) -> {
            String itemName = registryObject.getId().getPath();
            singleTexture(itemName, mcLoc("item/generated"), "layer0", modLoc("item/" + itemName));
        });
    }
}
