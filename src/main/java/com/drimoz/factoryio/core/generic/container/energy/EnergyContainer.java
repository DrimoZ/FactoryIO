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

    /** @return {@code true} si la machine dispose d'au moins {@code amount} FE. */
    public boolean hasEnergy(int amount) {
        return this.energy >= amount;
    }

    // Getters

    public int getCurrentEnergy() {
        return this.getEnergyStored();
    }

    public int getEnergyCapacity() {
        return this.getMaxEnergyStored();
    }

    public int getMaxReceive() {
        return this.maxReceive;
    }

    public int getMaxExtract() {
        return this.maxExtract;
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

    public EnergyStorage overrideEnergyCapacity(int capacity) {
        this.capacity = capacity;
        if (this.energy > capacity) {
            this.energy = capacity;
        }

        this.onEnergyChanged();
        return this;
    }

    public EnergyStorage overrideMaxTransfer(int maxTransfer) {
        this.overrideMaxReceive(maxTransfer);
        this.overrideMaxExtract(maxTransfer);
        return this;
    }

    public EnergyStorage overrideMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
        return this;
    }

    public EnergyStorage overrideMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
        return this;
    }
}
