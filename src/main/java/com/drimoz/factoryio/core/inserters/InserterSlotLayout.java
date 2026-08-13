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
 * les slots de filtre, puis les slots d'amélioration.
 *
 * <p><b>Les améliorations sont en dernier, et ce n'est pas un détail de mise en page.</b>
 * Les insérer avant les filtres décalerait tous les index suivants, et un monde sauvegardé
 * avant leur arrivée verrait ses filtres réapparaître comme carburant. En queue, les index
 * déjà écrits en NBT gardent leur sens et les nouveaux slots naissent vides.
 *
 * @param fuel         index du slot de carburant, ou {@link #NONE} pour un inserter électrique
 * @param firstFilter  index du premier slot de filtre, ou {@link #NONE}
 * @param filterCount  nombre de slots de filtre
 * @param firstUpgrade index du premier slot d'amélioration, ou {@link #NONE}
 * @param upgradeCount nombre de slots d'amélioration
 * @param size         taille totale de l'inventaire
 */
public record InserterSlotLayout(
        int fuel, int firstFilter, int filterCount, int firstUpgrade, int upgradeCount, int size) {

    /** Index d'un slot absent sur ce type d'inserter. */
    public static final int NONE = -1;

    /** Le buffer de transport existe toujours et occupe le premier slot. */
    public static final int BUFFER = 0;

    /** Nombre de slots de filtre d'un inserter filtrant. */
    public static final int FILTER_SLOT_COUNT = 5;

    public static InserterSlotLayout of(Inserter inserter) {
        return of(inserter.useEnergy(), inserter.isFilterable(), inserter.getUpgradeSlots());
    }

    public static InserterSlotLayout of(boolean useEnergy, boolean filterable, int upgradeSlots) {
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

        int firstUpgrade = NONE;
        int upgradeCount = 0;
        if (upgradeSlots > 0) {
            firstUpgrade = next;
            upgradeCount = upgradeSlots;
            next += upgradeCount;
        }

        return new InserterSlotLayout(fuel, firstFilter, filterCount, firstUpgrade, upgradeCount, next);
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

    public boolean hasUpgrades() {
        return firstUpgrade != NONE;
    }

    /** @return l'index du n-ième slot d'amélioration (0-based) */
    public int upgrade(int index) {
        if (!hasUpgrades() || index < 0 || index >= upgradeCount) {
            throw new IndexOutOfBoundsException("Slot d'amélioration inexistant : " + index);
        }

        return firstUpgrade + index;
    }

    public boolean isUpgrade(int slot) {
        return hasUpgrades() && slot >= firstUpgrade && slot < firstUpgrade + upgradeCount;
    }

    /**
     * Slots dont le contenu doit tomber au sol quand le bloc est cassé.
     *
     * <p>Les modules en font partie : ce sont des items que le joueur a fabriqués, pas un
     * réglage. Seuls les filtres restent au sol — ce sont des items fantômes, qui n'ont
     * jamais quitté l'inventaire de personne.
     */
    public boolean isDroppable(int slot) {
        return !isFilter(slot);
    }
}
