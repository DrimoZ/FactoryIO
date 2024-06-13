package com.drimoz.factoryio.core.generic.container.slots;

import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.generic.tag.FactoryIOTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SlotInserterFuel extends SlotItemHandler {
    FactoryIOInserterBlockEntity entity;
    public SlotInserterFuel(FactoryIOInserterBlockEntity entity, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.entity = entity;
    }

    public boolean mayPlace(ItemStack stack) {
        return (ForgeHooks.getBurnTime(stack, null) > 0 || isBucket(stack)) && stack.is(FactoryIOTags.Items.INSERTER_FUEL); //(stack.getBurnTime(null) > 0 || isBucket(stack)) &&
    }

    public int getMaxStackSize(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.getItem() == Items.BUCKET;
    }

    public boolean isActive() {
        return !this.entity.IS_ENERGY;
    }
}
