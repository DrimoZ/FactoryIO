package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.configs.CommonConfig;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * La vitesse effective d'un convoyeur : celle du barème, ou celle de la configuration.
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
public final class BeltSpeeds {

    private static volatile int generation;

    private BeltSpeeds() {}

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
