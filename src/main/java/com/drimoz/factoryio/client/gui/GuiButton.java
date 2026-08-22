package com.drimoz.factoryio.client.gui;

import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.List;

/**
 * Bouton bascule dessiné dans la texture de fond d'un écran.
 *
 * <p>En 1.20 le rendu passe par {@link GuiGraphics} : {@code Screen#blit} et
 * {@code Screen#renderTooltip} n'existent plus, et {@code blit} prend désormais la
 * {@link ResourceLocation} de la texture en premier argument.
 */
public class GuiButton {

    // Private properties

    private final int left;
    private final int top;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final int uEnabled;
    private final int vEnabled;

    /** Décalage vertical, dans l'atlas, entre l'état actif et l'état inactif. */
    private static final int DISABLED_V_OFFSET = 17;

    // Life cycle

    public GuiButton(int left, int top, int x, int y, int width, int height, int uEnabled, int vEnabled) {
        this.left = left;
        this.top = top;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.uEnabled = uEnabled;
        this.vEnabled = vEnabled;
    }

    // Interface (Rendu)

    public void render(GuiGraphics graphics, ResourceLocation texture, boolean enabled) {
        if (!hasUV()) return;

        graphics.blit(
                texture,
                left + x, top + y,
                uEnabled, enabled ? vEnabled : vEnabled + DISABLED_V_OFFSET,
                width, height);
    }

    public void renderComponentTooltip(GuiGraphics graphics, Font font, List<Component> text, int mouseX, int mouseY, boolean condition) {
        if (!condition) return;
        if (!hoveringFromWindow(mouseX, mouseY)) return;

        graphics.renderComponentTooltip(font, text, mouseX, mouseY);
    }

    // Interface (Interaction)

    public void onClick(double mouseX, double mouseY, BlockPos pos, int index, int set, boolean condition) {
        if (!condition) return;
        if (!hovering(mouseX, mouseY)) return;

        ModNetworks.sendToServer(new C2SInserterSetting(
                pos, C2SInserterSetting.Setting.FILTER_MODE, set));
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 0.3F));
    }

    /** Coordonnées relatives au coin haut-gauche de la fenêtre du GUI. */
    public boolean hovering(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    /** Coordonnées absolues à l'écran. */
    public boolean hoveringFromWindow(double mouseX, double mouseY) {
        return mouseX >= left + x && mouseX <= left + x + width
                && mouseY >= top + y && mouseY <= top + y + height;
    }

    // Inner work

    private boolean hasUV() {
        return uEnabled >= 0 && vEnabled >= 0;
    }
}
