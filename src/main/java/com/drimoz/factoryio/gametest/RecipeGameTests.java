package com.drimoz.factoryio.gametest;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.belts.BeltLane;
import com.drimoz.factoryio.core.model.Belt;
import com.drimoz.factoryio.core.model.BeltDefaults;
import com.drimoz.factoryio.core.registry.BeltRegistry;
import com.drimoz.factoryio.core.init.ModBlocks;
import com.drimoz.factoryio.core.init.ModItems;
import com.drimoz.factoryio.core.init.ModTags;
import com.drimoz.factoryio.core.registry.InserterRegistry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * La chaîne de fabrication est complète (FIO-164).
 *
 * <p>Une recette invalide — un tag mal orthographié, un item renommé — n'arrête pas le jeu :
 * elle est journalisée au chargement et simplement absente. Personne ne le voit avant de
 * chercher à fabriquer l'item. Ce test la cherche à sa place, dans le gestionnaire de recettes
 * réellement chargé, et vérifie aussi que les tags qui relient les étapes ne sont pas vides.
 */
@GameTestHolder(FactoryIO.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeGameTests {

    private static final String TEMPLATE = "empty";

    @GameTest(template = TEMPLATE)
    public static void everyUsefulItemCanBeCrafted(GameTestHelper helper) {
        List<Item> useful = new ArrayList<>();
        InserterRegistry.getInstance().getInserters().forEach(inserter -> useful.add(inserter.getItem().get()));
        for (Belt belt : BeltDefaults.all()) useful.add(BeltRegistry.block(belt.getId()).asItem());

        for (RegistryObject<Item> item : List.of(
                ModItems.IRON_PLATE, ModItems.COPPER_PLATE, ModItems.STEEL_PLATE,
                ModItems.IRON_GEAR_WHEEL, ModItems.COPPER_CABLE,
                ModItems.ELECTRONIC_CIRCUIT, ModItems.ADVANCED_CIRCUIT, ModItems.PROCESSING_UNIT,
                ModItems.SPEED_MODULE_1, ModItems.SPEED_MODULE_2, ModItems.SPEED_MODULE_3,
                ModItems.PRODUCTIVITY_MODULE_1, ModItems.PRODUCTIVITY_MODULE_2, ModItems.PRODUCTIVITY_MODULE_3,
                ModItems.EFFICIENCY_MODULE_1, ModItems.EFFICIENCY_MODULE_2, ModItems.EFFICIENCY_MODULE_3,
                ModItems.ADVANCED_REDSTONE_MODULE,
                ModItems.CONFIGURATOR)) {
            useful.add(item.get());
        }

        RegistryAccess access = helper.getLevel().registryAccess();
        Collection<Recipe<?>> recipes = helper.getLevel().getRecipeManager().getRecipes();

        List<String> missing = new ArrayList<>();
        for (Item item : useful) {
            boolean craftable = recipes.stream().anyMatch(recipe -> recipe.getResultItem(access).is(item));
            if (!craftable) missing.add(item.toString());
        }

        helper.assertTrue(missing.isEmpty(), "Aucune recette ne produit : " + missing);
        helper.succeed();
    }

    /** Les tags qui relient les étapes de la chaîne contiennent bien nos items. */
    @GameTest(template = TEMPLATE)
    public static void chainTagsAreFilled(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(ModItems.IRON_PLATE.get()).is(ModTags.Items.PLATES_IRON), "forge:plates/iron");
        helper.assertTrue(new ItemStack(ModItems.COPPER_PLATE.get()).is(ModTags.Items.PLATES_COPPER), "forge:plates/copper");
        helper.assertTrue(new ItemStack(ModItems.IRON_GEAR_WHEEL.get()).is(ModTags.Items.GEARS_IRON), "forge:gears/iron");
        helper.assertTrue(new ItemStack(ModItems.COPPER_CABLE.get()).is(ModTags.Items.WIRES_COPPER), "forge:wires/copper");
        helper.assertTrue(new ItemStack(ModItems.ELECTRONIC_CIRCUIT.get()).is(ModTags.Items.CIRCUITS_BASIC), "forge:circuits/basic");
        helper.assertTrue(new ItemStack(ModItems.ADVANCED_CIRCUIT.get()).is(ModTags.Items.CIRCUITS_ADVANCED), "forge:circuits/advanced");
        helper.assertTrue(new ItemStack(ModItems.PROCESSING_UNIT.get()).is(ModTags.Items.CIRCUITS_ELITE), "forge:circuits/elite");
        helper.assertTrue(new ItemStack(ModItems.PROCESSING_UNIT.get()).is(ModTags.Items.CIRCUITS), "forge:circuits");
        helper.succeed();
    }

    /** Les intermédiaires sans usage n'ont pas de recette : ils attendent les machines. */
    @GameTest(template = TEMPLATE)
    public static void hiddenItemsHaveNoRecipe(GameTestHelper helper) {
        RegistryAccess access = helper.getLevel().registryAccess();
        Collection<Recipe<?>> recipes = helper.getLevel().getRecipeManager().getRecipes();

        for (RegistryObject<Item> hidden : ModItems.HIDDEN) {
            boolean craftable = recipes.stream().anyMatch(recipe -> recipe.getResultItem(access).is(hidden.get()));
            helper.assertFalse(craftable, hidden.getId() + " est caché mais fabricable");
        }
        helper.succeed();
    }
}
