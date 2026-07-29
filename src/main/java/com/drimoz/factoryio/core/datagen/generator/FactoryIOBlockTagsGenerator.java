package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class FactoryIOBlockTagsGenerator extends BlockTagsProvider {

    public FactoryIOBlockTagsGenerator(PackOutput output,
                                       CompletableFuture<HolderLookup.Provider> lookupProvider,
                                       String modId,
                                       @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            this.tag(FactoryIOTags.Blocks.MINEABLE_PICKAXE).add(inserter.getBlock().get());
            this.tag(FactoryIOTags.Blocks.TOOL_STONE).add(inserter.getBlock().get());
        });
    }

    @Override
    public String getName() {
        return "Factory'I/O Block Tags";
    }
}
