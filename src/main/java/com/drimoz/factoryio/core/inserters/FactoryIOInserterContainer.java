package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.core.generic.container.FactoryIOContainer;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterBuffer;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterFilter;
import com.drimoz.factoryio.core.generic.container.slots.SlotInserterFuel;
import com.drimoz.factoryio.core.model.Inserter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.CapabilityItemHandler;
import org.jetbrains.annotations.Nullable;

public class FactoryIOInserterContainer extends FactoryIOContainer {

    // Private properties

    private final int TE_INVENTORY_SLOT_COUNT;
    private static final int FILTER_SLOT_COUNT = 5;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    private final FactoryIOInserterBlockEntity BLOCK_ENTITY;

    private final Inserter inserter;

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

        this.BLOCK_ENTITY = inserterData.getBlockEntityType().get().getBlockEntity(pLevel, pPos);
        this.TE_INVENTORY_SLOT_COUNT = 1 + (BLOCK_ENTITY.IS_ENERGY ? 0 : 1) + (BLOCK_ENTITY.IS_FILTER ? FactoryIOInserterBlockEntity.FILTER_SLOTS.length : 0);

        checkContainerSize(pPlayerInv, this.TE_INVENTORY_SLOT_COUNT);

        addPlayerInventory(pPlayerInv);
        addPlayerHotbar(pPlayerInv);

        this.BLOCK_ENTITY.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
            this.addSlot(new SlotInserterBuffer(handler, FactoryIOInserterBlockEntity.BUFFER_SLOT, 124, 45));

            if (!BLOCK_ENTITY.IS_ENERGY) {
                this.addSlot(new SlotInserterFuel(this.BLOCK_ENTITY, handler, FactoryIOInserterBlockEntity.FUEL_SLOT, 80, 49));
            }

            if (BLOCK_ENTITY.IS_FILTER) {
                this.addSlot(new SlotInserterFilter(handler, BLOCK_ENTITY.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[0] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[0], 8, 49));
                this.addSlot(new SlotInserterFilter(handler, BLOCK_ENTITY.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[1] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[1], 26, 49));
                this.addSlot(new SlotInserterFilter(handler, BLOCK_ENTITY.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[2] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[2], 44, 49));
                this.addSlot(new SlotInserterFilter(handler, BLOCK_ENTITY.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[3] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[3], 62, 49));
                this.addSlot(new SlotInserterFilter(handler, BLOCK_ENTITY.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[4] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[4], 80, 49));
            }
        });
    }

    // Interface BlockEntity

    public FactoryIOInserterBlockEntity getBlockEntity() {
        return BLOCK_ENTITY;
    }

    public boolean stillValid(Player player) {
        return this.BLOCK_ENTITY.stillValid(player);
    }

    public int getEnergyScaled(int pixels) {
        if (this.BLOCK_ENTITY.IS_ENERGY) {
            int i = this.BLOCK_ENTITY.getCurrentEnergy();
            int j = this.BLOCK_ENTITY.getEnergyCapacity();
            return j != 0 && i != 0 ? i * pixels / j : 0;
        }
        return -1;
    }

    public int getFuelScaled(int pixels) {
        if (!this.BLOCK_ENTITY.IS_ENERGY) {
            int i = this.BLOCK_ENTITY.getCurrentFuelValue();
            int j = this.BLOCK_ENTITY.getFuelCapacity();
            return j != 0 && i != 0 ? i * pixels / j : 0;
        }
        return -1;
    }

    public boolean hasEnergy() {
        if (this.BLOCK_ENTITY.IS_ENERGY && this.BLOCK_ENTITY.getCurrentEnergy() > 0) return true;
        return false;
    }

    public boolean hasFuel() {
        if (!this.BLOCK_ENTITY.IS_ENERGY && this.BLOCK_ENTITY.getCurrentFuelValue() > 0) return true;
        return false;
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
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX - 1, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            //This is a TE slot so merge the stack into the players inventory
            if ((BLOCK_ENTITY.IS_FILTER || BLOCK_ENTITY.IS_ENERGY) && TE_INVENTORY_SLOT_COUNT - FILTER_SLOT_COUNT > 0) {
                if (index > VANILLA_SLOT_COUNT + TE_INVENTORY_SLOT_COUNT - FILTER_SLOT_COUNT - 1) {
                    sourceSlot.set(ItemStack.EMPTY);
                    return copyOfSourceStack;
                }
            }

            else if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        } else {
            System.out.println("Invalid slotIndex:" + index);
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

    //@Override
    //public ItemStack quickMoveStack2(Player playerIn, int index) {
    //    Slot sourceSlot = slots.get(index);
    //    if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
    //    ItemStack sourceStack = sourceSlot.getItem();
    //    ItemStack copyOfSourceStack = sourceStack.copy();
//
    //    if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_EXTRACTABLE_INVENTORY_SLOT_COUNT) {
    //        if (!moveItemStackTo(sourceStack,VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,false))
    //            return ItemStack.EMPTY;
    //    } else if (index >= VANILLA_FIRST_SLOT_INDEX && index < VANILLA_FIRST_SLOT_INDEX+VANILLA_SLOT_COUNT) {
    //        if (!moveItemStackTo(sourceStack,TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX + TE_INSERTABLE_INVENTORY_SLOT_COUNT,false))
    //            return ItemStack.EMPTY;
    //    } else {
    //        System.out.println("Invalid slotIndex:" + index);
    //        return ItemStack.EMPTY;
    //    }
//
//
    //    // If stack size == 0 (the entire stack was moved) set slot contents to null
    //    if (sourceStack.getCount() == 0) {
    //        sourceSlot.set(ItemStack.EMPTY);
    //    } else {
    //        sourceSlot.setChanged();
    //    }
    //    sourceSlot.onTake(playerIn, sourceStack);
    //    return copyOfSourceStack;
    //}

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        if (TE_INVENTORY_SLOT_COUNT - FILTER_SLOT_COUNT > 0) {
            if (pSlotId > VANILLA_SLOT_COUNT + TE_INVENTORY_SLOT_COUNT - FILTER_SLOT_COUNT - 1) {
                if (pClickType == ClickType.PICKUP || pClickType == ClickType.QUICK_MOVE) {
                    if (!slots.get(pSlotId).getItem().isEmpty()) {
                        if (this.getCarried().isEmpty()) {
                            slots.get(pSlotId).set(ItemStack.EMPTY);
                        }
                        else {
                            ItemStack ts = this.getCarried().copy();
                            ts.setCount(1);
                            slots.get(pSlotId).set(ts);
                        }
                        return;
                    }
                }
            }
        }
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }
}
