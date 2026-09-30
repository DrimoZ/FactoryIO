package com.drimoz.factoryio.shared;

/**
 * Mesures des onglets latéraux, communes à toutes les fenêtres du mod.
 *
 * <p>Hors de {@code client/} parce que les menus en ont besoin aussi : un slot rangé dans
 * un onglet est placé côté serveur à partir de ces mêmes valeurs.
 */
public final class SideTabMetrics {

    /** Ordonnée du premier onglet, de chaque côté. */
    public static final int TOP = 4;
    /** Côté d'un onglet fermé, et hauteur de son en-tête une fois ouvert. */
    public static final int HEADER = 22;
    public static final int PADDING = 6;
    public static final int GAP = 2;

    private SideTabMetrics() {}
}
