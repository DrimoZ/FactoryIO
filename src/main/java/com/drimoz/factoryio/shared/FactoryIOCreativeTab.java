package com.drimoz.factoryio.shared;

import com.drimoz.factoryio.core.init.FactoryIOItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FactoryIOCreativeTab {
    public static final CreativeModeTab MOD_TAB = new CreativeModeTab("creativeTab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(FactoryIOItems.ELECTRONIC_CIRCUIT.get());
        }
    };
}