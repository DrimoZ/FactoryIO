package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.content.crafter.CrafterRecipe;
import com.drimoz.factoryio.content.crafter.CrafterRecipeSerializer;
import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.drimoz.factoryio.core.init.ModItems;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

/**
 * Le format {@code factor_io:crafting} (FIO-120). En GameTest et non en JUnit : lire un
 * ingrédient demande les registres du jeu.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class CrafterRecipeGameTests {

    private static final String TEMPLATE = "empty";
    private static final ResourceLocation ID = new ResourceLocation(FactoryIO.MOD_ID, "test");

    private static final String VALID = """
            {
              "type": "factor_io:crafting",
              "ingredients": [
                { "ingredient": { "tag": "forge:ingots/iron" }, "count": 20 },
                { "ingredient": { "item": "minecraft:redstone" } }
              ],
              "results": [
                { "item": "minecraft:comparator", "count": 2 },
                { "item": "minecraft:glowstone_dust", "chance": 0.1 }
              ],
              "time": 2.5,
              "minTier": 2
            }""";

    /** Une recette complète se lit, et traverse le réseau sans rien perdre. */
    @GameTest(template = TEMPLATE)
    public static void aValidRecipeReadsAndSurvivesTheNetwork(GameTestHelper helper) {
        CrafterRecipe recipe = CrafterRecipes.SERIALIZER.get().fromJson(ID, json(VALID));

        helper.assertTrue(recipe.inputs().size() == 2 && recipe.inputs().get(0).count() == 20
                && recipe.inputs().get(1).count() == 1, "Ingrédients mal lus");
        helper.assertTrue(recipe.outputs().get(0).stack().is(Items.COMPARATOR) && recipe.outputs().get(0).stack().getCount() == 2
                && recipe.outputs().get(0).isCertain(), "Premier résultat mal lu");
        helper.assertTrue(recipe.outputs().get(1).chance() == 0.1F, "Probabilité mal lue");
        helper.assertTrue(recipe.ticks() == 50 && recipe.minTier() == 2, "Temps ou palier mal lu");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        CrafterRecipes.SERIALIZER.get().toNetwork(buf, recipe);
        CrafterRecipe received = CrafterRecipes.SERIALIZER.get().fromNetwork(ID, buf);

        helper.assertTrue(received != null && received.ticks() == 50 && received.minTier() == 2
                && received.inputs().get(0).count() == 20
                && received.inputs().get(0).ingredient().test(new ItemStack(Items.IRON_INGOT))
                && received.outputs().get(1).chance() == 0.1F
                && ItemStack.matches(received.outputs().get(0).stack(), recipe.outputs().get(0).stack()),
                "La recette reçue par le client diffère");
        helper.succeed();
    }

    /**
     * Chaque valeur hors bornes est refusée, jamais ramenée en silence — et par une
     * exception que {@code RecipeManager} sait rattraper, sans quoi un {@code /reload} entier
     * échouerait pour une recette.
     */
    @GameTest(template = TEMPLATE)
    public static void outOfBoundsValuesAreRefused(GameTestHelper helper) {
        List<String[]> cases = List.of(
                new String[]{"\"count\": 20", "\"count\": 0"},
                new String[]{"\"count\": 20", "\"count\": 65"},
                new String[]{"\"chance\": 0.1", "\"chance\": 0"},
                new String[]{"\"chance\": 0.1", "\"chance\": 1.5"},
                new String[]{"\"time\": 2.5", "\"time\": 0"},
                new String[]{"\"time\": 2.5", "\"time\": 4000"},
                new String[]{"\"minTier\": 2", "\"minTier\": 0"},
                new String[]{"minecraft:comparator", "minecraft:no_such_item"},
                new String[]{"minecraft:comparator", "minecraft:air"},
                new String[]{"\"minTier\": 2", "\"minTier\": 2, \"fluidInputs\": []"},
                new String[]{"\"results\": [", "\"results\": [ ], \"unused\": ["});

        for (String[] replacement : cases) {
            String broken = VALID.replace(replacement[0], replacement[1]);
            helper.assertTrue(!broken.equals(VALID), "Cas de test sans effet : " + replacement[1]);

            try {
                CrafterRecipes.SERIALIZER.get().fromJson(ID, json(broken));
                helper.fail("Accepté à tort : " + replacement[1]);
            } catch (JsonParseException | IllegalArgumentException expected) {
                // Le refus attendu, sous une forme que RecipeManager journalise.
            }
        }
        helper.succeed();
    }

    /**
     * Une recette d'établi convertie regroupe ses ingrédients ; une recette spéciale, dont
     * le résultat dépend du NBT, n'a pas de place dans le crafter.
     */
    @GameTest(template = TEMPLATE)
    public static void vanillaRecipesConvertOnlyWhenOrdinary(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();

        CraftingRecipe chest = (CraftingRecipe) manager.byKey(new ResourceLocation("chest")).orElseThrow();
        CrafterRecipe converted = CrafterRecipes.fromVanilla(chest, helper.getLevel());
        helper.assertTrue(converted != null && converted.inputs().size() == 1 && converted.inputs().get(0).count() == 8
                        && converted.outputs().get(0).stack().is(Items.CHEST),
                "Le coffre devrait demander 8 planches d'un seul ingrédient");

        CraftingRecipe firework = (CraftingRecipe) manager.byKey(new ResourceLocation("firework_rocket")).orElseThrow();
        helper.assertTrue(CrafterRecipes.fromVanilla(firework, helper.getLevel()) == null,
                "Une recette spéciale ne doit pas passer");

        // Option à false par défaut : aucune recette vanilla ne se retrouve par son identifiant.
        helper.assertTrue(CrafterRecipes.byId(helper.getLevel(), chest.getId()).isEmpty(),
                "Les recettes vanilla sont désactivées par défaut");
        helper.assertTrue(manager.getAllRecipesFor(RecipeType.CRAFTING).size() > 0, "Pas de recettes vanilla chargées");
        helper.succeed();
    }

    /**
     * Circuit avancé et processeur ne se font qu'au crafter (FIO-126, docs/12 §6), et le
     * processeur demande un palier 2.
     */
    @GameTest(template = TEMPLATE)
    public static void advancedCircuitsOnlyComeFromTheCrafter(GameTestHelper helper) {
        var level = helper.getLevel();
        var manager = level.getRecipeManager();

        for (var item : List.of(ModItems.ADVANCED_CIRCUIT.get(), ModItems.PROCESSING_UNIT.get())) {
            helper.assertTrue(manager.getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .noneMatch(recipe -> recipe.getResultItem(level.registryAccess()).is(item)), item + " se fait encore à l'établi");
            helper.assertTrue(manager.getAllRecipesFor(CrafterRecipes.TYPE.get()).stream()
                    .anyMatch(recipe -> recipe.getResultItem(level.registryAccess()).is(item)), item + " n'a pas de recette de crafter");
        }

        CrafterRecipe processingUnit = CrafterRecipes.byId(level, new ResourceLocation(FactoryIO.MOD_ID, "crafter/processing_unit"))
                .orElseThrow();
        helper.assertTrue(processingUnit.minTier() == 2 && processingUnit.inputs().get(0).count() == 20,
                "Le processeur n'a pas les quantités ni le palier de Factorio");
        helper.succeed();
    }

    private static JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
