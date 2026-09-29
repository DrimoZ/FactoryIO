package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

/**
 * Recettes du mod, générées (FIO-164).
 *
 * <p>Les recettes des inserters étaient écrites à la main, pendant que les tags sortaient du
 * datagen : deux systèmes pour un même contenu, et rien pour vérifier qu'une recette nomme un
 * item qui existe. Tout passe désormais par ici, et {@code runData} échoue sur un item inconnu.
 *
 * <p>Les ingrédients sont des <b>tags</b> dès qu'un tag Forge existe : une plaque de fer ou un
 * lingot de cuivre d'un autre mod doit convenir aussi bien que ceux de vanilla.
 */
public class ModRecipeGenerator extends RecipeProvider {

    public ModRecipeGenerator(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        inserters(writer);
    }

    // Inserters : une chaîne, chacun fabriqué à partir du précédent

    private void inserters(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("burner_inserter"))
                .pattern(" A ")
                .pattern("DED")
                .pattern("BCB")
                .define('A', Items.HOPPER)
                .define('B', Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                .define('C', Items.COBBLESTONE_WALL)
                .define('D', Tags.Items.INGOTS_COPPER)
                .define('E', Tags.Items.DUSTS_REDSTONE)
                .unlockedBy("has_hopper", has(Items.HOPPER))
                .save(writer, inserterRecipe("burner_inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("inserter"))
                .pattern(" R ")
                .pattern("IBI")
                .pattern(" C ")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('B', inserter("burner_inserter"))
                .define('C', Tags.Items.INGOTS_COPPER)
                .unlockedBy("has_burner_inserter", has(inserter("burner_inserter")))
                .save(writer, inserterRecipe("inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("long_handed_inserter"))
                .pattern(" I ")
                .pattern(" A ")
                .pattern(" I ")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('A', inserter("inserter"))
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("long_handed_inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("filter_inserter"))
                .pattern(" C ")
                .pattern("RAR")
                .pattern(" C ")
                .define('C', Items.COMPARATOR)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('A', inserter("inserter"))
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("filter_inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("fast_inserter"))
                .pattern(" B ")
                .pattern("IAI")
                .pattern(" B ")
                .define('B', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('A', inserter("inserter"))
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("fast_inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("stack_inserter"))
                .pattern(" H ")
                .pattern("IFI")
                .pattern(" H ")
                .define('H', Items.HOPPER)
                .define('I', Tags.Items.STORAGE_BLOCKS_IRON)
                .define('F', inserter("fast_inserter"))
                .unlockedBy("has_fast_inserter", has(inserter("fast_inserter")))
                .save(writer, inserterRecipe("stack_inserter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("stack_filter_inserter"))
                .pattern(" C ")
                .pattern("CSC")
                .pattern(" C ")
                .define('C', Items.COMPARATOR)
                .define('S', inserter("stack_inserter"))
                .unlockedBy("has_stack_inserter", has(inserter("stack_inserter")))
                .save(writer, inserterRecipe("stack_filter_inserter"));
    }

    // Inner work

    /** Un inserter livré avec le mod ; absent, c'est une erreur de génération, pas un cas à taire. */
    private static Item inserter(String name) {
        Inserter definition = InserterRegistry.getInstance().getInserterByName(name);
        if (definition == null) throw new IllegalStateException("Inserter introuvable pour sa recette : " + name);

        return definition.getItem().get();
    }

    /** Même identifiant que les anciennes recettes écrites à la main : un pack qui les surcharge continue de le faire. */
    private static ResourceLocation inserterRecipe(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, "crafting/inserters/" + name);
    }
}
