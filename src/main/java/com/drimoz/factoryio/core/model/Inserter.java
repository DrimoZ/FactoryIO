package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterEntityBlock;
import com.drimoz.factoryio.core.inserters.FactoryIOInserterItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * Définition d'un type d'inserter.
 *
 * <p>Les paramètres de gameplay sont <b>immuables</b> et validés à la construction.
 * L'ancienne version exposait quatorze setters publics dont l'ordre d'appel comptait
 * — {@code setUseEnergy} devait précéder {@code setEnergyCapacity}, faute de quoi la
 * capacité était silencieusement remise à -1 (cf. DT-04).
 *
 * <p>Les valeurs hors bornes sont toujours ramenées dans le domaine valide, mais avec
 * un message dans le journal : une erreur dans un JSON utilisateur ne doit pas passer
 * inaperçue.
 *
 * <p>Seules les références runtime (bloc, item, block entity, menu) restent mutables :
 * elles sont renseignées pendant l'enregistrement, une fois par type.
 */
public class Inserter {

    // Private Properties

    private final ResourceLocation id;

    private final boolean filterable;
    private final boolean useEnergy;
    private final boolean affectedByRedstone;

    private final int energyCapacity;
    private final int energyTransferRate;
    private final int energyConsumption;

    private final int fuelCapacity;
    private final int fuelConsumption;

    private final int grabDistance;
    private final int ticksPerSwing;
    private final int preferredItemCountPerAction;

    private ResourceLocation texture;

    private Supplier<FactoryIOInserterEntityBlock> blockSupplier;
    private Supplier<FactoryIOInserterItem> itemSupplier;
    private Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityTypeSupplier;
    private Supplier<MenuType<FactoryIOInserterContainer>> menuTypeSupplier;

    private final Translation translation = new Translation();

    // Lifecycle

    /** Inserter à carburant. */
    public static Inserter burner(
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int ticksPerSwing, int preferredItemCountPerAction,
            boolean filterable,
            int fuelCapacity, int fuelConsumption
    ) {
        return new Inserter(
                id, affectedByRedstone,
                grabDistance, ticksPerSwing, preferredItemCountPerAction,
                filterable, false,
                fuelCapacity, fuelConsumption,
                0, 0, 0);
    }

    /** Inserter électrique. */
    public static Inserter electric(
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int ticksPerSwing, int preferredItemCountPerAction,
            boolean filterable,
            int energyCapacity, int energyTransferRate, int energyConsumption
    ) {
        return new Inserter(
                id, affectedByRedstone,
                grabDistance, ticksPerSwing, preferredItemCountPerAction,
                filterable, true,
                0, 0,
                energyCapacity, energyTransferRate, energyConsumption);
    }

    public Inserter(
            ResourceLocation id, boolean affectedByRedstone,
            int grabDistance, int ticksPerSwing, int preferredItemCountPerAction,
            boolean filterable, boolean useEnergy,
            int fuelCapacity, int fuelConsumption,
            int energyCapacity, int energyTransferRate, int energyConsumption
    ) {
        this.id = id;

        // Les deux dimensions sont indépendantes : un inserter filtrant à carburant est
        // une combinaison légitime, que l'ancien setUseEnergy interdisait (cf. BUG-014).
        this.filterable = filterable;
        this.useEnergy = useEnergy;

        this.affectedByRedstone = affectedByRedstone;

        this.grabDistance = atLeastOne(id, "grabDistance", grabDistance);
        this.ticksPerSwing = atLeastOne(id, "ticksPerSwing", ticksPerSwing);
        this.preferredItemCountPerAction = atLeastOne(id, "preferredItemCountPerAction", preferredItemCountPerAction);

        this.energyCapacity = useEnergy ? atLeastOne(id, "energyCapacity", energyCapacity) : UNUSED;
        this.energyTransferRate = useEnergy ? atLeastOne(id, "energyTransferRate", energyTransferRate) : UNUSED;
        this.energyConsumption = useEnergy ? atLeastOne(id, "energyConsumption", energyConsumption) : UNUSED;

        this.fuelCapacity = useEnergy ? UNUSED : atLeastOne(id, "fuelCapacity", fuelCapacity);
        this.fuelConsumption = useEnergy ? UNUSED : atLeastOne(id, "fuelConsumption", fuelConsumption);

        this.texture = new ResourceLocation(FactoryIO.MOD_ID, "block/inserters/" + getName());
    }

    /** Valeur des champs sans objet pour ce mode d'alimentation. */
    public static final int UNUSED = -1;

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

    public boolean isFilterable() {
        return filterable;
    }

    public boolean useEnergy() {
        return useEnergy;
    }

    public boolean isAffectedByRedstone() {
        return affectedByRedstone;
    }

    public int getEnergyCapacity() {
        return energyCapacity;
    }

    public int getEnergyTransferRate() {
        return energyTransferRate;
    }

    public int getEnergyConsumption() {
        return energyConsumption;
    }

    public int getFuelCapacity() {
        return fuelCapacity;
    }

    public int getFuelConsumption() {
        return fuelConsumption;
    }

    public int getGrabDistance() {
        return grabDistance;
    }

    /** Durée d'un mouvement de bras, en ticks. */
    public int getTicksPerSwing() {
        return ticksPerSwing;
    }

    /**
     * Nombre de ticks pour déplacer une main d'items.
     *
     * <p>Deux mouvements : aller chercher, puis livrer. C'est le cycle de Factorio, et
     * c'est déjà ce que fait la logique de transfert — une aspiration puis une éjection.
     * L'oublier fait annoncer le double du débit réel (cf. BUG-038).
     */
    public int getTicksPerItem() {
        return 2 * ticksPerSwing;
    }

    /** Débit théorique en items par seconde, à 20 ticks par seconde. */
    public double getItemsPerSecond() {
        return 20.0 * preferredItemCountPerAction / getTicksPerItem();
    }

    /** Taille de la main : nombre d'items déplacés par cycle. */
    public int getPreferredItemCountPerAction() {
        return preferredItemCountPerAction;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public void setTexture(ResourceLocation texture) {
        this.texture = texture;
    }

    public Translation getTranslation() {
        return translation;
    }

    // Interface (Références runtime)

    public Supplier<FactoryIOInserterEntityBlock> getBlock() {
        return this.blockSupplier;
    }

    public void setBlock(Supplier<FactoryIOInserterEntityBlock> blockSupplier) {
        this.blockSupplier = blockSupplier;
    }

    public Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> getBlockEntityType() {
        return this.blockEntityTypeSupplier;
    }

    public void setBlockEntityType(Supplier<BlockEntityType<FactoryIOInserterBlockEntity>> blockEntityTypeSupplier) {
        this.blockEntityTypeSupplier = blockEntityTypeSupplier;
    }

    public Supplier<FactoryIOInserterItem> getItem() {
        return this.itemSupplier;
    }

    public void setItem(Supplier<FactoryIOInserterItem> itemSupplier) {
        this.itemSupplier = itemSupplier;
    }

    public Supplier<MenuType<FactoryIOInserterContainer>> getMenuType() {
        return this.menuTypeSupplier;
    }

    public void setMenuType(Supplier<MenuType<FactoryIOInserterContainer>> menuTypeSupplier) {
        this.menuTypeSupplier = menuTypeSupplier;
    }

    // Inner work

    /**
     * Ramène une valeur à 1 minimum, en le signalant.
     *
     * <p>La coercition silencieuse de l'ancien code ({@code x > 0 ? x : 1}) masquait
     * les fautes de frappe dans les JSON utilisateur.
     */
    private static int atLeastOne(ResourceLocation id, String field, int value) {
        if (value >= 1) return value;

        FactoryIO.LOGGER.warn("{} : {} = {} est invalide, valeur ramenée à 1", id, field, value);
        return 1;
    }

    @Override
    public String toString() {
        return "Inserter{" +
                "id=" + id +
                ", filterable=" + filterable +
                ", useEnergy=" + useEnergy +
                ", affectedByRedstone=" + affectedByRedstone +
                ", energyCapacity=" + energyCapacity +
                ", energyTransferRate=" + energyTransferRate +
                ", energyConsumption=" + energyConsumption +
                ", fuelCapacity=" + fuelCapacity +
                ", fuelConsumption=" + fuelConsumption +
                ", grabDistance=" + grabDistance +
                ", ticksPerSwing=" + ticksPerSwing +
                ", preferredItemCountPerAction=" + preferredItemCountPerAction +
                ", texture=" + texture +
                '}';
    }
}
