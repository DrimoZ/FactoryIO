package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsGenerator extends BlockTagsProvider {

    public ModBlockTagsGenerator(PackOutput output,
                                       CompletableFuture<HolderLookup.Provider> lookupProvider,
                                       String modId,
                                       @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, modId, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        InserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            this.tag(ModTags.Blocks.MINEABLE_PICKAXE).add(inserter.getBlock().get());
            this.tag(ModTags.Blocks.TOOL_STONE).add(inserter.getBlock().get());
        });

        ModBlocks.ENTRIES.forEach(block -> {
            this.tag(ModTags.Blocks.MINEABLE_PICKAXE).add(block.get());
            this.tag(ModTags.Blocks.TOOL_STONE).add(block.get());
        });
    }

    @Override
    public String getName() {
        return "Factory'I/O Block Tags";
    }
}
