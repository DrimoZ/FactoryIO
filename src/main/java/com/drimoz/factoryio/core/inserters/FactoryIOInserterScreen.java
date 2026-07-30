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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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

        // Les slots en mode tag sont teintés par-dessus leur contenu : la texture de GUI
        // est figée et n'a pas de case libre pour une icône (cf. FIO-069, FIO-071).
        this.renderTagFilterHighlights(graphics);

        // Les tooltips du mod se dessinent après le rendu des slots, sinon ils passent
        // dessous. L'ancienne version les traçait depuis renderBg.
        this.renderTooltip(graphics, mouseX, mouseY);
        this.renderCustomTooltips(graphics, mouseX, mouseY);
    }

    /** Teinte les slots de filtre dont la correspondance porte sur le tag. */
    private void renderTagFilterHighlights(GuiGraphics graphics) {
        for (Slot slot : getMenu().slots) {
            if (!(slot instanceof InserterFilterSlot filter) || !filter.isTagFilter()) continue;

            int x = this.getGuiLeft() + slot.x;
            int y = this.getGuiTop() + slot.y;

            graphics.fill(x, y, x + 16, y + 16, TAG_FILTER_TINT);
        }
    }

    /** Teinte des slots en mode tag : ARGB, assez transparente pour laisser voir l'item. */
    private static final int TAG_FILTER_TINT = 0x6033B5E5;

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

    /**
     * Ajoute au tooltip d'un slot de filtre son mode de correspondance et les tags
     * concernés.
     *
     * <p>Sans cela, le mode par tag serait invisible : ni la teinte ni le clic droit ne
     * sont devinables, et la liste des tags est ce qui permet de comprendre <i>pourquoi</i>
     * un item passe le filtre.
     */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));

        if (!(this.hoveredSlot instanceof InserterFilterSlot filter) || stack.isEmpty()) return lines;

        boolean byTag = filter.isTagFilter();

        lines.add(FactoryIOUtils.tooltipComponent(byTag ? "filter_tag" : "filter_exact")
                .withStyle(byTag ? ChatFormatting.AQUA : ChatFormatting.GRAY));

        if (byTag) {
            List<String> tags = stack.getTags().map(tag -> tag.location().toString()).toList();

            if (tags.isEmpty()) {
                lines.add(FactoryIOUtils.tooltipComponent("filter_tag_none").withStyle(ChatFormatting.RED));
            } else {
                tags.forEach(tag -> lines.add(
                        Component.literal(" " + tag).withStyle(ChatFormatting.DARK_AQUA)));
            }
        }

        lines.add(FactoryIOUtils.tooltipComponent("filter_tag_switch").withStyle(ChatFormatting.DARK_GRAY));

        return lines;
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
            // Valeurs lues sur le menu : côté client le block entity n'est plus synchronisé
            // en continu, c'est le ContainerData qui fait foi (cf. BUG-004).
            energyBar.renderTooltip(graphics, this.font, mouseX, mouseY,
                    getMenu().getPowerStored(), getMenu().getPowerCapacity(), true);
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
