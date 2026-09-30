package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.configs.ServerConfig;
import com.drimoz.factoryio.core.init.ModRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Le type {@code factor_io:crafting}, et la seule porte d'accès aux recettes du crafter.
 *
 * <p>Passer par ici plutôt que par {@code RecipeManager} directement, c'est avoir les
 * recettes d'établi vanilla quand l'option serveur les autorise (docs/12 §3.4), déjà
 * converties au format du crafter.
 */
@Mod.EventBusSubscriber(modid = FactoryIO.MOD_ID)
public final class CrafterRecipes {

    public static final RegistryObject<RecipeType<CrafterRecipe>> TYPE = ModRegistries.RECIPE_TYPES.register(
            "crafting", () -> RecipeType.simple(new ResourceLocation(FactoryIO.MOD_ID, "crafting")));

    public static final RegistryObject<CrafterRecipeSerializer> SERIALIZER = ModRegistries.RECIPE_SERIALIZERS.register(
            "crafting", CrafterRecipeSerializer::new);

    /**
     * Change à chaque rechargement des recettes ou de l'option vanilla.
     *
     * <p>Un crafter garde sa recette résolue et ne la recherche que si ce compteur a bougé :
     * c'est une comparaison d'entiers au tick, pas une recherche dans une table.
     */
    private static volatile int generation;

    private CrafterRecipes() {}

    /** Force l'initialisation statique, donc les register(). */
    public static void init() {}

    public static int generation() {
        return generation;
    }

    public static void invalidate() {
        generation++;
    }

    /** Après un {@code /reload} : {@code getPlayer()} est nul quand tous les joueurs sont servis. */
    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) invalidate();
    }

    // Interface

    /** Toutes les recettes du crafter : celles du mod d'abord, puis les vanilla si permises. */
    public static List<CrafterRecipe> all(Level level) {
        RecipeManager manager = level.getRecipeManager();

        List<CrafterRecipe> recipes = new ArrayList<>(manager.getAllRecipesFor(TYPE.get()));
        recipes.sort(Comparator.comparing(recipe -> recipe.getId().toString()));

        if (vanillaAllowed()) {
            List<CrafterRecipe> vanilla = new ArrayList<>();
            for (CraftingRecipe recipe : manager.getAllRecipesFor(RecipeType.CRAFTING)) {
                CrafterRecipe converted = fromVanilla(recipe, level);
                if (converted != null) vanilla.add(converted);
            }
            vanilla.sort(Comparator.comparing(recipe -> recipe.getId().toString()));
            recipes.addAll(vanilla);
        }
        return recipes;
    }

    /** La recette choisie, si elle existe encore et reste permise. */
    public static Optional<CrafterRecipe> byId(Level level, ResourceLocation id) {
        Optional<? extends Recipe<?>> recipe = level.getRecipeManager().byKey(id);
        if (recipe.isEmpty()) return Optional.empty();

        if (recipe.get() instanceof CrafterRecipe crafter) return Optional.of(crafter);
        if (recipe.get() instanceof CraftingRecipe crafting && vanillaAllowed()) {
            return Optional.ofNullable(fromVanilla(crafting, level));
        }
        return Optional.empty();
    }

    // Inner work

    /** Lu à la demande : la valeur n'existe qu'une fois un monde chargé. */
    private static boolean vanillaAllowed() {
        return ServerConfig.SPEC.isLoaded() && ServerConfig.CRAFTER_VANILLA_RECIPES.get();
    }

    /**
     * Une recette d'établi au format du crafter, ou {@code null} si elle n'y a pas sa place.
     *
     * <p>Seules les recettes à forme ou sans forme <b>ordinaires</b> passent : les spéciales
     * (feu d'artifice, teinture, copie de carte) calculent leur résultat à partir du NBT des
     * entrées, que le crafter ne regarde pas. Les ingrédients identiques sont regroupés —
     * huit planches, pas huit fois une planche.
     */
    @Nullable
    public static CrafterRecipe fromVanilla(CraftingRecipe recipe, Level level) {
        if (recipe.isSpecial() || !(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe)) return null;

        ItemStack result = recipe.getResultItem(level.registryAccess());
        if (result.isEmpty()) return null;

        Map<String, CrafterRecipe.Input> merged = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient.isEmpty()) continue;

            merged.merge(ingredient.toJson().toString(), new CrafterRecipe.Input(ingredient, 1),
                    (a, b) -> new CrafterRecipe.Input(a.ingredient(), a.count() + 1));
        }
        if (merged.isEmpty()) return null;

        int ticks = CrafterRecipeSerializer.toTicks(ServerConfig.CRAFTER_VANILLA_TIME.get());
        return new CrafterRecipe(recipe.getId(), new ArrayList<>(merged.values()),
                List.of(new CrafterRecipe.Output(result.copy(), 1.0F)), ticks, 1);
    }
}
