package com.drimoz.factoryio.core.inserters;

/**
 * Sens du mouvement de bras en cours, et donc du trajet de l'item transporté.
 *
 * <p>L'inserter enchaîne deux actions par item : une prise, puis une dépose. La phase
 * dit laquelle des deux vient de démarrer, ce qui suffit à placer l'item entre le
 * voisin et la main (cf. {@link InserterCarryPath}).
 */
public enum InserterSwingPhase {

    /** Aucun mouvement : rien à afficher. */
    NONE,

    /** Le bras rapporte un item depuis l'inventaire situé derrière l'inserter. */
    INBOUND,

    /** Le bras porte un item vers l'inventaire situé devant l'inserter. */
    OUTBOUND;

    private static final InserterSwingPhase[] VALUES = values();

    /**
     * Décode une phase reçue du réseau.
     *
     * <p>Un ordinal hors bornes vaut {@link #NONE} : une valeur corrompue ne doit pas
     * lever d'exception dans le chemin de rendu.
     */
    public static InserterSwingPhase byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) return NONE;

        return VALUES[ordinal];
    }
}
