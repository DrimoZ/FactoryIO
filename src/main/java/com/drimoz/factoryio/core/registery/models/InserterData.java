package com.drimoz.factoryio.core.registery.models;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InserterData {
    public final String displayName;
    public final String identifier;

    public final String color;

    public final boolean isFilter;
    public final boolean useEnergy;

    public final boolean isWaterlogged;
    public final boolean isAffectedByRedstone;

    public final int energyMaxCapacity;
    public final int energyMaxTransferRate;
    public final int energyConsumptionPerAction;


    public final int fuelMaxCapacity;
    public final int fuelConsumptionPerAction;

    public final int grabDistance;

    public final int filterSlotCount;

    public final int timeBetweenActions;
    public final int preferredItemCountPerAction;

    private InserterRegistries registries;

    public InserterData(
            String displayName,
            String identifier,
            String color,
            boolean isFilter,
            boolean useEnergy,
            boolean isWaterlogged,
            boolean isAffectedByRedstone,
            int energyMaxCapacity,
            int energyMaxTransferRate,
            int energyConsumptionPerAction,
            int fuelMaxCapacity,
            int fuelConsumptionPerAction,
            int grabDistance,
            int filterSlotCount,
            int timeBetweenActions,
            int preferredItemsPerAction
    ) {
        if (displayName == null || displayName.isEmpty()) throw new IllegalArgumentException("Display name cannot be null or empty.");
        this.displayName = displayName;

        if (identifier == null || identifier.isEmpty()) throw new IllegalArgumentException("Identifier cannot be null or empty.");
        this.identifier = identifier;

        if (!isValidColor(color)) throw new IllegalArgumentException("Invalid color format. Must be a valid hex color code.");
        this.color = color;

        this.isFilter = isFilter;

        this.useEnergy = useEnergy;

        this.isWaterlogged = isWaterlogged;

        this.isAffectedByRedstone = isAffectedByRedstone;

        if (!useEnergy || energyMaxCapacity < 0) this.energyMaxCapacity = 0;
        else this.energyMaxCapacity = energyMaxCapacity;

        if (!useEnergy || energyMaxTransferRate < 0) this.energyMaxTransferRate = 0;
        else if (energyMaxTransferRate > energyMaxCapacity) this.energyMaxTransferRate = this.energyMaxCapacity;
        else this.energyMaxTransferRate = energyMaxTransferRate;

        if (!useEnergy || energyConsumptionPerAction < 0) this.energyConsumptionPerAction = 0;
        else if (energyConsumptionPerAction > energyMaxCapacity) this.energyConsumptionPerAction = this.energyMaxCapacity;
        else this.energyConsumptionPerAction = energyConsumptionPerAction;

        if (useEnergy || fuelMaxCapacity < 0) this.fuelMaxCapacity = 0;
        else this.fuelMaxCapacity = fuelMaxCapacity;

        if (useEnergy || fuelConsumptionPerAction < 0) this.fuelConsumptionPerAction = 0;
        else if (fuelConsumptionPerAction > fuelMaxCapacity) this.fuelConsumptionPerAction = this.fuelMaxCapacity;
        else this.fuelConsumptionPerAction = fuelConsumptionPerAction;

        if (grabDistance < 1) this.grabDistance = 1;
        else this.grabDistance = grabDistance;

        if (!isFilter) this.filterSlotCount = 0;
        else if (filterSlotCount < 1) this.filterSlotCount = 1;
        else if (filterSlotCount > 5) this.filterSlotCount = 5;
        else this.filterSlotCount = filterSlotCount;

        if (timeBetweenActions < 1) this.timeBetweenActions = 1;
        else this.timeBetweenActions = timeBetweenActions;

        if (preferredItemsPerAction < 1) this.preferredItemCountPerAction = 1;
        else this.preferredItemCountPerAction = preferredItemsPerAction;


    }

    // Interface

    public InserterRegistries registries() {
        return this.registries;
    }

    public void setupRegistries() {
        if (registries == null) this.registries = new InserterRegistries();
    }

    // Inner work

    private boolean isValidColor(String color) {
        String hexColorPattern = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
        Pattern pattern = Pattern.compile(hexColorPattern);
        Matcher matcher = pattern.matcher(color);
        return matcher.matches();
    }

    // Interface (Utils)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InserterData that = (InserterData) o;
        return identifier.equals(that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public String toString() {
        return "InserterData={" +
                "displayName='" + displayName + '\'' +
                ", identifier='" + identifier + '\'' +
                ", color='" + color + '\'' +
                ", isFilter=" + isFilter +
                ", useEnergy=" + useEnergy +
                ", isWaterlogged=" + isWaterlogged +
                ", isAffectedByRedstone=" + isAffectedByRedstone +
                ", energyMaxCapacity=" + energyMaxCapacity +
                ", energyMaxTransferRate=" + energyMaxTransferRate +
                ", energyConsumptionPerAction=" + energyConsumptionPerAction +
                ", fuelMaxCapacity=" + fuelMaxCapacity +
                ", fuelConsumptionPerAction=" + fuelConsumptionPerAction +
                ", grabDistance=" + grabDistance +
                ", filterSlotCount=" + filterSlotCount +
                ", timeBetweenActions=" + timeBetweenActions +
                ", preferredItemsPerAction=" + preferredItemCountPerAction +
                ", registries=" + registries +
                '}';
    }
}