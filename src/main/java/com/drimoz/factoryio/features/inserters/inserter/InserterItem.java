package com.drimoz.factoryio.features.inserters.inserter;

import com.drimoz.factoryio.core.items.inserters.FactoryIOInserterItemRenderer;
import com.drimoz.factoryio.core.registery.models.InserterData;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.IItemRenderProperties;

import java.util.function.Consumer;

public abstract class InserterItem extends BlockItem {
    public InserterItem(Block block, Item.Properties properties, InserterData inserterData) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IItemRenderProperties> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IItemRenderProperties() {

            private final BlockEntityWithoutLevelRenderer renderer = new FactoryIOInserterItemRenderer("");

            @Override
            public BlockEntityWithoutLevelRenderer getItemStackRenderer() {
                return renderer;
            }
        });
    }
}
