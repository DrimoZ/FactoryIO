package com.drimoz.factoryio.core.belts;

import net.minecraft.core.Direction;

/**
 * Forme visible d'un convoyeur : la valeur de la propriété {@code connected}, déduite de ses
 * voisins.
 *
 * <h2>Ce que {@code connected} encode réellement</h2>
 *
 * <p>Relevé sur les modèles, pas sur leurs noms : une <b>forme</b> — droit, virage gauche,
 * virage droit — et une <b>paire de raccords</b>, un à l'entrée et un à la sortie. Les huit
 * valeurs sont exactement les combinaisons réalisables.
 *
 * <p>Un virage a toujours une entrée : c'est elle qui le définit. Il n'existe donc pas de
 * « virage sans raccord d'entrée », et c'est pourquoi il n'y a que deux variantes par côté au
 * lieu de quatre.
 *
 * <h2>Le virage appartient à la bande qui reçoit</h2>
 *
 * <p>Une bande n'a qu'un {@code facing}, qui est sa <b>sortie</b>. Dans un coude, la tuile où
 * la direction change pointe vers la nouvelle direction et reçoit par le côté : c'est elle qui
 * se courbe. L'amont reste droit et pointe simplement vers elle.
 *
 * <p>D'où la règle qui décide de tout : <b>un virage se déduit quand l'unique entrée est
 * latérale</b>. Deux entrées, ou une entrée arrière, donnent une bande droite — un T, où la
 * bande latérale bute contre celle qui passe.
 *
 * <h2>Pourquoi ici et pas dans le bloc</h2>
 *
 * <p>Rien de tout cela ne demande le monde : quatre booléens et une orientation. Le bloc se
 * charge de lire ses voisins, cette classe de trancher — et c'est la partie qui se teste.
 */
public enum BeltShape {

    /** Bande droite : entrée arrière, ou plusieurs entrées, ou aucune. */
    STRAIGHT(0, 3, 2, 1),

    /** Virage recevant par la gauche. */
    CURVE_LEFT(BeltShape.NO_VARIANT, 4, BeltShape.NO_VARIANT, 5),

    /** Virage recevant par la droite. */
    CURVE_RIGHT(BeltShape.NO_VARIANT, 6, BeltShape.NO_VARIANT, 7);

    /** Combinaison qu'aucun modèle ne représente — un virage sans entrée n'existe pas. */
    private static final int NO_VARIANT = -1;

    /** Valeur maximale de la propriété {@code connected}. */
    public static final int MAX_CONNECTED = 7;

    private final int bare;
    private final int inputOnly;
    private final int outputOnly;
    private final int both;

    BeltShape(int bare, int inputOnly, int outputOnly, int both) {
        this.bare = bare;
        this.inputOnly = inputOnly;
        this.outputOnly = outputOnly;
        this.both = both;
    }

    // Interface

    /**
     * Valeur de {@code connected} pour cette forme et ces raccords.
     *
     * <p>Une combinaison sans modèle — un virage sans entrée — retombe sur la bande droite
     * plutôt que de lever : elle ne peut pas être produite par {@link #of}, et un état posé à
     * la main dans une commande ne doit pas casser le rendu.
     */
    public int connected(boolean hasInput, boolean hasOutput) {
        int value = hasInput
                ? (hasOutput ? this.both : this.inputOnly)
                : (hasOutput ? this.outputOnly : this.bare);

        return value == NO_VARIANT ? STRAIGHT.connected(hasInput, hasOutput) : value;
    }

    // Interface (Statique)

    /** Le côté gauche d'une bande, vu depuis sa sortie. */
    public static Direction leftOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Le côté droit d'une bande, vu depuis sa sortie. */
    public static Direction rightOf(Direction facing) {
        return facing.getClockWise();
    }

    /**
     * Forme à afficher, d'après les entrées effectivement raccordées.
     *
     * <p>« Raccordée » veut dire qu'un convoyeur <b>débouche</b> réellement de ce côté, ce que
     * seul {@link BeltSlope#feeds} établit. Un voisin qui se contente d'occuper la place n'y
     * change rien.
     *
     * @param canCurve {@code false} en pente : une rampe est forcément droite, un élément de
     *                 modèle n'admettant qu'une rotation et le virage ayant déjà pris la sienne
     */
    public static BeltShape of(boolean fromBack, boolean fromLeft, boolean fromRight, boolean canCurve) {
        if (!canCurve || fromBack) return STRAIGHT;

        // Deux entrées latérales forment une fusion, pas un coude : aucune des deux directions
        // ne l'emporte, la bande reste droite et les deux butent contre elle.
        if (fromLeft && fromRight) return STRAIGHT;

        if (fromLeft) return CURVE_LEFT;
        if (fromRight) return CURVE_RIGHT;

        return STRAIGHT;
    }

    /**
     * Valeur de {@code connected} complète, en une fois.
     *
     * <p>Le point d'entrée que le bloc appelle depuis {@code getStateForPlacement} et
     * {@code updateShape} — jamais depuis un tick.
     */
    public static int connectedOf(
            boolean fromBack, boolean fromLeft, boolean fromRight, boolean hasOutput, boolean canCurve) {

        boolean hasInput = fromBack || fromLeft || fromRight;

        return of(fromBack, fromLeft, fromRight, canCurve).connected(hasInput, hasOutput);
    }
}
