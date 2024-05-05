//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.drimoz.factoryio.core.containers.energy;

import net.minecraftforge.energy.EnergyStorage;

public class FactoryIOEnergyContainer extends EnergyStorage {

    //Life cycle

    public FactoryIOEnergyContainer(int capacity) {
        super(capacity);
    }

    public FactoryIOEnergyContainer(int capacity, int maxTransfer) {
        super(capacity, maxTransfer);
    }

    public FactoryIOEnergyContainer(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public FactoryIOEnergyContainer(int capacity, int maxReceive, int maxExtract, int energy) {
        super(capacity, maxReceive, maxExtract, energy);
    }

    // Event Emitters

    protected void onEnergyChanged() {}

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
