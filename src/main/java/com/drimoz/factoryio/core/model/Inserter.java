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
 * <p>Les paramètres de gameplay sont validés à la construction et regroupés dans un
 * {@link InserterTuning} remplacé d'un bloc, jamais champ par champ. L'ancienne version
 * exposait quatorze setters publics dont l'ordre d'appel comptait — {@code setUseEnergy}
 * devait précéder {@code setEnergyCapacity}, faute de quoi la capacité était
 * silencieusement remise à -1 (cf. DT-04). C'était l'ordre d'appel le problème, pas la
 * mutabilité : un datapack doit pouvoir régler la vitesse à chaud (FIO-037).
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

    /**
     * Traits structurels, figés à l'enregistrement.
     *
     * <p>Ils décident du plan d'inventaire, du type de block entity, du menu et de la
     * géométrie : les changer supposerait de reconstruire blocs et items, donc
     * d'invalider ceux déjà posés. Un datapack ne peut pas y toucher (cf. FIO-037).
     */
    private final boolean filterable;
    private final boolean useEnergy;

    /**
     * Réglages, remplaçables à chaud par un datapack.
     *
     * <p>Volontairement non final : c'est le seul état mutable qui subsiste, et il est
     * remplacé d'un bloc plutôt que champ par champ — l'écueil que DT-04 reprochait aux
     * quatorze setters d'origine était l'ordre d'appel, pas la mutabilité elle-même.
     */
    private InserterTuning tuning;

    /** Réglages d'origine, pour revenir en arrière quand un datapack disparaît. */
    private final InserterTuning defaultTuning;

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

        this.defaultTuning = new InserterTuning(
                affectedByRedstone,
                atLeastOne(id, "grabDistance", grabDistance),
                atLeastOne(id, "ticksPerSwing", ticksPerSwing),
                atLeastOne(id, "preferredItemCountPerAction", preferredItemCountPerAction),
                useEnergy ? atLeastOne(id, "energyCapacity", energyCapacity) : UNUSED,
                useEnergy ? atLeastOne(id, "energyTransferRate", energyTransferRate) : UNUSED,
                useEnergy ? atLeastOne(id, "energyConsumption", energyConsumption) : UNUSED,
                useEnergy ? UNUSED : atLeastOne(id, "fuelCapacity", fuelCapacity),
                useEnergy ? UNUSED : atLeastOne(id, "fuelConsumption", fuelConsumption));

        this.tuning = this.defaultTuning;

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
        return tuning.affectedByRedstone();
    }

    public int getEnergyCapacity() {
        return tuning.energyCapacity();
    }

    public int getEnergyTransferRate() {
        return tuning.energyTransferRate();
    }

    public int getEnergyConsumption() {
        return tuning.energyConsumption();
    }

    public int getFuelCapacity() {
        return tuning.fuelCapacity();
    }

    public int getFuelConsumption() {
        return tuning.fuelConsumption();
    }

    public int getGrabDistance() {
        return tuning.grabDistance();
    }

    /** Durée d'un mouvement de bras, en ticks. */
    public int getTicksPerSwing() {
        return tuning.ticksPerSwing();
    }

    /**
     * Nombre de ticks pour déplacer une main d'items.
     *
     * <p>Deux mouvements : aller chercher, puis livrer. C'est le cycle de Factorio, et
     * c'est déjà ce que fait la logique de transfert — une aspiration puis une éjection.
     * L'oublier fait annoncer le double du débit réel (cf. BUG-038).
     */
    public int getTicksPerItem() {
        return 2 * getTicksPerSwing();
    }

    /** Débit théorique en items par seconde, à 20 ticks par seconde. */
    public double getItemsPerSecond() {
        return 20.0 * getPreferredItemCountPerAction() / getTicksPerItem();
    }

    /** Taille de la main : nombre d'items déplacés par cycle. */
    public int getPreferredItemCountPerAction() {
        return tuning.handSize();
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

    // Interface (Réglages rechargeables)

    /** Réglages courants. */
    public InserterTuning getTuning() {
        return this.tuning;
    }

    /** Réglages tels que définis au chargement, avant tout datapack. */
    public InserterTuning getDefaultTuning() {
        return this.defaultTuning;
    }

    /** Remplace les réglages — un datapack vient d'être appliqué (cf. FIO-037). */
    public void applyTuning(InserterTuning tuning) {
        this.tuning = tuning;
    }

    /** Revient aux réglages d'origine : plus aucun datapack ne surcharge cet inserter. */
    public void resetTuning() {
        this.tuning = this.defaultTuning;
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
                ", affectedByRedstone=" + tuning.affectedByRedstone() +
                ", energyCapacity=" + tuning.energyCapacity() +
                ", energyTransferRate=" + tuning.energyTransferRate() +
                ", energyConsumption=" + tuning.energyConsumption() +
                ", fuelCapacity=" + tuning.fuelCapacity() +
                ", fuelConsumption=" + tuning.fuelConsumption() +
                ", grabDistance=" + tuning.grabDistance() +
                ", ticksPerSwing=" + getTicksPerSwing() +
                ", preferredItemCountPerAction=" + tuning.handSize() +
                ", texture=" + texture +
                '}';
    }
}
