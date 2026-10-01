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
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import java.util.Optional;
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
 *   "fluidIngredients": [ { "tag": "forge:water", "amount": 500 } ],
 *   "fluidResults": [ { "fluid": "minecraft:lava", "amount": 100 } ],
 *   "time": 10.0,
 *   "minTier": 2
 * }
 * }</pre>
 *
 * <p>Codec borné : une valeur hors bornes fait échouer la lecture avec un message qui la
 * nomme, et {@code RecipeManager} journalise l'échec — jamais de valeur ramenée en silence.
 * Tout autre champ {@code fluid…} est refusé pour la même raison : une faute de frappe ne doit
 * pas donner une recette sans son fluide.
 */
public class CrafterRecipeSerializer implements RecipeSerializer<CrafterRecipe> {

    public static final int MAX_INPUTS = 9;
    public static final int MAX_OUTPUTS = 4;
    public static final int MAX_COUNT = 64;
    /** Une heure : au-delà, c'est une faute de frappe, pas un réglage. */
    public static final float MAX_TIME = 3600.0F;
    public static final int MAX_TIER = 16;
    /** En millibuckets : deux crafts d'avance tiennent dans un réservoir de 64 000. */
    public static final int MAX_FLUID_AMOUNT = 32_000;

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

    private static final Codec<Fluid> FLUID = ForgeRegistries.FLUIDS.getCodec().flatXmap(
            fluid -> fluid == Fluids.EMPTY ? DataResult.error(() -> "fluide vide ou inconnu") : DataResult.success(fluid),
            DataResult::success);

    private static final Codec<Integer> FLUID_AMOUNT = Codec.intRange(1, MAX_FLUID_AMOUNT);

    /** Un fluide précis ({@code "fluid"}) ou un tag ({@code "tag"}), pas les deux. */
    private static final Codec<FluidInput> FLUID_INPUT = RecordCodecBuilder.<FluidInputFields>create(instance -> instance.group(
            StrictCodecs.optional(FLUID, "fluid").forGetter(FluidInputFields::fluid),
            StrictCodecs.optional(ResourceLocation.CODEC, "tag").forGetter(FluidInputFields::tag),
            FLUID_AMOUNT.fieldOf("amount").forGetter(FluidInputFields::amount)
    ).apply(instance, FluidInputFields::new)).flatXmap(
            fields -> fields.fluid().isPresent() == fields.tag().isPresent()
                    ? DataResult.error(() -> "un ingrédient fluide a « fluid » ou « tag », exactement l'un des deux")
                    : DataResult.success(new FluidInput(fields.fluid().orElse(null),
                            fields.tag().map(tag -> TagKey.create(ForgeRegistries.Keys.FLUIDS, tag)).orElse(null),
                            fields.amount())),
            input -> DataResult.success(new FluidInputFields(Optional.ofNullable(input.fluid()),
                    Optional.ofNullable(input.tag()).map(TagKey::location), input.amount())));

    private record FluidInputFields(Optional<Fluid> fluid, Optional<ResourceLocation> tag, int amount) {}

    private static final Codec<FluidStack> FLUID_OUTPUT = RecordCodecBuilder.create(instance -> instance.group(
            FLUID.fieldOf("fluid").forGetter(FluidStack::getFluid),
            FLUID_AMOUNT.fieldOf("amount").forGetter(FluidStack::getAmount)
    ).apply(instance, FluidStack::new));

    private record Fields(List<CrafterRecipe.Input> inputs, List<CrafterRecipe.Output> outputs,
                          List<FluidInput> fluidInputs, List<FluidStack> fluidOutputs, float time, int minTier) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                sized(INPUT, MAX_INPUTS, "ingredients").fieldOf("ingredients").forGetter(Fields::inputs),
                sized(OUTPUT, MAX_OUTPUTS, "results").fieldOf("results").forGetter(Fields::outputs),
                StrictCodecs.optional(sized(FLUID_INPUT, CrafterRecipe.MAX_FLUIDS, "fluidIngredients"), "fluidIngredients", List.of())
                        .forGetter(Fields::fluidInputs),
                StrictCodecs.optional(sized(FLUID_OUTPUT, CrafterRecipe.MAX_FLUIDS, "fluidResults"), "fluidResults", List.of())
                        .forGetter(Fields::fluidOutputs),
                TIME.fieldOf("time").forGetter(Fields::time),
                StrictCodecs.optional(Codec.intRange(1, MAX_TIER), "minTier", 1).forGetter(Fields::minTier)
        ).apply(instance, Fields::new));
    }

    private static final java.util.Set<String> FLUID_KEYS = java.util.Set.of("fluidIngredients", "fluidResults");

    // Interface (Datapack)

    @Override
    public CrafterRecipe fromJson(ResourceLocation id, JsonObject json) {
        for (String key : json.keySet()) {
            if (key.startsWith("fluid") && !FLUID_KEYS.contains(key)) {
                throw new JsonSyntaxException("« " + key + " » : champ inconnu, attendu « fluidIngredients » ou « fluidResults »");
            }
        }

        // RecipeManager ne rattrape que JsonParseException et IllegalArgumentException, pas
        // l'exception brute de getOrThrow : une recette invalide ferait échouer tout le /reload.
        DataResult<Fields> result = Fields.CODEC.parse(JsonOps.INSTANCE, json);
        Fields fields = result.result().orElseThrow(() -> new JsonSyntaxException(
                result.error().map(DataResult.PartialResult::message).orElse("recette illisible")));

        return new CrafterRecipe(id, fields.inputs(), fields.outputs(), fields.fluidInputs(), fields.fluidOutputs(),
                toTicks(fields.time()), fields.minTier());
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

        int fluidInputCount = buf.readVarInt();
        List<FluidInput> fluidInputs = new ArrayList<>(fluidInputCount);
        for (int i = 0; i < fluidInputCount; i++) fluidInputs.add(FluidInput.fromNetwork(buf));

        int fluidOutputCount = buf.readVarInt();
        List<FluidStack> fluidOutputs = new ArrayList<>(fluidOutputCount);
        for (int i = 0; i < fluidOutputCount; i++) fluidOutputs.add(FluidStack.readFromPacket(buf));

        return new CrafterRecipe(id, inputs, outputs, fluidInputs, fluidOutputs, buf.readVarInt(), buf.readVarInt());
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

        buf.writeVarInt(recipe.fluidInputs().size());
        for (FluidInput input : recipe.fluidInputs()) input.toNetwork(buf);

        buf.writeVarInt(recipe.fluidOutputs().size());
        for (FluidStack output : recipe.fluidOutputs()) output.writeToPacket(buf);

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
