package com.drimoz.factoryio.core.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;

/**
 * Lecture d'une définition de convoyeur, en {@code config/} comme en datapack.
 *
 * <pre>{@code
 * {
 *   "ticksPerSlot": 1,
 *   "ramp": true,
 *   "next": "factor_io:express_transport_belt",
 *   "texture": "mypack:block/belts/turbo",
 *   "translations": { "en_us": "Turbo Belt", "fr_fr": "Convoyeur turbo" }
 * }
 * }</pre>
 *
 * <p>Tout est optionnel, mais un champ présent et invalide fait échouer la lecture avec un
 * motif qui le nomme ({@link StrictCodecs}) : pas de coercition silencieuse.
 */
public final class BeltCodec {

    /** Le convoyeur de base : 10 items/s. */
    public static final int DEFAULT_TICKS_PER_SLOT = 4;

    /**
     * Au-delà, un bloc met quarante secondes à être traversé : aucun réglage utile n'y mène,
     * seule une faute de frappe.
     */
    public static final int MAX_TICKS_PER_SLOT = 200;

    private BeltCodec() {}

    public static Codec<Belt> forId(ResourceLocation id) {
        return Fields.CODEC.flatXmap(
                fields -> DataResult.success(fields.toBelt(id)),
                belt -> DataResult.success(Fields.of(belt)));
    }

    private record Fields(
            int ticksPerSlot,
            boolean ramp,
            Optional<ResourceLocation> next,
            Optional<ResourceLocation> texture,
            Optional<Map<String, String>> translations) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StrictCodecs.optional(Codec.intRange(1, MAX_TICKS_PER_SLOT), "ticksPerSlot", DEFAULT_TICKS_PER_SLOT)
                        .forGetter(Fields::ticksPerSlot),
                StrictCodecs.optional(Codec.BOOL, "ramp", true).forGetter(Fields::ramp),
                StrictCodecs.optional(ResourceLocation.CODEC, "next").forGetter(Fields::next),
                StrictCodecs.optional(ResourceLocation.CODEC, "texture").forGetter(Fields::texture),
                StrictCodecs.optional(Codec.unboundedMap(Codec.STRING, Codec.STRING), "translations")
                        .forGetter(Fields::translations)
        ).apply(instance, Fields::new));

        private static Fields of(Belt belt) {
            return new Fields(belt.getTicksPerSlot(), belt.hasRamp(), belt.getNext(),
                    Optional.of(belt.getTexture()), Optional.of(belt.getTranslation().asMap()));
        }

        private Belt toBelt(ResourceLocation id) {
            Belt belt = new Belt(id, this.ticksPerSlot, this.ramp, this.next, this.texture.orElse(null));
            this.translations.ifPresent(map -> map.forEach(belt.getTranslation()::addTranslation));
            return belt;
        }
    }
}
