package com.drimoz.factoryio.core.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * (Dé)sérialisation d'une définition d'inserter (FIO-034, cf. DT-04).
 *
 * <p>Remplace une lecture manuelle à coups de {@code GsonHelper.getAsInt(json, "clé",
 * défaut)}, qui ne disait jamais rien : une faute de frappe dans une clé passait pour une
 * absence, donc pour le défaut, et une valeur absurde était ramenée à 1 en silence. Un
 * {@code Codec} rend chaque erreur explicite et nommée, et donne au passage la
 * sérialisation dont la Phase 1 a besoin pour charger les définitions depuis un datapack
 * (FIO-037) et les synchroniser au client.
 *
 * <h2>L'identifiant ne vient pas du JSON</h2>
 *
 * <p>Il vient du nom du fichier — c'est la convention Minecraft, et c'est ce que
 * {@code SimpleJsonResourceReloadListener} fournira. D'où {@link #forId(ResourceLocation)}
 * plutôt qu'un {@code Codec} statique : le codec est fabriqué pour un identifiant connu.
 *
 * <h2>Ce qui est validé</h2>
 *
 * <ul>
 *   <li>les durées, portées et quantités doivent être <b>strictement positives</b> ;</li>
 *   <li>les champs d'énergie n'ont de sens que sur un inserter électrique, ceux de
 *       carburant que sur un burner — les mélanger est une <b>erreur</b>, pas un oubli à
 *       ignorer. C'est précisément le genre de réglage qui, avant, ne faisait rien du
 *       tout sans le dire.</li>
 * </ul>
 */
public final class InserterCodec {

    /** Vitesse par défaut : celle de l'{@code inserter} de base. */
    public static final int DEFAULT_TICKS_PER_SWING = 12;

    private static final int DEFAULT_ENERGY_CAPACITY = 9600;
    private static final int DEFAULT_ENERGY_TRANSFER_RATE = 500;
    private static final int DEFAULT_ENERGY_CONSUMPTION = 96;

    /**
     * Slots d'amélioration d'un inserter défini par l'utilisateur.
     *
     * <p>Le milieu du barème livré : un JSON qui ne dit rien obtient un inserter améliorable
     * sans être d'emblée au plafond.
     */
    public static final int DEFAULT_UPGRADE_SLOTS = 2;

    private static final int DEFAULT_FUEL_CAPACITY = 3200;
    private static final int DEFAULT_FUEL_CONSUMPTION = 68;

    /** Pas de l'ancien compteur de cooldown, par tick (cf. DT-10, FIO-065). */
    private static final int LEGACY_COOLDOWN_PER_TICK = 10;

    private InserterCodec() {}

    /**
     * Champ optionnel qui <b>propage</b> les erreurs.
     *
     * <p>Le mécanisme et sa raison d'être sont dans {@link StrictCodecs} : il a été extrait
     * pour servir aussi au barème des améliorations.
     */
    private static <T> MapCodec<Optional<T>> strictOptional(Codec<T> codec, String name) {
        return StrictCodecs.optional(codec, name);
    }

    /** Variante à valeur par défaut, tout aussi stricte sur un champ présent mais invalide. */
    private static <T> MapCodec<T> strictOptional(Codec<T> codec, String name, T fallback) {
        return StrictCodecs.optional(codec, name, fallback);
    }

    /**
     * @param id identifiant de l'inserter, tiré du nom du fichier
     * @return un codec qui lit et écrit la définition portée par le JSON
     */
    public static Codec<Inserter> forId(ResourceLocation id) {
        return Fields.CODEC.flatXmap(
                fields -> fields.toInserter(id),
                inserter -> DataResult.success(Fields.of(inserter)));
    }

    /**
     * Champs tels qu'ils apparaissent dans le JSON.
     *
     * <p>Tout ce qui dépend du mode d'alimentation est {@link Optional} : c'est ce qui
     * permet de distinguer « absent » de « valant le défaut », et donc de refuser une
     * capacité d'énergie posée sur un inserter à carburant.
     */
    private record Fields(
            boolean useEnergy,
            boolean filterable,
            boolean affectedByRedstone,
            int upgradeSlots,
            int grabDistance,
            Optional<Integer> ticksPerSwing,
            Optional<Integer> legacyCooldown,
            int preferredItemCountPerAction,
            Optional<Integer> energyCapacity,
            Optional<Integer> energyTransferRate,
            Optional<Integer> energyConsumption,
            Optional<Integer> fuelCapacity,
            Optional<Integer> fuelConsumption,
            Optional<ResourceLocation> texture,
            Optional<Map<String, String>> translations) {

        private static final Codec<Fields> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                strictOptional(Codec.BOOL, "useEnergy", false).forGetter(Fields::useEnergy),
                strictOptional(Codec.BOOL, "filterable", false).forGetter(Fields::filterable),
                strictOptional(Codec.BOOL, "affectedByRedstone", true).forGetter(Fields::affectedByRedstone),
                // NON_NEGATIVE_INT et non POSITIVE_INT : un inserter qui n'accepte aucune
                // amélioration est un choix de conception, pas une faute de frappe.
                strictOptional(ExtraCodecs.NON_NEGATIVE_INT, "upgradeSlots", DEFAULT_UPGRADE_SLOTS)
                        .forGetter(Fields::upgradeSlots),
                strictOptional(ExtraCodecs.POSITIVE_INT, "grabDistance", 1).forGetter(Fields::grabDistance),
                strictOptional(ExtraCodecs.POSITIVE_INT, "ticksPerSwing").forGetter(Fields::ticksPerSwing),
                strictOptional(ExtraCodecs.POSITIVE_INT, "cooldownBetweenActions").forGetter(Fields::legacyCooldown),
                strictOptional(ExtraCodecs.POSITIVE_INT, "preferredItemCountPerAction", 1)
                        .forGetter(Fields::preferredItemCountPerAction),
                strictOptional(ExtraCodecs.POSITIVE_INT, "energyCapacity").forGetter(Fields::energyCapacity),
                strictOptional(ExtraCodecs.POSITIVE_INT, "energyTransferRate").forGetter(Fields::energyTransferRate),
                strictOptional(ExtraCodecs.POSITIVE_INT, "energyConsumption").forGetter(Fields::energyConsumption),
                strictOptional(ExtraCodecs.POSITIVE_INT, "fuelCapacity").forGetter(Fields::fuelCapacity),
                strictOptional(ExtraCodecs.POSITIVE_INT, "fuelConsumption").forGetter(Fields::fuelConsumption),
                strictOptional(ResourceLocation.CODEC, "texture").forGetter(Fields::texture),
                strictOptional(Codec.unboundedMap(Codec.STRING, Codec.STRING), "translations")
                        .forGetter(Fields::translations)
        ).apply(instance, Fields::new));

        /** Reconstruit les champs depuis une définition, pour l'encodage. */
        private static Fields of(Inserter inserter) {
            boolean energy = inserter.useEnergy();

            return new Fields(
                    energy,
                    inserter.isFilterable(),
                    inserter.isAffectedByRedstone(),
                    inserter.getUpgradeSlots(),
                    inserter.getGrabDistance(),
                    Optional.of(inserter.getTicksPerSwing()),
                    Optional.empty(),
                    inserter.getPreferredItemCountPerAction(),
                    energy ? Optional.of(inserter.getEnergyCapacity()) : Optional.empty(),
                    energy ? Optional.of(inserter.getEnergyTransferRate()) : Optional.empty(),
                    energy ? Optional.of(inserter.getEnergyConsumption()) : Optional.empty(),
                    energy ? Optional.empty() : Optional.of(inserter.getFuelCapacity()),
                    energy ? Optional.empty() : Optional.of(inserter.getFuelConsumption()),
                    Optional.ofNullable(inserter.getTexture()),
                    Optional.of(inserter.getTranslation().asMap()));
        }

        private DataResult<Inserter> toInserter(ResourceLocation id) {
            DataResult<Integer> swing = resolveTicksPerSwing();
            if (swing.error().isPresent()) {
                return DataResult.error(() -> swing.error().orElseThrow().message());
            }

            String misplaced = misplacedFields();
            if (misplaced != null) return DataResult.error(() -> misplaced);

            Inserter inserter = useEnergy
                    ? Inserter.electric(
                            id, affectedByRedstone,
                            grabDistance, swing.result().orElseThrow(), preferredItemCountPerAction,
                            filterable, upgradeSlots,
                            energyCapacity.orElse(DEFAULT_ENERGY_CAPACITY),
                            energyTransferRate.orElse(DEFAULT_ENERGY_TRANSFER_RATE),
                            energyConsumption.orElse(DEFAULT_ENERGY_CONSUMPTION))
                    : Inserter.burner(
                            id, affectedByRedstone,
                            grabDistance, swing.result().orElseThrow(), preferredItemCountPerAction,
                            filterable, upgradeSlots,
                            fuelCapacity.orElse(DEFAULT_FUEL_CAPACITY),
                            fuelConsumption.orElse(DEFAULT_FUEL_CONSUMPTION));

            texture.ifPresent(inserter::setTexture);
            translations.ifPresent(map ->
                    map.forEach((code, value) -> inserter.getTranslation().addTranslation(code, value)));

            return DataResult.success(inserter);
        }

        /**
         * Vitesse, en tolérant l'ancienne clé.
         *
         * <p>{@code cooldownBetweenActions} n'exprimait pas des ticks : il était comparé à
         * un compteur incrémenté de dix par tick, donc valait dix fois la durée réelle
         * (cf. DT-10). Un JSON écrit avant FIO-065 est converti plutôt que rejeté ; les
         * deux clés à la fois, en revanche, est une contradiction qu'il vaut mieux
         * signaler que trancher au hasard.
         */
        private DataResult<Integer> resolveTicksPerSwing() {
            if (ticksPerSwing.isPresent() && legacyCooldown.isPresent()) {
                return DataResult.error(() ->
                        "« ticksPerSwing » et « cooldownBetweenActions » sont tous deux présents : "
                                + "gardez « ticksPerSwing », l'autre clé est obsolète");
            }

            if (ticksPerSwing.isPresent()) return DataResult.success(ticksPerSwing.get());

            return legacyCooldown
                    .map(legacy -> DataResult.success(Math.max(1, legacy / LEGACY_COOLDOWN_PER_TICK)))
                    .orElseGet(() -> DataResult.success(DEFAULT_TICKS_PER_SWING));
        }

        /** @return le message décrivant les champs sans objet pour ce mode, ou {@code null} */
        private String misplacedFields() {
            if (useEnergy) {
                if (fuelCapacity.isPresent() || fuelConsumption.isPresent()) {
                    return "« fuelCapacity » et « fuelConsumption » n'ont pas de sens sur un "
                            + "inserter électrique (useEnergy = true) ; utilisez « energyCapacity » "
                            + "et « energyConsumption »";
                }

                return null;
            }

            if (energyCapacity.isPresent() || energyTransferRate.isPresent() || energyConsumption.isPresent()) {
                return "« energyCapacity », « energyTransferRate » et « energyConsumption » n'ont pas "
                        + "de sens sur un inserter à carburant (useEnergy = false) ; utilisez "
                        + "« fuelCapacity » et « fuelConsumption »";
            }

            return null;
        }
    }
}
