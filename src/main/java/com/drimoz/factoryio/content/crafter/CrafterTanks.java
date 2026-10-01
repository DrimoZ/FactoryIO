package com.drimoz.factoryio.content.crafter;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Les réservoirs d'un crafter (FIO-179) : deux en entrée, deux en sortie.
 *
 * <p>Comme les slots d'item : le réservoir d'entrée {@code i} n'accepte que le fluide {@code i}
 * de la recette, et au plus {@code inputCrafts} crafts d'avance ; les sorties ne se remplissent
 * que par la machine. Vu de l'extérieur ({@link #external}), on remplit les entrées et on vide
 * les sorties — rien d'autre.
 *
 * <p>Les capacités suivent la recette et le réglage : elles sont recalculées par
 * {@link #configure}, appelé au changement de recette et à chaque tick (une comparaison
 * d'entiers, sans allocation).
 */
final class CrafterTanks {

    static final int TANKS = 2 * CrafterRecipe.MAX_FLUIDS;

    private final FluidTank[] inputs = new FluidTank[CrafterRecipe.MAX_FLUIDS];
    private final FluidTank[] outputs = new FluidTank[CrafterRecipe.MAX_FLUIDS];
    @Nullable private CrafterRecipe recipe;

    CrafterTanks(Runnable onChange) {
        for (int i = 0; i < CrafterRecipe.MAX_FLUIDS; i++) {
            int index = i;
            this.inputs[i] = new FluidTank(0, stack -> accepts(index, stack)) {
                @Override
                protected void onContentsChanged() {
                    onChange.run();
                }
            };
            this.outputs[i] = new FluidTank(0) {
                @Override
                protected void onContentsChanged() {
                    onChange.run();
                }
            };
        }
    }

    private boolean accepts(int index, FluidStack stack) {
        CrafterRecipe recipe = this.recipe;
        return recipe != null && index < recipe.fluidInputs().size() && recipe.fluidInputs().get(index).test(stack);
    }

    // Réglage

    /** Capacités pour cette recette et ce réglage. Sans allocation : appelé au tick. */
    void configure(@Nullable CrafterRecipe recipe, Crafter.Tuning tuning) {
        this.recipe = recipe;
        for (int i = 0; i < CrafterRecipe.MAX_FLUIDS; i++) {
            int in = recipe != null && i < recipe.fluidInputs().size()
                    ? Math.min(CrafterCodec.MAX_FLUID_CAPACITY, recipe.fluidInputs().get(i).amount() * tuning.inputCrafts())
                    : 0;
            if (this.inputs[i].getCapacity() != in) this.inputs[i].setCapacity(in);

            int out = recipe != null && i < recipe.fluidOutputs().size()
                    ? Math.max(tuning.fluidCapacity(), 2 * recipe.fluidOutputs().get(i).getAmount())
                    : tuning.fluidCapacity();
            out = Math.min(CrafterCodec.MAX_FLUID_CAPACITY, out);
            if (this.outputs[i].getCapacity() != out) this.outputs[i].setCapacity(out);
        }
    }

    /**
     * Nouvelle recette : un réservoir d'entrée dont le fluide ne lui sert plus est vidé — un
     * fluide ne se rend pas au joueur comme un item, et resterait sinon à bloquer l'entrée.
     */
    void onRecipeChanged(@Nullable CrafterRecipe recipe, Crafter.Tuning tuning) {
        configure(recipe, tuning);
        for (int i = 0; i < CrafterRecipe.MAX_FLUIDS; i++) {
            FluidStack held = this.inputs[i].getFluid();
            if (!held.isEmpty() && !accepts(i, held)) this.inputs[i].setFluid(FluidStack.EMPTY);
        }
    }

    // Travail

    boolean hasInputs(CrafterRecipe recipe) {
        List<FluidInput> wanted = recipe.fluidInputs();
        for (int i = 0; i < wanted.size(); i++) {
            FluidStack held = this.inputs[i].getFluid();
            if (held.getAmount() < wanted.get(i).amount() || !wanted.get(i).test(held)) return false;
        }
        return true;
    }

    /** {@code sets} jeux de résultats fluides tiennent-ils dans les sorties ? */
    boolean fits(CrafterRecipe recipe, int sets) {
        List<FluidStack> produced = recipe.fluidOutputs();
        for (int i = 0; i < produced.size(); i++) {
            FluidStack out = produced.get(i);
            FluidTank tank = this.outputs[i];
            FluidStack held = tank.getFluid();
            if (!held.isEmpty() && !held.isFluidEqual(out)) return false;
            if (held.getAmount() + (long) out.getAmount() * sets > tank.getCapacity()) return false;
        }
        return true;
    }

    /** Consomme les fluides d'un craft et range {@code sets} jeux de résultats ; {@link #fits} a vérifié la place. */
    void craft(CrafterRecipe recipe, int sets) {
        for (int i = 0; i < recipe.fluidInputs().size(); i++) {
            this.inputs[i].drain(recipe.fluidInputs().get(i).amount(), IFluidHandler.FluidAction.EXECUTE);
        }
        for (int i = 0; i < recipe.fluidOutputs().size(); i++) {
            FluidStack out = recipe.fluidOutputs().get(i);
            FluidStack batch = new FluidStack(out, out.getAmount() * sets);
            if (this.outputs[i].fill(batch, IFluidHandler.FluidAction.EXECUTE) < batch.getAmount()) {
                throw new IllegalStateException("Sortie fluide pleine malgré fits() : " + batch);
            }
        }
    }

    // Accès

    /** Réservoir {@code index} : 0 et 1 en entrée, 2 et 3 en sortie. */
    FluidTank tank(int index) {
        return index < CrafterRecipe.MAX_FLUIDS ? this.inputs[index] : this.outputs[index - CrafterRecipe.MAX_FLUIDS];
    }

    /** Ce que voient tuyaux et mods tiers, par n'importe quelle face des 18 blocs. */
    final IFluidHandler external = new IFluidHandler() {
        @Override
        public int getTanks() {
            return TANKS;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return tank(tank).getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank(tank).getCapacity();
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return tank < CrafterRecipe.MAX_FLUIDS && tank(tank).isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            for (FluidTank input : inputs) {
                if (!input.isFluidValid(resource)) continue;
                int filled = input.fill(resource, action);
                if (filled > 0) return filled;
            }
            return 0;
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return FluidStack.EMPTY;
            for (FluidTank output : outputs) {
                if (output.getFluid().isFluidEqual(resource)) return output.drain(resource, action);
            }
            return FluidStack.EMPTY;
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            for (FluidTank output : outputs) {
                if (!output.isEmpty()) return output.drain(maxDrain, action);
            }
            return FluidStack.EMPTY;
        }
    };

    // Persistance

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < TANKS; i++) tag.put("tank" + i, tank(i).writeToNBT(new CompoundTag()));
        return tag;
    }

    void load(CompoundTag tag) {
        for (int i = 0; i < TANKS; i++) {
            FluidTank tank = tank(i);
            // La capacité n'est pas encore connue (la recette se résout au premier tick) : le
            // contenu est rétabli tel quel, sans passer par fill qui le tronquerait.
            tank.setFluid(FluidStack.loadFluidStackFromNBT(tag.getCompound("tank" + i)));
        }
    }
}
