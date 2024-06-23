package com.drimoz.factoryio.core.generic.item;

import net.minecraft.world.item.ItemStack;

public class FactoryIOFoilItem extends FactoryIOItem {
    public FactoryIOFoilItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }
}
