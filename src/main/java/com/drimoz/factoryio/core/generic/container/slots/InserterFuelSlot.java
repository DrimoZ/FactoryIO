package com.drimoz.factoryio.core.generic.container.slots;

import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.init.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class InserterFuelSlot extends SlotItemHandler {
    InserterBlockEntity entity;
    public InserterFuelSlot(InserterBlockEntity entity, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        this.entity = entity;
    }

    public boolean mayPlace(ItemStack stack) {
        return (ForgeHooks.getBurnTime(stack, null) > 0 || isBucket(stack)) && stack.is(ModTags.Items.INSERTER_FUEL); //(stack.getBurnTime(null) > 0 || isBucket(stack)) &&
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
