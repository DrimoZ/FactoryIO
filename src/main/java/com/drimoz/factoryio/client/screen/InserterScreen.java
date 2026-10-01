package com.drimoz.factoryio.client.screen;

import com.drimoz.factoryio.client.gui.GuiSprites;
import com.drimoz.factoryio.client.gui.GuiTheme;
import com.drimoz.factoryio.shared.GuiMetrics;
import javax.annotation.Nullable;
import com.drimoz.factoryio.client.gui.IconButton;
import com.drimoz.factoryio.client.gui.SideTab;
import com.drimoz.factoryio.client.gui.SideTabs;
import com.drimoz.factoryio.core.generic.container.slots.OutputSlot;
import com.drimoz.factoryio.core.generic.container.slots.UpgradeSlot;
import com.drimoz.factoryio.core.init.ModNetworks;
import com.drimoz.factoryio.core.inserters.InserterBlockEntity;
import com.drimoz.factoryio.core.inserters.InserterContainer;
import com.drimoz.factoryio.core.inserters.InserterFilterSlot;
import com.drimoz.factoryio.core.inserters.InserterGuiLayout;
import com.drimoz.factoryio.core.inserters.InserterState;
import com.drimoz.factoryio.core.network.packet.C2SInserterSetting;
import com.drimoz.factoryio.shared.ModUtils;
import com.drimoz.factoryio.shared.StringHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
 *   <li>dans la fenêtre, le travail de la machine seulement — alimentation, trajet de la main,
 *       filtres et leur mode ;</li>
 *   <li>sur les côtés, des onglets pour tout le reste — informations et réglages à gauche ;
 *       améliorations et contrôle (marche et redstone) à droite. Deux par côté : aucune pile
 *       ne dépasse la fenêtre.</li>
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


    private final InserterGuiLayout gui;
    private final ThroughputMeter meter = new ThroughputMeter();
    private IconButton listButton;
    private SideTabs tabs;
    @Nullable private InserterTabs.Info info;

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

        this.listButton = null;
        this.info = null;
        this.tabs = new SideTabs(List.of(), InserterGuiLayout.WIDTH);
        if (!getMenu().isBacked()) return;

        if (getMenu().isFilterable()) {
            int size = InserterGuiLayout.LIST_BUTTON_SIZE;
            this.listButton = new IconButton(this.gui.listButtonX(), this.gui.listButtonY(), size, size)
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
                    .onPress(() -> send(C2SInserterSetting.Setting.FILTER_MODE, blockEntity().isWhitelist() ? 0 : 1));
        }

        List<SideTab> sideTabs = new ArrayList<>();
        this.info = new InserterTabs.Info(this);
        sideTabs.add(this.info);
        if (this.gui.hasAugmentTab()) sideTabs.add(new InserterTabs.Augments(this));
        sideTabs.add(InserterTabs.control(this));
        sideTabs.add(new InserterTabs.Settings(this));

        this.tabs = new SideTabs(sideTabs, InserterGuiLayout.WIDTH, InserterTabs.Augments.ID);
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
            if (slot instanceof UpgradeSlot upgrade) upgrade.setShown(shown);
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

        if (this.info != null) {
            GuiSprites.statusLight(graphics, this.info.state().status, left + GuiMetrics.LED_X, top + GuiMetrics.LED_Y);
        }

        renderPower(graphics, left, top);
        renderHand(graphics, left, top, partialTick);

        for (Slot slot : getMenu().slots) {
            // La main a son grand socle, et les slots d'onglet sont dessinés par l'onglet.
            if (slot instanceof OutputSlot || slot instanceof UpgradeSlot) continue;

            GuiSprites.slot(graphics, left + slot.x, top + slot.y);
        }

        if (this.listButton != null) this.listButton.render(graphics, this.font, left, top, mouseX, mouseY);

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

    /**
     * Le trajet : ▶ main ▶. Les flèches suivent le bras — la première se remplit quand il
     * revient chercher, la seconde quand il porte — et la main vide montre ce qui va là.
     */
    private void renderHand(GuiGraphics graphics, int left, int top, float partialTick) {
        InserterBlockEntity be = blockEntity();
        InserterState state = be.getState();
        float progress = be.getArmProgress(partialTick);

        float in = switch (state) {
            case RETURNING -> progress;
            case SWINGING, BLOCKED -> 1f;
            case WAITING -> 0f;
        };
        float out = switch (state) {
            case SWINGING -> progress;
            case BLOCKED -> 1f;
            case RETURNING, WAITING -> 0f;
        };

        GuiSprites.arrow(graphics, left + this.gui.inArrowX(), top + this.gui.arrowY(), in);
        GuiSprites.bigSlot(graphics, left + this.gui.handSocketX(), top + this.gui.handSocketY());
        GuiSprites.arrow(graphics, left + this.gui.outArrowX(), top + this.gui.arrowY(), out);

        renderNeighbour(graphics, left + this.gui.sourceSocketX(), top + this.gui.neighbourSocketY(), true);
        renderNeighbour(graphics, left + this.gui.targetSocketX(), top + this.gui.neighbourSocketY(), false);

        if (be.getHeldStack().isEmpty()) {
            GuiSprites.faintIcon(graphics, GuiSprites.Icon.HAND,
                    left + this.gui.handSocketX() + (InserterGuiLayout.HAND_SOCKET - GuiSprites.ICON_SIZE) / 2,
                    top + this.gui.handSocketY() + (InserterGuiLayout.HAND_SOCKET - GuiSprites.ICON_SIZE) / 2);
        }
    }

    /** Socle et icône du bloc visé ; vide s'il n'y a rien. */
    private void renderNeighbour(GuiGraphics graphics, int x, int y, boolean source) {
        GuiSprites.inset(graphics, x, y, InserterGuiLayout.SLOT, InserterGuiLayout.SLOT);

        ItemStack icon = iconOf(neighbourPos(source), neighbour(source));
        if (!icon.isEmpty()) graphics.renderItem(icon, x + 1, y + 1);
    }

    /**
     * Le bloc que l'inserter vise derrière ({@code source}) ou devant lui, à sa portée réelle —
     * modules compris : un long inserter regarde à deux blocs.
     */
    private BlockState neighbour(boolean source) {
        InserterBlockEntity be = blockEntity();
        if (be.getLevel() == null) return null;

        return be.getLevel().getBlockState(neighbourPos(source));
    }

    private BlockPos neighbourPos(boolean source) {
        InserterBlockEntity be = blockEntity();
        Direction facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return be.getBlockPos().relative(source ? facing.getOpposite() : facing, be.getGrabDistance());
    }

    /**
     * L'item que donnerait le « bloc choisi » : une partie de multibloc n'a pas d'item, mais
     * renvoie celui de sa machine — c'est elle qu'on veut voir.
     */
    private ItemStack iconOf(BlockPos pos, BlockState state) {
        if (state == null || state.isAir() || blockEntity().getLevel() == null) return ItemStack.EMPTY;

        return state.getBlock().getCloneItemStack(blockEntity().getLevel(), pos, state);
    }

    private List<Component> neighbourTooltip(double mouseX, double mouseY) {
        double y = mouseY - this.topPos;
        if (y < this.gui.neighbourSocketY() || y >= this.gui.neighbourSocketY() + InserterGuiLayout.SLOT) return List.of();

        double x = mouseX - this.leftPos;
        boolean source;
        if (x >= this.gui.sourceSocketX() && x < this.gui.sourceSocketX() + InserterGuiLayout.SLOT) source = true;
        else if (x >= this.gui.targetSocketX() && x < this.gui.targetSocketX() + InserterGuiLayout.SLOT) source = false;
        else return List.of();

        BlockState state = neighbour(source);
        if (state == null || state.isAir()) {
            return List.of(ModUtils.tooltipComponent(source ? "source_none" : "target_none").withStyle(ChatFormatting.GRAY));
        }

        ItemStack icon = iconOf(neighbourPos(source), state);
        Component name = icon.isEmpty() ? state.getBlock().getName() : icon.getHoverName().copy();
        return List.of(ModUtils.tooltipComponent(source ? "source_block" : "target_block",
                name.copy().withStyle(ChatFormatting.AQUA)));
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        GuiTheme.text(graphics, this.font, this.title, this.titleLabelX, this.titleLabelY);
        GuiTheme.text(graphics, this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY);
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

        if (this.listButton != null) lines.addAll(this.listButton.tooltipAt(this.leftPos, this.topPos, mouseX, mouseY));

        if (lines.isEmpty()) lines.addAll(this.tabs.tooltipAt(mouseX, mouseY));

        if (lines.isEmpty()) lines.addAll(neighbourTooltip(mouseX, mouseY));

        if (lines.isEmpty() && this.info != null && isOverLed(mouseX, mouseY)) {
            lines.add(ModUtils.tooltipComponent(this.info.state().key));
        }

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

    private boolean isOverLed(double mouseX, double mouseY) {
        double x = mouseX - this.leftPos - GuiMetrics.LED_X;
        double y = mouseY - this.topPos - GuiMetrics.LED_Y;
        return x >= -1 && x < GuiMetrics.LED_SIZE + 1 && y >= -1 && y < GuiMetrics.LED_SIZE + 1;
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
            if (this.listButton != null && this.listButton.mouseClicked(this.leftPos, this.topPos, mouseX, mouseY)) {
                return true;
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
