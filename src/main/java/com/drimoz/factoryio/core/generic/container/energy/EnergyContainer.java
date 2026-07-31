package com.drimoz.factoryio.core.generic.container.energy;

import net.minecraftforge.energy.EnergyStorage;

public class EnergyContainer extends EnergyStorage {

    //Life cycle

    public EnergyContainer(int capacity) {
        super(capacity);
    }

    public EnergyContainer(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public EnergyContainer(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public EnergyContainer(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    // Event Emitters

    protected void onEnergyChanged() {}

    /**
     * Réception d'énergie depuis un bloc voisin.
     *
     * <p>Surchargée pour la seule raison de déclencher {@link #onEnergyChanged()} :
     * {@code EnergyStorage#receiveEnergy} incrémente le champ directement et n'offre aucun
     * point d'accroche. Sans cette surcharge, la machine n'était prévenue que lorsqu'elle
     * <i>consommait</i>, jamais lorsqu'on l'alimentait — de sorte que le réveil sur retour
     * de courant (BUG-037) ne se déclenchait pas sur le cas qu'il visait, et que l'énergie
     * reçue ne marquait pas le block entity comme modifié.
     */
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);

        if (received > 0 && !simulate) {
            this.onEnergyChanged();
        }

        return received;
    }

    // Interface (Consommation interne)

    /**
     * Consomme de l'énergie pour le fonctionnement propre de la machine.
     *
     * <p>Volontairement distinct de {@link #extractEnergy(int, boolean)} : ce dernier
     * représente le contrat <i>externe</i> de la capability et peut être verrouillé par
     * les sous-classes pour empêcher les blocs voisins de pomper l'énergie. La
     * consommation interne, elle, doit toujours aboutir (cf. BUG-003).
     *
     * @return la quantité réellement consommée
     */
    public int consumeInternal(int amount) {
        if (amount <= 0) return 0;

        int consumed = Math.min(this.energy, amount);
        if (consumed > 0) {
            this.energy -= consumed;
            this.onEnergyChanged();
        }

        return consumed;
    }

    // Getters

    public int getCurrentEnergy() {
        return this.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return this.getMaxEnergyStored();
    }

    // Setters

    public void overrideCurrentEnergy(int energy) {
        this.energy = energy;

        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        } else if (this.energy < 0) {
            this.energy = 0;
        }

        this.onEnergyChanged();
    }

    public void overrideEnergyCapacity(int capacity) {
        if (this.capacity == capacity) return;

        this.capacity = capacity;
        if (this.energy > capacity) {
            this.energy = capacity;
        }

        this.onEnergyChanged();
    }

    public void overrideMaxTransfer(int maxTransfer) {
        this.maxReceive = maxTransfer;
        this.maxExtract = maxTransfer;
    }
}
