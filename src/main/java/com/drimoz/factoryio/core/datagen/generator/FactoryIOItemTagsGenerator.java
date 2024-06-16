package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;


public class FactoryIOItemTagsGenerator extends TagsProvider<Item> {
    public FactoryIOItemTagsGenerator(DataGenerator pGenerator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, Registry.ITEM, modId, existingFileHelper);
    }

    @Override
    protected void addTags() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> this.tag(FactoryIOTags.Items.INSERTERS).add(inserter.getItem().get()));

        this.tag(FactoryIOTags.Items.PLATES).add(FactoryIOItems.IRON_PLATE.get(), FactoryIOItems.COPPER_PLATE.get(), FactoryIOItems.STEEL_PLATE.get());

        this.tag(FactoryIOTags.Items.PLATES_IRON).add(FactoryIOItems.IRON_PLATE.get());
        this.tag(FactoryIOTags.Items.PLATES_STEEL).add(FactoryIOItems.STEEL_PLATE.get());
        this.tag(FactoryIOTags.Items.PLATES_COPPER).add(FactoryIOItems.COPPER_PLATE.get());
    }

    @Override
    public String getName() {
        return "Items Tags";
    }
}
