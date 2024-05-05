package com.drimoz.factoryio.core.items;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public abstract class FactoryIOBlockItem extends BlockItem {
    public FactoryIOBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }
}
