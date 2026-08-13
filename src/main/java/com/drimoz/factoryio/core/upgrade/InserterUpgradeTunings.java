package com.drimoz.factoryio.core.upgrade;

/**
 * Le barème d'améliorations en vigueur.
 *
 * <p>Une seule valeur pour tout le jeu, remplacée <b>d'un bloc</b> et jamais champ par champ —
 * même règle que pour les réglages d'inserter (FIO-037), et pour la même raison : c'est ce qui
 * permet de détecter un rechargement par simple comparaison de référence, sans compteur de
 * génération ni écouteur.
 *
 * <p>C'est précisément ce dont le block entity a besoin. Il met en cache les réglages
 * effectifs d'un exemplaire — type, plus modules posés — et doit savoir quand les recalculer.
 * Un {@code /reload} qui ne change que les <i>facteurs</i> laisse le réglage du type
 * inchangé : sans surveiller aussi cette référence-ci, le cache resterait valide et les
 * modules garderaient leur ancien effet jusqu'au prochain redémarrage.
 */
public final class InserterUpgradeTunings {

    private static volatile InserterUpgradeTuning current = InserterUpgradeTuning.DEFAULT;

    private InserterUpgradeTunings() {}

    /**
     * @return le barème courant, à comparer par <b>identité</b> pour détecter un rechargement
     *
     * <p>{@code volatile} parce que le chargement d'un datapack a lieu sur le fil serveur et
     * la lecture aussi bien sur le fil client, à chaque image, pour l'animation.
     */
    public static InserterUpgradeTuning current() {
        return current;
    }

    /** Remplace le barème — un datapack vient d'être appliqué, ou reçu du serveur. */
    public static void set(InserterUpgradeTuning tuning) {
        current = tuning;
    }

    /** Revient au barème livré : plus aucun datapack ne surcharge les améliorations. */
    public static void reset() {
        current = InserterUpgradeTuning.DEFAULT;
    }
}
