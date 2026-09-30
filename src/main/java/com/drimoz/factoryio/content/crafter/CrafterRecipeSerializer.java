package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.model.StrictCodecs;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lecture d'une recette {@code factor_io:crafting}, en datapack et sur le réseau.
 *
 * <pre>{@code
 * {
 *   "type": "factor_io:crafting",
 *   "ingredients": [ { "ingredient": { "tag": "forge:circuits/basic" }, "count": 20 } ],
 *   "results": [ { "item": "factor_io:processing_unit" },
 *                { "item": "minecraft:glowstone_dust", "chance": 0.1 } ],
 *   "time": 10.0,
 *   "minTier": 2
 * }
 * }</pre>
 *
 * <p>Codec borné : une valeur hors bornes fait échouer la lecture avec un message qui la
 * nomme, et {@code RecipeManager} journalise l'échec — jamais de valeur ramenée en silence.
 * Un champ {@code fluid…} est refusé pour la même raison : l'ignorer donnerait une recette
 * sans son fluide (docs/12 §3.1, FIO-179).
 */
public class CrafterRecipeSerializer implements RecipeSerializer<CrafterRecipe> {

    public static final int MAX_INPUTS = 9;
    public static final int MAX_OUTPUTS = 4;
    public static final int MAX_COUNT = 64;
    /** Une heure : au-delà, c'est une faute de frappe, pas un réglage. */
    public static final float MAX_TIME = 3600.0F;
    public static final int MAX_TIER = 16;

    private static final Codec<Ingredient> INGREDIENT = Codec.PASSTHROUGH.comapFlatMap(
            dynamic -> {
                try {
                    return DataResult.success(CraftingHelper.getIngredient(
                            dynamic.convert(JsonOps.INSTANCE).getValue(), false));
                } catch (JsonParseException e) {
                    return DataResult.error(e::getMessage);
                }
            },
            ingredient -> new Dynamic<>(JsonOps.INSTANCE, ingredient.toJson()));

    private static final Codec<Item> ITEM = ForgeRegistries.ITEMS.getCodec().flatXmap(
            item -> item == Items.AIR ? DataResult.error(() -> "l'air n'est pas un résultat") : DataResult.success(item),
            DataResult::success);

    private static final Codec<Float> CHANCE = Codec.FLOAT.flatXmap(
            chance -> chance > 0.0F && chance <= 1.0F
                    ? DataResult.success(chance)
                    : DataResult.error(() -> "« chance » doit être dans ]0, 1] : " + chance),
            DataResult::success);

    private static final Codec<Float> TIME = Codec.FLOAT.flatXmap(
            time -> time > 0.0F && time <= MAX_TIME
                    ? DataResult.success(time)
                    : DataResult.error(() -> "« time » doit être dans ]0, " + MAX_TIME + "] : " + time),
            DataResult::success);

    private static final Codec<CrafterRecipe.Input> INPUT = RecordCodecBuilder.create(instance -> instance.group(
            INGREDIENT.fieldOf("ingredient").forGetter(CrafterRecipe.Input::ingredient),
            StrictCodecs.optional(Codec.intRange(1, MAX_COUNT), "count", 1).forGetter(CrafterRecipe.Input::count)
    ).apply(instance, CrafterRecipe.Input::new));

    private static final Codec<CrafterRecipe.Output> OUTPUT = RecordCodecBuilder.create(instance -> instance.group(
            ITEM.fieldOf("item").forGetter(output -> output.stack().getItem()),
            StrictCodecs.optional(Codec.intRange(1, MAX_COUNT), "count", 1).forGetter(output -> output.stack().getCount()),
            StrictCodecs.optional(CHANCE, "chance", 1.0F).forGetter(CrafterRecipe.Output::chance)
    ).apply(instance, (item, count, chance) -> new CrafterRecipe.Output(new ItemStack(item, count), chance)));

    private record Fields(List<CrafterRecipe.Input> inputs, List<CrafterRecipe.Output> outputs, float time, int minTier) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                sized(INPUT, MAX_INPUTS, "ingredients").fieldOf("ingredients").forGetter(Fields::inputs),
                sized(OUTPUT, MAX_OUTPUTS, "results").fieldOf("results").forGetter(Fields::outputs),
                TIME.fieldOf("time").forGetter(Fields::time),
                StrictCodecs.optional(Codec.intRange(1, MAX_TIER), "minTier", 1).forGetter(Fields::minTier)
        ).apply(instance, Fields::new));
    }

    // Interface (Datapack)

    @Override
    public CrafterRecipe fromJson(ResourceLocation id, JsonObject json) {
        for (String key : json.keySet()) {
            if (key.startsWith("fluid")) {
                throw new JsonSyntaxException("« " + key + " » : les fluides ne sont pas encore pris en charge par le crafter");
            }
        }

        // RecipeManager ne rattrape que JsonParseException et IllegalArgumentException, pas
        // l'exception brute de getOrThrow : une recette invalide ferait échouer tout le /reload.
        DataResult<Fields> result = Fields.CODEC.parse(JsonOps.INSTANCE, json);
        Fields fields = result.result().orElseThrow(() -> new JsonSyntaxException(
                result.error().map(DataResult.PartialResult::message).orElse("recette illisible")));

        return new CrafterRecipe(id, fields.inputs(), fields.outputs(), toTicks(fields.time()), fields.minTier());
    }

    /** Arrondi au tick, jamais moins d'un tick. */
    public static int toTicks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0D));
    }

    // Interface (Réseau)

    @Override
    public @Nullable CrafterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int inputCount = buf.readVarInt();
        List<CrafterRecipe.Input> inputs = new ArrayList<>(inputCount);
        for (int i = 0; i < inputCount; i++) {
            inputs.add(new CrafterRecipe.Input(Ingredient.fromNetwork(buf), buf.readVarInt()));
        }

        int outputCount = buf.readVarInt();
        List<CrafterRecipe.Output> outputs = new ArrayList<>(outputCount);
        for (int i = 0; i < outputCount; i++) {
            outputs.add(new CrafterRecipe.Output(buf.readItem(), buf.readFloat()));
        }

        return new CrafterRecipe(id, inputs, outputs, buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, CrafterRecipe recipe) {
        buf.writeVarInt(recipe.inputs().size());
        for (CrafterRecipe.Input input : recipe.inputs()) {
            input.ingredient().toNetwork(buf);
            buf.writeVarInt(input.count());
        }

        buf.writeVarInt(recipe.outputs().size());
        for (CrafterRecipe.Output output : recipe.outputs()) {
            buf.writeItem(output.stack());
            buf.writeFloat(output.chance());
        }

        buf.writeVarInt(recipe.ticks());
        buf.writeVarInt(recipe.minTier());
    }

    // Inner work

    private static <T> Codec<List<T>> sized(Codec<T> element, int max, String name) {
        return element.listOf().flatXmap(
                list -> list.isEmpty() || list.size() > max
                        ? DataResult.error(() -> "« " + name + " » : entre 1 et " + max + " entrées, pas " + list.size())
                        : DataResult.success(list),
                DataResult::success);
    }
}
