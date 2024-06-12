package com.drimoz.factoryio.a_core.generic.container.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SlotInserterBuffer extends SlotItemHandler {
    IItemHandler ih;
    public SlotInserterBuffer(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
        this.ih = itemHandler;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPickup(Player playerIn) {
        return false;
    }

    public IItemHandler getIh() {
        return ih;

    }
}