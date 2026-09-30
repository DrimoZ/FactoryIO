package com.drimoz.factoryio.content.crafter;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Une recette {@code factor_io:crafting} : des ingrédients comptés, un à quatre résultats
 * éventuellement aléatoires, un temps. Voir docs/12 §3.
 *
 * <p>Le crafter ne cherche jamais « la recette qui correspond au contenu » : le joueur la
 * choisit, et la machine la retrouve par son identifiant. {@link #matches} ne sert donc à
 * rien et répond non.
 *
 * <p>Une recette d'établi vanilla, quand l'option l'autorise, est convertie dans ce même
 * format ({@link CrafterRecipes#byId}) : le crafter n'a qu'un seul modèle à connaître.
 *
 * @param ticks durée d'un craft à la vitesse 1, en ticks
 */
public record CrafterRecipe(
        ResourceLocation id,
        List<Input> inputs,
        List<Output> outputs,
        int ticks,
        int minTier) implements Recipe<Container> {

    /** Un ingrédient et la quantité qu'en consomme un craft. */
    public record Input(Ingredient ingredient, int count) {}

    /** Un résultat, obtenu à chaque craft avec la probabilité {@code chance} ∈ ]0, 1]. */
    public record Output(ItemStack stack, float chance) {

        public boolean isCertain() {
            return this.chance >= 1.0F;
        }
    }

    public CrafterRecipe {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return getResultItem(access).copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    /** Le premier résultat : ce que montrent l'icône du sélecteur et les listes vanilla. */
    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return this.outputs.get(0).stack();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (Input input : this.inputs) ingredients.add(input.ingredient());
        return ingredients;
    }

    /**
     * Vrai pour que le livre de recettes du client l'ignore : il ne connaît pas cette
     * catégorie et le dirait dans le journal pour chaque recette.
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CrafterRecipes.SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CrafterRecipes.TYPE.get();
    }
}
