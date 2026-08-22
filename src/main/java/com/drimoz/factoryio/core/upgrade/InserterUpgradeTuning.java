package com.drimoz.factoryio.core.upgrade;

import com.drimoz.factoryio.core.model.Inserter;
import com.drimoz.factoryio.core.model.StrictCodecs;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Les valeurs du système d'améliorations qu'un datapack peut redéfinir.
 *
 * <h2>Pourquoi un datapack, et pas la config</h2>
 *
 * <p>Même frontière que {@link com.drimoz.factoryio.core.model.InserterTuning}, et pour la
 * même raison. Ce que porte cet objet n'est que des <b>nombres lus au tick</b> : un facteur
 * par palier, un plafond, un bonus de main. Rien n'y décide d'un registre, d'une taille de
 * conteneur ni d'un menu, donc rien n'interdit de les changer à chaud.
 *
 * <p>Le <b>nombre de slots d'amélioration</b>, lui, n'est pas ici : il fixe la taille de
 * l'inventaire, donc il est structurel et vit dans la définition de l'inserter, réglable par
 * {@code config/} et prise en compte au lancement suivant.
 *
 * <h2>Le plafond n'est pas le palier d'un module</h2>
 *
 * <p>Deux limites différentes cohabitent, et les confondre serait une erreur :
 *
 * <ul>
 *   <li>{@link InserterUpgradeType.Nature#tiers()} borne ce qu'<b>un</b> module apporte —
 *       c'est une donnée structurelle, adossée aux tags qui existent ;</li>
 *   <li>{@link #maxLevel()} borne la <b>somme</b> des modules d'un même axe. C'est lui qui
 *       décide si empiler quatre modules de vitesse a encore un effet.</li>
 * </ul>
 *
 * @param speedFactor      facteur appliqué à la durée d'un mouvement, par palier de vitesse
 * @param efficiencyFactor facteur appliqué au coût d'un mouvement, par palier d'efficacité
 * @param capacityBonus    items ajoutés à la main, par palier de capacité
 * @param maxLevel         plafond du cumul des paliers d'un même axe
 * @param gated            natures débloquantes qui <b>exigent</b> un module ; une nature
 *                         absente de cet ensemble est acquise à tout le monde
 */
public record InserterUpgradeTuning(
        double speedFactor,
        double efficiencyFactor,
        int capacityBonus,
        int maxLevel,
        Set<InserterUpgradeType> gated) {

    /**
     * Plafond par défaut du cumul : ce que porteraient des slots pleins de modules maximaux.
     *
     * <p><b>Ce n'est pas le plafond d'un module.</b> Les fixer à la même valeur — ce qui a
     * été fait dans un premier temps — annule l'empilement : deux modules de palier 3 donnent
     * 6, écrêté à 3, donc le second ne sert à rien. C'est la limite « un par axe » de
     * l'ancien système, reconduite par accident dans celui qui devait la lever.
     *
     * <p>La vraie limite est le <b>nombre de slots</b>, qui est un trait du modèle
     * d'inserter. Ce plafond-ci n'est qu'un garde-fou pour un datapack qui distribuerait des
     * paliers absurdes ; il doit donc rester assez haut pour ne jamais être ce qui borne le
     * joueur.
     */
    public static final int DEFAULT_MAX_LEVEL =
            InserterUpgradeType.MAX_LEVEL * Inserter.MAX_UPGRADE_SLOTS;

    /**
     * Le barème livré.
     *
     * <p>Les facteurs reproduisent exactement le comportement d'avant la configurabilité :
     * un monde existant ne bouge pas tant qu'aucun datapack ne s'en mêle.
     *
     * <p><b>Aucune nature n'est verrouillée pour l'instant, et c'est volontaire.</b>
     * {@link InserterUpgradeType#ADVANCED_REDSTONE} n'a encore ni item ni tag peuplé :
     * l'inscrire ici retirerait la condition analogique à tout le monde <b>sans laisser
     * aucun moyen de la débloquer</b>. Le verrou se pose le jour où le module se fabrique,
     * pas avant.
     */
    public static final InserterUpgradeTuning DEFAULT = new InserterUpgradeTuning(
            0.75D, 0.75D, 1, DEFAULT_MAX_LEVEL, EnumSet.noneOf(InserterUpgradeType.class));

    public InserterUpgradeTuning {
        // Un facteur hors de ]0, 1] n'a pas de sens : au-dessus de 1 une « amélioration »
        // dégraderait, à 0 ou moins elle annulerait une durée. Un datapack fautif est
        // ramené dans le domaine plutôt que de produire une division par zéro au tick.
        speedFactor = clampFactor(speedFactor);
        efficiencyFactor = clampFactor(efficiencyFactor);
        capacityBonus = Math.max(0, capacityBonus);
        maxLevel = Math.max(0, maxLevel);
        // Copie défensive dans un EnumSet, sans passer par copyOf qui refuse une collection
        // vide n'étant pas déjà un EnumSet.
        Set<InserterUpgradeType> copy = EnumSet.noneOf(InserterUpgradeType.class);
        copy.addAll(gated);
        gated = copy;
    }

    private static double clampFactor(double factor) {
        if (!(factor > 0D)) return 1D;

        return Math.min(factor, 1D);
    }

    // Interface

    /**
     * @return {@code true} si cette nature exige un module pour être disponible
     *
     * <p>Ne concerne que les natures {@link InserterUpgradeType.Nature#UNLOCKING} : un axe
     * cumulatif n'a rien à débloquer, il n'agit que par ses paliers.
     */
    public boolean requiresModule(InserterUpgradeType type) {
        return this.gated.contains(type);
    }

    // Interface (Datapack)

    /**
     * Lecture depuis {@code data/<namespace>/factor_io/upgrades/tuning.json}.
     *
     * <p>Tous les champs sont optionnels et retombent sur le barème livré : un pack qui ne
     * veut changer qu'un facteur n'écrit qu'une ligne. Un champ <b>présent mais invalide</b>
     * fait en revanche échouer la lecture avec un motif nommant le champ — c'est tout l'objet
     * de {@link StrictCodecs}, et la raison pour laquelle {@code optionalFieldOf} ne convient
     * pas.
     *
     * <p>Les natures verrouillées sont listées par leur identifiant ({@code speed},
     * {@code advanced_redstone}, …) plutôt que par un ordinal : un ordinal dans un fichier de
     * données changerait de sens le jour où une nature s'insère au milieu de l'énumération.
     */
    private static final Codec<InserterUpgradeType> TYPE_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                for (InserterUpgradeType type : InserterUpgradeType.all()) {
                    if (type.id().equals(id)) return DataResult.success(type);
                }

                return DataResult.error(() -> "nature d'amélioration inconnue : « " + id + " »");
            },
            InserterUpgradeType::id);

    public static final Codec<InserterUpgradeTuning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StrictCodecs.optional(Codec.DOUBLE, "speedFactor", DEFAULT.speedFactor())
                    .forGetter(InserterUpgradeTuning::speedFactor),
            StrictCodecs.optional(Codec.DOUBLE, "efficiencyFactor", DEFAULT.efficiencyFactor())
                    .forGetter(InserterUpgradeTuning::efficiencyFactor),
            StrictCodecs.optional(ExtraCodecs.NON_NEGATIVE_INT, "capacityBonus", DEFAULT.capacityBonus())
                    .forGetter(InserterUpgradeTuning::capacityBonus),
            StrictCodecs.optional(ExtraCodecs.NON_NEGATIVE_INT, "maxLevel", DEFAULT.maxLevel())
                    .forGetter(InserterUpgradeTuning::maxLevel),
            StrictCodecs.optional(TYPE_CODEC.listOf(), "requiresModule", List.<InserterUpgradeType>of())
                    .forGetter(tuning -> List.copyOf(tuning.gated()))
    ).apply(instance, (speed, efficiency, capacity, max, gated) ->
            new InserterUpgradeTuning(speed, efficiency, capacity, max, Set.copyOf(gated))));

    // Interface (Réseau)

    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(this.speedFactor);
        buf.writeDouble(this.efficiencyFactor);
        buf.writeVarInt(this.capacityBonus);
        buf.writeVarInt(this.maxLevel);

        // Un masque plutôt qu'une liste : le nombre de natures est petit et connu des deux
        // côtés, et l'ordinal est déjà la clé utilisée partout ailleurs.
        int mask = 0;
        for (InserterUpgradeType type : this.gated) {
            mask |= 1 << type.ordinal();
        }

        buf.writeVarInt(mask);
    }

    public static InserterUpgradeTuning read(FriendlyByteBuf buf) {
        double speedFactor = buf.readDouble();
        double efficiencyFactor = buf.readDouble();
        int capacityBonus = buf.readVarInt();
        int maxLevel = buf.readVarInt();
        int mask = buf.readVarInt();

        Set<InserterUpgradeType> gated = EnumSet.noneOf(InserterUpgradeType.class);
        for (InserterUpgradeType type : InserterUpgradeType.all()) {
            if ((mask & (1 << type.ordinal())) != 0) gated.add(type);
        }

        return new InserterUpgradeTuning(speedFactor, efficiencyFactor, capacityBonus, maxLevel, gated);
    }
}
