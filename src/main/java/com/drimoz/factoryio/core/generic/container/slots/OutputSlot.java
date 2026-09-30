package com.drimoz.factoryio.core.generic.container.slots;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * La « main » de l'inserter : on peut la vider, pas la remplir.
 *
 * <p>Le retrait est sans risque : l'inserter qui arrive pour déposer une main vide repart
 * simplement (voir {@code InserterBlockEntity#tryDrop}). Le dépôt, lui, reste interdit — un
 * item posé à la main serait livré sans avoir été saisi.
 */
public class OutputSlot extends SlotItemHandler {

    public OutputSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}
