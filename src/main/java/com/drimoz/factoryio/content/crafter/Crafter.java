package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Definition;
import com.drimoz.factoryio.core.model.Translation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;

/**
 * Un palier de crafter, tel que le décrit sa définition. Voir docs/12 §4.1.
 *
 * <p>Le <b>palier</b> est structurel : il décide des recettes permises et d'un bloc, fixé au
 * lancement. Le reste se <b>règle</b> à chaud par datapack ({@link CrafterReloadListener}),
 * et le crafter le relit à chaque tick — un {@code /reload} s'applique aux machines posées.
 */
public class Crafter implements Definition {

    private final ResourceLocation id;
    private final int tier;
    private final int moduleSlots;
    private final Tuning defaults;
    private final Translation translation = new Translation();

    private Tuning tuning;
    private Supplier<Block> block;

    /**
     * @param craftingSpeed  multiplicateur du temps de recette, comme dans Factorio
     * @param energyPerTick  FE consommés par tick de travail ; rien à l'arrêt
     * @param inputCrafts    crafts d'avance qu'acceptent les entrées
     */
    public record Tuning(float craftingSpeed, int energyPerTick, int energyCapacity, int inputCrafts) {}

    /** @param moduleSlots slots de module, structurels comme le palier (FIO-127) */
    public Crafter(ResourceLocation id, int tier, int moduleSlots, Tuning tuning) {
        this.id = id;
        this.tier = tier;
        this.moduleSlots = moduleSlots;
        this.defaults = tuning;
        this.tuning = tuning;
    }

    // Interface (Définition)

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public Translation getTranslation() {
        return this.translation;
    }

    public int getTier() {
        return this.tier;
    }

    public int getModuleSlots() {
        return this.moduleSlots;
    }

    // Interface (Réglages)

    public Tuning getTuning() {
        return this.tuning;
    }

    public void setTuning(Tuning tuning) {
        this.tuning = tuning;
    }

    public void resetTuning() {
        this.tuning = this.defaults;
    }

    // Interface (Registre)

    public Supplier<Block> getBlock() {
        return this.block;
    }

    public void setBlock(Supplier<Block> block) {
        this.block = block;
    }

    @Override
    public String toString() {
        return "Crafter{" + this.id + ", tier=" + this.tier + ", modules=" + this.moduleSlots + ", " + this.tuning + "}";
    }

    public static ResourceLocation id(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, name);
    }
}
