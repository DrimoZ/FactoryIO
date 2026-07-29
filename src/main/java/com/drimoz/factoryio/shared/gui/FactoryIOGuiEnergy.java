package com.drimoz.factoryio.shared.gui;

import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Jauge d'énergie verticale, remplie par le bas. */
public class FactoryIOGuiEnergy {

    // Private properties

    private final int left;
    private final int top;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int u;
    private final int v;

    // Life cycle

    public FactoryIOGuiEnergy(int left, int top, int x, int y, int width, int height, int u, int v) {
        this.left = left;
        this.top = top;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
    }

    // Interface

    public void render(GuiGraphics graphics, ResourceLocation texture, int scaled) {
        graphics.blit(
                texture,
                left + x, top + y + height - scaled,
                u, v - scaled,
                width, scaled + 1);
    }

    public void renderTooltip(GuiGraphics graphics, Font font, int mouseX, int mouseY, int energy, int capacity, boolean condition) {
        if (!condition) return;
        if (!hovering(mouseX, mouseY)) return;

        graphics.renderTooltip(font, StringHelper.displayEnergy(energy, capacity), mouseX, mouseY);
    }

    public boolean hovering(double mouseX, double mouseY) {
        return mouseX >= left + x && mouseX <= left + x + width
                && mouseY >= top + y && mouseY <= top + y + height;
    }
}
