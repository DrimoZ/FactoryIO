package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class FactoryIOItemTagsGenerator extends TagsProvider<Item> {
    public FactoryIOItemTagsGenerator(DataGenerator pGenerator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, Registry.ITEM, modId, existingFileHelper);
    }

    @Override
    protected void addTags() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            this.tag(FactoryIOTags.Items.INSERTERS).add(inserter.getItem().get());
        });
    }
    @Override
    public String getName() {
        return FactoryIO.MOD_ID + " item tags generator";
    }


}
