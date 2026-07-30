package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.container.slots.GhostSlot;
import net.minecraftforge.items.IItemHandler;

/**
 * Slot de filtre d'un inserter : un {@link GhostSlot} qui connaît son rang.
 *
 * <p>Le rang sert à deux choses : basculer la correspondance par tag au clic droit
 * (FIO-069), et permettre à l'écran de retrouver le mode d'un slot pour le teinter.
 */
public class InserterFilterSlot extends GhostSlot {

    private final FactoryIOInserterBlockEntity inserter;
    private final int filterIndex;

    public InserterFilterSlot(
            FactoryIOInserterBlockEntity inserter, IItemHandler itemHandler,
            int filterIndex, int xPosition, int yPosition) {

        super(itemHandler, inserter.LAYOUT.filter(filterIndex), xPosition, yPosition);

        this.inserter = inserter;
        this.filterIndex = filterIndex;
    }

    public boolean isTagFilter() {
        return this.inserter.isTagFilter(this.filterIndex);
    }

    /** Clic droit : bascule entre correspondance exacte et correspondance par tag. */
    @Override
    protected boolean onAlternateClick() {
        this.inserter.toggleTagFilter(this.filterIndex);

        return true;
    }
}
