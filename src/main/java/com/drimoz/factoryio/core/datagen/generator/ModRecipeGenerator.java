package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltDefaults;
import com.drimoz.factoryio.core.registry.BeltRegistry;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
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
 *
 * <h2>La chaîne des composants</h2>
 *
 * <p>Celle de Factorio, ramenée à la grille 3×3 et aux matériaux de Minecraft : les plaques
 * sortent du lingot au tailleur de pierre, l'acier du haut fourneau ; le plastique devient de
 * l'algue séchée et l'acide sulfurique de la poudre à canon, faute de chimie avant la Phase 4.
 * Seul ce qui sert a une recette — les autres intermédiaires attendent les machines.
 */
public class ModRecipeGenerator extends RecipeProvider {

    public ModRecipeGenerator(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        inserters(writer);
        belts(writer);
        materials(writer);
        circuits(writer);
        modules(writer);
        tools(writer);
    }

    // Matériaux : plaques, engrenage, câble

    private void materials(Consumer<FinishedRecipe> writer) {
        // Le lingot est déjà la plaque de Minecraft : un passage au tailleur de pierre, un pour
        // un, sans perte — l'outil vanilla de la découpe.
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(Tags.Items.INGOTS_IRON), RecipeCategory.MISC, ModItems.IRON_PLATE.get())
                .unlockedBy("has_iron_ingot", has(Tags.Items.INGOTS_IRON))
                .save(writer, recipe("stonecutting/iron_plate"));

        SingleItemRecipeBuilder.stonecutting(Ingredient.of(Tags.Items.INGOTS_COPPER), RecipeCategory.MISC, ModItems.COPPER_PLATE.get())
                .unlockedBy("has_copper_ingot", has(Tags.Items.INGOTS_COPPER))
                .save(writer, recipe("stonecutting/copper_plate"));

        // La seconde fonte de Factorio, au haut fourneau.
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(ModTags.Items.PLATES_IRON), RecipeCategory.MISC,
                        ModItems.STEEL_PLATE.get(), 0.3F, 200)
                .unlockedBy("has_iron_plate", has(ModTags.Items.PLATES_IRON))
                .save(writer, recipe("blasting/steel_plate"));

        // Factorio : 2 plaques de fer → 1 engrenage.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.IRON_GEAR_WHEEL.get())
                .pattern("P")
                .pattern("P")
                .define('P', ModTags.Items.PLATES_IRON)
                .unlockedBy("has_iron_plate", has(ModTags.Items.PLATES_IRON))
                .save(writer, recipe("crafting/components/iron_gear_wheel"));

        // Factorio : 1 plaque de cuivre → 2 câbles.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.COPPER_CABLE.get(), 2)
                .requires(ModTags.Items.PLATES_COPPER)
                .unlockedBy("has_copper_plate", has(ModTags.Items.PLATES_COPPER))
                .save(writer, recipe("crafting/components/copper_cable"));
    }

    // Circuits

    private void circuits(Consumer<FinishedRecipe> writer) {
        // Factorio : 1 plaque de fer + 3 câbles.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ELECTRONIC_CIRCUIT.get())
                .pattern("CCC")
                .pattern(" P ")
                .define('C', ModTags.Items.WIRES_COPPER)
                .define('P', ModTags.Items.PLATES_IRON)
                .unlockedBy("has_copper_cable", has(ModTags.Items.WIRES_COPPER))
                .save(writer, recipe("crafting/components/electronic_circuit"));

        // Factorio : 2 circuits électroniques + 2 plastiques + 4 câbles.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ADVANCED_CIRCUIT.get())
                .pattern("CKC")
                .pattern("E E")
                .pattern("CKC")
                .define('C', ModTags.Items.WIRES_COPPER)
                .define('K', Items.DRIED_KELP)
                .define('E', ModTags.Items.CIRCUITS_BASIC)
                .unlockedBy("has_electronic_circuit", has(ModTags.Items.CIRCUITS_BASIC))
                .save(writer, recipe("crafting/components/advanced_circuit"));

        // Factorio : 20 électroniques + 2 avancés + acide sulfurique, ramenés à la grille.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PROCESSING_UNIT.get())
                .pattern("EAE")
                .pattern("EGE")
                .pattern(" A ")
                .define('E', ModTags.Items.CIRCUITS_BASIC)
                .define('A', ModTags.Items.CIRCUITS_ADVANCED)
                .define('G', Tags.Items.GUNPOWDER)
                .unlockedBy("has_advanced_circuit", has(ModTags.Items.CIRCUITS_ADVANCED))
                .save(writer, recipe("crafting/components/processing_unit"));
    }

    // Modules

    /**
     * Les trois familles ont la même recette dans Factorio ; une table de craft ne sait pas
     * départager trois recettes identiques. Un ingrédient central les distingue, choisi pour que
     * l'effet se devine : ce qui rend plus vif, ce qui pousse davantage, ce qui dépense moins.
     */
    private void modules(Consumer<FinishedRecipe> writer) {
        moduleLine(writer, "speed",
                ModItems.SPEED_MODULE_1.get(), ModItems.SPEED_MODULE_2.get(), ModItems.SPEED_MODULE_3.get(),
                Ingredient.of(Items.SUGAR), Ingredient.of(Items.RABBIT_FOOT), Ingredient.of(Items.PHANTOM_MEMBRANE));

        moduleLine(writer, "productivity",
                ModItems.PRODUCTIVITY_MODULE_1.get(), ModItems.PRODUCTIVITY_MODULE_2.get(), ModItems.PRODUCTIVITY_MODULE_3.get(),
                Ingredient.of(Items.PISTON), Ingredient.of(Items.STICKY_PISTON), Ingredient.of(Items.SHULKER_SHELL));

        moduleLine(writer, "efficiency",
                ModItems.EFFICIENCY_MODULE_1.get(), ModItems.EFFICIENCY_MODULE_2.get(), ModItems.EFFICIENCY_MODULE_3.get(),
                Ingredient.of(Tags.Items.GEMS_LAPIS), Ingredient.of(Tags.Items.GEMS_AMETHYST), Ingredient.of(Items.ECHO_SHARD));

        // Sans équivalent dans Factorio, où la condition de circuit est gratuite : un module
        // au prix d'électroniques seulement, autour d'un comparateur.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ADVANCED_REDSTONE_MODULE.get())
                .pattern("ERE")
                .pattern("RCR")
                .pattern("ERE")
                .define('E', ModTags.Items.CIRCUITS_BASIC)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', Items.COMPARATOR)
                .unlockedBy("has_basic_circuit", has(ModTags.Items.CIRCUITS_BASIC))
                .save(writer, recipe("crafting/modules/advanced_redstone_module"));
    }

    /**
     * Palier 1 : 4 électroniques et 4 avancés autour de l'ingrédient central (Factorio : 5 + 5).
     * Paliers suivants : 2 modules du palier précédent, 3 unités de traitement, 3 avancés
     * (Factorio : 4 modules, 5 + 5) — la montée coûte, sans devenir une corvée à la main.
     */
    private void moduleLine(Consumer<FinishedRecipe> writer, String family,
                            Item tier1, Item tier2, Item tier3,
                            Ingredient core1, Ingredient core2, Ingredient core3) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, tier1)
                .pattern("EAE")
                .pattern("AXA")
                .pattern("EAE")
                .define('E', ModTags.Items.CIRCUITS_BASIC)
                .define('A', ModTags.Items.CIRCUITS_ADVANCED)
                .define('X', core1)
                .unlockedBy("has_advanced_circuit", has(ModTags.Items.CIRCUITS_ADVANCED))
                .save(writer, recipe("crafting/modules/" + family + "_module"));

        upgrade(writer, tier2, tier1, core2, family + "_module_2");
        upgrade(writer, tier3, tier2, core3, family + "_module_3");
    }

    private void upgrade(Consumer<FinishedRecipe> writer, Item result, Item previous, Ingredient core, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("PAP")
                .pattern("MXM")
                .pattern("APA")
                .define('P', ModTags.Items.CIRCUITS_ELITE)
                .define('A', ModTags.Items.CIRCUITS_ADVANCED)
                .define('M', previous)
                .define('X', core)
                .unlockedBy("has_previous_tier", has(previous))
                .save(writer, recipe("crafting/modules/" + name));
    }

    // Outils

    /** Bon marché : un outil de confort, qui sert dès les premiers inserters. */
    private void tools(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.CONFIGURATOR.get())
                .pattern(" R ")
                .pattern("CPC")
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .define('C', ModTags.Items.WIRES_COPPER)
                .define('P', ModTags.Items.PLATES_IRON)
                .unlockedBy("has_copper_cable", has(ModTags.Items.WIRES_COPPER))
                .save(writer, recipe("crafting/tools/configurator"));
    }

    // Inserters

    /**
     * Les recettes de Factorio 1.1, ramenées à la grille : engrenages, plaques et circuits, plus
     * de pièces vanilla (entonnoir, comparateur, blocs de redstone). Deux écarts :
     *
     * <ul>
     *   <li>le filtrant se fait à partir de l'inserter simple, pas du rapide : ici il travaille à
     *       la vitesse de l'inserter simple, et le tirer d'un rapide ferait payer une vitesse
     *       perdue ;</li>
     *   <li>les quantités de l'empilable (15 engrenages, 15 circuits) sont ramenées à ce qui tient
     *       dans une grille, en gardant l'ordre de grandeur relatif.</li>
     * </ul>
     */
    private void inserters(Consumer<FinishedRecipe> writer) {
        // Factorio : 1 plaque + 1 engrenage.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("burner_inserter"))
                .pattern("G")
                .pattern("P")
                .define('G', ModTags.Items.GEARS_IRON)
                .define('P', ModTags.Items.PLATES_IRON)
                .unlockedBy("has_iron_gear", has(ModTags.Items.GEARS_IRON))
                .save(writer, inserterRecipe("burner_inserter"));

        // Factorio : 1 circuit électronique + 1 engrenage + 1 plaque.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("inserter"))
                .pattern("C")
                .pattern("G")
                .pattern("P")
                .define('C', ModTags.Items.CIRCUITS_BASIC)
                .define('G', ModTags.Items.GEARS_IRON)
                .define('P', ModTags.Items.PLATES_IRON)
                .unlockedBy("has_electronic_circuit", has(ModTags.Items.CIRCUITS_BASIC))
                .save(writer, inserterRecipe("inserter"));

        // Factorio : 1 inserter + 1 engrenage + 1 plaque.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("long_handed_inserter"))
                .pattern("P")
                .pattern("I")
                .pattern("G")
                .define('P', ModTags.Items.PLATES_IRON)
                .define('I', inserter("inserter"))
                .define('G', ModTags.Items.GEARS_IRON)
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("long_handed_inserter"));

        // Factorio : 1 inserter + 2 circuits électroniques + 2 plaques.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("fast_inserter"))
                .pattern(" C ")
                .pattern("PIP")
                .pattern(" C ")
                .define('C', ModTags.Items.CIRCUITS_BASIC)
                .define('P', ModTags.Items.PLATES_IRON)
                .define('I', inserter("inserter"))
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("fast_inserter"));

        // Factorio : 1 rapide + 4 circuits — ici l'inserter simple, voir plus haut.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("filter_inserter"))
                .pattern(" C ")
                .pattern("CIC")
                .pattern(" C ")
                .define('C', ModTags.Items.CIRCUITS_BASIC)
                .define('I', inserter("inserter"))
                .unlockedBy("has_inserter", has(inserter("inserter")))
                .save(writer, inserterRecipe("filter_inserter"));

        // Factorio : 1 rapide + 15 engrenages + 15 circuits + 1 circuit avancé.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("stack_inserter"))
                .pattern("GCG")
                .pattern("CFC")
                .pattern("GAG")
                .define('G', ModTags.Items.GEARS_IRON)
                .define('C', ModTags.Items.CIRCUITS_BASIC)
                .define('F', inserter("fast_inserter"))
                .define('A', ModTags.Items.CIRCUITS_ADVANCED)
                .unlockedBy("has_fast_inserter", has(inserter("fast_inserter")))
                .save(writer, inserterRecipe("stack_inserter"));

        // Factorio : 1 empilable + 5 circuits.
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, inserter("stack_filter_inserter"))
                .pattern(" C ")
                .pattern("CSC")
                .pattern(" C ")
                .define('C', ModTags.Items.CIRCUITS_BASIC)
                .define('S', inserter("stack_inserter"))
                .unlockedBy("has_stack_inserter", has(inserter("stack_inserter")))
                .save(writer, inserterRecipe("stack_filter_inserter"));
    }

    // Convoyeurs

    /**
     * Factorio : 1 plaque + 1 engrenage → 2 convoyeurs ; le rapide ajoute 5 engrenages ; l'express
     * 10 engrenages et du lubrifiant, que la boule de slime remplace faute de chimie.
     */
    private void belts(Consumer<FinishedRecipe> writer) {
        Item transport = BeltRegistry.block(BeltDefaults.TRANSPORT).asItem();
        Item fast = BeltRegistry.block(BeltDefaults.FAST).asItem();
        Item express = BeltRegistry.block(BeltDefaults.EXPRESS).asItem();

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, transport, 4)
                .pattern("PGP")
                .define('P', ModTags.Items.PLATES_IRON)
                .define('G', ModTags.Items.GEARS_IRON)
                .unlockedBy("has_iron_gear", has(ModTags.Items.GEARS_IRON))
                .save(writer, recipe("crafting/belts/transport_belt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, fast)
                .pattern("GGG")
                .pattern("GBG")
                .define('G', ModTags.Items.GEARS_IRON)
                .define('B', transport)
                .unlockedBy("has_transport_belt", has(transport))
                .save(writer, recipe("crafting/belts/fast_transport_belt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, express)
                .pattern("GSG")
                .pattern("GBG")
                .pattern(" S ")
                .define('G', ModTags.Items.GEARS_IRON)
                .define('S', Tags.Items.SLIMEBALLS)
                .define('B', fast)
                .unlockedBy("has_fast_transport_belt", has(fast))
                .save(writer, recipe("crafting/belts/express_transport_belt"));
    }

    // Inner work

    /** Un inserter livré avec le mod ; absent, c'est une erreur de génération, pas un cas à taire. */
    private static Item inserter(String name) {
        Inserter definition = InserterRegistry.getInstance().getInserterByName(name);
        if (definition == null) throw new IllegalStateException("Inserter introuvable pour sa recette : " + name);

        return definition.getItem().get();
    }

    private static ResourceLocation recipe(String path) {
        return new ResourceLocation(FactoryIO.MOD_ID, path);
    }

    /** Même identifiant que les anciennes recettes écrites à la main : un pack qui les surcharge continue de le faire. */
    private static ResourceLocation inserterRecipe(String name) {
        return new ResourceLocation(FactoryIO.MOD_ID, "crafting/inserters/" + name);
    }
}
