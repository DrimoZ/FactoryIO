package com.drimoz.factoryio.core.datagen.generator;

import com.drimoz.factoryio.content.crafter.CrafterRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Écrit une recette {@code factor_io:crafting} au format que lit
 * {@link com.drimoz.factoryio.content.crafter.CrafterRecipeSerializer}.
 *
 * <pre>{@code
 * CrafterRecipeBuilder.crafting(ModItems.IRON_GEAR_WHEEL.get(), 1, 0.5F)
 *         .requires(ModTags.Items.PLATES_IRON, 2)
 *         .save(writer, recipe("crafter/iron_gear_wheel"));
 * }</pre>
 */
public final class CrafterRecipeBuilder {

    private final JsonArray ingredients = new JsonArray();
    private final JsonArray results = new JsonArray();
    private final float time;
    private int minTier = 1;

    private CrafterRecipeBuilder(float time) {
        this.time = time;
    }

    /** @param time secondes de craft, avant la vitesse de la machine */
    public static CrafterRecipeBuilder crafting(ItemLike result, int count, float time) {
        return new CrafterRecipeBuilder(time).result(result, count, 1.0F);
    }

    public CrafterRecipeBuilder requires(TagKey<Item> tag, int count) {
        return requires(Ingredient.of(tag), count);
    }

    public CrafterRecipeBuilder requires(ItemLike item, int count) {
        return requires(Ingredient.of(item), count);
    }

    private CrafterRecipeBuilder requires(Ingredient ingredient, int count) {
        JsonObject input = new JsonObject();
        input.add("ingredient", ingredient.toJson());
        input.addProperty("count", count);
        this.ingredients.add(input);
        return this;
    }

    public CrafterRecipeBuilder result(ItemLike item, int count, float chance) {
        JsonObject output = new JsonObject();
        output.addProperty("item", ForgeRegistries.ITEMS.getKey(item.asItem()).toString());
        if (count != 1) output.addProperty("count", count);
        if (chance < 1.0F) output.addProperty("chance", chance);
        this.results.add(output);
        return this;
    }

    public CrafterRecipeBuilder minTier(int minTier) {
        this.minTier = minTier;
        return this;
    }

    public void save(Consumer<FinishedRecipe> writer, ResourceLocation id) {
        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject json) {
                json.add("ingredients", ingredients);
                json.add("results", results);
                json.addProperty("time", time);
                if (minTier > 1) json.addProperty("minTier", minTier);
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return CrafterRecipes.SERIALIZER.get();
            }

            @Override
            public @Nullable JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public @Nullable ResourceLocation getAdvancementId() {
                return null;
            }
        });
    }
}
