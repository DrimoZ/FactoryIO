package com.drimoz.factoryio.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Onglet à boutons : rendu, clic et infobulle délégués à la liste. */
public abstract class ButtonTab extends SideTab {

    protected final List<IconButton> buttons = new ArrayList<>();

    protected ButtonTab(String id, Side side, int colour) {
        super(id, side, colour);
    }

    protected void renderButtons(GuiGraphics graphics, Font font, int x, int y, int mouseX, int mouseY) {
        for (IconButton button : this.buttons) button.render(graphics, font, x, y, mouseX, mouseY);
    }

    @Override
    protected boolean contentClicked(int x, int y, double mouseX, double mouseY, int button) {
        for (IconButton b : this.buttons) {
            if (b.mouseClicked(x, y, mouseX, mouseY)) return true;
        }
        return false;
    }

    @Override
    protected List<Component> contentTooltip(int x, int y, double mouseX, double mouseY) {
        for (IconButton b : this.buttons) {
            List<Component> lines = b.tooltipAt(x, y, mouseX, mouseY);
            if (!lines.isEmpty()) return lines;
        }
        return List.of();
    }
}
