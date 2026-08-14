package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.configs.CommonConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Ce que la configuration dit des convoyeurs : leur vitesse, et l'usage des voies.
 *
 * <h2>Pourquoi cette classe existe plutôt qu'une méthode sur {@link BeltTier}</h2>
 *
 * <p>{@code BeltTier} est manipulé par des tests JUnit, qui tournent sans Forge et sans
 * configuration chargée. Y placer la lecture ferait charger {@code CommonConfig} — donc
 * construire un {@code ForgeConfigSpec} — au simple usage de l'énumération. C'est la même
 * frontière que celle qui a fait sortir {@code BeltPath} du renderer, et le même piège que
 * celui qui avait cassé les tests des améliorations d'inserter.
 *
 * <p>{@code BeltTier} garde donc le <b>barème livré</b>, qui reste la valeur par défaut de la
 * configuration et le repli quand elle n'est pas encore chargée.
 *
 * <h2>La génération</h2>
 *
 * <p>Un convoyeur fixe sa cadence à sa construction ; sans quoi il faudrait relire la
 * configuration à chaque tick, pour une valeur qui ne change qu'exceptionnellement. Un compteur
 * incrémenté à chaque (re)chargement suffit à leur dire de se remettre à jour, et le coût par
 * tick retombe à une comparaison d'entiers.
 *
 * <p>C'est la leçon de BUG-047 : une valeur dérivée d'un réglage doit être <b>réappliquée</b>
 * quand le réglage change, faute de quoi elle survit à sa propre source.
 */
public final class BeltSettings {

    private static volatile int generation;

    private BeltSettings() {}

    // Interface

    /**
     * Durée d'un pas pour ce tier, en ticks.
     *
     * <p>Retombe sur le barème tant que la configuration n'est pas chargée — ce qui arrive
     * réellement : les block entities sont construites bien après, mais rien ne garantit que
     * tout appelant le sera (cf. BUG-001, où un {@code get()} anticipé renvoyait la valeur par
     * défaut <i>en silence</i>).
     */
    public static int ticksPerSlot(BeltTier tier) {
        if (!CommonConfig.SPEC.isLoaded()) return tier.ticksPerSlot();

        return Math.max(1, value(tier).get());
    }

    /**
     * La voie proche est-elle interdite au dépôt ?
     *
     * <h3>Ce que change ce réglage</h3>
     *
     * <p>Factorio n'utilise <b>jamais</b> la voie proche : un inserter qui trouve la voie
     * lointaine pleine attend, il ne se rabat pas. C'est ce qui rend une voie utilisable comme
     * réserve, et ce sur quoi reposent les montages qui séparent deux ressources sur une même
     * bande.
     *
     * <p>Par défaut, ici, il se rabat — un inserter arrêté devant un convoyeur à moitié vide
     * se lit comme une panne pour qui ne connaît pas Factorio. Les deux comportements sont
     * indiscernables tant que la voie lointaine n'est pas saturée.
     *
     * <p>Lu à chaque appel : le réglage ne dérive de rien et n'a rien à mémoriser, contrairement
     * à la cadence.
     */
    public static boolean farLaneOnly() {
        if (!CommonConfig.SPEC.isLoaded()) return false;

        return CommonConfig.INSERT_ON_FAR_LANE_ONLY.get();
    }

    /** Numéro de génération courant : à comparer à celui qu'un convoyeur a mémorisé. */
    public static int generation() {
        return generation;
    }

    /** La configuration a été (re)chargée : les cadences en cours ne font plus autorité. */
    public static void invalidate() {
        generation++;
    }

    // Inner work

    private static ForgeConfigSpec.ConfigValue<Integer> value(BeltTier tier) {
        return switch (tier) {
            case TRANSPORT -> CommonConfig.BELT_COOLDOWN;
            case FAST -> CommonConfig.FAST_BELT_COOLDOWN;
            case EXPRESS -> CommonConfig.EXPRESS_BELT_COOLDOWN;
        };
    }
}
