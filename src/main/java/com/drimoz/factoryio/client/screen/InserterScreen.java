package com.drimoz.factoryio.client.screen;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.IconButton;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.client.gui.SideTabs;
import com.drimoz.factoryio.core.generic.container.slots.InserterBufferSlot;
import com.drimoz.factoryio.core.generic.container.slots.InserterUpgradeSlot;
import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.inserters.InserterAnimationMode;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterFilterSlot;
import com.drimoz.factoryio.core.inserters.InserterGuiLayout;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran d'un inserter, recomposé (FIO-071).
 *
 * <p>L'ancien écran collait trois textures complètes et posait chaque widget à la main sur un
 * fond qui ne le prévoyait pas ; le quatrième n'avait plus de place. Celui-ci se construit :
 *
 * <ul>
 *   <li>une fenêtre étirable, à la taille que donne {@link InserterGuiLayout} ;</li>
 *   <li>dans la fenêtre, le travail de la machine — alimentation, main, filtres ;</li>
 *   <li>dans le bandeau, les bascules à deux ou trois positions — marche, animation, liste ;</li>
 *   <li>sur les côtés, des onglets pour tout le reste — informations à gauche, améliorations,
 *       condition redstone et réglages à droite.</li>
 * </ul>
 *
 * <p>Aucune coordonnée de slot n'est écrite ici : elles viennent du layout, que le menu lit
 * aussi. Aucune coordonnée de texture non plus : elles sont toutes dans {@link GuiSprites}.
 *
 * <p>Rien n'est appliqué localement. Chaque réglage part au serveur, qui fait autorité et
 * renvoie l'état par {@code getUpdateTag} ; l'écran relit le block entity à chaque image.
 */
public class InserterScreen extends AbstractContainerScreen<InserterContainer> {

    /** Teinte des slots de filtre en mode tag : assez transparente pour laisser voir l'item. */
    private static final int TAG_FILTER_TINT = 0x6033B5E5;

    /** Largeur des infobulles longues, au-delà de laquelle elles passent à la ligne. */
    private static final int TOOLTIP_WIDTH = 200;

    private static final int LABEL_COLOUR = 0x404040;

    private final InserterGuiLayout gui;
    private final List<IconButton> toggles = new ArrayList<>();
    private final ThroughputMeter meter = new ThroughputMeter();
    private SideTabs tabs;

    public InserterScreen(InserterContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);

        this.gui = menu.getGuiLayout();
        this.imageWidth = InserterGuiLayout.WIDTH;
        this.imageHeight = this.gui.height();
        this.titleLabelY = InserterGuiLayout.TITLE_Y;
        this.inventoryLabelY = this.gui.inventoryLabelY();
    }

    @Override
    protected void init() {
        super.init();

        this.toggles.clear();
        this.tabs = new SideTabs(List.of(), null);
        if (!getMenu().isBacked()) return;

        buildToggles();

        List<SideTab> sideTabs = new ArrayList<>();
        sideTabs.add(new InserterTabs.Info(this));
        if (this.gui.hasAugmentTab()) sideTabs.add(new InserterTabs.Augments(this));
        if (getMenu().isAffectedByRedstone()) sideTabs.add(new InserterTabs.Redstone(this));
        sideTabs.add(new InserterTabs.Settings(this));

        this.tabs = new SideTabs(sideTabs, this.gui.hasAugmentTab() ? InserterTabs.Augments.ID : null);
    }

    /**
     * Bascules du bandeau, alignées à droite. Une bascule est un réglage à deux ou trois
     * positions qu'on veut changer d'un clic : elle ne mérite pas un onglet.
     */
    private void buildToggles() {
        int size = InserterGuiLayout.TOGGLE_SIZE;

        this.toggles.add(new IconButton(0, 0, size, size)
                .icon(() -> blockEntity().isSwitchedOn() ? GuiSprites.Icon.POWER_ON : GuiSprites.Icon.POWER_OFF)
                .tooltip(() -> List.of(
                        ModUtils.tooltipComponent(blockEntity().isSwitchedOn() ? "power_on" : "power_off")
                                .withStyle(blockEntity().isSwitchedOn() ? ChatFormatting.GREEN : ChatFormatting.RED),
                        ModUtils.tooltipComponent("power_help").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> send(C2SInserterSetting.Setting.POWER, blockEntity().isSwitchedOn() ? 0 : 1)));

        this.toggles.add(new IconButton(0, 0, size, size)
                .icon(() -> animationIcon(blockEntity().getAnimationMode()))
                .tooltip(() -> List.of(
                        ModUtils.tooltipComponent("animation",
                                ModUtils.tooltipComponent(blockEntity().getAnimationMode().translationKey())
                                        .withStyle(ChatFormatting.AQUA)),
                        ModUtils.tooltipComponent("animation_help").withStyle(ChatFormatting.GRAY)))
                .onPress(() -> send(C2SInserterSetting.Setting.ANIMATION,
                        blockEntity().getAnimationMode().next().ordinal())));

        if (getMenu().isFilterable()) {
            this.toggles.add(new IconButton(0, 0, size, size)
                    .icon(() -> blockEntity().isWhitelist() ? GuiSprites.Icon.WHITELIST : GuiSprites.Icon.BLACKLIST)
                    .tooltip(() -> {
                        boolean whitelist = blockEntity().isWhitelist();
                        return List.of(
                                ModUtils.tooltipComponent(whitelist ? "whitelist" : "blacklist"),
                                ModUtils.tooltipComponent("whitelist_switch",
                                                ModUtils.tooltipComponent(whitelist ? "blacklist" : "whitelist")
                                                        .withStyle(ChatFormatting.GOLD))
                                        .withStyle(ChatFormatting.GRAY));
                    })
                    .onPress(() -> send(C2SInserterSetting.Setting.FILTER_MODE, blockEntity().isWhitelist() ? 0 : 1)));
        }

        // Alignée à droite : les positions ne se connaissent qu'une fois la rangée complète.
        int step = size + 1;
        int x = InserterGuiLayout.WIDTH - 7 - this.toggles.size() * step + 1;
        for (IconButton toggle : this.toggles) {
            toggle.movedTo(x, InserterGuiLayout.TOGGLE_Y);
            x += step;
        }
    }

    private static GuiSprites.Icon animationIcon(InserterAnimationMode mode) {
        return switch (mode) {
            case SMOOTH -> GuiSprites.Icon.ANIMATION_SMOOTH;
            case SNAP -> GuiSprites.Icon.ANIMATION_SNAP;
            case OFF -> GuiSprites.Icon.ANIMATION_OFF;
        };
    }

    // Interface (pour les onglets)

    InserterBlockEntity blockEntity() {
        return getMenu().getBlockEntity();
    }

    InserterGuiLayout gui() {
        return this.gui;
    }

    ThroughputMeter meter() {
        return this.meter;
    }

    /**
     * Envoie un réglage au serveur, sans l'appliquer localement : c'est lui qui fait autorité,
     * et une prédiction afficherait brièvement un réglage qu'il pourrait refuser.
     */
    void send(C2SInserterSetting.Setting setting, int value) {
        if (!getMenu().isBacked()) return;

        ModNetworks.sendToServer(new C2SInserterSetting(blockEntity().getBlockPos(), setting, value));
    }

    /** Montre ou cache les slots d'amélioration avec l'onglet qui les porte. */
    void showUpgradeSlots(boolean shown) {
        for (Slot slot : getMenu().slots) {
            if (slot instanceof InserterUpgradeSlot upgrade) upgrade.setShown(shown);
        }
    }

    /** Rectangles occupés par les onglets, pour JEI (FIO-171). */
    public List<Rect2i> getExtraAreas() {
        return this.tabs == null ? List.of() : this.tabs.areas();
    }

    // Interface (Cycle)

    @Override
    protected void containerTick() {
        super.containerTick();

        if (this.minecraft != null && this.minecraft.level != null) {
            this.meter.sample(this.minecraft.level.getGameTime(), getMenu().getItemsDelivered());
        }
    }

    // Interface (Rendu)

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Le bloc a disparu sous l'écran : rien à dessiner, et le menu se ferme de lui-même au
        // prochain stillValid (cf. BUG-020).
        if (!getMenu().isBacked()) {
            this.onClose();
            return;
        }

        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        renderTagFilterHighlights(graphics);

        // Après les slots, sinon les infobulles passent dessous.
        this.renderTooltip(graphics, mouseX, mouseY);
        renderWidgetTooltips(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;

        // Les onglets d'abord : la fenêtre recouvre leur bord intérieur.
        this.tabs.renderBackgrounds(graphics, left, top);
        GuiSprites.panel(graphics, left, top, this.imageWidth, this.imageHeight);

        renderPower(graphics, left, top);
        renderHand(graphics, left, top);

        for (Slot slot : getMenu().slots) {
            // La main a son grand socle, et les slots d'onglet sont dessinés par l'onglet.
            if (slot instanceof InserterBufferSlot || slot instanceof InserterUpgradeSlot) continue;

            GuiSprites.slot(graphics, left + slot.x, top + slot.y);
        }

        for (IconButton toggle : this.toggles) toggle.render(graphics, this.font, left, top, mouseX, mouseY);

        this.tabs.renderForegrounds(graphics, this.font, mouseX, mouseY);
    }

    /** Colonne de gauche : jauge d'énergie, ou flamme et slot de carburant. */
    private void renderPower(GuiGraphics graphics, int left, int top) {
        int capacity = getMenu().getPowerCapacity();
        float fill = capacity > 0 ? (float) getMenu().getPowerStored() / capacity : 0f;

        if (getMenu().usesEnergy()) {
            GuiSprites.energyGauge(graphics, left + InserterGuiLayout.GAUGE_X, top + InserterGuiLayout.CONTENT_TOP,
                    InserterGuiLayout.GAUGE_WIDTH, this.gui.gaugeHeight(), fill);
        } else {
            GuiSprites.flame(graphics, left + InserterGuiLayout.FLAME_X, top + InserterGuiLayout.FLAME_Y, fill);
        }
    }

    /** Le trajet : ▶ main ▶. La main est le seul slot à grand socle — c'est ce qui bouge. */
    private void renderHand(GuiGraphics graphics, int left, int top) {
        GuiSprites.arrow(graphics, left + this.gui.inArrowX(), top + this.gui.arrowY());
        GuiSprites.bigSlot(graphics, left + this.gui.handSocketX(), top + this.gui.handSocketY());
        GuiSprites.arrow(graphics, left + this.gui.outArrowX(), top + this.gui.arrowY());
    }

    /**
     * Le titre cède la place aux bascules plutôt que de passer dessous : un nom trop long est
     * coupé et suffixé d'une ellipse.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int room = InserterGuiLayout.WIDTH - this.titleLabelX - 10
                - this.toggles.size() * (InserterGuiLayout.TOGGLE_SIZE + 1);

        String title = this.title.getString();
        if (this.font.width(title) > room) {
            title = this.font.plainSubstrByWidth(title, room - this.font.width("…")) + "…";
        }

        graphics.drawString(this.font, title, this.titleLabelX, this.titleLabelY, LABEL_COLOUR, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOUR, false);
    }

    /** Teinte les slots de filtre dont la correspondance porte sur le tag. */
    private void renderTagFilterHighlights(GuiGraphics graphics) {
        for (Slot slot : getMenu().slots) {
            if (!(slot instanceof InserterFilterSlot filter) || !filter.isTagFilter()) continue;

            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, TAG_FILTER_TINT);
        }
    }

    private void renderWidgetTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) return;

        List<Component> lines = new ArrayList<>();

        for (IconButton toggle : this.toggles) {
            lines.addAll(toggle.tooltipAt(this.leftPos, this.topPos, mouseX, mouseY));
        }

        if (lines.isEmpty()) lines.addAll(this.tabs.tooltipAt(mouseX, mouseY));

        if (lines.isEmpty() && isOverPower(mouseX, mouseY)) {
            lines.add(getMenu().usesEnergy()
                    ? StringHelper.displayEnergy(getMenu().getPowerStored(), getMenu().getPowerCapacity())
                    : ModUtils.tooltipComponent("fuel_stored",
                            getMenu().getPowerStored(), getMenu().getPowerCapacity()));
        }

        if (lines.isEmpty()) return;

        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(this.font.split(line, TOOLTIP_WIDTH));

        graphics.renderTooltip(this.font, wrapped, mouseX, mouseY);
    }

    private boolean isOverPower(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos;
        double y = mouseY - this.topPos;

        boolean energy = getMenu().usesEnergy();
        int x0 = energy ? InserterGuiLayout.GAUGE_X : InserterGuiLayout.FLAME_X;
        int width = energy ? InserterGuiLayout.GAUGE_WIDTH : InserterGuiLayout.FLAME_SIZE;
        int height = energy ? this.gui.gaugeHeight() : InserterGuiLayout.FLAME_SIZE;

        return x >= x0 && x < x0 + width
                && y >= InserterGuiLayout.CONTENT_TOP && y < InserterGuiLayout.CONTENT_TOP + height;
    }

    // Interface (Interaction)

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && getMenu().isBacked()) {
            for (IconButton toggle : this.toggles) {
                if (toggle.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY)) return true;
            }

            if (this.tabs.mouseClicked(mouseX, mouseY, button)) return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Un clic sur un onglet est hors de la fenêtre, et vanilla le prendrait pour un lâcher de
     * l'item porté par le curseur.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int mouseButton) {
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, mouseButton)
                && !this.tabs.contains(mouseX, mouseY);
    }

    /**
     * Ajoute au tooltip d'un slot de filtre son mode de correspondance et les tags concernés.
     *
     * <p>Sans cela, le mode par tag serait invisible : ni la teinte ni le clic droit ne sont
     * devinables, et la liste des tags est ce qui permet de comprendre <i>pourquoi</i> un item
     * passe le filtre.
     */
    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        List<Component> lines = new ArrayList<>(super.getTooltipFromContainerItem(stack));

        if (!(this.hoveredSlot instanceof InserterFilterSlot filter) || stack.isEmpty()) return lines;

        boolean byTag = filter.isTagFilter();

        lines.add(ModUtils.tooltipComponent(byTag ? "filter_tag" : "filter_exact")
                .withStyle(byTag ? ChatFormatting.AQUA : ChatFormatting.GRAY));

        if (byTag) {
            List<String> tags = stack.getTags().map(tag -> tag.location().toString()).toList();

            if (tags.isEmpty()) {
                lines.add(ModUtils.tooltipComponent("filter_tag_none").withStyle(ChatFormatting.RED));
            } else {
                tags.forEach(tag -> lines.add(Component.literal(" " + tag).withStyle(ChatFormatting.DARK_AQUA)));
            }
        }

        lines.add(ModUtils.tooltipComponent("filter_tag_switch").withStyle(ChatFormatting.DARK_GRAY));

        return lines;
    }
}
