package com.drimoz.factoryio.core.generic.container.slots;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * La « main » de l'inserter, affichée mais intouchable.
 *
 * <p>Ni dépôt ni retrait : son contenu appartient au mouvement en cours. Le laisser
 * manipuler ferait disparaître un item que la machine à états croit tenir.
 */
public class InserterBufferSlot extends SlotItemHandler {

    public InserterBufferSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
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
}
