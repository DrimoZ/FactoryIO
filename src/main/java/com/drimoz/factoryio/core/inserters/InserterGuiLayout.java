package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.shared.GuiMetrics;
import com.drimoz.factoryio.core.model.Inserter;

/**
 * Géométrie de l'écran d'inserter, calculée depuis ce que le type possède (FIO-071).
 *
 * <p><b>Pourquoi en code commun.</b> Les positions de slots sont fixées à la construction du
 * menu, qui existe des deux côtés : le serveur en a besoin pour les construire, le client pour
 * dessiner leur socle au même endroit. Tant que les coordonnées vivaient à moitié dans le menu
 * ({@code (124, 45)}, {@code 8 + i * 18}) et à moitié dans une texture figée, rien ne garantissait
 * qu'elles coïncident. Ici il n'y a qu'une source, et elle se teste sans monde.
 *
 * <p>Même rôle que {@link InserterSlotLayout} pour les index : celui-ci dit <i>quel</i> slot,
 * celui-là dit <i>où</i>.
 *
 * <h2>Disposition</h2>
 *
 * <pre>
 *  ┌──────────────────────────────────────────┐  bandeau : le titre seul
 *  │ ▌  [s] ▶ [ main ] ▶ [c]                  │  alimentation à gauche, trajet au centre
 *  │ ▌   [f][f][f][f][f] [≡]                  │  filtres et leur mode, s'il y en a
 *  │ inventaire du joueur                     │
 *  └──────────────────────────────────────────┘
 * </pre>
 *
 * <p>Tout ce qui n'est pas au cœur du travail de la machine — améliorations, marche et
 * condition redstone, réglages, informations — vit dans des onglets latéraux, à la manière de
 * Thermal.
 * Les slots d'amélioration sont dans le premier onglet de droite, dont la position ne dépend
 * donc d'aucun autre : c'est ce qui permet de la fixer ici.
 *
 * <p>Les coordonnées de slot désignent, comme dans vanilla, le coin de l'item (16×16) ; son
 * socle se dessine un pixel plus haut et plus à gauche.
 */
public record InserterGuiLayout(
        boolean usesEnergy, boolean filterable, int upgradeCount, boolean redstone) {

    // Fenêtre

    public static final int WIDTH = GuiMetrics.WIDTH;

    /** Hauteur du bandeau, qui ne porte que le titre. */
    public static final int CONTENT_TOP = GuiMetrics.CONTENT_TOP;
    public static final int TITLE_Y = GuiMetrics.TITLE_Y;

    public static final int SLOT = GuiMetrics.SLOT;
    public static final int HAND_SOCKET = GuiMetrics.SOCKET;

    /**
     * Hauteur du contenu, <b>la même pour tous les types</b> : celle d'un inserter filtrant, le
     * plus chargé. Passer d'un inserter à l'autre ne doit pas faire sauter la fenêtre ni
     * l'inventaire du joueur ; un inserter sans filtres laisse simplement la rangée vide.
     */
    private static final int CONTENT_HEIGHT = 48;

    public static InserterGuiLayout of(Inserter inserter) {
        return new InserterGuiLayout(
                inserter.useEnergy(), inserter.isFilterable(), inserter.getUpgradeSlots(),
                inserter.isAffectedByRedstone());
    }

    // Interface (Fenêtre)

    public int contentHeight() {
        return CONTENT_HEIGHT;
    }

    public int contentBottom() {
        return CONTENT_TOP + contentHeight();
    }

    public int inventoryY() {
        return GuiMetrics.inventoryY(contentBottom());
    }

    public int inventoryLabelY() {
        return inventoryY() - 11;
    }

    public int hotbarY() {
        return inventoryY() + 58;
    }

    public int height() {
        return hotbarY() + SLOT + 6;
    }

    // Interface (Alimentation, colonne de gauche)

    /** Colonne d'alimentation, alignée sur la première colonne de l'inventaire. */
    public static final int POWER_X = 8;
    public static final int GAUGE_X = GuiMetrics.GAUGE_X;
    public static final int GAUGE_WIDTH = GuiMetrics.GAUGE_WIDTH;

    /** Jauge d'énergie : toute la hauteur du contenu. */
    public int gaugeHeight() {
        return contentHeight();
    }

    /** Flamme du burner, au-dessus de son slot. */
    public static final int FLAME_X = POWER_X + 1;
    public static final int FLAME_Y = CONTENT_TOP;
    public static final int FLAME_SIZE = 14;

    public int fuelSlotX() {
        return POWER_X;
    }

    public int fuelSlotY() {
        return CONTENT_TOP + FLAME_SIZE + 3;
    }

    // Interface (Trajet : source ▶ main ▶ cible)

    public static final int CENTRE_X = WIDTH / 2;

    public int handSocketX() {
        return CENTRE_X - HAND_SOCKET / 2;
    }

    public int handSocketY() {
        return CONTENT_TOP;
    }

    public int handSlotX() {
        return handSocketX() + (HAND_SOCKET - 16) / 2;
    }

    public int handSlotY() {
        return handSocketY() + (HAND_SOCKET - 16) / 2;
    }

    public static final int ARROW_WIDTH = GuiMetrics.ARROW_WIDTH;
    public static final int ARROW_HEIGHT = GuiMetrics.ARROW_HEIGHT;

    public int arrowY() {
        return handSocketY() + (HAND_SOCKET - ARROW_HEIGHT) / 2;
    }

    public int inArrowX() {
        return handSocketX() - 6 - ARROW_WIDTH;
    }

    public int outArrowX() {
        return handSocketX() + HAND_SOCKET + 6;
    }

    /**
     * Socles de la source et de la cible, aux deux bouts du trajet : l'icône du bloc réellement
     * visé. Des flèches qui ne pointent sur rien ne disent pas d'où l'inserter prend ni où il
     * dépose — et c'est la première chose à vérifier devant un inserter qui ne fait rien.
     *
     * <p>Coordonnées du socle, pas d'un item : ce ne sont pas des slots.
     */
    public int sourceSocketX() {
        return inArrowX() - 4 - SLOT;
    }

    public int targetSocketX() {
        return outArrowX() + ARROW_WIDTH + 4;
    }

    public int neighbourSocketY() {
        return handSocketY() + (HAND_SOCKET - SLOT) / 2;
    }

    // Interface (Filtres)

    public int filterSlotX(int index) {
        int total = InserterSlotLayout.FILTER_SLOT_COUNT * SLOT;
        return CENTRE_X - total / 2 + 1 + index * SLOT;
    }

    public int filterSlotY() {
        return handSocketY() + HAND_SOCKET + 4 + 1;
    }

    /**
     * Bouton liste blanche / liste noire, au bout de la rangée de filtres : il règle ces
     * filtres-là, il est donc à côté d'eux et non dans le bandeau.
     */
    public static final int LIST_BUTTON_SIZE = SLOT;

    public int listButtonX() {
        return filterSlotX(InserterSlotLayout.FILTER_SLOT_COUNT - 1) + 16 + 1 + 4;
    }

    public int listButtonY() {
        return filterSlotY() - 1;
    }

    // Interface (Onglet des améliorations, premier à droite)

    public boolean hasAugmentTab() {
        return upgradeCount > 0;
    }

    public static int augmentSlotX(int index) {
        return WIDTH + GuiMetrics.TAB_PADDING + 1 + index * SLOT;
    }

    public static int augmentSlotY() {
        return GuiMetrics.TAB_TOP + GuiMetrics.TAB_HEADER + 1;
    }

    /** Largeur de l'onglet ouvert : de quoi loger quatre modules et leur effet. */
    public static final int AUGMENT_TAB_WIDTH = 140;
}
