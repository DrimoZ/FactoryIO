package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.model.Inserter;

/**
 * Plan d'inventaire d'un inserter — source unique de vérité pour les index de slots.
 *
 * <p>Le code précédent faisait cohabiter <b>trois</b> conventions concurrentes
 * (cf. DT-03) : des constantes {@code FILTER_SLOTS = {2,3,4,5,6}} sur le block entity,
 * des {@code FILTER_SLOTS[i] - 1} disséminés dans le menu, et des
 * {@code getSlots() - 5} dans la logique de filtrage. Sur un inserter électrique
 * filtrant, le premier slot de filtre portait l'index 1 — c'est-à-dire la valeur de la
 * constante {@code FUEL_SLOT}.
 *
 * <p>Disposition : le buffer d'abord, puis le slot de carburant s'il y en a un, puis
 * les slots de filtre.
 *
 * @param fuel        index du slot de carburant, ou {@link #NONE} pour un inserter électrique
 * @param firstFilter index du premier slot de filtre, ou {@link #NONE}
 * @param filterCount nombre de slots de filtre
 * @param size        taille totale de l'inventaire
 */
public record InserterSlotLayout(int fuel, int firstFilter, int filterCount, int size) {

    /** Index d'un slot absent sur ce type d'inserter. */
    public static final int NONE = -1;

    /** Le buffer de transport existe toujours et occupe le premier slot. */
    public static final int BUFFER = 0;

    /** Nombre de slots de filtre d'un inserter filtrant. */
    public static final int FILTER_SLOT_COUNT = 5;

    public static InserterSlotLayout of(Inserter inserter) {
        return of(inserter.useEnergy(), inserter.isFilterable());
    }

    public static InserterSlotLayout of(boolean useEnergy, boolean filterable) {
        int next = BUFFER + 1;

        int fuel = NONE;
        if (!useEnergy) {
            fuel = next;
            next++;
        }

        int firstFilter = NONE;
        int filterCount = 0;
        if (filterable) {
            firstFilter = next;
            filterCount = FILTER_SLOT_COUNT;
            next += filterCount;
        }

        return new InserterSlotLayout(fuel, firstFilter, filterCount, next);
    }

    // Interface

    public boolean hasFuelSlot() {
        return fuel != NONE;
    }

    public boolean hasFilters() {
        return firstFilter != NONE;
    }

    /** @return l'index du n-ième slot de filtre (0-based) */
    public int filter(int index) {
        if (!hasFilters() || index < 0 || index >= filterCount) {
            throw new IndexOutOfBoundsException("Slot de filtre inexistant : " + index);
        }

        return firstFilter + index;
    }

    public boolean isFilter(int slot) {
        return hasFilters() && slot >= firstFilter && slot < firstFilter + filterCount;
    }

    /** Slots dont le contenu doit tomber au sol quand le bloc est cassé. */
    public boolean isDroppable(int slot) {
        return !isFilter(slot);
    }
}
