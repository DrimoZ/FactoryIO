package com.drimoz.factoryio.client.gui;

import com.drimoz.factoryio.core.inserters.InserterGuiLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Onglet latéral, à la manière de Thermal (FIO-071).
 *
 * <p>Fermé, c'est un carré coloré portant une icône ; ouvert, il glisse jusqu'à sa taille et
 * montre son contenu. La couleur dit la fonction, et le côté la nature : à gauche ce qui
 * renseigne, à droite ce qui se règle.
 *
 * <p>Le contenu n'est dessiné et cliquable qu'une fois l'onglet <b>entièrement</b> ouvert :
 * un bouton qui glisse sous le curseur pendant l'animation recevrait des clics destinés à
 * autre chose.
 */
public abstract class SideTab {

    public enum Side { LEFT, RIGHT }

    /** Durée d'ouverture, en millisecondes. */
    private static final float OPENING_MS = 160f;

    private final String id;
    private final Side side;
    private final int colour;

    private boolean open;
    private float openness;

    // Position absolue de l'image courante, posée par SideTabs.
    int x;
    int y;

    protected SideTab(String id, Side side, int colour) {
        this.id = id;
        this.side = side;
        this.colour = colour;
    }

    // Contenu, fourni par chaque onglet

    protected abstract Component title();

    /** Icône de l'en-tête, dans un carré de 16×16. */
    protected abstract void renderIcon(GuiGraphics graphics, int x, int y);

    protected abstract int contentWidth();

    protected abstract int contentHeight();

    /** {@code x, y} : origine du contenu, sous l'en-tête. */
    protected abstract void renderContent(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY);

    /** @return {@code true} si le clic a été pris */
    protected boolean contentClicked(int x, int y, double mouseX, double mouseY, int button) {
        return false;
    }

    protected List<Component> contentTooltip(int x, int y, double mouseX, double mouseY) {
        return List.of();
    }

    /** Appelé à chaque image : l'onglet des améliorations y montre ou cache ses slots. */
    protected void onOpennessChanged(boolean fullyOpen) {}

    // Interface

    public String id() {
        return this.id;
    }

    public Side side() {
        return this.side;
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean isFullyOpen() {
        return this.open && this.openness >= 1f;
    }

    void setOpen(boolean open) {
        this.open = open;
    }

    /** Ouvre sans animation : l'onglet mémorisé est déjà ouvert quand l'écran apparaît. */
    void snapOpen() {
        this.open = true;
        this.openness = 1f;
    }

    void animate(float elapsedMs) {
        float target = this.open ? 1f : 0f;
        float step = elapsedMs / OPENING_MS;

        this.openness = this.openness < target
                ? Math.min(target, this.openness + step)
                : Math.max(target, this.openness - step);

        onOpennessChanged(isFullyOpen());
    }

    public int width() {
        return Math.round(Mth.lerp(eased(), InserterGuiLayout.TAB_HEADER, fullWidth()));
    }

    public int height() {
        return Math.round(Mth.lerp(eased(), InserterGuiLayout.TAB_HEADER, fullHeight()));
    }

    /** Assez large pour le contenu, et pour le titre : il ne déborde jamais de son onglet. */
    private int fullWidth() {
        int titled = TITLE_X + Minecraft.getInstance().font.width(title()) + InserterGuiLayout.TAB_PADDING;

        return Math.max(titled, contentWidth() + 2 * InserterGuiLayout.TAB_PADDING);
    }

    /** Abscisse du titre dans l'en-tête, après l'icône. */
    private static final int TITLE_X = 24;

    private int fullHeight() {
        return InserterGuiLayout.TAB_HEADER + contentHeight() + InserterGuiLayout.TAB_PADDING;
    }

    /** Ralentit en fin de course : l'onglet se pose au lieu de s'arrêter net. */
    private float eased() {
        float t = this.openness;
        return 1f - (1f - t) * (1f - t);
    }

    // Géométrie absolue

    /**
     * Le rectangle dessiné déborde de quatre pixels sous la fenêtre principale, dessinée
     * par-dessus : l'onglet semble en sortir plutôt que d'y être accolé.
     */
    int drawX() {
        return this.side == Side.RIGHT ? this.x - 4 : this.x - width();
    }

    int drawWidth() {
        return width() + 4;
    }

    public int left() {
        return this.side == Side.RIGHT ? this.x : this.x - width();
    }

    public int top() {
        return this.y;
    }

    int contentX() {
        return left() + InserterGuiLayout.TAB_PADDING;
    }

    int contentY() {
        return this.y + InserterGuiLayout.TAB_HEADER;
    }

    boolean contains(double mouseX, double mouseY) {
        return mouseX >= left() && mouseX < left() + width() && mouseY >= this.y && mouseY < this.y + height();
    }

    boolean headerContains(double mouseX, double mouseY) {
        return contains(mouseX, mouseY) && mouseY < this.y + InserterGuiLayout.TAB_HEADER;
    }

    void renderBackground(GuiGraphics graphics) {
        GuiSprites.tab(graphics, drawX(), this.y, drawWidth(), height(), this.colour);
    }

    void renderForeground(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int iconX = left() + (InserterGuiLayout.TAB_HEADER - 16) / 2;
        int iconY = this.y + (InserterGuiLayout.TAB_HEADER - 16) / 2;
        renderIcon(graphics, iconX, iconY);

        if (!isFullyOpen()) return;

        graphics.drawString(font, title(), left() + TITLE_X, this.y + 7, 0xFFFFFF, true);
        renderContent(graphics, font, contentX(), contentY(), mouseX, mouseY);
    }
}
