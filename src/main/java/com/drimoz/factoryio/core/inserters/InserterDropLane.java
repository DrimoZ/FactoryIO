package com.drimoz.factoryio.core.inserters;

/**
 * Voie de dépose choisie pour un inserter qui livre sur un convoyeur.
 *
 * <p>Par défaut la bande décide seule : elle présente la voie lointaine d'abord, et la proche
 * en recours sauf si le serveur impose la parité stricte ({@code insert_on_far_lane_only},
 * FIO-166). Un choix explicite passe outre ce réglage, dans les deux sens : c'est une
 * décision de montage, prise bloc par bloc, et la configuration du serveur n'en est que le
 * défaut.
 *
 * <p>Sans effet sur tout ce qui n'est pas un convoyeur, et sur un convoyeur abordé par
 * l'avant ou l'arrière — il n'y a alors pas de voie proche.
 */
public enum InserterDropLane {

    /** La bande décide, selon la configuration du serveur. */
    AUTO("lane_auto"),

    /** La voie du côté de l'inserter, seulement. */
    NEAR("lane_near"),

    /** La voie opposée, seulement : la règle de Factorio. */
    FAR("lane_far");

    private static final InserterDropLane[] VALUES = values();

    private final String translationKey;

    InserterDropLane(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Une valeur hors bornes vaut {@link #AUTO} : c'est aussi ce que lit un monde antérieur à
     * ce réglage, où l'octet est absent et vaut donc zéro.
     */
    public static InserterDropLane byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) return AUTO;

        return VALUES[ordinal];
    }

    public String translationKey() {
        return this.translationKey;
    }
}
