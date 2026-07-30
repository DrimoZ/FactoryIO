package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.container.BaseMenu;
import com.drimoz.factoryio.core.generic.container.slots.GhostSlot;
import com.drimoz.factoryio.core.generic.container.slots.InserterBufferSlot;
import com.drimoz.factoryio.core.generic.container.slots.InserterFuelSlot;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

public class InserterContainer extends BaseMenu {

    // Private properties

    private final int TE_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private final InserterSlotLayout LAYOUT;

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
        // la construction du menu — fréquent côté client, où le menu naît d'un paquet
        // réseau (cf. BUG-020).
        this.BLOCK_ENTITY = inserterData.getBlockEntityType().get().getBlockEntity(pLevel, pPos);
        if (this.BLOCK_ENTITY == null) {
            throw new IllegalStateException("Aucun inserter en " + pPos + " pour ouvrir le menu");
        }

        this.LAYOUT = BLOCK_ENTITY.LAYOUT;
        this.TE_INVENTORY_SLOT_COUNT = LAYOUT.size();

        // checkContainerSize validait l'inventaire du JOUEUR contre le nombre de slots de
        // la machine : l'assertion passait toujours et ne testait rien (cf. BUG-034).

        addPlayerInventory(pPlayerInv);
        addPlayerHotbar(pPlayerInv);

        this.BLOCK_ENTITY.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            this.addSlot(new InserterBufferSlot(handler, InserterBlockEntity.BUFFER_SLOT, 124, 45));

            if (LAYOUT.hasFuelSlot()) {
                this.addSlot(new InserterFuelSlot(this.BLOCK_ENTITY, handler, LAYOUT.fuel(), 80, 49));
            }

            // Les index viennent du layout : plus de « FILTER_SLOTS[i] - 1 » à corriger
            // à la main selon le type d'inserter (cf. DT-03).
            for (int i = 0; i < LAYOUT.filterCount(); i++) {
                this.addSlot(new InserterFilterSlot(this.BLOCK_ENTITY, handler, i, 8 + i * 18, 49));
            }
        });

        this.addDataSlots(this.powerData);
    }

    // Interface BlockEntity

    public InserterBlockEntity getBlockEntity() {
        return BLOCK_ENTITY;
    }

    public boolean stillValid(Player player) {
        return this.BLOCK_ENTITY.stillValid(player);
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

    /** Capacité maximale, connue des deux côtés : elle vient de la définition. */
    public int getPowerCapacity() {
        return BLOCK_ENTITY.IS_ENERGY ? inserter.getEnergyCapacity() : inserter.getFuelCapacity();
    }

    public int getEnergyScaled(int pixels) {
        if (!this.BLOCK_ENTITY.IS_ENERGY) return -1;

        return scaled(pixels);
    }

    public int getFuelScaled(int pixels) {
        if (this.BLOCK_ENTITY.IS_ENERGY) return -1;

        return scaled(pixels);
    }

    public boolean hasEnergy() {
        return this.BLOCK_ENTITY.IS_ENERGY && getPowerStored() > 0;
    }

    public boolean hasFuel() {
        return !this.BLOCK_ENTITY.IS_ENERGY && getPowerStored() > 0;
    }

    // Inner work (Synchronisation)

    private boolean isServerSide() {
        return BLOCK_ENTITY.getLevel() != null && !BLOCK_ENTITY.getLevel().isClientSide;
    }

    private int currentPowerOnServer() {
        return BLOCK_ENTITY.IS_ENERGY ? BLOCK_ENTITY.getCurrentEnergy() : BLOCK_ENTITY.getCurrentFuelValue();
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
