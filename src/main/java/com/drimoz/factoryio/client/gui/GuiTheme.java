package com.drimoz.factoryio.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * La palette et la typographie de tous les écrans du mod (FIO-181). Voir docs/09 §4.
 *
 * <p>Deux fonds, deux chartes :
 * <ul>
 *   <li><b>sur la fenêtre</b> (gris clair vanilla) : texte sombre, sans ombre ;</li>
 *   <li><b>dans un onglet</b> (teinte soutenue) : texte clair, ombré.</li>
 * </ul>
 * Une couleur ne sert qu'à une chose. Le vert dit « ça marche », le rouge « ça ne marchera
 * pas sans vous » : jamais de rouge pour décorer.
 */
public final class GuiTheme {

    // Sur la fenêtre

    public static final int TEXT = 0x404040;
    public static final int TEXT_MUTED = 0x6E6E6E;
    public static final int TEXT_PROBLEM = 0xA8322A;

    // Dans un onglet

    public static final int TAB_LABEL = 0xD8D8D8;
    public static final int TAB_VALUE = 0xFFFFFF;
    public static final int TAB_MUTED = 0x9A9A9A;
    /** Interligne des onglets. */
    public static final int LINE = 10;

    // Couleur d'onglet = fonction : à gauche ce qui renseigne, à droite ce qui se règle.

    public static final int TAB_INFO = 0xC89420;
    public static final int TAB_AUGMENTS = 0x3C6FC8;
    public static final int TAB_CONTROL = 0xC03C2C;
    public static final int TAB_SETTINGS = 0x7C7C94;

    // Voiles

    /** Sur un item « fantôme » : ce qui va là, pas ce qui y est. */
    public static final int GHOST_WASH = 0xA0C6C6C6;
    /** Sur une case verrouillée. */
    public static final int LOCKED_WASH = 0xB0202020;
    public static final int HOVER_WASH = 0x80FFFFFF;

    /**
     * L'état d'une machine, en cinq teintes partagées : voyant du bandeau et texte de l'onglet
     * d'informations disent la même chose de la même couleur, quelle que soit la machine.
     */
    public enum Status {
        WORKING(0x90FF90),
        WAITING(0xFFFFA0),
        BLOCKED(0xFFC060),
        PROBLEM(0xFF8A80),
        OFF(0xB8B8B8);

        /** Teinte du texte, sur fond d'onglet. */
        public final int tabColour;

        Status(int tabColour) {
            this.tabColour = tabColour;
        }
    }

    private GuiTheme() {}

    // Texte

    public static void text(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x, y, TEXT, false);
    }

    public static void tabLabel(GuiGraphics graphics, Font font, Component text, int x, int y) {
        graphics.drawString(font, text, x, y, TAB_LABEL, true);
    }

    public static void tabValue(GuiGraphics graphics, Font font, Component text, int x, int y, int colour) {
        graphics.drawString(font, text, x, y, colour, true);
    }
}
