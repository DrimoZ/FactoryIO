package com.drimoz.factoryio.core.model;

import com.drimoz.factoryio.core.belts.BeltTransport;
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
 *   "itemsPerSecond": 15,
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
    public static final int DEFAULT_RATE = BeltTransport.everyTicks(BeltDefaults.TRANSPORT_TICKS);

    /**
     * En deçà, un bloc met vingt secondes à être traversé : aucun réglage utile n'y mène,
     * seule une faute de frappe.
     */
    public static final double MIN_ITEMS_PER_SECOND = 0.2D;

    /** Ancienne borne de {@code ticksPerSlot}, la même lenteur que {@link #MIN_ITEMS_PER_SECOND}. */
    public static final int MAX_TICKS_PER_SLOT = 200;

    private BeltCodec() {}

    public static Codec<Belt> forId(ResourceLocation id) {
        return Fields.CODEC.flatXmap(
                fields -> fields.resolveRate().map(rate -> fields.toBelt(id, rate)),
                belt -> DataResult.success(Fields.of(belt)));
    }

    private record Fields(
            Optional<Double> itemsPerSecond,
            Optional<Integer> ticksPerSlot,
            boolean ramp,
            Optional<ResourceLocation> next,
            Optional<ResourceLocation> texture,
            Optional<Map<String, String>> translations) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                StrictCodecs.optional(Codec.doubleRange(MIN_ITEMS_PER_SECOND, Belt.MAX_ITEMS_PER_SECOND), "itemsPerSecond")
                        .forGetter(Fields::itemsPerSecond),
                StrictCodecs.optional(Codec.intRange(1, MAX_TICKS_PER_SLOT), "ticksPerSlot")
                        .forGetter(Fields::ticksPerSlot),
                StrictCodecs.optional(Codec.BOOL, "ramp", true).forGetter(Fields::ramp),
                StrictCodecs.optional(ResourceLocation.CODEC, "next").forGetter(Fields::next),
                StrictCodecs.optional(ResourceLocation.CODEC, "texture").forGetter(Fields::texture),
                StrictCodecs.optional(Codec.unboundedMap(Codec.STRING, Codec.STRING), "translations")
                        .forGetter(Fields::translations)
        ).apply(instance, Fields::new));

        private static Fields of(Belt belt) {
            return new Fields(Optional.of(belt.getItemsPerSecond()), Optional.empty(), belt.hasRamp(), belt.getNext(),
                    Optional.of(belt.getTexture()), Optional.of(belt.getTranslation().asMap()));
        }

        /**
         * Vitesse, en tolérant l'ancienne clé.
         *
         * <p>{@code ticksPerSlot} ne permettait que 40, 20, 13,3, 10… items/s ; elle reste lue pour
         * les datapacks écrits avant FIO-189. Les deux clés à la fois sont une contradiction qu'il
         * vaut mieux signaler que trancher au hasard.
         */
        private DataResult<Integer> resolveRate() {
            if (itemsPerSecond.isPresent() && ticksPerSlot.isPresent()) {
                return DataResult.error(() ->
                        "« itemsPerSecond » et « ticksPerSlot » sont tous deux présents : "
                                + "gardez « itemsPerSecond », l'autre clé est obsolète");
            }

            if (itemsPerSecond.isPresent()) return DataResult.success(Belt.rateOf(itemsPerSecond.get()));

            // Une valeur hors bornes arrive ici quand même, dans le résultat partiel que le codec
            // joint à son erreur : elle doit rester une erreur, pas devenir une exception.
            return ticksPerSlot
                    .map(ticks -> ticks < 1
                            ? DataResult.<Integer>error(() -> "« ticksPerSlot » doit valoir au moins 1 : " + ticks)
                            : DataResult.success(BeltTransport.everyTicks(ticks)))
                    .orElseGet(() -> DataResult.success(DEFAULT_RATE));
        }

        private Belt toBelt(ResourceLocation id, int rate) {
            Belt belt = new Belt(id, rate, this.ramp, this.next, this.texture.orElse(null));
            this.translations.ifPresent(map -> map.forEach(belt.getTranslation()::addTranslation));
            return belt;
        }
    }
}
