package com.drimoz.factoryio.core.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Champs optionnels qui <b>propagent</b> les erreurs, contrairement à
 * {@code Codec#optionalFieldOf}.
 *
 * <p>Le comportement de DFU est un piège coûteux : {@code optionalFieldOf} est <i>clément</i>,
 * il rattrape l'échec de lecture d'un champ présent et rend {@code Optional.empty()} — ou la
 * valeur par défaut. Autrement dit, avec le codec naïf, {@code "ticksPerSwing": -4000} et
 * {@code "grabDistance": "loin"} passent sans un mot, avec la valeur par défaut. C'est
 * exactement le défaut que FIO-034 devait corriger, et il aurait survécu à sa correction si
 * les tests ne l'avaient pas relevé.
 *
 * <p>{@code ExtraCodecs.strictOptionalField} rendrait ce service, mais n'existe pas en
 * 1.20.1.
 *
 * <p>Extrait de {@code InserterCodec} pour servir aussi au barème des améliorations : deux
 * formats de données, un seul piège à éviter.
 */
public final class StrictCodecs {

    private StrictCodecs() {}

    /** Champ optionnel : absent donne {@code Optional.empty()}, présent et invalide échoue. */
    public static <T> MapCodec<Optional<T>> optional(Codec<T> codec, String name) {
        return new MapCodec<>() {
            @Override
            public <O> DataResult<Optional<T>> decode(DynamicOps<O> ops, MapLike<O> input) {
                O value = input.get(name);
                if (value == null) return DataResult.success(Optional.empty());

                return codec.parse(ops, value)
                        .mapError(error -> "« " + name + " » : " + error)
                        .map(Optional::of);
            }

            @Override
            public <O> RecordBuilder<O> encode(Optional<T> value, DynamicOps<O> ops, RecordBuilder<O> prefix) {
                return value.isPresent()
                        ? prefix.add(name, codec.encodeStart(ops, value.get()))
                        : prefix;
            }

            @Override
            public <O> Stream<O> keys(DynamicOps<O> ops) {
                return Stream.of(ops.createString(name));
            }
        };
    }

    /** Variante à valeur par défaut, tout aussi stricte sur un champ présent mais invalide. */
    public static <T> MapCodec<T> optional(Codec<T> codec, String name, T fallback) {
        return optional(codec, name).xmap(
                value -> value.orElse(fallback),
                value -> Objects.equals(value, fallback) ? Optional.empty() : Optional.of(value));
    }
}
