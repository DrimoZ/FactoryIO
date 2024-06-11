package com.drimoz.factoryio.core.screens.inserters;

import com.drimoz.factoryio.FactoryIO;
import com.drimoz.factoryio.core.containers.inserters.FactoryIOInserterContainer;
import com.drimoz.factoryio.shared.gui.FactoryIOGuiButton;
import com.drimoz.factoryio.shared.gui.FactoryIOGuiEnergy;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class FactoryIOInserterScreen<T extends FactoryIOInserterContainer> extends AbstractContainerScreen<FactoryIOInserterContainer> {
    public static final ResourceLocation GUI_FILTER_INSERTER = new ResourceLocation(FactoryIO.MOD_ID + ":textures/gui/filter_inserter_gui.png");
    public static final ResourceLocation GUI_BURNER_INSERTER = new ResourceLocation(FactoryIO.MOD_ID + ":textures/gui/burner_inserter_gui.png");
    public static final ResourceLocation GUI_INSERTER = new ResourceLocation(FactoryIO.MOD_ID + ":textures/gui/inserter_gui.png");

    public FactoryIOGuiEnergy energyBar;
    public FactoryIOGuiButton whitelistButton;

    Inventory inv;
    Component name;

    public FactoryIOInserterScreen(T pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        inv = pPlayerInventory;
        name = pTitle;
    }
    protected void init() {
        super.init();
        int left = this.getGuiLeft();
        int top = this.getGuiTop();
        if (getMenu().getBlockEntity().IS_ENERGY)
            energyBar = new FactoryIOGuiEnergy(left, top, 153, 11, 12, 51, 179, 54);
        if (getMenu().getBlockEntity().IS_FILTER)
            whitelistButton = new FactoryIOGuiButton(left, top, 7, 30, 16, 16, 194, 0);
    }
    public void render(PoseStack matrix, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrix);
        super.render(matrix, mouseX, mouseY, partialTicks);
        this.renderTooltip(matrix, mouseX, mouseY);
    }

    @Override
    protected void renderBg(PoseStack pPoseStack, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        //if (getMenu().getBlockEntity() instanceof BurnerInserterEntity)
        //    RenderSystem.setShaderTexture(0, GUI_BURNER_INSERTER);
        //else if (getMenu().getBlockEntity() instanceof FilterInserterEntity)
        //    RenderSystem.setShaderTexture(0, GUI_FILTER_INSERTER);
        //else
            RenderSystem.setShaderTexture(0, GUI_INSERTER);

        int relX = (this.width - this.getXSize()) / 2;
        int relY = (this.height - this.getYSize()) / 2;

        this.blit(pPoseStack, relX, relY, 0, 0, this.getXSize(), this.getYSize());

        if (getMenu().getBlockEntity().IS_ENERGY && getMenu().hasEnergy()) {
            energyBar.render(this, pPoseStack,getMenu().getEnergyScaled(51));
        }

        if (!getMenu().getBlockEntity().IS_ENERGY && getMenu().hasFuel()) {
            int k = getMenu().getFuelScaled(13);
            this.blit(pPoseStack, relX + 80, relY + 32 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        if (getMenu().getBlockEntity().IS_FILTER) {
            whitelistButton.render(this, pPoseStack, pMouseX, pMouseY, getMenu().getBlockEntity().isWhitelist());

        }
        this.addToolTips(pPoseStack, pMouseX, pMouseY);
    }

    public static boolean isShiftKeyDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double actualMouseX = mouseX - (((double)this.width - (double)this.getXSize()) / 2);
        double actualMouseY = mouseY - (((double)this.height - (double)this.getYSize()) / 2);
        if (getMenu().getBlockEntity().IS_FILTER) {
            this.mouseClickedInventoryButtons(button, actualMouseX, actualMouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseClickedInventoryButtons(int button, double mouseX, double mouseY) {
        if (getMenu().getBlockEntity().IS_FILTER && !getMenu().getBlockEntity().isWhitelist())
        {
            whitelistButton.onClick(mouseX, mouseY, getMenu().getBlockEntity().getBlockPos(), 6, 1, true);
        }
        else if (getMenu().getBlockEntity().IS_FILTER && getMenu().getBlockEntity().isWhitelist())
        {
            whitelistButton.onClick(mouseX, mouseY, getMenu().getBlockEntity().getBlockPos(), 6, 0, true);
        }
    }

    private void addToolTips(PoseStack pPoseStack, int mouseX, int mouseY) {
        if (getMenu().getBlockEntity().IS_ENERGY) {
            energyBar.renderTooltip(this, pPoseStack, mouseX, mouseY, getMenu().getBlockEntity().getCurrentEnergy(), getMenu().getBlockEntity().getEnergyCapacity(), true);

        }
        if (getMenu().getBlockEntity().IS_FILTER) {
            List list = Lists.newArrayList();
            list.add(new TextComponent("" + (getMenu().getBlockEntity().isWhitelist() ?
                    new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".whitelist").getString() :
                    new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".blacklist").getString())));
            list.add(new TextComponent("§7Click to switch into a §6" + (getMenu().getBlockEntity().isWhitelist() ?
                    new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".blacklist").getString():
                    new TranslatableComponent("tooltip." + FactoryIO.MOD_ID + ".whitelist").getString())));
            whitelistButton.renderComponentTooltip(this, pPoseStack, list, mouseX, mouseY, true);
        }
    }

    public static boolean isKeyDown(int glfw) {
        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(glfw);
        int keyCode = key.getValue();
        if (keyCode != InputConstants.UNKNOWN.getValue()) {
            long windowHandle = Minecraft.getInstance().getWindow().getWindow();
            try {
                if (key.getType() == InputConstants.Type.KEYSYM) {
                    return InputConstants.isKeyDown(windowHandle, keyCode);
                } /**else if (key.getType() == InputMappings.Type.MOUSE) {
                 return GLFW.glfwGetMouseButton(windowHandle, keyCode) == GLFW.GLFW_PRESS;
                 }**/
            } catch (Exception ignored) {
            }
        }
        return false;
    }

}
