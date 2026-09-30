package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.generic.container.energy.EnergyContainer;
import com.drimoz.factoryio.core.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
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
public class CrafterBlockEntity extends BlockEntity {

    public static final int INPUT_SLOTS = 9;
    public static final int OUTPUT_SLOTS = 4;
    public static final int SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;

    /** Ticks sans travail avant d'éteindre {@link CrafterBlock#WORKING} : pas de clignotement entre deux crafts. */
    private static final int WORKING_GRACE_TICKS = 20;

    /** Ce que la machine fait, pour l'écran. L'ordre est celui du réseau ({@code ContainerData}). */
    public enum Status { NO_RECIPE, RECIPE_MISSING, TIER_TOO_LOW, DISABLED, NO_ENERGY, NO_INPUTS, OUTPUT_FULL, WORKING }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot >= INPUT_SLOTS) return false;

            CrafterRecipe recipe = CrafterBlockEntity.this.recipe;
            return recipe != null && slot < recipe.inputs().size() && recipe.inputs().get(slot).ingredient().test(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            CrafterRecipe recipe = CrafterBlockEntity.this.recipe;
            if (slot >= INPUT_SLOTS || recipe == null || slot >= recipe.inputs().size()) return 64;

            return Math.min(64, recipe.inputs().get(slot).count() * getCrafter().getTuning().inputCrafts());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot >= INPUT_SLOTS) CrafterBlockEntity.this.outputsChanged = true;
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

    private LazyOptional<IItemHandler> lazyItems = LazyOptional.of(() -> this.external);
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
        if (!state.getValue(CrafterBlock.ENABLED)) return Status.DISABLED;
        if (!hasInputs(recipe)) return Status.NO_INPUTS;

        if (this.progress < recipe.ticks()) {
            if (this.energy.getCurrentEnergy() < tuning.energyPerTick()) return Status.NO_ENERGY;

            this.energy.consumeInternal(tuning.energyPerTick());
            this.progress += tuning.craftingSpeed();
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
        List<ItemStack> produced = new ArrayList<>(recipe.outputs().size() + recipe.inputs().size());
        for (CrafterRecipe.Output output : recipe.outputs()) produced.add(output.stack());

        List<ItemStack> remainders = remainders(recipe);
        produced.addAll(remainders);
        if (!fits(produced)) return false;

        for (int i = 0; i < recipe.inputs().size(); i++) {
            this.items.extractItem(i, recipe.inputs().get(i).count(), false);
        }
        for (CrafterRecipe.Output output : recipe.outputs()) {
            if (output.isCertain() || level.random.nextFloat() < output.chance()) addOutput(output.stack().copy());
        }
        remainders.forEach(this::addOutput);
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

        for (int slot = 0; slot < SLOTS; slot++) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY(),
                    this.worldPosition.getZ(), this.items.getStackInSlot(slot));
        }
    }

    // Interface (Écran)

    /** Inventaire complet, pour le menu : le joueur peut aussi reprendre des entrées. */
    public ItemStackHandler getItems() {
        return this.items;
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
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.lazyItems.invalidate();
        this.lazyEnergy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        this.lazyItems = LazyOptional.of(() -> this.external);
        this.lazyEnergy = LazyOptional.of(() -> this.energy);
    }

    // Persistance

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("items", this.items.serializeNBT());
        tag.putInt("energy", this.energy.getCurrentEnergy());
        tag.putFloat("progress", this.progress);
        if (this.recipeId != null) tag.putString("recipe", this.recipeId.toString());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.items.deserializeNBT(tag.getCompound("items"));
        this.energy.overrideCurrentEnergy(tag.getInt("energy"));
        this.progress = tag.getFloat("progress");
        this.recipeId = tag.contains("recipe") ? ResourceLocation.tryParse(tag.getString("recipe")) : null;
        this.resolvedGeneration = -1;
    }
}
