package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.FactoryIOItems;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FactoryIOItemTagsGenerator extends ItemTagsProvider {

    public FactoryIOItemTagsGenerator(PackOutput output,
                                      CompletableFuture<HolderLookup.Provider> lookupProvider,
                                      CompletableFuture<TagLookup<Block>> blockTags,
                                      String modId,
                                      @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) ->
                this.tag(FactoryIOTags.Items.INSERTERS).add(inserter.getItem().get()));

        this.tag(FactoryIOTags.Items.PLATES).add(
                FactoryIOItems.IRON_PLATE.get(), FactoryIOItems.COPPER_PLATE.get(), FactoryIOItems.STEEL_PLATE.get());

        this.tag(FactoryIOTags.Items.PLATES_IRON).add(FactoryIOItems.IRON_PLATE.get());
        this.tag(FactoryIOTags.Items.PLATES_STEEL).add(FactoryIOItems.STEEL_PLATE.get());
        this.tag(FactoryIOTags.Items.PLATES_COPPER).add(FactoryIOItems.COPPER_PLATE.get());
    }

    @Override
    public String getName() {
        return "Factory'I/O Item Tags";
    }
}
