package com.drimoz.factoryio.content.crafter;

import com.drimoz.factoryio.core.model.StrictCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Lecture d'une définition de crafter, en {@code config/} comme en datapack.
 *
 * <pre>{@code
 * {
 *   "tier": 2,
 *   "craftingSpeed": 0.75,
 *   "energyPerTick": 60,
 *   "energyCapacity": 40000,
 *   "inputCrafts": 2,
 *   "translations": { "en_us": "Crafter Mk2", "fr_fr": "Crafter Mk2" }
 * }
 * }</pre>
 *
 * <p>Tout est optionnel ; un champ présent et invalide fait échouer la lecture
 * ({@link StrictCodecs}).
 */
public final class CrafterCodec {

    public static final int MAX_TIER = 16;
    public static final float MAX_SPEED = 100.0F;
    public static final int MAX_ENERGY = 1_000_000;
    public static final int MAX_INPUT_CRAFTS = 64;

    public static final Crafter.Tuning DEFAULT_TUNING = new Crafter.Tuning(1.0F, 50, 20_000, 2);

    private static final Codec<Float> SPEED = Codec.FLOAT.flatXmap(
            speed -> speed > 0.0F && speed <= MAX_SPEED
                    ? DataResult.success(speed)
                    : DataResult.error(() -> "« craftingSpeed » doit être dans ]0, " + MAX_SPEED + "] : " + speed),
            DataResult::success);

    private CrafterCodec() {}

    public static Codec<Crafter> forId(ResourceLocation id) {
        return Fields.CODEC.flatXmap(
                fields -> DataResult.success(fields.toCrafter(id)),
                crafter -> DataResult.success(Fields.of(crafter)));
    }

    private record Fields(
            int tier,
            float craftingSpeed,
            int energyPerTick,
            int energyCapacity,
            int inputCrafts,
            Optional<Map<String, String>> translations) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StrictCodecs.optional(Codec.intRange(1, MAX_TIER), "tier", 1).forGetter(Fields::tier),
                StrictCodecs.optional(SPEED, "craftingSpeed", DEFAULT_TUNING.craftingSpeed()).forGetter(Fields::craftingSpeed),
                StrictCodecs.optional(Codec.intRange(0, MAX_ENERGY), "energyPerTick", DEFAULT_TUNING.energyPerTick())
                        .forGetter(Fields::energyPerTick),
                StrictCodecs.optional(Codec.intRange(1, MAX_ENERGY * 100), "energyCapacity", DEFAULT_TUNING.energyCapacity())
                        .forGetter(Fields::energyCapacity),
                StrictCodecs.optional(Codec.intRange(1, MAX_INPUT_CRAFTS), "inputCrafts", DEFAULT_TUNING.inputCrafts())
                        .forGetter(Fields::inputCrafts),
                StrictCodecs.optional(Codec.unboundedMap(Codec.STRING, Codec.STRING), "translations")
                        .forGetter(Fields::translations)
        ).apply(instance, Fields::new));

        private Crafter toCrafter(ResourceLocation id) {
            Crafter crafter = new Crafter(id, this.tier,
                    new Crafter.Tuning(this.craftingSpeed, this.energyPerTick, this.energyCapacity, this.inputCrafts));
            this.translations.ifPresent(map -> map.forEach(crafter.getTranslation()::addTranslation));
            return crafter;
        }

        private static Fields of(Crafter crafter) {
            Crafter.Tuning tuning = crafter.getTuning();
            return new Fields(crafter.getTier(), tuning.craftingSpeed(), tuning.energyPerTick(),
                    tuning.energyCapacity(), tuning.inputCrafts(), Optional.empty());
        }
    }
}
