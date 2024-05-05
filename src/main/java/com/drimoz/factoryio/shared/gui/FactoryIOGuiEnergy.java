package com.drimoz.factoryio.shared.gui;

import com.drimoz.factoryio.shared.StringHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;

public class FactoryIOGuiEnergy {

    private int left;
    private int top;
    public int x;
    public int y;
    public int width;
    public int height;
    public int u;
    public int v;

    public FactoryIOGuiEnergy(int left, int top, int x, int y, int width, int height, int u, int v)
    {
        this.left = left;
        this.top = top;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.u = u;
        this.v = v;
    }

    public void changePos(int newX, int newY, boolean condition)
    {
        if (condition) {
            this.x = newX;
            this.y = newY;
        }
    }

    public void changeUV(int newU, int newV, boolean condition)
    {
        if (condition) {
            this.u = newU;
            this.v = newV;
        }
    }


    public void render(Screen screen, PoseStack matrix, int scaled)
    {
        screen.blit(matrix, left + x, top + y + height - scaled, u, v - scaled, width, scaled + 1);
    }

    public boolean hovering(double mouseX, double mouseY)
    {
        return mouseX >= left + x && mouseX <= left + x + width && mouseY >= top + y && mouseY <= top + y + height;
    }

    public void renderTooltip(Screen screen, PoseStack matrix, int mouseX, int mouseY, int energy, int capacity, boolean condition)
    {
        if (condition)
            if (hovering(mouseX, mouseY))
                screen.renderTooltip(matrix, new TextComponent(StringHelper.displayEnergy(energy, capacity).get(0)), mouseX, mouseY);
    }
}