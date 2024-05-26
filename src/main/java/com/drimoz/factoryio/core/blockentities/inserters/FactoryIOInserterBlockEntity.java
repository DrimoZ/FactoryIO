package com.drimoz.factoryio.core.blockentities.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.blockentities.FactoryIOMenuProvidedBlockEntity;
import com.drimoz.factoryio.core.containers.energy.FactoryIOEnergyContainer;
import com.drimoz.factoryio.core.containers.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CEnergy;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CFuel;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CWhitelistButton;
import com.drimoz.factoryio.core.registery.custom.FactoryIONetworks;
import com.drimoz.factoryio.core.registery.models.InserterData;
import com.drimoz.factoryio.core.tags.FactoryIOTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FactoryIOInserterBlockEntity extends FactoryIOMenuProvidedBlockEntity implements IAnimatable {

    // Public constants

    public static final int BUFFER_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int[] FILTER_SLOTS = new int[]{2, 3, 4, 5, 6};

    public final int INVENTORY_SIZE;
    public final boolean IS_ENERGY;
    public final boolean IS_FILTER;

    // Duration : 0 = 10a / tick || 10 = 1a / tick || 200 = 1a / 20tick (1sec) ||
    public static final int MAX_ACTIONS_PER_TICK = 10;

    // Private constants

    private final MenuType<FactoryIOInserterContainer> menuType;
    private final InserterData inserterData;

    // Protected properties

    protected FactoryIOEnergyContainer energyStorage;
    protected LazyOptional<IEnergyStorage> lazyEnergy;

    protected ItemStackHandler itemStorage;
    protected LazyOptional<IItemHandler> lazyItem;

    // private properties

    private int current_cooldown = 0;
    private boolean isWhitelist = true;
    private int current_fuel_value = 0;

    private AnimationFactory factory = new AnimationFactory(this);

    // Life cycle

    public FactoryIOInserterBlockEntity(BlockPos blockPos, BlockState blockState, InserterData inserterData) {
        this(
                (MenuType)inserterData.registries().getMenu().get(),
                (BlockEntityType)inserterData.registries().getBlockEntity().get(),
                blockPos,
                blockState,
                inserterData);
    }

    protected FactoryIOInserterBlockEntity(
            MenuType<FactoryIOInserterContainer> menuType,
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState,
            InserterData inserterData
    ) {
        super(blockEntityType, blockPos, blockState);

        this.menuType = menuType;
        this.inserterData = inserterData;

        this.IS_ENERGY = inserterData.useEnergy;
        this.IS_FILTER = inserterData.isFilter;

        if (IS_ENERGY) {
            this.energyStorage = new FactoryIOEnergyContainer(inserterData.energyMaxCapacity, inserterData.energyMaxTransferRate) {
                @Override
                protected void onEnergyChanged() {
                    FactoryIOInserterBlockEntity.this.setChanged();
                }
            };

            this.lazyEnergy = LazyOptional.of(() -> this.energyStorage);
        }

        this.INVENTORY_SIZE = 1 + (IS_ENERGY ? 0 : 1) + (IS_FILTER ? FILTER_SLOTS.length : 0);

        this.itemStorage = new ItemStackHandler(INVENTORY_SIZE) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (IS_ENERGY || slot != FUEL_SLOT) return stack;
                if (ForgeHooks.getBurnTime(stack, null) <= 0) return stack;
                if (!stack.is(FactoryIOTags.Items.INSERTER_FUEL)) return stack;

                return super.insertItem(slot, stack, simulate);
            }

            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (IS_ENERGY || slot != FUEL_SLOT) return ItemStack.EMPTY;

                return super.extractItem(slot, amount, simulate);
            }

        };
        this.lazyItem = LazyOptional.of(() -> this.itemStorage);
    }

    // Interface (DataFromData)

    public int getMaximumItemCountPerAction(){
        return inserterData.preferredItemCountPerAction;
    }

    public int getGrabDistance(){
        return inserterData.grabDistance;
    }

    public int getDurationBetweenActions(){
        return inserterData.timeBetweenActions;
    }

    public int getFuelCapacity(){
        return IS_ENERGY ? inserterData.energyMaxCapacity : inserterData.fuelMaxCapacity;
    }

    public int getFuelConsumptionPerAction() {
        return IS_ENERGY ? inserterData.energyConsumptionPerAction : inserterData.fuelConsumptionPerAction;
    }

    public int getPreferredFuelItemBufferCount() {
        return -1;
    }

    // Interface (Name)

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent(getBlockState().getBlock().getDescriptionId());
    }

    // Interface (ItemStorage)

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemStorage.getSlots());
        for (int i = 0; i < (this.IS_FILTER ? itemStorage.getSlots() - 5: itemStorage.getSlots()); i++) {
            inventory.setItem(i, itemStorage.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public boolean isEmpty() {
        for(int i = 0; i < this.itemStorage.getSlots(); i++) {
            if (!this.itemStorage.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    public void clearContent() {
        for(int i = 0; i < this.itemStorage.getSlots(); i++) {
            this.itemStorage.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // Interface (Capabilities)

    @Nonnull @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return lazyItem.cast();
        if (cap == CapabilityEnergy.ENERGY && IS_ENERGY) return lazyEnergy.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        this.lazyItem = LazyOptional.of(() -> itemStorage);
        if(IS_ENERGY) {
            this.lazyEnergy = LazyOptional.of(() -> energyStorage);
        }
    }

    @Override
    public void invalidateCaps()  {
        super.invalidateCaps();

        this.lazyItem.invalidate();
        if (IS_ENERGY) {
            this.lazyEnergy.invalidate();
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        tag.put("inserterInventory", this.itemStorage.serializeNBT());

        if (IS_ENERGY) {
            tag.putInt("inserterEnergyLevel", this.energyStorage.getEnergyStored());
        }
        else {
            tag.putInt("inserterFuelLevel",this.getCurrentFuelValue());
        }

        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        this.itemStorage.deserializeNBT(tag.getCompound("inserterInventory"));
        if(IS_ENERGY) {
            energyStorage.overrideCurrentEnergy(tag.getInt("inserterEnergyLevel"));
        }
        else {
            this.overrideCurrentFuelValue(tag.getInt("inserterFuelLevel"));
        }
    }

    public boolean stillValid(Player playerEntity) {
        if (this.level.getBlockEntity(this.worldPosition) != this) {
            return false;
        } else {
            return !(playerEntity.distanceToSqr((double)this.worldPosition.getX() + 0.5, (double)this.worldPosition.getY() + 0.5, (double)this.worldPosition.getZ() + 0.5) > 64.0);
        }
    }

    // Interface (Ticking)

    public static void tick(Level pLevel, BlockPos pPos, BlockState pState, final FactoryIOInserterBlockEntity pEntity) {
        if (!pLevel.isClientSide) {
            if (pEntity.IS_ENERGY) {
                FactoryIONetworks.sendToClients(new FactoryIOSyncS2CEnergy(pEntity.getCurrentEnergy(), pPos));
            }
            else {
                FactoryIONetworks.sendToClients(new FactoryIOSyncS2CFuel(pEntity.getCurrentFuelValue(), pPos));
            }
            if (pEntity.IS_FILTER) {
                FactoryIONetworks.sendToClients(new FactoryIOSyncS2CWhitelistButton((pEntity.isWhitelist()? 1 : 0), 6, pPos));
            }
        }

        if (!pEntity.isEnabled()) return;

        pEntity.current_cooldown += pEntity.MAX_ACTIONS_PER_TICK;

        if (pEntity.current_cooldown >= pEntity.getDurationBetweenActions()) {
            if ((pEntity.IS_ENERGY && pEntity.getCurrentEnergy() >= pEntity.getFuelConsumptionPerAction()) ||
                    (!pEntity.IS_ENERGY && pEntity.getCurrentFuelValue() >= pEntity.getFuelConsumptionPerAction())) {

                // TODO : Multiply item/energy count instead of for loop
                for (int i = 0; i < pEntity.getActionMultiplier(); i++) {
                    if (pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()) {
                        if (suckItems(pEntity, pLevel, pEntity.getGrabDistance(), pEntity.isWhitelist(), false)) {
                            pEntity.current_cooldown = 0;
                            pEntity.useFuelOrEnergy();
                        }
                    } else {
                        if (expelItems(pEntity, pLevel, pEntity.getGrabDistance(), false)) {
                            pEntity.current_cooldown = 0;
                            pEntity.useFuelOrEnergy();
                        }
                    }
                }
            }
        }

        if (!pEntity.IS_ENERGY) {
            ItemStack stack = pEntity.itemStorage.getStackInSlot(FUEL_SLOT);
            if (!stack.isEmpty() && stack.getItem() != Items.BUCKET &&
                    ForgeHooks.getBurnTime(stack, null) > 0 && ForgeHooks.getBurnTime(stack, null) < pEntity.getFuelCapacity() - pEntity.current_fuel_value) {
                pEntity.current_fuel_value += ForgeHooks.getBurnTime(stack, null);

                if (stack.is(Items.LAVA_BUCKET))
                    pEntity.itemStorage.setStackInSlot(FUEL_SLOT, new ItemStack(Items.BUCKET, 1));
                else
                    pEntity.itemStorage.setStackInSlot(FUEL_SLOT,  new ItemStack(stack.getItem(), stack.getCount()-1));
            }
        }
    }

    // Interface (Energy)

    public int getCurrentEnergy() {
        if (!IS_ENERGY) return -1;

        return this.energyStorage.getCurrentEnergy();
    }

    public int getEnergyCapacity() {
        if (!IS_ENERGY) return -1;

        return this.energyStorage.getEnergyCapacity();
    }

    public void overrideCurrentEnergy(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.overrideCurrentEnergy(energy);
    }

    public void overrideEnergyCapacity(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.overrideEnergyCapacity(energy);
    }

    public void removeEnergy(int energy, boolean simulate) {
        if (!IS_ENERGY) return;

        this.energyStorage.extractEnergy(energy, simulate);
    }

    // Interface (Fuel)

    public int getCurrentFuelValue() {
        if (IS_ENERGY) return -1;

        return this.current_fuel_value;
    }

    public int getInnerFuelCapacity() {
        if (IS_ENERGY) return -1;

        return this.getInnerFuelCapacity();
    }

    public void overrideCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        if (fuel < 0) this.current_fuel_value = 0;
        if (fuel > this.getFuelCapacity()) this.current_fuel_value = this.getFuelCapacity();
        this.current_fuel_value = fuel;
    }

    public void addToCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        this.current_fuel_value += fuel;
        this.overrideCurrentFuelValue(this.current_fuel_value);
    }

    public void removeFromToCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        this.current_fuel_value -= fuel;
        this.overrideCurrentFuelValue(this.current_fuel_value);
    }

    // Interface (Whitelist)

    public boolean isWhitelist() {
        return isWhitelist;
    }

    public void setWhitelist(boolean whitelist) {
        this.isWhitelist = whitelist;
    }

    // Interface (Enabled)

    public boolean isEnabled() {
        return this.getBlockState().getValue(BlockStateProperties.ENABLED);
    }

    public void setEnabled(boolean enabled) {
        this.getBlockState().setValue(BlockStateProperties.ENABLED, enabled);
    }

    // Inner work

    private static Direction getFacing(BlockEntity entity) {
        return entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static boolean checkItemStackNotPresentInWhitelist(FactoryIOInserterBlockEntity pEntity, ItemStack stack, boolean isWhitelist) {
        if (!pEntity.IS_FILTER) return true;
        else if (pEntity.itemStorage.getStackInSlot(pEntity.itemStorage.getSlots() - 5).isEmpty() &&
                pEntity.itemStorage.getStackInSlot(pEntity.itemStorage.getSlots() - 4).isEmpty() &&
                pEntity.itemStorage.getStackInSlot(pEntity.itemStorage.getSlots() - 3).isEmpty() &&
                pEntity.itemStorage.getStackInSlot(pEntity.itemStorage.getSlots() - 2).isEmpty() &&
                pEntity.itemStorage.getStackInSlot(pEntity.itemStorage.getSlots() - 1).isEmpty()
        ) return true;

        for(int i = pEntity.itemStorage.getSlots() - 5 ; i < pEntity.itemStorage.getSlots(); i++) {
            if (pEntity.itemStorage.getStackInSlot(i).isEmpty())
                continue;
            if (!pEntity.itemStorage.getStackInSlot(i).isEmpty() && ItemStack.isSameItemSameTags(pEntity.itemStorage.getStackInSlot(i), stack)) {
                if (isWhitelist) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        if(isWhitelist) return false;
        else return true;
    }

    private ItemStack insertItemInternal(int slot, @Nonnull ItemStack itemStack, boolean simulate) {
        if (itemStack.isEmpty()) return itemStack;

        ItemStack currentItemStack = this.itemStorage.getStackInSlot(slot);
        int countLimitForItemStack = itemStack.getMaxStackSize();

        if (!currentItemStack.isEmpty()) {
            if (!ItemHandlerHelper.canItemStacksStack(itemStack, currentItemStack)) {
                return itemStack;
            }

            countLimitForItemStack -= currentItemStack.getCount();
        }

        // Cannot Insert More
        if (countLimitForItemStack <= 0) return itemStack;

        boolean reachedLimit = itemStack.getCount() > countLimitForItemStack;

        if (!simulate) {
            if (currentItemStack.isEmpty()) {
                this.itemStorage.setStackInSlot(slot, reachedLimit ? ItemHandlerHelper.copyStackWithSize(itemStack, countLimitForItemStack) : itemStack);
            } else {
                currentItemStack.grow(reachedLimit ? countLimitForItemStack : itemStack.getCount());
            }

            this.setChanged();
        }

        return reachedLimit ? ItemHandlerHelper.copyStackWithSize(itemStack, itemStack.getCount() - countLimitForItemStack) : ItemStack.EMPTY;
    }

    @Nonnull
    private ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        // No Removal
        if (amount <= 0) return ItemStack.EMPTY;

        // Empty Slot
        ItemStack currentItemStack = this.itemStorage.getStackInSlot(slot);
        if (currentItemStack.isEmpty()) return ItemStack.EMPTY;

        int itemCountToExtract = Math.min(amount, currentItemStack.getMaxStackSize());

        if (currentItemStack.getCount() <= itemCountToExtract) {
            if (!simulate) {
                this.itemStorage.setStackInSlot(slot, ItemStack.EMPTY);
                this.setChanged();

                return currentItemStack;
            }

            return currentItemStack.copy();
        }

        if (!simulate) {
            this.itemStorage.setStackInSlot(slot, ItemHandlerHelper.copyStackWithSize(currentItemStack, currentItemStack.getCount() - itemCountToExtract));
            this.setChanged();
        }

        return ItemHandlerHelper.copyStackWithSize(currentItemStack, itemCountToExtract);
    }

    private static boolean suckItems(FactoryIOInserterBlockEntity pEntity, Level pLevel, int pDistance, boolean isWhitelist, boolean simulate) {
        BlockEntity pBackEntity = pLevel.getBlockEntity(pEntity.getBlockPos().relative(getFacing(pEntity).getOpposite(), pDistance));
        if (pBackEntity == null) return false;

        IItemHandler pBackEntityItemHandler = (IItemHandler) pBackEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing(pEntity)).orElse(null);
        if (pBackEntityItemHandler == null) return false;

        // No Energy => Items to Fuel Buffer
        // TODO : Choose item count in fuel buffer (check for buckets)
        if (!pEntity.IS_ENERGY && !pEntity.itemStorage.getStackInSlot(FUEL_SLOT).isEmpty()) {
            ItemStack itemStackToGet;

            for(int i = 0; i < pBackEntityItemHandler.getSlots(); i++) {
                itemStackToGet = pBackEntityItemHandler.extractItem(
                    i,
                    Math.min(pEntity.getMaximumItemCountPerAction(),
                    pBackEntityItemHandler.getStackInSlot(i).getMaxStackSize()),
                    true);

                if (!itemStackToGet.isEmpty() && itemStackToGet.is(FactoryIOTags.Items.INSERTER_FUEL)) {
                    pEntity.insertItemInternal(
                        FUEL_SLOT,
                        pBackEntityItemHandler.extractItem(
                            i,
                            Math.min(pEntity.getMaximumItemCountPerAction(),
                            pBackEntityItemHandler.getStackInSlot(i).getMaxStackSize()),
                            simulate),
                        simulate);

                    return true;
                }
            }

        }

        // In Internal Buffer ( BUFFER_SLOT ) if Empty
        if (pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()) {
            ItemStack itemStackToGet;

            for(int i = 0; i < pBackEntityItemHandler.getSlots(); i++) {
                itemStackToGet = pBackEntityItemHandler.extractItem(
                        i,
                        Math.min(pEntity.getMaximumItemCountPerAction(),
                                pBackEntityItemHandler.getStackInSlot(i).getMaxStackSize()),
                        true);

                if (checkItemStackNotPresentInWhitelist(pEntity, itemStackToGet, isWhitelist) && !itemStackToGet.isEmpty()) {
                    pEntity.insertItemInternal(
                            BUFFER_SLOT,
                            pBackEntityItemHandler.extractItem(
                                    i,
                                    Math.min(pEntity.getMaximumItemCountPerAction(),
                                            pBackEntityItemHandler.getStackInSlot(i).getMaxStackSize()),
                                    simulate),
                            simulate);

                    return true;
                }
            }
        }

        // If no Item found
        return false;
    }

    private static boolean expelItems(FactoryIOInserterBlockEntity pEntity, Level pLevel, int pDistance, boolean simulate) {
        if (pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()) return false;

        BlockEntity pBackEntity = pLevel.getBlockEntity(pEntity.getBlockPos().relative(getFacing(pEntity), pDistance));
        if (pBackEntity == null) return false;

        IItemHandler pBackEntityItemHandler = (IItemHandler) pBackEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, getFacing(pEntity)).orElse(null);
        if (pBackEntityItemHandler == null) return false;

        boolean check;

        for(int i = 0; i < pBackEntityItemHandler.getSlots(); ++i) {
            check = pBackEntityItemHandler.insertItem(
                    i,
                    pEntity.extractItemInternal(
                            BUFFER_SLOT,
                            Math.min(pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).getCount(), pEntity.getMaximumItemCountPerAction()),
                            true),
                    true)
                    .isEmpty();

            if (check) {
                pBackEntityItemHandler.insertItem(
                        i,
                        pEntity.extractItemInternal(
                                BUFFER_SLOT,
                                Math.min(pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).getCount(), pEntity.getMaximumItemCountPerAction()),
                                simulate),
                        simulate);
                return true;
            }

        }

        return false;
    }

    private int getActionMultiplier() {
        int actionMultiplier = 1;
        int duration = this.getDurationBetweenActions();

        if (duration < this.MAX_ACTIONS_PER_TICK) {
            actionMultiplier += this.MAX_ACTIONS_PER_TICK - duration;
        }

        return actionMultiplier;
    }

    private void useFuelOrEnergy() {
        if (this.IS_ENERGY) {
            this.removeEnergy(this.getFuelConsumptionPerAction(), false);
        }
        else {
            this.removeFromToCurrentFuelValue(this.getFuelConsumptionPerAction());
        }
    }

    // Interface GeckLib
    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<FactoryIOInserterBlockEntity>
                (this, "controller", 0, this::predicate));
    }

    private <E extends IAnimatable> PlayState predicate (AnimationEvent<E> event) {
        event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));

        return PlayState.CONTINUE;
    }

    public AnimationFactory getFactory() {
        return this.factory;
    }

    // Interface (Menu)

    @org.jetbrains.annotations.Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new FactoryIOInserterContainer(this.menuType, pContainerId, level, getBlockPos(), pPlayerInventory, pPlayer);
    }
}
