package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.container.BaseMenu;
import com.drimoz.factoryio.core.generic.container.slots.GhostSlot;
import com.drimoz.factoryio.core.generic.container.slots.OutputSlot;
import com.drimoz.factoryio.core.generic.container.slots.InserterFuelSlot;
import com.drimoz.factoryio.core.generic.container.slots.UpgradeSlot;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

public class InserterContainer extends BaseMenu {

    // Private properties

    private final int TE_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private final InserterSlotLayout LAYOUT;

    private final InserterGuiLayout GUI;

    private final InserterBlockEntity BLOCK_ENTITY;

    private final Inserter inserter;

    /**
     * Réserve courante, découpée en deux mots de 16 bits.
     *
     * <p>{@code ClientboundContainerSetDataPacket} ne transporte qu'un {@code short} :
     * une capacité configurée au-delà de 32 767 serait tronquée si on envoyait la valeur
     * telle quelle. Le découpage rend la synchronisation correcte quelle que soit la
     * valeur définie dans le JSON de l'inserter.
     */
    private final int[] syncedPower = new int[2];

    private final ContainerData powerData = new ContainerData() {
        @Override
        public int get(int index) {
            if (!isServerSide()) return syncedPower[index];

            int value = currentPowerOnServer();
            return index == 0 ? value & 0xFFFF : (value >>> 16) & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
            syncedPower[index] = value;
        }

        @Override
        public int getCount() {
            return syncedPower.length;
        }
    };

    /**
     * Compteur d'items livrés, tronqué à 16 bits (FIO-170).
     *
     * <p>Seules les différences entre deux lectures servent — l'écran en tire un débit — et
     * elles restent justes modulo 65 536 : aucun inserter n'en livre autant en une seconde.
     * Comme la réserve, la valeur ne part que si elle change, et seulement vers qui regarde.
     */
    private int syncedDelivered;

    private final ContainerData statsData = new ContainerData() {
        @Override
        public int get(int index) {
            if (!isServerSide()) return syncedDelivered;

            return BLOCK_ENTITY.getItemsDelivered() & 0xFFFF;
        }

        @Override
        public void set(int index, int value) {
            syncedDelivered = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    // Life cycle

    public InserterContainer(
            int pContainerId,
            Inserter inserter,
            Inventory pPlayerInv,
            Level pLevel,
            BlockPos pPos
    ) {
        this(
                inserter.getMenuType().get(),
                pContainerId,
                inserter,
                pPlayerInv,
                pLevel,
                pPos
        );
    }

    public InserterContainer(
            @Nullable MenuType<?> pMenuType,
            int pContainerId,
            Inserter inserterData,
            Inventory pPlayerInv,
            Level pLevel,
            BlockPos pPos
    ) {
        super(pMenuType, pContainerId);
        inserter = inserterData;

        // getBlockEntity renvoie null si le bloc a disparu entre l'ouverture demandée et
        // la construction du menu (cf. BUG-020). Le cas est rare — le serveur n'ouvre le
        // menu que depuis un block entity existant, et le client a forcément le chunk —
        // mais il n'est pas impossible, et une exception levée ici remonterait dans le
        // pipeline réseau du client : elle le déconnecterait au lieu de fermer un écran.
        //
        // Le menu se construit donc quand même, avec le seul inventaire du joueur, et
        // stillValid le fait fermer au tick suivant.
        this.BLOCK_ENTITY = inserterData.getBlockEntityType().get().getBlockEntity(pLevel, pPos);

        this.LAYOUT = BLOCK_ENTITY != null ? BLOCK_ENTITY.LAYOUT : InserterSlotLayout.of(inserterData);
        this.TE_INVENTORY_SLOT_COUNT = LAYOUT.size();
        this.GUI = InserterGuiLayout.of(inserterData);

        // checkContainerSize validait l'inventaire du JOUEUR contre le nombre de slots de
        // la machine : l'assertion passait toujours et ne testait rien (cf. BUG-034).

        addPlayerInventory(pPlayerInv, GUI.inventoryY());
        addPlayerHotbar(pPlayerInv, GUI.hotbarY());

        if (this.BLOCK_ENTITY == null) {
            FactoryIO.LOGGER.warn("Aucun inserter en {} : le menu s'ouvre vide et se referme", pPos);
            return;
        }

        // Le stockage réel, et non la capability : celle-ci est le contrat offert aux
        // voisins — dépôt de carburant, reprise des résidus — et n'a jamais eu vocation à
        // décrire ce qu'un joueur a le droit de faire dans l'écran. Passer par elle rendait
        // les slots d'amélioration inutilisables dans les deux sens.
        IItemHandler handler = this.BLOCK_ENTITY.getMenuItems();

        // Index : InserterSlotLayout. Positions : InserterGuiLayout. Plus une coordonnée
        // écrite ici (cf. DT-03, FIO-071).
        this.addSlot(new OutputSlot(handler, InserterBlockEntity.BUFFER_SLOT, GUI.handSlotX(), GUI.handSlotY()));

        if (LAYOUT.hasFuelSlot()) {
            this.addSlot(new InserterFuelSlot(this.BLOCK_ENTITY, handler, LAYOUT.fuel(), GUI.fuelSlotX(), GUI.fuelSlotY()));
        }

        for (int i = 0; i < LAYOUT.filterCount(); i++) {
            this.addSlot(new InserterFilterSlot(this.BLOCK_ENTITY, handler, i, GUI.filterSlotX(i), GUI.filterSlotY()));
        }

        // Dans l'onglet des améliorations, hors de la fenêtre. Ils existent toujours — le
        // shift-clic balaie [premier slot machine, premier + LAYOUT.size()[ — et l'écran ne
        // fait que les montrer ou les cacher avec l'onglet.
        for (int i = 0; i < LAYOUT.upgradeCount(); i++) {
            this.addSlot(new UpgradeSlot(handler, LAYOUT.upgrade(i),
                    InserterGuiLayout.augmentSlotX(i), InserterGuiLayout.augmentSlotY(),
                    stack -> InserterUpgradeType.of(stack) != null));
        }

        this.addDataSlots(this.powerData);
        this.addDataSlots(this.statsData);
    }

    /** Compteur d'items livrés, modulo 65 536 : n'en lire que les différences. */
    public int getItemsDelivered() {
        if (isServerSide()) return BLOCK_ENTITY.getItemsDelivered() & 0xFFFF;

        return this.syncedDelivered & 0xFFFF;
    }

    // Interface BlockEntity

    @Nullable
    public InserterBlockEntity getBlockEntity() {
        return BLOCK_ENTITY;
    }

    /** Géométrie de l'écran, identique des deux côtés. */
    public InserterGuiLayout getGuiLayout() {
        return GUI;
    }

    /** Définition du type d'inserter, connue des deux côtés. */
    public Inserter getInserter() {
        return inserter;
    }

    /** @return {@code true} si le menu est adossé à un inserter bien réel */
    public boolean isBacked() {
        return BLOCK_ENTITY != null;
    }

    @Override
    public boolean stillValid(Player player) {
        return BLOCK_ENTITY != null && BLOCK_ENTITY.stillValid(player);
    }

    /**
     * Réserve courante : énergie en FE, ou ticks de combustion restants.
     *
     * <p>Côté serveur la valeur est lue directement sur le block entity ; côté client
     * elle vient du {@code ContainerData}, synchronisé automatiquement par le menu et
     * uniquement vers les joueurs qui ont l'écran ouvert.
     */
    public int getPowerStored() {
        if (isServerSide()) return currentPowerOnServer();

        return (syncedPower[0] & 0xFFFF) | ((syncedPower[1] & 0xFFFF) << 16);
    }

    // Interface (Traits, lus sur la définition)
    //
    // L'écran interrogeait le block entity pour des valeurs qui n'en dépendent pas : le
    // mode d'alimentation, la présence de filtres et la sensibilité au redstone sont des
    // traits du *type* d'inserter. Les lire ici évite d'avoir à disposer d'un block entity
    // pour dessiner, et rend l'écran insensible au cas où le bloc vient de disparaître.

    public boolean usesEnergy() {
        return inserter.useEnergy();
    }

    public boolean isFilterable() {
        return inserter.isFilterable();
    }

    public boolean isAffectedByRedstone() {
        return inserter.isAffectedByRedstone();
    }

    /** Capacité maximale, connue des deux côtés : elle vient de la définition. */
    public int getPowerCapacity() {
        return usesEnergy() ? inserter.getEnergyCapacity() : inserter.getFuelCapacity();
    }

    public int getEnergyScaled(int pixels) {
        if (!usesEnergy()) return -1;

        return scaled(pixels);
    }

    public int getFuelScaled(int pixels) {
        if (usesEnergy()) return -1;

        return scaled(pixels);
    }

    public boolean hasEnergy() {
        return usesEnergy() && getPowerStored() > 0;
    }

    public boolean hasFuel() {
        return !usesEnergy() && getPowerStored() > 0;
    }

    // Inner work (Synchronisation)

    private boolean isServerSide() {
        return BLOCK_ENTITY != null
                && BLOCK_ENTITY.getLevel() != null
                && !BLOCK_ENTITY.getLevel().isClientSide;
    }

    private int currentPowerOnServer() {
        return usesEnergy() ? BLOCK_ENTITY.getCurrentEnergy() : BLOCK_ENTITY.getCurrentFuelValue();
    }

    private int scaled(int pixels) {
        int capacity = getPowerCapacity();
        if (capacity <= 0) return 0;

        return Math.min(pixels, getPowerStored() * pixels / capacity);
    }


    // Interface (Inventory Interaction)

    /**
     * Shift-clic, écrit une fois selon le patron vanilla (cf. DT-08).
     *
     * <p>Trois gardes que la version précédente n'avait pas : un slot fantôme s'efface au
     * lieu de se déplacer, un slot qui refuse d'être vidé est respecté ([BUG-036](../../../../../../../../docs/03-BUGS.md)),
     * et le transfert vers l'inventaire du joueur remplit à l'envers, comme partout dans
     * vanilla.
     *
     * <p>Le contrat de retour est subtil et vaut d'être rappelé : {@code doClick} rappelle
     * cette méthode tant qu'elle renvoie une pile non vide. Renvoyer la copie alors que
     * rien n'a bougé boucle donc à l'infini — d'où le retour vide dès que
     * {@code moveItemStackTo} échoue.
     */
    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (!sourceSlot.hasItem()) return ItemStack.EMPTY;

        // Un filtre ne contient pas d'item mais sa description : le shift-clic l'efface.
        if (sourceSlot instanceof GhostSlot ghost) {
            ghost.clearGhost();
            return ItemStack.EMPTY;
        }

        // Le buffer de transport interdit qu'on lui prenne son item ; le shift-clic
        // contournait cette garde (cf. BUG-036).
        if (!sourceSlot.mayPickup(playerIn)) return ItemStack.EMPTY;

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < TE_INVENTORY_FIRST_SLOT_INDEX) {
            // Inventaire du joueur vers la machine. Les bornes étaient inversées
            // (36 -> 35) : la boucle ne s'exécutait jamais (cf. BUG-009).
            if (!moveItemStackTo(sourceStack,
                    TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(sourceStack,
                    VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, true)) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    /**
     * Passe la main aux slots fantômes, qui décident seuls de ce qu'un clic veut dire.
     *
     * <p>Le menu ne connaît plus ni les filtres ni les tags : il route, et
     * {@link GhostSlot} tranche. C'est ce qui rend le mécanisme réutilisable pour les
     * filtres de séparateur de la Phase 3.
     *
     * <p>Cette surcharge ne peut pas disparaître, contrairement à ce que visait DT-08 :
     * vanilla court-circuite sur {@code mayPickup} avant d'appeler la moindre méthode du
     * slot, et ne lui transmet jamais le numéro du bouton. Le détail est dans
     * {@link GhostSlot}.
     */
    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        boolean clickable = pClickType == ClickType.PICKUP || pClickType == ClickType.QUICK_MOVE;

        if (clickable && pSlotId >= 0 && pSlotId < slots.size()
                && slots.get(pSlotId) instanceof GhostSlot ghost
                && ghost.onGhostClick(pButton, this.getCarried())) {
            return;
        }

        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

}
