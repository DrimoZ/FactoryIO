package com.drimoz.factoryio.core.generic.container.slots;

import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.init.ModTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Slot de carburant d'un burner inserter.
 *
 * <p>Deux conditions cumulatives : l'item doit brûler <b>et</b> appartenir au tag
 * {@code factor_io:inserter_fuel}. Le temps de combustion seul ferait accepter tout ce
 * qui alimente un four ; le tag seul laisserait passer un item qui ne brûle pas et
 * bloquerait le slot.
 *
 * <p>La version précédente tolérait aussi le seau vide, mais la condition était morte : un
 * seau n'a pas de temps de combustion et n'est pas dans le tag, si bien que le {@code &&}
 * l'écartait de toute façon. Le résidu d'un seau de lave ressort désormais par la
 * capability, comme sur un four.
 */
public class InserterFuelSlot extends SlotItemHandler {

    private final InserterBlockEntity entity;

    public InserterFuelSlot(InserterBlockEntity entity, IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);

        this.entity = entity;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return ForgeHooks.getBurnTime(stack, null) > 0 && stack.is(ModTags.Items.INSERTER_FUEL);
    }

    @Override
    public boolean isActive() {
        return !this.entity.IS_ENERGY;
    }
}
