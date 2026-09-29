package com.drimoz.factoryio.client.gui;

import com.drimoz.factoryio.FactoryIO;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * La planche {@code textures/gui/widgets.png} et la façon d'en dessiner chaque pièce (FIO-071).
 *
 * <p>Les emplacements recopient {@code tools/gui-sprites.js}, qui génère la planche : changer
 * l'un impose de changer l'autre. Aucune autre classe ne connaît une coordonnée de texture —
 * c'est ce qui manquait à l'ancien écran, où chaque widget portait ses {@code u, v} en dur.
 *
 * <p>La planche fait 256×256 : c'est le format que supposent les {@code blit} de vanilla qui
 * ne prennent pas de taille de texture, {@code blitNineSliced} compris.
 */
public final class GuiSprites {

    public static final ResourceLocation SHEET = new ResourceLocation(FactoryIO.MOD_ID, "textures/gui/widgets.png");

    private GuiSprites() {}

    // Cadres étirables

    private static final int PANEL_U = 0;
    private static final int TAB_U = 16;
    private static final int BUTTON_U = 32;
    private static final int BUTTON_HOVER_U = 48;
    private static final int BUTTON_PRESSED_U = 64;
    private static final int BUTTON_DISABLED_U = 80;
    private static final int INSET_U = 96;
    private static final int FRAME_V = 0;
    private static final int FRAME_SIZE = 16;

    /** Fenêtre principale, dans le ton des conteneurs vanilla. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitNineSliced(SHEET, x, y, width, height, 4, 4, FRAME_SIZE, FRAME_SIZE, PANEL_U, FRAME_V);
    }

    /** Onglet latéral, teinté à la couleur de sa fonction. */
    public static void tab(GuiGraphics graphics, int x, int y, int width, int height, int rgb) {
        graphics.setColor(red(rgb), green(rgb), blue(rgb), 1f);
        graphics.blitNineSliced(SHEET, x, y, width, height, 4, 4, FRAME_SIZE, FRAME_SIZE, TAB_U, FRAME_V);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    public enum ButtonState { NORMAL, HOVERED, PRESSED, DISABLED }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height, ButtonState state) {
        int u = switch (state) {
            case NORMAL -> BUTTON_U;
            case HOVERED -> BUTTON_HOVER_U;
            case PRESSED -> BUTTON_PRESSED_U;
            case DISABLED -> BUTTON_DISABLED_U;
        };
        graphics.blitNineSliced(SHEET, x, y, width, height, 3, 3, FRAME_SIZE, FRAME_SIZE, u, FRAME_V);
    }

    /** Zone en creux d'un pixel : jauges, cadres de texte. */
    public static void inset(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blitNineSliced(SHEET, x, y, width, height, 1, 1, FRAME_SIZE, FRAME_SIZE, INSET_U, FRAME_V);
    }

    // Slots et jauges

    public static final int SLOT_SIZE = 18;
    public static final int BIG_SLOT_SIZE = 26;

    /** Socle d'un slot dont l'item est en {@code (itemX, itemY)}. */
    public static void slot(GuiGraphics graphics, int itemX, int itemY) {
        graphics.blit(SHEET, itemX - 1, itemY - 1, 0, 16, SLOT_SIZE, SLOT_SIZE);
    }

    public static void bigSlot(GuiGraphics graphics, int x, int y) {
        graphics.blit(SHEET, x, y, 18, 16, BIG_SLOT_SIZE, BIG_SLOT_SIZE);
    }

    private static final int ENERGY_U = 44;
    private static final int ENERGY_EMPTY_U = 100;
    private static final int ENERGY_V = 16;
    private static final int ENERGY_WIDTH = 12;
    private static final int ENERGY_HEIGHT = 48;

    /**
     * Jauge verticale remplie par le bas, sur toute la hauteur donnée.
     *
     * @param fill part remplie, de 0 à 1
     */
    public static void energyGauge(GuiGraphics graphics, int x, int y, int width, int height, float fill) {
        inset(graphics, x, y, width, height);

        int inner = height - 2;

        // Segments éteints sur toute la hauteur : vide, la jauge reste lisible comme une jauge.
        graphics.blit(SHEET, x + 1, y + 1, width - 2, inner,
                ENERGY_EMPTY_U, ENERGY_V + ENERGY_HEIGHT - Math.min(inner, ENERGY_HEIGHT),
                ENERGY_WIDTH, Math.min(inner, ENERGY_HEIGHT), 256, 256);

        int filled = Math.round(inner * Math.max(0f, Math.min(1f, fill)));
        if (filled <= 0) return;

        // Le remplissage est pris par le bas du sprite : ses segments restent calés sur le
        // fond de la jauge, quelle que soit sa hauteur.
        int visible = Math.min(filled, ENERGY_HEIGHT);
        graphics.blit(SHEET, x + 1, y + 1 + inner - filled, width - 2, filled,
                ENERGY_U, ENERGY_V + ENERGY_HEIGHT - visible, ENERGY_WIDTH, visible, 256, 256);
    }

    public static final int FLAME_SIZE = 14;

    /** Flamme du burner : éteinte, puis allumée par le bas à hauteur de la réserve. */
    public static void flame(GuiGraphics graphics, int x, int y, float fill) {
        graphics.blit(SHEET, x, y, 70, 16, FLAME_SIZE, FLAME_SIZE);

        int lit = Math.round(FLAME_SIZE * Math.max(0f, Math.min(1f, fill)));
        if (lit <= 0) return;

        graphics.blit(SHEET, x, y + FLAME_SIZE - lit, 56, 16 + FLAME_SIZE - lit, FLAME_SIZE, lit);
    }

    public static final int ARROW_WIDTH = 16;
    public static final int ARROW_HEIGHT = 11;

    /**
     * Flèche de trajet, remplie de gauche à droite à hauteur de {@code progress} : elle suit le
     * bras, comme la flèche de cuisson d'un four suit la cuisson.
     */
    public static void arrow(GuiGraphics graphics, int x, int y, float progress) {
        graphics.blit(SHEET, x, y, 84, 16, ARROW_WIDTH, ARROW_HEIGHT);

        int filled = Math.round(ARROW_WIDTH * Math.max(0f, Math.min(1f, progress)));
        if (filled <= 0) return;

        graphics.blit(SHEET, x, y, 84, 28, filled, ARROW_HEIGHT);
    }

    /** Icône estompée : un rappel de ce qui va là, pas un contenu. */
    public static void faintIcon(GuiGraphics graphics, Icon icon, int x, int y) {
        RenderSystem.enableBlend();
        graphics.setColor(1f, 1f, 1f, 0.3f);
        icon(graphics, icon, x, y);
        graphics.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // Icônes 12×12

    public static final int ICON_SIZE = 12;

    /** Rang des icônes sur la planche, dans l'ordre où le script les dessine. */
    public enum Icon {
        POWER_ON, POWER_OFF,
        ANIMATION_SMOOTH, ANIMATION_SNAP, ANIMATION_OFF,
        WHITELIST, BLACKLIST,
        INFO, SETTINGS,
        LANE_AUTO, LANE_NEAR, LANE_FAR,
        LOCK, MINUS, PLUS, HAND
    }

    public static void icon(GuiGraphics graphics, Icon icon, int x, int y) {
        graphics.blit(SHEET, x, y, icon.ordinal() * ICON_SIZE, 64, ICON_SIZE, ICON_SIZE);
    }

    // Inner work

    private static float red(int rgb) {
        return ((rgb >> 16) & 0xFF) / 255f;
    }

    private static float green(int rgb) {
        return ((rgb >> 8) & 0xFF) / 255f;
    }

    private static float blue(int rgb) {
        return (rgb & 0xFF) / 255f;
    }
}
