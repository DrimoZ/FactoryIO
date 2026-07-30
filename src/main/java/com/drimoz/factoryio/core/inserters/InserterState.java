package com.drimoz.factoryio.core.inserters;

/**
 * État du bras d'un inserter (FIO-060, cf. {@code 07-DESIGN-INSERTERS.md} §2).
 *
 * <p>Un cycle complet déplace une main d'items et dure deux mouvements : le bras part
 * chargé vers la cible, puis revient à vide. C'est le cycle de Factorio, et il remplace
 * le compteur de cooldown qui ne disait rien de ce que la machine était en train de faire.
 *
 * <pre>
 *   WAITING ──saisie──▶ SWINGING ──dépose──▶ RETURNING ──arrivée──▶ WAITING
 *                            │                    ▲
 *                    cible pleine                 │
 *                            ▼                    │
 *                        BLOCKED ────dépose───────┘
 * </pre>
 *
 * <p><b>Écart assumé avec le diagramme du document de design</b>, qui prévoyait aussi
 * {@code PICKING} et {@code DROPPING} : ces deux-là n'ont aucune durée — ce sont les
 * transitions elles-mêmes, pas des états. Les inscrire dans l'énumération aurait donné
 * des états traversés en zéro tick, donc jamais observables ni persistés, à contre-emploi
 * de l'objectif du ticket. Ils vivent ici comme méthodes : {@code tryPick} et
 * {@code tryDrop}. {@code BLOCKED}, en revanche, dure — et c'est précisément le
 * comportement que le design réclamait : rester bras tendu, item en main, au lieu de
 * remettre l'item dans un slot.
 */
public enum InserterState {

    /** Main vide, bras du côté de la source : l'inserter cherche à saisir. */
    WAITING,

    /** Main pleine, bras en route vers la cible. */
    SWINGING,

    /**
     * Main pleine, bras tendu au-dessus d'une cible qui refuse l'item.
     *
     * <p>L'item <b>reste en main</b> et le mouvement n'est pas rejoué : c'est à la fois
     * plus juste visuellement et plus simple que de le rendre au buffer pour le
     * reprendre ensuite.
     */
    BLOCKED,

    /** Main vide, bras en route vers la source. */
    RETURNING;

    private static final InserterState[] VALUES = values();

    /**
     * Décode un état reçu du réseau ou d'une sauvegarde.
     *
     * <p>Une valeur hors bornes vaut {@link #WAITING} : un octet corrompu doit remettre
     * l'inserter au repos, pas lever d'exception dans le chemin de rendu.
     */
    public static InserterState byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) return WAITING;

        return VALUES[ordinal];
    }

    /** @return {@code true} si le bras porte un item, et donc s'il faut l'afficher */
    public boolean isCarrying() {
        return this == SWINGING || this == BLOCKED;
    }

    /** @return {@code true} si le bras est en mouvement, et donc si l'énergie est dépensée */
    public boolean isMoving() {
        return this == SWINGING || this == RETURNING;
    }
}
