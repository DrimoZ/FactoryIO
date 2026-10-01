package com.drimoz.factoryio.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Petit bouton de la nouvelle interface (FIO-071) : une icône ou un libellé court, un état
 * « enfoncé » pour le choix courant, une infobulle.
 *
 * <p>Volontairement pas un {@code AbstractWidget} vanilla : ces boutons vivent aussi dans des
 * onglets qui glissent, s'ouvrent et se referment, et un widget enregistré auprès de l'écran
 * garde sa position et reste cliquable même caché. Ici la position est relative à une origine
 * donnée à chaque image, et un bouton qu'on ne dessine pas n'existe pas.
 *
 * <p>Le clic n'applique rien localement : les réglages partent au serveur, qui fait autorité et
 * renvoie l'état — le bouton se contente de relire ses fournisseurs.
 */
public final class IconButton {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private Supplier<GuiSprites.Icon> icon = () -> null;
    private Supplier<Component> label = () -> null;
    private BooleanSupplier pressed = () -> false;
    private BooleanSupplier active = () -> true;
    private BooleanSupplier visible = () -> true;
    private Supplier<List<Component>> tooltip = List::of;
    private Runnable action = () -> {};

    public IconButton(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Configuration

    public IconButton icon(Supplier<GuiSprites.Icon> icon) {
        this.icon = icon;
        return this;
    }

    public IconButton label(Supplier<Component> label) {
        this.label = label;
        return this;
    }

    /** Le choix que ce bouton représente est-il celui en vigueur ? */
    public IconButton pressed(BooleanSupplier pressed) {
        this.pressed = pressed;
        return this;
    }

    public IconButton active(BooleanSupplier active) {
        this.active = active;
        return this;
    }

    /** Un bouton caché n'est ni dessiné, ni cliquable, ni survolable. */
    public IconButton visible(BooleanSupplier visible) {
        this.visible = visible;
        return this;
    }

    public IconButton tooltip(Supplier<List<Component>> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public IconButton onPress(Runnable action) {
        this.action = action;
        return this;
    }

    // Interface

    public void render(GuiGraphics graphics, Font font, int originX, int originY, int mouseX, int mouseY) {
        if (!this.visible.getAsBoolean()) return;

        int left = originX + this.x;
        int top = originY + this.y;

        GuiSprites.ButtonState state;
        if (!this.active.getAsBoolean()) state = GuiSprites.ButtonState.DISABLED;
        else if (this.pressed.getAsBoolean()) state = GuiSprites.ButtonState.PRESSED;
        else if (isOver(originX, originY, mouseX, mouseY)) state = GuiSprites.ButtonState.HOVERED;
        else state = GuiSprites.ButtonState.NORMAL;

        GuiSprites.button(graphics, left, top, this.width, this.height, state);

        GuiSprites.Icon glyph = this.icon.get();
        Component text = this.label.get();
        int colour = this.active.getAsBoolean() ? 0xFFFFFF : 0xA0A0A0;
        int iconY = top + (this.height - GuiSprites.ICON_SIZE) / 2;
        int textY = top + (this.height - 8) / 2;

        // Icône seule : centrée. Libellé seul : centré. Les deux : l'icône mène, le texte suit.
        if (glyph != null && text != null) {
            GuiSprites.icon(graphics, glyph, left + 3, iconY);
            graphics.drawString(font, text, left + 3 + GuiSprites.ICON_SIZE + 4, textY, colour, true);
        } else if (glyph != null) {
            GuiSprites.icon(graphics, glyph, left + (this.width - GuiSprites.ICON_SIZE) / 2, iconY);
        } else if (text != null) {
            graphics.drawCenteredString(font, text, left + this.width / 2, textY, colour);
        }
    }

    public boolean isOver(int originX, int originY, double mouseX, double mouseY) {
        if (!this.visible.getAsBoolean()) return false;

        int left = originX + this.x;
        int top = originY + this.y;

        return mouseX >= left && mouseX < left + this.width && mouseY >= top && mouseY < top + this.height;
    }

    /** @return {@code true} si le clic a été pris */
    public boolean mouseClicked(int originX, int originY, double mouseX, double mouseY) {
        if (!isOver(originX, originY, mouseX, mouseY) || !this.active.getAsBoolean()) return false;

        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        this.action.run();
        return true;
    }

    /** @return l'infobulle si la souris survole le bouton, sinon une liste vide */
    public List<Component> tooltipAt(int originX, int originY, double mouseX, double mouseY) {
        return isOver(originX, originY, mouseX, mouseY) ? this.tooltip.get() : List.of();
    }
}
