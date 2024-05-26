package com.drimoz.factoryio.shared;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FactoryIOCreativeTab {
    public static final CreativeModeTab MOD_TAB = new CreativeModeTab("modtab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(Items.DIAMOND);
        }
    };
}