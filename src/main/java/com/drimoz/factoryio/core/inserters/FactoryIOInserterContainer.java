package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.container.FactoryIOContainer;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterBuffer;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterFilter;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterFuel;
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

public class FactoryIOInserterContainer extends FactoryIOContainer {

    // Private properties

    private final int TE_INVENTORY_SLOT_COUNT;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private final InserterSlotLayout LAYOUT;

    private final FactoryIOInserterBlockEntity BLOCK_ENTITY;

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

    public FactoryIOInserterContainer(
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

    public FactoryIOInserterContainer(
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
            this.addSlot(new SlotInserterBuffer(handler, FactoryIOInserterBlockEntity.BUFFER_SLOT, 124, 45));

            if (LAYOUT.hasFuelSlot()) {
                this.addSlot(new SlotInserterFuel(this.BLOCK_ENTITY, handler, LAYOUT.fuel(), 80, 49));
            }

            // Les index viennent du layout : plus de « FILTER_SLOTS[i] - 1 » à corriger
            // à la main selon le type d'inserter (cf. DT-03).
            for (int i = 0; i < LAYOUT.filterCount(); i++) {
                this.addSlot(new SlotInserterFilter(handler, LAYOUT.filter(i), 8 + i * 18, 49));
            }
        });

        this.addDataSlots(this.powerData);
    }

    // Interface BlockEntity

    public FactoryIOInserterBlockEntity getBlockEntity() {
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

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // Only goes to inventory
            // Les bornes étaient inversées (36 -> 35) : la boucle de moveItemStackTo ne
            // s'exécutait jamais et le shift-clic depuis l'inventaire joueur ne faisait
            // rien du tout (cf. BUG-009).
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // Slot de filtre : c'est un item fantôme, shift-cliquer l'efface au lieu de
            // le déplacer.
            if (isFilterMenuSlot(index)) {
                sourceSlot.set(ItemStack.EMPTY);
                return copyOfSourceStack;
            }

            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        } else {
            FactoryIO.LOGGER.warn("Index de slot invalide : {}", index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    /**
     * Comportement « fantôme » des slots de filtre : cliquer y dépose une copie d'un
     * seul item sans consommer ce que le joueur tient, et cliquer à vide efface.
     */
    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        boolean ghostClick = isFilterMenuSlot(pSlotId)
                && (pClickType == ClickType.PICKUP || pClickType == ClickType.QUICK_MOVE);

        if (ghostClick && !slots.get(pSlotId).getItem().isEmpty()) {
            if (this.getCarried().isEmpty()) {
                slots.get(pSlotId).set(ItemStack.EMPTY);
            } else {
                ItemStack ghost = this.getCarried().copy();
                ghost.setCount(1);
                slots.get(pSlotId).set(ghost);
            }
            return;
        }

        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

    // Inner work (Slots)

    /** @param menuSlotId index dans le menu, pas dans l'inventaire de la machine */
    private boolean isFilterMenuSlot(int menuSlotId) {
        if (menuSlotId < TE_INVENTORY_FIRST_SLOT_INDEX) return false;
        if (menuSlotId >= TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) return false;

        return LAYOUT.isFilter(menuSlotId - TE_INVENTORY_FIRST_SLOT_INDEX);
    }
}
