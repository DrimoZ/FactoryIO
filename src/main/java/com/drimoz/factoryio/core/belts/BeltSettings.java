package com.drimoz.factoryio.core.belts;

import com.drimoz.factoryio.core.configs.CommonConfig;

/**
 * Réglages globaux des convoyeurs : l'usage des voies, et la génération des vitesses.
 *
 * <p>La vitesse elle-même appartient à chaque définition de convoyeur
 * ({@link com.drimoz.factoryio.core.model.Belt}), réglable par datapack.
 *
 * <h2>La génération</h2>
 *
 * <p>Un convoyeur fixe sa cadence à sa construction ; sans quoi il faudrait relire sa
 * définition à chaque tick, pour une valeur qui ne change qu'exceptionnellement. Un compteur
 * incrémenté à chaque {@code /reload} suffit à leur dire de se remettre à jour, et le coût par
 * tick retombe à une comparaison d'entiers.
 *
 * <p>C'est la leçon de BUG-047 : une valeur dérivée d'un réglage doit être <b>réappliquée</b>
 * quand le réglage change, faute de quoi elle survit à sa propre source.
 */
public final class BeltSettings {

    private static volatile int generation;

    /**
     * Valeur imposée par un GameTest, ou {@code null} pour lire la configuration.
     *
     * <p>Un test qui passait par {@code CommonConfig...set()} écrivait dans le fichier de
     * configuration du joueur, et l'observateur de fichiers de Forge le rechargeait en entier dès
     * qu'un autre test écrivait le sien : la valeur revenait en arrière au milieu du test, qui
     * échouait une fois sur quelques-unes. Les tests passent désormais par ici, en mémoire.
     */
    private static volatile Boolean farLaneOnlyOverride;

    private BeltSettings() {}

    // Interface

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
     */
    public static boolean farLaneOnly() {
        Boolean override = farLaneOnlyOverride;
        if (override != null) return override;

        if (!CommonConfig.SPEC.isLoaded()) return false;

        return CommonConfig.INSERT_ON_FAR_LANE_ONLY.get();
    }

    /** Pour les GameTests seulement : impose la parité stricte, ou rend la main à la configuration ({@code null}). */
    public static void overrideFarLaneOnly(Boolean value) {
        farLaneOnlyOverride = value;
    }

    /** Numéro de génération courant : à comparer à celui qu'un convoyeur a mémorisé. */
    public static int generation() {
        return generation;
    }

    /** Une vitesse a changé : les cadences en cours ne font plus autorité. */
    public static void invalidate() {
        generation++;
    }
}
