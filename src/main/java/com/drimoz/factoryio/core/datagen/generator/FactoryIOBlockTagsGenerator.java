package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.core.init.FactoryIOTags;
import com.drimoz.factoryio.core.registery.FactoryIOInserterRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;


public class FactoryIOBlockTagsGenerator extends TagsProvider<Block> {
    public FactoryIOBlockTagsGenerator(DataGenerator pGenerator, String modId, @Nullable ExistingFileHelper existingFileHelper) {
        super(pGenerator, Registry.BLOCK, modId, existingFileHelper);
    }

    @Override
    protected void addTags() {
        FactoryIOInserterRegistry.getInstance().getInserters().forEach((inserter) -> {
            this.tag(FactoryIOTags.Blocks.MINEABLE_PICKAXE).add(inserter.getBlock().get());
            this.tag(FactoryIOTags.Blocks.TOOL_STONE).add(inserter.getBlock().get());

        });
    }

    @Override
    public String getName() {
        return "Items Tags";
    }
}
