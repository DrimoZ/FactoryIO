package com.drimoz.factoryio.core.inserters;

/**
 * Ce que la machine fait de son animation (FIO-161).
 *
 * <p>Trois positions plutôt que deux. Une bascule oui/non n'avait pas de bonne réponse :
 * couper l'interpolation gardait l'information mais laissait le bras sauter, et l'immobiliser
 * rendait indiscernables un inserter bloqué, un inserter au repos et un inserter au travail.
 * Ce sont deux besoins différents, ils méritent deux positions.
 */
public enum InserterAnimationMode {

    /** Mouvement continu. */
    SMOOTH("animation_smooth"),

    /**
     * Le bras saute d'une pose à l'autre.
     *
     * <p>Pour les modèles rapides : un {@code fast_inserter} sous module de vitesse fait un
     * demi-tour en cent millisecondes, soit six images. On garde l'information — de quel côté
     * est le bras, que tient-il — et on supprime le flou.
     */
    SNAP("animation_fast"),

    /** Rien ne bouge. Le bras reste dans la pose sculptée du modèle. */
    OFF("animation_off");

    private static final InserterAnimationMode[] VALUES = values();

    private final String translationKey;

    InserterAnimationMode(String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Décode un mode reçu du réseau ou d'une sauvegarde.
     *
     * <p>Une valeur hors bornes vaut {@link #SMOOTH} : c'est aussi le défaut d'un monde
     * antérieur à ce réglage, où l'octet est absent et vaut donc zéro. Les inserters déjà
     * posés continuent de bouger.
     */
    public static InserterAnimationMode byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= VALUES.length) return SMOOTH;

        return VALUES[ordinal];
    }

    public InserterAnimationMode next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public String translationKey() {
        return this.translationKey;
    }

    /** @return {@code true} si le mouvement doit être interpolé entre ses deux extrémités */
    public boolean isInterpolated() {
        return this == SMOOTH;
    }

    /** @return {@code true} si la machine doit rester strictement immobile */
    public boolean isFrozen() {
        return this == OFF;
    }
}
