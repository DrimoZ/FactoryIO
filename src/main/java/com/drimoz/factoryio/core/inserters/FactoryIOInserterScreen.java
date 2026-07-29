package com.drimoz.factoryio.core.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.shared.FactoryIOUtils;
import com.drimoz.factoryio.shared.gui.FactoryIOGuiButton;
import com.drimoz.factoryio.shared.gui.FactoryIOGuiEnergy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class FactoryIOInserterScreen<T extends FactoryIOInserterContainer> extends AbstractContainerScreen<FactoryIOInserterContainer> {

    // Public properties

    public static final ResourceLocation GUI_FILTER_INSERTER = new ResourceLocation(FactoryIO.MOD_ID, "textures/gui/filter_inserter_gui.png");
    public static final ResourceLocation GUI_BURNER_INSERTER = new ResourceLocation(FactoryIO.MOD_ID, "textures/gui/burner_inserter_gui.png");
    public static final ResourceLocation GUI_INSERTER = new ResourceLocation(FactoryIO.MOD_ID, "textures/gui/inserter_gui.png");

    /** Identifiant du bouton whitelist, partagé avec le paquet C→S. */
    private static final int WHITELIST_BUTTON = 6;

    // Private properties

    private FactoryIOGuiEnergy energyBar;
    private FactoryIOGuiButton whitelistButton;

    // Life cycle

    public FactoryIOInserterScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();

        int left = this.getGuiLeft();
        int top = this.getGuiTop();

        if (getMenu().getBlockEntity().IS_ENERGY) {
            energyBar = new FactoryIOGuiEnergy(left, top, 153, 11, 12, 51, 179, 54);
        }
        if (getMenu().getBlockEntity().IS_FILTER) {
            whitelistButton = new FactoryIOGuiButton(left, top, 7, 30, 16, 16, 194, 0);
        }
    }

    // Interface (Rendu)

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        // Les tooltips du mod se dessinent après le rendu des slots, sinon ils passent
        // dessous. L'ancienne version les traçait depuis renderBg.
        this.renderTooltip(graphics, mouseX, mouseY);
        this.renderCustomTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        ResourceLocation texture = backgroundTexture();

        int relX = (this.width - this.getXSize()) / 2;
        int relY = (this.height - this.getYSize()) / 2;

        // GuiGraphics.blit prend la texture en argument : plus besoin de
        // RenderSystem.setShader / setShaderTexture.
        graphics.blit(texture, relX, relY, 0, 0, this.getXSize(), this.getYSize());

        if (getMenu().getBlockEntity().IS_ENERGY && getMenu().hasEnergy()) {
            energyBar.render(graphics, texture, getMenu().getEnergyScaled(51));
        }

        if (!getMenu().getBlockEntity().IS_ENERGY && getMenu().hasFuel()) {
            int k = getMenu().getFuelScaled(13);
            graphics.blit(texture, relX + 80, relY + 32 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        if (getMenu().getBlockEntity().IS_FILTER) {
            whitelistButton.render(graphics, texture, getMenu().getBlockEntity().isWhitelist());
        }
    }

    // Interface (Interaction)

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (getMenu().getBlockEntity().IS_FILTER) {
            double relativeX = mouseX - this.getGuiLeft();
            double relativeY = mouseY - this.getGuiTop();

            boolean whitelist = getMenu().getBlockEntity().isWhitelist();
            whitelistButton.onClick(relativeX, relativeY, getMenu().getBlockEntity().getBlockPos(),
                    WHITELIST_BUTTON, whitelist ? 0 : 1, true);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // Inner work

    private ResourceLocation backgroundTexture() {
        if (!getMenu().getBlockEntity().IS_ENERGY) return GUI_BURNER_INSERTER;
        if (getMenu().getBlockEntity().IS_FILTER) return GUI_FILTER_INSERTER;
        return GUI_INSERTER;
    }

    private void renderCustomTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        FactoryIOInserterBlockEntity blockEntity = getMenu().getBlockEntity();

        if (blockEntity.IS_ENERGY) {
            energyBar.renderTooltip(graphics, this.font, mouseX, mouseY,
                    blockEntity.getCurrentEnergy(), blockEntity.getEnergyCapacity(), true);
        }

        if (blockEntity.IS_FILTER) {
            boolean whitelist = blockEntity.isWhitelist();

            List<Component> lines = new ArrayList<>();
            lines.add(FactoryIOUtils.tooltipComponent(whitelist ? "whitelist" : "blacklist"));
            lines.add(FactoryIOUtils.tooltipComponent("whitelist_switch").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(" "))
                    .append(FactoryIOUtils.tooltipComponent(whitelist ? "blacklist" : "whitelist")
                            .withStyle(ChatFormatting.GOLD)));

            whitelistButton.renderComponentTooltip(graphics, this.font, lines, mouseX, mouseY, true);
        }
    }
}
