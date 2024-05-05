package com.drimoz.factoryio.core.containers.inserters;

import com.drimoz.factoryio.core.blockentities.inserters.FactoryIOInserterBlockEntity;
import com.drimoz.factoryio.core.containers.FactoryIOContainer;
import com.drimoz.factoryio.core.containers.slots.SlotInserterBuffer;
import com.drimoz.factoryio.core.containers.slots.SlotInserterFilter;
import com.drimoz.factoryio.core.containers.slots.SlotInserterFuel;
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

public abstract class FactoryIOInserterContainer extends FactoryIOContainer {
    private final int TE_INVENTORY_SLOT_COUNT;

    private final FactoryIOInserterBlockEntity blockEntity;
    private final Level level;

    protected Player playerEntity;

    protected FactoryIOInserterContainer(@Nullable MenuType<?> pMenuType, int pContainerId, Level pLevel, BlockPos pPos, Inventory pPlayerInv, Player pPlayer) {
        super(pMenuType, pContainerId);


        this.blockEntity = (FactoryIOInserterBlockEntity) pLevel.getBlockEntity(pPos);
        this.level = pLevel;
        this.playerEntity = pPlayer;
        this.TE_INVENTORY_SLOT_COUNT = 1 + (blockEntity.IS_ENERGY ? 0 : 1) + (blockEntity.IS_FILTER ? FactoryIOInserterBlockEntity.FILTER_SLOTS.length : 0);

        checkContainerSize(pPlayerInv, this.TE_INVENTORY_SLOT_COUNT);
        addPlayerInventory(pPlayerInv);
        addPlayerHotbar(pPlayerInv);

        this.blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(handler -> {
            this.addSlot(new SlotInserterBuffer(handler, FactoryIOInserterBlockEntity.BUFFER_SLOT, 124, 45));

            if (!blockEntity.IS_ENERGY) {
                this.addSlot(new SlotInserterFuel(this.blockEntity, handler, FactoryIOInserterBlockEntity.FUEL_SLOT, 80, 49));
            }

            if (blockEntity.IS_FILTER) {
                this.addSlot(new SlotInserterFilter(handler, blockEntity.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[0] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[0], 8, 49));
                this.addSlot(new SlotInserterFilter(handler, blockEntity.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[1] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[1], 26, 49));
                this.addSlot(new SlotInserterFilter(handler, blockEntity.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[2] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[2], 44, 49));
                this.addSlot(new SlotInserterFilter(handler, blockEntity.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[3] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[3], 62, 49));
                this.addSlot(new SlotInserterFilter(handler, blockEntity.IS_ENERGY ? FactoryIOInserterBlockEntity.FILTER_SLOTS[4] - 1 : FactoryIOInserterBlockEntity.FILTER_SLOTS[4], 80, 49));
            }
        });
    }

    public FactoryIOInserterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public boolean stillValid(Player player) {
        return this.blockEntity.stillValid(player);
    }

    public int getEnergyScaled(int pixels) {
        if (this.blockEntity.IS_ENERGY) {
            int i = this.blockEntity.getCurrentEnergy();
            int j = this.blockEntity.getEnergyCapacity();
            return j != 0 && i != 0 ? i * pixels / j : 0;
        }
        return -1;
    }

    public int getFuelScaled(int pixels) {
        if (!this.blockEntity.IS_ENERGY) {
            int i = this.blockEntity.getCurrentFuelValue();
            int j = this.blockEntity.getFuelCapacity();
            return j != 0 && i != 0 ? i * pixels / j : 0;
        }
        return -1;
    }

    public boolean hasEnergy() {
        if (this.blockEntity.IS_ENERGY && this.blockEntity.getCurrentEnergy() > 0) return true;
        return false;
    }

    public boolean hasFuel() {
        if (!this.blockEntity.IS_ENERGY && this.blockEntity.getCurrentFuelValue() > 0) return true;
        return false;
    }

    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int FILTER_SLOT_COUNT = 5;

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
            if ((blockEntity.IS_FILTER || blockEntity.IS_ENERGY) && TE_INVENTORY_SLOT_COUNT - FILTER_SLOT_COUNT > 0) {
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

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
