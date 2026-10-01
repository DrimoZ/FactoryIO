package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.generic.block.RedstoneCondition;
import com.drimoz.factoryio.core.generic.container.energy.EnergyContainer;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeEffects;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeType;
import com.drimoz.factoryio.core.upgrade.InserterUpgradeTunings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Le crafter au travail. Voir docs/12 §4.
 *
 * <h2>Inventaire</h2>
 *
 * <p>Neuf entrées, <b>une par ingrédient</b> de la recette : l'entrée {@code i} n'accepte que
 * l'ingrédient {@code i}, et au plus {@code inputCrafts} crafts d'avance — l'inserter s'arrête
 * de lui-même au lieu de vider un convoyeur dans une seule machine. Quatre sorties en
 * réserve commune, pour les résultats comme pour les restes (le seau vide du seau de lait).
 *
 * <p>Vu de l'extérieur, par n'importe quelle face des 18 blocs, on ne fait qu'insérer dans
 * les entrées et extraire des sorties ({@link #external}).
 *
 * <h2>Les ingrédients se consomment à la fin du craft</h2>
 *
 * <p>Pas au début, comme dans Factorio : tant que le craft n'est pas fini, ses ingrédients
 * restent dans leurs slots. Casser la machine ou changer de recette les rend donc sans
 * aucune comptabilité, et la conservation des items ne dépend d'aucun état intermédiaire.
 * L'inserter garde {@code inputCrafts} crafts d'avance, craft en cours compris.
 *
 * <h2>Chemin chaud</h2>
 *
 * <p>La recette résolue est gardée, et recherchée seulement quand
 * {@link CrafterRecipes#generation()} a bougé. Une sortie pleine n'est réessayée que quand
 * une sortie a changé. Le tick ordinaire n'alloue rien.
 */
public class CrafterBlockEntity extends BlockEntity implements MenuProvider {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    /** Entrées et sorties : tout ce que voient inserters et convoyeurs. */
    public static final int SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    /** Slots de module, après les autres et invisibles de l'extérieur (FIO-127). Le palier dit combien servent. */
    public static final int MODULE_SLOTS = 4;
    public static final int MODULE_FIRST = SLOTS;
    public static final int TOTAL_SLOTS = SLOTS + MODULE_SLOTS;

    /** Ticks sans travail avant d'éteindre {@link CrafterBlock#WORKING} : pas de clignotement entre deux crafts. */
    private static final int WORKING_GRACE_TICKS = 20;

    /** Ce que la machine fait, pour l'écran. L'ordre est celui du réseau ({@code ContainerData}). */
    public enum Status { NO_RECIPE, RECIPE_MISSING, TIER_TOO_LOW, SWITCHED_OFF, DISABLED, NO_ENERGY, NO_INPUTS, OUTPUT_FULL, WORKING }

    private final ItemStackHandler items = new ItemStackHandler(TOTAL_SLOTS) {
        /**
         * La taille sauvegardée ne fait pas foi : {@code deserializeNBT} redimensionnerait
         * l'inventaire à celle d'une sauvegarde antérieure (13 slots avant les modules), et le
         * premier accès à un slot de module planterait le serveur.
         */
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            CompoundTag current = nbt.copy();
            current.putInt("Size", TOTAL_SLOTS);
            super.deserializeNBT(current);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= MODULE_FIRST) return slot - MODULE_FIRST < getCrafter().getModuleSlots() && isModule(stack);
            if (slot >= INPUT_SLOTS) return false;

            CrafterRecipe recipe = CrafterBlockEntity.this.recipe;
            return recipe != null && slot < recipe.inputs().size() && recipe.inputs().get(slot).ingredient().test(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot >= MODULE_FIRST) return 1;
            CrafterRecipe recipe = CrafterBlockEntity.this.recipe;
            if (slot >= INPUT_SLOTS || recipe == null || slot >= recipe.inputs().size()) return 64;

            return Math.min(64, recipe.inputs().get(slot).count() * getCrafter().getTuning().inputCrafts());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot >= MODULE_FIRST) {
                CrafterBlockEntity.this.modules = readModules();
                reevaluateEnabled();
            }
            else if (slot >= INPUT_SLOTS) CrafterBlockEntity.this.outputsChanged = true;
        }
    };

    /** Ce que voient inserters, convoyeurs et mods tiers : on remplit les entrées, on vide les sorties. */
    private final IItemHandler external = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return slot < INPUT_SLOTS ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= INPUT_SLOTS ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot < INPUT_SLOTS && items.isItemValid(slot, stack);
        }
    };

    /** Reçoit, ne rend jamais : aucun voisin ne doit pouvoir siphonner la machine. */
    private final EnergyContainer energy = new EnergyContainer(1, Integer.MAX_VALUE, 0);

    /** Deux réservoirs d'entrée, deux de sortie (FIO-179). */
    private final CrafterTanks tanks = new CrafterTanks(this::setChanged);

    private LazyOptional<IItemHandler> lazyItems = LazyOptional.of(() -> this.external);
    private LazyOptional<IFluidHandler> lazyFluids = LazyOptional.of(() -> this.tanks.external);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> this.energy);

    @Nullable private ResourceLocation recipeId;
    @Nullable private CrafterRecipe recipe;
    private int resolvedGeneration = -1;

    /** Avancement du craft, en ticks de recette : on y ajoute la vitesse de la machine. */
    private float progress;
    private boolean outputBlocked;
    private boolean outputsChanged;
    private int idleTicks;
    private Status status = Status.NO_RECIPE;

    // Contrôle (FIO-183), comme sur les inserters : l'interrupteur l'emporte sur tout signal.
    private boolean switchedOn = true;
    private RedstoneCondition redstoneCondition = RedstoneCondition.DEFAULT;
    /** Crafts achevés depuis le chargement : l'écran en tire un rythme mesuré, par différence. */
    private int craftsCompleted;

    /** Effet des modules posés, relu quand un slot de module change — jamais au tick. */
    private CrafterModules modules = CrafterModules.NONE;
    /** Avancement de la productivité : à 1, un jeu de résultats en plus (FIO-127). */
    private float productivityProgress;

    // Réserve de simulation des sorties, allouée une fois.
    private final ItemStack[] simulatedItems = new ItemStack[OUTPUT_SLOTS];
    private final int[] simulatedCounts = new int[OUTPUT_SLOTS];

    public CrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CRAFTER_ENTITY.get(), pos, state);

        this.energy.overrideEnergyCapacity(getCrafter().getTuning().energyCapacity());
    }

    public Crafter getCrafter() {
        return ((CrafterBlock) getBlockState().getBlock()).getCrafter();
    }

    // Interface (Tick)

    public static void tick(Level level, BlockPos pos, BlockState state, CrafterBlockEntity crafter) {
        crafter.serverTick(level, pos, state);
    }

    private void serverTick(Level level, BlockPos pos, BlockState state) {
        Crafter.Tuning tuning = getCrafter().getTuning();
        if (this.energy.getEnergyCapacity() != tuning.energyCapacity()) {
            this.energy.overrideEnergyCapacity(tuning.energyCapacity());
        }

        this.tanks.configure(resolveRecipe(level), tuning);
        this.status = work(level, state, tuning);
        boolean worked = this.status == Status.WORKING;

        if (worked) {
            this.idleTicks = 0;
            level.blockEntityChanged(pos);
            if (!state.getValue(CrafterBlock.WORKING)) {
                level.setBlock(pos, state.setValue(CrafterBlock.WORKING, true), Block.UPDATE_CLIENTS);
            }
        } else if (state.getValue(CrafterBlock.WORKING) && ++this.idleTicks > WORKING_GRACE_TICKS) {
            level.setBlock(pos, state.setValue(CrafterBlock.WORKING, false), Block.UPDATE_CLIENTS);
        }
    }

    private Status work(Level level, BlockState state, Crafter.Tuning tuning) {
        CrafterRecipe recipe = resolveRecipe(level);
        if (this.recipeId == null) return Status.NO_RECIPE;
        if (recipe == null) return Status.RECIPE_MISSING;
        if (recipe.minTier() > getCrafter().getTier()) return Status.TIER_TOO_LOW;
        if (!state.getValue(CrafterBlock.ENABLED)) return this.switchedOn ? Status.DISABLED : Status.SWITCHED_OFF;
        if (!hasInputs(recipe)) return Status.NO_INPUTS;

        if (this.progress < recipe.ticks()) {
            int energyPerTick = this.modules.energyPerTick(tuning.energyPerTick());
            if (this.energy.getCurrentEnergy() < energyPerTick) return Status.NO_ENERGY;

            this.energy.consumeInternal(energyPerTick);
            this.progress += this.modules.speed(tuning.craftingSpeed());
            if (this.progress < recipe.ticks()) return Status.WORKING;
        }

        // Craft achevé : il ne reste qu'à trouver la place de ce qu'il produit.
        if (this.outputBlocked && !this.outputsChanged) return Status.OUTPUT_FULL;
        this.outputsChanged = false;

        if (!finish(level, recipe)) {
            this.outputBlocked = true;
            return Status.OUTPUT_FULL;
        }

        this.outputBlocked = false;
        this.progress = 0.0F;
        return Status.WORKING;
    }

    /** La recette choisie, recherchée à nouveau seulement après un rechargement. */
    @Nullable
    private CrafterRecipe resolveRecipe(Level level) {
        int generation = CrafterRecipes.generation();
        if (this.resolvedGeneration != generation) {
            this.resolvedGeneration = generation;
            this.recipe = this.recipeId == null ? null : CrafterRecipes.byId(level, this.recipeId).orElse(null);
            this.outputBlocked = false;
        }
        return this.recipe;
    }

    private boolean hasInputs(CrafterRecipe recipe) {
        if (!this.tanks.hasInputs(recipe)) return false;

        List<CrafterRecipe.Input> inputs = recipe.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack stack = this.items.getStackInSlot(i);
            CrafterRecipe.Input input = inputs.get(i);
            if (stack.getCount() < input.count() || !input.ingredient().test(stack)) return false;
        }
        return true;
    }

    /**
     * Consomme les ingrédients et range les résultats — ou ne touche à rien si tout ne
     * tient pas. La place est réservée pour <b>tous</b> les résultats possibles, tirage ou
     * non : un tirage heureux ne doit jamais se perdre.
     */
    private boolean finish(Level level, CrafterRecipe recipe) {
        // La productivité qui déborde ce craft rend un jeu de résultats de plus : sa place aussi
        // est réservée d'avance.
        boolean bonus = this.productivityProgress + this.modules.productivity() >= 1.0F;

        List<ItemStack> produced = new ArrayList<>(2 * recipe.outputs().size() + recipe.inputs().size());
        for (CrafterRecipe.Output output : recipe.outputs()) produced.add(output.stack());
        if (bonus) for (CrafterRecipe.Output output : recipe.outputs()) produced.add(output.stack());

        List<ItemStack> remainders = remainders(recipe);
        produced.addAll(remainders);
        int sets = bonus ? 2 : 1;
        if (!fits(produced) || !this.tanks.fits(recipe, sets)) return false;

        for (int i = 0; i < recipe.inputs().size(); i++) {
            this.items.extractItem(i, recipe.inputs().get(i).count(), false);
        }
        this.tanks.craft(recipe, sets);
        for (int set = 0; set < sets; set++) {
            for (CrafterRecipe.Output output : recipe.outputs()) {
                if (output.isCertain() || level.random.nextFloat() < output.chance()) addOutput(output.stack().copy());
            }
        }
        remainders.forEach(this::addOutput);

        this.productivityProgress += this.modules.productivity();
        if (bonus) this.productivityProgress -= 1.0F;
        this.craftsCompleted++;
        return true;
    }

    /** Ce que laissent les ingrédients consommés : un seau par seau de lait. */
    private List<ItemStack> remainders(CrafterRecipe recipe) {
        List<ItemStack> remainders = new ArrayList<>();
        for (int i = 0; i < recipe.inputs().size(); i++) {
            ItemStack remainder = this.items.getStackInSlot(i).getCraftingRemainingItem();
            if (remainder.isEmpty()) continue;

            remainder.setCount(remainder.getCount() * recipe.inputs().get(i).count());
            remainders.add(remainder);
        }
        return remainders;
    }

    /** Tout {@code stacks} tient-il dans les sorties, ensemble ? */
    private boolean fits(List<ItemStack> stacks) {
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            ItemStack current = this.items.getStackInSlot(INPUT_SLOTS + slot);
            this.simulatedItems[slot] = current;
            this.simulatedCounts[slot] = current.getCount();
        }

        for (ItemStack stack : stacks) {
            int left = stack.getCount();
            for (int slot = 0; slot < OUTPUT_SLOTS && left > 0; slot++) {
                ItemStack held = this.simulatedItems[slot];
                if (this.simulatedCounts[slot] > 0 && ItemStack.isSameItemSameTags(held, stack)) {
                    int moved = Math.min(left, stack.getMaxStackSize() - this.simulatedCounts[slot]);
                    this.simulatedCounts[slot] += Math.max(0, moved);
                    left -= Math.max(0, moved);
                }
            }
            for (int slot = 0; slot < OUTPUT_SLOTS && left > 0; slot++) {
                if (this.simulatedCounts[slot] == 0) {
                    this.simulatedItems[slot] = stack;
                    int moved = Math.min(left, stack.getMaxStackSize());
                    this.simulatedCounts[slot] = moved;
                    left -= moved;
                }
            }
            if (left > 0) return false;
        }
        return true;
    }

    /** Range dans les sorties ; {@link #fits} a déjà garanti la place. */
    private void addOutput(ItemStack stack) {
        for (int slot = INPUT_SLOTS; slot < SLOTS && !stack.isEmpty(); slot++) {
            ItemStack held = this.items.getStackInSlot(slot);
            if (!held.isEmpty() && ItemStack.isSameItemSameTags(held, stack)) {
                int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                if (moved > 0) {
                    this.items.setStackInSlot(slot, held.copyWithCount(held.getCount() + moved));
                    stack.shrink(moved);
                }
            }
        }
        for (int slot = INPUT_SLOTS; slot < SLOTS && !stack.isEmpty(); slot++) {
            if (this.items.getStackInSlot(slot).isEmpty()) {
                int moved = Math.min(stack.getCount(), stack.getMaxStackSize());
                this.items.setStackInSlot(slot, stack.copyWithCount(moved));
                stack.shrink(moved);
            }
        }
    }

    // Interface (Recette)

    @Nullable
    public ResourceLocation getRecipeId() {
        return this.recipeId;
    }

    /**
     * Choisit une recette, ou aucune avec {@code null}.
     *
     * <p>Les entrées de l'ancienne recette — y compris celles d'un craft en cours, qui
     * n'avait encore rien consommé — vont au joueur, et le surplus tombe à ses pieds. Les
     * sorties restent.
     *
     * @return faux si la recette n'existe pas ou dépasse le palier de la machine
     */
    public boolean selectRecipe(@Nullable ResourceLocation id, @Nullable Player player) {
        if (this.level == null) return false;

        CrafterRecipe candidate = null;
        if (id != null) {
            candidate = CrafterRecipes.byId(this.level, id).orElse(null);
            if (candidate == null || candidate.minTier() > getCrafter().getTier()) return false;
        }
        if (id == null ? this.recipeId == null : id.equals(this.recipeId)) return true;

        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            this.items.setStackInSlot(slot, ItemStack.EMPTY);
            giveBack(stack, player);
        }

        // Résolue tout de suite : les entrées filtrent dès maintenant sur la nouvelle recette.
        this.recipeId = id;
        this.recipe = candidate;
        this.tanks.onRecipeChanged(candidate, getCrafter().getTuning());
        this.resolvedGeneration = CrafterRecipes.generation();
        this.progress = 0.0F;
        this.outputBlocked = false;
        setChanged();
        return true;
    }

    private void giveBack(ItemStack stack, @Nullable Player player) {
        if (player != null && player.getInventory().add(stack) && stack.isEmpty()) return;

        if (player != null) {
            player.drop(stack, false);
        } else {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY() + 1, this.worldPosition.getZ(), stack);
        }
    }

    /** Casse : tout ce que contient la machine, entrées comprises. */
    public void dropContents() {
        if (this.level == null) return;

        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(),
                    this.worldPosition.getZ(), this.items.getStackInSlot(slot));
        }
    }

    // Interface (Contrôle, FIO-183)

    public boolean isSwitchedOn() {
        return this.switchedOn;
    }

    public void setSwitchedOn(boolean on) {
        if (this.switchedOn == on) return;

        this.switchedOn = on;
        setChanged();
        reevaluateEnabled();
    }

    /**
     * La condition en vigueur : celle réglée si le module de redstone avancée est posé, la
     * réaction native sinon — exactement comme un inserter (FIO-172, FIO-185).
     */
    public RedstoneCondition getRedstoneCondition() {
        return isConditionUnlocked() ? this.redstoneCondition : RedstoneCondition.DEFAULT;
    }

    /** La condition telle que le joueur l'a réglée, module ou non : l'écran l'affiche. */
    public RedstoneCondition getConfiguredRedstoneCondition() {
        return this.redstoneCondition;
    }

    /** Le module de redstone avancée est posé — ou le barème des datapacks ne l'exige pas. */
    public boolean isConditionUnlocked() {
        return InserterUpgradeEffects.unlocked(InserterUpgradeType.ADVANCED_REDSTONE,
                hasAdvancedRedstone() ? 1 : 0, InserterUpgradeTunings.current());
    }

    private boolean hasAdvancedRedstone() {
        int active = Math.min(MODULE_SLOTS, getCrafter().getModuleSlots());
        for (int i = 0; i < active; i++) {
            if (InserterUpgradeType.ADVANCED_REDSTONE.levelOf(this.items.getStackInSlot(MODULE_FIRST + i)) > 0) return true;
        }
        return false;
    }

    public void setRedstoneCondition(RedstoneCondition condition) {
        if (this.redstoneCondition.equals(condition)) return;

        this.redstoneCondition = condition;
        setChanged();
        reevaluateEnabled();
    }

    /** Recalcule {@code ENABLED} tout de suite, sans attendre un changement de voisinage. */
    private void reevaluateEnabled() {
        if (this.level == null || this.level.isClientSide) return;

        BlockState state = getBlockState();
        state.getBlock().neighborChanged(state, this.level, this.worldPosition, state.getBlock(), this.worldPosition, false);
    }

    // Interface (Écran)

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CrafterMenu(containerId, inventory, this);
    }

    /** Inventaire complet, pour le menu : le joueur peut aussi reprendre des entrées. */
    public ItemStackHandler getItems() {
        return this.items;
    }

    public int getCraftsCompleted() {
        return this.craftsCompleted;
    }

    /** Réservoir {@code index} : 0 et 1 en entrée, 2 et 3 en sortie. */
    public net.minecraftforge.fluids.capability.templates.FluidTank getTank(int index) {
        return this.tanks.tank(index);
    }

    public CrafterModules getModules() {
        return this.modules;
    }

    public float getProductivityProgress() {
        return this.productivityProgress;
    }

    /** Vitesse de fabrication avec les modules. */
    public float getEffectiveSpeed() {
        return this.modules.speed(getCrafter().getTuning().craftingSpeed());
    }

    /** FE par tick de travail avec les modules. */
    public int getEffectiveEnergyPerTick() {
        return this.modules.energyPerTick(getCrafter().getTuning().energyPerTick());
    }

    /**
     * La nature d'un module, par les tags des inserters ; {@code null} si ce n'en est pas un
     * qui sert au crafter. Le tag {@code capacity} porte les modules de productivité.
     */
    /** Ce qu'un slot de module accepte : un module d'effet, ou celui de redstone avancée. */
    public static boolean isModule(ItemStack stack) {
        return moduleKind(stack) != null || InserterUpgradeType.ADVANCED_REDSTONE.levelOf(stack) > 0;
    }

    @Nullable
    public static CrafterModules.Kind moduleKind(ItemStack stack) {
        if (InserterUpgradeType.SPEED.levelOf(stack) > 0) return CrafterModules.Kind.SPEED;
        if (InserterUpgradeType.CAPACITY.levelOf(stack) > 0) return CrafterModules.Kind.PRODUCTIVITY;
        if (InserterUpgradeType.EFFICIENCY.levelOf(stack) > 0) return CrafterModules.Kind.EFFICIENCY;
        return null;
    }

    private static int moduleTier(CrafterModules.Kind kind, ItemStack stack) {
        return switch (kind) {
            case SPEED -> InserterUpgradeType.SPEED.levelOf(stack);
            case PRODUCTIVITY -> InserterUpgradeType.CAPACITY.levelOf(stack);
            case EFFICIENCY -> InserterUpgradeType.EFFICIENCY.levelOf(stack);
        };
    }

    /** Les modules des slots actifs : un slot au-delà du palier ne compte pas. */
    private CrafterModules readModules() {
        int[][] counts = new int[CrafterModules.Kind.values().length][CrafterModules.MAX_TIER];
        int active = Math.min(MODULE_SLOTS, getCrafter().getModuleSlots());
        for (int i = 0; i < active; i++) {
            ItemStack stack = this.items.getStackInSlot(MODULE_FIRST + i);
            CrafterModules.Kind kind = moduleKind(stack);
            if (kind == null) continue;
            int tier = Math.min(CrafterModules.MAX_TIER, moduleTier(kind, stack));
            if (tier > 0) counts[kind.ordinal()][tier - 1]++;
        }
        return CrafterModules.of(counts);
    }

    public Status getStatus() {
        return this.status;
    }

    /** Avancement du craft en cours, de 0 à 1. */
    public float getProgress() {
        return this.recipe == null ? 0.0F : Math.min(1.0F, this.progress / this.recipe.ticks());
    }

    public int getEnergyStored() {
        return this.energy.getCurrentEnergy();
    }

    public int getEnergyCapacity() {
        return this.energy.getEnergyCapacity();
    }

    // Interface (Capabilities)

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return this.lazyItems.cast();
        if (cap == ForgeCapabilities.ENERGY) return this.lazyEnergy.cast();
        if (cap == ForgeCapabilities.FLUID_HANDLER) return this.lazyFluids.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.lazyItems.invalidate();
        this.lazyEnergy.invalidate();
        this.lazyFluids.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.lazyItems = LazyOptional.of(() -> this.external);
        this.lazyEnergy = LazyOptional.of(() -> this.energy);
        this.lazyFluids = LazyOptional.of(() -> this.tanks.external);
    }

    // Persistance

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", this.items.serializeNBT());
        tag.putInt("energy", this.energy.getCurrentEnergy());
        tag.putFloat("progress", this.progress);
        if (this.recipeId != null) tag.putString("recipe", this.recipeId.toString());
        tag.putBoolean("switchedOn", this.switchedOn);
        tag.putFloat("productivity", this.productivityProgress);
        tag.put("tanks", this.tanks.save());
        tag.putByte("redstoneMode", (byte) this.redstoneCondition.mode().ordinal());
        tag.putByte("redstoneThreshold", (byte) this.redstoneCondition.threshold());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.deserializeNBT(tag.getCompound("items"));
        this.energy.overrideCurrentEnergy(tag.getInt("energy"));
        this.progress = tag.getFloat("progress");
        this.recipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        this.resolvedGeneration = -1;

        // contains et non getBoolean : une machine posée avant FIO-183 reste allumée.
        this.switchedOn = !tag.contains("switchedOn") || tag.getBoolean("switchedOn");
        this.productivityProgress = tag.getFloat("productivity");
        this.tanks.load(tag.getCompound("tanks"));
        this.modules = readModules();
        this.redstoneCondition = tag.contains("redstoneMode")
                ? new RedstoneCondition(RedstoneCondition.Mode.byOrdinal(tag.getByte("redstoneMode")), tag.getByte("redstoneThreshold"))
                : RedstoneCondition.DEFAULT;
    }
}
