package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterItem;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class Inserter {

    // Private Properties

    private final ResourceLocation id;
    private Component displayName;
    private boolean filterable;
    private boolean useEnergy;
    private boolean affectedByRedstone;
    private int energyCapacity;
    private int energyTransferRate;
    private int energyConsumption;
    private int fuelCapacity;
    private int fuelConsumption;
    private int grabDistance;
    private int filterSlotCount;
    private int cooldownBetweenActions;
    private int preferredItemCountPerAction;
    private ResourceLocation texture;

    private Supplier<FactoryIOInserterEntityBlock> blockSupplier;
    private Supplier<FactoryIOInserterItem> itemSupplier;
    private Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityTypeSupplier;
    private Supplier<MenuType<FactoryIOInserterContainer>> menuTypeSupplier;


    // Lifecycle

    public Inserter (
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int cooldownBetweenActions, int preferredItemCountPerAction,
            int fuelCapacity, int fuelConsumption
    ) {
        this(
                id, affectedByRedstone,
                grabDistance, cooldownBetweenActions, preferredItemCountPerAction,
                false, false,
                fuelCapacity, fuelConsumption,
                -1, -1, -1
        );
    }

    public Inserter (
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int cooldownBetweenActions, int preferredItemCountPerAction,
            boolean filterable,
            int energyCapacity, int energyTransferRate, int energyConsumption
    ) {
        this(
                id, affectedByRedstone,
                grabDistance, cooldownBetweenActions, preferredItemCountPerAction,
                filterable, true,
                -1, -1,
                energyCapacity, energyTransferRate, energyConsumption
        );
    }

    public Inserter (
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int cooldownBetweenActions, int preferredItemCountPerAction,
            boolean filterable, boolean useEnergy,
            int fuelCapacity, int fuelConsumption,
            int energyCapacity, int energyTransferRate, int energyConsumption
    ) {
        this.id = id;

        this.filterable = filterable;

        setUseEnergy(useEnergy);

        this.affectedByRedstone = affectedByRedstone;

        setEnergyCapacity(energyCapacity);
        setEnergyTransferRate(energyTransferRate);
        setEnergyConsumption(energyConsumption);

        setFuelCapacity(fuelCapacity);
        setFuelConsumption(fuelConsumption);


        setGrabDistance(grabDistance);
        setCooldownBetweenActions(cooldownBetweenActions);
        setPreferredItemCountPerAction(preferredItemCountPerAction);

        setupTexture();

    }

    // Interface

    public ResourceLocation getId() {
        return this.id;
    }

    public String getName() {
        return this.getId().getPath();
    }

    public String getModId() {
        return this.getId().getNamespace();
    }

    public String getNameWithSuffix(String suffix) {
        return String.format("%s_%s", this.getName(), suffix);
    }

    public Component getDisplayName() {
        return (Component)(this.displayName != null ? this.displayName : Component.nullToEmpty(String.format("crop.%s.%s", this.getModId(), this.getName())));
    }

    public Inserter setDisplayName(Component name) {
        this.displayName = name;
        return this;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean useEnergy() {
        return useEnergy;
    }

    public void setUseEnergy(boolean useEnergy) {
        this.useEnergy = isFilterable() || useEnergy;
    }

    public boolean isAffectedByRedstone() {
        return affectedByRedstone;
    }

    public void setAffectedByRedstone(boolean affectedByRedstone) {
        this.affectedByRedstone = affectedByRedstone;
    }

    public int getEnergyCapacity() {
        return energyCapacity;
    }

    public void setEnergyCapacity(int energyCapacity) {
        this.energyCapacity = useEnergy ? energyCapacity > 0 ? energyCapacity : 1 : -1;
    }

    public int getEnergyTransferRate() {
        return energyTransferRate;
    }

    public void setEnergyTransferRate(int energyTransferRate) {
        this.energyTransferRate = useEnergy ? energyTransferRate > 0 ? energyTransferRate : 1 : -1;
    }

    public int getEnergyConsumption() {
        return energyConsumption;
    }

    public void setEnergyConsumption(int energyConsumption) {
        this.energyConsumption = useEnergy ? energyConsumption > 0 ? energyConsumption : 1 : -1;
    }

    public int getFuelCapacity() {
        return fuelCapacity;
    }

    public void setFuelCapacity(int fuelCapacity) {
        this.fuelCapacity = !useEnergy ? fuelCapacity > 0 ? fuelCapacity : 1 : -1;
    }

    public int getFuelConsumption() {
        return fuelConsumption;
    }

    public void setFuelConsumption(int fuelConsumption) {
        this.fuelConsumption = !useEnergy ? fuelConsumption > 0 ? fuelConsumption : 1 : -1;
    }

    public int getGrabDistance() {
        return grabDistance;
    }

    public void setGrabDistance(int grabDistance) {
        this.grabDistance = grabDistance > 0 ? grabDistance : 1;
    }

    public int getCooldownBetweenActions() {
        return cooldownBetweenActions;
    }

    public void setCooldownBetweenActions(int cooldownBetweenActions) {
        this.cooldownBetweenActions = cooldownBetweenActions > 0 ? cooldownBetweenActions : 1;
    }

    public int getPreferredItemCountPerAction() {
        return preferredItemCountPerAction;
    }

    public void setPreferredItemCountPerAction(int preferredItemCountPerAction) {
        this.preferredItemCountPerAction = preferredItemCountPerAction > 0 ? preferredItemCountPerAction : 1;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public Supplier<FactoryIOInserterEntityBlock> getBlock() {
        return this.blockSupplier;
    }

    public void setBlock(Supplier<FactoryIOInserterEntityBlock> blockSupplier) {
        this.blockSupplier = blockSupplier;
    }

    public Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> getBlockEntityType() {
        return this.blockEntityTypeSupplier;
    }

    public Supplier<FactoryIOInserterItem> getItem() {
        return this.itemSupplier;
    }

    public void setItem(Supplier<FactoryIOInserterItem> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }


    public void setBlockEntityType(Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityTypeSupplier) {
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
    }

    public Supplier<MenuType<FactoryIOInserterContainer>> getMenuType() {
        return this.menuTypeSupplier;
    }

    public void setMenuType(Supplier<MenuType<FactoryIOInserterContainer>> menuTypeSupplier) {
        this.menuTypeSupplier = menuTypeSupplier;
    }

    // Inner Work

    private void setupTexture() {
        this.texture = new ResourceLocation(FactoryIO.MOD_ID, "block/inserters/" + getName());
    }


    @Override
    public String toString() {
        return "Inserter{" +
                "id=" + id +
                ", displayName=" + displayName +
                ", filterable=" + filterable +
                ", useEnergy=" + useEnergy +
                ", affectedByRedstone=" + affectedByRedstone +
                ", energyCapacity=" + energyCapacity +
                ", energyTransferRate=" + energyTransferRate +
                ", energyConsumption=" + energyConsumption +
                ", fuelCapacity=" + fuelCapacity +
                ", fuelConsumption=" + fuelConsumption +
                ", grabDistance=" + grabDistance +
                ", filterSlotCount=" + filterSlotCount +
                ", cooldownBetweenActions=" + cooldownBetweenActions +
                ", preferredItemCountPerAction=" + preferredItemCountPerAction +
                ", texture=" + texture +
                ", blockEntityTypeSupplier=" + blockEntityTypeSupplier +
                ", blockSupplier=" + blockSupplier +
                ", menuTypeSupplier=" + menuTypeSupplier +
                '}';
    }
}