package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsGenerator extends ItemTagsProvider {

    public ModItemTagsGenerator(PackOutput output,
                                      CompletableFuture<HolderLookup.Provider> lookupProvider,
                                      CompletableFuture<TagLookup<Block>> blockTags,
                                      String modId,
                                      @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        InserterRegistry.getInstance().getInserters().forEach((inserter) ->
                this.tag(ModTags.Items.INSERTERS).add(inserter.getItem().get()));

        this.tag(ModTags.Items.PLATES).add(
                ModItems.IRON_PLATE.get(), ModItems.COPPER_PLATE.get(), ModItems.STEEL_PLATE.get());

        this.tag(ModTags.Items.PLATES_IRON).add(ModItems.IRON_PLATE.get());
        this.tag(ModTags.Items.PLATES_STEEL).add(ModItems.STEEL_PLATE.get());
        this.tag(ModTags.Items.PLATES_COPPER).add(ModItems.COPPER_PLATE.get());
    }

    @Override
    public String getName() {
        return "Factory'I/O Item Tags";
    }
}
