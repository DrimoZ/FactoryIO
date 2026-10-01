package com.drimoz.factoryio.shared;

/**
 * La grille commune à tous les écrans du mod (FIO-181). Voir docs/09 §4.
 *
 * <p>Hors de {@code client/} parce que les menus en ont besoin : une position de slot se fixe à
 * la construction du menu, des deux côtés. Les couleurs, elles, sont dans
 * {@code client/gui/GuiTheme}.
 *
 * <p><b>Tout s'aligne sur les colonnes de l'inventaire du joueur</b> : cadres de slot en
 * {@code 7 + 18 × n}. La jauge d'énergie occupe la colonne 0, le contenu de la machine commence
 * colonne 1. C'est ce qui donne aux écrans leur air d'ensemble.
 */
public final class GuiMetrics {

    // Fenêtre

    public static final int WIDTH = 176;
    public static final int MARGIN = 8;
    /** Bandeau : le titre, et le voyant d'état à droite. */
    public static final int TITLE_Y = 7;
    public static final int CONTENT_TOP = 20;

    // Pièces

    public static final int SLOT = 18;
    /** Le grand socle du « sujet » de l'écran : la main d'un inserter, la recette d'un crafter. */
    public static final int SOCKET = 26;

    /** Colonne n de l'inventaire : bord gauche du cadre de slot. */
    public static int column(int n) {
        return MARGIN - 1 + n * SLOT;
    }

    /** Jauge d'énergie : centrée dans la colonne 0, sur toute la hauteur du contenu. */
    public static final int GAUGE_WIDTH = 14;
    public static final int GAUGE_X = column(0) + (SLOT - GAUGE_WIDTH) / 2;

    public static final int ARROW_WIDTH = 20;
    public static final int ARROW_HEIGHT = 13;

    public static final int LED_SIZE = 7;
    public static final int LED_X = WIDTH - MARGIN - LED_SIZE;
    public static final int LED_Y = TITLE_Y;

    // Inventaire du joueur, sous le contenu

    public static int inventoryY(int contentBottom) {
        return contentBottom + 15;
    }

    public static int inventoryLabelY(int contentBottom) {
        return inventoryY(contentBottom) - 11;
    }

    public static int hotbarY(int contentBottom) {
        return inventoryY(contentBottom) + 58;
    }

    public static int height(int contentBottom) {
        return hotbarY(contentBottom) + SLOT + 6;
    }

    // Onglets latéraux

    /** Ordonnée du premier onglet, de chaque côté. */
    public static final int TAB_TOP = 4;
    /** Côté d'un onglet fermé, et hauteur de son en-tête une fois ouvert. */
    public static final int TAB_HEADER = 22;
    public static final int TAB_PADDING = 6;
    public static final int TAB_GAP = 2;

    private GuiMetrics() {}
}
