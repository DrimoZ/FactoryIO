package com.drimoz.factoryio.core.generic.container.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class SlotInserterFilter extends SlotItemHandler {
    protected ItemStackHandler itemHandler;
    protected int index;

    public SlotInserterFilter(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.itemHandler = (ItemStackHandler) itemHandler;
        this.index = index;
    }

    @Override
    public int getMaxStackSize() {
        return  1;
    }

    @Override
    public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
        if (!pStack.isEmpty() && this.mayPlace(pStack)) {
            ItemStack is = pStack.copy();
            is.setCount(1);
            this.set(is);
        }
        return pStack;
    }

    @Override
    public Optional<ItemStack> tryRemove(int pCount, int pDecrement, Player pPlayer) {
        return Optional.empty();
    }

    @Override
    protected void onQuickCraft(ItemStack pStack, int pAmount) {

    }

    @NotNull
    @Override
    public ItemStack remove(int amount) {
        this.getItemHandler().extractItem(index, amount, false);
        return ItemStack.EMPTY;
    }
}