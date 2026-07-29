package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.generic.block_entity.FactoryIOBlockEntityMenuProvided;
import com.drimoz.factoryio.core.generic.container.energy.FactoryIOEnergyContainer;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CEnabledState;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CEnergy;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CFuel;
import com.drimoz.factoryio.core.network.packet.FactoryIOSyncS2CWhitelistButton;
import com.drimoz.factoryio.core.init.FactoryIONetworks;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.init.FactoryIOTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.network.chat.Component;


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

import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class FactoryIOInserterBlockEntity extends FactoryIOBlockEntityMenuProvided implements GeoBlockEntity {

    // Public constants

    public static final int BUFFER_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int[] FILTER_SLOTS = new int[]{2, 3, 4, 5, 6};

    public final int INVENTORY_SIZE;
    public final boolean IS_ENERGY;
    public final boolean IS_FILTER;

    // Duration : 0 = 10a / tick || 10 = 1a / tick || 200 = 1a / 20tick (1sec) ||
    public static final int MAX_ACTIONS_PER_TICK = 10;

    /** Nombre d'items de carburant que l'inserter garde en réserve dans son slot. */
    public static final int FUEL_BUFFER_TARGET = 5;

    // Private constants

    private final Inserter inserter;

    // Protected properties

    protected FactoryIOEnergyContainer energyStorage;
    protected LazyOptional<IEnergyStorage> lazyEnergy;

    protected ItemStackHandler itemStorage;
    protected LazyOptional<IItemHandler> lazyItem;

    // private properties

    private int current_cooldown = 0;
    private boolean isWhitelist = true;
    private int current_fuel_value = 0;

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    // Life cycle

    public FactoryIOInserterBlockEntity(BlockPos blockPos, BlockState blockState, Inserter inserter) {
        this(inserter.getBlockEntityType().get(), blockPos, blockState, inserter);
    }

    public FactoryIOInserterBlockEntity(
            BlockEntityType<?> blockEntityType,
            BlockPos blockPos,
            BlockState blockState,
            Inserter inserter
    ) {
        super(blockEntityType, blockPos, blockState);

        this.inserter = inserter;

        this.IS_ENERGY = inserter.useEnergy();
        this.IS_FILTER = inserter.isFilterable();

        if (IS_ENERGY) {
            this.energyStorage = new FactoryIOEnergyContainer(inserter.getEnergyCapacity(), inserter.getEnergyTransferRate()) {
                @Override
                protected void onEnergyChanged() {
                    FactoryIOInserterBlockEntity.this.setChanged();
                }

                // Contrat externe : un bloc voisin ne doit jamais pouvoir vider
                // l'inserter. La consommation propre passe par consumeInternal().

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    return 0;
                }

                @Override
                public boolean canExtract() {
                    return false;
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
        return inserter.getPreferredItemCountPerAction();
    }

    public int getGrabDistance(){
        return inserter.getGrabDistance();
    }

    public int getDurationBetweenActions(){
        return inserter.getCooldownBetweenActions();
    }

    public int getFuelCapacity(){
        return IS_ENERGY ? inserter.getEnergyCapacity() : inserter.getFuelCapacity();
    }

    public int getFuelConsumptionPerAction() {
        return IS_ENERGY ? inserter.getEnergyConsumption() : inserter.getFuelConsumption();
    }

    /** Nombre d'items de carburant que l'inserter cherche à conserver en réserve. */
    public int getPreferredFuelItemBufferCount() {
        return FUEL_BUFFER_TARGET;
    }

    // Interface (Name)

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
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
        if (cap == ForgeCapabilities.ITEM_HANDLER) return lazyItem.cast();
        if (cap == ForgeCapabilities.ENERGY && IS_ENERGY && side == Direction.DOWN) return lazyEnergy.cast();
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

        // Sans ces deux lignes, chaque rechargement de monde remettait tous les filtres
        // en whitelist et repartait d'un cooldown nul (cf. BUG-008).
        tag.putBoolean("inserterWhitelist", this.isWhitelist);
        tag.putInt("inserterCooldown", this.current_cooldown);

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

        // contains() : un monde sauvegardé avant ce correctif n'a pas ces clés, et
        // getBoolean() renverrait false — soit l'inverse du défaut attendu.
        this.isWhitelist = !tag.contains("inserterWhitelist") || tag.getBoolean("inserterWhitelist");
        this.current_cooldown = tag.getInt("inserterCooldown");
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
            if (pState.getValue(FactoryIOInserterEntityBlock.ENABLED)) {
                FactoryIONetworks.sendToClients(new FactoryIOSyncS2CEnabledState(true, pPos));
            }
            else {
                FactoryIONetworks.sendToClients(new FactoryIOSyncS2CEnabledState(false, pPos));
            }

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

        if (pEntity.current_cooldown < pEntity.getDurationBetweenActions()) {
            pEntity.current_cooldown += MAX_ACTIONS_PER_TICK;
        }

        if (pEntity.current_cooldown >= pEntity.getDurationBetweenActions()) {

            // Le ravitaillement en carburant est gratuit et prioritaire : le conditionner
            // à la réserve courante enfermerait tout burner à sec dans un blocage
            // définitif (cf. BUG-012).
            if (pEntity.needsFuel() && refuel(pEntity, pLevel, pEntity.getGrabDistance())) {
                pEntity.current_cooldown = 0;
            }
            else if (pEntity.hasPowerForAction()) {
                // TODO : Multiply item/energy count instead of for loop
                for (int i = 0; i < pEntity.getActionMultiplier(); i++) {
                    if (pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()) {
                        if (suckItems(pEntity, pLevel, pEntity.getGrabDistance(), pEntity.isWhitelist())) {
                            pEntity.current_cooldown = 0;
                            pEntity.useFuelOrEnergy();
                        }
                    } else {
                        if (expelItems(pEntity, pLevel, pEntity.getGrabDistance())) {
                            pEntity.current_cooldown = 0;
                            pEntity.useFuelOrEnergy();
                        }
                    }
                }
            }
        }

        pEntity.burnFuel();
    }

    /** @return {@code true} si la réserve permet de payer une action */
    private boolean hasPowerForAction() {
        return IS_ENERGY
                ? getCurrentEnergy() >= getFuelConsumptionPerAction()
                : getCurrentFuelValue() >= getFuelConsumptionPerAction();
    }

    /** @return {@code true} si le slot de carburant doit être réapprovisionné */
    private boolean needsFuel() {
        if (IS_ENERGY) return false;

        return this.itemStorage.getStackInSlot(FUEL_SLOT).getCount() < this.getPreferredFuelItemBufferCount();
    }

    /** Aspire du carburant depuis l'inventaire situé à l'arrière. */
    private static boolean refuel(FactoryIOInserterBlockEntity pEntity, Level pLevel, int pDistance) {
        Direction facing = getFacing(pEntity);

        IItemHandler source = neighbourHandler(pEntity, pLevel, facing.getOpposite(), pDistance, facing);
        if (source == null) return false;

        return grabInto(pEntity, source, FUEL_SLOT, stack -> stack.is(FactoryIOTags.Items.INSERTER_FUEL));
    }

    /** Convertit un item du slot de carburant en réserve de combustion. */
    private void burnFuel() {
        if (IS_ENERGY) return;

        ItemStack stack = this.itemStorage.getStackInSlot(FUEL_SLOT);
        if (stack.isEmpty()) return;

        int burnTime = ForgeHooks.getBurnTime(stack, null);
        if (burnTime <= 0) return;
        if (burnTime > this.getFuelCapacity() - this.current_fuel_value) return;

        this.addToCurrentFuelValue(burnTime);

        // Conserve les items conteneurs (seau de lave -> seau vide) sans casser le NBT.
        ItemStack remainder = stack.hasCraftingRemainingItem() ? stack.getCraftingRemainingItem() : ItemStack.EMPTY;
        stack.shrink(1);

        if (stack.isEmpty() && !remainder.isEmpty()) {
            this.itemStorage.setStackInSlot(FUEL_SLOT, remainder);
        } else {
            this.itemStorage.setStackInSlot(FUEL_SLOT, stack);
            this.dropAtBlock(remainder);
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

    /**
     * Consomme l'énergie de fonctionnement.
     *
     * <p>Passe par {@code consumeInternal} et non par {@code extractEnergy}, que la
     * sous-classe anonyme neutralise volontairement pour interdire aux blocs voisins
     * de vider l'inserter (cf. BUG-003).
     */
    public void consumeEnergy(int energy) {
        if (!IS_ENERGY) return;

        this.energyStorage.consumeInternal(energy);
    }

    // Interface (Fuel)

    public int getCurrentFuelValue() {
        if (IS_ENERGY) return -1;

        return this.current_fuel_value;
    }

    public void overrideCurrentFuelValue(int fuel) {
        if(IS_ENERGY) return;

        // Les deux gardes précédentes étaient écrasées par l'affectation finale, si bien
        // que la réserve pouvait devenir négative ou dépasser la capacité (cf. BUG-013).
        this.current_fuel_value = Mth.clamp(fuel, 0, this.getFuelCapacity());
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
        if (this.level == null) return;

        // BlockState est immuable : setValue renvoie un nouvel état, il faut le poser
        // dans le monde (cf. BUG-018).
        this.level.setBlock(
                this.worldPosition,
                this.getBlockState().setValue(BlockStateProperties.ENABLED, enabled),
                Block.UPDATE_ALL);
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

    /**
     * @param offset  direction dans laquelle chercher le voisin, depuis l'inserter
     * @param side    face du voisin en contact avec l'inserter, du point de vue du voisin
     */
    @Nullable
    private static IItemHandler neighbourHandler(BlockEntity pEntity, Level pLevel, Direction offset, int pDistance, Direction side) {
        BlockEntity neighbour = pLevel.getBlockEntity(pEntity.getBlockPos().relative(offset, pDistance));
        if (neighbour == null) return null;

        return neighbour.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    /** Nombre d'items de {@code stack} que le slot interne {@code slot} peut réellement accueillir. */
    private int simulateInsertInternal(int slot, @Nonnull ItemStack stack) {
        if (stack.isEmpty()) return 0;

        ItemStack current = this.itemStorage.getStackInSlot(slot);
        if (current.isEmpty()) {
            return Math.min(stack.getCount(), stack.getMaxStackSize());
        }
        if (!ItemHandlerHelper.canItemStacksStack(stack, current)) return 0;

        return Math.max(0, Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount()));
    }

    /** Nombre d'items de {@code stack} que {@code handler} accepte, tous slots confondus. */
    private static int simulateInsert(IItemHandler handler, @Nonnull ItemStack stack) {
        int remaining = stack.getCount();

        for (int slot = 0; slot < handler.getSlots() && remaining > 0; slot++) {
            remaining = handler.insertItem(slot, ItemHandlerHelper.copyStackWithSize(stack, remaining), true).getCount();
        }

        return stack.getCount() - remaining;
    }

    /** Insère en répartissant sur plusieurs slots. @return ce qui n'a pas pu être placé */
    @Nonnull
    private static ItemStack insertDistributed(IItemHandler handler, @Nonnull ItemStack stack) {
        ItemStack remaining = stack;

        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
        }

        return remaining;
    }

    /**
     * Filet de sécurité : un reliquat inattendu ne doit jamais être détruit (cf. BUG-006).
     * On tente de le rendre à la source, puis on le laisse tomber au sol en dernier recours.
     */
    private void rescueLeftover(@Nonnull ItemStack leftover, UnaryOperator<ItemStack> fallback) {
        if (leftover.isEmpty()) return;

        FactoryIO.LOGGER.warn("Reliquat inattendu lors d'un transfert en {} : {}", this.worldPosition, leftover);

        this.dropAtBlock(fallback.apply(leftover));
    }

    /** Laisse tomber une pile au sol, en dernier recours. Sans effet si elle est vide. */
    private void dropAtBlock(@Nonnull ItemStack stack) {
        if (stack.isEmpty() || this.level == null) return;

        Containers.dropItemStack(
                this.level,
                this.worldPosition.getX() + 0.5,
                this.worldPosition.getY() + 0.5,
                this.worldPosition.getZ() + 0.5,
                stack);
    }

    /**
     * Prend au plus une « main » d'items acceptés par {@code accept} et les place dans
     * {@code targetSlot}.
     *
     * <p>Ordre impératif : simuler l'extraction, calculer ce que la destination accepte
     * réellement, puis extraire exactement cette quantité. L'inverse détruit des items.
     */
    private static boolean grabInto(FactoryIOInserterBlockEntity pEntity, IItemHandler source, int targetSlot, Predicate<ItemStack> accept) {
        int wanted = pEntity.getMaximumItemCountPerAction();

        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack probe = source.extractItem(slot, wanted, true);
            if (probe.isEmpty() || !accept.test(probe)) continue;

            int movable = pEntity.simulateInsertInternal(targetSlot, probe);
            if (movable <= 0) continue;

            ItemStack taken = source.extractItem(slot, movable, false);
            if (taken.isEmpty()) continue;

            pEntity.rescueLeftover(
                    pEntity.insertItemInternal(targetSlot, taken, false),
                    rest -> ItemHandlerHelper.insertItem(source, rest, false));
            return true;
        }

        return false;
    }

    private static boolean suckItems(FactoryIOInserterBlockEntity pEntity, Level pLevel, int pDistance, boolean isWhitelist) {
        Direction facing = getFacing(pEntity);

        // La face du coffre en contact avec l'inserter, vue depuis le coffre, est `facing`.
        IItemHandler source = neighbourHandler(pEntity, pLevel, facing.getOpposite(), pDistance, facing);
        if (source == null) return false;

        // 1. Ravitaillement en carburant tant que le buffer interne n'est pas rempli.
        //    La condition portait auparavant sur un slot NON vide, ce qui empêchait tout
        //    redémarrage après panne sèche (cf. BUG-012).
        if (pEntity.needsFuel()
                && grabInto(pEntity, source, FUEL_SLOT, stack -> stack.is(FactoryIOTags.Items.INSERTER_FUEL))) {
            return true;
        }

        // 2. Buffer de transport, uniquement s'il est libre.
        if (pEntity.itemStorage.getStackInSlot(BUFFER_SLOT).isEmpty()
                && grabInto(pEntity, source, BUFFER_SLOT, stack -> checkItemStackNotPresentInWhitelist(pEntity, stack, isWhitelist))) {
            return true;
        }

        return false;
    }

    private static boolean expelItems(FactoryIOInserterBlockEntity pEntity, Level pLevel, int pDistance) {
        ItemStack buffer = pEntity.itemStorage.getStackInSlot(BUFFER_SLOT);
        if (buffer.isEmpty()) return false;

        Direction facing = getFacing(pEntity);

        // La face de la cible en contact avec l'inserter, vue depuis la cible, est l'opposé
        // de `facing` (cf. BUG-023).
        IItemHandler target = neighbourHandler(pEntity, pLevel, facing, pDistance, facing.getOpposite());
        if (target == null) return false;

        int wanted = Math.min(buffer.getCount(), pEntity.getMaximumItemCountPerAction());

        ItemStack probe = pEntity.extractItemInternal(BUFFER_SLOT, wanted, true);
        if (probe.isEmpty()) return false;

        int movable = simulateInsert(target, probe);
        if (movable <= 0) return false;

        ItemStack taken = pEntity.extractItemInternal(BUFFER_SLOT, movable, false);
        if (taken.isEmpty()) return false;

        pEntity.rescueLeftover(
                insertDistributed(target, taken),
                rest -> pEntity.insertItemInternal(BUFFER_SLOT, rest, false));
        return true;
    }

    private int getActionMultiplier() {
        int actionMultiplier = 1;
        int duration = this.getDurationBetweenActions();

        if (duration < MAX_ACTIONS_PER_TICK) {
            actionMultiplier += MAX_ACTIONS_PER_TICK - duration;
        }

        return actionMultiplier;
    }

    private void useFuelOrEnergy() {
        if (this.IS_ENERGY) {
            this.consumeEnergy(this.getFuelConsumptionPerAction());
        }
        else {
            this.removeFromToCurrentFuelValue(this.getFuelConsumptionPerAction());
        }
    }

    // Interface GeckLib

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::idlePredicate));
    }

    private PlayState idlePredicate(AnimationState<FactoryIOInserterBlockEntity> state) {
        state.getController().setAnimation(IDLE);

        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }

    // Interface (Menu)

    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new FactoryIOInserterContainer(pContainerId, inserter, pPlayerInventory, level, getBlockPos());
    }
}
