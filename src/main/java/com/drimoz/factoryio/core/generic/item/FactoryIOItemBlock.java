package com.drimoz.factoryio.core.generic.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public abstract class FactoryIOItemBlock extends BlockItem {
    public FactoryIOItemBlock(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }
}
